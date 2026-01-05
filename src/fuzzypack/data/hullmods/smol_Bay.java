package fuzzypack.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;


public class smol_Bay extends BaseHullMod {

	public ShipAPI ship;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getFighterRefitTimeMult().modifyPercent(id, REFIT_TIME_PLUS);
                //stats.getNumFighterBays().modifyFlat(id, 2f);
                stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyPercent(id, 100);
                //stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, 25f);
	}
	
	
        //Built-in only
        @Override
        public boolean isApplicableToShip(ShipAPI ship) {
            return false;
        }
		@Override
	public boolean affectsOPCosts() {
		return true;
	}
}

