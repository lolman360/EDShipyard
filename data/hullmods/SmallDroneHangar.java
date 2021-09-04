package data.hullmods;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class SmallDroneHangar extends BaseHullMod {
		
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    	//based on code from Sylphon RnD SRD_DroneCarrier
    	/*boolean shouldSoundError = false;
        int wrong = 0;
        float total = ship.getMutableStats().getNumFighterBays().getModifiedValue();
        int i = 0;
        while (i < total && i < 20) {
            
        	if (ship.getVariant().getWing(i) == null) {
                i++;
                continue;
            }            
            if (ship.getVariant().getWing(i).getVariant().getHullSpec().getMinCrew() > 0 || ship.getVariant().getWing(i).getOpCost(ship.getMutableStats()) > 8) {
            	String LPC = ship.getVariant().getWingId(i);
            	ship.getVariant().setWingId(i ,null);
            	if(ship.getVariant().getWing(i) == null) {
            		if (Global.getSector() != null) {
                        if (Global.getSector().getPlayerFleet() != null) {
                            Global.getSector().getPlayerFleet().getCargo().addFighters(LPC, 1);
                        }
                    }
            	}               
                shouldSoundError = true;
            }
            i++;
            wrong++;
        }
        
        if (shouldSoundError) {
        	ship.getMutableStats().getFighterRefitTimeMult().modifyMult(id, 1.1f - wrong/total);
            Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 2f);
        }*/ 
    	
    	
    }
    
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width,boolean isForModSpec) {

    	if(ship == null) {
    		return;
    	}
        float total = ship.getMutableStats().getNumFighterBays().getModifiedValue();
        List<String> wrongLPCs = new ArrayList<String>();
        int i = 0;
        while (i < total && i < 20) {
            
        	if (ship.getVariant().getWing(i) == null) {
                i++;
                continue;
            }            
            if ((!ship.getVariant().getHullMods().contains("SKR_remote") && ship.getVariant().getWing(i).getVariant().getHullSpec().getMinCrew() > 0) || ship.getVariant().getWing(i).getOpCost(ship.getMutableStats()) > 12) {
            	wrongLPCs.add("Bay " + i + ": " + ship.getVariant().getWing(i).getVariant().getHullSpec().getHullName() + " " + ship.getVariant().getWing(i).getVariant().getDisplayName());            	
            }
            i++;            
        }
    	
    	if(wrongLPCs.size() > 0) {
    		LabelAPI label = tooltip.addPara("WARNING: You have equipped LPCs that are not compatible with this ship's drone bays. ED Shipyards is not responsible for the lost lives of your pilots due to your negligent use of our carriers.", 5f);
    		tooltip.addPara("The following LPCs are prone to malfuncion:", 5f);
            label.setHighlightColors(Color.RED);
            label.setHighlight("WARNING:");            
    		for(String s : wrongLPCs) {
    			tooltip.addPara(s, 0f);
    		}
    	}
    }
    
    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
    	float op = fighter.getWing().getSpec().getOpCost(ship.getMutableStats());
    	if(op > 12) {    		
    		fighter.getMutableStats().getEngineMalfunctionChance().modifyFlat(id + "fighter", op/100f);
    		fighter.getMutableStats().getCriticalMalfunctionChance().modifyFlat(id + "fighter", 0.1f);
    	}
    	if (!ship.getVariant().getHullMods().contains("SKR_remote")) {
        	if(fighter.getHullSpec().getMinCrew() > 0) {
        		fighter.getMutableStats().getShieldMalfunctionChance().modifyFlat(id + "fighter", 0.5f);
        		fighter.getMutableStats().getWeaponMalfunctionChance().modifyFlat(id + "fighter", 0.5f);
        		fighter.getMutableStats().getCriticalMalfunctionChance().modifyFlat(id + "fighter", 0.2f);
        	}
    	}
    	super.applyEffectsToFighterSpawnedByShip(fighter, ship, id);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}



