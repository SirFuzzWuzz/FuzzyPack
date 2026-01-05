package fuzzypack.data.hullmods;

//import com.fs.starfarer.api.Global;
//import java.util.HashMap;
//import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;

import com.fs.starfarer.api.combat.ShipAPI;

import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;


public class rngtime extends BaseHullMod {
    
        private static final String big_id = "time_id";
        
        private final IntervalUtil interval = new IntervalUtil(3f,8f);
        
        private float intensity = 0;
        
     
        
        @Override
	public void advanceInCombat(ShipAPI ship, float amount) {

            interval.advance(amount);
            
            if (interval.intervalElapsed()) {
                
                ship.getMutableStats().getTimeMult().unmodify(big_id);
                
                /*
                counter += 0.1f;
                ship.getMutableStats().getTimeMult().modifyFlat(big_id, (float) Math.sin(counter) + 0.25f);
                if (counter >= 1f) {
                    counter = 0f;
                }*/
                
                intensity = (float) Math.random()*2f;
                float rngTime =  intensity + 0.5f;
                ship.getMutableStats().getTimeMult().modifyMult(big_id, rngTime);
                
            }
            
            //Visual
            ship.setJitter(this, new Color(200,120,255,55), intensity, 3, 0, 0 + intensity);
            if (!ship.isPhased()) {ship.setJitterUnder(this, new Color(200,120,255,155), intensity*2f, 25, 0, 7f + intensity);}
            
	} //advanceInCombat
        
	

}
