package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class WurgandalMassiveHull extends BaseHullMod {

    public static final float SENSOR_MOD = 150f;
    public static final float PROFILE_MOD = 60f;
    public static final float ZEROFLUXSPEED_MOD = 0.30F;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSensorStrength().modifyFlat(id, SENSOR_MOD);
        stats.getSensorProfile().modifyFlat(id, PROFILE_MOD);
        stats.getZeroFluxSpeedBoost().modifyMult(id, ZEROFLUXSPEED_MOD);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
    	switch (index) {
        case 0:
            return "" + (int) SENSOR_MOD;
        case 1:
            return "" + (int) PROFILE_MOD;
        case 2:
            return "" + (int) Math.round(ZEROFLUXSPEED_MOD * 100f) + "%";
        default:
            break;
    }
        return null;
    }
}
