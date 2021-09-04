package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.StolenUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SteamDriveAI implements ShipSystemAIScript {
	
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipAPI ship;
    private final IntervalUtil tracker = new IntervalUtil(0.8f, 1f);

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
            boolean use = false;
            
            //Global.getCombatEngine().addFloatingText(ship.getLocation(),""+ship.getSystem().isActive(), 100, Color.RED, ship, 1, 1);
            
            if(ship.getSystem().isActive()) {
            	Vector2f endPoint = new Vector2f(ship.getLocation());
		        endPoint.x += Math.cos(Math.toRadians(ship.getFacing())) * 5000;
		        endPoint.y += Math.sin(Math.toRadians(ship.getFacing())) * 5000;
		        
		        ShipAPI s = StolenUtils.getFirstNonFighterOnSegment(ship.getLocation(), endPoint, ship);
            	if(ship.getFluxLevel() > 0.75f || (s == null || s.getOwner() == ship.getOwner())) {
            		use = true;
            	}            	
            }else {
            	if(ship.getFluxLevel() < 0.25f && !(flags.hasFlag(AIFlags.MANEUVER_TARGET) || flags.hasFlag(AIFlags.TURN_QUICKLY)) ) {
            		Vector2f endPoint = new Vector2f(ship.getLocation());
			        endPoint.x += Math.cos(Math.toRadians(ship.getFacing())) * 5000;
			        endPoint.y += Math.sin(Math.toRadians(ship.getFacing())) * 5000;
			        
			        ShipAPI s = StolenUtils.getFirstNonFighterOnSegment(ship.getLocation(), endPoint, ship);
			        if(s!= null && s.isAlive() && s.getOwner() != ship.getOwner() && MathUtils.getDistance(s, ship) > 1000) {
			        	use = true;
			        }
            	}
            }
            
            if(use) {
            	ship.useSystem();
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        this.flags = flags;
    }
}
