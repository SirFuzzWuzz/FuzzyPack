package fuzzypack.data.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lazywizard.lazylib.MathUtils;

public class dakkabox_onfire implements OnFireEffectPlugin {


	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(projectile);
        List<WeaponSpecAPI> weaponList = Global.getSettings().getAllWeaponSpecs();
        if (weaponList.isEmpty()) return;
        for (int i = 0; i<12; i++) {
            int r = MathUtils.getRandomNumberInRange(0, weaponList.size() - 1);
            WeaponSpecAPI weap = weaponList.get(r);
            System.out.println("Weapon: " + weap.getWeaponId());
            String weapName = weap.getWeaponId();
            weapName = weapName.toLowerCase();
            //Skip beams, lower missile rate, no firing dummies and avoid the blacklist
            if (weap.isBeam() ||
                    (weap.getType() == WeaponAPI.WeaponType.MISSILE && Math.random() < 0.8f) ||
                    projHasOnFire(weap) ||
                    weapName.contains("_copy") ||
                    weapName.contains("vayra_")) { // WHO USES A COMBAT PLUGIN FOR WEAPON EFFECTS
                i -= 1;
            } else {
                engine.spawnProjectile(
                        weapon.getShip(),
                        weapon,
                        weap.getWeaponId(),
                        weapon.getFirePoint(0),
                        projectile.getWeapon().getCurrAngle() + MathUtils.getRandomNumberInRange(-15, 15),
                        weapon.getShip().getVelocity());
            }

        }
	}

    static boolean projHasOnFire(WeaponSpecAPI weap) {
        if (!(weap instanceof ProjectileWeaponSpecAPI)) return false;

        Object raw = ((ProjectileWeaponSpecAPI) weap).getProjectileSpec();
        if (raw == null) return true; // no proj
        if (raw instanceof MissileSpecAPI) {
            return ((MissileSpecAPI) raw).getOnFireEffect() != null || ((MissileSpecAPI) raw).getOnHitEffect() !=null;
        }
        if (raw instanceof ProjectileSpecAPI) {
            return ((ProjectileSpecAPI) raw).getOnFireEffect() != null || ((ProjectileSpecAPI) raw).getOnFireEffect() !=null;
        }
        return true;
    }
}
