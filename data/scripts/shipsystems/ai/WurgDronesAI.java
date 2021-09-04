package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.List;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class WurgDronesAI implements ShipSystemAIScript {
	
    private CombatEngineAPI engine;
    private ShipSystemAPI system;
    private ShipAPI ship;
    private WeaponAPI maw;

    private final IntervalUtil tracker = new IntervalUtil(2.5f, 3f);
    
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
            	if (w.getDisplayName().contains("Maw")) {
                    maw = w;
                    break;
                }
            }
    		return;
    	}

        tracker.advance(amount);

        if (tracker.intervalElapsed()) {
        	
            int droneCount = 0;
        	List<ShipAPI> list = CombatUtils.getShipsWithinRange(ship.getLocation(),2000f);
        	        	
        	if(list != null) {
        		for(ShipAPI s : list) {
        			if(s.getHullSpec().getHullId().equals("edshipyard_wurgdrone") && (s.getOwner() == ship.getOwner()) && s.isAlive()) {
        				droneCount++;
        			}
        		}
        	}
            
            if(maw != null && maw.isFiring()) {
            	if(droneCount > 0) {
            		ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);                	
                }
            	return;
            }
            
            if(droneCount == 0 && system.getAmmo() > 0) {
            	ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.engine = engine;        
    }
}
