package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.ArrayList;
import java.util.List;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class GrandSalvoAI implements ShipSystemAIScript {
	
	//code based on Shock Buster system by Dark Revenant (Interstellar Imperium)

    private CombatEngineAPI engine;
    private static final float TARGET_RANGE = 900f;
    public ArrayList<WeaponAPI> guns;
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
            if (!AIUtils.canUseSystemThisFrame(ship)) {            	
                return;
            }

            if(guns == null || guns.isEmpty()) {
            	guns = new ArrayList<WeaponAPI>();
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (w.isDecorative() && w.getDisplayName().contains("Hellborn")) {
                    	guns.add(w);
                    }
                }
            	return;
            }
            
            boolean use = false;
            for(WeaponAPI g : guns) {
            	ShipAPI ship = WeaponUtils.getNearestEnemyInArc(g);
            	if(ship != null && !ship.isFighter()) {
            		if(MathUtils.getDistance(this.ship, ship) < TARGET_RANGE) {            			
            			use = true;            			
            			
            			List<ShipAPI> ships = WeaponUtils.getAlliesInArc(g);
                    	if(ships != null) {
            				for(ShipAPI ally : ships) {
            					if(!ally.isStationModule()) {
            						return;
            					}
            				}
            			}
            			
            			break;
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
    }
}
