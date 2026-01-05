package fuzzypack.data.shipsystems;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
//import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class SpotDodge extends BaseShipSystemScript {

        @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
	
            ShipAPI ship = (ShipAPI) stats.getEntity();
            
            ship.setCollisionClass(CollisionClass.NONE);
            ship.setAlphaMult(0.2f);
            ship.setJitterUnder(ship, new Color(255,170,50,130), 0.5f, 5, 12f);
            
            //ship.blockCommandForOneFrame(ShipCommand.FIRE);
            
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
                
                ship.setCollisionClass(CollisionClass.SHIP);
                ship.setAlphaMult(1f);
                
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Timeflow Increased", false);
		}
		return null;
	}
	




}


