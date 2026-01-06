package fuzzypack.data.weapons.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;

//Kotlin is bad and you should feel bad
public class PhaseBuildUp implements AdvanceableListener {
    private ShipAPI ship;
    public static ArrayList<PhaseStack> stacks = new ArrayList<>();

    protected static boolean isPhased = false;
    protected final Color phaseColor = new Color(190, 100, 200, 150);


    public PhaseBuildUp(ShipAPI ship) {
        this.ship = ship;
    }

    public static class PhaseStack {
        private float duration;
        public PhaseStack(float duration) {
            //Cap max stacks here instead of in the onHit
            if (stacks.size() >= 100 || isPhased) return;
            this.duration = duration;
        }
    }

    private float jitterIntensity = 0.2f;

    @Override
    public void advance(float amount) {
        ArrayList<PhaseStack> copy = new ArrayList<>(stacks);
        for (PhaseStack stack : copy) {
            stack.duration -= amount;
            if (stack.duration < 0) {
                stacks.remove(stack);
            }
        }

        if (stacks.size() >= 10 && !isPhased) {
            stacks.add(new PhaseStack(10f));
            ship.setAlphaMult(0.3f);
            ship.setPhased(true);
            isPhased = true;
            Global.getSoundPlayer().playSound("gate_explosion", 0.2f, 0.8f, ship.getLocation(), new Vector2f());
            Global.getCombatEngine().addSwirlyNebulaParticle(
                    ship.getLocation(),
                    new Vector2f(),
                    ship.getCollisionRadius() * 2f,
                    0f,
                    -0.5f,
                    0.5f,
                    1.5f,
                    new Color(190, 100, 200, 200),
                    true
            );
        }
        if (isPhased) {
            ship.blockCommandForOneFrame(ShipCommand.FIRE);
            ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
            ship.setJitterUnder(ship, phaseColor, 2, 3, 5f);
            if (stacks.isEmpty()) {
                ship.setAlphaMult(1f);
                ship.setPhased(false);
                isPhased = false;
                Global.getSoundPlayer().playSound("gate_explosion", 0.2f, 0.8f, ship.getLocation(), new Vector2f());
                Global.getCombatEngine().addSwirlyNebulaParticle(
                        ship.getLocation(),
                        new Vector2f(),
                        (ship.getCollisionRadius() * 2f)/10f,
                        1.5f,
                        0.5f,
                        1f,
                        1.5f,
                        new Color(190, 100, 200, 200),
                        true
                );
            }
        } else {
            jitterIntensity = stacks.size() * 0.1f;
            ship.setJitter(ship, phaseColor, jitterIntensity, 3, 5f);
        }
    }
}

