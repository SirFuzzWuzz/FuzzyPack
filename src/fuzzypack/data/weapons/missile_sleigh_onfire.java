package fuzzypack.data.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class missile_sleigh_onfire implements OnFireEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(projectile);
        ShipAPI carrier = weapon.getShip().getWing().getSourceShip();
        if (carrier == null) return;
        List<WeaponAPI> wpnList = carrier.getAllWeapons();
        List<WeaponAPI> foundWpns = new ArrayList<>();
        for (WeaponAPI wpn : wpnList) {
            if (wpn.getType() == WeaponAPI.WeaponType.MISSILE) {
                if (wpn.getAmmo() > 0) foundWpns.add(wpn);
            }
        }
        if (foundWpns.isEmpty()) {
            engine.spawnProjectile(weapon.getShip(), weapon, "bomb", weapon.getFirePoint(0), weapon.getCurrAngle(), weapon.getShip().getVelocity());
        } else {
            String wpnId = foundWpns.get(MathUtils.getRandomNumberInRange(0, foundWpns.size() - 1)).getId();
            engine.spawnProjectile(weapon.getShip(), weapon, wpnId, weapon.getFirePoint(0), weapon.getCurrAngle(), weapon.getShip().getVelocity());
        }
    }
}
