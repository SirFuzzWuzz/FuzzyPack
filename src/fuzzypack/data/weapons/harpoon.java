package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import fuzzypack.data.weapons.tools.RopeChain;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.ArrayList;

public class harpoon implements OnHitEffectPlugin, OnFireEffectPlugin {

    private static final float LINK_SPACING = 8f;
    private static final float LINK_THICKNESS = 6f;
    private static final int MIN_LINKS = 6;
    private static final int MAX_LINKS = 100;
    private static final int SOLVER_ITERS = 8;
    private static final float DAMPING = 0.95f;
    private static final float MOMENT_SPIN = 75f;
    private static final float STIFFNESS = 500f;
    private static final float DURATION = 30f;
    private static final float BREAK_FORCE = 0f; // 0 means can't break
    private static final float SNAP_TIME = 5f;
    private static final float MAX_REEL_SPEED = 100f;
    private static final float MIN_LENGTH = 300f;   // stop reeling at
    private static final String SPRITE_CAT = "projectiles";
    private static final String LINK_SPRITE = "harpoon_chain";
    private static final String HOOK_SPRITE = "fp_harpoon_missile";
    private static final String LINK_DUMMY_WEAPON = "fp_dummy_missile";
    private static final int NODE_STEP = 8; // Dummy missile placed every x links

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (weapon.getShip() == null || projectile == null) return;
        weapon.getShip().addListener(new flightListener(projectile, weapon, false, null));
    }

    class flightListener implements AdvanceableListener {
        final DamagingProjectileAPI proj;
        final WeaponAPI weap;
        final ShipAPI host;
        boolean done;
        boolean retracting;
        Vector2f tip = new Vector2f();

        flightListener(DamagingProjectileAPI proj, WeaponAPI weap, boolean retracting, Vector2f tip) {
            this.proj = proj;
            this.weap = weap;
            this.host = weap.getShip();
            this.retracting = retracting;
            if (tip != null) this.tip = tip;
        }

        @Override
        public void advance(float amount) {
            if (host == null || !host.isAlive()) {
                if (host != null) host.removeListener(this);
                return;
            }
            weap.setRemainingCooldownTo(weap.getRefireDelay());
            CombatEngineAPI engine = Global.getCombatEngine();
            Vector2f start = weap.getFirePoint(0);

            if (!retracting) {
                if (proj == null || proj.didDamage()) {
                    done = true;
                    weap.setRemainingCooldownTo(weap.getRefireDelay() * 0.5f);
                    host.removeListener(this);
                    return;
                }
                tip.set(proj.getLocation());
                float dist = MathUtils.getDistance(start, tip);
                if (dist < 1f) {
                    weap.setRemainingCooldownTo(weap.getRefireDelay() * 0.5f);
                    return;
                }

                if (dist >= weap.getRange()) {
                    engine.removeEntity(proj);
                    retracting = true;
                }
            }

            RopeChain.renderStraight(start, tip, LINK_SPACING, MIN_LINKS, MAX_LINKS,
                    SPRITE_CAT, LINK_SPRITE, LINK_THICKNESS, amount);
            if (retracting) {
                if (RopeChain.reelToward(start, tip, weap.getProjectileSpeed() * 0.5f, amount)) {
                    done = true;
                    weap.setRemainingCooldownTo(weap.getRefireDelay() * 0.5f);
                    host.removeListener(this);
                    return;
                }
            }

            if (retracting) {
                SpriteAPI sprote = Global.getSettings().getSprite(SPRITE_CAT, HOOK_SPRITE);
                MagicRender.battlespace(
                        sprote,
                        tip, new Vector2f(),
                        new Vector2f(sprote.getWidth(), sprote.getHeight()), new Vector2f(),
                        VectorUtils.getAngle(start, tip) -90,
                        0f, Color.WHITE, false, 0f, amount, 0f);
            }
        }
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI)) return;
        if (shieldHit) {
            projectile.getWeapon().getShip().addListener(
                    new flightListener(projectile, projectile.getWeapon(), true, point));
            return;
        }
        if (projectile.getWeapon() == null || projectile.getWeapon().getShip() == null) return;
        float hitDist = MathUtils.getDistance(projectile.getWeapon().getFirePoint(0), point);
        if (hitDist >= projectile.getWeapon().getRange() || hitDist < 10f) return;

        projectile.getWeapon().getShip().addListener(
                new attachedListener(projectile, (ShipAPI) target, point, hitDist));
    }

    class attachedListener implements AdvanceableListener {
        final ShipAPI target;
        final ShipAPI host;
        final WeaponAPI weap;
        final IntervalUtil life = new IntervalUtil(DURATION, DURATION);
        final float impactDist;
        final float impactOffset;
        final float hookFacingOffset;
        final RopeChain chain;
        final float hookBack;
        float forceCounter = 0f;
        // final ArrayList<MissileAPI> nodes = new ArrayList<>();
        final float segment_hitpoints;

        attachedListener(DamagingProjectileAPI proj, ShipAPI target, Vector2f point, float hitDist) {
            this.target = target;
            this.host = proj.getWeapon().getShip();
            this.weap = proj.getWeapon();
            this.impactDist = MathUtils.getDistance(point, target.getLocation());
            this.impactOffset = VectorUtils.getAngle(target.getLocation(), point) - target.getFacing();
            this.hookFacingOffset = proj.getFacing() - target.getFacing();
            SpriteAPI hookSpr = Global.getSettings().getSprite(SPRITE_CAT, HOOK_SPRITE);
            this.hookBack = hookSpr.getHeight() * 0.5f;
            this.chain = new RopeChain(
                    weap.getFirePoint(0), point,
                    LINK_SPACING, MIN_LINKS, MAX_LINKS,
                    SOLVER_ITERS, DAMPING, SPRITE_CAT, LINK_SPRITE, LINK_THICKNESS);
            this.segment_hitpoints = proj.getHitpoints();
            spawnNodes();
        }

        @Override
        public void advance(float amount) {
            if (host == null || target == null || !host.isAlive() || !target.isAlive() || weap.isDisabled()) {
                if (host != null) host.removeListener(this);
                chainRemoved(chain);
                clearNodes();
                return;
            }

            life.advance(amount);
            if (life.intervalElapsed()) {
                clearNodes();
                chainRemoved(chain);
                host.removeListener(this);
                return;
            }

            Vector2f start = weap.getFirePoint(0);
            Vector2f end = MathUtils.getPointOnCircumference(
                    target.getLocation(), impactDist, target.getFacing() + impactOffset);

            float hookFacing = target.getFacing() + hookFacingOffset;
            Vector2f chainEnd = MathUtils.getPointOnCircumference(end, hookBack, hookFacing + 180f);

            // Adjust reel speed based on tension
            float tension = chain.getTension(start, chainEnd, STIFFNESS);
            float speed = Math.max(10f, MAX_REEL_SPEED / (1f + tension / 100000f));
            // Global.getCombatEngine().addFloatingText(chain.pos[0], "Reel speed: " + speed, 16f, Color.YELLOW, host, 0f, 0f);

            if (chain.restLength > MIN_LENGTH) {
                chain.setRestLength(chain.restLength - speed * amount);
                chain.fitLinks(start, chainEnd, LINK_SPACING, MIN_LINKS, MAX_LINKS);
                chain.linkLen = chain.restLength / Math.max(chain.links - 1, 1);
            }

            chain.pinEnds(start, chainEnd);
            chain.simulate();
            chain.applyPull(host, target, start, chainEnd, STIFFNESS, MOMENT_SPIN, amount);

            // For dummy missiles to be shot at
            if (updateNodes()) {
                host.removeListener(this);
                chainRemoved(chain);
                return;
            }

            // Circles of debugging
            for (ChainNode m : nodes) {
                Vector2f p = m.missile.getLocation();
                Global.getCombatEngine().addHitParticle(p, new Vector2f(),
                        LINK_THICKNESS + 6f, 2f, amount, new Color(255,180,100,25));
                /* SpriteAPI dot = Global.getSettings().getSprite("markers", "filled_circle");
                MagicRender.battlespace(
                        dot,
                        p,
                        target.getVelocity(),
                        new Vector2f(10f, 10f),
                        new Vector2f(),
                        0f,
                        0f,
                        new Color(255, 50, 50, 200),
                        false,
                        0f,
                        amount,
                        0f
                ); */
            }

            // Break if force is too high for too long
            if (BREAK_FORCE > 0f && chain.getTension(start, chainEnd, STIFFNESS) >= BREAK_FORCE) {
                forceCounter += amount;
                if (forceCounter > SNAP_TIME) {
                    clearNodes();
                    host.removeListener(this);
                    return;
                }
            } else {
                forceCounter = 0f;
            }

            weap.setRemainingCooldownTo(weap.getRefireDelay());
            chain.render(amount);

            // Hook sprite on target
            SpriteAPI sprote = Global.getSettings().getSprite(SPRITE_CAT, "fp_harpoon_missile_attached");
            MagicRender.battlespace(
                    sprote,
                    end, target.getVelocity(),
                    new Vector2f(sprote.getWidth(), sprote.getHeight()), new Vector2f(),
                    hookFacing - 90f,
                    0f, Color.WHITE, false, 0f, amount, 0f);
        }

        private void chainRemoved(RopeChain chain) {
            Global.getSoundPlayer().playSound("hit_heavy_energy", 1f, 0.8f, weap.getLocation(), host.getVelocity());
            weap.setRefireDelay(weap.getRefireDelay());
            for (Vector2f p : chain.pos) {
                Global.getCombatEngine().spawnExplosion(p, target.getVelocity(),
                        new Color(255,180,100,25), LINK_SPACING*2f, 0.2f);
            }
        }

        // Missiles to give chain hp
        class ChainNode {
            final MissileAPI missile;
            final int fromHook;
            ChainNode(MissileAPI missile, int fromHook) {
                this.missile = missile;
                this.fromHook = fromHook;
            }
        }

        final ArrayList<ChainNode> nodes = new ArrayList<>();

        private void spawnNodes() {
            CombatEngineAPI engine = Global.getCombatEngine();
            nodes.clear();
            for (int back = 2; back < chain.links - 2; back += NODE_STEP) {
                int idx = chain.links - 1 - back;
                if (idx < 2) break;
                MissileAPI m = (MissileAPI) engine.spawnProjectile(
                        host, weap, LINK_DUMMY_WEAPON,
                        new Vector2f(chain.pos[idx]), 0f, new Vector2f());
                m.setCollisionClass(CollisionClass.FIGHTER);
                m.setHitpoints(segment_hitpoints);

                m.getVelocity().set(0f, 0f);
                nodes.add(new ChainNode(m, back));
            }
        }

        private boolean updateNodes() {
            CombatEngineAPI engine = Global.getCombatEngine();
            for (int n = nodes.size() - 1; n >= 0; n--) {
                ChainNode node = nodes.get(n);
                MissileAPI m = node.missile;
                int idx = chain.links - 1 - node.fromHook;

                if (idx < 2 || idx > chain.links - 2) {
                    if (m != null && engine.isEntityInPlay(m)) engine.removeEntity(m);
                    nodes.remove(n);
                    continue;
                }

                if (m == null || !engine.isEntityInPlay(m) || m.isFading()) {
                    clearNodes();
                    return true;
                }

                m.getLocation().set(chain.pos[idx]);
                m.getVelocity().set(0f, 0f);
            }
            return false;
        }

        private void clearNodes() {
            CombatEngineAPI engine = Global.getCombatEngine();
            for (ChainNode node : nodes) {
                if (node.missile != null && engine.isEntityInPlay(node.missile)) {
                    engine.removeEntity(node.missile);
                }
            }
            nodes.clear();
        }
    }

}