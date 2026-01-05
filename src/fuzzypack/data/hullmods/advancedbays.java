package fuzzypack.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.awt.Color;


public class advancedbays extends BaseHullMod {

        /*public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        //stats.getFighterRefitTimeMult().modifyPercent(id, REFIT_TIME_PLUS);
        //stats.getNumFighterBays().modifyFlat(id, 2f);
        stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyPercent(id, 100);
        //stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, 25f);
        }*/
	
        
        public void advanceInCombat(ShipAPI ship, float amount) {
            
            /* if (ship.getHitpoints() >= ship.getMaxHitpoints()*0.75f) {
            ship.getMutableStats().getFighterRefitTimeMult().modifyMult("adv_bays", 1.25f);
            
            } else if (ship.getHitpoints() <= ship.getMaxHitpoints()*0.75f && ship.getHitpoints() >= ship.getMaxHitpoints()*0.5f) {
            ship.getMutableStats().getFighterRefitTimeMult().unmodify("adv_bays");
            
            } else if (ship.getHitpoints() < ship.getMaxHitpoints()*0.5f) {
            ship.getMutableStats().getFighterRefitTimeMult().modifyMult("adv_bays", 0.3f);
            }*/
            
            //float x = ship.getHitpoints()/ship.getMaxHitpoints();
            //float y = (float) (1.9*x*x - 0.95*x + 0.3); 
            ship.getMutableStats().getDynamic().getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).modifyPercent("adv_bays", 1- ship.getHitpoints()/ship.getMaxHitpoints());
            
            
        }
        
	
        //Built-in only
        @Override
        public boolean isApplicableToShip(ShipAPI ship) {
            int bays = (int) ship.getMutableStats().getNumFighterBays().getModifiedValue();
            return bays > 0;
        }
	
}

