package fuzzypack.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import java.util.ArrayList;
import java.util.Collection;
//ty banano for some lazylib vector help, lucas for advanceable listener setup

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import static javax.swing.Action.DEFAULT;


public class reactivearmour_alt extends BaseHullMod {
    
    private final float triggerUnder = 50f; //%
    private final float HE_resist = 0.85f;
    private final float EMP_resist = 0.75f;
    private final float Armor_resist = 0.9f;
    
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        switch (id) {
            case "fp_reactivearmour":
                stats.getHighExplosiveDamageTakenMult().modifyMult(id, HE_resist);
                break;
            case "fp_reactivearmour_kinetic":
                stats.getEmpDamageTakenMult().modifyMult(id,EMP_resist);
                break;
            case "fp_reactivearmour_frag":
                stats.getArmorDamageTakenMult().modifyMult(id,Armor_resist);
                //stats.getHighExplosiveDamageTakenMult().modifyMult(id, HE_resist);
                break;
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new listener(ship, id));
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        Collection<String> hullmods = ship.getVariant().getHullMods();

        for (String hullmodId: hullmods) {
            switch (hullmodId) {
                case "fp_reactivearmour":
                    return !ship.getVariant().hasHullMod("fp_reactivearmour_kinetic") || !ship.getVariant().hasHullMod("fp_reactivearmour_frag");

                case "fp_reactivearmour_kinetic":
                    return !ship.getVariant().hasHullMod("fp_reactivearmour") || !ship.getVariant().hasHullMod("fp_reactivearmour_frag");

                case "fp_reactivearmour_frag":
                    return !ship.getVariant().hasHullMod("fp_reactivearmour") || !ship.getVariant().hasHullMod("fp_reactivearmour_kinetic");
            }
        }
        return true;
    }


    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return "Can only install one type of Reactive Armour";

        /*if (ship.getVariant().hasHullMod("fp_reactivearmour_kinetic")) {
            return "Can only install one type of Reactive Armour";
        }
        return null;*/
    }

    class listener implements AdvanceableListener {

        CombatEngineAPI engine = Global.getCombatEngine();

        ShipAPI ship;
        String id;
        
        ArrayList<Vector2f> firedCells = new ArrayList<Vector2f>();


        public listener(ShipAPI ship, String id) {
            this.ship = ship;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            if (ship.isHulk()) return;

            ArmorGridAPI grid = ship.getArmorGrid();

            int gridWidth = grid.getGrid().length;
            int gridHeight = grid.getGrid()[0].length;
            

            for (int x = 0; x < gridWidth; x++) {
                for (int y = 0; y < gridHeight; y++) {

                    Vector2f arr = new Vector2f(x,y);

                    if (grid.getArmorValue(x, y) <= grid.getMaxArmorInCell() * (triggerUnder/100) && !firedCells.contains(arr)) {
                        
                        firedCells.add(arr);
                        ship.getMutableStats().getHighExplosiveDamageTakenMult().unmodify(id);
                        ship.getMutableStats().getEmpDamageTakenMult().unmodify(id);
                        ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                        
                        //To spawn the projectile
                        Vector2f vec = grid.getLocation(x, y);
                        float angle = VectorUtils.getAngle(ship.getLocation(), vec); //from, too
                        //WeaponAPI weapon = engine.createFakeWeapon(ship, "hellbore");
                        //                    ship  weapAPI weapName
                        if(ship.getHullSize() == HullSize.FIGHTER) {
                            engine.spawnProjectile(ship, null, "fp_reactivecharge_fighter", vec, angle, ship.getVelocity());
                        } else {
                            switch (id) {
                                case "fp_reactivearmour":
                                    engine.spawnProjectile(ship, null, "fp_reactivecharge", vec, angle, ship.getVelocity()); //ship and weapon can be null
                                    break;
                                case "fp_reactivearmour_kinetic":
                                    for (int i = 0; i < 2; i++) {
                                        engine.spawnProjectile(ship, null, "fp_reactivekinetic", vec,
                                                MathUtils.getRandomNumberInRange(angle -5f, angle +5f), ship.getVelocity());
                                    }
                                    break;
                                case "fp_reactivearmour_frag":
                                    for (int i = 0; i < 2; i++) {
                                        engine.spawnProjectile(ship, null, "devastator", vec,
                                                MathUtils.getRandomNumberInRange(angle -5f, angle +5f), ship.getVelocity());
                                    }
                                    break;
                                case DEFAULT:
                                    break;
                            }
                        }
                    }
                    
                    if (grid.getArmorValue(x, y) > grid.getMaxArmorInCell()/2 && firedCells.contains(arr)) {
                        firedCells.remove(arr);
                    }
                    
                } //for
            } //for
            
        } //advance
    }
    
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round(triggerUnder) + "%";
        //if (index == 1) return Math.round((1-HE_resist)*100) + "%";
        return null;
    }
}