package fuzzypack.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import java.util.ArrayList;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SindrianFervor extends BaseShipSystemScript {
    
        //private final float energyDmgMult = 1.2f;
        //private final float dmgTakenMult = 0.9f;
        //private final float speedMult = 1.2f;
        private final float crChange = 2f;
        private final float crLoss = 4f;
        
        private float oldCr;
        
        private boolean once = true;
        private ArrayList<ShipAPI> trgtList = new ArrayList<ShipAPI>();

        @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
            ShipAPI ship;
            if (stats.getEntity() instanceof ShipAPI) {
                ship = (ShipAPI) stats.getEntity();
            } else return;
            
            if (once) {
                oldCr = ship.getCurrentCR();
                ship.setCurrentCR(crChange);

                MagicRender.battlespace(
                Global.getSettings().getSprite("markers", "sindria_emblem"),
                ship.getLocation(),
                new Vector2f(),
                new Vector2f(300 ,250),
                new Vector2f(0,0),
                0f, //VectorUtils.getAngle(projVector, target.getLocation())
                0f,
                new Color(255,255,255,150),
                true,
                0.5f,
                1f,
                2f);
                
                once = false;
            }
            
            //ship.setJitter(null, new Color(200,10,200,170), 5f, 3, 50f);
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (once == false) {
            if (ship.getPeakTimeRemaining() > 0) {
                stats.getPeakCRDuration().modifyFlat(id, -30);
                ship.setCurrentCR(oldCr);
            } else {
                ship.setCurrentCR((float) (oldCr - 0.01 * MathUtils.getRandomNumberInRange(1, crLoss)));
            }
        }
        once = true;
	}
        
    public StatusData getStatusData(int index, State state, float effectLevel) {
    if (index == 0) {
        return new StatusData("Combat Readiness Increased", false);
    }
    return null;
	}

}


