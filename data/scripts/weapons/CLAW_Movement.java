package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class CLAW_Movement implements EveryFrameWeaponEffectPlugin {

	
	int frame = 0;
	boolean retracting = false;
	boolean animating = false;
	boolean skipFrame = true;
	private final IntervalUtil interval = new IntervalUtil(3f, 10f);
	
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) return;				
		
		ShipAPI ship = weapon.getShip();
		if (ship.isHulk()){
			weapon.getAnimation().setFrame(11);
			return;
		}
		
		weapon.getAnimation().setFrame(frame);
		
		if (ship.getFluxTracker().isOverloaded()) {
			if(Math.random() > 0.65) {
				return;
			}
			if(Math.random() > 0.5) {
				frame++;
			}else {
				frame--;
			}
			if(frame >= 10) {
				frame = 10;
			}else if(frame <= 0) {
				frame = 0;
			}
			weapon.getAnimation().setFrame(frame);
		}else {			
			if(animating) {				
				if(skipFrame) {
					skipFrame = false;
					return;
				}
				skipFrame = true;
				if (retracting) {
					if(frame <= 0) {
						frame = 0;
						animating = false;
					}else {
						frame--;
					}
				} else {
					if(frame >= 10) {
						frame = 10;
						animating = false;
					}else {
						frame++;
					}
				}
			}else {
				interval.advance(amount);
				if(interval.intervalElapsed()) {
					animating = true;
					retracting = !retracting;
				}
			}			
		}
	}
}
