package fuzzypack.data.weapons;


import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class heatsink implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private WeaponAPI tpc;
    private boolean once = true;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        
        if (once){
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getSlot().isBuiltIn() && w.getSlot().getSlotSize() == WeaponAPI.WeaponSize.LARGE) {
                    //TPC found 
                    tpc = w;
                }   
            }
            once = false;
        }

        if (tpc != null) {
            if (tpc.getAmmo() == 0) { //tpc is out of charges
                weapon.setAmmo(20); //give the heatsink 1 ammo
            }
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
    	tpc.setAmmo(tpc.getMaxAmmo());
    }

}