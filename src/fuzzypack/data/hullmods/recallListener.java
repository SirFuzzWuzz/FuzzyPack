package fuzzypack.data.hullmods;


import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;


/*public class recallListener implements AdvanceableListener {
        ShipAPI fighter;
        ShipAPI carrier;
        float acc = 0.01f;
        IntervalUtil interval = new IntervalUtil(0.3f,0.3f);
        Color JITTER_COLOR = new Color(100,165,255,155);

        public recallListener(ShipAPI fighter, ShipAPI carrier) {
            this.fighter = fighter;
            this.carrier = carrier;
        }
        @Override
        public void advance(float amount) {
            if (!fighter.isAlive() ||!carrier.isAlive()) return;
            fighter.setHoldFireOneFrame(true);
            if (interval.intervalElapsed()) {
                fighter.setAlphaMult(0.5f);
                fighter.setJitter(null, JITTER_COLOR, 1f, 3, 5f);
                float deltaX = carrier.getLocation().x - fighter.getLocation().x;
                float deltaY = carrier.getLocation().y - fighter.getLocation().y;
                float dist = MathUtils.getDistance(fighter.getLocation(), carrier.getLocation());
                fighter.getLocation().set(fighter.getLocation().x + deltaX*acc,
                        fighter.getLocation().y + deltaY*acc);
                if (dist < 200f) {
                    fighter.getWing().getSource().land(fighter);
                    return;
                }
            }
            interval.advance(amount);
        } //advance
    } */