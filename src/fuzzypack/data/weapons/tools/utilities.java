package fuzzypack.data.weapons.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class utilities {

    public static class hideWeaponListener implements AdvanceableListener {
        final WeaponAPI wpn;
        public hideWeaponListener(WeaponAPI wpn) { this.wpn = wpn; }

        @Override
        public void advance(float amount) {
            if (wpn == null || wpn.getShip() == null || !wpn.getShip().isAlive()) {
                assert wpn != null;
                if (wpn.getShip() != null) wpn.getShip().removeListener(this);
                return;
            }
            hide(wpn.getSprite());
            hide(wpn.getBarrelSpriteAPI());
            hide(wpn.getUnderSpriteAPI());
            hide(wpn.getGlowSpriteAPI());
        }

        private static void hide(SpriteAPI s) {
            if (s != null) s.setSize(0, 0);
        }
    }

    public static class ModuleHit {
        public final ShipAPI ship;
        public final WeaponAPI weapon;
        public final ShipEngineControllerAPI.ShipEngineAPI engine;

        public ModuleHit(ShipAPI ship, WeaponAPI weapon, ShipEngineControllerAPI.ShipEngineAPI engine) {
            this.ship = ship;
            this.weapon = weapon;
            this.engine = engine;
        }

        public boolean isWeapon() { return weapon != null; }
        public boolean isEngine() { return engine != null; }
    }


    public static ShipAPI findEnemyHullAt(ShipAPI firer, Vector2f worldPoint, float extraSearch) {
        if (worldPoint == null) return null;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return null;

        for (ShipAPI ship : engine.getShips()) {
            if (ship == firer || !ship.isAlive() || ship.isHulk() || ship.isFighter()) continue;
            if (firer != null && ship.getOwner() == firer.getOwner()) continue;
            if (MathUtils.getDistance(worldPoint, ship.getLocation())
                    > ship.getCollisionRadius() + extraSearch) continue;
            if (CollisionUtils.isPointWithinBounds(worldPoint, ship)) return ship;
        }
        return null;
    }

    public static ModuleHit findNearestEnemyModule(ShipAPI host, Vector2f tip, float radius,
                                                   boolean includeWeapons, boolean includeEngines) {
        if (host == null || tip == null) return null;
        CombatEngineAPI engine = Global.getCombatEngine();
        ModuleHit best = null;
        float bestD = radius;

        for (ShipAPI ship : engine.getShips()) {
            if (ship == host || !ship.isAlive() || ship.isHulk()) continue;
            if (ship.getOwner() == host.getOwner()) continue;
            if (MathUtils.getDistance(tip, ship.getLocation()) > ship.getCollisionRadius() + radius) continue;

            if (includeWeapons) {
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (isGrabbed(w)) continue;
                    if (w.isDecorative() || w.getSlot().isHidden() || w.isDisabled()) continue;
                    float d = MathUtils.getDistance(tip, w.getLocation());
                    if (d < bestD) {
                        bestD = d;
                        best = new ModuleHit(ship, w, null);
                    }
                }
            }
            if (includeEngines) {
                for (ShipEngineControllerAPI.ShipEngineAPI e : ship.getEngineController().getShipEngines()) {
                    if (isGrabbed(e)) continue;
                    if (e.isDisabled()) continue;
                    float d = MathUtils.getDistance(tip, e.getLocation());
                    if (d < bestD) {
                        bestD = d;
                        best = new ModuleHit(ship, null, e);
                    }
                }
            }
        }
        return best;
    }

    private static final Set<Object> GRABBED = new HashSet<Object>();

    public static boolean isGrabbed(Object module) {
        return module != null && GRABBED.contains(module);
    }
    public static boolean tryGrab(Object module) {
        if (module == null || GRABBED.contains(module)) return false;
        GRABBED.add(module);
        return true;
    }
    public static void releaseGrab(Object module) {
        if (module != null) GRABBED.remove(module);
    }
    public static Object moduleOf(ModuleHit hit) {
        if (hit == null) return null;
        return hit.weapon != null ? hit.weapon : hit.engine;
    }


    public static Vector2f toWorld(ShipAPI ship, Vector2f local) {
        float facing = (float) Math.toRadians(ship.getFacing());
        float cos = (float) Math.cos(facing);
        float sin = (float) Math.sin(facing);
        return new Vector2f(
                ship.getLocation().x + local.x * cos - local.y * sin,
                ship.getLocation().y + local.x * sin + local.y * cos
        );
    }

    public static class WalkParams {
        public float tick = 0.08f;
        public float step = 12f;
        public float bend = 28f;
        public float branchChance = 0.05f;
        public int maxVines = 16;
        public int maxPoints = 25;
        public int starters = 1;
        public float coverFraction = 0.4f;
        public float maxGrowTime = 3f;
        public float starterSpread = 35f;
        public float branchAngle = 55f;
    }

    public static class Vine {
        public final List<Vector2f> points = new ArrayList<Vector2f>();
        public float heading;
        public boolean alive = true;
        public boolean growing = true;
        public int painted;

        public Vine(Vector2f start, float heading) {
            points.add(new Vector2f(start));
            this.heading = heading;
        }
    }

    public static class Segment {
        public final Vine vine;
        public final Vector2f fromLocal;
        public final Vector2f toLocal;

        public Segment(Vine vine, Vector2f fromLocal, Vector2f toLocal) {
            this.vine = vine;
            this.fromLocal = fromLocal;
            this.toLocal = toLocal;
        }
    }

    public static class VineWalk {
        public final ShipAPI ship;
        public final List<Vine> vines = new ArrayList<Vine>();
        public final Set<Long> covered = new HashSet<Long>();
        public final WalkParams p;
        public final int coverableCells;
        public boolean growing = true;

        private final IntervalUtil interval;
        private float age;

        public VineWalk(ShipAPI ship, Vector2f startLocal, float inboundLocal, WalkParams p) {
            this.ship = ship;
            this.p = p;
            this.coverableCells = Math.max(1, countCoverable(ship));
            this.interval = new IntervalUtil(p.tick, p.tick);
            markCovered(startLocal);
            addStarters(startLocal, inboundLocal, p.starters);
        }

        public void addHit(Vector2f startLocal, float inboundLocal) {
            growing = true;
            age = 0f;
            markCovered(startLocal);
            addStarters(startLocal, inboundLocal, p.starters);
        }

        public List<Segment> advance(float amount) {
            List<Segment> added = new ArrayList<Segment>();
            if (!growing || ship == null || !ship.isAlive()) return added;

            age += amount;
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                added.addAll(step());
            }

            boolean any = false;
            for (Vine v : vines) {
                if (v.growing) {
                    any = true;
                    break;
                }
            }
            if (!any || coverage() >= p.coverFraction || age >= p.maxGrowTime) {
                freezeAll();
                growing = false;
            }
            return added;
        }

        public float coverage() {
            return covered.size() / (float) coverableCells;
        }

        public void freezeAll() {
            for (Vine v : vines) v.growing = false;
            growing = false;
        }

        private void addStarters(Vector2f startLocal, float inboundLocal, int requested) {
            int canAdd = Math.max(0, p.maxVines - vines.size());
            int add = Math.min(requested, canAdd);
            for (int i = 0; i < add; i++) {
                float spread = (add <= 1) ? 0f : (i - (add - 1) * 0.5f) * p.starterSpread;
                Vine v = new Vine(startLocal, inboundLocal + 180f + spread
                        + (float) (Math.random() * 20f - 10f));
                v.growing = true;
                vines.add(v);
            }
        }

        private List<Segment> step() {
            List<Segment> added = new ArrayList<Segment>();
            List<Vine> born = new ArrayList<Vine>();

            for (Vine v : vines) {
                if (!v.alive || !v.growing || v.points.size() >= p.maxPoints) {
                    v.growing = false;
                    if (v.points.size() >= p.maxPoints) v.alive = false;
                    continue;
                }

                Vector2f tip = v.points.get(v.points.size() - 1);
                v.heading += (float) (Math.random() * 2f * p.bend - p.bend);
                float rad = (float) Math.toRadians(v.heading);
                Vector2f next = new Vector2f(
                        tip.x + (float) Math.cos(rad) * p.step,
                        tip.y + (float) Math.sin(rad) * p.step
                );

                if (!CollisionUtils.isPointWithinBounds(toWorld(ship, next), ship)) {
                    v.heading += 150f + (float) (Math.random() * 60f);
                    continue;
                }

                v.points.add(next);
                markCovered(next);
                added.add(new Segment(v, tip, next));

                if (vines.size() + born.size() < p.maxVines && Math.random() < p.branchChance) {
                    Vine child = new Vine(next, v.heading + (Math.random() < 0.5f ? p.branchAngle : -p.branchAngle));
                    child.growing = true;
                    born.add(child);
                }
            }
            vines.addAll(born);
            return added;
        }

        private boolean markCovered(Vector2f local) {
            int[] cell = ship.getArmorGrid().getCellAtLocation(toWorld(ship, local));
            if (cell == null) return false;
            long key = (((long) cell[0]) << 32) ^ (cell[1] & 0xffffffffL);
            return covered.add(key);
        }

        private static int countCoverable(ShipAPI ship) {
            ArmorGridAPI grid = ship.getArmorGrid();
            float[][] a = grid.getGrid();
            int n = 0;
            for (int x = 0; x < a.length; x++) {
                for (int y = 0; y < a[x].length; y++) {
                    if (a[x][y] <= 0f) continue;
                    Vector2f p = grid.getLocation(x, y);
                    if (p != null && CollisionUtils.isPointWithinBounds(p, ship)) n++;
                }
            }
            return Math.max(1, n);
        }
    }
}