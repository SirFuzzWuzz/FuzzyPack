package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;


import java.awt.*;


public class prownovacannon implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private boolean disabled = false;

    private boolean once = true;
    private float prevChargeLevel = 0;

    private float distance = 100;
    private Vector2f point = new Vector2f();

    private IntervalUtil visualInterval = new IntervalUtil(0.1f,0.2f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || !weapon.getShip().isAlive() || disabled) return;

        //weapon.getSprite().setAlphaMult(1f);

        if (weapon.isDisabled()) {
            weapon.repair();
        }

        if (weapon.getShip().getChildModulesCopy().isEmpty() || !weapon.getShip().getChildModulesCopy().get(0).isAlive()) {
            weapon.disable(true);
            disabled = true;
            weapon.getSprite().setColor(new Color(1,1,1,0));
        }

        WeaponGroupAPI grp = weapon.getShip().getWeaponGroupFor(weapon);
        ShipAPI ship = weapon.getShip();
        if (weapon.getChargeLevel() > 0 && once) {
            once = false;

            //Player control
            if (ship.getAI() == null) {
                distance = MathUtils.getDistance(weapon.getLocation(), ship.getMouseTarget());

                //If the player uses autofire
                if (grp != weapon.getShip().getSelectedGroupAPI()) {
                    distance = MathUtils.getDistance(weapon.getLocation(), grp.getAutofirePlugin(weapon).getTarget());
                }

            } else { //AI control
                distance = MathUtils.getDistance(weapon.getLocation(), ship.getShipTarget().getLocation());

                //If the player uses autofire
                if (grp != weapon.getShip().getSelectedGroupAPI()) {
                    distance = MathUtils.getDistance(weapon.getLocation(), grp.getAutofirePlugin(weapon).getTarget());
                }
            }

            if (distance > weapon.getRange()) {
                point = MathUtils.getPointOnCircumference(weapon.getLocation(), weapon.getRange(), weapon.getCurrAngle());
            } else {
                point = MathUtils.getPointOnCircumference(weapon.getLocation(), distance, weapon.getCurrAngle());
            }

            /*mine = (MissileAPI) engine.spawnProjectile(weapon.getShip(),
                    null,
                    "fp_novacannon_spawn",
                    point,
                    90, null);*/

        }

        //Slow ship while firing
        if (weapon.getChargeLevel() > prevChargeLevel) {
            //weapon.setCurrAngle(VectorUtils.getAngle(weapon.getLocation(), point));
            ship.getVelocity().scale((float) Math.sqrt(1 - weapon.getChargeLevel()));
            ship.setAngularVelocity(ship.getAngularVelocity() - (weapon.getChargeLevel() * ship.getAngularVelocity()));

            //Lock weapon angle and move target area if ship is moved
            weapon.setCurrAngle(weapon.getCurrAngle());
            point = MathUtils.getPointOnCircumference(weapon.getLocation(), distance, weapon.getCurrAngle());

            //Visuals
            MagicRender.battlespace(
                    Global.getSettings().getSprite("markers", "circle"), //"artillery_shell"
                    point,
                    new Vector2f(),
                    new Vector2f(300 * weapon.getChargeLevel(), 300 * weapon.getChargeLevel()),
                    new Vector2f(0,0),
                    90f,
                    0f,
                    new Color(255,50,50,220),
                    true,
                    0.01f,
                    0.02f,
                    0f);
            MagicRender.battlespace(
                    Global.getSettings().getSprite("markers", "circle"), //"artillery_shell"
                    point,
                    new Vector2f(),
                    new Vector2f(300, 300),
                    new Vector2f(0,0),
                    90f,
                    0f,
                    new Color(255,70,50,240),
                    true,
                    0.01f,
                    0.02f,
                    0f);

            visualInterval.advance(amount);
            if (visualInterval.intervalElapsed()) {
                engine.spawnEmpArcVisual(weapon.getFirePoint(0),
                        null,
                        MathUtils.getRandomPointInCircle(weapon.getFirePoint(0), MathUtils.getRandomNumberInRange(10, 100)),
                        null,
                        3,
                        new Color(100, 100, 255, 200),
                        Color.white);
            }

        }
        prevChargeLevel = weapon.getChargeLevel();

        if (weapon.getChargeLevel() == 0) {
            once = true;
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(projectile);

        DamagingExplosionSpec exp = new DamagingExplosionSpec(
                0.5f, //dur
                280f, //rad
                100f, //core
                projectile.getDamageAmount(), //maxDmg
                projectile.getDamageAmount()*0.5f, //minDmg
                CollisionClass.MISSILE_FF, //collision
                CollisionClass.MISSILE_FF, //fighter collision
                0.5f, //min particle size
                10f, // particle size range
                2f, //particle dur
                10, //particle count
                new Color(50,100,255,200), //particle color
                new Color(100,100,255,255)); //explosion color
        engine.spawnDamagingExplosion(exp, projectile.getSource(), point);

        engine.addSwirlyNebulaParticle(
                point, //loc
                new Vector2f(0,0), //vel
                400f, //size
                1.2f, //endsize mult?
                0.2f, //rampup frac
                1f, //fullbright frac
                3f, //total
                new Color(20,100,200,255),
                false);

        /*MagicFakeBeam.spawnFakeBeam(engine,
                weapon.getFirePoint(0),        //could reserve an offset
                MathUtils.getDistance(weapon.getFirePoint(0), point),
                VectorUtils.getAngle(weapon.getFirePoint(0), point),
                25,         //width
                0.5f,         //full
                0.3f,       //fading
                0,          //impact size
                Color.white, //core color
                Color.blue,   //fringe color
                0,              //damage
                DamageType.ENERGY, //damage type
                0,                 //emp
                weapon.getShip()); //source */

        for (int i =0;  i < 10; i++) {
            engine.addNebulaParticle(point,
                    VectorUtils.getDirectionalVector(point, MathUtils.getRandomPointInCircle(point, 10)),
                    10,1, 0.5f, 1f, 4, Color.blue);

            engine.spawnEmpArcVisual(weapon.getFirePoint(0),
                    null,
                    MathUtils.getRandomPointInCircle(point, MathUtils.getRandomNumberInRange(1, 150)),
                    null,
                    3,
                    new Color(100, 100, 255, 150),
                    Color.white);
        }
    }
}
