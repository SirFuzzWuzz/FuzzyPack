package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import java.awt.Color;

import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;



public class magnetmine implements OnFireEffectPlugin {

    private static final float ARMING_TIME = 3f;
    private static final float MAX_DIST = 650f;
    private static final float ACC = 80f;


    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        weapon.getShip().addListener(new listener(projectile));
    }

    
    class listener implements AdvanceableListener {
        
        CombatEngineAPI engine = Global.getCombatEngine();
        DamagingProjectileAPI mine;
        private final IntervalUtil beamInterval = new IntervalUtil(0.07f, 0.07f);
        
        public listener(DamagingProjectileAPI proj) {
            this.mine = proj;
        }
        
        @Override
        public void advance(float amount) {
            if (mine == null || mine.didDamage() || mine.isFading() || !engine.isEntityInPlay(mine)) return;
            if (mine.getElapsed() <= ARMING_TIME) return;

            ShipAPI trgt = AIUtils.getNearestEnemy(mine);
            if (trgt == null || !trgt.isAlive()) return;
            if (trgt.getHullSize() == ShipAPI.HullSize.FIGHTER) return;

            float dist = MathUtils.getDistance(mine, trgt);
            if (dist <= 1f || dist > MAX_DIST) return;

            Vector2f loc = mine.getLocation();
            Vector2f tLoc = trgt.getLocation();
            float inv = 1f / dist;
            float dx = (tLoc.x - loc.x) * inv;
            float dy = (tLoc.y - loc.y) * inv;

            Vector2f vel = mine.getVelocity();
            vel.set(vel.x + dx * ACC * amount, vel.y + dy * ACC * amount);

            beamInterval.advance(amount);
            if (!beamInterval.intervalElapsed()) return;

            MagicFakeBeam.spawnFakeBeam(
                    Global.getCombatEngine(),
                    loc,
                    dist,
                    VectorUtils.getAngle(loc, tLoc),
                    5f,
                    0.03f,
                    0.1f,
                    5f,
                    Color.BLACK,
                    new Color(255, 50, 50, 70),
                    0f,
                    DamageType.ENERGY,
                    0f,
                    mine.getSource()
            );
        }
    }
}