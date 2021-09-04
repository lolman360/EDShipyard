
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class ClusterCannon implements OnHitEffectPlugin {
	
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {    	
    	float angle = VectorUtils.getAngle(target.getLocation(), projectile.getLocation());
    	Global.getSoundPlayer().playSound("bomb_bay_fire", 0.7f,0.7f, projectile.getLocation(), target.getVelocity());
    	for(int i = 0;i < 3 ;i++) {
    		float newangle = angle -30 + (float)(Math.random()*60f);
    		Vector2f spawn = MathUtils.getPoint(projectile.getLocation(), 60, newangle);
    		engine.spawnProjectile(projectile.getSource(), projectile.getWeapon(), "edshipyard_Fragbomb", spawn, newangle, new Vector2f());
    	}
    }
}
