package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import data.scripts.util.MagicLensFlare;

import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class Illusion extends BaseHullMod {

	public final Color JITTER_COLOR = new Color(125, 50, 150, 150);
	private static final String ILLUSIONKEY = "IllusionDamageReduction";
	
	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//stats.getSensorProfile().modifyMult(id, 1f - PROFILE_DECREASE * 0.01f);
		stats.getSensorProfile().modifyMult(id, 0f);
	}

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
    	
    	if(ship == null) {
    		return;
    	}

        float radius = ship.getCollisionRadius();
    	
    	List<ShipAPI> ships = CombatUtils.getShipsWithinRange(ship.getLocation(),radius+100);
    	for(ShipAPI s : ships) {    		
    		if(s != null && s.isDrone() && s.getOwner() == ship.getOwner() && (s.getHullSpec().getBaseHullId().equals("edshipyard_chihuahua_doppel") || s.getHullSpec().getBaseHullId().equals("edshipyard_shiba_doppel"))) {
    			MutableShipStatsAPI stats = s.getMutableStats();
    			if(stats.getHullDamageTakenMult().getMultMods().containsKey(ILLUSIONKEY)) {
    				continue;
    			}
    			float angle = ship.getFacing();		
	    		Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(),MathUtils.getRandomNumberInRange(radius*3f +300, radius*5f +400), MathUtils.getRandomNumberInRange(angle +270f, angle +90f));
	    		ship.setJitter(ship, JITTER_COLOR, 1, 2, ship.getCollisionRadius()*1.5f);
	            s.getLocation().set(point);
	            ship.setJitter(s, JITTER_COLOR, 1, 15, ship.getCollisionRadius()*2.5f);
	            s.setFacing(angle);
	            stats.getHullDamageTakenMult().modifyMult(ILLUSIONKEY, 0.01f);
	            MagicLensFlare.createSmoothFlare(
                        Global.getCombatEngine(),
                        ship,
                        point,
                        50,
                        (radius+100)*2,
                        0,
                        new Color(250,150,255,128),
                        Color.white
                );
	        }
    	}
    	
    	super.advanceInCombat(ship, amount);
    }
}
