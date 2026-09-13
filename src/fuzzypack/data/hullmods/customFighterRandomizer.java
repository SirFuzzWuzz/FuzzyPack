package fuzzypack.data.hullmods;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import com.fs.starfarer.api.util.Misc;


public class customFighterRandomizer extends BaseHullMod {

    private static final String SLOT = "WS0003";
    private static final String[] WEAPON_POOL = {
            "fp_custom_cannon_fighter",
            "fp_custom_blaster_fighter",
            "fp_custom_vulcan_fighter",
            "fp_custom_lance_fighter"
    };

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize size,
                                               MutableShipStatsAPI stats, String id) {
        if (Global.getCurrentState() == GameState.CAMPAIGN) return;
        ShipVariantAPI v = stats.getVariant();
        if (v == null || v.getSlot(SLOT) == null) return;
        v.clearSlot(SLOT);
        v.addWeapon(SLOT, WEAPON_POOL[Misc.random.nextInt(WEAPON_POOL.length)]);
        v.getWeaponGroups().add(new WeaponGroupSpec(WeaponGroupType.LINKED));
        v.assignUnassignedWeapons();
    }
}
