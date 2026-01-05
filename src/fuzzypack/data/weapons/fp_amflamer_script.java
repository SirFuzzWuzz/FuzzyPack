package fuzzypack.data.weapons;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;


//Basically cryoflux script with spawnpoint added to the key for multibarrel flaming
public class fp_amflamer_script extends BaseCombatLayeredRenderingPlugin implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

	public fp_amflamer_script() {
	}

	protected List<fp_amflamer_script> trails;

	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (trails == null) return;

		Iterator<fp_amflamer_script> iter = trails.iterator();
		while (iter.hasNext()) {
			if (iter.next().isExpired()) iter.remove();
		}

		// sound loop playback
		if (weapon.getShip() != null) {
			float maxRange = weapon.getRange();
			ShipAPI ship = weapon.getShip();
			Vector2f com = new Vector2f();
			float weight = 0f;
			float totalDist = 0f;
			Vector2f source = weapon.getLocation();
			for (fp_amflamer_script curr : trails) {
				if (curr.proj != null) {
					Vector2f.add(com, curr.proj.getLocation(), com);
					weight += curr.proj.getBrightness();
					totalDist += Misc.getDistance(source, curr.proj.getLocation());
				}
			}
			if (weight > 0.1f) {
				com.scale(1f / weight);
				float volume = Math.min(weight, 1f);
				if (!trails.isEmpty()) {
					totalDist /= (float) trails.size();
					float mult = totalDist / Math.max(maxRange, 1f);
					mult = 1f - mult;
					if (mult > 1f) mult = 1f;
					if (mult < 0f) mult = 0f;
					mult = (float) Math.sqrt(mult);
					volume *= mult;
				}
				float pitch = 1f;
				float volumeMod = 1f;
				if (weapon.getSpec().getSize() == WeaponAPI.WeaponSize.MEDIUM) {
					pitch = 1.2f; volumeMod = 0.75f;
				}
				Global.getSoundPlayer().playLoop("flamer_loop", ship, pitch, volume * volumeMod, com, ship.getVelocity());
			}
		}
		float numIter = 1f; // more doesn't actually change anything
		amount /= numIter;
		// drag along the previous projectile, starting with the most recently launched; new ones are added at the start
		// note: prev is fired before and so is in front of proj
		for (int i = 0; i < numIter; i++) {
			for (fp_amflamer_script trail : trails) {
				//trail.proj.setFacing(trail.proj.getFacing() + 180f * amount);
				if (trail.prev != null && !trail.prev.isExpired() && Global.getCombatEngine().isEntityInPlay(trail.prev)) {
					float dist1 = Misc.getDistance(trail.prev.getLocation(), trail.proj.getLocation());
					if (dist1 < trail.proj.getProjectileSpec().getLength() * 1f) {
						float maxSpeed = trail.prev.getMoveSpeed() * 0.5f;// * Math.max(0.5f, 1f - trail.prev.getElapsed() * 0.5f);
						// goal here is to prevent longer shot series (e.g. from Paragon) from moving too unnaturally
						float e = trail.prev.getElapsed();
						float t = 0.5f;
						if (e > t) {
							maxSpeed *= Math.max(0.25f, 1f - (e - t) * 0.5f);
						}
						if (dist1 < 20f && e > t) {
							maxSpeed *= dist1 / 20f; //20f
						}

						Vector2f driftTo = Misc.closestPointOnLineToPoint(trail.proj.getLocation(), trail.proj.getTailEnd(), trail.prev.getLocation());
						float dist = Misc.getDistance(driftTo, trail.prev.getLocation());
						Vector2f diff = Vector2f.sub(driftTo, trail.prev.getLocation(), new Vector2f());
						diff = Misc.normalise(diff);
						diff.scale(Math.min(dist, maxSpeed * amount));
						Vector2f.add(trail.prev.getLocation(), diff, trail.prev.getLocation());
						Vector2f.add(trail.prev.getTailEnd(), diff, trail.prev.getTailEnd());
					}
				}
			}
		}
	}


	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		projectile.getDamage().setSoftFlux(true);

		String prevKey = "flamer_prev_" + weapon.getShip().getId() + "_" + weapon.getSlot().getId(); // + "_" + projectile.getSpawnLocation().toString()
		DamagingProjectileAPI prev = (DamagingProjectileAPI) engine.getCustomData().get(prevKey);

		fp_amflamer_script trail = new fp_amflamer_script(projectile, prev);
		CombatEntityAPI e = engine.addLayeredRenderingPlugin(trail);
		e.getLocation().set(projectile.getLocation());

		engine.getCustomData().put(prevKey, projectile);

		if (trails == null) {
			trails = new ArrayList<fp_amflamer_script>();
		}
		trails.add(0, trail);
	}



	public static class ParticleData {
		public SpriteAPI sprite;
		public Vector2f offset = new Vector2f();
		public Vector2f vel = new Vector2f();
		public float scale = 1f;
		public DamagingProjectileAPI proj;
		public float scaleIncreaseRate = 1f;
		public float turnDir = 1f;
		public float angle = 1f;
		public FaderUtil fader;

		public ParticleData(DamagingProjectileAPI proj) {
			this.proj = proj;
			sprite = Global.getSettings().getSprite("misc", "nebula_particles");
			//sprite = Global.getSettings().getSprite("misc", "dust_particles");
			float i = Misc.random.nextInt(4);
			float j = Misc.random.nextInt(4);
			sprite.setTexWidth(0.25f);
			sprite.setTexHeight(0.25f);
			sprite.setTexX(i * 0.25f);
			sprite.setTexY(j * 0.25f);
			sprite.setAdditiveBlend();

			angle = (float) Math.random() * 360f;

			float maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();
			scaleIncreaseRate = 4f / maxDur;
			scale = 1f;

			turnDir = Math.signum((float) Math.random() - 0.5f) * 60f * (float) Math.random();
			//turnDir = 0f;

			float driftDir = (float) Math.random() * 360f;
			vel = Misc.getUnitVectorAtDegreeAngle(driftDir);
			vel.scale(proj.getProjectileSpec().getWidth() / maxDur * 0.33f);

//			offset.x += vel.x * 1f;
//			offset.y += vel.y * 1f;
			fader = new FaderUtil(0f, 0.25f, 0.5f);
			fader.fadeIn();
		}

		public void advance(float amount) {
			scale += scaleIncreaseRate * amount;
			if (scale < 1f) {
				scale += scaleIncreaseRate * amount * 1f;
			}

			offset.x += vel.x * amount;
			offset.y += vel.y * amount;

			angle += turnDir * amount;

			fader.advance(amount);
		}
	}

	protected List<ParticleData> particles = new ArrayList<>();

	protected DamagingProjectileAPI proj;
	protected DamagingProjectileAPI prev;
	protected float baseFacing = 0f;
	public fp_amflamer_script(DamagingProjectileAPI proj, DamagingProjectileAPI prev) {
		this.proj = proj;
		this.prev = prev;

		baseFacing = proj.getFacing();

		int num = 7;
		for (int i = 0; i < num; i++) {
			particles.add(new fp_amflamer_script.ParticleData(proj));
		}

		float length = proj.getProjectileSpec().getLength();
		float width = proj.getProjectileSpec().getWidth();

		float index = 0;
		for (fp_amflamer_script.ParticleData p : particles) {
			float f = index / (particles.size() - 1);
			Vector2f dir = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);
			dir.scale(length * f);
			Vector2f.add(p.offset, dir, p.offset);

			p.offset = Misc.getPointWithinRadius(p.offset, width * 0.5f);
			//p.scale = 0.25f + 0.75f * (1 - f);

			index++;
		}
	}

	public float getRenderRadius() {
		return 300f;
	}


	protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return layers;
	}


	public void init(CombatEntityAPI entity) {
		super.init(entity);
	}

	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) return;

		entity.getLocation().set(proj.getLocation());

		for (ParticleData p : particles) {
			p.advance(amount);
		}
	}


	public boolean isExpired() {
		return proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
	}

	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		float x = entity.getLocation().x;
		float y = entity.getLocation().y;


		Color color = proj.getProjectileSpec().getFringeColor();
		color = Misc.setAlpha(color, 60);
		float b = proj.getBrightness();
		b *= viewport.getAlphaMult();

		for (ParticleData p : particles) {
			float size = proj.getProjectileSpec().getWidth() * 0.6f;
			size *= p.scale;

			float alphaMult = 1f;
			Vector2f offset = p.offset;
			float diff = Misc.getAngleDiff(baseFacing, proj.getFacing());
			if (Math.abs(diff) > 0.1f) {
				offset = Misc.rotateAroundOrigin(offset, diff);
			}
			Vector2f loc = new Vector2f(x + offset.x, y + offset.y);

			p.sprite.setAngle(p.angle);
			p.sprite.setSize(size, size);
			p.sprite.setAlphaMult(b * alphaMult * p.fader.getBrightness());
			p.sprite.setColor(color);
			p.sprite.renderAtCenter(loc.x, loc.y);
		}
	}

}
