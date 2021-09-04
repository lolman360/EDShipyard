package data.hullmods;

import java.util.HashSet;
import java.util.Set;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class FieldShield extends BaseHullMod {
	
	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);
	
	static {
        BLOCKED_HULLMODS.add("frontshield");
        BLOCKED_HULLMODS.add("adaptiveshields");
        BLOCKED_HULLMODS.add("swp_shieldbypass");
        BLOCKED_HULLMODS.add("advancedshieldemitter");
        BLOCKED_HULLMODS.add("extendedshieldemitter");
        BLOCKED_HULLMODS.add("hardenedshieldemitter");
        BLOCKED_HULLMODS.add("stabilizedshieldemitter");
        BLOCKED_HULLMODS.add("frontemitter");
    }

	@Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
            }
        }
    }
	
	@Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
