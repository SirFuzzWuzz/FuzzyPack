package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import fuzzypack.data.weapons.tools.utilities;
import fuzzypack.data.weapons.tools.utilities.Segment;
import fuzzypack.data.weapons.tools.utilities.VineWalk;
import fuzzypack.data.weapons.tools.utilities.WalkParams;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class vinegun implements OnFireEffectPlugin {

    private static final float HOLD_TIME = 60f;
    private static final float THICKNESS = 10f;
    private static final String DUMMY_WEAPON = "fp_dummy_missile";
    private static final float FLOWER_HITPOINTS = 500f;

    // Debuff multipliers
    private static final float WEAPON_TURN_MULT = 0.01f;
    private static final float SPEED_MULT = 0.5f;
    private static final float DISSIPATION_MULT = 0.25f;
    private static final float FIGHTER_REFIT_TIME_MULT = 1.50f;
    private static final String DEBUFF_ID = "fp_vinegun";

    private static final Color COLOR = new Color(255, 255, 255, 225);
    private static final String LINK_SPRITE = "graphics/missiles/vine_link.png";

    private static WalkParams vineParams() {
        WalkParams p = new WalkParams();
        p.tick = 0.12f;
        p.step = 12f;
        p.bend = 28f;
        p.branchChance = 0.05f;
        p.maxVines = 16; // Max amount of vines on the target
        p.maxPoints = 32; // Max points of one vine
        p.starters = 1;
        p.coverFraction = 0.75f;
        p.maxGrowTime = 3f;
        return p;
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        projectile.setCollisionClass(CollisionClass.NONE);
        engine.addPlugin(new VineSeed(projectile, weapon.getShip()));
    }

    static class Flower {
        final MissileAPI dummy;
        final Vector2f local;

        Flower(MissileAPI dummy, Vector2f local) {
            this.dummy = dummy;
            this.local = new Vector2f(local);
        }
    }

    static class BakedLink {
        final Vector2f fromLocal;
        final Vector2f toLocal;
        final float localAngle;

        BakedLink(Vector2f fromLocal, Vector2f toLocal) {
            this.fromLocal = new Vector2f(fromLocal);
            this.toLocal = new Vector2f(toLocal);
            this.localAngle = VectorUtils.getAngle(fromLocal, toLocal) - 90f;
        }
    }

    // Seed that pierces shield
    static class VineSeed extends BaseEveryFrameCombatPlugin {
        private final DamagingProjectileAPI proj;
        private final ShipAPI host;

        VineSeed(DamagingProjectileAPI proj, ShipAPI host) {
            this.proj = proj;
            this.host = host;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            if (proj == null || !engine.isEntityInPlay(proj)) {
                engine.removePlugin(this);
                return;
            }

            Vector2f tip = proj.getLocation();
            ShipAPI target = utilities.findEnemyHullAt(host, tip, 20f);
            if (target == null) return;

            plant(engine, target, new Vector2f(tip));
            engine.removeEntity(proj);
            engine.removePlugin(this);
        }

        private void plant(CombatEngineAPI engine, ShipAPI ship, Vector2f worldPoint) {
            Vector2f local = Vector2f.sub(worldPoint, ship.getLocation(), new Vector2f());
            VectorUtils.rotate(local, -ship.getFacing());
            float inbound = proj.getFacing() - ship.getFacing();

            MissileAPI dummy = (MissileAPI) engine.spawnProjectile(
                    host, null, DUMMY_WEAPON, worldPoint, proj.getFacing(), ship.getVelocity());
            dummy.setCollisionClass(CollisionClass.FIGHTER);
            dummy.setHitpoints(FLOWER_HITPOINTS);
            dummy.setAngularVelocity(0f);

            Global.getSoundPlayer().playSound("hit_solid", 1f, 1f, worldPoint, ship.getVelocity());
            engine.spawnExplosion(worldPoint, ship.getVelocity(), COLOR, 70f, 0.6f);
            engine.addHitParticle(worldPoint, ship.getVelocity(), 110f, 1f, 0.25f, COLOR);

            Object existing = ship.getCustomData().get(DEBUFF_ID);
            if (existing instanceof VinePainter) {
                ((VinePainter) existing).addHit(local, inbound, dummy);
                return;
            }
            VinePainter g = new VinePainter(ship, local, inbound, dummy);
            ship.setCustomData(DEBUFF_ID, g);
            engine.addPlugin(g);
        }
    }

    static class VinePainter extends BaseEveryFrameCombatPlugin {
        private final ShipAPI ship;
        private final VineWalk walk;
        private final SpriteAPI flowerSprite;
        private final List<Flower> flowers = new ArrayList<Flower>();
        private final List<BakedLink> links = new ArrayList<BakedLink>();
        private float holdAge;
        private boolean buffed;

        VinePainter(ShipAPI ship, Vector2f startLocal, float inboundLocal, MissileAPI dummy) {
            this.ship = ship;
            this.walk = new VineWalk(ship, startLocal, inboundLocal, vineParams());
            this.flowerSprite = Global.getSettings().getSprite("projectiles", "vine_flower");
            if (dummy != null) flowers.add(new Flower(dummy, startLocal));
        }

        void addHit(Vector2f local, float inboundLocal, MissileAPI newDummy) {
            walk.addHit(local, inboundLocal);
            holdAge = 0f;
            if (newDummy != null) flowers.add(new Flower(newDummy, local));
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            if (ship == null || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
                teardown(engine);
                return;
            }

            // Remove vines if all flowers are dead
            boolean hadFlowers = !flowers.isEmpty();
            stickAndPruneFlowers(engine);
            if (hadFlowers && flowers.isEmpty()) {
                teardown(engine);
                return;
            }

            if (buffed && ship == engine.getPlayerShip()) {
                float t = walk.coverage() / walk.p.coverFraction;
                // if (t > 1f) t = 1f;
                engine.maintainStatusForPlayerShip(
                        DEBUFF_ID,
                        "graphics/icons/hullsys/fortress_shield.png",
                        "Vines",
                        "Mobility restricted and flux dissipation reduced, destroy all growths",
                        true
                );
            }

            List<Segment> neu = walk.advance(amount);
            for (Segment s : neu) links.add(new BakedLink(s.fromLocal, s.toLocal));

            updateDebuffs();
            draw(amount);

            if (!walk.growing) {
                holdAge += amount;
                if (holdAge >= HOLD_TIME) teardown(engine);
            }
        }

        private void stickAndPruneFlowers(CombatEngineAPI engine) {
            Iterator<Flower> it = flowers.iterator();
            while (it.hasNext()) {
                Flower f = it.next();
                if (f.dummy == null || !engine.isEntityInPlay(f.dummy)) {
                    it.remove();
                    continue;
                }
                Vector2f world = utilities.toWorld(ship, f.local);
                f.dummy.getLocation().set(world);
                f.dummy.getVelocity().set(ship.getVelocity());
                f.dummy.setFacing(ship.getFacing());
                f.dummy.setAngularVelocity(0f);
            }
        }

        private boolean anyFlowerAlive(CombatEngineAPI engine) {
            for (Flower f : flowers) {
                if (f.dummy != null && engine.isEntityInPlay(f.dummy)) return true;
            }
            return false;
        }

        private void draw(float amount) {
            //float fade = walk.growing ? 1f : Math.max(0f, 1f - holdAge / HOLD_TIME);
            //Color color = new Color(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(),
            //        Math.max(0, (int) (COLOR.getAlpha() * fade)));
            float shipFacing = ship.getFacing();

            for (BakedLink link : links) {
                Vector2f a = utilities.toWorld(ship, link.fromLocal);
                Vector2f b = utilities.toWorld(ship, link.toLocal);
                float length = MathUtils.getDistance(a, b);
                if (length < 1f) continue;

                MagicRender.battlespace(
                        Global.getSettings().getSprite(LINK_SPRITE),
                        MathUtils.getMidpoint(a, b),
                        new Vector2f(0f, 0f),
                        new Vector2f(THICKNESS, length + 2f),
                        new Vector2f(0f, 0f),
                        link.localAngle + shipFacing,
                        0f,
                        COLOR,
                        false,
                        0f,
                        amount,
                        0.1f
                );
            }

            for (Flower f : flowers) {
                if (f.dummy == null) continue;
                MagicRender.battlespace(
                        flowerSprite,
                        utilities.toWorld(ship, f.local),
                        new Vector2f(0f, 0f),
                        new Vector2f(20f, 20f),
                        new Vector2f(0f, 0f),
                        shipFacing + 90f,
                        0f,
                        COLOR,
                        false,
                        0f,
                        amount,
                        0.1f
                );
            }
        }

        private void updateDebuffs() {
            if (ship == null) return;
            float t = walk.coverage() / walk.p.coverFraction;
            if (t > 1f) t = 1f;

            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getWeaponTurnRateBonus().modifyMult(DEBUFF_ID, lerp(1f, WEAPON_TURN_MULT, t));
            stats.getBeamWeaponTurnRateBonus().modifyMult(DEBUFF_ID, lerp(1f, WEAPON_TURN_MULT, t));
            stats.getMaxSpeed().modifyMult(DEBUFF_ID, lerp(1f, SPEED_MULT, t));
            stats.getFluxDissipation().modifyMult(DEBUFF_ID, lerp(1f, DISSIPATION_MULT, t));
            stats.getFighterRefitTimeMult().modifyMult(DEBUFF_ID, lerp(1f, FIGHTER_REFIT_TIME_MULT, t));
            buffed = true;
        }

        private static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }

        private void clearBuffs() {
            if (!buffed || ship == null) return;
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getWeaponTurnRateBonus().unmodify(DEBUFF_ID);
            stats.getBeamWeaponTurnRateBonus().unmodify(DEBUFF_ID);
            stats.getMaxSpeed().unmodify(DEBUFF_ID);
            stats.getFluxDissipation().unmodify(DEBUFF_ID);
            stats.getFighterRefitTimeMult().unmodify(DEBUFF_ID);
            buffed = false;
        }

        private void teardown(CombatEngineAPI engine) {
            clearBuffs();
            for (Flower f : flowers) {
                if (f.dummy != null && engine.isEntityInPlay(f.dummy)) engine.removeEntity(f.dummy);
            }
            flowers.clear();
            if (ship != null) ship.getCustomData().remove(DEBUFF_ID);
            engine.removePlugin(this);
        }
    }
}