package fuzzypack.data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;


public class ExtremeEnergyFocusStats extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS_PERCENT = 100f;
	//public static final float EXTRA_DAMAGE_TAKEN_PERCENT = 100f;
        private boolean hasRun = false;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
                //ShipAPI ship = (ShipAPI) stats.getEntity();
		
		float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
		stats.getEnergyWeaponDamageMult().modifyPercent(id, bonusPercent);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, bonusPercent*0.25f);
                //stats.getPeakCRDuration().modifyMult(id, 0);
                

		hasRun = true;
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
            if (hasRun) {
                    ShipAPI ship = (ShipAPI) stats.getEntity();
                    if (ship.getOriginalOwner() == -1) return;

                    //ship.getFluxTracker().setOverloadDuration(3f);

                    stats.getEnergyWeaponDamageMult().unmodify(id);
                    stats.getEnergyWeaponRangeBonus().unmodify(id);
                    //stats.getPeakCRDuration().unmodify(id);

                    for (WeaponAPI wpn : ship.getAllWeapons()) {
                        if (wpn.getType() == WeaponType.ENERGY && Math.random() >= 0.87f) {
                                wpn.disable();
                        }
                }
            }
                
//		stats.getEnergyWeaponRangeBonus().unmodify(id);
//		stats.getArmorDamageTakenMult().unmodify(id);
//		stats.getHullDamageTakenMult().unmodify(id);
//		stats.getShieldDamageTakenMult().unmodify(id);
//		stats.getWeaponDamageTakenMult().unmodify(id);
//		stats.getEngineDamageTakenMult().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
		//float damageTakenPercent = EXTRA_DAMAGE_TAKEN_PERCENT * effectLevel;
		if (index == 0) {
			return new StatusData("+" + (int) bonusPercent + "% energy weapon damage" , false);
		} else if (index == 1) {
			//return new StatusData("+" + (int) damageTakenPercent + "% weapon/engine damage taken", false);
			return new StatusData("+" + (int) bonusPercent*0.25f + "% energy weapon range" , false);
		} else if (index == 2) {
			//return new StatusData("shield damage taken +" + (int) damageTakenPercent + "%", true);
			return null;
		}
		return null;
	}
}
