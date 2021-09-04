package data.scripts.ai;

import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipwideAIFlags;

public class edshipyard_Dummy_AI implements ShipAIPlugin {

    private final ShipwideAIFlags AIFlages;

    public edshipyard_Dummy_AI() {
        AIFlages = new ShipwideAIFlags();
    }

    @Override
    public void setDoNotFireDelay(float amount) {
    }

    @Override
    public void forceCircumstanceEvaluation() {
    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return AIFlages;
    }

    @Override
    public void cancelCurrentManeuver() {        
    }

    @Override
    public ShipAIConfig getConfig() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
