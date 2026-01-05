package fuzzypack.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;


public class rangelimiter extends BaseHullMod {

	//public ShipAPI ship;
        
        public static final float RANGE_THRESHOLD = 900f;
        public static final float RANGE_MULT = 0.5f;
        
        public static final float TURRET_SPEED_BONUS = 75f;
	
        @Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

                stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD);
		stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT);
                
                stats.getWeaponTurnRateBonus().modifyPercent(id, TURRET_SPEED_BONUS);
		stats.getBeamWeaponTurnRateBonus().modifyPercent(id, TURRET_SPEED_BONUS);
                
                stats.getDynamic().getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(id, 1f); //shhhh
	}
	
	
        //Built-in only
        @Override
        public boolean isApplicableToShip(ShipAPI ship) {
            return false;
        }

}

