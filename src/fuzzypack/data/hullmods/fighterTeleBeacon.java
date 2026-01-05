package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.*;
import org.magiclib.util.MagicRender;


public class fighterTeleBeacon extends BaseHullMod {

    private final float hpPercentage = 25f;
    public static final Color JITTER_COLOR = new Color(100,165,255,155);
    private static float wingRangeMult = 0.75f;

    public boolean isApplicableToShip(ShipAPI ship) {
        int bays = (int) ship.getMutableStats().getNumFighterBays().getModifiedValue();
        return bays > 0;
    }
    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.hasLaunchBays()) {
            return "Ship does not have fighter bays";
        }
        return super.getUnapplicableReason(ship);
    }
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round(hpPercentage) + "%";
        if (index == 1) return wingRangeMult*100 + "%";
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFighterRefitTimeMult().modifyMult(id,1.5f);
        stats.getFighterWingRange().modifyMult(id, wingRangeMult);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;
        for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
            if (bay.getWing() == null) continue;
            for (ShipAPI fighter : bay.getWing().getWingMembers()) {
                if (fighter.isHulk()) continue;
                if (fighter.getHitpoints() <= fighter.getMaxHitpoints() * (hpPercentage / 100) &&
                        !fighter.hasListenerOfClass(recallListener.class) && !ship.getFluxTracker().isOverloaded()) {
                    Global.getSoundPlayer().playSound("system_phase_skimmer", 1.2f, 0.5f,
                            fighter.getLocation(), fighter.getVelocity());
                    Global.getCombatEngine().spawnExplosion(
                            fighter.getLocation(),
                            new Vector2f(0, 0),
                            JITTER_COLOR,
                            fighter.getSpriteAPI().getWidth(),
                            1f);
                    fighter.addListener(new recallListener(fighter, ship));
                    Global.getCombatEngine().addFloatingText(ship.getLocation(), "found ship", 20f, Color.green, fighter, 3, 1);
                }
            }
        }
    }

    private class recallListener implements AdvanceableListener {
        ShipAPI fighter;
        SpriteAPI sprite;
        Vector2f pos;
        ShipAPI carrier;
        float speed = 50f;
        IntervalUtil interval = new IntervalUtil(0.1f,0.1f);

        public recallListener(ShipAPI fighter, ShipAPI carrier) {
            fighter.setPhased(true);
            fighter.setCollisionClass(CollisionClass.NONE);
            fighter.getVelocity().set(0,0);
            //fighter.setAlphaMult(0);
            this.fighter = fighter;
            this.sprite = fighter.getSpriteAPI();
            this.pos = new Vector2f(fighter.getLocation());
            this.carrier = carrier;
        }
        @Override
        public void advance(float amount) {
            if (!fighter.isAlive() ||!carrier.isAlive()) {
                fighter.removeListenerOfClass(recallListener.class);
                return;
            }
            fighter.setHoldFireOneFrame(true);
            if (interval.intervalElapsed()) {
                pos.set(MathUtils.getPoint(pos, speed, VectorUtils.getAngle(pos, carrier.getLocation())));
                MagicRender.battlespace(
                        sprite,
                        pos,
                        new Vector2f(),
                        new Vector2f(sprite.getWidth(), sprite.getHeight()),
                        new Vector2f(0,0),
                        fighter.getFacing(),
                        0f,
                        JITTER_COLOR,
                        true,
                        0.05f,
                        0.5f,
                        0.5f);
                float dist = MathUtils.getDistance(pos, carrier.getLocation());
                if (dist < 55f) {
                    fighter.removeListenerOfClass(recallListener.class);
                    fighter.getWing().getSource().makeCurrentIntervalFast();
                    fighter.getWing().getSource().land(fighter);
                    Global.getCombatEngine().addFloatingText(pos, "landed", 20f, Color.red, carrier, 3, 1);
                    return;
                }
                Global.getCombatEngine().addFloatingText(pos, "recalling", 20f, Color.blue, carrier, 3, 1);
            }
            interval.advance(amount);
        } //advance
    }
}