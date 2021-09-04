package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class RelativityDriveAI implements ShipSystemAIScript {
	
	//code based on Celerity Drive system by Dark Revenant (Interstellar Imperium)

    private static final float TARGET_DESIRE = 1f;

    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipSystemAPI system;

    private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);
    private boolean resetAI = false;
    private final ShipAIConfig savedConfig = new ShipAIConfig();

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
            if (ship.getFluxTracker().isOverloadedOrVenting() || system.isActive() || system.isCoolingDown() || ship.isPhased()) {
                return;
            }

            boolean phaseCooldown = false;
            boolean phaseCooldownShort = false;
            if (ship.getPhaseCloak() != null) {
                if (ship.getPhaseCloak().isCoolingDown()) {
                    phaseCooldown = true;
                    if (ship.getPhaseCloak().getCooldownRemaining() < (ship.getPhaseCloak().getCooldown() / 2f)) {
                        phaseCooldownShort = true;
                    }
                } else if (ship.getPhaseCloak().isChargedown()) {
                    phaseCooldown = true;
                }
            }

            float fluxRemaining = ship.getFluxTracker().getMaxFlux() - ship.getFluxTracker().getCurrFlux();
            if ((ship.getSystem().getFluxPerUse() > fluxRemaining)) {
                return;
            }
            
            float missileThreatLevel = 0f;
            
            boolean missileThreat = missileThreatLevel >= ship.getHitpoints() * 0.5f;
            boolean highMissileThreat = missileThreatLevel >= ship.getHitpoints();
            boolean ultraHighMissileThreat = missileThreatLevel >= ship.getHitpoints() * 2f;

            float desire = 0f;
            if (!flags.hasFlag(AIFlags.BACKING_OFF)) {
                desire = (1 - ship.getFluxLevel()) * 0.75f;
            }

            if (flags.hasFlag(AIFlags.PHASE_ATTACK_RUN_FROM_BEHIND_DIST_CRITICAL)) {
            	desire += (1 - ship.getFluxLevel());
            }

            if (flags.hasFlag(AIFlags.PHASE_ATTACK_RUN_IN_GOOD_SPOT)) {
            	desire += (1 - ship.getFluxLevel()) * 5f;
            }

            if (flags.hasFlag(AIFlags.MAINTAINING_STRIKE_RANGE)) {
                desire += 0.5f;
            }

            desire *= fluxRemaining / Math.max(1f, fluxRemaining + system.getFluxPerUse());

            if (flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER) || ultraHighMissileThreat) {
                if (phaseCooldown && !phaseCooldownShort) {
                    if (highMissileThreat) {
                        desire += 1.25f;
                    } else if (ultraHighMissileThreat) {
                        desire += 1f;
                    } else if (missileThreat) {
                        desire += 0.75f;
                    } else {
                        desire += 0.5f;
                    }
                } else {
                    if (ultraHighMissileThreat) {
                        desire += 1.25f;
                    } else if (highMissileThreat) {
                        desire += 1f;
                    } else if (missileThreat) {
                        desire += 0.5f;
                    } else {
                        desire -= 1f;
                    }
                }
            }

            if (flags.hasFlag(AIFlags.NEEDS_HELP)) {
                if (phaseCooldown && !phaseCooldownShort) {
                	desire += 0.5f;
                }
            }

            if (flags.hasFlag(AIFlags.HAS_INCOMING_DAMAGE) || ultraHighMissileThreat) {
                if (phaseCooldown && !phaseCooldownShort) {
                    if (missileThreat) {
                        desire += 0.25f;
                    }
                } else {
                    if (highMissileThreat) {
                        desire += 0.25f;
                    }
                }
            }

            if (flags.hasFlag(AIFlags.DO_NOT_PURSUE) && !missileThreat) {
                desire -= 0.25f;
            }

            if (flags.hasFlag(AIFlags.DO_NOT_USE_FLUX) && !highMissileThreat) {
                desire -= 0.25f;
            }

            if (flags.hasFlag(AIFlags.RUN_QUICKLY) && !highMissileThreat) {
                desire -= 0.25f;
            }

            if (flags.hasFlag(AIFlags.TURN_QUICKLY) && !missileThreat) {
                desire -= 0.25f;
            }

            float targetDesire;
            targetDesire = TARGET_DESIRE;

            if (desire >= targetDesire) {
                ship.useSystem();
            }
        }

        if (system.isActive()) {
            flags.setFlag(AIFlags.DO_NOT_BACK_OFF, 0.25f);
            flags.unsetFlag(AIFlags.BACK_OFF);
            flags.unsetFlag(AIFlags.DO_NOT_PURSUE);
            flags.unsetFlag(AIFlags.DO_NOT_USE_FLUX);
            flags.unsetFlag(AIFlags.DO_NOT_AUTOFIRE_NON_ESSENTIAL_GROUPS);

            if (!resetAI) {
                resetAI = true;
                saveAIConfig(ship);
                ship.getShipAI().getConfig().alwaysStrafeOffensively = true;
                ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = false;
                ship.getShipAI().getConfig().personalityOverride = Personalities.RECKLESS;
                ship.getShipAI().forceCircumstanceEvaluation();
            }
            
        } else {
            if (resetAI) {
                resetAI = false;
                flags.unsetFlag(AIFlags.DO_NOT_BACK_OFF);
                restoreAIConfig(ship);
                ship.getShipAI().forceCircumstanceEvaluation();
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
        this.engine = engine;
    }

    private void saveAIConfig(ShipAPI ship) {
        if (ship.getShipAI().getConfig() != null) {
            savedConfig.alwaysStrafeOffensively = ship.getShipAI().getConfig().alwaysStrafeOffensively;
            savedConfig.turnToFaceWithUndamagedArmor = ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor;
            savedConfig.personalityOverride = ship.getShipAI().getConfig().personalityOverride;
        }
    }

    private void restoreAIConfig(ShipAPI ship) {
        if (ship.getShipAI().getConfig() != null) {
            ship.getShipAI().getConfig().alwaysStrafeOffensively = savedConfig.alwaysStrafeOffensively;
            ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = savedConfig.turnToFaceWithUndamagedArmor;
            ship.getShipAI().getConfig().personalityOverride = savedConfig.personalityOverride;
        }
    }
}
