package data.scripts.shipsystems;

import java.awt.Color;
import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;

import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SpatialSwapStats extends BaseShipSystemScript {
	
	public ShipAPI ship;
	public ShipAPI target;
	public ArrayList<Integer> sizeRange;	
	public ShipAPI lockedTarget;
	public boolean teleported = false;
	
	private static final float MAX_RANGE = 4000;

    private static final Color EMP_CORE_COLOR = new Color(200, 175, 200, 255);
    private static final Color EMP_FRINGE_COLOR = new Color(150, 75, 150, 200);
    private static final Color JITTER_UNDER_COLOR = new Color(150, 50, 150, 100);
    private static final Color JITTER_OVER_COLOR = new Color(150, 150, 50, 100);
    private static final String SHIELDKEY = "SpatialSwapDamageReduction";

    private final IntervalUtil interval = new IntervalUtil(0.20f, 0.25f);
    
    private int getSize(ShipAPI test) {//add to utils later
    	switch(test.getHullSize()) {
    		case CAPITAL_SHIP: return 4;
    		case CRUISER: return 3;
    		case DESTROYER: return 2;
    		default: return 1;
    	}
    }

    private int isTargetValid(ShipAPI ship, ShipAPI target) {
    	    	
        if (target == null || ship == null) {
            return 7;
        }
        if(target.getOwner() != ship.getOwner()) {
        	return 6;
        }
        if(target.isPhased()) {
        	return 5;
        }        
        if(!target.isAlive()) {
        	return 4;
        }
        if(target.isStation()) {
        	return 3;
        }
        if(target.isStationModule()) {
        	return 2;
        }
        if(sizeRange != null && !sizeRange.contains(getSize(target))) {
        	return 1;
        }
        if(MathUtils.getDistance(ship, target) > MAX_RANGE) {
        	return 8;
        }
        MutableShipStatsAPI targetStats = target.getMutableStats();
		if(targetStats == null || targetStats.getHullDamageTakenMult().getMultMods().containsKey(SHIELDKEY)) {
			return 9;
		}
        return 0;
    }
    
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        
    	switch(system.getState()) {
        	case COOLDOWN: return "COOLING DOWN";
        	case IN: return "CHARGING UP";
        	case OUT: return "COOLING DOWN";
        	case ACTIVE: return "SHIELDS REINFORCED";
        	case IDLE: break;
        	default: return null;
        }        
        if(target == null || ship == null) {
        	 return "NO TARGET";
        }else {
        	switch(isTargetValid(ship,target)) {
        		case 9: return "ALREADY BEING SWAPPED";
        		case 8: return "OUT OF RANGE";
        		case 7: return "INVALID TARGET";
        		case 6: return "TARGET IS NOT ALLY";
        		case 5: return "TARGET IS PHASED";
        		case 4: return "CONNECTION LOST";
        		case 3: return "STATIONS ARE INVALID";
        		case 2: return "MODULES ARE INVALID";
        		case 1: return "TARGET MASS MISMATCH";
        		default: return "READY";
        	}
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
    	switch (state) {
        case ACTIVE: {
            if (index == 0) {
                return new StatusData("Shields Strengthened", false);
            }
            break;
        }
        default:
            break;
    }
    return null;
    }
    
    public void init(ShipAPI ship) {
    	this.ship = ship;
    	sizeRange = new ArrayList<Integer>();
    	int size = getSize(ship);
    	sizeRange.add(size - 1);
    	sizeRange.add(size);
    	sizeRange.add(size + 1);
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
    	if (this.ship == null) {
        	init(ship);
        }
    	target = ship.getShipTarget();
        return isTargetValid(ship,target) == 0;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    	stats.getShieldAbsorptionMult().unmodify(id);
		teleported = false;
		if(lockedTarget != null) {
			MutableShipStatsAPI targetStats = ship.getMutableStats();
			if(targetStats != null && targetStats.getHullDamageTakenMult().getMultMods().containsKey(SHIELDKEY)) {
				targetStats.getHullDamageTakenMult().unmodify(SHIELDKEY);
			}
			lockedTarget = null;
		}		
		if(ship != null) {
			ship.setJitterShields(false);
		}		
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {        
        if (ship == null) {
        	init((ShipAPI) stats.getEntity());
            return;
        }
        
        target = ship.getShipTarget();
        
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
	    if (Global.getCombatEngine().isPaused()) {
	        amount = 0f;
	    }
        
	    if(state == State.ACTIVE){
			if(!teleported) {
				teleported = true;
				if(lockedTarget != null) {
					MutableShipStatsAPI targetStats = lockedTarget.getMutableStats();
		            if(targetStats != null) {
		            	targetStats.getHullDamageTakenMult().unmodify(SHIELDKEY);
		            }
					if(lockedTarget.isAlive()) {
						CollisionClass col = ship.getCollisionClass();
						ship.setCollisionClass(CollisionClass.NONE);
						
						float shipRadius = lockedTarget.getCollisionRadius();
						float startSize = shipRadius;
						float endSize = (shipRadius * 5f);
			        	
			            RippleDistortion ripple2 = new RippleDistortion(lockedTarget.getLocation(), new Vector2f());
			            ripple2.setSize(endSize);
			            ripple2.setIntensity(20f);
			            ripple2.setFrameRate(60f / 0.3f);
			            ripple2.fadeInSize(0.6f * endSize / (endSize - startSize));
			            ripple2.fadeOutIntensity(0.5f);
			            ripple2.setSize(startSize);
			            DistortionShader.addDistortion(ripple2);
						
						ship.getVelocity().set(new Vector2f());
						lockedTarget.getVelocity().set(new Vector2f());
						Vector2f loc1 = new Vector2f(ship.getLocation());
						Vector2f loc2 = new Vector2f(lockedTarget.getLocation());
						ship.getLocation().set(loc2);
						lockedTarget.getLocation().set(loc1);
						
						ship.setCollisionClass(col);

						Global.getSoundPlayer().playSound("ed_mag2", 1f, 1f, ship.getLocation(), ship.getVelocity());
						Global.getSoundPlayer().playSound("ed_mag2", 1f, 1f, lockedTarget.getLocation(), lockedTarget.getVelocity());
				        
				        for(int i = 0; i < getSize(ship)*2; i++) {			        	
					        Global.getCombatEngine().spawnEmpArc(ship, ship.getLocation(), ship, lockedTarget, DamageType.ENERGY, 0f, 0f, MAX_RANGE*1.25f, null, 20f, EMP_FRINGE_COLOR, EMP_CORE_COLOR);
				        }
				        
				        for(int i = 0; i < getSize(lockedTarget); i++) {			        	
					        Global.getCombatEngine().spawnEmpArc(lockedTarget, lockedTarget.getLocation(), lockedTarget, ship, DamageType.ENERGY, 0f, 0f, MAX_RANGE*1.25f, null, 20f, EMP_FRINGE_COLOR, EMP_CORE_COLOR);
				        }
				        
				        lockedTarget.setJitter(this, JITTER_UNDER_COLOR, 1f, Math.round(5 + 5), getSize(ship)*100 + 50);
				        ship.setJitter(this, JITTER_UNDER_COLOR, 1f, Math.round(5 + 5), getSize(lockedTarget)*100 + 50);
				        
				        lockedTarget = null;
						ship.setShipTarget(null);
					}else {
						Global.getSoundPlayer().playSound("mawend", 1f, 1f, ship.getLocation(), ship.getVelocity());
						ship.getFluxTracker().forceOverload(3f);
						return;
					}					
				}				
			}
			stats.getShieldAbsorptionMult().modifyMult(id, 0.01f);
			ship.setJitterShields(true);
			ship.setJitterUnder(this, JITTER_UNDER_COLOR, 1f, 3, 5f, 25f);
		}else if(state == State.IN) {			
			
			if(lockedTarget == null) {				
				lockedTarget = target;
				MutableShipStatsAPI targetStats = lockedTarget.getMutableStats();		            
		        if(targetStats != null) {
		        	targetStats.getHullDamageTakenMult().modifyMult(SHIELDKEY, 1 - ((getSize(lockedTarget)+1f)*0.15f), "WARP DISTORTION SHIELD");		            	
		       	}
		        lockedTarget.setJitter(this, JITTER_OVER_COLOR, 1f, 3, 2f, 10f);
			}
			
			interval.advance(amount);
	        if (interval.intervalElapsed()) {
	        	
	        	float shipRadius = ship.getCollisionRadius();
	        	Vector2f offset = new Vector2f(-6f, 0f);
	     	    Vector2f centerLocation = Vector2f.add(ship.getLocation(), offset, new Vector2f());
	        	float startSize = shipRadius * 1.5f * effectLevel;
	            float endSize = (shipRadius * 2f) * effectLevel + 200f; 
	        	
	            RippleDistortion ripple1 = new RippleDistortion(centerLocation, new Vector2f());
	            ripple1.setSize(endSize);
	            ripple1.setIntensity(3f * (2f * effectLevel));
	            ripple1.setFrameRate(60f / 0.3f);
	            ripple1.fadeInSize(0.6f * endSize / (endSize - startSize));
	            ripple1.fadeOutIntensity(0.5f);
	            ripple1.setSize(startSize);
	            DistortionShader.addDistortion(ripple1);
	            
	            float jitterScale = 1f;
	            float jitterLevel = 1f + 1f*effectLevel;
	            float maxRangeBonus = 30f * jitterScale;
	            float jitterRangeBonus = ((0.5f + jitterLevel) / 1.5f) * maxRangeBonus;
	            
	            ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(5 * jitterScale), 0f, 3f + jitterRangeBonus);
	            Global.getSoundPlayer().playSound("kfp", MathUtils.getRandomNumberInRange(0.95f, 1.05f) * 0.5f ,0.75f , ship.getLocation(), ship.getVelocity());
	            
	            if(lockedTarget != null && lockedTarget.isAlive()) {
	            	shipRadius = lockedTarget.getCollisionRadius();
		     	    centerLocation = Vector2f.add(lockedTarget.getLocation(), offset, new Vector2f());
		        	startSize = shipRadius * 1.5f * effectLevel;
		            endSize = (shipRadius * 2f) * effectLevel + 200f; 
		        	
		            RippleDistortion ripple2 = new RippleDistortion(centerLocation, new Vector2f());
		            ripple2.setSize(endSize);
		            ripple2.setIntensity(3f * (2f * effectLevel));
		            ripple2.setFrameRate(60f / 0.3f);
		            ripple2.fadeInSize(0.6f * endSize / (endSize - startSize));
		            ripple2.fadeOutIntensity(0.5f);
		            ripple2.setSize(startSize);
		            DistortionShader.addDistortion(ripple2);
		            
		            lockedTarget.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, Math.round(5 * jitterScale), 0f, 3f + jitterRangeBonus);
		            lockedTarget.setJitter(this, JITTER_OVER_COLOR, 0.5f, 2, 0f, 5f);	            
		           	            
		            Global.getSoundPlayer().playSound("kfp", MathUtils.getRandomNumberInRange(0.95f, 1.05f) * 0.5f ,0.95f , lockedTarget.getLocation(), lockedTarget.getVelocity());
	            }
	        }	        
		}
    }    
}
