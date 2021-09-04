package data.scripts.shipsystems;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class MagneticBurstStats extends BaseShipSystemScript {
	
	//code based on Shock Buster system by Dark Revenant (Interstellar Imperium)

	public ShipAPI target;
	public ShipAPI lockedTarget;
   
    public final float BASE_RANGE = 700f;
    public final float RADIUS_PER_BOLT = 25f;
    public final float DAMAGE_PER_BOLT = 50f;
    public final float EMP_PER_BOLT = 200f; 
    public final float BLAST_AREA_RADIUS_SCALE = 3f;
    public final float BLAST_AREA_FLAT = 300f;
    public final float BLAST_MISSILE_DAMAGE = 200f;
    public final float BLAST_MAX_DAMAGE = BLAST_MISSILE_DAMAGE * 5f;
    public final float BLAST_MISSILE_EMP = 200f;
    public final int BLAST_MAX_MISSILE_TARGETS = 15;    

    private final Color EMP_CORE_COLOR_STANDARD = new Color(255, 175, 225, 255);
    private final Color EMP_FRINGE_COLOR_STANDARD = new Color(255, 175, 225, 200);
    private final Color JITTER_UNDER_COLOR_STANDARD = new Color(255, 175, 225, 100);
    private final Color JITTER_OVER_COLOR_STANDARD = new Color(255, 175, 225, 225);
	
    private final IntervalUtil interval = new IntervalUtil(0.25f, 0.33f);
    private boolean fired = false;
    
    
    private int isTargetValid(ShipAPI ship, ShipAPI target) {
        if (target == null || ship == null || (target.getOwner() == 100)) {
            return 2;
        }
    	float range = BASE_RANGE + 200f*getSize(ship);
        if(MathUtils.getDistance(ship, target) > range) {
        	return 1;
        }
        if((target.getOwner() == ship.getOwner())) {
        	return 3;
        }
        if(!target.isAlive()) {
        	return 4;
        }
        if(target.isStation()) {
        	return 5;
        }
        if(target.isStationModule()) {
        	return 6;
        }
        if(target.isFighter() || target.isDrone()) {
        	return 7;
        }
        return 0;
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
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        
    	switch(system.getState()) {
        	case COOLDOWN: return "COOLING DOWN";
        	case IN: return "CHARGING UP";
        	case OUT: return "COOLING DOWN";
        	case ACTIVE: return "ACTIVE";
        	case IDLE: break;
        	default: return null;
        }
    	if(ship != null && ship.isAlive()) {
        	target = ship.getShipTarget();    		
    	}
        if(target == null) {
        	 return "READY [NO TARGET]";
        }else {
        	switch(isTargetValid(ship,target)) {
        		case 1: return "OUT OF RANGE";
        		case 2: return "INVALID TARGET";
        		case 3: return "TARGET IS AN ALLY";
        		case 4: return "TARGET IS DEAD";
        		case 5: return "STATIONS ARE INVALID";
        		case 6: return "MODULES ARE INVALID";
        		case 7: return "FIGHTERS ARE INVALID";
        		default: return "READY [TARGET LOCKED]";
        	}
        }
    }
    

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        Color JITTER_UNDER_COLOR = JITTER_UNDER_COLOR_STANDARD;
        float jitterScale = 1f;
        float pitchScale = 0.5f;
        float soundVol = 0.35f;

        float jitterLevel = effectLevel;
        if (state == State.OUT) {
            jitterLevel *= jitterLevel;
        }
        float maxRangeBonus = 30f * jitterScale;
        float jitterRangeBonus = ((0.5f + jitterLevel) / 1.5f) * maxRangeBonus;

        Color jitterUnderColor = new Color(JITTER_UNDER_COLOR.getRed(), JITTER_UNDER_COLOR.getGreen(), JITTER_UNDER_COLOR.getBlue(),
                clamp255(Math.round(jitterLevel * JITTER_UNDER_COLOR.getAlpha())));
        ship.setJitterUnder(this, jitterUnderColor, effectLevel, Math.round(20 * jitterScale), 5f, 5f + jitterRangeBonus);
        ship.setJitter(this, JITTER_OVER_COLOR_STANDARD, 0.4f + effectLevel/2f, 1, 0f, 1f + jitterRangeBonus);
        
        stats.getArmorDamageTakenMult().modifyMult(id, 1 - effectLevel, "MAGNETIC FIELD SHIELD");
        stats.getHullDamageTakenMult().modifyMult(id, 0.5f - (effectLevel*0.5f));
        
        if (state == State.IN) {
            interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (interval.intervalElapsed()) {
                Global.getSoundPlayer().playSound("ed_mag1", MathUtils.getRandomNumberInRange(0.95f, 1.05f) * pitchScale,soundVol, ship.getLocation(), ship.getVelocity());
                if(Math.random() < effectLevel) {
                	if(lockedTarget != null && lockedTarget.isAlive()) {
                    	applyEffectToTarget(ship, lockedTarget, 1, 2, 250, true);
                    }else {
                    	List<ShipAPI> targets = findTargets(ship);
                    	int lightning = (int)(effectLevel)*3 + 1;
                    	for(ShipAPI t : targets) {
                    		applyEffectToTarget(ship, t, 1, 2, -50, false);
                    		lightning--;
                    		if(lightning <= 0) {
                    			break;
                    		}
                    	}
                    	zapNearbyMissiles(ship);
                    }
                }
            }
            fired = false;
            if(lockedTarget != null && lockedTarget.isAlive()) {
		        lockedTarget.setJitterUnder(this, jitterUnderColor, (effectLevel/2f) +0.5f, Math.round(20 * jitterScale), 5f, 5f + jitterRangeBonus);
			}else {
				target = ship.getShipTarget();
				if(isTargetValid(ship, target) == 0) {
					lockedTarget = target;
				}
			}
        } else if ((state == State.ACTIVE) && !fired) {        	
        	if(lockedTarget != null && lockedTarget.isAlive()) {
        		if(lockedTarget.isPhased()) {
        			if(lockedTarget.getPhaseCloak() != null) {
        				lockedTarget.getPhaseCloak().deactivate();
        			}        			
                }
        		int size = getSize(lockedTarget)+getSize(ship);
        		applyEffectToTarget(ship, lockedTarget, size*3 +3, size*3 +5, 1500, true);
        		lockedTarget = null;
        	}else {
        		List<ShipAPI> targets = findTargets(ship);
            	if(!targets.isEmpty()) {
            		for (ShipAPI t : targets) {
    					applyEffectToTarget(ship, t, 5, 8, -200, false);
    				}
            	}
        	}
        	
            zapNearbyMissiles(ship);
            fired = true;
            if(ship.isPhased()) {
            	ship.getPhaseCloak().deactivate();
            }            
        }
        
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        switch (state) {
            case IN: 
            case ACTIVE:
            case OUT: {
                if (index == 0) {
                    return new StatusData("Armor strengthened", false);
                }
                break;
            }
            default:
                break;
        }
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship == null) {
            return false;
        }
        
        target = ship.getShipTarget();
		if(isTargetValid(ship, target) == 0) {
			return true;
		}

        boolean hasTarget;
        List<ShipAPI> targets = findTargets(ship);
        hasTarget = (targets != null) && !targets.isEmpty();

        if (!hasTarget) {
            float shipRadius = effectiveRadius(ship);
            float blastArea = shipRadius * BLAST_AREA_RADIUS_SCALE + BLAST_AREA_FLAT;

            List<MissileAPI> allMissiles = CombatUtils.getMissilesWithinRange(ship.getLocation(), blastArea);
            for (MissileAPI missile : allMissiles) {
                if (missile.getOwner() != ship.getOwner()) {
                    hasTarget = true;
                    break;
                }
            }
        }

        return hasTarget;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    	 stats.getArmorDamageTakenMult().unmodify(id);
    	 stats.getHullDamageTakenMult().unmodify(id);
    }
    
    public static float effectiveRadius(ShipAPI ship) {
        if (ship.getSpriteAPI() == null || ship.isPiece()) {
            return ship.getCollisionRadius();
        } else {
            float fudgeFactor = 1.5f;
            return ((ship.getSpriteAPI().getWidth() / 2f) + (ship.getSpriteAPI().getHeight() / 2f)) * 0.5f * fudgeFactor;
        }
    }

    protected void zapNearbyMissiles(final ShipAPI ship) {
        float shipRadius = effectiveRadius(ship);

        Color EMP_CORE_COLOR = EMP_CORE_COLOR_STANDARD;
        Color EMP_FRINGE_COLOR = EMP_FRINGE_COLOR_STANDARD;
        float blastDamage = BLAST_MISSILE_DAMAGE;
        float blastEMP = BLAST_MISSILE_EMP;
        float blastArea = shipRadius * BLAST_AREA_RADIUS_SCALE + BLAST_AREA_FLAT;
        float blastMaxDamage = BLAST_MAX_DAMAGE;
        int blastMaxTargets = BLAST_MAX_MISSILE_TARGETS;        

        float totalDamage = 0f;
        int missileTargets = 0;
        List<MissileAPI> allMissiles = CombatUtils.getMissilesWithinRange(ship.getLocation(), blastArea);
        Collections.shuffle(allMissiles);
        for (MissileAPI missile : allMissiles) {
            if (missile.getOwner() != ship.getOwner()) {
                float contribution = 0.1f;
                float falloff = 1f - MathUtils.getDistance(ship, missile) / blastArea;
                if (missile.getCollisionClass() == CollisionClass.NONE) {
                    continue;
                }

                missileTargets++;
                totalDamage += Math.min(missile.getHitpoints(), blastDamage * falloff) * contribution;
                if (missileTargets >= blastMaxTargets) {
                    break;
                }
                if (totalDamage >= blastMaxDamage) {
                    break;
                }
            }
        }

        float attenuation = 1f;
        if (totalDamage > blastMaxDamage) {
            attenuation *= blastMaxDamage / totalDamage;
        }

        totalDamage = 0f;
        missileTargets = 0;
        for (MissileAPI missile : allMissiles) {
            if (missile.getOwner() != ship.getOwner()) {
                float falloff = 1f - MathUtils.getDistance(ship, missile) / blastArea;
                if (missile.getCollisionClass() == CollisionClass.NONE) {
                    continue;
                }

                missileTargets++;
                MissileAPI empTarget = missile;
                Vector2f point = null;
                int max = 10;
                while (point == null && max > 0) {
                    point = MathUtils.getRandomPointInCircle(ship.getLocation(), shipRadius);
                    if (!CollisionUtils.isPointWithinBounds(point, ship)) {
                        point = null;
                    }
                    max--;
                }
                if (point == null) {
                    point = MathUtils.getRandomPointInCircle(ship.getLocation(), shipRadius);
                }
                Global.getCombatEngine().spawnEmpArc(ship, point, ship, empTarget, DamageType.ENERGY,
                        blastDamage * falloff * attenuation, blastEMP * falloff * attenuation,
                        10000f, null, (float) Math.sqrt(blastDamage * falloff * attenuation), EMP_FRINGE_COLOR, EMP_CORE_COLOR);
                if (missileTargets >= blastMaxTargets) {
                    break;
                }
                if (totalDamage >= blastMaxDamage) {
                    break;
                }
            }
        }
    }

    protected void applyEffectToTarget(final ShipAPI ship, final ShipAPI target, int minBolts, int maxBolts, float forcePerBolt, boolean applyToSource) {
        if (target == ship){
            return;
        }

        float shipRadius = effectiveRadius(ship);
        float targetRadius = effectiveRadius(target);

    	float range = BASE_RANGE + 200f*getSize(ship);
        Color EMP_CORE_COLOR = EMP_CORE_COLOR_STANDARD;
        Color EMP_FRINGE_COLOR = EMP_FRINGE_COLOR_STANDARD;
        float falloffRange = range*0.9f;
        float radiusPerBolt = RADIUS_PER_BOLT;
        float damagePerBolt = DAMAGE_PER_BOLT;
        float empPerBolt = EMP_PER_BOLT;
        int retries = 10;
        float empScale = 1f;
        int particlesPerBolt = 10;
        float particleScale = 1f;
        float volumeScale = 1f;        

        float falloff = 1f - MathUtils.getDistance(ship, target.getLocation()) / falloffRange;
        int bolts = Math.max(minBolts, Math.min(maxBolts, Math.round(targetRadius / radiusPerBolt)));        

        ShipAPI empTarget = target;
        for (int i = 0; i <= bolts; i++) {
            Vector2f point = null;
            int max = retries;
            while (point == null && max > 0) {
                point = MathUtils.getRandomPointInCircle(ship.getLocation(), shipRadius);
                if (!CollisionUtils.isPointWithinBounds(point, ship)) {
                    point = null;
                }
                max--;
            }
            if (point == null) {
                point = MathUtils.getRandomPointInCircle(ship.getLocation(), shipRadius);
            }
            Global.getCombatEngine().spawnEmpArc(ship, point, ship, empTarget, DamageType.ENERGY,
                    damagePerBolt * falloff, empPerBolt * falloff, 10000f, null, 20f * empScale * falloff, EMP_FRINGE_COLOR, EMP_CORE_COLOR);
            for (int x = 0; x < particlesPerBolt; x++) {
                Global.getCombatEngine().addHitParticle(
                        MathUtils.getPointOnCircumference(target.getLocation(),
                                MathUtils.getRandomNumberInRange(0f, targetRadius * 0.5f) * particleScale,
                                MathUtils.getRandomNumberInRange(0f, 360f)),
                        MathUtils.getPointOnCircumference(null,
                                MathUtils.getRandomNumberInRange(100f, 400f) * particleScale,
                                MathUtils.getRandomNumberInRange(0f, 360f)),
                        7f * particleScale, 1f * falloff, MathUtils.getRandomNumberInRange(1f, 2f) * particleScale * falloff, EMP_CORE_COLOR);
            }
        }

        float volume = 1f * ((float) bolts / (float) maxBolts) * volumeScale * falloff;
        Global.getSoundPlayer().playSound("ed_shock", 1f, volume, target.getLocation(), target.getVelocity());
        
        CombatUtils.applyForce(getRoot(target), VectorUtils.getDirectionalVector(target.getLocation(), ship.getLocation()), forcePerBolt * bolts * falloff);
        if(applyToSource) {
        	CombatUtils.applyForce(getRoot(ship), VectorUtils.getDirectionalVector(ship.getLocation(), target.getLocation()), forcePerBolt * bolts * falloff);
        }
    }

    public static ShipAPI getRoot(ShipAPI ship) {
        if (isMultiShip(ship)) {
            ShipAPI root = ship;
            while (root.getParentStation() != null) {
                root = root.getParentStation();
            }
            return root;
        } else {
            return ship;
        }
    }

    public static boolean isMultiShip(ShipAPI ship) {
        return ship.getParentStation() != null || ship.isShipWithModules();
    }
    
    public static int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
    }
    
    public static List<ShipAPI> getShipsWithinRange(Vector2f location, float range) {
        List<ShipAPI> ships = new ArrayList<>();

        for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
            if (tmp.isShuttlePod()) {
                continue;
            }
            if (MathUtils.isWithinRange(tmp, location, range)) {
                ships.add(tmp);
            }
        }

        return ships;
    }

    protected List<ShipAPI> findTargets(ShipAPI ship) {
    	float range = BASE_RANGE + 200f*getSize(ship);
        List<ShipAPI> targets = new ArrayList<ShipAPI>();

        for (ShipAPI target : getShipsWithinRange(ship.getLocation(), range + ship.getCollisionRadius())) {
            if (target.isShuttlePod() || !target.isAlive() || target.isStation() || target.isStationModule()) {
                continue;
            }
            if ((ship.getOwner() != target.getOwner()) && (target.getOwner() != 100)) {
                targets.add(target);
            }
        }

        return targets;
    }
}
