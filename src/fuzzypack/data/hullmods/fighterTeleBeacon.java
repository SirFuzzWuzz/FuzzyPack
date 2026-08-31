package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class fighterTeleBeacon extends BaseHullMod {

    private static final float HP_PERCENTAGE = 50f;
    private static final float WING_RANGE_MULT = 0.75f;
    private static final float PULL_SPEED = 500f; // units per second
    private static final Color JITTER_COLOR = new Color(170, 100, 200, 200);
    private static final Color EXPLOSION_COLOR = new Color(190, 100, 200, 250);

    private final List<Recall> recalls = new ArrayList<>();

    private static class Recall {
        final ShipAPI fighter;
        final ShipAPI carrier;
        final Vector2f pos;
        boolean done;

        Recall(ShipAPI fighter, ShipAPI carrier) {
            this.fighter = fighter;
            this.carrier = carrier;
            this.pos = new Vector2f(fighter.getLocation());
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getMutableStats().getNumFighterBays().getModifiedValue() > 0f;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.hasLaunchBays()) return "Ship does not have fighter bays";
        return super.getUnapplicableReason(ship);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round(HP_PERCENTAGE) + "%";
        if (index == 1) return Math.round(WING_RANGE_MULT * 100f) + "%";
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFighterRefitTimeMult().modifyMult(id, 1.5f);
        stats.getFighterWingRange().modifyMult(id, WING_RANGE_MULT);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive() || Global.getCombatEngine().isPaused()) return;

        startRecalls(ship);
        tickRecalls(amount);
    }

    private void startRecalls(ShipAPI ship) {
        if (ship.getFluxTracker().isOverloaded()) return;

        for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
            if (bay.getWing() == null) continue;
            for (ShipAPI fighter : bay.getWing().getWingMembers()) {
                if (fighter == null || !fighter.isAlive() || fighter.isHulk() || fighter.isLanding()) continue;
                if (alreadyRecalling(fighter)) continue;
                if (fighter.getHitpoints() > fighter.getMaxHitpoints() * (HP_PERCENTAGE / 100f)) continue;

                Global.getSoundPlayer().playSound("system_phase_skimmer", 1.2f, 0.5f,
                        fighter.getLocation(), fighter.getVelocity());
                Global.getCombatEngine().spawnExplosion(
                        fighter.getLocation(), new Vector2f(),
                        EXPLOSION_COLOR, fighter.getCollisionRadius() * 2.5f, 1f);

                recalls.add(new Recall(fighter, ship));
            }
        }
    }

    private boolean alreadyRecalling(ShipAPI fighter) {
        for (Recall r : recalls) {
            if (r.fighter == fighter && !r.done) return true;
        }
        return false;
    }

    private void tickRecalls(float amount) {
        Iterator<Recall> it = recalls.iterator();
        while (it.hasNext()) {
            Recall r = it.next();
            ShipAPI fighter = r.fighter;
            ShipAPI carrier = r.carrier;

            if (fighter == null || carrier == null || !carrier.isAlive()
                    || !fighter.isAlive() || fighter.isHulk() || fighter.getWing() == null) {
                it.remove();
                continue;
            }

            fighter.setHoldFireOneFrame(true);
            fighter.setCollisionClass(CollisionClass.NONE);
            fighter.getMutableStats().getHullDamageTakenMult().modifyMult("tele_beacon_recall", 0f);
            fighter.getVelocity().set(0f, 0f);
            fighter.setAlphaMult(0f);

            float angle = VectorUtils.getAngle(r.pos, carrier.getLocation());
            r.pos.set(MathUtils.getPointOnCircumference(r.pos, PULL_SPEED * amount, angle));
            fighter.getLocation().set(r.pos);

            SpriteAPI sprite = Global.getSettings().getSprite(fighter.getHullSpec().getSpriteName());
            sprite.setCenter(sprite.getWidth() / 2f, sprite.getHeight() / 2f);
            MagicRender.battlespace(
                    sprite,
                    new Vector2f(r.pos),
                    new Vector2f(),
                    new Vector2f(sprite.getWidth(), sprite.getHeight()),
                    new Vector2f(),
                    fighter.getFacing(),
                    0f,
                    JITTER_COLOR,
                    true,
                    0f,
                    amount,
                    0.15f
            );

            float landAt = carrier.getCollisionRadius();
            if (MathUtils.getDistance(r.pos, carrier.getLocation()) <= landAt) {
                fighter.getMutableStats().getHullDamageTakenMult().unmodify("tele_beacon_recall");
                fighter.setAlphaMult(1f);
                fighter.setCollisionClass(CollisionClass.FIGHTER);
                if (fighter.getWing().getSource() != null) {
                    fighter.getWing().getSource().makeCurrentIntervalFast();
                    fighter.getWing().getSource().land(fighter);
                }
                it.remove();
            }
        }
    }
}