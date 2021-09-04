package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class FieldShieldControlAI implements ShipSystemAIScript {
	
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipAPI module;
    private final IntervalUtil tracker = new IntervalUtil(1f, 1f);
    
    public final String shieldModule = "edshipyard_retriever_shield";

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }
        
        if(module == null) {
        	for (ShipAPI m : ship.getChildModulesCopy()) {
            	if(m.getHullSpec().getBaseHullId().equals(shieldModule)) {            
                	module = m;
                    break;   
            	}
            }
        	return;
        }
        
        if(!module.isAlive() || module.getFluxTracker().isOverloadedOrVenting()) {
        	return;
        }
        
        tracker.advance(amount);
        if (tracker.intervalElapsed()) {        	
        	if((module.getShield().isOn() && (ship.getFluxLevel() >= 0.75f || module.getFluxLevel() >= 0.9f)) || (module.getShield().isOff() && (ship.getFluxLevel() < 0.75f && module.getFluxLevel() < 0.9f))) {
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
