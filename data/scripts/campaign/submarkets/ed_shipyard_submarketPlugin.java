package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;

public class ed_shipyard_submarketPlugin extends BaseSubmarketPlugin {
	
	//Based on Nia's tahran stuff

    private final RepLevel MIN_STANDING = RepLevel.FAVORABLE;

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }


    @Override
    public float getTariff() {
        switch (market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER))) {
            case COOPERATIVE:
                return 0.15f;
            case FRIENDLY:
                return 0.30f;
            case WELCOMING:
                return 0.45f;
            default:
                return 0.60f;
        }
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        if (market.getFaction() != Global.getSector().getFaction(Factions.INDEPENDENT)) {
            return "Defunct due to hostile occupation";
        }
        if (!Global.getSector().getPlayerFleet().isTransponderOn()) {
            return "Requires: Transponder on";
        }
        if (!level.isAtWorst(MIN_STANDING)) {
            return "Requires: " + market.getFaction().getDisplayName() + " - "
                    + MIN_STANDING.getDisplayName().toLowerCase();
        }
        return super.getTooltipAppendix(ui);
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        if (market.getFaction() != Global.getSector().getFaction(Factions.INDEPENDENT)) {
            return false;
        }
        if (!Global.getSector().getPlayerFleet().isTransponderOn()) {
            return false;
        }
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        return level.isAtWorst(MIN_STANDING);
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        sinceLastCargoUpdate = 0f;

        if (okToUpdateShipsAndWeapons()) {
            sinceSWUpdate = 0f;

            getCargo().getMothballedShips().clear();
            
            getCargo().clear();

            float quality = 1.25f;

            FactionDoctrineAPI doctrineOverride = submarket.getFaction().getDoctrine().clone();
            doctrineOverride.setShipSize(3);
            addShips(submarket.getFaction().getId(),
                    160f, // combat
                    40f, // freighter
                    40f, // tanker
                    10f, // transport
                    10f, // liner
                    40f, // utilityPts
                    quality, // qualityOverride
                    0f, // qualityMod
                    ShipPickMode.PRIORITY_THEN_ALL,
                    doctrineOverride);

            //pruneWeapons(0f);

            addWeapons(9, 12, 5, submarket.getFaction().getId());
            
            addFighters(1, 3, 5, submarket.getFaction().getId());

            //pruneShips(0.5f);
        }

        getCargo().sort();
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "Sales only!";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "Sales only!";
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }
}