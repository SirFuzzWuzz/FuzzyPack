package fuzzypack.data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
//import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;





//import java.util.*;

public class energyrangebuff extends BaseHullMod {

    private static final float RANGE_BUFF = 200f;


    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        
        stats.getEnergyWeaponRangeBonus().modifyFlat(id, RANGE_BUFF);
        stats.getBeamWeaponRangeBonus().modifyFlat(id, -RANGE_BUFF);
    }


}


