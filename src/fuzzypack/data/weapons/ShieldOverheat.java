package fuzzypack.data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;


import java.awt.Color;
import java.util.ArrayList;

//Kotlin is bad and you should feel bad
public class ShieldOverheat implements AdvanceableListener {
    private ShipAPI ship;
    public ArrayList<OverheatStack> stacks = new ArrayList<>();

    public ShieldOverheat(ShipAPI ship) {
        this.ship = ship;
    }

    public static class OverheatStack {
        private float duration;
        public OverheatStack(float duration) {
            this.duration = duration;
        }
    }

    @Override
    public void advance(float amount) {
        ArrayList<OverheatStack> copy = new ArrayList<>(stacks);
        for (OverheatStack stack : copy) {
            stack.duration -= amount;

            if (stack.duration < 0) {
                stacks.remove(stack);
            }
        }

        float mult = 0.005f; //Each stack adds 0.5%
        mult *= stacks.size();

        //Do the thing
        ship.getMutableStats().getShieldDamageTakenMult().modifyMult("fp_shield_overheat", 1f + mult);
        ship.setJitterUnder(ship, new Color(255,70,0,255), 3*mult, 1, 1f);

        //System.out.println("Shield Damage taken mult: " + ship.getMutableStats().getShieldDamageTakenMult().getModifiedValue());
    }
}

