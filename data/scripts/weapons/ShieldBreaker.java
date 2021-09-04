
package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;

import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class ShieldBreaker implements OnHitEffectPlugin {
    
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {        
    	if(target != null && target instanceof ShipAPI) {
    		FluxTrackerAPI flux = ((ShipAPI) target).getFluxTracker();
    		if(flux != null && (flux.getHardFlux() > 0.75f || flux.isOverloaded())) {
    			engine.applyDamage(target, point, 4 + ((ShipAPI) target).getFluxLevel()*6f, DamageType.ENERGY, 0, false, false, projectile.getSource());    	
    	        engine.addSmoothParticle(point,MathUtils.getPoint(new Vector2f(),150,VectorUtils.getAngle(target.getLocation(), point)),70,0.75f,0.5f,new Color(200,150,50));
    		}
    	}
    }
}
