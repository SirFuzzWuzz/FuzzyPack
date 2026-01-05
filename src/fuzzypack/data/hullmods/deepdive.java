package fuzzypack.data.hullmods;

//import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
//import java.util.HashMap;
//import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.magiclib.plugins.MagicTrailPlugin;
//import com.fs.starfarer.api.util.IntervalUtil;
//import java.awt.Color;

//ty ruddy(?) for the armour regen script

public class deepdive extends BaseHullMod {
        
        @Override
        public boolean shipHasOtherModInCategory(ShipAPI ship, String currMod, String category) {
            return super.shipHasOtherModInCategory(ship, currMod, category); //To change body of generated methods, choose Tools | Templates.
        }
        
        private float maxSpeed;
        
        public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

            maxSpeed  = stats.getMaxSpeed().modified;

            stats.getDynamic().getMod(Stats.PHASE_TIME_BONUS_MULT).modifyMult(id, 3f);
            //stats.getPhaseCloakUpkeepCostBonus().modifyMult("pr_id", 0.85f);
            stats.getPhaseCloakActivationCostBonus().modifyMult("pr_id", 1.5f);
            stats.getDynamic().getMod(
				Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, 10f);
        }
        
        private boolean hasPhased = false;
	
	public void advanceInCombat(ShipAPI ship, float amount) {
            
            
            if (ship.isPhased() && ship.getExtraAlphaMult() <= 0.3f) { 
                
                float fadeMult = ship.getMaxSpeed()/maxSpeed;
                
                ship.setExtraAlphaMult( fadeMult -0.3f);
                
                hasPhased = true;
                
                    
            } else if(hasPhased) {
                hasPhased = false;
                ship.setExtraAlphaMult(0.3f);
            }
                //isPhased && interval
            

	} //advanceInCombat
        
        
        @Override
        public boolean isApplicableToShip(ShipAPI ship) {
            return ship.getHullSpec().isPhase(); // not null = has phase = true = big brain
        }
        
        
        @Override
	public String getUnapplicableReason(ShipAPI ship) {
                /*if (ship.getVariant().hasHullMod(HullMods.PHASE_ANCHOR)) {
			return "Incompatible with Phase Anchor";
		}*/
		if (!ship.getHullSpec().isPhase()) {
			return "Can only be installed on phase ships";
		}
		return super.getUnapplicableReason(ship);
	}
	
        /*
	public String getDescriptionParam(int index, HullSize hullSize) {

		return null;
	} */
	

}
