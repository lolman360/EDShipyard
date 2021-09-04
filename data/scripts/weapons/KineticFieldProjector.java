
package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class KineticFieldProjector implements OnHitEffectPlugin {
    
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {
        
        CombatUtils.applyForce(target, VectorUtils.getDirectionalVector(projectile.getLocation(), target.getLocation()), 300f);
        engine.addSmoothParticle(point,MathUtils.getPoint(new Vector2f(),150,VectorUtils.getAngle(target.getLocation(), point)),100,0.75f,0.5f,new Color(100,25,110));
    }
}
