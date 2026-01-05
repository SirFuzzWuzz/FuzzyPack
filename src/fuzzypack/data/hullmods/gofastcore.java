package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
//import java.awt.Color;
//import org.lazywizard.lazylib.MathUtils;

public class gofastcore extends BaseHullMod {
    
        private static final String big_id = "vroom_id";
        //private static ShipAPI ship;
        private float maxSpeed;
        
        private Color jitterColor = new Color(220,165,160,75);


    @Override
    public boolean shipHasOtherModInCategory(ShipAPI ship, String currMod, String category) {
        return super.shipHasOtherModInCategory(ship, currMod, category); //To change body of generated methods, choose Tools | Templates.
    }
    
    /*
    public float applyEffectsBeforeShipCreation(ShipAPI ship,  String id) {
        return maxSpeed = ship.getMutableStats().getMaxSpeed().base;
    }*/
        
	
	public void advanceInCombat(ShipAPI ship, float amount) {
                
                if (!ship.getEngineController().isFlamedOut() && !ship.getSystem().isActive()) {
                    maxSpeed = ship.getMaxSpeedWithoutBoost();
                } else if (ship.getEngineController().isFlamedOut()) {
                    maxSpeed = 500; // big sad
                }
                
                float speed = ship.getVelocity().length();
                
                float mod = (speed/maxSpeed); //percent of max speed
		
                ship.getShield().setArc(30 + mod*190);


                
                if (mod < 0.5) {
                    ship.getMutableStats().getShieldAbsorptionMult().modifyMult(big_id, 1.5f);
                    
                } else if (mod >= 0.5 && mod < 0.8) {
                    ship.getMutableStats().getShieldAbsorptionMult().modifyMult(big_id, 1f);
                    
                    
                } else if (mod >= 0.8 && mod < 2.0) {
                    ship.getMutableStats().getShieldAbsorptionMult().modifyMult(big_id, 0.5f);
                    

                } else if (mod >= 2.0) { //should only trigger when using system
                    ship.getMutableStats().getShieldAbsorptionMult().modifyMult(big_id, 0.2f);
                } 
                
                
                //setJitter(Object source, Color color, float intensity, int copies, float range);
                if (mod > 0.5 && !ship.getSystem().isActive() && ship.getShield().isOn()) {
                    ship.setJitterShields(true);
                    ship.setJitter(big_id, jitterColor, 0.5f*mod, 3, 80*mod);
                }

                
                /*
                float imageMod = mod * 1f;
                float Instability = (MathUtils.getRandomNumberInRange(0f, 100f)) * 0.01f;
                int ShieldRed = Math.round(COLOR_AFTERIMAGE_ORANGE.getRed() * (1 - Instability) + COLOR_AFTERIMAGE_RED.getRed() * Instability);
		int ShieldGreen = Math.round(COLOR_AFTERIMAGE_ORANGE.getGreen() * (1 - Instability) + COLOR_AFTERIMAGE_RED.getGreen() * Instability);
		int ShieldBlue = Math.round(COLOR_AFTERIMAGE_ORANGE.getBlue() * (1 - Instability) + COLOR_AFTERIMAGE_RED.getBlue() * Instability);
		int ShieldAlpha = Math.round(MathUtils.getRandomNumberInRange(0, MathUtils.getRandomNumberInRange(10, 50)) * 1);
                
                if (ship != null) {
			ship.addAfterimage(new Color(ShieldRed, ShieldGreen, ShieldBlue, ShieldAlpha), 0, 0, 
                                90f * MathUtils.getRandomNumberInRange(-5f, 5f) * imageMod, 90f * MathUtils.getRandomNumberInRange(-5f, 5f) * imageMod,
					1f,
					0f, 0.1f, 2f * imageMod, false, false, true);
		} */
                //ship.addAfterimage(Color.green, 0f, 90f, 0f, 1f*ship.getVelocity(), 0f, 0f, 0f, mod, true, true, true);
                
      
                if (ship == Global.getCombatEngine().getPlayerShip()) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(big_id, "graphics/icons/hullsys/infernium_injector.png", "Shield Flux/Dam", 
                            "" + ship.getShield().getFluxPerPointOfDamage() , false);
                }
		
		//stats.getAcceleration().modifyPercent(id, ACCELERATION_BONUS);
		//stats.getEngineDamageTakenMult().modifyPercent(id, EXTRA_DAMAGE);
		
		//stats.getMaxBurnLevel().modifyFlat(id, BURN_LEVEL_BONUS);
	}
        
        
                @Override
        public boolean isApplicableToShip(ShipAPI ship) {
            return false;
        }
	
        /*
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + ((Float) mag.get(HullSize.FRIGATE)).intValue();
		if (index == 1) return "" + ((Float) mag.get(HullSize.DESTROYER)).intValue();
		if (index == 2) return "" + ((Float) mag.get(HullSize.CRUISER)).intValue();
		if (index == 3) return "" + ((Float) mag.get(HullSize.CAPITAL_SHIP)).intValue();
		if (index == 4) return "" + (int) Math.round((1f - RANGE_MULT) * 100f) + "%";
		if (index == 5) return "" + (int) Math.round(FIGHTER_RATE) + "%";
//		if (index == 4) return "" + (int) ACCELERATION_BONUS;
//		//if (index == 5) return "four times";
//		if (index == 5) return "4" + Strings.X;
//		if (index == 6) return "" + BURN_LEVEL_BONUS;
		return null;
	} */
	

}
