package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.ArrayList;
import java.util.Random;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class GrandSalvoStats extends BaseShipSystemScript {

    private ShipAPI ship;
    private Random rng;
    private CombatEngineAPI engine;
    private ArrayList<Float> times;
    private ArrayList<Integer> currentFrames;
    private ArrayList<Integer> extraFrames;
    private ArrayList<WeaponAPI> guns;
    private float curTime = 0;
	private float shootingTime = 0;
        
    public void init(MutableShipStatsAPI stats) {
    	ship = (ShipAPI) stats.getEntity();
    	engine = Global.getCombatEngine();
    	rng = new Random();
    	times = new ArrayList<Float>();
    	currentFrames = new ArrayList<Integer>();
    	extraFrames = new ArrayList<Integer>();
    	guns = new ArrayList<WeaponAPI>();
    	ArrayList<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && w.getDisplayName().contains("Hellborn")) {
            	currentFrames.add(0);
            	extraFrames.add(0);
            	weapons.add(w);
            }
        }
        float waitTime = 0;
        if(!weapons.isEmpty()) {
        	waitTime = 1f/weapons.size();
        }
        for(int i = 0; i < currentFrames.size(); i++) {
            for (WeaponAPI w : weapons) {
                if(w.getSlot().getId().contains((i+1)+"")) {                	
                	guns.add(w);
                	times.add(-(i)*waitTime);
                	break;
                }
            }            
        }        
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
        
        if(state == State.IN){ //open and spin        	
        	curTime += Global.getCombatEngine().getElapsedInLastFrame(); 
        	for(int i = 0; i < times.size(); i++){
        		float time = times.get(i) + curTime;
        		if(time <= 0) { //closed
        			currentFrames.set(i, 0);
        		}else if(time > 1) { //start spinning
        			if(extraFrames.get(i) > Math.min(1, (1 - effectLevel)*5)) {
        				extraFrames.set(i, 0);
            			currentFrames.set(i, currentFrames.get(i) + 1);
            			if(currentFrames.get(i) >= 54) {
                			currentFrames.set(i, 50);
                		}
            		}else {
            			extraFrames.set(i, extraFrames.get(i) + 1);
            		}
        		}else{ //opening
        			if(currentFrames.get(i) == 0) {
    					Global.getSoundPlayer().playSound("system_fast_missile_racks", 1f, 1f, ship.getLocation(), ship.getVelocity());
    				}
        			currentFrames.set(i, (int)(Math.min(50, time*50f)));
        		}
        	}
        	shootingTime = 0;
        }else if(state == State.ACTIVE){ //spin speed max
        	for(int i = 0; i < times.size(); i++){
        		currentFrames.set(i, currentFrames.get(i) + 1);
    			if(currentFrames.get(i) >= 54) {
        			currentFrames.set(i, 50);
        		}
        	}
        	curTime = 0;
        	
        	shootingTime += Global.getCombatEngine().getElapsedInLastFrame();
			while(shootingTime > 0.05) {
				shootingTime -= 0.05f;
				for(int i = 0; i < guns.size();i++) {
					Global.getSoundPlayer().playSound("hellbore_fire", 1f, 1f, ship.getLocation(), ship.getVelocity());
					float aim = guns.get(i).getArcFacing() + ship.getFacing();
		            engine.spawnProjectile(ship, guns.get(i), "edshipyard_BIGGUN", MathUtils.getPoint(guns.get(i).getLocation(), 65, aim), aim  -10f + rng.nextFloat()*20f, new Vector2f());
	        	}
			}        	
        }else{ //retract
        	if(currentFrames.get(currentFrames.size()-1) > 0) {
        		curTime += Global.getCombatEngine().getElapsedInLastFrame(); 
            	for(int i = 0; i < times.size(); i++){
            		float time = times.get(i) + curTime;
            		if(time >= 3) { //closed
            			currentFrames.set(i, 0);
            		}else if(time < 2) { //slow spinning
            			if(extraFrames.get(i) > Math.min(1, (1 - effectLevel)*10)) {
            				extraFrames.set(i, 0);
                			currentFrames.set(i, currentFrames.get(i) + 1);
                			if(currentFrames.get(i) >= 54) {
                    			currentFrames.set(i, 50);
                    		}
                		}else {
                			extraFrames.set(i, extraFrames.get(i) + 1);
                		}
            		}else{ //closing
            			if(currentFrames.get(i) >= 50) {
        					Global.getSoundPlayer().playSound("system_reserve_wing", 1f, 1f, ship.getLocation(), ship.getVelocity());
        				}
            			currentFrames.set(i, (int)(Math.max(0, 50 - (time - 2)*50f)));
            		}
            	}
        	}else {
        		curTime = 0;
        	}        	
        }
        
        for(int i = 0; i < guns.size();i++) {
    		guns.get(i).getAnimation().setFrame(Math.max(currentFrames.get(i), 0));            		
    	}
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        
        if (!isUsable(system, ship)) {
            return "RELOADING";
        }
        return null;
    }
}
