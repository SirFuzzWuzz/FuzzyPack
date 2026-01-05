package fuzzypack.data.weapons;


import com.fs.starfarer.api.combat.CombatEngineAPI;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;

import com.fs.starfarer.api.combat.WeaponAPI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RepLevel;


public class dudhammer implements OnFireEffectPlugin {

        
        public dudhammer() {}

        @Override
        public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
            float rng = 0.25f;
            if (Global.getSector().getFaction("luddic_path").getRelToPlayer().getLevel() == RepLevel.COOPERATIVE ||
                    Global.getSector().getFaction("luddic_church").getRelToPlayer().getLevel() == RepLevel.COOPERATIVE) {
                rng = 1;
            }
            if (Math.random() < rng) {
                projectile.getDamage().setType(DamageType.FRAGMENTATION);
                MissileAPI missile = (MissileAPI) projectile;
                missile.flameOut();
            }
        }
}
