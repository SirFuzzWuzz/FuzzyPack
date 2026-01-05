package fuzzypack.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import static com.fs.starfarer.api.impl.combat.EntropyAmplifierStats.TEXT_COLOR;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.List;


public class RangeReducerStats extends BaseShipSystemScript {
    
    public static Object KEY_SHIP = new Object();
    public static Object KEY_TARGET = new Object();
    
    public static float RANGE_MULT = 0.5f;
    protected static float RANGE = 1500f;
    
    public static final float SENSOR_RANGE_PERCENT = 50f;
    public static final float WEAPON_RANGE_PERCENT = 25f;
    
    public static Color JITTER_COLOR = new Color(50,250,50,75);
    public static Color JITTER_UNDER_COLOR = new Color(100,250,100,155);

    
    public static class TargetData {
        public ShipAPI ship;
        public ShipAPI target;
        public EveryFrameCombatPlugin targetEffectPlugin;
        public float currRangeMult;
        public float elaspedAfterInState;

        public TargetData(ShipAPI ship, ShipAPI target) {
                this.ship = ship;
                this.target = target;
        }
    }
    
 
    
    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
            ShipAPI ship = null;
            if (stats.getEntity() instanceof ShipAPI) {
                    ship = (ShipAPI) stats.getEntity();
            } else {
                    return;
            }

            final String targetDataKey = ship.getId() + "_range_target_data";

            Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
            if (state == State.IN && targetDataObj == null) {
                    ShipAPI target = findTarget(ship);
                    Global.getCombatEngine().getCustomData().put(targetDataKey, new RangeReducerStats.TargetData(ship, target));
                    if (target != null) {
                            if (target.getFluxTracker().showFloaty() || ship == Global.getCombatEngine().getPlayerShip() ||
                            target == Global.getCombatEngine().getPlayerShip()) {
                                target.getFluxTracker().showOverloadFloatyIfNeeded("Targeting Disrupted!", TEXT_COLOR, 4f, true);
                                }
                    }
            } else if (state == State.IDLE && targetDataObj != null) {
                    Global.getCombatEngine().getCustomData().remove(targetDataKey);
                    ((RangeReducerStats.TargetData)targetDataObj).currRangeMult = 0.5f;
                    targetDataObj = null;
            }
            if (targetDataObj == null || ((RangeReducerStats.TargetData) targetDataObj).target == null) return;

            final RangeReducerStats.TargetData targetData = (RangeReducerStats.TargetData) targetDataObj;
            targetData.currRangeMult = 1f + (RANGE_MULT - 1f) * effectLevel;
            if (targetData.targetEffectPlugin == null) {
                    targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
                            @Override
                            public void advance(float amount, List<InputEventAPI> events) {
                                    if (Global.getCombatEngine().isPaused()) return;
                                    
                                    //If player ship is the target, left side visual
                                    if (targetData.target == Global.getCombatEngine().getPlayerShip()) { 
                                            Global.getCombatEngine().maintainStatusForPlayerShip(KEY_TARGET, 
                                                            targetData.ship.getSystem().getSpecAPI().getIconSpriteName(),
                                                            targetData.ship.getSystem().getDisplayName(), 
                                                            "" + (int)((targetData.currRangeMult - 1f) * 100f) + "% Weapon Range", true);
                                    }
                                                                // <=
                                    if (targetData.currRangeMult >= 1f || !targetData.ship.isAlive()) {
                                            targetData.target.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
                                            targetData.target.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
                                            targetData.target.getMutableStats().getSightRadiusMod().unmodify(id);
                                            Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
                                    } else {
                                            targetData.target.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(id,targetData.currRangeMult);
                                            targetData.target.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(id,targetData.currRangeMult);
                                            targetData.target.getMutableStats().getSightRadiusMod().modifyMult(id, 0.01f);
                                    }
                            }
                    };
                    Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
            }
            
            
            //only visual I think
            if (effectLevel > 0) {
                            if (state != State.IN) {
                                    targetData.elaspedAfterInState += Global.getCombatEngine().getElapsedInLastFrame();
                            }
                            float shipJitterLevel = 0;
                            if (state == State.IN) {
                                    shipJitterLevel = effectLevel;
                            } else {
                                    float durOut = 0.5f;
                                    shipJitterLevel = Math.max(0, durOut - targetData.elaspedAfterInState) / durOut;
                            }
                            float targetJitterLevel = effectLevel;

                            float maxRangeBonus = 50f;
                            float jitterRangeBonus = shipJitterLevel * maxRangeBonus;

                            Color color = JITTER_COLOR;
                            if (shipJitterLevel > 0) {
                                    //ship.setJitterUnder(KEY_SHIP, JITTER_UNDER_COLOR, shipJitterLevel, 21, 0f, 3f + jitterRangeBonus);
                                    ship.setJitter(KEY_SHIP, color, shipJitterLevel, 4, 0f, 0 + jitterRangeBonus * 1f);
                            }

                            if (targetJitterLevel > 0) {
                                    //target.setJitterUnder(KEY_TARGET, JITTER_UNDER_COLOR, targetJitterLevel, 5, 0f, 15f);
                                    targetData.target.setJitter(KEY_TARGET, color, targetJitterLevel, 3, 0f, 5f);
                            }
                    }
    }
    

    public void unapply(MutableShipStatsAPI stats, String id) {
        
    }
    
    
    protected ShipAPI findTarget(ShipAPI ship) {
            float range = getMaxRange(ship);
            boolean player = ship == Global.getCombatEngine().getPlayerShip();
            ShipAPI target = ship.getShipTarget();
            if (target != null) {
                    float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
                    float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
                    if (dist > range + radSum) target = null;
            } else {
                    if (target == null || target.getOwner() == ship.getOwner()) {
                            if (player) {
                                    target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), ShipAPI.HullSize.FIGHTER, range, true);
                            } else {
                                    Object test = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
                                    if (test instanceof ShipAPI) {
                                            target = (ShipAPI) test;
                                            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
                                            float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
                                            if (dist > range + radSum) target = null;
                                    }
                            }
                    }
                    if (target == null) {
                            target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), ShipAPI.HullSize.FIGHTER, range, true);
                    }
            }
            return target;
    }
    
    public static float getMaxRange(ShipAPI ship) {
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
    }
    
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
            if (system.isOutOfAmmo()) return null;
            if (system.getState() != ShipSystemAPI.SystemState.IDLE) return null;

            ShipAPI target = findTarget(ship);
            if (target != null && target != ship) {
                    return "READY";
            }
            if ((target == null) && ship.getShipTarget() != null) {
                    return "OUT OF RANGE";
            }
            return "NO TARGET";
    }
        
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
            //if (true) return true;
            ShipAPI target = findTarget(ship);
            return target != null && target != ship;
    }
    
        
}