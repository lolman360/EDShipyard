package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.List;
import java.util.Random;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class MisdirectionAI implements ShipSystemAIScript {
	
	//code based on Shock Buster system by Dark Revenant (Interstellar Imperium)

    private CombatEngineAPI engine;
    
    private ShipAPI ship;
    private Random rng;
    private final IntervalUtil tracker = new IntervalUtil(0.75f, 1f);

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }

        tracker.advance(amount);
        Vector2f shipLoc = ship.getLocation();
        
        if (tracker.intervalElapsed()) {
            if (!AIUtils.canUseSystemThisFrame(ship)) {            	
                return;
            }

            float maxRange = ship.getCollisionRadius()*4;
            
            float largestmt = 0;
            List<FighterWingAPI> fighters = ship.getAllWings();
            if(fighters != null && !fighters.isEmpty()) {
            	for(FighterWingAPI wing : fighters){
            		int num = wing.getWingMembers().size();
            		if(num <= 0) {
            			continue;
            		}
            		ShipAPI fighter = wing.getWingMembers().get(rng.nextInt(num));
            		float fightermt = 0;
                	if(fighter != null && fighter.isAlive()) {
                		List<MissileAPI> allMissiles = CombatUtils.getMissilesWithinRange(fighter.getLocation(), 250);
                		for (MissileAPI missile : allMissiles) {
                            if (missile.getOwner() != ship.getOwner()) {
                                float scale = 1f;
                                switch (missile.getDamageType()) {
                                    case FRAGMENTATION:
                                        scale = 0.25f;
                                        break;
                                    case KINETIC:
                                        scale = 0.5f;
                                        break;
                                    default:
                                    case ENERGY:
                                        break;
                                }
                                fightermt += missile.getDamageAmount() * scale;
                            }
                        }
                		if(fightermt > largestmt) {
                			largestmt = fightermt;
                		}
                	}                	
            	}
            }

            float missileThreatLevel = largestmt*0.6f;
            List<MissileAPI> allMissiles = CombatUtils.getMissilesWithinRange(shipLoc, maxRange);
            for (MissileAPI missile : allMissiles) {
                if (missile.getOwner() != ship.getOwner()) {
                    float scale = 1f;
                    switch (missile.getDamageType()) {
                        case FRAGMENTATION:
                            scale = 0.25f;
                            break;
                        case KINETIC:
                            scale = 0.5f;
                            break;
                        default:
                        case ENERGY:
                            break;
                    }
                    missileThreatLevel += missile.getDamageAmount() * scale;
                }
            }
            if (missileThreatLevel*(1 + ship.getFluxLevel()*2f) >= ship.getHitpoints()/2f) {                
                ship.useSystem();
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    	rng = new Random();
    }
}
