package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;
import java.util.List;

import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class WurgSensorAI implements ShipSystemAIScript {
	
    private static final float TARGET_DESIRE = 1f;

    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private WeaponAPI maw;

    private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);//new IntervalUtil(0.2f, 0.3f);
    
    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }
        
        if(maw == null) {
        	for(WeaponAPI w : ship.getAllWeapons()) {
            	if(w.getDisplayName().contains("Maw")) {
            		maw = w;
            		break;
            	}
            }
        	return;
        }

        tracker.advance(amount);

        if (tracker.intervalElapsed()) {
        	
        	if(maw != null && !maw.isDisabled() && maw.getCooldownRemaining() == 0 && this.ship.getFluxLevel() < 0.2f) {
        		ShipAPI ship = WeaponUtils.getNearestEnemyInArc(maw);
            	if(ship != null) {
            		if(ship.getHullSize() == HullSize.CAPITAL_SHIP || ship.isStation()) {
            			boolean safe = true;
            			List<ShipAPI> ships = CombatUtils.getShipsWithinRange(ship.getLocation(),800f);
                        for(ShipAPI s : ships) {
                        	if(s.getOwner() != ship.getOwner() && s.isAlive() && !s.isFighter()) {
                        		safe = false;
                            }
                        }
                        if(safe) {
                        	List<ShipAPI> allies = WeaponUtils.getAlliesInArc(maw);
                        	for(ShipAPI ally : allies){
                        		if(ally.isAlive() && !(ally.getHullSize() == HullSize.FIGHTER) && !ally.isStationModule() && !ally.isDrone()) {
                        			safe = false;
                        			break;
                        		}
                        	}
                        	if(safe) {
                        		this.ship.giveCommand(ShipCommand.FIRE, this.ship.getLocation(), 0);
                        	}                        	
                        }                        
            		}            		
            	}
        	}
        	
        	
            if (ship.getFluxTracker().isOverloadedOrVenting() || system.isCoolingDown()) {
                return;
            }
            
            float desire = 0f;
            
            if(system.isActive()) {
            	desire = ship.getFluxLevel()*1.25f;
            	
            	List<ShipAPI> ships = CombatUtils.getShipsWithinRange(ship.getLocation(),600f);
                
                for(ShipAPI s : ships) {
                	if(s.getOwner() != ship.getOwner() && s.isAlive()) {
                		desire += 0.20f;
                    }
                }
                
                if(maw != null && maw.isFiring() && maw.getChargeLevel() < 0.75){
                	desire += 0.75f;
                }
            	
            	if (desire >= TARGET_DESIRE) {
            		//engine.addFloatingText(this.ship.getLocation(), "DISABLE", 100, Color.red, this.ship, 0.5f, 2f);
            		ship.useSystem();
                }
            }else {
            	desire = 1.0f - ship.getFluxLevel()*1.5f;

            	if(maw != null && maw.isFiring() && maw.getChargeLevel() >= 0.75){
                	desire += 0.50f;
                }
            	
            	if (flags.hasFlag(AIFlags.MAINTAINING_STRIKE_RANGE)) {
            		desire += 0.35f;
                }
            	
            	if (flags.hasFlag(AIFlags.SAFE_FROM_DANGER_TIME)) {
            		desire += 0.15f;
                }
            	
            	if (flags.hasFlag(AIFlags.PURSUING)) {
            		desire += 0.35f;
                }
            	
            	if (flags.hasFlag(AIFlags.NEEDS_HELP)) {
            		desire -= 0.25f;
                }

                if (flags.hasFlag(AIFlags.DO_NOT_USE_FLUX)) {
                    desire -= 0.25f;
                }
                
                if (flags.hasFlag(AIFlags.BACK_OFF)) {
                    desire -= 0.25f;
                }
                //engine.addFloatingText(this.ship.getLocation(), "desire: "+desire, 100, Color.red, this.ship, 0.5f, 0.3f);
                if (desire >= TARGET_DESIRE) {                	
                    ship.useSystem();
                }
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
        this.engine = engine;        
    }
}
