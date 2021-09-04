package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.util.StolenUtils;

import java.awt.Color;
import java.util.Random;

public class IllusionStats extends BaseShipSystemScript {

    public final Color JITTER_COLOR = new Color(125, 50, 150, 255);
    public final Color GLOW_COLOR = new Color(175, 75, 175);
    
    public ShipAPI ship;
    public Random rng;
    public CombatEngineAPI engine;
    public int team;
    public boolean glitched;
    private final IntervalUtil interval = new IntervalUtil(2f, 5f);
        
    public void init(MutableShipStatsAPI stats) {
    	ship = (ShipAPI) stats.getEntity();
    	engine = Global.getCombatEngine();
    	rng = new Random();
    	glitched = false;
    	team = ship.getOwner();
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        
        if (ship == null) {
        	init(stats);
        }
        
        if (engine == null) {
            return;
        }
        
        if(!ship.isAlive()) {
        	engine.removeEntity(ship);
        }
        
        float amount = Global.getCombatEngine().getElapsedInLastFrame();

        if (engine.isPaused()) {
        	amount = 0;
            return;
        }
        
        interval.advance(amount);
        if (interval.intervalElapsed()) {
        	if(rng.nextFloat() > 0.75f) {
        		ship.getAIFlags().setFlag(AIFlags.BACK_OFF, rng.nextFloat()*5f);
        		ship.getAIFlags().setFlag(AIFlags.BACKING_OFF, rng.nextFloat()*5f);
        		ship.getAIFlags().setFlag(AIFlags.RUN_QUICKLY, rng.nextFloat()*5f);
        		ship.getAIFlags().setFlag(AIFlags.PHASE_ATTACK_RUN_TIMEOUT, rng.nextFloat()*5f);
        		ship.getAIFlags().setFlag(AIFlags.DO_NOT_PURSUE, rng.nextFloat()*5f);
        		ship.getAIFlags().removeFlag(AIFlags.HARASS_MOVE_IN);
        		ship.getAIFlags().removeFlag(AIFlags.IN_ATTACK_RUN);
        	}
        }
        
        if(glitched) {
        	if(effectLevel <= 0) {
        		StolenUtils.setArmorPercentage(ship, 1f);
        		ship.setHitpoints(ship.getMaxHitpoints());
        		ship.setPhased(false);
        		engine.removeEntity(ship);
        		glitched = false;
        	}else {
        		//engine.addFloatingText(ship.getLocation(), "jittah", 100, Color.RED, ship, 1, 1);
        		ship.setJitter(ship, JITTER_COLOR, 1f - effectLevel*0.5f, 8, ship.getCollisionRadius()*(1.1f - effectLevel)*2.5f);
        	}
        }else {
        	if(StolenUtils.getArmorPercent(ship) < 1f) {
            	glitched = true;
            	ship.setPhased(true);
            	ship.beginLandingAnimation(ship.getDroneSource());
            	ship.useSystem();
            }
        }
    }
}
