package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class WurgandalBombard extends BaseHullMod {

    public static final float GROUND_BONUS = 400;
    public static final float BOMBARD_BONUS = 8000;
    public static final float CR_PENALITY = 0.25f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.FLEET_GROUND_SUPPORT).modifyFlat(id, GROUND_BONUS);
        stats.getDynamic().getMod(Stats.FLEET_BOMBARD_COST_REDUCTION).modifyFlat(id, BOMBARD_BONUS);
    }    

    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        switch (index) {
            case 0:
                return "" + (int) GROUND_BONUS;
            case 1:
                return "" + (int) BOMBARD_BONUS;
            case 2:
                return "" + (int) Math.round(CR_PENALITY * 100f) + "%";
            default:
                break;
        }
        return null;
    }
}
