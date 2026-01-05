package fuzzypack.data.weapons.onHit;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class smokebomb implements OnHitEffectPlugin { //, OnFireEffectPlugin


    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        projectile.getWeapon().getShip().addListener(new listener(projectile, projectile.getWeapon(), engine, point));

        engine.addSwirlyNebulaParticle(
                point, //loc
                new Vector2f(0,0), //vel
                200f, //size
                2f, //endsize mult?
                0.2f, //rampup frac
                10f, //fullbright frac
                12f, //total
                new Color(200,200,200,200),
                false);
    }


    //public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {}

    class listener implements AdvanceableListener {

        protected MissileAPI missile;
        protected WeaponAPI weapon;
        protected CombatEngineAPI engine;
        protected Vector2f point;

        protected IntervalUtil interval;

        public listener(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine, Vector2f point) {
            this.missile = (MissileAPI) projectile;
            this.weapon = weapon;
            this.engine = engine;
            this.point = point;
            this.interval = new IntervalUtil(5f,5f);
        }


        @Override
        public void advance(float amount) {
            //engine.getFleetManager(weapon.getShip().getOwner()).spawnShipOrWing("fp_scarydummy", missile.getLocation(), 0f);


            interval.advance(amount);
            if (interval.intervalElapsed()) return;
        }
    }

}
