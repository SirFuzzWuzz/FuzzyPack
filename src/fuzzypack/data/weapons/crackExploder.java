package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import fuzzypack.data.weapons.tools.utilities;
import fuzzypack.data.weapons.tools.utilities.Segment;
import fuzzypack.data.weapons.tools.utilities.VineWalk;
import fuzzypack.data.weapons.tools.utilities.WalkParams;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class crackExploder implements OnHitEffectPlugin {

    private static final float DETONATE_DELAY = 2f;
    private static final float EXPLOSION_RADIUS = 10f;
    private static final float EXPLOSION_DAMAGE = 100f;
    private static final float THICKNESS = 6f;
    private static final String CRACK_SPRITE = "graphics/damage/damage_cracks48_0_base.png";
    private static final Color CRACK_COLOR = new Color(40, 40, 45, 255);
    private static final Color BOOM_COLOR = new Color(255, 160, 60, 220);

    private static WalkParams crackParams() {
        WalkParams p = new WalkParams();
        p.tick = 0.05f;
        p.step = 16f;
        p.bend = 12f;
        p.branchChance = 0.12f;
        p.branchAngle = 12f;
        p.starterSpread = 8f;
        p.maxVines = 6;
        p.maxPoints = 12;
        p.starters = 3;
        p.coverFraction = 1f;
        p.maxGrowTime = 2f;
        return p;
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {
        if (shieldHit || !(target instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) target;
        if (!ship.isAlive() || ship.isFighter()) return;

        Vector2f local = Vector2f.sub(point, ship.getLocation(), new Vector2f());
        VectorUtils.rotate(local, -ship.getFacing());
        float inbound = projectile.getFacing() - ship.getFacing();
        engine.addPlugin(new CrackBurst(ship, local, inbound, projectile.getSource()));
    }

    // The crack segments to blow up
    static class Baked {
        final Vector2f fromLocal;
        final Vector2f toLocal;
        final float localAngle;

        Baked(Vector2f from, Vector2f to) {
            this.fromLocal = new Vector2f(from);
            this.toLocal = new Vector2f(to);
            this.localAngle = VectorUtils.getAngle(from, to) - 90f;
        }
    }

    static class CrackBurst extends BaseEveryFrameCombatPlugin {
        private final ShipAPI ship;
        private final ShipAPI source;
        private final List<Baked> links = new ArrayList<Baked>();
        private float wait;

        CrackBurst(ShipAPI ship, Vector2f startLocal, float inboundLocal, ShipAPI source) {
            this.ship = ship;
            this.source = source;
            VineWalk walk = new VineWalk(ship, startLocal, inboundLocal, crackParams());
            int guard = 80;
            while (walk.growing && guard-- > 0) {
                for (Segment s : walk.advance(walk.p.tick)) {
                    links.add(new Baked(s.fromLocal, s.toLocal));
                }
            }
            this.wait = DETONATE_DELAY;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            if (ship == null || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
                engine.removePlugin(this);
                return;
            }

            float heat = 1f - (wait / DETONATE_DELAY);
            if (heat < 0f) heat = 0f;
            if (heat > 1f) heat = 1f;



            draw(amount, heat);
            wait -= amount;
            if (wait > 0f) return;

            detonate(engine);
            engine.removePlugin(this);
        }

        private void draw(float amount, float heat) {
            float facing = ship.getFacing();
            for (Baked link : links) {
                Vector2f a = utilities.toWorld(ship, link.fromLocal);
                Vector2f b = utilities.toWorld(ship, link.toLocal);
                float length = MathUtils.getDistance(a, b);
                if (length < 1f) continue;
                MagicRender.battlespace(
                        Global.getSettings().getSprite(CRACK_SPRITE),
                        MathUtils.getMidpoint(a, b),
                        new Vector2f(0f, 0f),
                        new Vector2f(THICKNESS, length + 2f),
                        new Vector2f(0f, 0f),
                        link.localAngle + facing,
                        0f,
                        CRACK_COLOR,
                        false,
                        0f,
                        amount + 0.03f,
                        0f
                );

                int red = (int) (CRACK_COLOR.getRed()   + heat * (BOOM_COLOR.getRed()   - CRACK_COLOR.getRed()));
                int green = (int) (CRACK_COLOR.getGreen() + heat * (BOOM_COLOR.getGreen() - CRACK_COLOR.getGreen()));
                int blue = (int) (CRACK_COLOR.getBlue()  + heat * (BOOM_COLOR.getBlue()  - CRACK_COLOR.getBlue()));
                int alpha = (int) (heat * 255);
                Color glow = new Color(red, green, blue, alpha);
                MagicRender.battlespace(
                        Global.getSettings().getSprite(CRACK_SPRITE),
                        MathUtils.getMidpoint(a, b),
                        new Vector2f(0f, 0f),
                        new Vector2f(THICKNESS, length + 2f),
                        new Vector2f(0f, 0f),
                        link.localAngle + facing,
                        0f,
                        glow,
                        true,
                        0f,
                        amount + 0.03f,
                        0f
                );
            }
        }

        private void detonate(CombatEngineAPI engine) {
            for (Baked link : links) {
                Vector2f p = utilities.toWorld(ship, link.toLocal);
                engine.spawnExplosion(p, ship.getVelocity(), BOOM_COLOR, EXPLOSION_RADIUS, 0.35f);
                engine.applyDamage(
                        ship, p, EXPLOSION_DAMAGE, DamageType.HIGH_EXPLOSIVE,
                        0f, false, false,
                        source != null ? source : ship
                );
            }
            if (!links.isEmpty()) {
                Global.getSoundPlayer().playSound(
                        "explosion_flak", 1f, 1f,
                        utilities.toWorld(ship, links.get(0).toLocal),
                        ship.getVelocity()
                );
            }
        }
    }
}