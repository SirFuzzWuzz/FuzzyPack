package fuzzypack.data.hullmods;

import com.fs.starfarer.api.combat.ArmorGridAPI;
//import java.util.HashMap;
//import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
//ty ruddy(?) for the armour regen script

public class phaserestoration extends BaseHullMod {
        
    private static float flatRepairAmount; //restored per tic
    private IntervalUtil interval = new IntervalUtil(0.5f,0.5f);


    @Override
    public boolean shipHasOtherModInCategory(ShipAPI ship, String currMod, String category) {
        return super.shipHasOtherModInCategory(ship, currMod, category); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.PHASE_TIME_BONUS_MULT).modifyMult(id, 1.1f);
        stats.getPhaseCloakUpkeepCostBonus().modifyMult(id, 0.5f);
        stats.getPhaseCloakActivationCostBonus().modifyMult(id, 1.2f);
    }
        
	
    @Override
	public void advanceInCombat(ShipAPI ship, float amount) {
            ArmorGridAPI grid = ship.getArmorGrid();
            if (grid == null) return; //if ship is ded
            flatRepairAmount = grid.getMaxArmorInCell()/20; //maybe /40 ?
            interval.advance(amount);
            if (ship.isPhased()) { //make a smarter % of dmg taken later, ev change to CR for armor/hull
                if (interval.intervalElapsed()) {
                    int gridWidth = grid.getGrid().length;
                    int gridHeight = grid.getGrid()[0].length;
                    float maxArmorInCell = grid.getMaxArmorInCell();
                    for (int x = 0; x < gridWidth; x++) {
                        for (int y = 0; y < gridHeight; y++) {
                            if (grid.getArmorValue(x, y) < maxArmorInCell) {
                                grid.setArmorValue(x, y, Math.min(maxArmorInCell, flatRepairAmount + grid.getArmorValue(x, y))); //I fuckin love p-space
                            }
                        }
                    }
                    ship.syncWithArmorGridState();
                    ship.syncWeaponDecalsWithArmorDamage();
                }
            //p-space sucks man
            ship.getMutableStats().getPeakCRDuration().modifyMult("phaseregen", 0f);
            } else {
                ship.getMutableStats().getPeakCRDuration().unmodify("phaseregen");
            }
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

}
