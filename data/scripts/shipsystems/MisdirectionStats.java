package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import data.scripts.util.MagicRender;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class MisdirectionStats extends BaseShipSystemScript {

    private ShipAPI ship;
    private Random rng;
    private CombatEngineAPI engine;
    private float curTime = 0;
    private float shipradius;
    private Queue<MissileAPI> effectQueue;
    private static final Vector2f ZERO = new Vector2f();
    
    private static final Color JITTER_UNDER_COLOR = new Color(150, 125, 50, 200);
    private static final Color JITTER_OVER_COLOR = new Color(150, 125, 50, 100);
        
    public void init(MutableShipStatsAPI stats) {
    	ship = (ShipAPI) stats.getEntity();
    	engine = Global.getCombatEngine();
    	rng = new Random();
        curTime = 0;
        effectQueue = new LinkedList<MissileAPI>();
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        
        if (ship == null) {
        	init(stats);
        }
        
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }
        
        if(!effectQueue.isEmpty()) {
        	if(effectQueue.size() > 10 || rng.nextBoolean()) {
        		MissileAPI missile = effectQueue.poll();
            	if(missile != null) {
            		MagicRender.battlespace(                			
                	        Global.getSettings().getSprite("fx","ed_target"),
                	        missile.getLocation(),
                	        missile.getVelocity(),
                	        new Vector2f(24,24),
                	        new Vector2f(24,24),
                	        ship.getFacing() -90f,
                	        (float)(Math.random()-0.5f)*10,
                	        new Color(255,200,255),
                	        true,
                	        0,
                	        0.75f,
                	        0.25f
                	);
            		Global.getSoundPlayer().playSound("detected_by_player", 0.8f, 0.8f, missile.getLocation(), ZERO);
            	}
        	}        	
        }
        
        if(state == State.IN){ 
        	shipradius = ship.getCollisionRadius();
        	ship.setJitterUnder(this, JITTER_UNDER_COLOR, 0.5f + effectLevel*0.5f, 1, 1, 5 + effectLevel*100f);
        	ship.setJitter(this, JITTER_OVER_COLOR, 0.5f + effectLevel*0.5f, 1, 1, 5 + effectLevel*50f);
        	
        	curTime += Global.getCombatEngine().getElapsedInLastFrame();
			while(curTime >= 0.5f) {
				curTime -= 0.5f;				
				pulse(effectLevel);
			} 
        	
        }else if(state == State.ACTIVE){ 
        	ship.setJitterUnder(this, JITTER_UNDER_COLOR, 1f, 1, 3f, 5 + effectLevel*100f);
        	ship.setJitter(this, JITTER_OVER_COLOR, 1f, 1, 3f, 5 + effectLevel*50f);
        	
        	curTime += Global.getCombatEngine().getElapsedInLastFrame();
			while(curTime >= 0.5f) {
				curTime -= 0.5f;	
				pulse(effectLevel);
			} 
        }else if(state == State.OUT){ 
        	ship.setJitter(this, JITTER_OVER_COLOR, 0.5f + effectLevel*0.5f, 1, 1, 5+effectLevel*50f);
        }
    }
    
    public void pulse(float effectLevel) {
    	float radius = shipradius + shipradius*effectLevel*3f;
    	
    	Global.getSoundPlayer().playSound("system_quantumdisruptor", 0.7f, 0.4f, ship.getLocation(), ZERO);
		
    	int numParticles = 90;
    	float angleicnrease = 4f;
    	float curangle = 0;
        for (int i = 0; i < numParticles; i++) {
            Vector2f particlePoint = MathUtils.getPointOnCircumference(ship.getLocation(), shipradius/2f, curangle);
            Vector2f particleVel = new Vector2f(radius, 0f);
            VectorUtils.rotate(particleVel, curangle, particleVel);
            Global.getCombatEngine().addHitParticle(particlePoint, particleVel, 15f, 0.6f + (effectLevel*0.3f), 1f, JITTER_OVER_COLOR);
            curangle += angleicnrease;
        }         
        
		List<MissileAPI> allMissiles = CombatUtils.getMissilesWithinRange(ship.getLocation(), radius);
        Collections.shuffle(allMissiles);
        for (MissileAPI missile : allMissiles) {
            if (missile.getOwner() != ship.getOwner()) {
                if (missile.getCollisionClass() == CollisionClass.NONE) {
                    continue;
                }
                missile.setOwner(ship.getOwner());
                missile.setSource(ship);
                effectQueue.add(missile);
            }
        }
        
        List<FighterWingAPI> fighters = ship.getAllWings();
        if(fighters != null && !fighters.isEmpty()) {
        	for(FighterWingAPI wing : fighters){
        		int num = wing.getWingMembers().size();
        		if(num <= 0) {
        			continue;
        		}
        		ShipAPI fighter = wing.getWingMembers().get(rng.nextInt(num));
            	ship.setJitter(fighter, JITTER_OVER_COLOR, 1f, 5, 3f, 5 + effectLevel*50f);
        		numParticles = 40;
        		angleicnrease = 360f/numParticles;
        		curangle = 0;
                for (int i = 0; i < numParticles; i++) {
                    Vector2f particlePoint = MathUtils.getPointOnCircumference(fighter.getLocation(), 50, curangle);
                    Vector2f particleVel = new Vector2f(250, 0f);
                    VectorUtils.rotate(particleVel, curangle, particleVel);
                    Global.getCombatEngine().addHitParticle(particlePoint, particleVel, 12f, 0.5f + (effectLevel*0.3f), 1f, JITTER_OVER_COLOR);
                    curangle += angleicnrease;
                }         
                
				List<MissileAPI> fMissiles = CombatUtils.getMissilesWithinRange(fighter.getLocation(), 200);
		        Collections.shuffle(fMissiles);
		        for (MissileAPI missile : fMissiles) {
		            if (missile.getOwner() != ship.getOwner()) {
		                if (missile.getCollisionClass() == CollisionClass.NONE) {
		                    continue;
		                }
		                missile.setOwner(ship.getOwner());
		                missile.setSource(ship);
		                effectQueue.add(missile);
		            }
		        }
        	}
        }
    }
}
