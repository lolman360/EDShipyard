package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class NoHangar extends BaseHullMod {

	@Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);

        if (ship.getVariant().getHullMods().contains("converted_hangar")) {
            ship.getVariant().removeMod("converted_hangar");
        }
    }
	
	@Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
