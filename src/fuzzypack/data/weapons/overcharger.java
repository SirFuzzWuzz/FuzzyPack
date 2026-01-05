package fuzzypack.data.weapons;


import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
/*import java.util.ArrayList;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
*/
//import com.fs.starfarer.api.util.Misc;  OnFireEffectPlugin
import com.fs.starfarer.api.util.IntervalUtil;

public class overcharger implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private boolean once = true;
    private boolean fired = false;
    private float currentRefire = 0f;
    private final float bonusRefire = 0.007f;

    private final float minRoF = 0.3f;
    private final float maxRoF = 0.01f;

    private IntervalUtil interval = new IntervalUtil(1f,1f);
    

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (once) {
            currentRefire = maxRoF; //start RoF
            once = false;
        }
        if (fired) {
            fired = false;
            weapon.setRemainingCooldownTo(currentRefire);
            interval.forceCurrInterval(1f);
        }
        interval.advance(amount);
        if (weapon.getCooldownRemaining() == 0 && currentRefire > maxRoF && interval.intervalElapsed()) {
            currentRefire -= bonusRefire * 5;
            if (currentRefire < maxRoF) currentRefire = maxRoF;
            //System.out.println("current refire delay: " +currentRefire);
        }
    }
    
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        fired = true;
        if (currentRefire < minRoF) currentRefire += bonusRefire;
    }
}