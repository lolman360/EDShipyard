package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class ReloadCRAI implements ShipSystemAIScript {
	
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
    private ShipAPI ship;
    private WeaponAPI gun;
    private final IntervalUtil tracker = new IntervalUtil(1f, 1f);

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }

        if(gun == null) {
        	for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getSlot().isBuiltIn() && w.getDisplayName().equals("Carolina Reaper")) {
                	gun = w;
                	break;
                }
            }
        	return;
        }
        
        tracker.advance(amount);
        if (tracker.intervalElapsed()) {        	
            if (!AIUtils.canUseSystemThisFrame(ship)) {            	
                return;
            }            
            if(gun.getAmmo() == 0) {
            	ship.useSystem();
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;        
    }
}
