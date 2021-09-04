
package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class RicochetPellet implements OnHitEffectPlugin {
    
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {        
    	
        if(Math.random() > 0.25f && !shieldHit) {
        	float angle = VectorUtils.getAngle(target.getLocation(), projectile.getLocation());
        	Vector2f spawn = MathUtils.getPoint(projectile.getLocation(), 30, angle);
        	engine.spawnProjectile(projectile.getSource(), projectile.getWeapon(), "edshipyard_SC0", spawn, angle , new Vector2f());
        }
    }
}
