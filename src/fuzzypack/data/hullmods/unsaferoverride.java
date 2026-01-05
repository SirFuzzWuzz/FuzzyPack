package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class unsaferoverride extends BaseHullMod {

    private final CombatEngineAPI engine = Global.getCombatEngine();
    
    private static final float ROF_MULT = 1.25f;
    private static final float RECOIL_MULT = 1.3f;
    private static final float CAP_MULT = 1.2f;
    private final int zeroBoost = 25;
    private final float expDamage = 0.4f; //percent of max hull
    private static final float OVERLOAD_MULT = 0.1f;

    
    /*
    private static final float PEAK_MULT = 0.33f;
    private static final float RANGE_THRESHOLD = 600f;
    private static final float RANGE_MULT = 0.30f;
    */

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getOverloadTimeMod().modifyMult(id, OVERLOAD_MULT);
        stats.getFluxCapacity().modifyMult(id, CAP_MULT);
        stats.getBallisticRoFMult().modifyMult(id, ROF_MULT);
        stats.getEnergyRoFMult().modifyMult(id, ROF_MULT);
        stats.getMaxRecoilMult().modifyMult(id, RECOIL_MULT);
        stats.getZeroFluxSpeedBoost().modifyFlat(id, zeroBoost); // + speed yay
    }
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.setExplosionFlashColorOverride(Color.red);
        ship.setExplosionScale(2f);
    }
    
    private boolean onExp = true; //make sure only one explosion spawns
    //private final Color expColor = new Color(200, 255, 125, 255);

    private static final Map expSize = new HashMap();
	static {
		expSize.put(HullSize.FRIGATE, 50f);
		expSize.put(HullSize.DESTROYER, 75f);
		expSize.put(HullSize.CRUISER, 100f);
		expSize.put(HullSize.CAPITAL_SHIP, 150f);
	}
    
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getCurrFlux() > ship.getMaxFlux()/1.5f) {
            ship.getMutableStats().getWeaponMalfunctionChance().modifyFlat(ship.getFleetMemberId()+"_SR", 0.015f * ship.getCurrFlux()/ship.getMaxFlux());
            ship.getMutableStats().getEngineMalfunctionChance().modifyFlat(ship.getFleetMemberId()+"_SR", 0.01f * ship.getCurrFlux()/ship.getMaxFlux());
        } else {
            ship.getMutableStats().getWeaponMalfunctionChance().unmodify(ship.getFleetMemberId()+"_SR");
            ship.getMutableStats().getEngineMalfunctionChance().unmodify(ship.getFleetMemberId()+"_SR");
        }
        if (ship.getCurrFlux() >= ship.getMaxFlux()*0.98f) {
            DamagingExplosionSpec exp = new DamagingExplosionSpec(
                    0.5f, //dur
                    (Float) expSize.get(ship.getHullSize()), //rad
                    (Float) expSize.get(ship.getHullSize()) -25f, //core
                    ship.getMaxHitpoints()*expDamage, //maxDmg 1000f
                    ship.getMaxHitpoints()*0.1f, //minDmg 500f
                    CollisionClass.MISSILE_FF, //collision
                    CollisionClass.MISSILE_FF, //fighter collision
                    0.1f, //min particle size
                    1f, // particle size range
                    7f, //particle dur
                    10, //particle count
                    Color.MAGENTA, //particle color
                    Color.pink); //explosion color
            if (onExp) {
                int x = MathUtils.getRandomNumberInRange(0, ship.getArmorGrid().getGrid().length-1);
                int y = MathUtils.getRandomNumberInRange(0, ship.getArmorGrid().getGrid()[0].length-1);
                Vector2f location = ship.getArmorGrid().getLocation(x, y);
                engine.spawnDamagingExplosion(exp, ship, location, true);
                ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getMaxFlux()/2.1f);
                onExp = false;
            }
        }
        if (ship.getFluxTracker().getOverloadTimeRemaining() == 0f) {
            onExp = true;
        }
    }

    public boolean isApplicableToShip(ShipAPI ship) {
//		return !ship.getVariant().getHullMods().contains("unstable_injector") &&
//			   !ship.getVariant().getHullMods().contains("augmented_engines");
            //if (ship.getVariant().getHullSize() == HullSize.CAPITAL_SHIP) return false;
            if (ship.getVariant().hasHullMod(HullMods.CIVGRADE) && !ship.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS)) return false;
            if (ship.getHullSpec().isPhase()) return false;
            return (ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES));
    }
    
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getVariant().hasHullMod(HullMods.CIVGRADE) && !ship.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS)) {
                return "Can not be installed on civilian ships";
        }
        if (ship.getHullSpec().isPhase()) return "Can not be installed on phase ships";
        if (!ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES)) return "Can only be installed on ships with Safety Overrides";
        return null;
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round((ROF_MULT-1) * 100) + "%";
        if (index == 1) return Math.round((CAP_MULT-1) * 100) + "%";
        if (index == 2) return zeroBoost + "";
        if (index == 3) return Math.round((RECOIL_MULT-1) * 100) + "%";
        if (index == 4) return Math.round(expDamage * 100) + "%";
        return null;
    }
}


