package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;

import data.scripts.ai.edshipyard_Dummy_AI;
import data.scripts.ai.edshipyard_Maltese_AI;
import data.scripts.ai.edshipyard_Terrier_AI;
import data.scripts.campaign.intel.bar.events.WurgBarEventCreator;

public class ED_modPlugin extends BaseModPlugin {
    
    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        return null;
    }
    
    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
    	switch(ship.getHullSpec().getHullId()) {
    		case "edshipyard_maltese": 
    			return new PluginPick<ShipAIPlugin>(new edshipyard_Maltese_AI(ship), CampaignPlugin.PickPriority.MOD_GENERAL);
    		case "edshipyard_terrier": 
    			return new PluginPick<ShipAIPlugin>(new edshipyard_Terrier_AI(ship), CampaignPlugin.PickPriority.MOD_GENERAL);
    		case "edshipyard_retriever_shield":
    			return new PluginPick<ShipAIPlugin>(new edshipyard_Dummy_AI(), CampaignPlugin.PickPriority.MOD_GENERAL);
    	}    	
    	return super.pickShipAI(member, ship);
    }
    
    @Override
    public void onNewGameAfterEconomyLoad() {
    	for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getSize() >= 5 && market.getFactionId().contentEquals(Factions.INDEPENDENT) && (market.hasIndustry("heavyindustry") || market.hasIndustry("orbitalworks") || market.hasIndustry("waystation"))) {
                market.addSubmarket("ed_shipyard");
            }
        }
    	//TODO look in scalartech for sample on how to add Eron Dust admin
    }
    
    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        
        //TODO wurg event
        BarEventManager barEventManager = BarEventManager.getInstance();
        
        if (!barEventManager.hasEventCreator(WurgBarEventCreator.class)) {
            barEventManager.addEventCreator(new WurgBarEventCreator());
        }
    }
}

