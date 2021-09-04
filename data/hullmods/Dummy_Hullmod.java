package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class Dummy_Hullmod extends BaseHullMod {

	@Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
    }
	
	@Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
