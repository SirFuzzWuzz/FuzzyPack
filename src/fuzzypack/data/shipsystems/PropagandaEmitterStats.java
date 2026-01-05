package fuzzypack.data.shipsystems;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;


public class PropagandaEmitterStats extends BaseShipSystemScript {
        
        private final int radius = 2500;
        private final float selfTimeBuff = 1.15f;
        private final float allyTimeBuff = 1.2f;
        
        private ArrayList<ShipAPI> trgtList = new ArrayList<ShipAPI>();

        @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
            ShipAPI ship;
            if (stats.getEntity() instanceof ShipAPI) {
                ship = (ShipAPI) stats.getEntity();
            } else return;
            if (!ship.isAlive()) return;
            
            //self
            stats.getTimeMult().modifyMult(id, selfTimeBuff);
            
            MagicRender.battlespace(
                Global.getSettings().getSprite("markers", "sindria_emblem"),
                ship.getLocation(),
                new Vector2f(),
                new Vector2f(600 ,400),
                new Vector2f(0,0),
                ship.getFacing() -90, //VectorUtils.getAngle(projVector, target.getLocation())
                0f,
                new Color(255,255,255,15),
                true,
                0.05f,
                0.01f,
                0.05f);
            
            
            if (state == State.ACTIVE) {
                Iterator iter = Global.getCombatEngine().getShipGrid().getCheckIterator(ship.getLocation(), radius, radius);

                while(iter.hasNext()) {
                    ShipAPI trgt = (ShipAPI) iter.next();
                    if (trgt != ship && !trgtList.contains(trgt)) { //&& trgt != null
                        //Global.getCombatEngine().addFloatingText(trgt.getLocation(), "TRGT FOUND" + trgt.isAlly(), 100, Color.yellow, ship, 50, 50);
                        if (trgt.getOwner() == ship.getOwner()) {
                            trgt.getMutableStats().getTimeMult().modifyMult(id, allyTimeBuff);
                            trgtList.add(trgt);
                        }
                    }
                }
                
                //Remove buff when ally moves out of the aoe
                Iterator iter2 = trgtList.iterator();
                if (!trgtList.isEmpty() && trgtList != null) {
                    while(iter2.hasNext()) {
                        ShipAPI trgtShip = (ShipAPI) iter2.next();
                        if (!trgtShip.isFighter() && !trgtShip.isShuttlePod()) {
                            MagicRender.battlespace(
                                Global.getSettings().getSprite("markers", "sindria_emblem"),
                                trgtShip.getLocation(),
                                new Vector2f(),
                                new Vector2f(300 ,200),
                                new Vector2f(0,0),
                                trgtShip.getFacing() -90, //VectorUtils.getAngle(projVector, target.getLocation())
                                0f,
                                new Color(255,255,255,10),
                                true,
                                0.05f,
                                0.01f,
                                0.05f); 
                        }
                        if (MathUtils.getDistance(ship, trgtShip) > radius) {
                            trgtShip.getMutableStats().getTimeMult().unmodify(id);
                            iter2.remove();
                        }
                    }
                }
            }
            
            
            //If player ship is the target, left side visual, ignore for now
            /*if (trgtList != null && trgtList.contains(Global.getCombatEngine().getPlayerShip())) {
                
                ShipAPI trgtShip = (ShipAPI) trgtList.get(trgtList.indexOf(Global.getCombatEngine().getPlayerShip()));
 
                Global.getCombatEngine().maintainStatusForPlayerShip(new Object(), 
                                ship.getSystem().getSpecAPI().getIconSpriteName(),
                                ship.getSystem().getDisplayName(),
                                "% DIKTAT PROPAGANDA, TIMEFLOW INCREASED", false);
            }*/
	}
        
        
	public void unapply(MutableShipStatsAPI stats, String id) {
            stats.getTimeMult().unmodify(id);
            
            Iterator iter = trgtList.iterator();
            
            if (!trgtList.isEmpty() && trgtList != null) {
                while(iter.hasNext()) {
                    ShipAPI trgtShip = (ShipAPI) iter.next();
                    trgtShip.getMutableStats().getTimeMult().unmodify(id);
                    iter.remove();
                }
            }
	}
        
        
        public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Timeflow increased", false);
		}
		return null;
	}
            
}


