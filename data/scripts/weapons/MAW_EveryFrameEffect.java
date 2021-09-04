package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;

import data.scripts.util.MagicFakeBeam;
import data.scripts.util.MagicLensFlare;
import data.scripts.util.MagicRender;
import java.awt.Color;
import java.util.Random;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class MAW_EveryFrameEffect implements EveryFrameWeaponEffectPlugin{
    
    private ShipAPI ship;
    private ShipAPI ShieldModuleL; 
    private ShipAPI ShieldModuleR;
    private ShipAPI WeaponModuleL; 
    private ShipAPI WeaponModuleR; 
    private ShipAPI HangarModuleL;  
    private ShipAPI HangarModuleR;  
    private ShipAPI EngineModuleB;
    
    public static final String wurg_SML = "edshipyard_wurg_jawleft";
    public static final String wurg_SMR = "edshipyard_wurg_jawright";
    public static final String wurg_WML = "edshipyard_wurg_weaponleft";
    public static final String wurg_WMR = "edshipyard_wurg_weaponright";
    public static final String wurg_HML = "edshipyard_wurg_hangarleft";
    public static final String wurg_HMR = "edshipyard_wurg_hangarright";
    public static final String wurg_BTC = "edshipyard_wurg_buttocks";
    
    private float timer;
    private float check;
    private float period;
    private float prevCharge;
    private boolean fire;
    private boolean disabled;
    private Random rnd;
    
    private String zapSprite = "edzap_";
    private String effectID = "Tyrant Maw Siege Mode";
    private MutableShipStatsAPI stats;
    
    private float energy;
    private WeaponAPI bar;
    
    @Override
    public void advance (float amount, CombatEngineAPI engine, WeaponAPI weapon) {
                
        if(engine.isPaused()) return;  
        
        if (ship == null) {
            ship=weapon.getShip();
            fire = false;
            check = 0;
            timer = 0;
            period = 0;
            energy = 0;
            disabled = false;
            stats = ship.getMutableStats();
            rnd = new Random();
            //get the weapon, all the sprites and sizes
            for (ShipAPI m : ship.getChildModulesCopy()) {
                switch(m.getHullSpec().getBaseHullId()) {                    
                    case wurg_SML:
                    	ShieldModuleL = m;
                        break;                        
                    case wurg_SMR:
                    	ShieldModuleR = m;
                        break;
                    case wurg_WML:
                    	WeaponModuleL = m;
                        break;
                    case wurg_WMR:
                    	WeaponModuleR = m;
                        break;
                    case wurg_HML:
                    	HangarModuleL = m;
                        break;
                    case wurg_HMR:
                    	HangarModuleR = m;
                        break;
                    case wurg_BTC:
                    	EngineModuleB = m;
                        break;
                }
            }
            weapon.setAmmo(0);
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.isDecorative() && w.getDisplayName().contains("Charge Bar")) {
                    bar = w;
                    break;
                }
            }
            return;
        }
        
        float charge=weapon.getChargeLevel();
        
        if(charge > 0 && charge < 1 && !fire) {
        	energy = 0;
        	spark(0, 0.05f + charge*charge*0.75f, weapon);
        	if(ship.getFluxTracker().isVenting()) {
        		interrupt(weapon);
        	}
        }
        
        if(disabled) {
        	bar.getAnimation().setFrame(48);
        	return;
        }
        
        bar.getAnimation().setAlphaMult(0.5f + charge*0.5f);
        bar.getAnimation().setFrame(48 - Math.min(48, Math.max(0, (int) energy)));
        
        check += amount;
        //check if all modules are still here, if not blow up the weapon
        if(check >= 0.5f) {
        	check -= 0.5f;
        	if(ShieldModuleL == null || ShieldModuleR == null || WeaponModuleL == null || WeaponModuleR == null || HangarModuleL == null || HangarModuleR == null || EngineModuleB == null ||
        	!ShieldModuleL.isAlive() || !ShieldModuleR.isAlive() || !WeaponModuleL.isAlive() || !WeaponModuleR.isAlive() || !HangarModuleL.isAlive() || !HangarModuleR.isAlive() || !EngineModuleB.isAlive()) {
        		disabled = true;
        		weapon.disable(true);        		
        		stats.getMaxSpeed().unmodify(effectID);
                stats.getMaxTurnRate().unmodify(effectID);
                stats.getTurnAcceleration().unmodify(effectID);
                energy = 0;
        		return;        		
        	}
        }
        
        if(charge==1){
            //Visual effect
        	energy = 0;
        	bar.getAnimation().setFrame(0);
        	Vector2f muzzle = new Vector2f(
                    MathUtils.getPoint(
                            weapon.getLocation(),
                            17,
                            weapon.getCurrAngle()
                    )
            );
            if(!fire) {
            	//shoot da woop
            	fire = true;
            	Global.getSoundPlayer().playSound("mawfire", 1f, 2f, ship.getLocation(), ship.getVelocity());
            	float range = weapon.getRange() * (1f - (stats.getEnergyWeaponRangeBonus().getPercentMod()/100f));
            	MagicFakeBeam.spawnFakeBeam(engine, weapon.getLocation(), Math.max(weapon.getRange(), range), ship.getFacing(), 200f, 0.1f, 0.9f, 150, new Color(255,225,255), new Color(100,0,150,125), 10000f, DamageType.ENERGY, 5000, ship);
            	MagicLensFlare.createSmoothFlare(
                        engine,
                        ship,
                        muzzle,
                        50,
                        900,
                        0,
                        new Color(250,150,255,128),
                        Color.white
                );
            }            
            period += amount;
            if(period >= 0.5f) {
        		period = 0;
        		 Global.getSoundPlayer().playSound("mawfireloop", 1f, 1.2f, ship.getLocation(), ship.getVelocity());
             	if(MagicRender.screenCheck(0.25f, weapon.getLocation())){
                     MagicLensFlare.createSharpFlare(
                             engine,
                             ship,
                             muzzle,
                             6,
                             600,
                             0,
                             new Color(250,150,255,128),
                             Color.white
                     );
                 }
        	}
        }else if(charge==0){
        	fire = false;
        	timer = 0;
        	
        	if(ship.getFluxTracker().isOverloaded()) {
    			energy = 0;
    			bar.getAnimation().setAlphaMult(0.5f + (float)Math.random()*0.4f);
    			bar.getAnimation().setFrame(Math.max(0,48-(int)Math.random()*100));
    			return;
    		}else {
    			period += amount;
            	if(period >= 1f) {
            		period = 0;
            		if(!ShieldModuleL.getFluxTracker().isOverloaded() && !ShieldModuleR.getFluxTracker().isOverloaded()) {
            			energy += ship.getFluxLevel()/2.5f;
            			if(energy >= 48 && weapon.getAmmo() == 0) {
            				Global.getSoundPlayer().playSound("ed_mag2", 0.7f, 1f, ship.getLocation(), ship.getVelocity());
                    		weapon.setAmmo(1);
                    	}
            		}        		
            	}
    		}
        	
        	if(energy > 48) {
        		bar.getAnimation().setAlphaMult(0.9f + (float)Math.random()*0.1f);
        	}
        }else if(charge > 0 && charge < 1 && !fire) {
        	energy = 0;
        	bar.getAnimation().setFrame(0);
        	timer += (charge -prevCharge)*20f;
        	
        	if(ship.getFluxTracker().isOverloaded()) {
        		interrupt(weapon);
        		return;
        	}else if(ship.getFluxLevel() > 0.999999f) {
        		ship.getFluxTracker().forceOverload(10f);
        		interrupt(weapon);
        		return;
        	}
        	
        	if(prevCharge == 0) {
        		spark(1f, charge, weapon);
        		Global.getSoundPlayer().playSound("ed_spark", 1f, 1f, ship.getLocation(), ship.getVelocity());
                ShieldModuleL.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_USE_FLUX, 30f);
                ShieldModuleR.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_USE_FLUX, 30f);
                WeaponModuleL.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_USE_FLUX, 30f);
                WeaponModuleR.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_USE_FLUX, 30f);
                EngineModuleB.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_USE_FLUX, 30f);
                ShieldModuleL.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 30f);
                ShieldModuleR.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 30f);
                WeaponModuleL.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 30f);
                WeaponModuleR.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 30f);
                HangarModuleL.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 30f);
                HangarModuleR.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 30f);
                EngineModuleB.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 30f);
                if(ship.getShipAI() != null && ship.getShipAI().getAIFlags() != null) {
                	ship.getShipAI().getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 30f);
                }
                stats.getMaxSpeed().modifyMult(effectID, 0);
                stats.getMaxTurnRate().modifyMult(effectID, 0);
                stats.getTurnAcceleration().modifyMult(effectID, 0);
        	}
        	
        	prevCharge = charge;
            
        	if(timer >= 1f){                    
                Global.getSoundPlayer().playSound("mawloop", 1f + charge*3, 1f, ship.getLocation(), ship.getVelocity());
                timer -= 1f;
                if(charge > 0.3f) {
                	Global.getSoundPlayer().playSound("mawfireloop", 1f, charge/2f, ship.getLocation(), ship.getVelocity());
                }                
                if(ShieldModuleL.getFluxTracker().isOverloaded() ||
                   ShieldModuleR.getFluxTracker().isOverloaded() ||
                   WeaponModuleL.getFluxTracker().isOverloaded() ||
                   WeaponModuleR.getFluxTracker().isOverloaded() ||
                   HangarModuleL.getFluxTracker().isOverloaded() ||
                   HangarModuleR.getFluxTracker().isOverloaded() ||
                   EngineModuleB.getFluxTracker().isOverloaded()) {
                   ship.getFluxTracker().forceOverload(10f);
                   ship.setCurrentCR(ship.getCurrentCR()*0.75f);
                   weapon.disable();
                   fire = true;
                   Global.getSoundPlayer().playSound("mawend", 1f, 1f, ship.getLocation(), ship.getVelocity());
                   stats.getMaxSpeed().unmodify(effectID);
                   stats.getMaxTurnRate().unmodify(effectID);
                   stats.getTurnAcceleration().unmodify(effectID);
                }
            }
        }else if(charge > 0 && charge < 1 && fire) {
        	energy = 0;
        	period = 0;
        	bar.getAnimation().setFrame(Math.max(0, 48 - (int)(charge*48)));
        	if(prevCharge > 0) {
        		prevCharge = 0;
                ShieldModuleL.getFluxTracker().forceOverload(3f);
                ShieldModuleR.getFluxTracker().forceOverload(3f);
                Global.getSoundPlayer().playSound("mawend", 1f, 1f, ship.getLocation(), ship.getVelocity());
                stats.getMaxSpeed().unmodify(effectID);
                stats.getMaxTurnRate().unmodify(effectID);
                stats.getTurnAcceleration().unmodify(effectID);
        	}        	
        }
    }
    
    public void interrupt(WeaponAPI weapon) {
    	ship.setCurrentCR(ship.getCurrentCR()*0.75f);
		Global.getSoundPlayer().playSound("mawend", 1f, 1f, ship.getLocation(), ship.getVelocity());
		stats.getMaxSpeed().unmodify(effectID);
        stats.getMaxTurnRate().unmodify(effectID);
        stats.getTurnAcceleration().unmodify(effectID);
        weapon.disable();
		fire = true;
    }
    
    public void spark(float bonusChance, float charge, WeaponAPI weapon) {
    	if(!MagicRender.screenCheck(0.25f, weapon.getLocation())){
    		return;
    	}
    	if(rnd.nextFloat() - bonusChance <= charge) {
        	Vector2f loc = new Vector2f(
                    MathUtils.getPoint(
                            weapon.getLocation(),
                            rnd.nextFloat()*240 + 40,
                            weapon.getCurrAngle()
                    )
            );
        	MagicRender.battlespace(                			
        	        Global.getSettings().getSprite("fx",zapSprite+rnd.nextInt(9)),
        	        loc,
        	        ship.getVelocity(),
        	        new Vector2f(24,24),
        	        new Vector2f(24,24),
        	        ship.getFacing() -90f +rnd.nextInt(2)*180f,
        	        (float)(Math.random()-0.5f)*10,
        	        new Color(255,200,255),
        	        true,
        	        0,
        	        0.1f,
        	        0.1f
        	);
        	Global.getSoundPlayer().playSound("ed_shock", 1f + charge, 1f, ship.getLocation(), ship.getVelocity());
        }    	
    }
}