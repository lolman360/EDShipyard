package data.scripts.weapons;

import java.util.Random;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class ABYSS_BlinkerEffect implements EveryFrameWeaponEffectPlugin {

	boolean on = true;
	boolean flicker = true;
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) return;
		flicker = !flicker;
		
		on = true;
		
		ShipAPI ship = weapon.getShip();
		if (ship.isHulk()) on = false;
		
		if (ship.getFluxTracker().isOverloaded()) {
			on = new Random().nextInt(4) == 3;
		}
		
		if (on) {
			if(flicker) {
				weapon.getAnimation().setFrame(0);
			}else {
				weapon.getAnimation().setFrame(1);
			}			
		} else {
			weapon.getAnimation().setFrame(2);
		}
	}
}
