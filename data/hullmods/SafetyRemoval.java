package data.hullmods;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.Misc;

public class SafetyRemoval extends BaseHullMod {

	private static Map<HullSize, Float> speed = new HashMap<HullSize, Float>();
	static {
		speed.put(HullSize.FRIGATE, 30f);
		speed.put(HullSize.DESTROYER, 30f);
		speed.put(HullSize.CRUISER, 30f);
		speed.put(HullSize.CAPITAL_SHIP, 30f);
	}
	
	private static final float RANGE_THRESHOLD = 450f;
	private static final float RANGE_MULT = 0.25f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		if(stats == null) {
			return;
		}
		stats.getMaxSpeed().modifyFlat(id, (Float) speed.get(hullSize));
		//stats.getAcceleration().modifyFlat(id, (Float) speed.get(hullSize));
		//stats.getDeceleration().modifyFlat(id, (Float) speed.get(hullSize));
		stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f);
		
		stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD);
		stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + ((Float) speed.get(HullSize.FRIGATE)).intValue();
		if (index == 1) return "" + ((Float) speed.get(HullSize.DESTROYER)).intValue();
		if (index == 2) return "" + ((Float) speed.get(HullSize.CRUISER)).intValue();
		if (index == 3) return "" + ((Float) speed.get(HullSize.CAPITAL_SHIP)).intValue();
		if (index == 4) return Misc.getRoundedValue(RANGE_THRESHOLD);
		
		return null;
	}
	
	@Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);

        if (ship.getVariant().getHullMods().contains("safetyoverrides")) {
            ship.getVariant().removeMod("safetyoverrides");
        }
    }
	
	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod(HullMods.CIVGRADE) && !ship.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS)) {
			return "Can not be installed on civilian ships";
		}
		
		return null;
	}	

	private Color color = new Color(225,150,225,255);
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
			ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
			ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
	}
	
	@Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

	

}
