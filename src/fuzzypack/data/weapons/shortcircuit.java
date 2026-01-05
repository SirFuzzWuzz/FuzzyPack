package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;


import com.fs.starfarer.api.combat.OnHitEffectPlugin;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;


import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;
//I'm the best

public class shortcircuit extends BaseCombatLayeredRenderingPlugin implements OnHitEffectPlugin {

        
        public shortcircuit() {
	}
        
        private final float duration = 7f;
        private final float variance = 1f;

        
        public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
            if (projectile.isFading()) return;
            
            if (shieldHit && target instanceof ShipAPI) {
                ShipAPI targetShip = (ShipAPI) target;
                /*if (!targetShip.getChildModulesCopy().isEmpty()) return;*/
                
                targetShip.getFluxTracker().showOverloadFloatyIfNeeded("Shield Locked!", Color.white, 4f, true);
                
                shortcircuit pg = new shortcircuit(targetShip, duration);
                CombatEntityAPI e = engine.addLayeredRenderingPlugin(pg);
                //engine.addPlugin((EveryFrameCombatPlugin) pg);
            }
	}
        
        
        protected ShipAPI target;
        protected IntervalUtil interval;
        protected boolean hasTarget = false;
        
        public shortcircuit(ShipAPI ship, float duration) {
            this.target = ship;
            this.interval = new IntervalUtil(duration - variance,duration + variance);
            
            this.hasTarget = true;
            target.getShield().toggleOn();
            
            
            if (target.getShield().getFluxPerPointOfDamage() < 1f) {
                target.getMutableStats().getShieldAbsorptionMult().modifyFlat("shield_short", 1000f);
                target.getMutableStats().getShieldAbsorptionMult().modifyMult("shield_short", 0.001f);
            }

        }
        
        public void advance(float amount) { //, CombatEngineAPI engine, WeaponAPI weapon public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
            if (Global.getCombatEngine().isPaused()) return;
            
            if (hasTarget) {
                interval.advance(amount);
                
                this.target.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
		        this.target.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                
                if (interval.intervalElapsed()) {
                    this.hasTarget = false;
                    this.target.getMutableStats().getShieldAbsorptionMult().unmodify("shield_short");
                    this.target.getFluxTracker().showOverloadFloatyIfNeeded("Shield OK!", Color.white, 4f, true);
                }
            } 
        } 
        
        public void init(CombatEntityAPI entity) {
		super.init(entity);
	}
        
        public boolean isExpired() {
		return !target.isAlive() || 
                        !Global.getCombatEngine().isEntityInPlay(target) || !hasTarget;
	}
        
        

}
