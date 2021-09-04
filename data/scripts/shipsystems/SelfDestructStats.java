package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;

import org.lazywizard.lazylib.combat.CombatUtils;

public class SelfDestructStats extends BaseShipSystemScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private final IntervalUtil tracker = new IntervalUtil(0.25f, 0.25f);
        
    public void init(MutableShipStatsAPI stats) {
    	ship = (ShipAPI) stats.getEntity();
    	engine = Global.getCombatEngine();
    }
    
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
    	return true;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        
        if (ship == null) {
        	init(stats);
        }
        
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }        
        
        if(state == State.IN){
        	ship.setCollisionClass(CollisionClass.SHIP);
        	float penality = 1-effectLevel;
        	stats.getTurnAcceleration().modifyMult(id, penality);
        	stats.getDeceleration().modifyMult(id, penality);
        	stats.getAcceleration().modifyMult(id, penality);
        	stats.getMaxTurnRate().modifyMult(id, penality);
        	stats.getMaxSpeed().modifyMult(id, penality);
        	tracker.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (tracker.intervalElapsed()) {
            	if(effectLevel < 0.4f) {
            		Global.getSoundPlayer().playSound("mawloop", 1.2f + 0.6f*effectLevel, 0.1f + 0.5f*effectLevel, ship.getLocation(), ship.getVelocity());
            	}else {
            		for(ShipAPI s : CombatUtils.getShipsWithinRange(ship.getLocation(), 300)){
            			if(s.isAlive()) {
            				ShipwideAIFlags ai = s.getAIFlags();
                			if(ai != null) {
                				ai.setFlag(AIFlags.KEEP_SHIELDS_ON, 7-7*effectLevel);
                				ai.setFlag(AIFlags.HAS_INCOMING_DAMAGE, 7-7*effectLevel);
                				if(s.getOwner() != ship.getOwner()) {
                					s.setShipTarget(ship);
                				}                				
                			}
            			}            			
            		}
            	}
            }
        	Color jitterColor = new Color(255, (int)(55 + 200*effectLevel), 50, (int)(40 + 160*effectLevel));
            ship.setJitter(this, jitterColor, 0.5f + effectLevel*0.5f, 5, 1f, 10*effectLevel);
        }else {
        	boolean blowup = false;
        	for(ShipAPI s : CombatUtils.getShipsWithinRange(ship.getLocation(), 350)){
    			if(s.isAlive() && !s.isFighter() && !s.isDrone()) {
    				if(s.getOwner() != ship.getOwner()) {
    					blowup = true;
    					break;
    				}
    			}            			
    		}
        	for(ShipAPI s : CombatUtils.getShipsWithinRange(ship.getLocation(), 300)){
    			if(s.isAlive() && !s.isFighter() && !s.isDrone()) {
    				if(s.getOwner() == ship.getOwner()) {
    					Global.getCombatEngine().addFloatingText(ship.getLocation(), "Ally Detected!", 30, Color.RED, ship, 1, 1);
    					blowup = false;
    					break;
    				}
    			}            			
    		}
        	if(blowup) {
        		DamagingExplosionSpec explosion = new DamagingExplosionSpec(1f, 400, 200, 2500, 500, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FF, 10, 50, 1, 100, new Color(255, 175, 50, 175), new Color(255, 175, 50, 255));
        		explosion.setDamageType(DamageType.HIGH_EXPLOSIVE);
        		if(ship.isDrone()) {
        			engine.spawnDamagingExplosion(explosion, ship.getDroneSource(), ship.getLocation());
        		}else {
        			engine.spawnDamagingExplosion(explosion, ship, ship.getLocation());
        		}        		
        		//ship.setHitpoints(1f);
            	//ship.applyCriticalMalfunction(ship.getAllWeapons().get(0));
        	}else {
        		ship.setCollisionClass(CollisionClass.FIGHTER);
        		ship.getFluxTracker().forceOverload(5f);
				Global.getSoundPlayer().playSound("mawend", 1.8f, 0.8f, ship.getLocation(), ship.getVelocity());
				stats.getTurnAcceleration().unmodify();
	        	stats.getDeceleration().unmodify();
	        	stats.getAcceleration().unmodify();
	        	stats.getMaxTurnRate().unmodify();
	        	stats.getMaxSpeed().unmodify();
        	}
        }        
    }
}
