
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;

import data.scripts.util.StolenUtils;

import java.awt.Color;
import java.util.List;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class Magnablast implements OnHitEffectPlugin {
	
	private final Color EMP_CORE_COLOR_STANDARD = new Color(255, 175, 255, 255);
	private final Color EMP_FRINGE_COLOR_STANDARD = new Color(255, 175, 255, 200);
    
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {    	
    	if(shieldHit) {
    		Global.getSoundPlayer().playSound("ed_mag4", 1f, 1, target.getLocation(), target.getVelocity());
        	float angle = VectorUtils.getAngle(target.getLocation(), projectile.getLocation());
        	for(int i = 0;i < 60;i++) {
        		Vector2f spawn = MathUtils.getPoint(projectile.getLocation(), 1000, angle -75 + (float)(Math.random()*150f));
        		List<ShipAPI> ships = StolenUtils.getShipsOnSegment(projectile.getLocation(), spawn);
        		for(ShipAPI s : ships) {
        			if(s != target && s.getOwner() == target.getOwner() && !s.isStationModule()) {
        				applyEffectToTarget(point, s, projectile,400);
        				i+=2;
        				break;
        			}
        		}
        	}
        	List<CombatEntityAPI> asteroids = CombatUtils.getAsteroidsWithinRange(projectile.getLocation(), 1400);
        	List<MissileAPI> missiles = CombatUtils.getMissilesWithinRange(projectile.getLocation(), 600);
        	for(CombatEntityAPI entity: asteroids) {
        		applyEffectToTarget(projectile.getLocation(), entity, projectile,800);
        	}
        	for(CombatEntityAPI entity: missiles) {
        		applyEffectToTarget(projectile.getLocation(), entity, projectile,100);
        	}
        }
    }
    
    protected void applyEffectToTarget(final Vector2f point, final CombatEntityAPI target, final DamagingProjectileAPI proj, float force) {
        Global.getCombatEngine().spawnEmpArc(proj.getWeapon().getShip(), point, proj, target, DamageType.ENERGY, 0, 100f, 1500, null, 5f, EMP_FRINGE_COLOR_STANDARD, EMP_CORE_COLOR_STANDARD);
        CombatUtils.applyForce(target, VectorUtils.getDirectionalVector(target.getLocation(), point), force);
    }
}
