package data.scripts.campaign.intel.bar.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;

import java.util.List;

public class WurgBarEvent extends BaseBarEventWithPerson {

	// the stages of the dialogue
	public static enum OptionId {
		INIT,
        BUY_WURG,
        LEAVE
	}
	
	public final int COST = 25000000;
	
	public WurgBarEvent() {
		super();
	}

	// where the bar event will show up
	public boolean shouldShowAtMarket(MarketAPI market) {
		if (!super.shouldShowAtMarket(market)) return false;

		boolean hasEDMarket = false;
		
		List<SubmarketAPI> submarkets = market.getSubmarketsCopy();
		for(SubmarketAPI sbm : submarkets) {
			if (sbm.getSpecId().equals("ed_shipyard")){
				hasEDMarket = true;
				break;
			}
		}

		if (!market.getFactionId().equals(Factions.INDEPENDENT) || !hasEDMarket) {
			return false;
		}
		
		if	(market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER)) != RepLevel.COOPERATIVE) {
			return false;
		}
		
		MutableValue purse = Global.getSector().getPlayerFleet().getCargo().getCredits();
		
		if (COST > purse.get()) {
			return false;
		}

		if (Global.getSector().getPlayerStats().getLevel() < 50 && !DebugFlags.BAR_DEBUG) return false;
		
		return true;
	}

	/**
     * Set up the text that appears when the player goes to the bar
     * and the option for them to start the conversation.
     */
    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog) {
        // Calling super does nothing in this case, but is good practice because a subclass should
        // implement all functionality of the superclass (and usually more)
        super.addPromptAndOption(dialog);
        regen(dialog.getInteractionTarget().getMarket()); // Sets field variables and creates a random person

        // Display the text that will appear when the player first enters the bar and looks around
        dialog.getTextPanel().addPara("An Eccentric Designs representative waves at you confidently, seems like " +
                "they have been expecting you.");

        // Display the option that lets the player choose to investigate our bar event
        dialog.getOptionPanel().addOption("See what kind of deal the representative has for you.", this);
    }

    /**
     * Called when the player chooses this event from the list of options shown when they enter the bar.
     */
    @Override
    public void init(InteractionDialogAPI dialog) {
        super.init(dialog);
        
        // If player starts our event, then backs out of it, `done` will be set to true.
        // If they then start the event again without leaving the bar, we should reset `done` to false.
        done = false;

        // The boolean is for whether to show only minimal person information. True == minimal
        dialog.getVisualPanel().showPersonInfo(person, true);

        // Launch into our event by triggering the "INIT" option, which will call `optionSelected()`
        this.optionSelected(null, OptionId.INIT);
    }

    /**
     * This method is called when the player has selected some option for our bar event.
     *
     * @param optionText the actual text that was displayed on the selected option
     * @param optionData the value used to uniquely identify the option
     */
    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData instanceof OptionId) {
            // Clear shown options before we show new ones
            dialog.getOptionPanel().clearOptions();
            FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "edshipyard_wurgandal_worldeater");
            MutableValue purse = Global.getSector().getPlayerFleet().getCargo().getCredits();
            // Handle all possible options the player can choose
            switch ((OptionId) optionData) {
                case INIT:
                    // The player has chosen to walk over to the crowd, so let's tell them what happens.
                    dialog.getTextPanel().addPara("You walk over and see that the " + getManOrWoman() +
                            " is offering. You feel like it is going to be something big.");
                    dialog.getTextPanel().addPara("\"You have been making waves, the higher ups got their eyes on you, they sent " +
                            "me to offer you an exclusive deal. We have a produrct we only offer to special clients. " +
                    		"It is called the Wurgandal, here, you can have a look at the specs.\"");
                    String desc = "\"It can be yours for just "+ Misc.getDGSCredits(COST)+" \"";
                    dialog.getTextPanel().addPara(desc, Misc.getTextColor(), Misc.getHighlightColor(), Misc.getDGSCredits(COST));
                    
                    dialog.getVisualPanel().showFleetMemberInfo(ship, true);

                    // And give them some options on what to do next
                    dialog.getOptionPanel().addOption("Pay 25.000.000 credits for the Wurgandal", OptionId.BUY_WURG);
                    dialog.getOptionPanel().addOption("Not right now, thanks", OptionId.LEAVE);
                    
                    if (COST > purse.get()) {
                    	dialog.getOptionPanel().setEnabled(OptionId.BUY_WURG, false);
                    	dialog.getOptionPanel().setTooltip(OptionId.BUY_WURG, "You don't have enough credits.");
					}
                    break;
                case BUY_WURG:
                	purse.subtract(COST);
                	AddRemoveCommodity.addCreditsLossText((int) COST, dialog.getTextPanel());
                	Global.getSector().getPlayerFleet().getFleetData().addFleetMember(ship);
                    dialog.getTextPanel().addPara("Congratulations, you are the proud owner of a planet killer dreadnought!");
                    dialog.getOptionPanel().addOption("Leave", OptionId.LEAVE);
                    // Removes this event from the bar so it isn't offered again
                    BarEventManager.getInstance().notifyWasInteractedWith(this);
                    break;
                case LEAVE:
                    // They've chosen to leave, so end our interaction. This will send them back to the bar.
                    // If noContinue is false, then there will be an additional "Continue" option shown
                    // before they are returned to the bar. We don't need that.
                    noContinue = true;
                    done = true;

                    
                    
                    break;
            }
        }
    }	
}


