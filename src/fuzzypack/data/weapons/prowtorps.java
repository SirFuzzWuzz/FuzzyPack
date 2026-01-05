package fuzzypack.data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.*;


public class prowtorps implements EveryFrameWeaponEffectPlugin {

    private boolean disabled = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || !weapon.getShip().isAlive() || disabled) return;
        if (weapon.isDisabled()) {
            weapon.repair();
        }
        if (weapon.getShip().getChildModulesCopy().isEmpty() || !weapon.getShip().getChildModulesCopy().get(0).isAlive()) {
            weapon.disable(true);
            disabled = true;
            weapon.getSprite().setColor(new Color(1,1,1,0));
        }


    }
}
