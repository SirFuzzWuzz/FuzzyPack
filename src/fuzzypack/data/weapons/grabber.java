package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import fuzzypack.data.weapons.tools.RopeChain;
import fuzzypack.data.weapons.tools.utilities;
import fuzzypack.data.weapons.tools.utilities.ModuleHit;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class grabber implements OnFireEffectPlugin {

    private static final float LINK_SPACING = 8f;
    private static final float LINK_THICKNESS = 4f;
    private static final int MIN_LINKS = 6;
    private static final int MAX_LINKS = 40;
    private static final float GRAB_RADIUS = 25f;
    private static final float REEL_SPEED = 400f;

    private static final String SPRITE_CAT = "projectiles";
    private static final String LINK_SPRITE = "grabber_chain";
    private static final String HOOK_SPRITE = "grabber_missile_fighter";
    private static final String FUEL_SPRITE = "fuel_missile";

    private enum Grab {
        MISSILE(2f),
        ENGINE(3f),
        SMALL_WEAPON(5f);
        // MEDIUM_WEAPON(5f),
        // LARGE_WEAPON(5f);

        final float yankTime;
        Grab(float yankTime) { this.yankTime = yankTime; }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (weapon.getShip() == null || projectile == null) return;
        weapon.getShip().addListener(new flightListener(projectile, weapon));
    }

    private static Grab classify(ModuleHit hit) {
        if (hit == null) return null;
        if (hit.weapon != null) {
            if (hit.weapon.getType() == WeaponType.MISSILE) {
                if (hit.weapon.getAmmo() > 0) return Grab.MISSILE;
            }
            if (hit.ship.isFighter()) return null;
            if (hit.weapon.getSize() == WeaponSize.SMALL) return Grab.SMALL_WEAPON;
            // if (hit.weapon.getSize() == WeaponSize.MEDIUM) return Grab.MEDIUM_WEAPON;
            // if (hit.weapon.getSize() == WeaponSize.LARGE) return Grab.LARGE_WEAPON;
            return null;
        }
        if (hit.engine != null) return Grab.ENGINE;
        return null;
    }

    class flightListener implements AdvanceableListener {
        final DamagingProjectileAPI proj;
        final WeaponAPI weap;
        final ShipAPI host;
        final Vector2f tip = new Vector2f();
        boolean done, retracting;

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
                if (proj == null || !engine.isEntityInPlay(proj) || proj.didDamage() || proj.isFading()) {
                    finish();
                    return;
                }
                tip.set(proj.getLocation());
                float dist = MathUtils.getDistance(start, tip);
                if (dist < 1f) return;

                ModuleHit raw = utilities.findNearestEnemyModule(host, tip, GRAB_RADIUS, true, true);
                Grab type = classify(raw);
                if (type != null && utilities.tryGrab(utilities.moduleOf(raw))) {
                    engine.removeEntity(proj);
                    host.addListener(new grabListener(weap, type, raw, tip));
                    finish();
                    return;
                }
                if (dist >= weap.getRange()) {
                    engine.removeEntity(proj);
                    retracting = true;
                }
            }

            if (retracting && RopeChain.reelToward(start, tip, weap.getProjectileSpeed() * 0.5f, amount)) {
                finish();
                return;
            }

            RopeChain.renderStraight(start, tip, LINK_SPACING, MIN_LINKS, MAX_LINKS,
                    SPRITE_CAT, LINK_SPRITE, LINK_THICKNESS, amount);
            drawSprite(HOOK_SPRITE, tip, VectorUtils.getAngle(start, tip) - 90f, amount);
        }

        private void finish() {
            done = true;
            host.removeListener(this);
        }
    }

    class grabListener implements AdvanceableListener {
        final ShipAPI target, host;
        final WeaponAPI weap, grabbedWpn;
        final ShipEngineAPI grabbedEng;
        final Grab type;
        final SpriteAPI loot;
        final Vector2f lootPos = new Vector2f();
        final Vector2f hookPos = new Vector2f();
        final CombatEngineAPI engine = Global.getCombatEngine();
        float yankTimer;
        boolean snapped = false;

        grabListener(WeaponAPI weap, Grab type, ModuleHit hit, Vector2f point) {
            this.weap = weap;
            this.host = weap.getShip();
            this.target = hit.ship;
            this.type = type;
            this.grabbedWpn = hit.weapon;
            this.grabbedEng = hit.engine;
            this.hookPos.set(point);
            this.lootPos.set(point);
            this.loot = lootSprite();
            this.yankTimer = type.yankTime;
        }

        @Override
        public void advance(float amount) {
            if (host == null || !host.isAlive() || target == null || !target.isAlive()) {
                finish();
                return;
            }
            weap.setRemainingCooldownTo(weap.getRefireDelay());
            Vector2f start = weap.getFirePoint(0);
            stickToModule();

            if (yankTimer > 0f) {
                // No more infinite range yoinks
                if (MathUtils.getDistance(start, hookPos) > weap.getRange()) {
                    yankTimer = 0f;
                    // lootPos.set(hookPos);
                    snapped = true;
                    drawCable(start, hookPos, amount);
                    return;
                }
                yankTimer -= amount;
                if (yankTimer <= 0f) {
                    lootPos.set(hookPos);
                    applyGrab();
                }
                drawCable(start, hookPos, amount);
                if (grabbedWpn != null) {
                    Vector2f p = MathUtils.getRandomPointInCircle(grabbedWpn.getLocation(), 10f);
                    engine.addHitParticle(p, target.getVelocity(),
                            5f + (float) Math.random() * 10f, 1f, 0.08f,
                            new Color(150, 50, 50, 200));
                }
                return;
            }

            if (RopeChain.reelToward(start, lootPos, REEL_SPEED, amount)) {
                if (type == Grab.MISSILE && grabbedWpn != null) {
                    engine.spawnProjectile(host, weap, grabbedWpn.getId(),
                            start, weap.getCurrAngle(), host.getVelocity());
                }
                finish();
                return;
            }
            drawCable(start, lootPos, amount);
            if (!snapped) drawLoot(start, amount);
        }

        private Object module() {
            return grabbedWpn != null ? grabbedWpn : grabbedEng;
        }

        private void finish() {
            utilities.releaseGrab(module());
            if (host != null) host.removeListener(this);
        }

        private void stickToModule() {
            if (grabbedWpn != null) hookPos.set(grabbedWpn.getLocation());
            else if (grabbedEng != null) hookPos.set(grabbedEng.getLocation());
        }

        private void applyGrab() {
            switch (type) {
                case MISSILE:
                    if (grabbedWpn != null && grabbedWpn.usesAmmo() && grabbedWpn.getAmmo() > 0) {
                        grabbedWpn.setAmmo(grabbedWpn.getAmmo() - 1);
                    }
                    break;
                case ENGINE:
                    if (grabbedEng != null && !grabbedEng.isDisabled()) grabbedEng.disable();
                    break;
                case SMALL_WEAPON: // Should just have it be WEAPON and multiply time based on size
                // case MEDIUM_WEAPON:
                // case LARGE_WEAPON:
                    if (grabbedWpn != null && !grabbedWpn.isDisabled()) {
                        grabbedWpn.disable(true);
                        target.addListener(new utilities.hideWeaponListener(grabbedWpn));
                    }
                    break;
            }
        }

        private void drawCable(Vector2f start, Vector2f end, float amount) {
            RopeChain.renderStraight(start, end, LINK_SPACING, MIN_LINKS, MAX_LINKS,
                    SPRITE_CAT, LINK_SPRITE, LINK_THICKNESS, amount);
            drawSprite(HOOK_SPRITE, end, VectorUtils.getAngle(start, end) - 90f, amount);
        }

        private SpriteAPI lootSprite() {
            if (type == Grab.MISSILE) return missileSprite(grabbedWpn);
            if (type == Grab.ENGINE) return Global.getSettings().getSprite(SPRITE_CAT, FUEL_SPRITE);
            if (grabbedWpn != null) {
                String name = grabbedWpn.getSlot().isHardpoint()
                        ? grabbedWpn.getSpec().getHardpointSpriteName()
                        : grabbedWpn.getSpec().getTurretSpriteName();
                if (name != null && !name.isEmpty()) return Global.getSettings().getSprite(name);
            }
            return Global.getSettings().getSprite(SPRITE_CAT, HOOK_SPRITE);
        }

        private SpriteAPI missileSprite(WeaponAPI wpn) {
            if (wpn == null) return Global.getSettings().getSprite(SPRITE_CAT, HOOK_SPRITE);
            try {
                if (wpn.getSpec() instanceof ProjectileWeaponSpecAPI) {
                    Object spec = ((ProjectileWeaponSpecAPI) wpn.getSpec()).getProjectileSpec();
                    if (spec instanceof MissileSpecAPI) {
                        String name = ((MissileSpecAPI) spec).getHullSpec().getSpriteName();
                        if (name != null && !name.isEmpty()) return Global.getSettings().getSprite(name);
                    }
                }
                if (wpn.getMissileRenderData() != null && !wpn.getMissileRenderData().isEmpty()) {
                    SpriteAPI s = wpn.getMissileRenderData().get(0).getSprite();
                    if (s != null) return s;
                }
            } catch (Exception ignored) {}
            return wpn.getSprite() != null ? wpn.getSprite()
                    : Global.getSettings().getSprite(SPRITE_CAT, HOOK_SPRITE);
        }

        private void drawLoot(Vector2f start, float amount) {
            if (loot == null) return;
            loot.setAngle(0f);
            MagicRender.battlespace(loot, lootPos, new Vector2f(),
                    new Vector2f(loot.getWidth(), loot.getHeight()), new Vector2f(),
                    VectorUtils.getAngle(start, lootPos) - 90f,
                    0f, Color.WHITE, false, 0f, amount, 0f);
        }
    }

    private static void drawSprite(String key, Vector2f at, float angle, float amount) {
        SpriteAPI spr = Global.getSettings().getSprite(SPRITE_CAT, key);
        MagicRender.battlespace(spr, at, new Vector2f(),
                new Vector2f(spr.getWidth(), spr.getHeight()), new Vector2f(),
                angle, 0f, Color.WHITE, false, 0f, amount, 0f);
    }
}