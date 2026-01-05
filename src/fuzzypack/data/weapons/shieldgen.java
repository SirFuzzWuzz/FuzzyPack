package fuzzypack.data.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;


public class shieldgen implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

        public shieldgen() {
	    }
        public boolean once = true;
        public ShipAPI drone;
        public Color shieldColor; //= new Color(80,60,200,200);
        private boolean wpnDisabled = false;
        //for EMP visual
        public IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);
        public IntervalUtil intShieldOff = new IntervalUtil(1.5f,1.5f);


        @Override
        public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
            //Spawn the drone
            if (once) {
               //Pick drone depending on which weapon version
                if (weapon.getSpec().getSize() == WeaponSize.LARGE) {
                    ShipHullSpecAPI spec = Global.getSettings().getHullSpec("fp_shielddrone");
                    ShipVariantAPI variant = Global.getSettings().createEmptyVariant("fp_shielddrone_base", spec);
                    drone = engine.createFXDrone(variant);
                    engine.addEntity(drone);
                    once = false;
                } else {
                    ShipHullSpecAPI spec = Global.getSettings().getHullSpec("fp_shielddroneM");
                    ShipVariantAPI variant = Global.getSettings().createEmptyVariant("fp_shielddroneM_base", spec);
                    drone = engine.createFXDrone(variant);
                    engine.addEntity(drone);
                    once = false;
                }
                //Lobotomize
                drone.setShipAI(null);
                //No collision with host ship
                drone.setCollisionClass(CollisionClass.FIGHTER);
                //drone.setHullSize(ShipAPI.HullSize.FIGHTER);
                drone.getShield().setRadius(weapon.getRange()/3);
                drone.setCollisionRadius(weapon.getRange()/3);

                //Better shield if a missile slot was used
                WeaponType type = weapon.getSlot().getWeaponType();
                if (type != WeaponType.BALLISTIC && type != WeaponType.ENERGY && type != WeaponType.HYBRID) {
                    drone.getMutableStats().getShieldDamageTakenMult().modifyMult("shieldgen_id", 0.8f);
                }
                //Color
                shieldColor = drone.getShield().getInnerColor();
                drone.getShield().setRingColor(shieldColor.brighter());
                drone.getShield().setInnerColor(shieldColor.brighter());
                once = false;
            }
            if (!weapon.getShip().isAlive()) engine.removeEntity(drone);
            //Turn shield on/off
            if (weapon.isFiring()) {
                intShieldOff.setElapsed(0);
                if (drone.getShield().isOff()) {
                    //drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    drone.getShield().toggleOn();
                }
            } else if (!weapon.isFiring() && drone.getShield().isOn()) {
                if (intShieldOff.intervalElapsed()) {
                    //drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    drone.getShield().toggleOff();
                }
                intShieldOff.advance(amount);
            }
            if (drone.getFluxTracker().isOverloaded()) {
                if (!wpnDisabled) {
                    weapon.disable();
                    wpnDisabled = true;
                } else if (!weapon.isDisabled()) {
                    weapon.repair();
                    drone.getFluxTracker().setOverloadProgress(1f);
                    wpnDisabled = false;
                }
            }
            //Move the drone to follow the weapon
            drone.getLocation().set(weapon.getLocation().x, weapon.getLocation().y);
            drone.setFacing(weapon.getCurrAngle());

            //EMP visual
            if (weapon.isFiring() && interval.intervalElapsed()) {
                float rad = drone.getShield().getRadius();
                Vector2f onRad = MathUtils.getPointOnCircumference(weapon.getLocation(), rad-10f,
                        weapon.getCurrAngle() + MathUtils.getRandomNumberInRange(-drone.getShield().getArc()/2, drone.getShield().getArc()/2));
                engine.spawnEmpArcVisual(weapon.getFirePoint(MathUtils.getRandomNumberInRange(0, 4)), drone, onRad, drone, 10f, 
                        new Color(0,0,10,50), shieldColor);
            }
            interval.advance(amount);
        }
        
        @Override
        public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
            engine.removeEntity(projectile);
        }
}