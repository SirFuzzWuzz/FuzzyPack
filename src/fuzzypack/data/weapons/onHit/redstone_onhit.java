package fuzzypack.data.weapons.onHit;


import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;

import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import java.awt.Color;
//import com.fs.starfarer.api.util.IntervalUtil;
//import com.fs.starfarer.api.util.Misc;

public class redstone_onhit extends BaseCombatLayeredRenderingPlugin implements OnHitEffectPlugin {

        public redstone_onhit() {
        }

	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (!shieldHit && target instanceof ShipAPI) {
            float dam = projectile.getDamageAmount()*0.25f;
            float emp = 0;
            float thickness = 20;
            if (activateSystem((ShipAPI) target, engine)) {
                dam = projectile.getDamageAmount()*0.5f;
                emp = projectile.getEmpAmount();
                thickness = 50;
            }
            engine.spawnEmpArc(projectile.getSource(), point, target, target,
                    DamageType.HIGH_EXPLOSIVE,
                    dam,
                    emp, // emp
                    100000f, // max range
                    "tachyon_lance_emp_impact",
                    thickness, // thickness
                    new Color(200,40,40,255),
                    new Color(255,255,255,255)
            );
		}
	}

        public static boolean activateSystem(ShipAPI target, CombatEngineAPI engine) {
            boolean zap = false;

            if (target.getSystem() == null) return true;
            
            //workaround for IED's
            if (target.getSystem().getId().contains("le_explode")) { 
                target.getSystem().forceState(ShipSystemAPI.SystemState.IN, 0);
            }

            if (target.getSystem().isCoolingDown() || target.getSystem().isOutOfAmmo() 
                    || target.getSystem().isActive() || target.getFluxTracker().isVenting() ) {
                return true;
            }
            
            target.giveCommand(ShipCommand.USE_SYSTEM, target.getLocation(), 0);
            
            return zap;
        }
        
        
}
