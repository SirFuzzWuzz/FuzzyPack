package fuzzypack.data.shipsystems;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import org.lwjgl.util.vector.Vector2f;

public class PhaseDisruptor extends BaseShipSystemScript {
	//public static final float ENERGY_DAM_PENALTY_MULT = 0.5f;
	
	public static float DISRUPTION_DUR = 1f;
	protected static float MIN_DISRUPTION_RANGE = 400f;
        
        protected static float expSize = 100f;
	
	public static final Color OVERLOAD_COLOR = new Color(255,120,220,255);
	
	public static final Color JITTER_COLOR = new Color(255,155,255,75);
	public static final Color JITTER_UNDER_COLOR = new Color(255,155,255,155);

	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;

		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();

		} else {
			return;
		}
		

		float jitterLevel = effectLevel;
		if (state == State.OUT) {
			jitterLevel *= jitterLevel;
		}
		float maxRangeBonus = 10f;
		//float jitterRangeBonus = jitterLevel * maxRangeBonus;
		float jitterRangeBonus = jitterLevel * maxRangeBonus;
		if (state == State.OUT) {
			//jitterRangeBonus = maxRangeBonus + (1f - jitterLevel) * maxRangeBonus; 
		}
		
		ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 5, 0f, 3f + jitterRangeBonus);
		//ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus * 0.67f);
		ship.setJitter(this, JITTER_COLOR, jitterLevel, 2, 0f, 0 + jitterRangeBonus);
		
		String targetKey = ship.getId() + "_acausal_target";
		Object foundTarget = Global.getCombatEngine().getCustomData().get(targetKey); 
		if (state == State.IN) {
			if (foundTarget == null) {
				ShipAPI target = findTarget(ship);
				if (target != null) {
					Global.getCombatEngine().getCustomData().put(targetKey, target);
				}
			}
		} else if (effectLevel >= 1) {
			if (foundTarget instanceof ShipAPI) {
				ShipAPI target = (ShipAPI) foundTarget;
				if (target.getFluxTracker().isOverloadedOrVenting()) target = ship;
				applyEffectToTarget(ship, target);
                                
                                Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(), ship, target.getLocation(), target, 60f, new Color(0,0,10,50), OVERLOAD_COLOR);
			}
		} else if (state == State.OUT && foundTarget != null) {
			Global.getCombatEngine().getCustomData().remove(targetKey);
		}
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponDamageMult().unmodify(id);
	}
	
	protected ShipAPI findTarget(ShipAPI ship) {
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();
		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM)){
			target = (ShipAPI) ship.getAIFlags().getCustom(AIFlags.TARGET_FOR_SHIP_SYSTEM);
		}
		
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum) target = null;
		} else {
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FIGHTER, range, true);
				} else {
					Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum) target = null;
					}
				}
			}
			if (target == null) {
				target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FIGHTER, range, true);
			}
		}
		if (target == null || target.getFluxTracker().isOverloadedOrVenting() || !target.getHullSpec().isPhase()) target = ship;
		
		return target;
	}
	
	public static float getMaxRange(ShipAPI ship) {

            return ship.getMutableStats().getSystemRangeBonus().computeEffective(MIN_DISRUPTION_RANGE);

	}

	
	protected void applyEffectToTarget(final ShipAPI ship, final ShipAPI target) {
		if (target.getFluxTracker().isOverloadedOrVenting()) {
			return;
		}
		if (target == ship) return;
		
		target.setOverloadColor(OVERLOAD_COLOR);
		target.getFluxTracker().beginOverloadWithTotalBaseDuration(DISRUPTION_DUR);
		target.getEngineController().forceFlameout(true);
                
                /*
                DamagingExplosionSpec exp = new DamagingExplosionSpec(
                    0.5f, //dur
                    (Float) expSize, //rad
                    (Float) expSize-25f, //core
                    target.getMaxHitpoints()*0.2f, //maxDmg 1000f
                    ship.getMaxHitpoints()*0.1f, //minDmg 500f
                    CollisionClass.MISSILE_FF, //collision
                    CollisionClass.MISSILE_FF, //fighter collision
                    0.1f, //min particle size
                    1f, // particle size range
                    7f, //particle dur
                    10, //particle count
                    OVERLOAD_COLOR, //particle color
                    JITTER_COLOR); //explosion color

                Vector2f location = target.getLocation();
                Global.getCombatEngine().spawnDamagingExplosion(exp, target, location, true);*/
                

		if (target.getFluxTracker().showFloaty() || 
				ship == Global.getCombatEngine().getPlayerShip() ||
				target == Global.getCombatEngine().getPlayerShip()) {
			target.getFluxTracker().playOverloadSound();
			target.getFluxTracker().showOverloadFloatyIfNeeded("Cringe!", OVERLOAD_COLOR, 4f, true);
		}
		
		Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (!target.getFluxTracker().isOverloadedOrVenting()) {
					target.resetOverloadColor();
					Global.getCombatEngine().removePlugin(this);
				}
			}
		});
	}
	

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;
		
		ShipAPI target = findTarget(ship);
		if (target != null && target != ship && target.getHullSpec().isPhase()) {
			return "READY";
		}

		if ((target == null || target == ship) && ship.getShipTarget() != null) {
                    if (!ship.getShipTarget().getHullSpec().isPhase()) return "NO PHASECLOAK";
			return "OUT OF RANGE";
		}
		return "NO TARGET";

	}

	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI target = findTarget(ship);
		return target != null && target != ship;
		//return super.isUsable(system, ship);
	}
	

	
}








