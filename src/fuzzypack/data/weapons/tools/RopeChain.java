package fuzzypack.data.weapons.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class RopeChain {

    public final int links;
    public final float linkLen;
    public final float restLength;
    public final Vector2f[] pos;
    public final Vector2f[] prev;

    private final String spriteCat;
    private final String spriteKey;
    private final float thickness;
    private final int solverIters;
    private final float damping;

    public RopeChain(Vector2f start, Vector2f end, float spacing, int minLinks, int maxLinks,
                     int solverIters, float damping, String spriteCat, String spriteKey, float thickness) {
        this.solverIters = solverIters;
        this.damping = damping;
        this.spriteCat = spriteCat;
        this.spriteKey = spriteKey;
        this.thickness = thickness;
        float dist = Math.max(MathUtils.getDistance(start, end), 1f);
        this.restLength = dist;
        this.links = (int) Math.max(minLinks, Math.min(maxLinks, Math.round(dist / spacing) + 1));
        this.linkLen = restLength / (links - 1);

        this.pos = new Vector2f[links];
        this.prev = new Vector2f[links];
        for (int i = 0; i < links; i++) {
            float t = i / (float) (links - 1);
            pos[i] = new Vector2f(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t);
            prev[i] = new Vector2f(pos[i]);
        }
    }

    public void pinEnds(Vector2f start, Vector2f end) {
        pos[0].set(start);
        prev[0].set(start);
        pos[links - 1].set(end);
        prev[links - 1].set(end);
    }

    public void simulate() {
        for (int i = 1; i < links - 1; i++) {
            float vx = (pos[i].x - prev[i].x) * damping;
            float vy = (pos[i].y - prev[i].y) * damping;
            prev[i].set(pos[i]);
            pos[i].set(pos[i].x + vx, pos[i].y + vy);
        }

        for (int n = 0; n < solverIters; n++) {
            for (int i = 0; i < links - 1; i++) {
                Vector2f a = pos[i];
                Vector2f b = pos[i + 1];
                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < 0.001f) continue;

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
    }

    public boolean applyPull(ShipAPI host, ShipAPI target, Vector2f start, Vector2f end,
                             float stiffness, float momentSpin, float amount) {
        if (host == null || target == null) return false;
        if (host.getMass() <= 0f || target.getMass() <= 0f) return false;

        float dist = MathUtils.getDistance(start, end);
        if (dist <= restLength) return false;

        float force = (dist - restLength) * stiffness * amount;
        float fx = force * ((end.x - start.x) / dist);
        float fy = force * ((end.y - start.y) / dist);

        float tot = host.getMass() + target.getMass();
        float hostRatio = target.getMass() / tot;
        float targetRatio = host.getMass() / tot;

        host.getVelocity().set(
                host.getVelocity().x + (fx * hostRatio) / host.getMass(),
                host.getVelocity().y + (fy * hostRatio) / host.getMass());
        target.getVelocity().set(
                target.getVelocity().x - (fx * targetRatio) / target.getMass(),
                target.getVelocity().y - (fy * targetRatio) / target.getMass());

        applyTorque(host, start, fx * hostRatio, fy * hostRatio, momentSpin);
        applyTorque(target, end, -fx * targetRatio, -fy * targetRatio, momentSpin);
        return true;
    }

    public static void applyTorque(ShipAPI ship, Vector2f attach, float fx, float fy, float momentSpin) {
        float rx = attach.x - ship.getLocation().x;
        float ry = attach.y - ship.getLocation().y;
        float torque = rx * fy - ry * fx;
        float inertia = ship.getMass() * momentSpin;
        if (inertia < 1f) return;
        ship.setAngularVelocity(ship.getAngularVelocity() + torque / inertia);
    }

    public static boolean reelToward(Vector2f start, Vector2f tip, float speed, float amount) {
        float dist = MathUtils.getDistance(start, tip);
        if (dist <= 15f) return true; // finished
        float step = Math.min(speed * amount, dist);
        Vector2f dir = VectorUtils.getDirectionalVector(tip, start);
        tip.x += dir.x * step;
        tip.y += dir.y * step;
        return false;
    }

    public float getTension(Vector2f start, Vector2f end, float stiffness) {
        float dist = MathUtils.getDistance(start, end);
        if (dist <= restLength) return 0f;
        return (dist - restLength) * stiffness;
    }

    public boolean isTaut(Vector2f start, Vector2f end) {
        return MathUtils.getDistance(start, end) > restLength;
    }

    public void render(float amount) {
        render(pos, links, spriteCat, spriteKey, thickness, amount);
    }

    public static void renderStraight(Vector2f start, Vector2f end, float spacing, int minLinks, int maxLinks,
                                      String spriteCat, String spriteKey, float thickness, float amount) {
        float dist = MathUtils.getDistance(start, end);
        if (dist < 1f) return;
        int n = (int) Math.max(minLinks, Math.min(maxLinks, Math.round(dist / spacing) + 1));
        Vector2f[] pts = new Vector2f[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            pts[i] = new Vector2f(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t);
        }
        render(pts, n, spriteCat, spriteKey, thickness, amount);
    }

    public static void render(Vector2f[] pts, int n, String spriteCat, String spriteKey,
                              float thickness, float amount) {
        for (int i = 0; i < n - 1; i++) {
            Vector2f a = pts[i];
            Vector2f b = pts[i + 1];
            float len = MathUtils.getDistance(a, b);
            if (len < 0.001f) continue;

            float facing = VectorUtils.getAngle(a, b);
            Vector2f mid = new Vector2f((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f);

            SpriteAPI sprite = Global.getSettings().getSprite(spriteCat, spriteKey);
            sprite.setAngle(0f);
            //sprite.setCenter(sprite.getWidth() / 2f, sprite.getHeight() / 2f); EVIL

            MagicRender.battlespace(
                    sprite,
                    mid,
                    new Vector2f(),
                    new Vector2f(len+thickness, thickness), // length on X, thickness on Y
                    new Vector2f(),
                    facing,                      // no -90
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