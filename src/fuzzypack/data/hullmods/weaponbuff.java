package fuzzypack.data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;



//import java.util.*;

public class weaponbuff extends BaseHullMod {

    private static final float ROF_MOD = 100f; //firerate bonus, 100f is 2x (also ammo regen)
    private static final float RANGE_THRESHOLD = 600f; //range cutoff limit
    private static final float RANGE_MULT = 0.50f; //range reduction multiplier above limit, ty ness


    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getBallisticRoFMult().modifyPercent(id, ROF_MOD);
        stats.getEnergyRoFMult().modifyPercent(id, ROF_MOD);

        stats.getBeamWeaponDamageMult().modifyMult(id, 1 + ROF_MOD / 1000 ); //no funny beaming
        stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD);
        stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT);
    }

}


