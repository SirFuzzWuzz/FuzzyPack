package fuzzypack.data.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class missile_sleigh_onfire implements OnFireEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        ShipAPI carrier = weapon.getShip().getWing().getSourceShip();
        List<WeaponAPI> wpnList = carrier.getAllWeapons();
        List<WeaponAPI> foundWpns = new ArrayList<>();
        for (WeaponAPI wpn : wpnList) {
            if (wpn.getType() == WeaponAPI.WeaponType.MISSILE) {
                foundWpns.add(wpn);
            }
        }
        if (foundWpns.isEmpty()) return;
        String wpnId = foundWpns.get(MathUtils.getRandomNumberInRange(0, foundWpns.size()-1)).getId();
        engine.spawnProjectile(weapon.getShip(), weapon, wpnId, weapon.getFirePoint(0), weapon.getCurrAngle(), weapon.getShip().getVelocity());
    }
}
