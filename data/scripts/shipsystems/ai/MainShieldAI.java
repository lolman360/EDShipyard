package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.List;

import org.lwjgl.util.vector.Vector2f;

public class MainShieldAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    //private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipAPI module;

    private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);

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
            if (ship.getFluxTracker().isOverloadedOrVenting()) {
                return;
            }            
            if (module == null) {
            	List<ShipAPI> modules = ship.getChildModulesCopy();
        		if(modules != null && !modules.isEmpty()) {
        			module = modules.get(0);
        		}
            }else {
            	if (module.getShield() != null && module.getShield().isOn()) {
                	if ((module.getFluxLevel() <= 0.5f && ship.getFluxLevel() >= 0.5f) || ship.getHardFluxLevel() >= 0.8f) {
                		ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                    }
                	if(system.isActive()) {
                		ship.getSystem().deactivate();
                	}
                    return;
                }else {
                	if(!system.isActive() && ship.getShield().isOn() && module.getFluxTracker().isOverloadedOrVenting()) {
                		ship.useSystem();
                	}                	
                }
            }                        
        }        
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        //this.flags = flags;
        this.system = system;
        this.engine = engine;
    }
}
