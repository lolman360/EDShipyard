package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class ModuleShieldAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    //private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipAPI parent;

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
            if (parent == null) {
            	parent = ship.getParentStation();
            }else {
            	if (parent.isPhased() || (parent.getShield() != null && parent.getShield().isOn())) {
                	if (parent.getFluxLevel() <= 0.5f && ship.getFluxLevel() >= 0.3f) {
                		ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                    }
                	if(system.isActive()) {
                		ship.getSystem().deactivate();
                	}
                    return;
                }else {
                	if(!system.isActive() && ship.getShield().isOn()) {
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
