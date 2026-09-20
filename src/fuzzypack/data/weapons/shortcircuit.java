package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class shortcircuit implements OnHitEffectPlugin {

    private static final float DURATION = 7f;
    private static final float VARIANCE = 1f;
    private static final String STAT_ID = "shield_short";

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {
        if (!shieldHit || !(target instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) target;
        if (ship.getShield() == null) return;

        for (lockListener existing : ship.getListeners(lockListener.class)) {
            existing.refresh();
            return;
        }
        ship.addListener(new lockListener(ship, point));
    }

    static class lockListener implements AdvanceableListener {
        final ShipAPI target;
        final IntervalUtil interval = new IntervalUtil(DURATION - VARIANCE, DURATION + VARIANCE);
        boolean applied;
        final float impactDist;
        final float impactOffset;

        lockListener(ShipAPI target, Vector2f point) {
            this.target = target;
            this.impactDist = MathUtils.getDistance(point, target.getLocation());
            ShieldAPI sh = target.getShield();
            this.impactOffset = VectorUtils.getAngle(sh.getLocation(), point) - sh.getFacing();
            applyLock();
            target.getFluxTracker().showOverloadFloatyIfNeeded("Shield Locked!", Color.WHITE, 4f, true);
        }

        void refresh() {
            interval.setInterval(DURATION - VARIANCE, DURATION + VARIANCE);
            interval.setElapsed(0f);
            applyLock();
            target.getFluxTracker().showOverloadFloatyIfNeeded("Shield Locked!", Color.RED, 5f, true);
        }

        private void applyLock() {
            if (target.getShield() == null) return;
            target.getShield().toggleOn();
            if (!applied && target.getShield().getFluxPerPointOfDamage() < 1f) {
                target.getMutableStats().getShieldAbsorptionMult().modifyMult(STAT_ID, 0.001f);
                applied = true;
            }
        }

        private void clearLock() {
            target.getMutableStats().getShieldAbsorptionMult().unmodify(STAT_ID);
            applied = false;
            target.getFluxTracker().showOverloadFloatyIfNeeded("Shield OK!", Color.BLUE, 5f, true);
            target.removeListener(this);
        }

        @Override
        public void advance(float amount) {
            if (Global.getCombatEngine().isPaused()) return;
            if (target == null || !target.isAlive() || !Global.getCombatEngine().isEntityInPlay(target)
                    || target.getShield() == null) {
                if (target != null) {
                    target.getMutableStats().getShieldAbsorptionMult().unmodify(STAT_ID);
                    target.removeListener(this);
                }
                return;
            }

            target.getShield().toggleOn();
            target.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
            target.blockCommandForOneFrame(ShipCommand.VENT_FLUX);

            ShieldAPI shield = target.getShield();
            Vector2f pin = MathUtils.getPointOnCircumference(
                    shield.getLocation(),
                    shield.getRadius(),
                    shield.getFacing() + impactOffset);

            float pinAngle = shield.getFacing() + impactOffset;

            Color c = shield.getInnerColor();
            if (c == null) c = Color.CYAN;

            SpriteAPI sprote = Global.getSettings().getSprite("projectiles", "shortcircuit_missile");
            sprote.setAngle(0f);
            MagicRender.battlespace(
                    sprote, pin, target.getVelocity(),
                    new Vector2f(10, 23f), new Vector2f(),
                    target.getShield().getFacing() + impactOffset + 90,
                    0f, Color.WHITE, false, 0f, amount, 0f);

            // Visuals
            if (Math.random() < 0.4f) {
                Vector2f spark = MathUtils.getRandomPointInCircle(pin, 16f);
                Global.getCombatEngine().addHitParticle(
                        spark, target.getVelocity(),
                        20f + (float) Math.random() * 10f,
                        1f, 0.12f, c);
            }
            if (Math.random() < 0.06f) {
                float jitter = (float) (Math.random() * 40f - 20f);
                Vector2f rim = MathUtils.getPointOnCircumference(
                        shield.getLocation(),
                        shield.getRadius(),
                        pinAngle + jitter);
                EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
                        pin, target,
                        rim, target,
                        6f,
                        c,
                        Color.WHITE);
                arc.setSingleFlickerMode();
                arc.setRenderGlowAtStart(false);
                //arc.setRenderGlowAtEnd(false);
            }

            interval.advance(amount);
            if (interval.intervalElapsed()) {
                clearLock();
            }
        }
    }
}