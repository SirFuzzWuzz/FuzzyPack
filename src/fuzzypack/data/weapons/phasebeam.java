package fuzzypack.data.weapons;


import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import fuzzypack.data.weapons.effects.PhaseBuildUp;
import java.awt.Color;


public class phasebeam implements BeamEffectPlugin {

    private final IntervalUtil fireInterval = new IntervalUtil(0.2f, 0.2f);

    // Visual
    private final Color WOBBLE_COLOR = new Color(190, 100, 200, 150);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        if (!(target instanceof ShipAPI ship) || beam.getBrightness() < 1f) return;

        fireInterval.advance(beam.getDamage().getDpsDuration());
        if (!fireInterval.intervalElapsed()) return;

        boolean hitShield = ship.getShield() != null && ship.getShield().isWithinArc(beam.getTo());
        if (hitShield) return;

        PhaseBuildUp.get(ship).addStack(3f);
        engine.spawnEmpArcVisual(beam.getRayEndPrevFrame(), beam.getSource(), target.getLocation(), target,
                40f, WOBBLE_COLOR, new Color(255, 255, 255, 100));
    }


    /* private void drawField(ShipAPI ship, BeamAPI beam) {
        if (ship.isPhased()) return;

        Vector2f hit = new Vector2f(beam.getTo());
        Vector2f c = ship.getLocation();
        float rad = Math.max(ship.getCollisionRadius(), 40f);
        Vector2f exit = MathUtils.getPointOnCircumference(
                c, rad, VectorUtils.getAngle(c, hit) + 180f);

        float dx = exit.x - hit.x;
        float dy = exit.y - hit.y;
        float len = Math.max((float) Math.sqrt(dx * dx + dy * dy), 1f);
        float px = -dy / len;
        float py = dx / len;

        int lines = 4;
        int segs = 200;

        float time = Global.getCombatEngine().getTotalElapsedTime(false);

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 1; i <= lines; i++) {
                float bulge = rad * (0.4f + 0.35f * i);
                float phase = i * 1.7f + side * 0.9f;
                float wobble = 1f + 0.18f * (float) Math.sin(time * 6f + phase);
                float shift = rad * 0.08f * (float) Math.sin(time * 4f + phase * 1.3f);

                for (int s = 0; s <= segs; s++) {
                    float t = s / (float) segs;
                    float bow = (float) Math.sin(Math.PI * t) * bulge * side * wobble;
                    Vector2f p = new Vector2f(
                            hit.x + dx * t + px * (bow + shift),
                            hit.y + dy * t + py * (bow + shift));
                    Global.getCombatEngine().addHitParticle(
                            p, ship.getVelocity(),
                            10f, 1f, 0.08f, WOBBLE_COLOR);
                }
            }
        }
    } */
}