package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import java.util.Random;
import java.util.Stack;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class ReloadCRStats extends BaseShipSystemScript {
	
	public final Color JITTER_COLOR = new Color(125, 50, 150, 150);

	private Random rng;
	private ShipAPI ship;
    private CombatEngineAPI engine;
	private WeaponAPI gun;
	private WeaponAPI loader;
	private WeaponAPI cylinder;
	private boolean spun;
	private boolean lucky;
	private boolean firing;
	private boolean loaded;
	private boolean spinning;
	private boolean reloading;
	private int ammo;
	private int frameS;
	private int frameR;
	private float currentTimer = 0;
	private final float timePerFrame = 0.0166666666667f;//change frame
	private final float luckychance = 0.05f;
	private IntervalUtil interval = new IntervalUtil(1f, 1f);
	private final int[] spinningFrames = {0,1,1,1,1,2,2,2,3,3,4,4,5,5,6,6,7,7,7,0};//20 frames
	private final int[] reloadingFrames = {0,1,1,2,2,3,4,5,6,7,8,8,9,9,10,10,11,11,12,12,13,13,12,11,10,9,8,7,6,5,4,4,3,3,2,2,2,1,1,1,0};//40 frames
	private Stack<String> orders;
    
    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0 && lucky) {
			return new StatusData("Next shot deals 50% extra damage", false);
		}
        return null;
    }
    
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
    	if(firing) {
        	return "WEAPON FIRING";
        }
        if(reloading) {
        	return "LOADING BULLET";
        }
        if(spinning) {
        	return "SPINNING CYLINDER";
        }
        if(gun == null) {
        	return "GUN NOT FOUND";
        }
        if(gun.getAmmo() >= 6) {
        	return "FULLY LOADED";
        }
        if(gun.getAmmo() == 5) {
        	return "RELOAD 1 BULLET";
        }
    	return "RELOAD " + (6-gun.getAmmo()) + " BULLETS";
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
    	if(gun == null || orders == null || !orders.isEmpty() || firing) {
    		return false;
    	}
    	if(gun.getAmmo() < 6) {
    		return true;
    	}
    	return false;
    }
    
    public void init(MutableShipStatsAPI stats) {
    	ship = (ShipAPI) stats.getEntity();
    	engine = Global.getCombatEngine();
    	rng = new Random();
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().isBuiltIn() && w.getDisplayName().equals("Carolina Reaper")) {
            	gun = w;
            }else if (w.isDecorative() && w.getDisplayName().equals("Carolina Cylinder")) {
            	cylinder = w;
            }else if (w.isDecorative() && w.getDisplayName().equals("Carolina Loader")) {
            	loader = w;
            }
        }
        orders = new Stack<String>();
        frameS = 0;
        frameR = 0;
        spun = false;
        lucky = false;
        firing = false;
        loaded = false;
        spinning = false;
        reloading = false;        
    }
    
    public void rollLuck(int ammo, MutableShipStatsAPI stats, String id) {    	
    	if(rng.nextFloat() < (6-ammo)*luckychance) {
    		Global.getCombatEngine().addFloatingText(ship.getLocation(), "Feeling Lucky", 30, Color.YELLOW, ship, 1, 1);
    		stats.getBallisticWeaponDamageMult().modifyMult(id, 1.5f, "Lucky Shot");
    		lucky = true;
    	}
    }
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
    	
        if (ship == null || gun == null) {
        	init(stats);        	
            return;
        }
        
        if(gun.getAmmo() > 6) {//magazines
        	gun.setAmmo(6);
        }
        
        if (engine.isPaused()) {
            return;
        }
        
        if(!orders.isEmpty()) {
        	if(!orders.peek().equals("wait")){
        		String order = orders.pop();
        		switch(order) {
        			case "spin":spinning = true;spun = false;orders.add("wait");break;
        			case "load":reloading = true;loaded = false;orders.add("wait");break;
        			case "end":gun.setRemainingCooldownTo(1f); gun.setAmmo(6);rollLuck(ammo, stats, id);ammo = 0;break;
        		}
        	}
        }        
        
        float time = Global.getCombatEngine().getElapsedInLastFrame();
        
        if(reloading == true) {
        	if(loaded == true) {
        		currentTimer += time;
        		frameR = (int)(currentTimer/timePerFrame);
        		if(frameR >= reloadingFrames.length - 1) {
        			frameR = 0;
        			currentTimer = 0;
        			reloading = false;
            		if(!orders.isEmpty()) {
            			orders.pop();//remove wait order
            		}
            		orders.add("spin");
        		}
        		loader.getAnimation().setFrame(reloadingFrames[frameR]);
        	}else {
        		interval.advance(time);
            	if(interval.intervalElapsed()) {
            		loaded = true;            		
            		Global.getSoundPlayer().playSound("carolina_load", 1f, 1f, ship.getLocation(), ship.getVelocity());
            	}
        	}
        }else if(spinning == true) {
        	if(spun == true) {
        		currentTimer += time;
        		frameS = (int)(currentTimer/timePerFrame);
        		if(frameS >= spinningFrames.length - 1) {
        			frameS = 0;
        			currentTimer = 0;
        			spinning = false;
        			 if(!orders.isEmpty()) {
        				 orders.pop();//remove wait order
        			 }
        		}
        		cylinder.getAnimation().setFrame(spinningFrames[frameS]);
        	}else {
        		interval.advance(time);
            	if(interval.intervalElapsed()) {
            		spun = true;            		
            		Global.getSoundPlayer().playSound("carolina_cylinder", 1f, 1f, ship.getLocation(), ship.getVelocity());
            	}
        	}
        }
        
        //if(gun.getAmmo() == 0) {
        //	gun.setRemainingCooldownTo(3.5f);
        //}
        
        if(orders.isEmpty()) {
        	if(effectLevel > 0) {
            	ammo = gun.getAmmo();
            	orders.clear();
            	gun.setAmmo(0);
            	switch(ammo) {//fill stack, reverse order
            		case 5: orders.add("end");orders.add("load");orders.add("spin");orders.add("spin");break; //spin 2, load 1
            		case 4: orders.add("end");orders.add("load");orders.add("load");orders.add("spin");break; //spin 1, load 2
            		case 3: orders.add("end");orders.add("load");orders.add("load");orders.add("load");break; //load 3
            		case 2: orders.add("end");orders.add("load");orders.add("spin");orders.add("spin");orders.add("load");orders.add("load");orders.add("load");break; //load 3, spin 2, load 1
            		case 1: orders.add("end");orders.add("load");orders.add("load");orders.add("spin");orders.add("load");orders.add("load");orders.add("load");break; //load 3, spin 1, load 2
            		case 0: orders.add("end");orders.add("load");orders.add("load");orders.add("load");orders.add("load");orders.add("load");orders.add("load");break; //spin 6
            	}
            }else {
            	if(!firing) {            		
                	if(gun.getChargeLevel() >= 1) {
                		firing = true;
                		orders.add("spin");
                		float aim = gun.getArcFacing() + ship.getFacing();
    		            Vector2f recoil = MathUtils.getPoint(gun.getLocation(), 100, aim-180);
    		            float feedback = 450f;                		
                		if(lucky) {
                			lucky = false;
                			feedback = 750f;
                			Global.getCombatEngine().addFloatingText(MathUtils.getPoint(gun.getLocation(), 100, ship.getFacing()), "Lucky Shot", 30, Color.RED, ship, 1, 1);
                			Global.getSoundPlayer().playSound("carolina_fire", 0.7f, 1.6f, ship.getLocation(), ship.getVelocity());
                			stats.getBallisticWeaponDamageMult().unmodify(id);
                		}
                		CombatUtils.applyForce(ship, VectorUtils.getDirectionalVector(gun.getLocation(), recoil), feedback);
                	}
                }else {
                	if(gun.getChargeLevel() == 0) {
                		firing = false;
                	}
                }
            }
        }       
    }
}
