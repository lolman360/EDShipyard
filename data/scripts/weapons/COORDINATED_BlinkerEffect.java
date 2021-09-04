package data.scripts.weapons;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class COORDINATED_BlinkerEffect implements EveryFrameWeaponEffectPlugin {

	
	List<WeaponAPI> blinkers;
	ShipAPI ship;
	boolean dead = false;
	int frame = 0;
	int blinker = 0;
	int wait = 60;
	boolean next = false;	
	IntervalUtil fuck = new IntervalUtil(1, 1);
	
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) return;
		
		if(ship == null) {
			init(weapon);
			return;
		}
		
		if(dead) {
			return;
		}
		
		if (ship.isHulk()) {
			for(WeaponAPI b : blinkers) {
				b.getAnimation().setFrame(0);
			}
			dead = true;
			return;
		}
		
		if (ship.getFluxTracker().isOverloaded()) {
			for(WeaponAPI b : blinkers) {
				b.getAnimation().setFrame(Math.max(0, ((int)(Math.random()*12)) - 8));
			}
			frame = 0;
			blinker = 0;
			next = false;
		}else {
			if(wait > 0) {
				wait--;
			}else {
				if(next) {
					blinker++;
					if(blinker >= blinkers.size()) {
						blinker = 0;
						wait = 60;
					}
					next = false;
				}else {
					frame++;
					if(frame >= 6) {
						frame = 0;
						next = true;
					}					
				}
				blinkers.get(blinker).getAnimation().setFrame(frame);
			}			
		}
	}
	
	public void init(WeaponAPI weapon) {
		ship = weapon.getShip();
		blinkers = new ArrayList<WeaponAPI>();
    	//ArrayList<WeaponAPI> weapons = new ArrayList<WeaponAPI>();
		for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && w.getDisplayName().contains("Blinker Light")) {
            	blinkers.add(w);
            }
        }
        /*for(int i = 0; i < weapons.size(); i++) {
            for (WeaponAPI w : weapons) {
            	String s = "";
            	if(i < 9) {
            		s += "0";
            	}
                if(w.getSlot().getId().contains(s+((int)i+1))) {
                	blinkers.add(w);
                	break;
                }
            }            
        }*/
	}
}
