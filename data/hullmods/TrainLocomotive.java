package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import data.scripts.util.TrainWagon;
import java.util.List;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;

public class TrainLocomotive extends BaseHullMod {

	//Based on KT_SinuousBody by Sinosauropteryx (Kingdom of Terra mod)
	
	public final float ZEROFLUXSPEED_MOD = 0.30F;
	public final float PROFILE_MOD = 150f;
    public final int NUMBER_OF_SEGMENTS = 5;
    public final float RANGE = 90f; // Flexibility constant. Range of movement of each segment.
    public final float REALIGNMENT_CONSTANT = 3f; // Elasticity constant. How quickly the body unfurls after being curled up.

    private TrainWagon[] seg = new TrainWagon[NUMBER_OF_SEGMENTS];
    private String[] args = new String[NUMBER_OF_SEGMENTS];


    public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    	stats.getZeroFluxSpeedBoost().modifyMult(id, ZEROFLUXSPEED_MOD);
    	stats.getSensorProfile().modifyFlat(id, PROFILE_MOD);
    	
    	ShipVariantAPI ship = stats.getVariant();
    	if(ship != null && stats != null) {
    		float fuel = 0;
    		float crew = 0;
    		float cargo = 0;
    		float fuelCost = 0;
    		float crewLimit = 0;
    		float supplyCost = 0;
    		for(int i = 1; i < 6; i++) {
    			//ShipVariantAPI module = ship.getModuleVariant("SEGMENT"+i);
    			//todo check the type of wagon
    			cargo += 1250f;
    			fuel += 500f;
    			crew += 50f;
    			crewLimit += 250f;
    			fuelCost += 2f;
    			supplyCost += 4f;
    		}
    		stats.getFuelMod().modifyFlat("trainModuleBonus", fuel);
    		stats.getCargoMod().modifyFlat("trainModuleBonus", cargo);
    		stats.getMinCrewMod().modifyFlat("trainModuleBonus", crew);
    		stats.getFuelUseMod().modifyFlat("trainModuleBonus", fuelCost);
    		stats.getMaxCrewMod().modifyFlat("trainModuleBonus", crewLimit);
    		stats.getSuppliesPerMonth().modifyFlat("trainModuleBonus", supplyCost);
		}
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    	
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        super.advanceInCombat(ship, amount);

        // Initiates the SinuousSegment array.
        args[0] = "SEGMENT1";
        args[1] = "SEGMENT2";
        args[2] = "SEGMENT3";
        args[3] = "SEGMENT4";
        args[4] = "SEGMENT5";

        List<ShipAPI> children = ship.getChildModulesCopy();

        advanceParent(ship,children);
        for (ShipAPI s : children){
            advanceChild(s, ship);
        }

        TrainWagon.setup(seg, children, args);

        // Iterates through each SinuousSegment
        for (int f = 0; f < NUMBER_OF_SEGMENTS; f++) {
            if (seg[f] != null && seg[f].ship != null && seg[f].ship.isAlive()) {
                try {

                    // First segment is "vanilla" / attached to mothership. Rest are pseudo-attached to previous segment's SEGMENT slot
                    if (f != 0)
                        seg[f].ship.getLocation().set(seg[f].previousSegment.ship.getHullSpec().getWeaponSlotAPI("SEGMENT").computePosition(seg[f].previousSegment.ship));


                    // Each module hangs stationary in real space, instead of turning with the mothership, unless it's at max turning range
                    float angle = normalizeAngle(seg[f].ship.getFacing() - seg[f].ship.getParentStation().getFacing());

                    // angle of module is offset by angle of previous module, normalized to between 180 and -180
                    float angleOffset = getAngleOffset(seg[f]);
                    if (angleOffset > 180f)
                        angleOffset -= 360f;

                    // angle of range check is offset by angle of previous segment in relation to mothership
                    float localMod = normalizeAngle(seg[f].previousSegment.ship.getFacing() - seg[f].ship.getParentStation().getFacing());

                    // range limit handler. If the tail is outside the max range, it won't swing any farther.
                    if (angleOffset < RANGE * -0.5)
                        angle = normalizeAngle(RANGE * -0.5f + localMod);
                    if (angleOffset > RANGE * 0.5)
                        angle = normalizeAngle(RANGE * 0.5f + localMod);

                    // Tail returns to straight position, moving faster the more bent it is - spring approximation
                    angle -= (angleOffset / RANGE * 0.5f) * REALIGNMENT_CONSTANT;

                    seg[f].ship.getStationSlot().setAngle(normalizeAngle(angle));
                } catch (Exception e) {
                    // This covers the gap between when a segment and its dependents die
                }

            } else {
                // When a segment dies, remove all dependent segments
                for (int g = f; g < NUMBER_OF_SEGMENTS; g++){
                    if (seg[g] != null && seg[g].ship != null && seg[g].ship.isAlive()) {
                        try {
                            seg[g].ship.setHitpoints(1f);
                            seg[g].ship.applyCriticalMalfunction(seg[g].ship.getAllWeapons().get(0));
                            seg[g].ship.applyCriticalMalfunction(seg[g].ship.getEngineController().getShipEngines().get(0)); // The ONLY way I've found to kill a module
                        } catch (Exception e){
                        }
                        //seg[g].ship.getFleetMember().getStatus().setDetached(0,true);
                        //seg[g].ship.getFleetMember().getStatus().applyDamage(100000);
                        //Global.getCombatEngine().removeEntity(seg[g].ship);
                    }
//                    seg[g] = null;
                }
            }
        }

    }

    private float normalizeAngle (float f){
	    if (f < 0f)
            return f + 360f;
	    if (f > 360f)
	        return f - 360f;
	    return f;
    }

    private float getAngleOffset (TrainWagon seg){
        try {
            return normalizeAngle(seg.ship.getFacing() - seg.previousSegment.ship.getFacing());
        } catch (Exception e) {
            return 0f;
        }
    }

    //////////
    // This section of code was taken largely from the Ship and Weapon Pack mod.
    // I did not create it. Credit goes to DarkRevenant.
    //////////
    private static void advanceChild(ShipAPI child, ShipAPI parent) {
        ShipEngineControllerAPI ec = parent.getEngineController();
        if (ec != null) {
            if (parent.isAlive()) {
                if (ec.isAccelerating()) {
                    child.giveCommand(ShipCommand.ACCELERATE, null, 0);
                }
                if (ec.isAcceleratingBackwards()) {
                    child.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
                }
                if (ec.isDecelerating()) {
                    child.giveCommand(ShipCommand.DECELERATE, null, 0);
                }
                if (ec.isStrafingLeft()) {
                    child.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
                }
                if (ec.isStrafingRight()) {
                    child.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
                }
                if (ec.isTurningLeft()) {
                    child.giveCommand(ShipCommand.TURN_LEFT, null, 0);
                }
                if (ec.isTurningRight()) {
                    child.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
                }
            }

            ShipEngineControllerAPI cec = child.getEngineController();
            if (cec != null) {
                if ((ec.isFlamingOut() || ec.isFlamedOut()) && !cec.isFlamingOut() && !cec.isFlamedOut()) {
                    child.getEngineController().forceFlameout(true);
                }
            }
        }
        /* Mirror parent's fighter commands */
        if (child.hasLaunchBays()) {
            if (parent.getAllWings().size() == 0 && (Global.getCombatEngine().getPlayerShip() != parent || !Global.getCombatEngine().isUIAutopilotOn()))
                parent.setPullBackFighters(false); // otherwise module fighters will only defend if AI parent has no bays
            if (child.isPullBackFighters() ^ parent.isPullBackFighters()) {
                child.giveCommand(ShipCommand.PULL_BACK_FIGHTERS, null, 0);
            }
            if (child.getAIFlags() != null) {
                if (((Global.getCombatEngine().getPlayerShip() == parent) || (parent.getAIFlags() == null))
                        && (parent.getShipTarget() != null)) {
                    child.getAIFlags().setFlag(AIFlags.CARRIER_FIGHTER_TARGET, 1f, parent.getShipTarget());
                } else if ((parent.getAIFlags() != null)
                        && parent.getAIFlags().hasFlag(AIFlags.CARRIER_FIGHTER_TARGET)
                        && (parent.getAIFlags().getCustom(AIFlags.CARRIER_FIGHTER_TARGET) != null)) {
                    child.getAIFlags().setFlag(AIFlags.CARRIER_FIGHTER_TARGET, 1f, parent.getAIFlags().getCustom(AIFlags.CARRIER_FIGHTER_TARGET));
                } else if (parent.getShipTarget() != null){
                    child.getAIFlags().setFlag(AIFlags.CARRIER_FIGHTER_TARGET, 1f, parent.getShipTarget());
                }
            }
        }
    }
    private static void advanceParent(ShipAPI parent, List<ShipAPI> children) {
        ShipEngineControllerAPI ec = parent.getEngineController();
        if (ec != null) {
            float originalMass = 17000;
            int originalEngines = 16;

            float thrustPerEngine = originalMass / originalEngines;

            /* Don't count parent's engines for this stuff - game already affects stats */
            float workingEngines = ec.getShipEngines().size();
            for (ShipAPI child : children) {
                if ((child.getParentStation() == parent) && (child.getStationSlot() != null) && child.isAlive()) {
                    ShipEngineControllerAPI cec = child.getEngineController();
                    if (cec != null) {
                        float contribution = 0f;
                        for (ShipEngineAPI ce : cec.getShipEngines()) {
                            if (ce.isActive() && !ce.isDisabled() && !ce.isPermanentlyDisabled() && !ce.isSystemActivated()) {
                                contribution += ce.getContribution();
                            }
                        }
                        workingEngines += cec.getShipEngines().size() * contribution;
                    }
                }
            }

            float thrust = workingEngines * thrustPerEngine;
            float enginePerformance = thrust / Math.max(1f, getTrainMass(parent,children));
            parent.getMutableStats().getZeroFluxSpeedBoost().modifyMult("ED_trainlocomotive", enginePerformance);
            parent.getMutableStats().getTurnAcceleration().modifyMult("ED_trainlocomotive", enginePerformance);
            parent.getMutableStats().getAcceleration().modifyMult("ED_trainlocomotive", enginePerformance);
            parent.getMutableStats().getMaxTurnRate().modifyMult("ED_trainlocomotive", enginePerformance);
            parent.getMutableStats().getMaxSpeed().modifyMult("ED_trainlocomotive", enginePerformance);
        }
    }
    
    private static float getTrainMass(ShipAPI ship, List<ShipAPI> modules) {
    	float mass = ship.getMass();    	
    	if(modules != null) {
    		for(ShipAPI m : modules) {
    			if(m != null && m.isAlive()) {
    				mass += m.getMass();
    			}
    		}
    	}
    	return mass;
    }
}
