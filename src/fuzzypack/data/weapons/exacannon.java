package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;

import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;


import org.lwjgl.util.vector.Vector2f;

public class exacannon extends BaseCombatLayeredRenderingPlugin implements OnHitEffectPlugin, OnFireEffectPlugin {
    
        private float selfDamage = 300f;

        
        public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
            engine = Global.getCombatEngine();
            engine.addSwirlyNebulaParticle(
                    point, //loc 
                    new Vector2f(0,0), //vel
                    200f, //size
                    5f, //endsize
                    0.2f, //rampup frac
                    0.75f, //fullbright frac
                    2.5f, //total
                    new Color(150,100,255,255),
                    false);
            engine.addSmoothParticle(
                    point,
                    new Vector2f(0,0), 
                    100f, 
                    10f, 
                    4, 
                    new Color(150,100,255,100));
            
            
            DamagingExplosionSpec exp = new DamagingExplosionSpec(
                    0.5f, //dur
                    (Float) 500f, //rad
                    (Float) 150f, //core
                    projectile.getDamageAmount() * 0.2f, //maxDmg 1000f
                    projectile.getDamageAmount() * 0.1f, //minDmg 500f
                    CollisionClass.MISSILE_FF, //collision
                    CollisionClass.MISSILE_FF, //fighter collision
                    0.5f, //min particle size
                    10f, // particle size range
                    2f, //particle dur
                    10, //particle count
                    new Color(150,100,255,200), //particle color
                    new Color(150,100,255,255)); //explosion color
            
            engine.spawnDamagingExplosion(exp, projectile.getSource(), point);
            
            Global.getSoundPlayer().playSound("exacannon_exp", 1, 0.8f, point, new Vector2f(0,0));
	}

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        
        int noArcs = MathUtils.getRandomNumberInRange(3, 7);
        for (int i = 0; i < noArcs; i++) {
            Vector2f trgtLoc = weapon.getShip().getArmorGrid().getLocation(
                    MathUtils.getRandomNumberInRange(0, weapon.getShip().getArmorGrid().getGrid().length-1),
                    MathUtils.getRandomNumberInRange(0, weapon.getShip().getArmorGrid().getGrid()[0].length-1));
          
            engine.spawnEmpArc(
                                projectile.getSource(), trgtLoc, projectile.getSource(), projectile.getSource(),
                                DamageType.ENERGY, 
                                selfDamage, // damage
                                50, // emp 
                                100000f, // max range 
                                "tachyon_lance_emp_impact",
                                5f,
                                new Color (100,100,255,255),
                                Color.black
                                );
        }
    }

}
