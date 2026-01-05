package fuzzypack.data.weapons;


import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;

//import com.fs.starfarer.api.util.Misc;  OnFireEffectPlugin

public class revcannon implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {


    private boolean once = true;
    private boolean fired = false;
    private float currentRefire = 0f;
    private final float bonusRefire = 0.1f;

    private final float maxRoF = 0.3f;

    private IntervalUtil interval = new IntervalUtil(1f,1f);


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (once) {
            currentRefire = weapon.getCooldown();
            once = false;
        }
        if (fired) {
            fired = false;
            weapon.setRemainingCooldownTo(currentRefire);

            interval.forceCurrInterval(weapon.getCooldown());
        }
        interval.advance(amount);
        if (weapon.getCooldownRemaining() == 0 && currentRefire < weapon.getCooldown() && interval.intervalElapsed()) {
            currentRefire += bonusRefire;
        }

    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        fired = true;
        if (currentRefire > maxRoF) currentRefire -= bonusRefire;
    }


}