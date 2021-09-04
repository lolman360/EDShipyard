package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class PDOnlyWeapons extends BaseHullMod {	
	
	public static final float PD_RANGE_BONUS = 100f;
	public static final float TURN_BONUS = 35f;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {        

        List<WeaponAPI> weapons = ship.getAllWeapons();
        ArrayList<WeaponAPI> weaponsToDisable = new ArrayList<WeaponAPI>();
        
        for (WeaponAPI weapon : weapons) {        	
        	
        	WeaponSlotAPI slot = weapon.getSlot();
        	if(slot != null) {
        		if(slot.isDecorative() || slot.isBuiltIn()) {
        			continue;
        		}
        	}
        	String role = weapon.getSpec().getPrimaryRoleStr();
        	if(role != null && role.length() > 0) {
        		if(role.contains("Point Defense") || role.contains("Anti Fighter") || role.contains("Anti Small Craft")) {
    				continue;
    			}
        	}
        	EnumSet<AIHints> hints = weapon.getSpec().getAIHints();
        	if(hints != null && hints.size() > 0) {
        		if(hints.contains(AIHints.PD) || hints.contains(AIHints.ANTI_FTR)) {
    				continue;
    			}
        	}
			
			weaponsToDisable.add(weapon);
		}
        
        /*for (WeaponAPI weapon : weaponsToDisable) {
        	
        	if (Global.getSector() != null) {
                if (Global.getSector().getPlayerFleet() != null) {
                    Global.getSector().getPlayerFleet().getCargo().addWeapons(weapon.getId(), 1);;
                }
            }
        	variant.clearSlot(weapon.getSlot().getId());
		}*/
        
        if (weaponsToDisable.size() > 0) {
        	ship.getMutableStats().getWeaponMalfunctionChance().modifyFlat(id, ((float)weaponsToDisable.size())/(ship.getAllWeapons().size()+2f));
        	ship.getMutableStats().getCriticalMalfunctionChance().modifyFlat(id, 0.1f);
        }else {
        	ship.getMutableStats().getWeaponMalfunctionChance().unmodify(id);
        	ship.getMutableStats().getCriticalMalfunctionChance().unmodify(id);
        }        
    }
    
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

    	if(ship == null) {
    		return;
    	}
    	List<WeaponAPI> weapons = ship.getAllWeapons();
        ArrayList<WeaponAPI> weaponsToDisable = new ArrayList<WeaponAPI>();
        
        for (WeaponAPI weapon : weapons) {        	
        	
        	WeaponSlotAPI slot = weapon.getSlot();
        	if(slot != null) {
        		if(slot.isDecorative() || slot.isBuiltIn()) {
        			continue;
        		}
        	}
        	String role = weapon.getSpec().getPrimaryRoleStr();
        	if(role != null && role.length() > 0) {
        		if(role.contains("Point Defense") || role.contains("Anti Fighter") || role.contains("Anti Small Craft")) {
    				continue;
    			}
        	}
        	EnumSet<AIHints> hints = weapon.getSpec().getAIHints();
        	if(hints != null && hints.size() > 0) {
        		if(hints.contains(AIHints.PD) || hints.contains(AIHints.ANTI_FTR)) {
    				continue;
    			}
        	}
			
			weaponsToDisable.add(weapon);
		}
    	
    	if(weaponsToDisable.size() > 0) {
    		LabelAPI label = tooltip.addPara("WARNING: You have equipped weapons that are neither anti-fighter nor point defense, this conflicts with this hullmod and may cause your weapons to malfuncion in combat.", 5f);
    		tooltip.addPara("Malfuncion chance: " + (int)(((float)weaponsToDisable.size())/(ship.getAllWeapons().size()+2f)*100) + "%", 5f);
            label.setHighlightColors(Color.RED);
            label.setHighlight("WARNING:");
    	}
    }
    
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamPDWeaponRangeBonus().modifyFlat(id, PD_RANGE_BONUS);
		stats.getBallisticWeaponRangeBonus().modifyFlat(id, PD_RANGE_BONUS);//no PD exclusive, but whatever you cannot use non-PD anyways
		stats.getMissileWeaponRangeBonus().modifyFlat(id, PD_RANGE_BONUS);//no PD exclusive, but whatever you cannot use non-PD anyways
		stats.getWeaponTurnRateBonus().modifyMult(id, 1f + TURN_BONUS * 0.01f);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) PD_RANGE_BONUS;
		if (index == 1) return "" + (int) TURN_BONUS + "%";
		return null;
	}

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}



