package fuzzypack.data.weapons;


import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;


import com.fs.starfarer.api.combat.OnHitEffectPlugin;

import com.fs.starfarer.api.combat.ShipAPI;



import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;


import org.lwjgl.util.vector.Vector2f;
//I'm the best

public class boomerang implements OnHitEffectPlugin {

        
        //public static Object KEY_TARGET = new Object();
        
        //public ShipAPI ship;
        //public int frameCounter = 0;
        //public int ammo;
        //public boolean chargeBattery = true;
        
        public boomerang() {
	}
        private CombatFleetManagerAPI fleetManager; 
        
        public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
            if (projectile.isFading()) return;
            
            if (target instanceof ShipAPI) {
                ShipAPI targetShip = (ShipAPI) target;

                engine.removeEntity(projectile);
                
                ShipAPI returnBoomer = fleetManager.spawnShipOrWing("talon", point, 0f);
                returnBoomer.setLaunchingShip(projectile.getSource());
                returnBoomer.setFighterTimeBeforeRefit(0);
                
                
                /*                boomerang pg = new boomerang(targetShip, duration);
                CombatEntityAPI e = engine.addLayeredRenderingPlugin(pg);*/

                
            }
	}
        
        
        /*        protected ShipAPI target;
        protected IntervalUtil interval;
        protected boolean hasTarget = false;
        
        public boomerang(ShipAPI ship, float duration) {
        this.target = ship;
        this.interval = new IntervalUtil(duration - variance,duration + variance);
        
        this.hasTarget = true;
        target.getShield().toggleOn();
        
        
        if (target.getShield().getFluxPerPointOfDamage() < 1f) {
        target.getMutableStats().getShieldAbsorptionMult().modifyFlat("shield_short", 1000f);
        target.getMutableStats().getShieldAbsorptionMult().modifyMult("shield_short", 0.001f);
        }
        
        //}
        }
        
        public void advance(float amount) { //, CombatEngineAPI engine, WeaponAPI weapon public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
        if (Global.getCombatEngine().isPaused()) return;
        
        if (hasTarget) {
        
        
        
        }
        }
        
        public void init(CombatEntityAPI entity) {
        super.init(entity);
        }
        
        public boolean isExpired() {
        return !target.isAlive() ||
        !Global.getCombatEngine().isEntityInPlay(target) || !hasTarget;
        }
        */
        

}
