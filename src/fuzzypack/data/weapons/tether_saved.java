package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class tether_saved implements OnHitEffectPlugin, OnFireEffectPlugin {

    private static final float LINK_SPACING = 30f;
    private static final int MIN_LINKS = 6;
    private static final int MAX_LINKS = 30;
    private static final int SOLVER_ITERS = 8;
    private static final float DAMPING = 0.95f;
    private static final float MOMENT_SPIN = 100f; // ~100 is good, change for ship class?
    private static final float STIFFNESS = 900f;
    private static final float DURATION = 20f;
    private static final String LINK_SPRITE = "taser_chain";


    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (weapon.getShip() == null || projectile == null) return;
        weapon.getShip().addListener(new flightListener(projectile, weapon));
    }

    // make chain to projectile while in flight
    class flightListener implements AdvanceableListener {
        final DamagingProjectileAPI proj;
        final WeaponAPI weap;
        final ShipAPI host;
        boolean done;

        flightListener(DamagingProjectileAPI proj, WeaponAPI weap) {
            this.proj = proj;
            this.weap = weap;
            this.host = weap.getShip();
        }

        @Override
        public void advance(float amount) {
            if (done) return;
            CombatEngineAPI engine = Global.getCombatEngine();
            if (host == null || !host.isAlive() || proj == null || proj.didDamage() || proj.isFading()) {
                done = true;
                if (host != null) host.removeListener(this);
                return;
            }

            Vector2f start = weap.getFirePoint(0);
            Vector2f end = proj.getLocation();
            float dist = MathUtils.getDistance(start, end);
            if (dist < 1f) return;

            if (dist >= weap.getRange()) {
                engine.removeEntity(proj); // TODO visuals?
                host.removeListener(this);
                return;
            }

            int n = (int) Math.max(MIN_LINKS, Math.min(MAX_LINKS,
                    Math.round(dist / LINK_SPACING) + 1));

            for (int i = 0; i < n - 1; i++) {
                float t0 = i / (float) (n - 1);
                float t1 = (i + 1) / (float) (n - 1);
                Vector2f a = new Vector2f(
                        start.x + (end.x - start.x) * t0,
                        start.y + (end.y - start.y) * t0);
                Vector2f b = new Vector2f(
                        start.x + (end.x - start.x) * t1,
                        start.y + (end.y - start.y) * t1);
                Vector2f mid = new Vector2f((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f);
                float len = MathUtils.getDistance(a, b);
                MagicRender.battlespace(
                        Global.getSettings().getSprite("projectiles", LINK_SPRITE),
                        mid,
                        new Vector2f(),
                        new Vector2f(10f, len + 8f),   // thickness, length + overlap
                        new Vector2f(),
                        VectorUtils.getAngle(a, b) - 90f,
                        0f,
                        new Color(255, 255, 255, 240),
                        false,
                        0f,
                        amount,
                        0f
                );
            }
        }
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI) || shieldHit) return;
        if (projectile.getWeapon() == null || projectile.getWeapon().getShip() == null) return;
        float MAX_DIST = projectile.getWeapon().getRange();
        float hitDist = MathUtils.getDistance(projectile.getWeapon().getFirePoint(0), point);
        if (hitDist >= MAX_DIST || hitDist < 10f) return;

        projectile.getWeapon().getShip().addListener(
                new listener(projectile, (ShipAPI) target, point, hitDist));
    }

    class listener implements AdvanceableListener {
        final ShipAPI target;
        final ShipAPI host;
        final WeaponAPI weap;
        final IntervalUtil life = new IntervalUtil(DURATION, DURATION);
        final float impactDist;
        final float impactOffset;
        final float hookFacingOffset;
        final int links;
        final float linkLen;
        final float restLength;
        final Vector2f[] pos;
        final Vector2f[] prev;
        //final float linkLen = MIN_DIST / (LINKS - 1);

        listener(DamagingProjectileAPI proj, ShipAPI target, Vector2f point, float hitDist) {
            this.target = target;
            this.host = proj.getWeapon().getShip();
            this.weap = proj.getWeapon();
            this.impactDist = MathUtils.getDistance(point, target.getLocation());
            this.impactOffset = VectorUtils.getAngle(target.getLocation(), point) - target.getFacing();
            this.hookFacingOffset = proj.getFacing() - target.getFacing();

            this.restLength = hitDist;
            this.links = (int) Math.max(MIN_LINKS, Math.min(MAX_LINKS,
                    Math.round(hitDist / LINK_SPACING) + 1));
            this.linkLen = restLength / (links - 1);

            this.pos = new Vector2f[links];
            this.prev = new Vector2f[links];

            Vector2f a = new Vector2f(weap.getFirePoint(0));
            Vector2f b = new Vector2f(point);
            for (int i = 0; i < links; i++) {
                float t = i / (float) (links - 1);
                pos[i] = new Vector2f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
                prev[i] = new Vector2f(pos[i]);
            }
        }

        @Override
        public void advance(float amount) {
            if (host == null || target == null || !host.isAlive() || !target.isAlive()) {
                if (host != null) host.removeListener(this);
                return;
            }

            life.advance(amount);
            if (life.intervalElapsed()) {
                host.removeListener(this);
                return;
            }

            Vector2f start = weap.getFirePoint(0);
            Vector2f end = MathUtils.getPointOnCircumference(
                    target.getLocation(), impactDist, target.getFacing() + impactOffset);

            // pin ends
            pos[0].set(start);
            prev[0].set(start);
            pos[links - 1].set(end);
            prev[links - 1].set(end);

            // verlet on free links, MATH
            for (int i = 1; i < links - 1; i++) {
                float vx = (pos[i].x - prev[i].x) * DAMPING;
                float vy = (pos[i].y - prev[i].y) * DAMPING;
                prev[i].set(pos[i]);
                pos[i].set(pos[i].x + vx, pos[i].y + vy);
            }

            // distance constraints
            for (int n = 0; n < SOLVER_ITERS; n++) {
                for (int i = 0; i < links - 1; i++) {
                    Vector2f a = pos[i];
                    Vector2f b = pos[i + 1];
                    float dx = b.x - a.x;
                    float dy = b.y - a.y;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d < 0.001f) continue; // or just rly small, unnecessary?

                    float diff = (d - linkLen) / d;
                    float ox = dx * diff * 0.5f;
                    float oy = dy * diff * 0.5f;

                    if (i == 0) {
                        b.set(b.x - ox * 2f, b.y - oy * 2f);
                    } else if (i + 1 == links - 1) {
                        a.set(a.x + ox * 2f, a.y + oy * 2f);
                    } else {
                        a.set(a.x + ox, a.y + oy);
                        b.set(b.x - ox, b.y - oy);
                    }
                }
            }

            // pull ships only when the chain is taut
            float shipDist = MathUtils.getDistance(start, end);
            if (shipDist > restLength) {
                float dx = end.x - start.x;
                float dy = end.y - start.y;
                float force = (shipDist - restLength) * STIFFNESS * amount;
                float fx = force * (dx / shipDist);
                float fy = force * (dy / shipDist);

                float tot = host.getMass() + target.getMass();
                if (tot > 0f && host.getMass() > 0f && target.getMass() > 0f) {
                    float hostRatio = target.getMass() / tot;
                    float targetRatio = host.getMass() / tot;

                    // linear
                    host.getVelocity().set(
                            host.getVelocity().x + (fx * hostRatio) / host.getMass(),
                            host.getVelocity().y + (fy * hostRatio) / host.getMass());
                    target.getVelocity().set(
                            target.getVelocity().x - (fx * targetRatio) / target.getMass(),
                            target.getVelocity().y - (fy * targetRatio) / target.getMass());

                    // torque = r × F (rx*Fy - ry*Fx) speen
                    applyTorque(host, start, fx * hostRatio, fy * hostRatio);
                    applyTorque(target, end, -fx * targetRatio, -fy * targetRatio);
                }
            }

            // Render missile at target
            MagicRender.battlespace(
                    Global.getSettings().getSprite("projectiles", "tether_missile"),
                    end,
                    target.getVelocity(),
                    new Vector2f(12f, 20f),
                    new Vector2f(),
                    target.getFacing() + hookFacingOffset - 90,
                    0f,
                    new Color(255, 255, 255, 255),
                    false,
                    0f,
                    amount,
                    0f
            );

            // draw each link
            for (int i = 0; i < links - 1; i++) {
                Vector2f a = pos[i];
                Vector2f b = pos[i + 1];
                Vector2f mid = new Vector2f((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f);
                float len = MathUtils.getDistance(a, b);
                MagicRender.battlespace(
                        Global.getSettings().getSprite("projectiles", LINK_SPRITE),
                        mid,
                        new Vector2f(),
                        new Vector2f(10f, len + 8f),   // thickness, length + overlap
                        new Vector2f(),
                        VectorUtils.getAngle(a, b) - 90f,
                        0f,
                        new Color(255, 255, 255, 240),
                        false,
                        0f,
                        amount,
                        0f
                );
            }
        }

        private void applyTorque(ShipAPI ship, Vector2f attachPoint, float fx, float fy) {
            float rx = attachPoint.x - ship.getLocation().x;
            float ry = attachPoint.y - ship.getLocation().y;
            float torque = rx * fy - ry * fx;
            float inertia = ship.getMass() * MOMENT_SPIN; // could add another variable, pixelmass, collision rad?
            if (inertia < 1f) return;
            ship.setAngularVelocity(ship.getAngularVelocity() + torque / inertia);
        }
    }
}