package fuzzypack.data.weapons.onHit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

public class rod_onhit implements OnHitEffectPlugin {

    private static final float CR_PER_TICK_FRIGATE = 0.02f;
    private static final float CR_PER_TICK_DESTROYER = 0.01f;
    private static final float CR_PER_TICK_CRUISER = 0.005f;
    private static final float CR_PER_TICK_CAPITAL = 0.002f;

    private static final float SHIELD_SOFT_FLUX = 1000f;

    private static final float ZONE_RADIUS = 400f;
    private static final float ZONE_DURATION = 6f;
    private static final float ZONE_TICK = 0.5f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        engine.addSwirlyNebulaParticle(
                point, new Vector2f(0, 0),
                ZONE_RADIUS * 0.7f, 1.2f,
                0.2f, 1f, ZONE_DURATION+1f,
                new Color(80, 200, 10, 200),
                false);

        MissileAPI mine = (MissileAPI) engine.spawnProjectile(null,
                null,
                "fp_danger_dummy",
                point,
                90, null);
        mine.setCollisionClass(CollisionClass.NONE);
        mine.setMineExplosionRange(ZONE_RADIUS);
        //mine.setSpriteAlphaOverride(0f);
        //mine.getSpec().setGlowSpriteName("radioactive_indicator");
        engine.addPlugin(new RodCrZone(new Vector2f(point), projectile.getSource(), mine));
    }

    static class RodCrZone extends BaseEveryFrameCombatPlugin {
        private final Vector2f loc;
        //private final ShipAPI source;
        private final IntervalUtil pulse = new IntervalUtil(ZONE_TICK, ZONE_TICK);
        private float duration = 0f;
        private MissileAPI mine;

        RodCrZone(Vector2f loc, ShipAPI source, MissileAPI mine) {
            this.loc = loc;
            //this.source = source;
            this.mine = mine;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;

            duration += amount;
            pulse.advance(amount);
            float r = ZONE_RADIUS * (0.4f + 0.3f * (1f - duration / ZONE_DURATION));

            /* engine.addSwirlyNebulaParticle(
                    loc, new Vector2f(0, 0),
                    r + 50f,
                    1.1f, 0.1f, 0.4f, 0.35f,
                    new Color(80, 200, 10, 80),
                    true); */
            engine.addHitParticle(
                    loc,
                    new Vector2f(0,0),
                    r + 200f,
                    1f,
                    amount,
                    new Color(80, 200,1,150)
            );

            if (pulse.intervalElapsed()) {
                applyPulse(engine, r);
            }

            if (duration >= ZONE_DURATION) {
                engine.addHitParticle(
                        loc,
                        new Vector2f(0,0),
                        r + 200f,
                        1f,
                        2f,
                        new Color(80, 200,1,150)
                );
                engine.removeEntity(mine);
                engine.removePlugin(this);
            }
        }

        private float crFor(ShipAPI.HullSize size) {
            switch (size) {
                case FRIGATE: return CR_PER_TICK_FRIGATE;
                case DESTROYER: return CR_PER_TICK_DESTROYER;
                case CRUISER: return CR_PER_TICK_CRUISER;
                case CAPITAL_SHIP: return CR_PER_TICK_CAPITAL;
                default: return 0f;
            }
        }

        private void applyPulse(CombatEngineAPI engine, float radius) {
            Iterator<Object> it = engine.getShipGrid().getCheckIterator(loc, radius, radius);
            while (it.hasNext()) {
                Object o = it.next();
                if (!(o instanceof ShipAPI)) continue;
                ShipAPI ship = (ShipAPI) o;
                if (!ship.isAlive()) continue;
                if (ship.getHullSize() == ShipAPI.HullSize.FIGHTER || ship.isStation() || ship.isStationModule()) continue;
                // if (source != null && ship.getOwner() == source.getOwner()) continue;


                float dist = MathUtils.getDistance(loc, ship.getLocation());
                if (dist > radius + ship.getCollisionRadius()) continue;

                float crMod = crFor(ship.getHullSize());
                int[] cell = ship.getArmorGrid().getCellAtLocation(loc);
                if (cell != null) {
                    float max = ship.getArmorGrid().getMaxArmorInCell();
                    if (max > 0f) {
                        crMod *= 1f - (ship.getArmorGrid().getArmorValue(cell[0], cell[1]) / max);
                    }
                }
                if (crMod <= 0f) continue;

                ship.setCurrentCR(Math.max(0f, ship.getCurrentCR() - crMod));
            }
        }
    }
}