package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

public class mlrs implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private static final float SWEEP_ANGLE = 45f;

    private float prevCharge = 0f;
    private final ArrayList<ShipAPI> queue = new ArrayList<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) return;

        float charge = weapon.getChargeLevel();
        if (charge > prevCharge) {
            float sweepAng = weapon.getCurrAngle() - SWEEP_ANGLE + charge * (SWEEP_ANGLE * 2f);
            MagicFakeBeam.spawnFakeBeam(
                    engine, weapon.getLocation(), weapon.getRange(), sweepAng,
                    2f, amount, 0.1f, 0f,
                    Color.WHITE, Color.GREEN,
                    0f, DamageType.ENERGY, 0f, weapon.getShip());
        }
        prevCharge = charge;
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (queue.isEmpty()) {
            queue.addAll(detector(weapon));
            if (queue.isEmpty()) {
                weapon.setAmmo(weapon.getAmmo()+1);
            }
        }

        if (queue.isEmpty()) {
            engine.removeEntity(projectile);
            weapon.stopFiring();
            return;
        }

        ShipAPI trgt = queue.remove(0);
        if (projectile instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) projectile;
            if (missile.getAI() instanceof GuidedMissileAI) {
                ((GuidedMissileAI) missile.getAI()).setTarget(trgt);
            }
        }

        if (queue.isEmpty()) {
            weapon.stopFiring();
        }
    }

    private ArrayList<ShipAPI> detector(WeaponAPI weapon) {
        ArrayList<ShipAPI> list = new ArrayList<>();
        int cap = SWEEP_ANGLE > 0 ? 99 : 99;
        if (weapon.getSpec() instanceof ProjectileWeaponSpecAPI) {
            cap = ((ProjectileWeaponSpecAPI) weapon.getSpec()).getBurstSize();
        }

        ShipAPI host = weapon.getShip();
        Vector2f from = weapon.getFirePoint(0);
        float facing = weapon.getCurrAngle();

        Iterator<Object> iter = Global.getCombatEngine().getShipGrid()
                .getCheckIterator(from, weapon.getRange() + 250f, weapon.getRange() + 250f);
        while (iter.hasNext() && list.size() < cap) {
            Object o = iter.next();
            if (!(o instanceof ShipAPI)) continue;
            ShipAPI other = (ShipAPI) o;
            if (!other.isAlive() || other.isHulk()) continue;
            if (other.getOwner() == host.getOwner()) continue;
            if (MathUtils.getDistance(from, other.getLocation())
                    > weapon.getRange() + other.getCollisionRadius()) continue;
            if (!Misc.isInArc(facing, SWEEP_ANGLE * 2f, from, other.getLocation())) continue;
            list.add(other);
        }
        return list;
    }
}