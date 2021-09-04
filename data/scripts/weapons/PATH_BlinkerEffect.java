package data.scripts.weapons;

import java.util.Random;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class PATH_BlinkerEffect implements EveryFrameWeaponEffectPlugin {

	int frame = 0;
	int wait = 0;
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) return;

		ShipAPI ship = weapon.getShip();
		
		if (ship.getFluxTracker().isOverloaded()) {
			weapon.getAnimation().setFrame(Math.max(0, new Random().nextInt(12) - 8));
		}else {
			wait++;
			if(wait < 10) {
				return;
			}else {
				wait = 0;
			}
			
			frame++;
			if(frame >= 4) {
				frame = 0;
				wait = -30;//large wait time
			}		
			
			if (ship.isHulk()) {
				weapon.getAnimation().setFrame(0);
			} else {
				weapon.getAnimation().setFrame(frame);
			}
		}
	}
}
