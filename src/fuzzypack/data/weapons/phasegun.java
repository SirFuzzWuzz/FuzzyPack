package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;


import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;


import com.fs.starfarer.api.combat.WeaponGroupAPI;

import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import org.lwjgl.util.vector.Vector2f;
//ty me for solving it, juju and banano for previous versions, ruddy for iterator

public class phasegun extends BaseCombatLayeredRenderingPlugin implements OnHitEffectPlugin {
        
        public phasegun() {
	    }
        private final float duration = 7f;
        private final float aoeRange = 300f;
        
        public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
            if (projectile.isFading()) return;
            ArrayList<phasegun>  list = new ArrayList();
            if (!shieldHit && target instanceof ShipAPI) {
                Iterator<Object> iterator = Global.getCombatEngine().getShipGrid().getCheckIterator(projectile.getLocation(), aoeRange, aoeRange);
                while (iterator.hasNext()) {
                ShipAPI otherShip = (ShipAPI) iterator.next();
                    phasegun pg1 = new phasegun(otherShip, duration);
                    list.add(pg1);
                    engine.spawnEmpArc(projectile.getSource(), 
                            point, 
                            projectile, 
                            otherShip, 
                            DamageType.ENERGY, 
                            projectile.getDamageAmount()/2, 
                            projectile.getEmpAmount()/2, 
                            10000f, 
                            "tachyon_lance_emp_impact", 
                            30f, 
                            new Color(150, 50, 160, 255), 
                            new Color(255, 255, 255, 155));
                    for (int i = 0; i < 5; i ++) {
                        engine.spawnEmpArcVisual(point, projectile, otherShip.getLocation(), otherShip, 40f, new Color(150, 50, 160, 150), new Color(255, 255, 255, 100));
                    }
                }
            }
            for (phasegun pg : list) {
                CombatEntityAPI e = engine.addLayeredRenderingPlugin(pg);
            }
            engine.spawnExplosion(point, new Vector2f(0,0), new Color(150, 50, 160, 150), 300f, 1f);
	    }

        protected ShipAPI target;
        protected IntervalUtil interval;
        protected boolean hasTarget = false;
        
        public phasegun(ShipAPI ship, float duration) {
            this.target = ship;
            this.interval = new IntervalUtil(duration,duration);
            this.hasTarget = true;
            
            target.setPhased(true);
            target.setAlphaMult(0.3f);
            for (WeaponGroupAPI weap : target.getWeaponGroupsCopy()) {
                    weap.toggleOff();
            }
            
            target.getMutableStats().getPeakCRDuration().modifyMult("pspace_bad", 0f);
            target.getMutableStats().getTimeMult().modifyMult("pgun", 0.5f);
            target.getMutableStats().getCRLossPerSecondPercent().modifyMult("pgun", 3f);
        }
        
        public void advance(float amount) { //, CombatEngineAPI engine, WeaponAPI weapon public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
            if (Global.getCombatEngine().isPaused()) return;
            
            if (hasTarget) {
                interval.advance(amount);
                //Color color = new Color(1, 1, 1, 50);
                Color color_u = new Color(190, 100, 200, 150);
                //target.setJitter(target, color , 2f, 3, 5f);
                target.setJitterUnder(target, color_u , 2f, 3, 5f);
                target.blockCommandForOneFrame(ShipCommand.FIRE);
                target.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                if (interval.intervalElapsed()) {
                    this.target.setPhased(false);
                    this.hasTarget = false;
                    this.target.setJitterUnder(target, Color.MAGENTA, 1f, 3, 0f);
                    target.setAlphaMult(1f);
                    target.getMutableStats().getPeakCRDuration().unmodify("pspace_bad");
                    target.getMutableStats().getTimeMult().unmodify("pgun");
                    target.getMutableStats().getCRLossPerSecondPercent().unmodify("pgun");
                }
                
            }
        } 
        
        public void init(CombatEntityAPI entity) {
		super.init(entity);
	}

        public boolean isExpired() {
		    return !target.isAlive() || !Global.getCombatEngine().isEntityInPlay(target) || !hasTarget;
	    }

}
