package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;

public class FieldShieldControlStats extends BaseShipSystemScript {
	
	public final Color JITTER_COLOR = new Color(125, 50, 150, 150);

	private ShipAPI ship;
	private ShipAPI module;
	private boolean activated;
	private float accumulation;
	private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.1f);
	public final String shieldModule = "edshipyard_retriever_shield";

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {    	
    	if(ship == null || !ship.isAlive() ||  module == null || !module.isAlive() || module.getFluxTracker().isOverloadedOrVenting()) {
        	return false;
        }    	
    	return true;
    }
    
    public void init(MutableShipStatsAPI stats) {
    	ship = (ShipAPI) stats.getEntity();
    	if(ship != null && ship.isAlive() && ship.getChildModulesCopy() != null) {
    		for (ShipAPI m : ship.getChildModulesCopy()) {
            	if(m.getHullSpec().getBaseHullId().equals(shieldModule)) {            
                	module = m;
                    break;   
            	}
            }
            activated = false;
            accumulation = 0;
    	}
    }
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {    	
        if (ship == null) {
        	init(stats);        	
            return;
        }
        if(!ship.isAlive()) {
        	return;
        }
        if(module == null || !module.isAlive() || module.getShield() == null) {
        	return;
        }
        
        if(module.getFluxTracker().isOverloadedOrVenting()) {
        	if(module.getCollisionClass() == CollisionClass.FIGHTER) {
        		module.setCollisionClass(CollisionClass.SHIP);
            	Global.getSoundPlayer().playSound("kfp", 0.5f ,0.75f , ship.getLocation(), ship.getVelocity()); 
        	}        	
        	return;
        }
        if(ship.getFluxTracker().isOverloadedOrVenting()) {
        	if(module.getShield().isOn()) {
            	module.getShield().toggleOff();            	
            	Global.getSoundPlayer().playSound("kfp", 0.5f ,0.75f , ship.getLocation(), ship.getVelocity());            	
            }
        	module.setCollisionClass(CollisionClass.SHIP);
        }
        
        if(module.getShield().isOn()) {
        	
        	float hardflux = module.getFluxTracker().getHardFlux();
        	
        	if (!Global.getCombatEngine().isPaused()) {                
            	tracker.advance(Global.getCombatEngine().getElapsedInLastFrame());
    			if(tracker.intervalElapsed()) {
    				hardflux += 40;
    			}
            }        	
        	
        	accumulation += hardflux;
        	float multiplier = (accumulation + 20000f)/20000f;
        	//Global.getCombatEngine().addFloatingText(ship.getLocation(),  String.format("%.2f", multiplier), 100, Color.RED, ship, 1, 1);
        	ship.getFluxTracker().increaseFlux(hardflux*multiplier, true);
            module.getFluxTracker().setHardFlux(0);
            module.getShield().setInnerColor(new Color((int)Math.min(254, 40*multiplier +20),50,150,175));
        }else {
        	accumulation = 0f;
        }
        if(state == State.ACTIVE) {
        	if(!activated) {
        		activated = true;
            	if(module.getShield().isOn()) {
                	module.getShield().toggleOff();
                	module.setCollisionClass(CollisionClass.SHIP);
                	Global.getSoundPlayer().playSound("kfp", 0.5f ,0.75f , ship.getLocation(), ship.getVelocity());
                }else {
                	module.getShield().toggleOn();
                	module.setCollisionClass(CollisionClass.FIGHTER);
                	Global.getSoundPlayer().playSound("kfp", 1.5f ,0.75f , ship.getLocation(), ship.getVelocity());                	
                }
        	}        	
        }else {
        	activated = false;
        }        
    }
}
