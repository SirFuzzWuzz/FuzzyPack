package fuzzypack.data.weapons.onHit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

public class fp_tracker_onhit implements OnHitEffectPlugin {
    private String key;

    //private IntervalUtil interval = new IntervalUtil(0.5f,0.5f);

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        key = "tracker" + projectile.getWeapon().getSlot().getId();
        if (!shieldHit && target instanceof ShipAPI) {
            if (!target.getCustomData().containsKey(key)) {
                target.setCustomData(key, 1);
            } else  {
                target.setCustomData(key, (int) target.getCustomData().get(key) + 1);
            }

            Vector2f missileSpawnLoc = MathUtils.getPointOnCircumference(projectile.getWeapon().getLocation(), 8f, projectile.getWeapon().getCurrAngle() - 70);

            if ((int) target.getCustomData().get(key) >= 4){
                GuidedMissileAI missileAI = (GuidedMissileAI) engine.spawnProjectile(projectile.getSource(), projectile.getWeapon(), "fp_tracker_missile", missileSpawnLoc,
                        projectile.getWeapon().getCurrAngle(), projectile.getSource().getVelocity()).getAI();
                Global.getSoundPlayer().playSound("atropos_fire", 1.1f, 0.9f, missileSpawnLoc, projectile.getSource().getVelocity());

                MagicRender.battlespace(Global.getSettings().getSprite("glow","missile_flash"),
                        missileSpawnLoc,
                        projectile.getSource().getVelocity(),
                        new Vector2f(6,20),
                        new Vector2f(0,0),
                        projectile.getWeapon().getCurrAngle(),
                        0,
                        new Color(220,160,50,240),
                        true,
                        0.01f, 0.1f, 0.07f);

                missileAI.setTarget(target);
                target.getCustomData().remove(key);
            }
        }
    }

}
