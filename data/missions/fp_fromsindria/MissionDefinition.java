package data.missions.fp_fromsindria;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.PLAYER, "LGS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "HSS", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "Lions Guard Defense Fleet");
        api.setFleetTagline(FleetSide.ENEMY, "Hegemony Armed Relief Fleet (Heading towards Volturn)");

        api.addBriefingItem("Tear the enemy fleet apart");
        api.addBriefingItem("The LGS Executor's Will must survive");

        api.addToFleet(FleetSide.PLAYER, "fp_bigboi_pride", FleetMemberType.SHIP, "LGS Executor's Will", true);
        api.addToFleet(FleetSide.PLAYER, "fp_adjucator_chosen", FleetMemberType.SHIP, "LGS Inquisitor", false);
        api.addToFleet(FleetSide.PLAYER, "eagle_LG_Assault", FleetMemberType.SHIP, "LGS Law", false);
        api.addToFleet(FleetSide.PLAYER, "fp_severance_chosen", FleetMemberType.SHIP, "LGS Pride of Askonia", false);
        api.addToFleet(FleetSide.PLAYER, "sunder_LG_Assault", FleetMemberType.SHIP, "LGS Order", false);
        api.addToFleet(FleetSide.PLAYER, "centurion_LG_Assault", FleetMemberType.SHIP, "TTS Beast of Volturn", false);
        api.addToFleet(FleetSide.PLAYER, "centurion_LG_Assault", FleetMemberType.SHIP, "TTS Claw", false);

        FactionAPI hegemony = Global.getSettings().createBaseFaction(Factions.HEGEMONY);
        FleetMemberAPI member;
        member = api.addToFleet(FleetSide.ENEMY, "onslaught_Standard", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "onslaught_Outdated", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "mora_Strike", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "dominator_Assault", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "dominator_Support", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "enforcer_Balanced", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "enforcer_Assault", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "enforcer_Balanced", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "lasher_Strike", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "lasher_Assault", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "colossus_Standard", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "colossus_Standard", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
        member = api.addToFleet(FleetSide.ENEMY, "buffalo_hegemony_Standard", FleetMemberType.SHIP, false);
        member.setShipName(hegemony.pickRandomShipName());
      

        api.defeatOnShipLoss("LGS Executor's Will");

        float width = 18000f;
        float height = 16000f;
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

        for (int i = 0; i < 6; i++) {
            float x = (float) Math.random() * width - width / 2;
            float y = (float) Math.random() * height - height / 2;
            float radius = 100f + (float) Math.random() * 400f;
            api.addNebula(x, y, radius);
        }

        api.addObjective(width * 0.35f, -height * 0.1f, "nav_buoy");
        api.addObjective(-width * 0.35f, -height * 0.1f, "nav_buoy");
        //api.addObjective(0f, -height * 0.3f, "sensor_array");
        api.addObjective(width * 0.2f, height * 0.35f, "comm_relay");
        api.addObjective(-width * 0.2f, height * 0.35f, "comm_relay");

        api.addNebula(0f, -height * 0.3f, 1000f);
        api.addNebula(width * 0.15f, -height * 0.05f, 2000f);
        //api.addNebula(-width * 0.15f, -height * 0.05f, 2000f);

        api.addRingAsteroids(0f, 0f, 40f, width, 30f, 40f, 400);
        
        api.addPlanet(0, 0, 350f, "water", 0f, true);
    }
}
