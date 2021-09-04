package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.StolenUtils;

import java.awt.Color;
import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

public class DoppelswapStats extends BaseShipSystemScript {
	
	public final Color JITTER_COLOR = new Color(125, 50, 150, 150);

	public Random rng;
	public ShipAPI ship;
	public ShipAPI target;
	boolean teleported;
    
    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
    
    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    	teleported = false;
    	return;
    }
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
    	
        if (ship == null) {
        	ship = ((ShipAPI) stats.getEntity());
        	teleported = false;
        	rng = new Random();
            return;
        }
        
        //Global.getCombatEngine().addFloatingText(ship.getLocation(), module.toString(), 100, Color.RED, ship, 1, 1);
        
        if(!teleported) {        	
        	ShipAPI target = ship.getShipTarget();
            if(target != null && target.isDrone() && target.getDroneSource() == ship && !target.isPhased()) {
            	swap(ship,target);
            }else {
            	target = null;
                float minthreat = Float.MAX_VALUE;
                for(ShipAPI doppel : ship.getDeployedDrones()){
            		if(doppel.isAlive() && !doppel.isPhased()) {
            			float inc = StolenUtils.estimateAllIncomingDamage(doppel) + rng.nextInt(9);
            			if(minthreat > inc) {
            				minthreat = inc;
            				target = doppel;
            			}
            		}
            	}
                if(target != null) {
                	swap(ship,target);
                }
            }
        }
    }

    public void swap(ShipAPI ship, ShipAPI target) {
    	ship.setJitterUnder(ship, JITTER_COLOR, 1f, 5, ship.getCollisionRadius()*1.5f);
    	target.setJitterUnder(target, JITTER_COLOR, 1f, 5, target.getCollisionRadius()*1.5f);
    	Vector2f loc1 = new Vector2f(ship.getLocation());
		Vector2f loc2 = new Vector2f(target.getLocation());
		Vector2f spd1 = new Vector2f(ship.getVelocity());
		Vector2f spd2 = new Vector2f(target.getVelocity());
		float facing = ship.getFacing();
		ship.setFacing(target.getFacing());
		target.setFacing(facing);
		ship.getLocation().set(loc2);
		target.getLocation().set(loc1);
		ship.getVelocity().set(spd2);
		target.getVelocity().set(spd1);
		teleported = true;
    }
}
