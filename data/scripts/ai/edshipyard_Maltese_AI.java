package data.scripts.ai;

import data.scripts.util.MagicLensFlare;
import data.scripts.util.StolenUtils;

import java.awt.Color;
import java.util.Iterator;
import java.util.Random;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.Point;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.IntervalUtil;

//based on code from Sundog's ICE repair drone and Dark.Revenant's Imperium Titan

public class edshipyard_Maltese_AI extends BaseShipAI {
	private final ShipwideAIFlags flags = new ShipwideAIFlags();
    private final ShipAIConfig config = new ShipAIConfig();
    private ShipAPI carrier;
    ShipAPI target;
    Vector2f targetOffset;
    Point cellToFix = new Point();
    Random rng = new Random();
    ArmorGridAPI armorGrid;
    SoundAPI repairSound;
    float max, cellSize;
    int gridWidth, gridHeight, cellCount;
    boolean doingMx = false;
    boolean returning = false;
    boolean spark = false;
    float dontRestoreAmmoUntil = 0;
    float targetFacingOffset = Float.MIN_VALUE;
    float range = 4000f;
    
    private final IntervalUtil interval = new IntervalUtil(0.25f, 0.33f);
    private final IntervalUtil countdown = new IntervalUtil(4f, 4f);
    
    Vector2f getDestination() {
        return new Vector2f();
    }

    private final float REPAIR_RANGE = 25f;
    private final float REPAIR_HULL = 1f;
    private final float REPAIR_ARMOR = 0.05f;
    private final float FLUX_PER_MX_PERFORMED = 1f;

    private final Color SPARK_COLOR = new Color(255, 225, 150, 100);
    //private final Color SPARK_COLOR_CORE = new Color(255, 175, 25, 100);
    private final String SPARK_SOUND_ID = "ed_shock";
    //private final float SPARK_DURATION = 0.2f;
    //private final float SPARK_BRIGHTNESS = 1.0f;
    //private final float SPARK_MAX_RADIUS = 7f;
    private final float SPARK_CHANCE = 0.67f;
    private final float SPARK_SPEED_MULTIPLIER = 500.0f;
    private final float SPARK_VOLUME = 1.0f;
    private final float SPARK_PITCH = 1.0f;

    public edshipyard_Maltese_AI(ShipAPI ship) {
		super(ship);		
	}

    @Override
    public void advance(float amount) {
    	
    	if(carrier == null) {
    		init();
    		//if(carrier == null){
    			//delete
    		//}
    	}
    	
    	if(ship.isLanding()) {
    		countdown.advance(amount);
    		if (countdown.intervalElapsed()) {
    			ship.getWing().getSource().land(ship);
    			return;
    		}
    	}
    	
    	interval.advance(amount);
        if (interval.intervalElapsed()) {
        	super.advance(amount);
            
            if(target == null) return;
            
            if (doingMx) {
            	repairArmor();
            } else if(returning && !ship.isLanding() && MathUtils.getDistance(ship, carrier) < carrier.getCollisionRadius()/3f) {
            	ship.beginLandingAnimation(carrier);
            }
            
        }
        goToDestination();
        
    }
    @Override
    public boolean needsRefit() {
        return ship.getFluxTracker().getFluxLevel() >= 1f;
    }

    @Override
    public void cancelCurrentManeuver() {
    }

    @Override
    public void evaluateCircumstances() {
        if(carrier == null || !carrier.isAlive()) {
        	StolenUtils.destroy(ship);
        	return;
        }

        setTarget(chooseTarget());

        if(returning) {
            targetOffset = StolenUtils.toRelative(target, ship.getWing().getSource().getLandingLocation(ship));
        } else {
        	do {
                 targetOffset = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
            } while(!CollisionUtils.isPointWithinBounds(targetOffset, target));
             
            targetOffset = StolenUtils.toRelative(target, targetOffset);
             
            armorGrid = target.getArmorGrid();
            max = armorGrid.getMaxArmorInCell();
            cellSize = armorGrid.getCellSize();
            gridWidth = armorGrid.getGrid().length;
            gridHeight = armorGrid.getGrid()[0].length;
            cellCount = gridWidth * gridHeight;      	
        }

        if ((target.getPhaseCloak() == null || !target.getPhaseCloak().isOn()) && !returning && (StolenUtils.getArmorPercent(target) < 1f) && MathUtils.getDistance(ship, target) < REPAIR_RANGE) {
            performMaintenance();
        } else {
            doingMx = false;
        }
    }
    
    void performMaintenance() {
    	if(gridWidth <= 0 || gridHeight <= 0) {
    		return;//failsafe
    	}
        for(int i = 0; i < (1 + cellCount / 5); ++i) {
            cellToFix.setX(rng.nextInt(gridWidth));
            cellToFix.setY(rng.nextInt(gridHeight));

            if(armorGrid.getArmorValue(cellToFix.getX(), cellToFix.getY()) < max) break;
        }

        ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getCurrFlux() + FLUX_PER_MX_PERFORMED);

        doingMx = true;
    }
    
    void repairArmor() {    	
        if(cellToFix == null) return;
        ShieldAPI shield = target.getShield();
        if(shield != null && shield.isOn()) {
        	if(rng.nextFloat() > shield.getFluxPerPointOfDamage()/2f) {
        		spark = false;
        		return;//repair failed due to shield
        	}
        }
        spark = true;        
        
        if(target.getHullLevel() < 1f) {//hull first then armor
        	float bonus = 0f;
        	if(target.getCurrentCR() == target.getCRAtDeployment()) {//peak performance?
        		if(target.getHullSize() == HullSize.CAPITAL_SHIP) {
        			bonus = 5;
        		}else if(target.getHullSize() == HullSize.CRUISER) {
        			bonus = 4;
        		}else if(target.getHullSize() == HullSize.DESTROYER) {
        			bonus = 3;
        		}else {
        			bonus = 2;
        		}
        	}
        	target.setHitpoints(target.getHitpoints() + REPAIR_HULL + bonus);
        }else {
        	for(int x = cellToFix.getX() - 1; x <= cellToFix.getX() + 1; ++ x) {
                if(x < 0 || x >= gridWidth) continue;

                for(int y = cellToFix.getY() - 1; y <= cellToFix.getY() + 1; ++ y) {
                    if(y < 0 || y >= gridHeight) continue;
                    
                    armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y) + REPAIR_ARMOR));
                }
            }
        }
    }
    
    ShipAPI chooseTarget() {
        if(needsRefit()) {
            returning = true;
            //ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getMaxFlux());
            return carrier;
        } else returning = false;
        
        if(carrier.getShipTarget() != null
                && carrier.getOwner() == carrier.getShipTarget().getOwner()
                && !carrier.getShipTarget().isDrone()
                && !carrier.getShipTarget().isFighter()) {
            return carrier.getShipTarget();
        }
        
        ShipAPI mostWounded = carrier;
        float mostDamage = 4f;
        
        for(Iterator<ShipAPI> iter = Global.getCombatEngine().getShips().iterator(); iter.hasNext(); ) {
            ShipAPI s = (ShipAPI)iter.next();
            float d = MathUtils.getDistance(carrier, s);
            if(!(s.isFighter() || s.isDrone() || s.isHulk() || d > range)) {
            	float currDamage = StolenUtils.getArmorPercent(s) + s.getHullLevel();
            	float priority =  d/1000f;
            	if(s.isStationModule()) {
            		priority++;
            	}
                if(s.getOwner() == carrier.getOwner() && currDamage < 1.98f) {
                    if(mostDamage > (currDamage+(priority/2f))) {
                    	mostDamage = (currDamage+(priority/2f));
                    	mostWounded = s;
                    }
                }
            }            
        }
        
        return mostWounded;        
    }
    
    void setTarget(ShipAPI t) {
        if(target == t) return;
        target = t;
        this.ship.setShipTarget(t);
    }
    
    void goToDestination() {
        Vector2f to = StolenUtils.toAbsolute(target, targetOffset);
        float distance = MathUtils.getDistance(ship, to);
        
        if(doingMx) {
            if(distance < 100) {
                float f = (1 - distance / 100) * 0.2f;
                ship.getLocation().x = (to.x * f + ship.getLocation().x * (2 - f)) / 2;
                ship.getLocation().y = (to.y * f + ship.getLocation().y * (2 - f)) / 2;
                ship.getVelocity().x = (target.getVelocity().x * f + ship.getVelocity().x * (2 - f)) / 2;
                ship.getVelocity().y = (target.getVelocity().y * f + ship.getVelocity().y * (2 - f)) / 2;
            }
        }
        
        if(doingMx && distance < 25) {
        	if(spark) {
        		Global.getSoundPlayer().playLoop(SPARK_SOUND_ID, ship, SPARK_PITCH,
                        SPARK_VOLUME, ship.getLocation(), ship.getVelocity());

                if(targetFacingOffset == Float.MIN_VALUE) {
                    targetFacingOffset = ship.getFacing() - target.getFacing();
                } else {
                    ship.setFacing(MathUtils.clampAngle(targetFacingOffset + target.getFacing()));
                }
                
                if(Math.random() < SPARK_CHANCE) {
                    Vector2f loc = new Vector2f(ship.getLocation());
                    loc.x += cellSize * 0.5f - cellSize * (float) Math.random();
                    loc.y += cellSize * 0.5f - cellSize * (float) Math.random();

                    Vector2f vel = new Vector2f(ship.getVelocity());
                    vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
                    vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

                    MagicLensFlare.createSharpFlare(
                    		Global.getCombatEngine(),
                            ship,
                            loc,
                            5,
                            100,
                            0,
                            SPARK_COLOR,
                            Color.white
                    );
                }
        	}
        	spark = false;            
        } else {        	
        	float distToCarrier = (float)(MathUtils.getDistanceSquared(carrier.getLocation(),ship.getLocation()) / Math.pow(target.getCollisionRadius(),2));
        	if(target == carrier && distToCarrier < 1.0f || ship.isLanding() == true) {
                float f= 1.0f-Math.min(1,distToCarrier);
                if(returning == false) f = f*0.1f;
                turnToward(target.getFacing());
                ship.getLocation().x = (to.x * (f*0.1f) + ship.getLocation().x * (2 - f*0.1f)) / 2;
                ship.getLocation().y = (to.y * (f*0.1f) + ship.getLocation().y * (2 - f*0.1f)) / 2;
                ship.getVelocity().x = (target.getVelocity().x * f + ship.getVelocity().x * (2 - f)) / 2;
                ship.getVelocity().y = (target.getVelocity().y * f + ship.getVelocity().y * (2 - f)) / 2;
            }else {
            	targetFacingOffset = Float.MIN_VALUE;
                float angleDif = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), to));

                if(Math.abs(angleDif) < 30){
                    accelerate();
                } else {
                    turnToward(to);
                    decelerate();
                }        
                strafeToward(to);
            }            
        }
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return flags;
    }

    @Override
    public void setDoNotFireDelay(float amount) {
    }

    @Override
    public ShipAIConfig getConfig() {
        return config;
    }
    
    public void init() {
    	carrier = ship.getWing().getSourceShip();
    	target = carrier;
    	targetOffset = StolenUtils.toRelative(carrier, carrier.getLocation());
    	range = ship.getWing().getRange();
    	
    }
}
