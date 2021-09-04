package data.scripts.shipsystems;

import java.awt.Color;
import java.util.ArrayList;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;

public class WurgSensorStats extends BaseShipSystemScript {

	public static final float WURG_SENSOR_RANGE_PERCENT = 120f;
	public static final float WURG_WEAPON_RANGE_PERCENT = 60f;
	
	private ShipAPI ship;
    private ShipAPI ShieldModuleL; 
    private ShipAPI ShieldModuleR;
    private ShipAPI WeaponModuleL; 
    private ShipAPI WeaponModuleR; 
    private ShipAPI HangarModuleL;  
    private ShipAPI HangarModuleR;  
    private ShipAPI EngineModuleB;
    private MutableShipStatsAPI ShieldModuleLStats; 
    private MutableShipStatsAPI ShieldModuleRStats;
    private MutableShipStatsAPI WeaponModuleLStats; 
    private MutableShipStatsAPI WeaponModuleRStats; 
    private MutableShipStatsAPI HangarModuleLStats;  
    private MutableShipStatsAPI HangarModuleRStats;  
    private MutableShipStatsAPI EngineModuleBStats;
    private ArrayList<WeaponAPI> eyes;
    
    public static final String wurg_SML = "edshipyard_wurg_jawleft";
    public static final String wurg_SMR = "edshipyard_wurg_jawright";
    public static final String wurg_WML = "edshipyard_wurg_weaponleft";
    public static final String wurg_WMR = "edshipyard_wurg_weaponright";
    public static final String wurg_HML = "edshipyard_wurg_hangarleft";
    public static final String wurg_HMR = "edshipyard_wurg_hangarright";
    public static final String wurg_BTC = "edshipyard_wurg_buttocks";
    
    private static final Color JITTER_UNDER_COLOR = new Color(225, 175, 225, 125);
    
    private static final Vector2f ZERO = new Vector2f();
    
    private final IntervalUtil interval = new IntervalUtil(1f, 1f);
    
    private int currentFrame;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		float sensorRangePercent = WURG_SENSOR_RANGE_PERCENT * effectLevel;
		float weaponRangePercent = WURG_WEAPON_RANGE_PERCENT * effectLevel;
		
		if (ship == null) { 
            ship=(ShipAPI) stats.getEntity();
            //stats = ship.getMutableStats();
            currentFrame = 0;
            //get the weapon, all the sprites and sizes
            for (ShipAPI m : ship.getChildModulesCopy()) {
                switch(m.getHullSpec().getBaseHullId()) {                    
                    case wurg_SML:
                    	ShieldModuleL = m;
                    	ShieldModuleLStats = m.getMutableStats();
                        break;                        
                    case wurg_SMR:
                    	ShieldModuleR = m;
                    	ShieldModuleRStats = m.getMutableStats();
                        break;
                    case wurg_WML:
                    	WeaponModuleL = m;
                    	WeaponModuleLStats = m.getMutableStats();
                        break;
                    case wurg_WMR:
                    	WeaponModuleR = m;
                    	WeaponModuleRStats = m.getMutableStats();
                        break;
                    case wurg_HML:
                    	HangarModuleL = m;
                    	HangarModuleLStats = m.getMutableStats();
                        break;
                    case wurg_HMR:
                    	HangarModuleR = m;
                    	HangarModuleRStats = m.getMutableStats();
                        break;
                    case wurg_BTC:
                    	EngineModuleB = m;
                    	EngineModuleBStats = m.getMutableStats();
                        break;
                }
            }
            eyes = new ArrayList<WeaponAPI>();
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.isDecorative() && w.getDisplayName().contains("Eye")) {
                    eyes.add(w);
                }
            }
            return;
        }
		
		stats.getSightRadiusMod().modifyPercent(id, sensorRangePercent);
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		
		float amount = Global.getCombatEngine().getElapsedInLastFrame();
	    if (Global.getCombatEngine().isPaused()) {
	        amount = 0f;
	    }

	    float shipRadius = effectiveRadius(ship);
	    Vector2f offset = new Vector2f(-6f, 0f);
	    Vector2f centerLocation = Vector2f.add(ship.getLocation(), offset, new Vector2f());
	    
	    float startSize = shipRadius * 1.5f * effectLevel;
        float endSize = (shipRadius * 2f) * effectLevel + 200f;        
        
        if (state == State.COOLDOWN || state == State.IDLE) {        	
			unapply(stats, id);
			return;
		}
        
        for(WeaponAPI w : eyes) {
    		w.getAnimation().setFrame(currentFrame);            		
    	}
        if(state == State.ACTIVE) {
        	if(currentFrame < 19) {
        		currentFrame++;
        	}
        }else {
        	if(currentFrame > 0) {
        		currentFrame--;
        	}
        }
        
        if(ship.getFluxLevel() > 0.99f) {
    		ship.getFluxTracker().forceOverload(10f);
    		return;
    	}

        interval.advance(amount);
        if (interval.intervalElapsed()) {
        	
            RippleDistortion ripple = new RippleDistortion(centerLocation, ZERO);
            ripple.setSize(endSize);
            ripple.setIntensity(1f * (2f * effectLevel));
            ripple.setFrameRate(60f / 0.3f);
            ripple.fadeInSize(0.3f * endSize / (endSize - startSize));
            ripple.fadeOutIntensity(0.5f);
            ripple.setSize(startSize);
            DistortionShader.addDistortion(ripple);

            Global.getSoundPlayer().playSound("wurg_sensor", 1f, 0.3f + effectLevel*0.2f, ship.getLocation(), ship.getVelocity());
            
            float jitterScale = 1f;
            float jitterLevel = 0.5f + 0.5f*effectLevel;
            float maxRangeBonus = 30f * jitterScale;
            float jitterRangeBonus = ((0.5f + jitterLevel) / 1.5f) * maxRangeBonus;
            
            ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(5 * jitterScale), 0f, 3f + jitterRangeBonus);
            
            if(ShieldModuleL != null && ShieldModuleL.isAlive()) {
            	ShieldModuleL.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(4 * jitterScale), 0f, 3f + jitterRangeBonus);    			
    		}
            if(ShieldModuleR != null && ShieldModuleR.isAlive()) {
            	ShieldModuleR.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(4 * jitterScale), 0f, 3f + jitterRangeBonus);    			
    		}
            if(WeaponModuleL != null && WeaponModuleL.isAlive()) {
            	WeaponModuleL.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(3 * jitterScale), 0f, 3f + jitterRangeBonus);    			
    		}
            if(WeaponModuleR != null && WeaponModuleR.isAlive()) {
            	WeaponModuleR.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(3 * jitterScale), 0f, 3f + jitterRangeBonus);    			
    		}
            if(HangarModuleL != null && HangarModuleL.isAlive()) {
            	HangarModuleL.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(4 * jitterScale), 0f, 3f + jitterRangeBonus);    			
    		}
            if(HangarModuleR != null && HangarModuleR.isAlive()) {
            	HangarModuleR.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(4 * jitterScale), 0f, 3f + jitterRangeBonus);    			
    		}
            if(EngineModuleB != null && EngineModuleB.isAlive()) {
            	EngineModuleB.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(4 * jitterScale), 0f, 3f + jitterRangeBonus);    			
    		}
            //no jitter for the drone control module because it doesn't look good
        }
		
		if(ShieldModuleLStats != null) {
			ShieldModuleLStats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
			ShieldModuleLStats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		}
		
		if(ShieldModuleRStats != null) {
			ShieldModuleRStats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
			ShieldModuleRStats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		}
		
		if(WeaponModuleLStats != null) {
			WeaponModuleLStats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
			WeaponModuleLStats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		}
		
		if(WeaponModuleRStats != null) {
			WeaponModuleRStats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
			WeaponModuleRStats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		}
		
		if(HangarModuleLStats != null) {
			HangarModuleLStats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
			HangarModuleLStats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		}
		
		if(HangarModuleRStats != null) {
			HangarModuleRStats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
			HangarModuleRStats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		}
		
		if(EngineModuleBStats != null) {
			EngineModuleBStats.getBallisticWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
			EngineModuleBStats.getEnergyWeaponRangeBonus().modifyPercent(id, weaponRangePercent);
		}
		
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getSightRadiusMod().unmodify(id);
		stats.getBallisticWeaponRangeBonus().unmodify(id);
		stats.getEnergyWeaponRangeBonus().unmodify(id);
		
		if(ShieldModuleLStats != null) {
			ShieldModuleLStats.getBallisticWeaponRangeBonus().unmodify(id);
			ShieldModuleLStats.getEnergyWeaponRangeBonus().unmodify(id);
		}		
		if(ShieldModuleRStats != null) {
			ShieldModuleRStats.getBallisticWeaponRangeBonus().unmodify(id);
			ShieldModuleRStats.getEnergyWeaponRangeBonus().unmodify(id);
		}		
		if(WeaponModuleLStats != null) {
			WeaponModuleLStats.getBallisticWeaponRangeBonus().unmodify(id);
			WeaponModuleLStats.getEnergyWeaponRangeBonus().unmodify(id);
		}		
		if(WeaponModuleRStats != null) {
			WeaponModuleRStats.getBallisticWeaponRangeBonus().unmodify(id);
			WeaponModuleRStats.getEnergyWeaponRangeBonus().unmodify(id);
		}		
		if(HangarModuleLStats != null) {
			HangarModuleLStats.getBallisticWeaponRangeBonus().unmodify(id);
			HangarModuleLStats.getEnergyWeaponRangeBonus().unmodify(id);
		}		
		if(HangarModuleRStats != null) {
			HangarModuleRStats.getBallisticWeaponRangeBonus().unmodify(id);
			HangarModuleRStats.getEnergyWeaponRangeBonus().unmodify(id);
		}
		if(EngineModuleBStats != null) {
			EngineModuleBStats.getBallisticWeaponRangeBonus().unmodify(id);
			EngineModuleBStats.getEnergyWeaponRangeBonus().unmodify(id);
		}
	}
	
    public static float effectiveRadius(ShipAPI ship) {
        if (ship.getSpriteAPI() == null || ship.isPiece()) {
            return ship.getCollisionRadius();
        } else {
            float fudgeFactor = 1.5f;
            return ((ship.getSpriteAPI().getWidth() / 2f) + (ship.getSpriteAPI().getHeight() / 2f)) * 0.5f * fudgeFactor;
        }
    }
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float sensorRangePercent = WURG_SENSOR_RANGE_PERCENT * effectLevel;
		float weaponRangePercent = WURG_WEAPON_RANGE_PERCENT * effectLevel;
		if (index == 0) {
			return new StatusData("sensor range +" + (int) sensorRangePercent + "%", false);
		} else if (index == 1) {
			//return new StatusData("increased energy weapon range", false);
			return null;
		} else if (index == 2) {
			return new StatusData("weapon range +" + (int) weaponRangePercent + "%", false);
		}
		return null;
	}
}
