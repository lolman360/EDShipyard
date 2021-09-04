// spawn the bar event for A Promise
package data.scripts.campaign.intel.bar.events;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class WurgBarEventCreator extends BaseBarEventCreator {
	
	public PortsideBarEvent createBarEvent() {
		return new WurgBarEvent();
	}

	@Override
	public float getBarEventAcceptedTimeoutDuration() {
		return 10000000000f; // Alex took the easy way out, didn't he? well, we might as well do the same
	}

	@Override
	public float getBarEventFrequencyWeight() {
			return super.getBarEventFrequencyWeight();
	}



}
