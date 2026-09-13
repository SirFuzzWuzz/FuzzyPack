package fuzzypack.data.weapons.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


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

        // cLeAn cOdE also in case barrel/glow sprite null
        private static void hide(SpriteAPI s) {
            if (s != null) s.setSize(0,0);;
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

    // Finds weapons or engines near a thing, not actual modules
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

    // So that a grabbed wpn or engine can't be double grabbed, maybe make it so 2 speeds up the yoink?
    private static final java.util.Set<Object> GRABBED = new java.util.HashSet<Object>();

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
    // ------------------------------------------------------------------
}
