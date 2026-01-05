package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class missilepicker extends BaseHullMod {


    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    }
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    }
    private boolean weaponChosen = false;
    private boolean once = true;
    public void advanceInCombat(ShipAPI ship, float amount) {
        if ((ship.isFinishedLanding() && !weaponChosen) || once) {
            ShipAPI carrier = ship.getWing().getSourceShip();
            List<WeaponAPI> wpnList = carrier.getAllWeapons();

            List<WeaponAPI> foundWpns = new ArrayList<>();
            for (WeaponAPI wpn : wpnList) {
                if (wpn.getType() == WeaponAPI.WeaponType.MISSILE) {
                    foundWpns.add(wpn);
                }
            }
            if (foundWpns == null) return;
            String wpnId = foundWpns.get(MathUtils.getRandomNumberInRange(0, foundWpns.size())).getId();

            ship.getVariant().addWeapon("slot_id", wpnId);
            weaponChosen = true;
            once = false;
        }

        if (ship.isLanding()) {
            weaponChosen = false;
        }

    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        return null;
    }
}

