package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;



public class tether implements OnHitEffectPlugin {

    private final float minDist = 700f;
    private final IntervalUtil interval = new IntervalUtil(15f,15f);
    private final IntervalUtil visualInterval = new IntervalUtil(0.3f,0.5f);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof ShipAPI && !shieldHit && MathUtils.getDistance(projectile.getWeapon().getLocation(), target.getLocation()) < minDist) {
            projectile.getWeapon().getShip().addListener(new listener(projectile, (ShipAPI) target, point));
        }
    } //onHit
    
    
    
    class listener implements AdvanceableListener {
        
        ShipAPI target;
        ShipAPI host;
        WeaponAPI weap;
        DamagingProjectileAPI proj;
        
        float impactDist;
        float projOffset;
        float impactOffset;

        
        
        public listener(DamagingProjectileAPI proj, ShipAPI target, Vector2f point) {
            this.target = target;
            this.host = proj.getWeapon().getShip();
            this.weap = proj.getWeapon();
            this.proj = proj;

            this.impactDist = MathUtils.getDistance(point, target.getLocation());
            this.projOffset = VectorUtils.getAngle(point, target.getLocation()) - proj.getFacing();
            this.impactOffset = VectorUtils.getAngle(target.getLocation(), point) - target.getFacing();
        }
        
        
        @Override
        public void advance(float amount) {
            if (interval.intervalElapsed()) return;
            interval.advance(amount);
            
            //physics
            float dist = MathUtils.getDistance(host.getLocation(), target.getLocation());
            if (dist >= minDist) {


                float dx = target.getLocation().x - host.getLocation().x;
                float dy = target.getLocation().y - host.getLocation().y;
                
                float force = (dist - minDist) * 500f; // times stiffness
                
                float totMass = host.getMass() + target.getMass();
                
                float hostRatio = target.getMass() / totMass;
                float targetRatio = host.getMass() / totMass;
                
                float forceX = force * (dx / dist);
                float forceY = force * (dy / dist);
                
                host.getVelocity().set(host.getVelocity().x + ((forceX * hostRatio) / host.getMass()), 
                        host.getVelocity().y + ((forceY * hostRatio) / host.getMass()));
                target.getVelocity().set(target.getVelocity().x - ((forceX * targetRatio) / target.getMass()), 
                        target.getVelocity().y - ((forceY * targetRatio) / target.getMass()));
            }
            
            
            //Visuals

            Vector2f projVector = MathUtils.getPointOnCircumference(target.getLocation(), 
                    impactDist, target.getFacing() + impactOffset);

            if (visualInterval.intervalElapsed()) {
                Global.getCombatEngine().spawnEmpArcVisual(weap.getFirePoint(0), host, projVector, target, 6f, Color.red, Color.black);
            }
            visualInterval.advance(amount);

            MagicRender.battlespace(
                Global.getSettings().getSprite("projectiles", "tether_missile"),
                projVector,
                new Vector2f(),
                new Vector2f(10 ,21),
                new Vector2f(0,0),
                VectorUtils.getAngle(projVector, target.getLocation()) - 90f, //VectorUtils.getAngle(projVector, target.getLocation())
                0f,
                new Color(255,255,255,240),
                false,
                0.01f,
                0.02f,
                0.01f);

        }
    }
    


}