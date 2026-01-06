package fuzzypack.data.weapons;


import com.fs.starfarer.api.combat.*;
import java.util.List;
import com.fs.starfarer.api.util.IntervalUtil;
import fuzzypack.data.weapons.effects.PhaseBuildUp;
import fuzzypack.data.weapons.effects.ShieldOverheat;

//import com.fs.starfarer.api.util.Misc;  OnFireEffectPlugin

public class phasebeam implements BeamEffectPlugin {
    
    private IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.2f);
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {
            float dur = beam.getDamage().getDpsDuration();
            // needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
            fireInterval.advance(dur);
            if (fireInterval.intervalElapsed()) {
                ShipAPI ship = (ShipAPI) target;
                boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
                if (!hitShield) {
                    if (!ship.hasListenerOfClass(PhaseBuildUp.class)) {
                        ship.addListener(new PhaseBuildUp(ship));
                    }
                    List<PhaseBuildUp> listeners = ship.getListeners(PhaseBuildUp.class);
                    if (listeners.isEmpty()) return;
                    PhaseBuildUp listener = listeners.get(0);
                    if (listener == null) return;
                    listener.stacks.add(new PhaseBuildUp.PhaseStack(3f));
                }
            }
        }
    }


}