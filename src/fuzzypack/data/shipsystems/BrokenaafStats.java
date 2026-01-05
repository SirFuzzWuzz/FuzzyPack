package fuzzypack.data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;

public class BrokenaafStats extends BaseShipSystemScript {

	public static final float ROF_BONUS = 1f;
	public static final float energy_dmg = 1.5f;
	public static final float dmg_taken = 1.5f; //damage taken
	//public static final float FLUX_REDUCTION = 50f;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		float mult = 1f + ROF_BONUS * effectLevel;
		stats.getBallisticRoFMult().modifyMult(id, mult);
                stats.getEnergyWeaponDamageMult().modifyMult(id, energy_dmg);
                
                //debuff
                stats.getHullDamageTakenMult().modifyMult(id, dmg_taken);
		stats.getArmorDamageTakenMult().modifyMult(id, dmg_taken);
                
                ShipAPI ship = (ShipAPI)stats.getEntity();
                ship.setJitter(ship, Color.red, 5, 0, 2);
                
//		ShipAPI ship = (ShipAPI)stats.getEntity();
//		ship.blockCommandForOneFrame(ShipCommand.FIRE);
//		ship.setHoldFireOneFrame(true);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticRoFMult().unmodify(id);
                stats.getEnergyWeaponDamageMult().unmodify(id);
                
                stats.getHullDamageTakenMult().unmodify(id);
                stats.getArmorDamageTakenMult().unmodify(id);
                
                ShipAPI ship = (ShipAPI)stats.getEntity();
                ship.setJitter(ship, Color.red, 5, 0, 0);
                
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float mult = 1f + ROF_BONUS * effectLevel;
		float bonusPercent = (int) ((mult - 1f) * 100f);
		if (index == 0) {
			return new StatusData("weapon rate of fire +" + (int) bonusPercent + "%", false);
		}

		return null;
	}
}
