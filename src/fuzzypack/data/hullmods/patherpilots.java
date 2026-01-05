package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;

import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAPI;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.combat.DroneStrikeStats;
import com.fs.starfarer.api.impl.combat.DroneStrikeStatsAIInfoProvider;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
//ty arthr for icon

public class patherpilots extends BaseHullMod implements DroneStrikeStatsAIInfoProvider {
    
        private final float healthThreshold = 50f; //percent
        private final float damageMult = 1f;
        
        private final List<String> quoteList =  Arrays.asList("FOR LUDD!", "DIE MOLOCH!", "AAAAAAAAAA!!", ">:)",
                "THEY CAME FROM, BEHIND!", "I REGRET EVERYTHING","I REGRET NOTHING", "BANZAI!!!", "WHERE ARE THE BREAKS?!?!", "FOR THE EMPEROR!");
        
        @Override
        public boolean isApplicableToShip(ShipAPI ship) {
            
            if (ship.getVariant().hasHullMod(HullMods.RECOVERY_SHUTTLES)) return false;
            int bays = (int) ship.getMutableStats().getNumFighterBays().getModifiedValue();
            return bays > 0; 
        }
        
        @Override
	public String getUnapplicableReason(ShipAPI ship) {
            
            if (ship.getVariant().hasHullMod(HullMods.RECOVERY_SHUTTLES)) {
                return "Incompatible with Recovery Shuttles, these boys aren't going home";
            }

            if (!ship.hasLaunchBays()) {
                return "Ship does not have fighter bays";
            }
            
            return super.getUnapplicableReason(ship);
	}
	
        @Override
	public void advanceInCombat(ShipAPI ship, float amount) {
                        
            if (!ship.isAlive()) return;
            
            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing() == null) continue;

                for (ShipAPI fighter: bay.getWing().getWingMembers()) {
                    if (fighter.isDrone() || fighter.getHullSpec().getMaxCrew() == 0) continue;
                    if (fighter.getHitpoints() <= fighter.getMaxHitpoints() * (healthThreshold / 100)) {
                        fighter.setShipAI(null);
                        convertDrones(fighter, fighter.getShipTarget());
                    }
                }
            }
	}

        
        protected WeaponAPI weapon;
	protected boolean fired = false;
        protected ShipAPI forceNextTarget;

        public void convertDrones(ShipAPI ship, final ShipAPI target) {
		CombatEngineAPI engine = Global.getCombatEngine();
		fired = true;
		forceNextTarget = null;
                
                ShipAPI drone = ship;
                //drone.setShipAI(null);

                MissileAPI missile = (MissileAPI) engine.spawnProjectile(
                                ship, weapon, getWeaponId(),
                                new Vector2f(drone.getLocation()), drone.getFacing(), new Vector2f(drone.getVelocity()));
                if (target != null && missile.getAI() instanceof GuidedMissileAI) {
                        GuidedMissileAI ai = (GuidedMissileAI) missile.getAI();
                        ai.setTarget(target);
                }

                missile.setEmpResistance(10000);

                float base = missile.getMaxRange();
                float max = getMaxRange(ship);
                missile.setMaxRange(max);
                missile.setMaxFlightTime(missile.getMaxFlightTime() * max/base);

                ship.setHitpoints(ship.getMaxHitpoints());
                missile.setDamageAmount(damageMult*ship.getMaxHitpoints());
                missile.getDamage().setType(DamageType.HIGH_EXPLOSIVE);

                drone.getWing().removeMember(drone);
                //drone.setWing(null); crashcode
                
                drone.setExplosionFlashColorOverride(new Color(255, 150, 50, 255));
                engine.addLayeredRenderingPlugin(new DroneStrikeStats.DroneMissileScript(drone, missile));

                //float thickness = 16f;
                float thickness = 20f;
                float coreWidthMult = 0.67f;
                EmpArcEntityAPI arc = engine.spawnEmpArcVisual(ship.getLocation(), ship,
                                missile.getLocation(), missile, thickness, new Color(255,100,100,255), Color.white);
                arc.setCoreWidthOverride(thickness * coreWidthMult);
                arc.setSingleFlickerMode();
                
                String quote = (String) quoteList.get(MathUtils.getRandomNumberInRange(0, quoteList.size()-1));
                engine.addFloatingText(ship.getLocation(), quote, 12f, Color.red, ship, 3, 1);
	}
        
        protected String getWeaponId() {
		return "terminator_missile";
	}
        
        
        public List<ShipAPI> getDrones(ShipAPI ship) {
		List<ShipAPI> result = new ArrayList<ShipAPI>();
		for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
			if (bay.getWing() == null) continue;
			for (ShipAPI drone : bay.getWing().getWingMembers()) {
				result.add(drone);
			}
		}
		return result;
	}
	public float getMaxRange(ShipAPI ship) {
		if (weapon == null) {
			weapon = Global.getCombatEngine().createFakeWeapon(ship, getWeaponId());
		}
		//return weapon.getRange();
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(weapon.getRange());
	}
        public boolean dronesUsefulAsPD() {
		return true;
	}
	public boolean droneStrikeUsefulVsFighters() {
		return false;
	}
	public int getMaxDrones() {
		return 99;
	}
	public float getMissileSpeed() {
		return weapon.getProjectileSpeed();
	}
	public void setForceNextTarget(ShipAPI forceNextTarget) {
		this.forceNextTarget = forceNextTarget;
	}
	public ShipAPI getForceNextTarget() {
		return forceNextTarget;
	}
        
        public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round(healthThreshold) + "%";
        if (index == 1) return damageMult + "";
        return null;
    }

}
