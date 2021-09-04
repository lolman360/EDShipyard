package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class NoOmni extends BaseHullMod {

	@Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);

        if (ship.getVariant().getHullMods().contains("adaptiveshields")) {
            ship.getVariant().removeMod("adaptiveshields");
        }
    }
	
	@Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
