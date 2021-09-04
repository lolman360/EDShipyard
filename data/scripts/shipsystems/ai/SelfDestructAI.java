package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class SelfDestructAI implements ShipSystemAIScript {
	
	//code based on Shock Buster system by Dark Revenant (Interstellar Imperium)

    private CombatEngineAPI engine;
    private final float TARGET_RANGE = 400f;
    private ShipAPI ship;
    private boolean used;
    private final IntervalUtil tracker = new IntervalUtil(0.25f, 0.25f);

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused() || used) {
            return;
        }

        tracker.advance(amount);
        if (tracker.intervalElapsed()) {
            
            boolean use = false;

            for(ShipAPI s : CombatUtils.getShipsWithinRange(ship.getLocation(), TARGET_RANGE)){
            	if(s.getOwner() == ship.getOwner()) {
            		if(!s.isFighter()) {
            			return;//ally nearby
            		}
            		continue;
            	}else {
            		//Global.getCombatEngine().addFloatingText(ship.getLocation(), "BOOM", 100, Color.RED, ship, 1, 1);
            		if(s.isAlive() && !s.isFighter() && !s.isDrone()) {
            			use = true;
            			break;
            		}
            	}
            }
            
            if(use) {            	
            	ship.useSystem();
            	used = true;
            }            
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        used = false;
    }
}
