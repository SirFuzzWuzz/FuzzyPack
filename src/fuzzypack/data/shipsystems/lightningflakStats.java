package fuzzypack.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class lightningflakStats extends BaseShipSystemScript {

	private float searchAoe = 300f;
	private int maxTargets = 10;
	private float dmg = 50;
	private float emp = 0;
	private IntervalUtil interval = new IntervalUtil(0.2f,0.2f);
	private List<CombatEntityAPI> trgtList = new ArrayList<>();
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		interval.advance(0.03f);
		if (state == State.IN && interval.intervalElapsed()){
			CombatEngineAPI engine =Global.getCombatEngine();
			ShipAPI ship = (ShipAPI) stats.getEntity();
			//List shipList = engine.getShips();
			engine.spawnEmpArc(ship, ship.getLocation(), ship,
					getRandomTarget(ship),
					DamageType.FRAGMENTATION,
					dmg, //dam
					emp, // emp
					100000f, // max range
					"shock_repeater_emp_impact",
					10, // thickness
					Color.GREEN,
					Color.WHITE
			);

		}
	}

	private CombatEntityAPI getRandomTarget(CombatEntityAPI ship) {
		Iterator<Object> iter = Global.getCombatEngine().getShipGrid().getCheckIterator(ship.getLocation(),searchAoe, searchAoe);
		while(iter.hasNext()) {
			ShipAPI trgt = (ShipAPI) iter.next();
			if (trgt != ship && !trgtList.contains(trgt)) {
				if (trgt.getOwner() == ship.getOwner()) {
					if(trgtList.size() < maxTargets) {
						trgtList.add(trgt);
					} else {
						break;
					}
				}
			}
		}
		return trgtList.get(MathUtils.getRandomNumberInRange(0, trgtList.size()-1));
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		trgtList.clear();
	}
}
