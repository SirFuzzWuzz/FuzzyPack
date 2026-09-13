package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import fuzzypack.data.weapons.effects.PhaseBuildUp;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Iterator;

public class phasegun implements OnHitEffectPlugin {

    private static final float DURATION = 7f;
    private static final float AOE = 300f;
    private static final int STACKS = 10; // trips PhaseBuildUp

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult,
                      CombatEngineAPI engine) {
        if (shieldHit || !(target instanceof ShipAPI)) return;

        Color fringe = new Color(150, 50, 160, 255);
        Color core = new Color(255, 255, 255, 155);

        Iterator<Object> it = engine.getShipGrid().getCheckIterator(point, AOE, AOE);
        while (it.hasNext()) {
            Object o = it.next();
            if (!(o instanceof ShipAPI)) continue;
            ShipAPI other = (ShipAPI) o;
            if (!other.isAlive()) continue;

            PhaseBuildUp build = PhaseBuildUp.get(other);
            for (int i = 0; i < STACKS; i++) build.addStack(DURATION);

            engine.spawnEmpArc(projectile.getSource(), point, projectile, other,
                    DamageType.ENERGY,
                    projectile.getDamageAmount() / 2f,
                    projectile.getEmpAmount() / 2f,
                    10000f, "tachyon_lance_emp_impact",
                    30f, fringe, core);
            for (int i = 0; i < 5; i++) {
                engine.spawnEmpArcVisual(point, projectile, other.getLocation(), other,
                        40f, new Color(150, 50, 160, 150), new Color(255, 255, 255, 100));
            }
        }
        engine.spawnExplosion(point, new Vector2f(), new Color(150, 50, 160, 150), 300f, 1f);
    }
}