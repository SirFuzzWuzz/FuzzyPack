package fuzzypack.data.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import java.awt.Color;
import java.util.Iterator;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import fuzzypack.data.functions.tools;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;



public class fluxbattery implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

        public static float fluxCap = 8000;
        private final float fluxDiss = 200; //Per second

        private float fluxStored = 0;
        private final int projZapAoe = 125;

        private int maxAmmo;
        private boolean once = true;
        private final IntervalUtil interval = new IntervalUtil(1f, 1f);

        
        public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
            if (engine.isPaused()) return;
            if (!weapon.getShip().isAlive()) return;
            if (weapon.isDisabled())return;
            if (once) {
                maxAmmo = weapon.getMaxAmmo();
                weapon.setAmmo(0);
                once = false;
            }
            if (maxAmmo == 0) return;
            
            ShipAPI ship = weapon.getShip();
            
            if (interval.intervalElapsed() && maxAmmo != 0) {
                if (ship.getFluxTracker().getCurrFlux() > fluxDiss  && fluxStored < fluxCap) {
                    ship.getFluxTracker().decreaseFlux(fluxDiss);
                    fluxStored += fluxDiss;

                } else if (fluxStored >= fluxCap) {
                    weapon.setAmmo(1);

                    Vector2f from = weapon.getLocation();
                    Vector2f too = MathUtils.getPointOnCircumference(from, MathUtils.getRandomNumberInRange(30,50), MathUtils.getRandomNumberInRange(1,360));
                    engine.spawnEmpArcVisual(from, ship, too, ship, 1.5f, Color.black, weapon.getSpec().getGlowColor());
                }
            }
            interval.advance(amount);

            Vector2f glowLoc = weapon.getLocation();
            String spriteName = "battery_t_glow";
            if (weapon.getSlot().isHardpoint()) {
                spriteName = "battery_h_glow";
                glowLoc = MathUtils.getPointOnCircumference(glowLoc, 8, weapon.getCurrAngle());
            }
            MagicRender.battlespace(Global.getSettings().getSprite("glow", spriteName),
                    glowLoc,
                    new Vector2f(0,0),
                    new Vector2f(weapon.getGlowSpriteAPI().getWidth(),weapon.getGlowSpriteAPI().getHeight()),
                    new Vector2f(0,0),
                    weapon.getCurrAngle() -90,
                    0,
                    tools.ColorAlphaChange(weapon.getSpec().getGlowColor(), 255 * fluxStored/fluxCap),
                    true,
                    0.01f,0.04f,0.01f);


            //If player ship is the ship, left side visual
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(ship+weapon.getSlot().getId(),
                                    ship.getSystem().getSpecAPI().getIconSpriteName(),
                                    "Flux Battery", (int)(fluxStored) + " Flux Stored in battery", true);
            }

        }


        
        public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
            if (fluxStored >= fluxCap) {
                fluxStored = 0;
                maxAmmo -=1;
                weapon.getShip().addListener(new fluxbattery.listener(projectile));
            }
        }


    class listener implements AdvanceableListener {

        CombatEngineAPI engine = Global.getCombatEngine();
        DamagingProjectileAPI proj;
        IntervalUtil ticInterval = new IntervalUtil(0.2f,0.3f);

        public listener(DamagingProjectileAPI proj) {
            this.proj = proj;
        }

        @Override
        public void advance(float amount) {
            if (proj.isExpired() || proj.didDamage() || !engine.isEntityInPlay(proj)) {
                return;
            }
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                Iterator iter = engine.getShipGrid().getCheckIterator(proj.getLocation(), projZapAoe, projZapAoe);
                while (iter.hasNext()) {
                    ShipAPI target = (ShipAPI) iter.next();
                    if (proj.getSource().getOriginalOwner() == target.getOriginalOwner() || target.getHullSize().equals(ShipAPI.HullSize.FIGHTER)) continue;
                    engine.spawnEmpArc(proj.getSource(),
                            proj.getLocation(),
                            proj,
                            target,
                            DamageType.ENERGY,
                            proj.getDamageAmount()*0.5f,
                            proj.getEmpAmount(),
                            10000f,
                            "tachyon_lance_emp_impact",
                            4f,
                            proj.getWeapon().getSpec().getGlowColor(),
                            Color.white);
                    break;
                }
                Vector2f from = proj.getLocation();
                Vector2f too = MathUtils.getPointOnCircumference(from, MathUtils.getRandomNumberInRange(projZapAoe,projZapAoe*1.2f),
                        MathUtils.getRandomNumberInRange(1,360));
                engine.spawnEmpArcVisual(from, proj, too, proj, 1.5f, proj.getWeapon().getSpec().getGlowColor(),Color.white);
            }
        } //advance
    }

}
