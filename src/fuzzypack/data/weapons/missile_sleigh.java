package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.util.ArrayList;
import java.util.List;

public class missile_sleigh implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private WeaponAPI chosenWpn = null;
    private boolean findNewWeapon = true;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getShip() == null || weapon.getShip().getWing() == null) return;
        ShipAPI carrier = weapon.getShip().getWing().getSourceShip();
        if (carrier == null) return;

        if (chosenWpn != null && chosenWpn.getMissileRenderData() != null) {
            Object projSpec = ((ProjectileWeaponSpecAPI) chosenWpn.getSpec()).getProjectileSpec();
            String spriteName = ((MissileSpecAPI) projSpec).getHullSpec().getSpriteName();
            SpriteAPI missileSprite = Global.getSettings().getSprite(spriteName);
            MagicRender.battlespace(
                    missileSprite,
                    weapon.getFirePoint(0),
                    new Vector2f(),
                    new Vector2f(missileSprite.getWidth(), missileSprite.getHeight()),
                    new Vector2f(),
                    weapon.getCurrAngle() - 90f,
                    0f,
                    missileSprite.getColor(),
                    false,
                    0f,
                    amount,
                    0f
            );
            return;
        } else if (findNewWeapon) {
            List<WeaponAPI> foundWpns = new ArrayList<>();
            for (WeaponAPI wpn : carrier.getAllWeapons()) {
                if (wpn.getType() == WeaponAPI.WeaponType.MISSILE && wpn.getAmmo() > 0) {
                    foundWpns.add(wpn);
                }
            }
            if (foundWpns.isEmpty()) return;
            chosenWpn = foundWpns.get((int) (Math.random() * foundWpns.size()));
            chosenWpn.setAmmo(chosenWpn.getAmmo() - chosenWpn.getSpec().getBurstSize());
            findNewWeapon = false;
        } else if (weapon.getShip().isLanding()) {
            findNewWeapon = true;
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (chosenWpn == null) return;
        engine.removeEntity(projectile);
        ProjectileWeaponSpecAPI spec = (ProjectileWeaponSpecAPI) chosenWpn.getSpec();
        int burstSize = spec.getBurstSize();
        float burstDelay = spec.getBurstDelay();
        String missileId = chosenWpn.getId();
        if (burstSize <= 1 || burstDelay <= 0f) {
            engine.spawnProjectile(
                    weapon.getShip(), weapon, missileId,
                    weapon.getFirePoint(0), weapon.getCurrAngle(),
                    weapon.getShip().getVelocity()
            );
        } else {
            weapon.getShip().addListener(new listener(burstSize, burstDelay, weapon, missileId));
        }
        chosenWpn = null;
    }

    class listener implements AdvanceableListener {
        private final IntervalUtil interval;
        private int shotsLeft;
        private final WeaponAPI weapon;
        private final String missileId;
        private final CombatEngineAPI engine = Global.getCombatEngine();
        private boolean first = true;

        public listener(int burstSize, float burstDelay, WeaponAPI weapon, String missileId) {
            this.shotsLeft = burstSize;
            this.weapon = weapon;
            this.missileId = missileId;
            this.interval = new IntervalUtil(burstDelay, burstDelay);
        }

        @Override
        public void advance(float amount) {
            if (engine.isPaused()) return;
            if (shotsLeft <= 0) {
                weapon.getShip().removeListener(this);
                return;
            }
            if (first) {
                first = false;
                fireOne();
                return;
            }
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                fireOne();
            }
        }

        private void fireOne() {
            engine.spawnProjectile(
                    weapon.getShip(), weapon, missileId,
                    weapon.getFirePoint(0), weapon.getCurrAngle(),
                    weapon.getShip().getVelocity()
            );
            shotsLeft--;
            if (shotsLeft <= 0) {
                weapon.getShip().removeListener(this);
            }
        }
    }
}