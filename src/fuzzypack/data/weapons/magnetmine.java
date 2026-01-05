package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;



public class magnetmine implements OnFireEffectPlugin {
    
    private final float maxDist = 650f;
    private final float acc = 1.6f;


    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        weapon.getShip().addListener(new listener(projectile));
    }

    
    class listener implements AdvanceableListener {
        
        CombatEngineAPI engine = Global.getCombatEngine();
        DamagingProjectileAPI proj;
        
        public listener(DamagingProjectileAPI proj) {
            this.proj = proj;
        }
        
        @Override
        public void advance(float amount) {
            if (proj.isExpired() || proj.didDamage() || !engine.isEntityInPlay(proj)) {
                return; 
            }
            
            ShipAPI trgt = AIUtils.getNearestEnemy(proj);

            if (trgt != null && trgt.getHullSize() != ShipAPI.HullSize.FIGHTER && proj.getElapsed() > 5f) {
                float dist = MathUtils.getDistance(proj.getLocation(), trgt.getLocation());
                if (dist < maxDist) {
                    Vector2f trgtVec = trgt.getLocation();
                    Vector2f projVec = proj.getLocation();

                    float deltaX = trgtVec.x - projVec.x;
                    float deltaY = trgtVec.y - projVec.y;

                    float accX = (deltaX/dist) * acc;
                    float accY = (deltaY/dist) * acc;

                    proj.getVelocity().set(proj.getVelocity().x + accX, proj.getVelocity().y + accY);
                    
                    MagicFakeBeam.spawnFakeBeam(Global.getCombatEngine(),
                                        proj.getLocation(),        //could reserve an offset                          
                                        MathUtils.getDistance(proj.getLocation(), trgt.getLocation()),                                  
                                        VectorUtils.getAngle(proj.getLocation(), trgt.getLocation()),                                  
                                        5,         //width                 
                                        0.03f,         //full                         
                                        0.1f,       //fading                           
                                        5,          //impact size                        
                                        Color.BLACK, //core color                                 
                                        new Color(255,50,50,70),   //fringe color                               
                                        0,              //damage                                
                                        DamageType.ENERGY, //damage type                                 
                                        0,                 //emp                         
                                        proj.getSource()); //source
                    
                }
            }
            
            
        } //advance
        
    }

}