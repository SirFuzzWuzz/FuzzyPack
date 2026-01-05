package fuzzypack.data.weapons.onHit;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

import java.awt.*;

public class rod_onhit implements OnHitEffectPlugin {

        private final float baseCRdamage = 0.28f; //-0.05 per hulllsize above frigate
        private final float crReductionHullsize = 0.06f;
        private final float shieldSoftFluxDamage = 1000f;

        public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                          Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
            engine.addSwirlyNebulaParticle(
                    point, //loc
                    new Vector2f(0,0), //vel
                    200f, //size
                    1.2f, //endsize mult?
                    0.2f, //rampup frac
                    1f, //fullbright frac
                    3f, //total
                    new Color(80,200,10,200),
                    false);
            if (target instanceof ShipAPI) {
                if (shieldHit) {
                    float currFlux = ((ShipAPI) target).getFluxTracker().getCurrFlux();
                    ((ShipAPI) target).getFluxTracker().setCurrFlux(currFlux + shieldSoftFluxDamage);
                    return;
                }
                float targetCr = ((ShipAPI) target).getCurrentCR();
                float CRmod = baseCRdamage;
                switch (((ShipAPI) target).getHullSize()) {
                    case FRIGATE:
                        break;
                    case DESTROYER:
                        CRmod -= crReductionHullsize;
                        break;
                    case CRUISER:
                        CRmod -= 2*crReductionHullsize;
                        break;
                    case CAPITAL_SHIP:
                        CRmod -= 3*crReductionHullsize;
                        break;
                    case FIGHTER:
                        return;
                    case DEFAULT:
                        break;
                } //switch

                int[] cellCoords = ((ShipAPI) target).getArmorGrid().getCellAtLocation(point);
                if(cellCoords == null) return;
                float reductionArmour = 1 - (((ShipAPI) target).getArmorGrid().getArmorValue(cellCoords[0], cellCoords[1])
                        / ((ShipAPI) target).getArmorGrid().getMaxArmorInCell());
                CRmod = CRmod * reductionArmour;

                ((ShipAPI) target).setCurrentCR(targetCr - CRmod);
            }
        }
}
