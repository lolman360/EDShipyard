package data.scripts.shipsystems.ai;

//import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.util.StolenUtils;

//import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class SpatialSwapAI implements ShipSystemAIScript {
	
	private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipSystemAPI system;
    
    private static final float MAX_RANGE = 4000;

    private final IntervalUtil tracker = new IntervalUtil(0.5f, 0.5f);
    private boolean resetAI = false;
    private final ShipAIConfig savedConfig = new ShipAIConfig();

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }

        tracker.advance(amount);

        if (tracker.intervalElapsed()) {
            if (ship.getFluxTracker().isOverloadedOrVenting() || !ship.isAlive() || system.isActive() || system.isCoolingDown() || ship.getFluxLevel() > 0.25f) {
                return;
            }
            if(!ship.isDrone()) {
            	if(flags.hasFlag(AIFlags.BACKING_OFF) || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER) || flags.hasFlag(AIFlags.NEEDS_HELP) || flags.hasFlag(AIFlags.HAS_INCOMING_DAMAGE)) {
                	return;
                }
            }else {
            	List<ShipAPI> ships = CombatUtils.getShipsWithinRange(ship.getLocation(),100);
            	for(ShipAPI s : ships) {
            		if(s.isFighter() || s.isDrone() || s == ship.getDroneSource() || s == ship.getDroneSource().getParentStation() || s.isStationModule()) {
        	        	continue;
        	        }
            		//Global.getCombatEngine().addFloatingText(ship.getLocation(), s.getHullSpec().getBaseHullId(), 100, Color.RED, ship, 1, 1);
            		return;
            	}
            }
            List<ShipAPI> ships = CombatUtils.getShipsWithinRange(ship.getLocation(),MAX_RANGE);
        	for(ShipAPI s : ships) {        		
        		if(ship.getOwner() != s.getOwner() || !s.isAlive() || s.isPhased() || s.isStation() || s.isStationModule() || s.isFighter() || s.isDrone() || ship == s) {
    	        	continue;
    	        }
        		MutableShipStatsAPI targetStats = s.getMutableStats();
        		if(targetStats != null && targetStats.getHullDamageTakenMult().getMultMods().containsKey("SpatialSwapDamageReduction")) {
        			continue;
        		}
        		if(s.getFluxTracker().isOverloadedOrVenting() || s.getHullLevel() < 1f) {
        			if(StolenUtils.estimateIncomingDamage(s, 1f) >= s.getHitpoints()/5f) {
            			switch(ship.getHullSize()) {
                			case CAPITAL_SHIP: if(!(s.getHullSize() == HullSize.CAPITAL_SHIP || s.getHullSize() == HullSize.CRUISER)){continue;}break;
                			case CRUISER: if(!(s.getHullSize() == HullSize.CAPITAL_SHIP || s.getHullSize() == HullSize.CRUISER || s.getHullSize() == HullSize.DESTROYER)){continue;};break;
                			case DESTROYER: if(!(s.getHullSize() == HullSize.CRUISER || s.getHullSize() == HullSize.DESTROYER || s.getHullSize() == HullSize.FRIGATE)){continue;};break;
                			case FRIGATE: if(!(s.getHullSize() == HullSize.DESTROYER || s.getHullSize() == HullSize.FRIGATE || s.getHullSize() == HullSize.FIGHTER)){continue;};break;
                			case FIGHTER: if(!(s.getHullSize() == HullSize.DESTROYER || s.getHullSize() == HullSize.FRIGATE)){continue;};break;
                			default: continue;
            			}        			
            			ship.setShipTarget(s);
                        if(s.getShipAI() != null) {//AI controlled
                        	ShipwideAIFlags tFlags = s.getAIFlags();
                        	if(tFlags != null) {
                        		flags.setFlag(AIFlags.KEEP_SHIELDS_ON, 12f);
                        	}
                        	ship.useSystem();
                        	break;
                        }else {//Player Controlled
                        	if(s.getShipTarget() == ship) {
                        		ship.useSystem();
                        		break;
                        	}
                        }                 
            		}
        		}        		
        	}
        }

        if (system.isActive()) {
            flags.setFlag(AIFlags.KEEP_SHIELDS_ON, 1.5f);
            flags.unsetFlag(AIFlags.BACK_OFF);
            flags.unsetFlag(AIFlags.DO_NOT_PURSUE);
            flags.unsetFlag(AIFlags.DO_NOT_USE_FLUX);
            flags.unsetFlag(AIFlags.DO_NOT_AUTOFIRE_NON_ESSENTIAL_GROUPS);

            if (!resetAI) {
                resetAI = true;
                saveAIConfig(ship);
                ship.getShipAI().getConfig().alwaysStrafeOffensively = true;
                ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = false;
                ship.getShipAI().getConfig().personalityOverride = Personalities.RECKLESS;
                ship.getShipAI().forceCircumstanceEvaluation();
            }
            
        } else {
            if (resetAI) {
                resetAI = false;
                //flags.unsetFlag(AIFlags.DO_NOT_BACK_OFF);
                restoreAIConfig(ship);
                ship.getShipAI().forceCircumstanceEvaluation();
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

    private void saveAIConfig(ShipAPI ship) {
        if (ship.getShipAI().getConfig() != null) {
            savedConfig.alwaysStrafeOffensively = ship.getShipAI().getConfig().alwaysStrafeOffensively;
            savedConfig.turnToFaceWithUndamagedArmor = ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor;
            savedConfig.personalityOverride = ship.getShipAI().getConfig().personalityOverride;
        }
    }

    private void restoreAIConfig(ShipAPI ship) {
        if (ship.getShipAI().getConfig() != null) {
            ship.getShipAI().getConfig().alwaysStrafeOffensively = savedConfig.alwaysStrafeOffensively;
            ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = savedConfig.turnToFaceWithUndamagedArmor;
            ship.getShipAI().getConfig().personalityOverride = savedConfig.personalityOverride;
        }
    }
}
