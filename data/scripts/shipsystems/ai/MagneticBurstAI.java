package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.MagneticBurstStats;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class MagneticBurstAI implements ShipSystemAIScript {
	
	//code based on Shock Buster system by Dark Revenant (Interstellar Imperium)

    private CombatEngineAPI engine;
    
    public final float BASE_RANGE = 700f;
    public final float BLAST_AREA_RADIUS_SCALE = 3f;
    public final float BLAST_AREA_FLAT = 300f;

    private ShipwideAIFlags flags;
    private ShipAPI ship;
    private final IntervalUtil tracker = new IntervalUtil(0.3f, 0.5f);

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

            float shipRadius = MagneticBurstStats.effectiveRadius(ship);

            float maxRange = BASE_RANGE + 200f*getSize(ship);
            float falloffRange = maxRange*0.9f;
            float decisionTarget = 200f;
            float blastArea = shipRadius * BLAST_AREA_RADIUS_SCALE + BLAST_AREA_FLAT;            

            ShipAPI bestTarget = null;
            float bestTargetWeight = 0f;
            float totalTargetWeight = 0f;

            List<ShipAPI> nearbyTargets = MagneticBurstStats.getShipsWithinRange(shipLoc, maxRange);
            for (ShipAPI t : nearbyTargets) {
                if ((t.getOwner() == ship.getOwner()) || !t.isAlive() || t.isShuttlePod() || t.isDrone()) {
                    continue;
                }

                float weight = (5 - getSize(t))*5f + (t.getFluxLevel()*2f)*5f * (1 - t.getHullLevel())*5f;

                float shipStrength = 5f;
                
                weight *= (1f - MathUtils.getDistance(ship, t) / falloffRange)*7f;
                
                if (t.isPhased()) {                   	
                   	weight *= 4;
                }

                FleetMemberAPI member = CombatUtils.getFleetMember(t);
                if (member != null) {
                    shipStrength += 0.1f + member.getFleetPointCost();
                }

                weight *= (float) Math.sqrt(shipStrength);

                if (weight >= bestTargetWeight) {
                    bestTarget = t;
                    bestTargetWeight = weight;                    
                }

                totalTargetWeight += weight;
            }

            float missileThreatLevel = 0f;
            List<MissileAPI> allMissiles = CombatUtils.getMissilesWithinRange(ship.getLocation(), blastArea * 0.9f);
            for (MissileAPI missile : allMissiles) {
                if (missile.getOwner() != ship.getOwner()) {
                    float scale = 3f;
                    switch (missile.getDamageType()) {
                        case FRAGMENTATION:
                            scale = 0.5f;
                            break;
                        case KINETIC:
                            scale = 0.5f;
                            break;
                        case HIGH_EXPLOSIVE:
                            scale = 2.5f;
                            break;
                        default:
                        case ENERGY:
                            break;
                    }
                    missileThreatLevel += missile.getDamageAmount() * scale;
                }
            }
            boolean missileThreat = missileThreatLevel >= ship.getHitpoints() * 0.25f;
            
            if((bestTarget == null)) {
            	if (!missileThreat) {
                    return;
                }
            }else {
            	ship.setShipTarget(bestTarget);
            }

            float decisionLevel = bestTargetWeight + totalTargetWeight;
            
            decisionLevel += missileThreatLevel/ship.getHitpoints()*200f;
            if (flags.hasFlag(AIFlags.BACK_OFF) || flags.hasFlag(AIFlags.BACKING_OFF)) {
                decisionLevel *= 1.25f;
            }
            if(ship.isPhased() && ship.getFluxTracker().getFluxLevel() >= 0.9f) {
            	decisionLevel += 150f;
            }
            if (decisionLevel >= decisionTarget) {                
                ship.useSystem();
            }
        }
    }
    
    private int getSize(ShipAPI test) {//add to utils later
    	switch(test.getHullSize()) {
    		case CAPITAL_SHIP: return 4;
    		case CRUISER: return 3;
    		case DESTROYER: return 2;
    		default: return 1;
    	}
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
    }
}
