package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;

import data.scripts.util.StolenUtils;

import org.lwjgl.util.vector.Vector2f;

public class DoppelswapAI implements ShipSystemAIScript {
	
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
    private ShipAPI ship;

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }        
        
        if(ship.getCurrFlux() + 100 > ship.getMaxFlux()) {
        	return;
        }
        
        if(ship.getFluxLevel() < 0.8f) {
        	flags.setFlag(AIFlags.DO_NOT_VENT);
        }else {
        	flags.unsetFlag(AIFlags.DO_NOT_VENT);
        }
        
        float incomming = StolenUtils.estimateIncomingDamage(ship,0.5f);
        
        if(incomming > 0) {
        	ShipAPI doppel = null;
            float minthreat = incomming;
            for(ShipAPI drone : ship.getDeployedDrones()){
        		if(drone.isAlive() && !drone.isPhased()) {
        			float inc = StolenUtils.estimateIncomingDamage(drone,0.5f);
        			if(minthreat > inc) {
        				minthreat = inc;
        				doppel = drone;
        			}
        		}
        	}
            if(doppel != null) {
            	ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
    }
}
