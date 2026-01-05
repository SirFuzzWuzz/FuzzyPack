package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.*;

import java.util.HashMap;
import java.util.Map;

public class dictator_wpn_swapper extends BaseHullMod
{
    public static final String WEAPON_SLOT = "WS0026";
    public static final String WEAPON_PREFIX = "fp_dictator_";

    public static final String MODULE_SLOT = "WS0028";
    public static final String MODULE_PREFIX = "fp_dictator_module_";

    // points to the next weapon/hullmod suffix
    public static final Map<String, String> LOADOUT_CYCLE = new HashMap<>();

    static
    {
        LOADOUT_CYCLE.put("torpedo", "novacannon");
        LOADOUT_CYCLE.put("novacannon", "torpedo");
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        if (stats.getEntity() == null) return;

        //WEAPONS
        // trigger a weapon switch if none of the selector hullmods are present (because one was removed, or because the ship was just spawned without one)
        boolean switchLoadout = true;
        for (String mode : LOADOUT_CYCLE.values())
        {
            if (stats.getVariant().getHullMods().contains("dictator_prow_mode_" + mode))
            {
                if (stats.getVariant().getWeaponId(WEAPON_SLOT) == null) {
                    stats.getVariant().addWeapon(WEAPON_SLOT, WEAPON_PREFIX + mode);
                }
                switchLoadout = false;
                break;
            }
        }

        if (switchLoadout)
        {
            // DEFAULT
            String newWeawpon = "torpedo";
            for (String key : LOADOUT_CYCLE.keySet())
            {
                // cycle to whatever the next weapon is, based on the weapon currently in the slot
                if (stats.getVariant().getWeaponId(WEAPON_SLOT) != null && stats.getVariant().getWeaponId(WEAPON_SLOT).contains(key))
                {
                    newWeawpon = LOADOUT_CYCLE.get(key);
                }
            }

            // add hullmod to match new weapons
            stats.getVariant().addMod("dictator_prow_mode_" + newWeawpon);

            // clear slot
            stats.getVariant().clearSlot(WEAPON_SLOT);
            // add gun
            stats.getVariant().addWeapon(WEAPON_SLOT, WEAPON_PREFIX + newWeawpon);

            // module
            stats.getVariant().setModuleVariant(MODULE_SLOT, null);
            ShipVariantAPI moduleVariant = Global.getSettings().getVariant(MODULE_PREFIX + newWeawpon + "_Hull");
            stats.getVariant().setModuleVariant(MODULE_SLOT, moduleVariant);


            /*System.out.println("Module variant id: " + moduleVariant.getHullVariantId());
            System.out.println("Current module: " + stats.getVariant().getModuleVariant(stats.getVariant().getModuleSlots().get(0)).getHullVariantId());
            System.out.println("Module slot id: " + stats.getVariant().getModuleSlots().get(0));*/

        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        if(ship.getOriginalOwner()<0){
            //undo fix for weapons put in cargo
            if(
                    Global.getSector()!=null &&
                            Global.getSector().getPlayerFleet()!=null &&
                            Global.getSector().getPlayerFleet().getCargo()!=null &&
                            Global.getSector().getPlayerFleet().getCargo().getStacksCopy()!=null &&
                            !Global.getSector().getPlayerFleet().getCargo().getStacksCopy().isEmpty()
            ){
                for (CargoStackAPI s : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()){
                    if(
                            s.isWeaponStack()
                                    && s.getWeaponSpecIfWeapon().getWeaponId().startsWith("fp_dictator_")
                    ){
                        Global.getSector().getPlayerFleet().getCargo().removeStack(s);
                    }
                }
            }
        }
    }

    @Override
    public int getDisplayCategoryIndex()
    {
        return 2;
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        return "without the normal CR penalty";
    }
}
