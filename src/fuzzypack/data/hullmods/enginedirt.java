package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.VectorUtils;


public class enginedirt extends BaseHullMod {
    private final float zeroFluxBoostMult = 1.5f;
    private final float engineDamageTakenMult = 1.25f;
    private final IntervalUtil interval = new IntervalUtil(0.1f,2f);

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEngineDamageTakenMult().modifyMult(id, engineDamageTakenMult);
        if (hullSize == HullSize.FRIGATE) {
            stats.getZeroFluxSpeedBoost().modifyMult(id, zeroFluxBoostMult);
        }
        else if (hullSize == HullSize.DESTROYER){
            stats.getZeroFluxSpeedBoost().modifyMult(id, zeroFluxBoostMult - 0.1f);
        }
        else if (hullSize == HullSize.CRUISER) {
            stats.getZeroFluxSpeedBoost().modifyMult(id, zeroFluxBoostMult - 0.2f);
        }
        else if (hullSize == HullSize.CAPITAL_SHIP) {
            stats.getZeroFluxSpeedBoost().modifyMult(id, zeroFluxBoostMult - 0.3f);
        }
    }
    @Override
	public void advanceInCombat(ShipAPI ship, float amount) {            
            if (ship.getEngineController().getShipEngines().isEmpty())  return;
            CombatEngineAPI engine = Global.getCombatEngine();
            List<ShipEngineAPI> shipEngines = ship.getEngineController().getShipEngines();
            if (interval.intervalElapsed()) {
                int rng = (int) Math.round((shipEngines.size()-1) * Math.random());
                ShipEngineAPI shipEng = (ShipEngineAPI) shipEngines.get(rng);
                float angle = VectorUtils.getAngle(ship.getLocation(), shipEng.getLocation());
                engine.spawnProjectile(ship, null, "flarelauncher1", shipEng.getLocation(), angle, ship.getVelocity());
            }
            if (ship.getHullSize().FRIGATE == HullSize.FRIGATE) {
                interval.advance(amount);
            } else if (ship.getHullSize().DESTROYER == HullSize.DESTROYER){
                interval.advance(amount + 0.03f);
            } else if (ship.getHullSize().CRUISER == HullSize.CRUISER){
                interval.advance(amount + 0.05f);
            } else if (ship.getHullSize().CAPITAL_SHIP == HullSize.CAPITAL_SHIP){
                interval.advance(amount + 0.08f);
            }
            //Visual
            Color clr = new Color(
                        255,
                        130, 
                        255, 
                        200); //alpha
            ship.getEngineController().fadeToOtherColor(this, clr, null, 3f, 0.5f);
            ship.getEngineController().extendFlame(this, 0.4f, 0.4f, 0.4f);
	} //advanceInCombat
    @Override
     public boolean isApplicableToShip(ShipAPI ship) {
         return !ship.getEngineController().getShipEngines().isEmpty() && !ship.getHullSpec().isPhase();
     }
	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getEngineController().getShipEngines().isEmpty()) return "Ship has no engines";
        if (ship.getHullSpec().isPhase()) return "Cannot be installed on phase ships";
		return super.getUnapplicableReason(ship);
	}
    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {

        if (index == 0) return Math.round((engineDamageTakenMult-1) * 100) + "%";

        return null;
    }
}
