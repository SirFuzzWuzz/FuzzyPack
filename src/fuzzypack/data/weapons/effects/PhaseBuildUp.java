package fuzzypack.data.weapons.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import org.lwjgl.util.vector.Vector2f;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import java.awt.Color;
import java.util.ArrayList;

public class PhaseBuildUp implements AdvanceableListener {

    private static final int STACKS_TO_PHASE = 10;
    private static final int MAX_STACKS = 100;
    private static final float PHASE_EXTRA = 5f;
    private static final Color PHASE_COLOR = new Color(190, 100, 200, 150);
    private final ShipAPI ship;
    private final ArrayList<PhaseStack> stacks = new ArrayList<>();
    private boolean isPhased = false;

    public PhaseBuildUp(ShipAPI ship) {
        this.ship = ship;
    }

    public static PhaseBuildUp get(ShipAPI ship) {
        for (PhaseBuildUp p : ship.getListeners(PhaseBuildUp.class)) return p;
        PhaseBuildUp p = new PhaseBuildUp(ship);
        ship.addListener(p);
        return p;
    }

    public void addStack(float duration) {
        if (isPhased || stacks.size() >= MAX_STACKS) return;
        stacks.add(new PhaseStack(duration));
    }

    public static class PhaseStack {
        float duration;
        PhaseStack(float duration) {
            this.duration = duration;
        }
    }

    @Override
    public void advance(float amount) {
        if (ship == null || !ship.isAlive()) {
            forceClear();
            return;
        }

        ArrayList<PhaseStack> copy = new ArrayList<>(stacks);
        for (PhaseStack stack : copy) {
            stack.duration -= amount;
            if (stack.duration < 0f) stacks.remove(stack);
        }

        if (stacks.size() >= STACKS_TO_PHASE && !isPhased) {
            stacks.add(new PhaseStack(PHASE_EXTRA));
            enterPhase();
        }

        applyPhaseEffects();
    }

    private void applyPhaseEffects() {
        if (isPhased) {
            ship.blockCommandForOneFrame(ShipCommand.FIRE);
            ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
            ship.setPhased(true);
            ship.setAlphaMult(0.3f);
            ship.setJitterUnder(ship, PHASE_COLOR, 2f, 3, 5f);
            if (stacks.isEmpty()) exitPhase();
        } else {
            ship.setJitter(ship, PHASE_COLOR, stacks.size() * 0.1f, 3, 5f);
        }
    }

    private void enterPhase() {
        isPhased = true;
        ship.setPhased(true);
        ship.setAlphaMult(0.3f);
        ship.getMutableStats().getPeakCRDuration().modifyMult("pspace_bad", 0f);
        ship.getMutableStats().getTimeMult().modifyMult("pgun", 0.5f);
        ship.getMutableStats().getCRLossPerSecondPercent().modifyMult("pgun", 3f);
        Global.getSoundPlayer().playSound("gate_explosion", 0.2f, 0.8f, ship.getLocation(), new Vector2f());
        Global.getCombatEngine().addSwirlyNebulaParticle(
                ship.getLocation(), new Vector2f(),
                ship.getCollisionRadius() * 2f,
                0f, -0.5f, 0.5f, 1.5f,
                new Color(190, 100, 200, 200), true);
        distWave(24f, 0.5f);
    }

    private void exitPhase() {
        isPhased = false;
        ship.setPhased(false);
        ship.setAlphaMult(1f);
        ship.getMutableStats().getPeakCRDuration().unmodify("pspace_bad");
        ship.getMutableStats().getTimeMult().unmodify("pgun");
        ship.getMutableStats().getCRLossPerSecondPercent().unmodify("pgun");
        ship.setJitterUnder(ship, PHASE_COLOR, 1f, 5, 20f);
        Global.getSoundPlayer().playSound("gate_explosion", 0.3f, 1.8f, ship.getLocation(), new Vector2f());
        distWave(24f, 0.2f);
    }

    private void forceClear() {
        if (ship != null) {
            ship.setPhased(false);
            ship.setAlphaMult(1f);
            ship.removeListener(this);
        }
        stacks.clear();
        isPhased = false;
    }

    private void distWave(float intensity, float fade) {
        WaveDistortion w = new WaveDistortion(ship.getLocation(), ship.getVelocity());
        w.setSize(ship.getCollisionRadius() * 2.5f);
        w.setIntensity(intensity);
        w.fadeInSize(0.05f);
        w.fadeOutIntensity(fade);
        DistortionShader.addDistortion(w);
    }
}