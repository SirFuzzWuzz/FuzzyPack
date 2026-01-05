package fuzzypack.data.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lazywizard.lazylib.MathUtils;

public class dakkabox_onfire implements OnFireEffectPlugin {
    public static final ArrayList<String> blacklist = new ArrayList<>();
    static  {
        blacklist.add("MSS_thumper");
        blacklist.add("MSS_thumper_sub");
        blacklist.add("nskr_emgl");
        blacklist.add("nskr_emgl_sub");
        blacklist.add("fp_artillery");
        blacklist.add("fp_artillery_spawn");

        blacklist.add("prv_spatterflamer");
        blacklist.add("prv_spattergun");
        blacklist.add("prv_spattergun_1");
        blacklist.add("prv_spattergun_2");
        blacklist.add("prv_spattergun_fighter");
        blacklist.add("prv_spattergun_fighter_1");
        blacklist.add("prv_spattergun_fighter_2");

        blacklist.add("uaf_swaras_m_mlrs");
        blacklist.add("uaf_swaras_l_mlrs");

        blacklist.add("vayra_shockweb_canister");
        blacklist.add("vayra_canister");

        blacklist.add("KoT");
    }

	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(projectile);
        List<WeaponSpecAPI> weaponList = Global.getSettings().getAllWeaponSpecs();
        if (weaponList.isEmpty()) return;
        for (int i = 0; i<12; i++) {
                int r = MathUtils.getRandomNumberInRange(0, weaponList.size() - 1);
                WeaponSpecAPI weap = weaponList.get(r);
                System.out.println("Weapon: " + weap.getWeaponId());
                //Skip beams, lower missile rate, no firing dummies and avoid the blacklist
                if (weap.isBeam() || (weap.getType() == WeaponAPI.WeaponType.MISSILE && Math.random() < 0.8f) ||
                        weap.getWeaponId().contains("dummy") || weap.getWeaponId().contains("copy") || blacklist.contains(weap.getWeaponId()) ||
                        weap.getWeaponId().contains("armaa") || weap.getWeaponId().contains("goat")) {
                    i -= 1;
                    continue;
                }
            try {
                engine.spawnProjectile(weapon.getShip(), weapon, weap.getWeaponId(), weapon.getFirePoint(0), projectile.getWeapon().getCurrAngle() + MathUtils.getRandomNumberInRange(-15, 15), weapon.getShip().getVelocity());
            } catch (Exception e) {
                System.out.println("Dakkabox error when firing: "+ ( (weap != null) ? weap.getWeaponId() : "weap was NULL!") );
            }
        }
	}

}
