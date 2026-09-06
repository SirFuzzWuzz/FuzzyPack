package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import fuzzypack.data.weapons.tools.RopeChain;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import java.awt.Color;

public class tether implements OnHitEffectPlugin, OnFireEffectPlugin {

    private static final float LINK_SPACING = 5f;
    private static final float LINK_THICKNESS = 8f;
    private static final int MIN_LINKS = 6;
    private static final int MAX_LINKS = 80;
    private static final int SOLVER_ITERS = 8;
    private static final float DAMPING = 0.95f;
    private static final float MOMENT_SPIN = 100f;
    private static final float STIFFNESS = 900f;
    private static final float DURATION = 15f;
    private static final float BREAK_FORCE = 30000; // 0 = never breaks
    private static final float SNAP_TIME = 1f; // sec need at max tension to snap chain
    private static final String SPRITE_CAT = "projectiles";
    private static final String LINK_SPRITE = "taser_chain";
    private static final String HOOK_SPRITE = "tether_missile";

    //Taser effect
    private static final float CHARGE_TIME = 3f;
    private static final float PULSE_TIME = 0.25f;
    private static final float ZAPS_PER_SEC = 3f;


    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (weapon.getShip() == null || projectile == null) return;
        weapon.getShip().addListener(new flightListener(projectile, weapon));
    }

    class flightListener implements AdvanceableListener {
        final DamagingProjectileAPI proj;
        final WeaponAPI weap;
        final ShipAPI host;
        boolean done;
        boolean retracting;
        final Vector2f tip = new Vector2f();

        flightListener(DamagingProjectileAPI proj, WeaponAPI weap) {
            this.proj = proj;
            this.weap = weap;
            this.host = weap.getShip();
        }

        @Override
        public void advance(float amount) {
            if (done || host == null || !host.isAlive()) {
                if (host != null) host.removeListener(this);
                return;
            }

            CombatEngineAPI engine = Global.getCombatEngine();
            Vector2f start = weap.getFirePoint(0);

            if (!retracting) {
                if (proj == null || proj.didDamage()) {
                    done = true;
                    host.removeListener(this);
                    return;
                }
                tip.set(proj.getLocation());
                float dist = MathUtils.getDistance(start, tip);
                if (dist < 1f) return;

                if (dist >= weap.getRange()) {
                    engine.removeEntity(proj);
                    weap.setAmmo(1);
                    retracting = true;
                }
            }

            if (retracting) {
                if (RopeChain.reelToward(start, tip, weap.getProjectileSpeed(), amount)) {
                    done = true;
                    host.removeListener(this);
                    return;
                }
            }

            RopeChain.renderStraight(start, tip, LINK_SPACING, MIN_LINKS, MAX_LINKS,
                    SPRITE_CAT, LINK_SPRITE, LINK_THICKNESS, amount);

            float linkAngle = VectorUtils.getAngle(start, tip) - 90f;
            MagicRender.battlespace(
                    Global.getSettings().getSprite("projectiles", "tether_missile"),
                    tip,
                    new Vector2f(),
                    new Vector2f(12f, 20f),
                    new Vector2f(),
                    linkAngle,
                    0f,
                    Color.WHITE,
                    false,
                    0f,
                    amount,
                    0f
            );
        }
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI) || shieldHit) return;
        if (projectile.getWeapon() == null || projectile.getWeapon().getShip() == null) return;
        float maxDist = projectile.getWeapon().getRange();
        float hitDist = MathUtils.getDistance(projectile.getWeapon().getFirePoint(0), point);
        if (hitDist >= maxDist || hitDist < 10f) return;

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
        float forceCounter = 0f;
        boolean primed = false;
        float charge = 0f;
        float pulse = -1f;
        final IntervalUtil zap_interval = new IntervalUtil(1f / ZAPS_PER_SEC, 1f / ZAPS_PER_SEC);

        attachedListener(DamagingProjectileAPI proj, ShipAPI target, Vector2f point, float hitDist) {
            this.target = target;
            this.host = proj.getWeapon().getShip();
            this.weap = proj.getWeapon();
            this.impactDist = MathUtils.getDistance(point, target.getLocation());
            this.impactOffset = VectorUtils.getAngle(target.getLocation(), point) - target.getFacing();
            this.hookFacingOffset = proj.getFacing() - target.getFacing();
            this.chain = new RopeChain(
                    weap.getFirePoint(0), point,
                    LINK_SPACING, MIN_LINKS, MAX_LINKS,
                    SOLVER_ITERS, DAMPING, SPRITE_CAT, LINK_SPRITE, LINK_THICKNESS);
            chain.setMaxLength(weap.getRange());
        }

        @Override
        public void advance(float amount) {
            if (host == null || target == null || !host.isAlive() || !target.isAlive() || weap.isDisabled()) {
                if (host != null) host.removeListener(this);
                return;
            }
            weap.setRemainingCooldownTo(weap.getRefireDelay());
            life.advance(amount);
            if (life.intervalElapsed()) {
                chainRemoved(chain);
                host.removeListener(this);
                return;
            }
            // Block firing while it's attached
            weap.setRemainingCooldownTo(weap.getRefireDelay());

            Vector2f start = weap.getFirePoint(0);
            Vector2f end = MathUtils.getPointOnCircumference(
                    target.getLocation(), impactDist, target.getFacing() + impactOffset);

            // Pull from rear of hook instead of the hitpoint, good if long hook
            float hookFacing = target.getFacing() + hookFacingOffset;
            Vector2f chainEnd = MathUtils.getPointOnCircumference(end, 10f, hookFacing + 180f);

            // Magic
            chain.fitLinks(start, chainEnd, LINK_SPACING, MIN_LINKS, MAX_LINKS);
            chain.pinEnds(start, chainEnd);
            chain.simulate();
            chain.applyPull(host, target, start, chainEnd, STIFFNESS, MOMENT_SPIN, amount);

            // Global.getCombatEngine().addFloatingText(weap.getLocation(),
            //        "FORCE: "+ chain.getTension(start,end,STIFFNESS),50f,Color.GREEN, weap.getShip(),0f,0f);
            if (BREAK_FORCE > 0f && chain.getTension(start, chainEnd, STIFFNESS) >= BREAK_FORCE) {
                forceCounter += amount;
                // SNAP
                if (forceCounter > SNAP_TIME) {
                    chainRemoved(chain);
                    host.removeListener(this);
                    return;
                }
            } else if(chain.getTension(start, chainEnd, STIFFNESS) < BREAK_FORCE) {
                forceCounter = 0;
            }

            // For taser stuff
            if (!primed) {
                charge += amount;
                if (charge >= CHARGE_TIME) primed = true;
            } else if (pulse < 0f) {
                zap_interval.advance(amount);
                if (zap_interval.intervalElapsed()) pulse = 0f;
            } else {
                pulse += amount / PULSE_TIME;
                drawPulse(chain, pulse, amount);
                Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1f, 0.6f, start, target.getVelocity());
                if (pulse >= 1f) {
                    applyShock(end);
                    pulse = -1f;
                }
            }
            // Circles of debugging
            //Global.getCombatEngine().addFloatingText(chain.pos[0], "0", 16f, Color.YELLOW, host, 0f, 0f);
            //Global.getCombatEngine().addFloatingText(chain.pos[chain.links - 1], "N", 16f, Color.YELLOW, target, 0f, 0f);
            /* for (Vector2f p : chain.pos) {
                SpriteAPI dot = Global.getSettings().getSprite("markers", "circle");
                MagicRender.battlespace(
                        dot,
                        p,
                        target.getVelocity(),
                        new Vector2f(12f, 20f),
                        new Vector2f(),
                        0f,
                        0f,
                        new Color(255, 255, 255, 255),
                        false,
                        0f,
                        amount,
                        0f
                );
            } */

            // Render actual chain, only visual
            chain.render(amount);
            // To render the missile stuck in the target
            MagicRender.battlespace(
                    Global.getSettings().getSprite("projectiles", HOOK_SPRITE),
                    end,
                    target.getVelocity(),
                    new Vector2f(12f, 20f),
                    new Vector2f(),
                    target.getFacing() + hookFacingOffset - 90,
                    0f,
                    new Color(255, 255, 255, 255),
                    false,
                    0f,
                    amount,
                    0f
            );
        }

        private void chainRemoved(RopeChain chain) {
            Global.getSoundPlayer().playSound("hit_heavy_energy", 1f, 0.8f, weap.getLocation(), host.getVelocity());
            weap.setRefireDelay(weap.getRefireDelay());
            for (Vector2f p : chain.pos) {
                Global.getCombatEngine().spawnExplosion(p, target.getVelocity(),
                        new Color(80, 160, 255, 180), LINK_SPACING*4f, 0.2f);
            }
        }

        private void drawPulse(RopeChain chain, float t, float amount) {
            float f = Math.max(0f, Math.min(0.999f, t)) * (chain.links - 1);
            int i = (int) f;
            float local = f - i;
            Vector2f a = chain.pos[i];
            Vector2f b = chain.pos[Math.min(i + 1, chain.links - 1)];
            Vector2f p = new Vector2f(a.x + (b.x - a.x) * local, a.y + (b.y - a.y) * local);
            Global.getCombatEngine().addHitParticle(p, new Vector2f(),
                    LINK_THICKNESS + 6f, 2f, 0.1f, new Color(150, 210, 255, 200));
        }

        private void applyShock(Vector2f hit) {
            CombatEngineAPI engine = Global.getCombatEngine();
            engine.applyDamage(
                    target, hit,
                    weap.getDamage().getDamage(), DamageType.ENERGY,
                    weap.getDerivedStats().getEmpPerShot(),
                    true, false, host);
            for (int i = 0; i < 3; i++) {
                engine.spawnEmpArcPierceShields(
                        host, hit, target,
                        target, DamageType.ENERGY,
                        0f, weap.getDerivedStats().getEmpPerShot(),
                        1000f, null, 12f,
                        new Color(100, 180, 255),
                        Color.WHITE);
                /*Vector2f dest = MathUtils.getRandomPointInCircle(target.getLocation(),
                        target.getCollisionRadius() * 0.8f);
                EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
                        hit, target, dest, target,
                        10f,
                        new Color(100, 180, 255, 160),
                        Color.WHITE);
                arc.setSingleFlickerMode();*/
            }
            // visuals
            engine.addHitParticle(hit, target.getVelocity(), 80f, 1f, 0.15f, Color.WHITE);
            engine.addSmoothParticle(hit, target.getVelocity(), 120f, 0.8f, 0.25f,
                    new Color(150, 210, 255, 200));
            Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 0.9f, 1f, hit, target.getVelocity());
        }
    }
}