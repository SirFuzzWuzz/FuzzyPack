package fuzzypack.data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class softflux_projectile implements OnFireEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        projectile.getDamage().setSoftFlux(true);

        /*for (int i = 0; i<5; i++) {
            DamagingProjectileAPI projSpawn = (DamagingProjectileAPI) engine.spawnProjectile(weapon.getShip(), weapon, "fp_plasmarail",
                    weapon.getFirePoint(0), weapon.getCurrAngle(), weapon.getShip().getVelocity());
            projSpawn.setDamageAmount(50);
        }*/
    }
}
