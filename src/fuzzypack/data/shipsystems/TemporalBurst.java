package fuzzypack.data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
//import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class TemporalBurst extends BaseShipSystemScript {

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
	
            stats.getTimeMult().modifyMult(id, 3 * effectLevel + 1);
            stats.getAcceleration().modifyMult(id, 3 * effectLevel + 1);
            stats.getMaxSpeed().modifyFlat(id, 1.5f * effectLevel + 1);
            
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getTimeMult().unmodify(id);
                stats.getAcceleration().unmodify(id);
                stats.getMaxSpeed().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Timeflow Increased", false);
		}
		return null;
	}
	
	
	public float getActiveOverride(ShipAPI ship) {
//		if (ship.getHullSize() == HullSize.FRIGATE) {
//			return 1.25f;
//		}
//		if (ship.getHullSize() == HullSize.DESTROYER) {
//			return 0.75f;
//		}
//		if (ship.getHullSize() == HullSize.CRUISER) {
//			return 0.5f;
//		}
		return -1;
	}
	public float getInOverride(ShipAPI ship) {
		return -1;
	}
	public float getOutOverride(ShipAPI ship) {
		return -1;
	}
	
	public float getRegenOverride(ShipAPI ship) {
		return -1;
	}

	public int getUsesOverride(ShipAPI ship) {
		if (ship.getHullSize() == HullSize.FRIGATE) {
			return 2;
		}
		if (ship.getHullSize() == HullSize.DESTROYER) {
			return 2;
		}
		if (ship.getHullSize() == HullSize.CRUISER) {
			return 2;
		}
		return -1;
	}
}


