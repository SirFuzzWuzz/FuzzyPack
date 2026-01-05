package fuzzypack.data.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;


//import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;


import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

import org.lazywizard.lazylib.MathUtils;

import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.VectorUtils;
//import com.fs.starfarer.api.util.IntervalUtil;
//import com.fs.starfarer.api.util.Misc;

public class artillery extends BaseCombatLayeredRenderingPlugin implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private final float minRange = 800;
    private final float hitVariance = 120f;
    private final int crosshair_alpha = 120;
    //private final float projspeed = 400;
    private Vector2f crosshairLoc;
    private final float crossairSpeed = 4;
    private int move = 0;
    private boolean once = true;
    
    private final IntervalUtil renderInterval = new IntervalUtil(0.03f,0.03f);
    
    public artillery() {}
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCombatEngine() == null || engine.isPaused() || !weapon.getShip().isAlive()) {
            return;
        }
        float wpnFacing = weapon.getCurrAngle();
        //wpnFacing = (float) (wpnFacing * Math.PI / 180);
        Vector2f wpnLoc = weapon.getLocation();
        //set initial location
        if (once) {
            crosshairLoc = MathUtils.getPointOnCircumference(wpnLoc, minRange + 50, wpnFacing);
            once = false;
        }
        //Only move crosshair towards mouse/target if it's within bounds
        if (MathUtils.isWithinRange(weapon.getLocation(), crosshairLoc, weapon.getRange()) &&
                                !MathUtils.isWithinRange(weapon.getLocation(), crosshairLoc, minRange)) {
            WeaponGroupAPI grp = weapon.getShip().getWeaponGroupFor(weapon);
            Vector2f towards;
            //Mouse control when player is in control and not autopiloting
            if (weapon.getShip().getAI() == null) {
                
                towards = weapon.getShip().getMouseTarget();
                
                //If the player uses autofire
                if (grp != weapon.getShip().getSelectedGroupAPI()) {
                    towards = grp.getAutofirePlugin(weapon).getTarget();
                }
            //AI aim assist 
            } else if (weapon.getShip().getShipTarget() != null) { //should cover AI controlled ship

                towards = weapon.getShip().getShipTarget().getLocation();
                
                if (grp != weapon.getShip().getSelectedGroupAPI()){
                    towards = grp.getAutofirePlugin(weapon).getTarget();
                }
            //AI controlled but no target
            } else {
                towards = MathUtils.getPointOnCircumference(wpnLoc, MathUtils.getDistance(wpnLoc, crosshairLoc), wpnFacing);
            }
            //Direction to move
            if (towards != null && MathUtils.getDistance(crosshairLoc, towards) > 5) {
                if (MathUtils.getDistance(weapon.getLocation(), towards) > MathUtils.getDistance(weapon.getLocation(), crosshairLoc)) {
                        move = 1;
                } else {
                        move = -1;
                }
            } else {
                move = 0;
            }
            //Move the crosshair
            crosshairLoc = MathUtils.getPointOnCircumference(wpnLoc, 
                    MathUtils.getDistance(weapon.getLocation(), crosshairLoc) + crossairSpeed*move, wpnFacing);
        }
        
        //Keep crosshair within boundaries
        if (MathUtils.getDistance(weapon.getLocation(), crosshairLoc) > weapon.getRange()){
            crosshairLoc = MathUtils.getPointOnCircumference(wpnLoc, weapon.getRange() -5f, wpnFacing);
        } else if (MathUtils.getDistance(weapon.getLocation(), crosshairLoc) < minRange) {
            crosshairLoc = MathUtils.getPointOnCircumference(wpnLoc, minRange +5f, wpnFacing);
        }
        //Render the crosshair
        if (renderInterval.intervalElapsed() && weapon.getShip() == engine.getPlayerShip()) {
            //engine.addFloatingText(crosshairLoc, "X", 50, Color.yellow, weapon.getShip(), 0.01f, 1f);
            //if (weapon.getShip() != engine.getPlayerShip()) crosshair_alpha = crosshair_alpha/2;
            MagicRender.battlespace(
                Global.getSettings().getSprite("markers", "artillery_crosshair"),
                crosshairLoc,
                new Vector2f(),
                new Vector2f(150,150),
                new Vector2f(0,0),
                weapon.getCurrAngle(),
                0f,
                new Color(200,50,50,crosshair_alpha),
                false,
                0.03f,
                0.03f,
                0.01f);
        }
        renderInterval.advance(amount);
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(projectile);
        //projectile.setCollisionClass(CollisionClass.NONE);
        float t = MathUtils.getDistance(weapon.getFirePoint(0), crosshairLoc) / weapon.getProjectileSpeed(); // t = d/v
        interval = new IntervalUtil(t, t);
        //Add inaccuracy
        Vector2f hitPos = new Vector2f(crosshairLoc.x + MathUtils.getRandomNumberInRange(-hitVariance, hitVariance), 
                                        crosshairLoc.y + MathUtils.getRandomNumberInRange(-hitVariance, hitVariance));

        MissileAPI mine = (MissileAPI) engine.spawnProjectile(weapon.getShip(), 
                                                            null, 
                                                            "fp_artillery_spawn", 
                                                            hitPos, 
                                                            90, null);
        mine.setDamageAmount(weapon.getDamage().getDamage());
        //mine.getSpec().setArmingTime(t);
        mine.setCollisionClass(CollisionClass.NONE);
        // interval,
        artillery pg = new artillery(hitPos, engine, weapon, mine);
        CombatEntityAPI e = engine.addLayeredRenderingPlugin(pg);
    }
    
    //I don't know what I need so get everything
    protected IntervalUtil interval;
    protected Vector2f hitLoc;
    protected boolean fired;
    protected CombatEngineAPI engine;
    protected WeaponAPI weapon;
    protected MissileAPI mine; 
    protected Vector2f dummyLoc;
    //protected Vector2f midpoint;
    protected float distance;
    protected float angleSnap;
    protected float theta;
    protected float time;
    protected float space_gravity;
                                    //IntervalUtil interval, 
    public artillery(Vector2f hitLoc, CombatEngineAPI engine, WeaponAPI weapon, MissileAPI mine) {
        //this.interval = interval;
        this.hitLoc = hitLoc;
        this.fired = false;
        this.engine = engine;
        this.weapon= weapon;
        this.mine = mine;
        this.angleSnap = weapon.getCurrAngle();
        this.dummyLoc = weapon.getFirePoint(0);
        //this.midpoint = MathUtils.getMidpoint(weapon.getFirePoint(0), hitLoc);
        this.distance = MathUtils.getDistance(weapon.getFirePoint(0), hitLoc);
        this.space_gravity = 60f;
        float maxSpeed = (float) (weapon.getProjectileSpeed()*Math.cos(Math.PI/4));
        float maxTime = (float) (2*weapon.getProjectileSpeed()*Math.sin(Math.PI/4)/space_gravity);
        float maxRange = (float) (maxSpeed*maxTime);
        if (distance > maxRange) this.space_gravity = 35;
        
        this.theta = (float) (0.5*Math.asin((space_gravity*distance)/(weapon.getProjectileSpeed()*weapon.getProjectileSpeed())));
        
        float x_speed = (float) (weapon.getProjectileSpeed()*Math.cos(theta));
        
        this.time = distance/x_speed;
        
        this.interval = new IntervalUtil(time,time);
    }
    
    @Override
    public void advance(float amount) {
        dummyLoc = MathUtils.getPoint(dummyLoc, (float) (weapon.getProjectileSpeed()*Math.cos(theta))/60f, VectorUtils.getAngle(dummyLoc, hitLoc));
        //float size = 0.1f* (float) (distance - MathUtils.getDistance(midpoint, dummyLoc));
        float size = (float) (weapon.getProjectileSpeed()*Math.sin(theta)*interval.getElapsed() - 0.5f*space_gravity*interval.getElapsed()*interval.getElapsed())*1.0f;
        size = size+35;
        String shell;
        if (interval.getElapsed() < interval.getMaxInterval()/5) {
            shell = "artyshell0";
        } else if (interval.getElapsed() > interval.getMaxInterval()/5 && interval.getElapsed() < 2*interval.getMaxInterval()/5) {
            shell = "artyshell1";
        } else if (interval.getElapsed() > 2*interval.getMaxInterval()/5 && interval.getElapsed() < 3*interval.getMaxInterval()/5) {
            shell = "artyshell2";
        } else if (interval.getElapsed() > 3*interval.getMaxInterval()/5 && interval.getElapsed() < 4*interval.getMaxInterval()/5) {
            shell = "artyshell3";
        } else {
            shell = "artyshell4";
        }
        MagicRender.battlespace(
                Global.getSettings().getSprite("projectiles", shell), //"artillery_shell"
                dummyLoc,
                new Vector2f(),
                new Vector2f(size,size),
                new Vector2f(0,0),
                angleSnap-90f,
                0f,
                new Color(255,255,255,240),
                false,
                0.01f,
                0.02f,
                0.01f);
        if (interval.intervalElapsed()) {
            mine.explode();
            engine.removeEntity(mine);
            fired = true;
        }
        interval.advance(amount);
    }

    public void init(CombatEntityAPI entity) {
		super.init(entity);
	}
        
    public boolean isExpired() {
            return fired;
    }
    

}
