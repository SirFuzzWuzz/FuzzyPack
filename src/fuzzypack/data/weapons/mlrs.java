package fuzzypack.data.weapons;


import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;



import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.MagicFakeBeam;
import data.scripts.util.MagicTargeting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;


public class mlrs extends BaseCombatLayeredRenderingPlugin implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

        
        private final float sweepAngle = 30;
        private final float chargeupDur = 1.5f; //check csv, curr = 3
        
        private float currAngle;
        private float prevCharge = 0;
        
        private ArrayList<ShipAPI> targetList; // = new Arraylist<ShipAPI>();
        
        public mlrs() {}
   
        
            @Override
        public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
            engine.removeEntity(projectile);
            prevCharge = 0;
            
            targetList = detector(weapon.getFirePoint(0), weapon.getCurrAngle(), weapon, engine);
            
            if (!targetList.isEmpty()) {
                for (ShipAPI trgt: targetList) {

                        MissileAPI missile = (MissileAPI) engine.spawnProjectile(weapon.getShip(), weapon, "fp_mlrs", weapon.getLocation(), weapon.getCurrAngle(), weapon.getShip().getVelocity());
                        GuidedMissileAI mAi = (GuidedMissileAI) missile.getAI();
                        mAi.setTarget(trgt);

                }
            }
                
            //To render the marked targets
            mlrs pg = new mlrs(targetList, engine);
            CombatEntityAPI e = engine.addLayeredRenderingPlugin(pg);
            
        }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        if (weapon.getChargeLevel() > prevCharge) {
            
            float angleSpeed = (sweepAngle/chargeupDur)/60;
            
            
            MagicFakeBeam.spawnFakeBeam(engine,
                                        weapon.getLocation(),        //could reserve an offset                          
                                        weapon.getRange(),                                  
                                        currAngle,                                  
                                        3,         //width                 
                                        0.06f,         //full                         
                                        0.3f,       //fading                           
                                        0,          //impact size                        
                                        Color.white, //core color                                 
                                        Color.green,   //fringe color                               
                                        0,              //damage                                
                                        DamageType.ENERGY, //damage type                                 
                                        0,                 //emp                         
                                        weapon.getShip()); //source
            currAngle += angleSpeed;
            
            //if (weapon.getChargeLevel() == 0.5f) {}
            
            
            
            /*if (weapon.getChargeLevel() == 1f && targetList != null) {
            
            for (ShipAPI trgt: targetList) {
            
            MissileAPI missile = (MissileAPI) engine.spawnProjectile(weapon.getShip(), weapon, "fp_mlrs", weapon.getLocation(), weapon.getCurrAngle(), weapon.getShip().getVelocity());
            GuidedMissileAI mAi = (GuidedMissileAI) missile.getAI();
            mAi.setTarget(trgt);
            
            }
            }*/
            
        } else {
            currAngle = weapon.getCurrAngle() - sweepAngle;
        }
        
        prevCharge = weapon.getChargeLevel();
        
    }

    
    private ArrayList<ShipAPI> detector(Vector2f from, float angle, WeaponAPI weapon, CombatEngineAPI engine) {
        
        ArrayList<ShipAPI> list = new ArrayList<ShipAPI>();
        

        Iterator iter = engine.getShipGrid().getCheckIterator(from, weapon.getRange() + 200, weapon.getRange() + 200);
        //Vector2f point = MathUtils.getPoint(from, weapon.getRange(), angle);
        //engine.addFloatingText(point, "X", 50, Color.red, weapon.getShip(), 5f, 1f);
       
        
        while (iter.hasNext()) {
            ShipAPI nextShip = (ShipAPI) iter.next();
            
            //engine.addFloatingText(weapon.getLocation(), "Angle: " + VectorUtils.getAngle(point, nextShip.getLocation()), 50, Color.yellow, weapon.getShip(), 5f, 1f);
            Vector2f shipLoc = nextShip.getLocation();
            
            if (Misc.isInArc(angle, sweepAngle*2, from, shipLoc) && !nextShip.isAlly()) {
                
                list.add(nextShip);
                
            }
            
        }
        
        return list;
    }
        

    protected ArrayList<ShipAPI> list;
    protected CombatEngineAPI engine;
    protected boolean done = false;
    protected IntervalUtil interval;
    
    private mlrs(ArrayList<ShipAPI> list, CombatEngineAPI engine) {
        this.list = list;
        this.engine = engine;
        this.interval = new IntervalUtil(4,4);
        
    }
    
        @Override
    public void advance(float amount) {
        
        for (ShipAPI ship: list) {
            if (!ship.isAlive()) done = true;
            engine.addFloatingText(ship.getLocation(), "X", 50, Color.red, ship, 0.06f, 0.06f);
        }
        
        this.interval.advance(amount);
        if (interval.intervalElapsed()) done = true;
        
    }
    
        @Override
    public void init(CombatEntityAPI entity) {
		super.init(entity);
	}
        
        @Override
    public boolean isExpired() {
            return done;
    }
    
}
