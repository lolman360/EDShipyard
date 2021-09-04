package data.scripts.shipsystems;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.util.StolenUtils;

public class SteamDriveStats extends BaseShipSystemScript {

	public float SPEED_BONUS = 125f;
	
	private final IntervalUtil tracker = new IntervalUtil(1f, 1f);
	private Color color = new Color(255,75,175,255);
	private Random rng = new Random();
	boolean releaseSound = false;
	boolean whistleSound = false;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			releaseSound = false; whistleSound = false;
		} else {
			stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS * effectLevel);
			stats.getAcceleration().modifyPercent(id, SPEED_BONUS * 3f * effectLevel);
			stats.getTurnAcceleration().modifyMult(id, 1 - effectLevel);
			stats.getMaxTurnRate().modifyMult(id, 1 - effectLevel);
		}
		
		CombatEngineAPI engine = Global.getCombatEngine();
		
		if (engine.isPaused()) {
            return;
        }
		
		if (stats.getEntity() instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			
			if(state == ShipSystemStatsScript.State.IN) {
				if (!releaseSound) {
	                Global.getSoundPlayer().playSound("ed_steam_release", 1f, 1f, ship.getLocation(), ship.getVelocity());
	                releaseSound = true;
	                float angle = ship.getFacing();
            		List<WeaponAPI> weaps = ship.getAllWeapons();
	                for (WeaponAPI w : weaps) {
	            		if(!w.isDecorative()) {
	            			continue;
	            		}	            		
	            		for(int i = 0; i < 24; i++) {
	            			engine.addSmokeParticle(w.getLocation(), MathUtils.getPoint(new Vector2f(), 100 + 150f*rng.nextFloat(), w.getSlot().getAngle() + angle + 24f*rng.nextFloat() - 12), 15f * (0.6f + 0.3f * rng.nextFloat()) * 3f, 0.10f + 0.01f*i, 2f, new Color(170, 170, 200, 20));
	            		}
	               	}
	            }
			}else if(state == ShipSystemStatsScript.State.ACTIVE){				
				if (!whistleSound) {
					tracker.advance(Global.getCombatEngine().getElapsedInLastFrame());
					if(tracker.intervalElapsed()) {
						Vector2f endPoint = new Vector2f(ship.getLocation());
				        endPoint.x += Math.cos(Math.toRadians(ship.getFacing())) * 1000;
				        endPoint.y += Math.sin(Math.toRadians(ship.getFacing())) * 1000;
				        
				        ShipAPI target = StolenUtils.getFirstNonFighterOnSegment(ship.getLocation(), endPoint, ship);
				        if(target != null) {
				        	//Global.getCombatEngine().addFloatingText(ship.getLocation(), target.getHullSpec().getBaseHullId(), 100, Color.RED, ship, 1, 1);
				        	Global.getSoundPlayer().playSound("ed_steam_whistle", 0.75f, 1.6f, ship.getLocation(), ship.getVelocity());
				            whistleSound = true;
				        }
					}
				}
			}
			
			ship.getEngineController().fadeToOtherColor(this, color, new Color(0,0,0,0), effectLevel, 0.67f);
			ship.getEngineController().extendFlame(this, 2f * effectLevel, 0f * effectLevel, 0f * effectLevel);
			
			List<ShipAPI> modules = ship.getChildModulesCopy();
        	if(modules != null) {
        		for(ShipAPI m : modules) {
        			m.getEngineController().fadeToOtherColor(this, color, new Color(0,0,0,0), effectLevel, 0.67f);
        			m.getEngineController().extendFlame(this, 2f * effectLevel, 0f * effectLevel, 0f * effectLevel);
        		}
        	}
        	
        	float angle = ship.getFacing();
        	float smokeSizeValue = 1f + effectLevel;
        	Global.getSoundPlayer().playLoop("ed_steam_loop", ship, 1f, 0.3f + 0.3f*effectLevel, ship.getLocation(), ship.getVelocity());        	
        	List<WeaponAPI> weaps = ship.getAllWeapons();
        	for (WeaponAPI w : weaps) {
        		if(!w.isDecorative()) {
        			continue;
        		}                
                engine.addSmokeParticle(w.getLocation(), MathUtils.getPoint(new Vector2f(), 200 + 50f*rng.nextFloat(), w.getSlot().getAngle() + angle + 16f*rng.nextFloat() - 8), 15f * (0.6f + 0.3f * rng.nextFloat()) * smokeSizeValue, 0.15f, 1f, new Color(170, 170, 200, 20));
                engine.addSmokeParticle(w.getLocation(), MathUtils.getPoint(new Vector2f(), 200 + 50f*rng.nextFloat(), w.getSlot().getAngle() + angle + 16f*rng.nextFloat() - 8), 15f * (0.6f + 0.3f * rng.nextFloat()) * smokeSizeValue, 0.1f, 1f, new Color(170, 170, 200, 20));
            }
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("+" + (int)(SPEED_BONUS * effectLevel) + " top speed", false);
		}
		return null;
	}
}
