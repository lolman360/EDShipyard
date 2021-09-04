package data.scripts.ai;

import data.scripts.util.StolenUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.LinkedList;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.util.IntervalUtil;

//import data.scripts.util.MagicRender;
//import java.awt.Color;

//based on code from Sundog's ICE repair drone and Dark.Revenant's Imperium Titan, Special thanks to WASP103

public class edshipyard_Terrier_AI extends BaseShipAI {
	private final ShipwideAIFlags flags = new ShipwideAIFlags();
    private final ShipAIConfig config = new ShipAIConfig();
    private ShipAPI carrier;
    ShipAPI target;
    boolean targetHasChanged;
    Vector2f shipLastVelocity = new Vector2f();
    Vector2f targetLastVelocity = new Vector2f();
    WeaponAPI weapon;
    int reloadAmount;
    Vector2f targetOffset;
    Random rng = new Random();
    float distToCarrier;
    
    boolean firstRun = true;
    boolean doingMx = false;
    boolean returning = false;
    float targetFacingOffset = Float.MIN_VALUE;
    float range = 4000f;
    float origMass;

    WeaponAPI arms;
    int armsFrame = 0;

    private final IntervalUtil interval = new IntervalUtil(0.25f, 0.33f);
    private final IntervalUtil countdown = new IntervalUtil(4f, 4f);
    
    Vector2f getDestination() {
        return new Vector2f();
    }

    private final float AMMO_RESTORE_SMALL = 1f;
    private final float AMMO_RESTORE_MEDIUM = 0.25f;
    private final float AMMO_RESTORE_LARGE = 0.1f;
    private final float RELOAD_RANGE = 10f;

    public edshipyard_Terrier_AI(ShipAPI ship) {
		super(ship);
                origMass = ship.getMass();
	}

    @Override
    public void advance(float amount) {

    	if(carrier == null) {
    		init();
    	}

    	if(ship.isLanding()) {
    		countdown.advance(amount);
    		if (countdown.intervalElapsed()) {
    			ship.getWing().getSource().land(ship);
    			return;
    		}
    	}

    	if(needsRefit()) {
    		arms.getAnimation().setFrame(15);
    	}else {
    		arms.getAnimation().setFrame(Math.min(15,armsFrame));
    	}

    	interval.advance(amount);
        distToCarrier = (float)(MathUtils.getDistanceSquared(carrier.getLocation(),ship.getLocation()) / Math.pow(target.getCollisionRadius(),2));
            
        if (interval.intervalElapsed()) {
        	super.advance(amount);
            if(returning && !ship.isLanding() && distToCarrier < 1.0f) {
            	ship.beginLandingAnimation(carrier);
            }
        }
        if(target == null) return;
        
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
        
 
        if(targetHasChanged == true || firstRun == true || (target == carrier && returning == false)){
            
            if(target == carrier) {
                targetOffset = ship.getWing().getSource().getLandingLocation(ship);
                if(returning == false) {
                   targetOffset = randomizePointOnEntity(targetOffset,target.getCollisionRadius()/2,target);
                } 
                targetOffset = StolenUtils.toRelative(target, targetOffset);
            } else {
                targetOffset = StolenUtils.toRelative(target, 
                        randomizePointOnEntity(target.getLocation(),target.getCollisionRadius(),target)
                );
            }
            firstRun = false;
        }
        
        if ((target.getPhaseCloak() == null || !target.getPhaseCloak().isOn()) && !returning && !doingMx && MathUtils.getDistance(ship, target) < RELOAD_RANGE) {
        	restoreAmmo();
        }
        
    }
    
    Vector2f randomizePointOnEntity(Vector2f pointToRand, float radius, CombatEntityAPI entity) {
        Vector2f retVar;
        do {
            retVar = MathUtils.getRandomPointInCircle(pointToRand, radius);
        } while(!CollisionUtils.isPointWithinBounds(retVar, entity));

        return retVar;
    }
    
    Vector2f randomizePointOffEntity(Vector2f pointToRand, float radius, CombatEntityAPI entity) {
        Vector2f retVar;
        do {
            retVar = MathUtils.getRandomPointInCircle(pointToRand, radius);
        } while(CollisionUtils.isPointWithinBounds(retVar, entity));

        return retVar;
    }
    
    void restoreAmmo() {
    	if(weapon != null && weapon.getAmmo() < weapon.getMaxAmmo()) {
    		weapon.setAmmo(Math.min(weapon.getAmmo() + reloadAmount, weapon.getMaxAmmo()));
    		doingMx = true;
    	}else {
    		weapon = null;
                WeaponAPI tempW = null;
    		List<WeaponAPI> weapons = target.getAllWeapons();
        	for(WeaponAPI w : weapons) {
        		if(w.usesAmmo() && w.getAmmo() < w.getMaxAmmo() && !(w.getSlot().isDecorative() || w.getSlot().isBuiltIn())) {
        			if(w.getSlot().getSlotSize() == WeaponSize.LARGE) {
        				reloadAmount = (int)(w.getMaxAmmo()*AMMO_RESTORE_LARGE);
        			}else if(w.getSlot().getSlotSize() == WeaponSize.MEDIUM) {
        				reloadAmount = (int)(w.getMaxAmmo()*AMMO_RESTORE_MEDIUM);
        			}else {
        				reloadAmount = (int)(w.getMaxAmmo()*AMMO_RESTORE_SMALL);
        			}
        			if(reloadAmount > 0) {
                                        if(w.getAmmoPerSecond() == 0 && w.getAmmo() + reloadAmount <= w.getMaxAmmo()) {
                                            weapon = w;
                                            break;
        				}else {
                                            if(tempW == null) {
                                                tempW = w;
                                            } else {
                                                if(tempW.getAmmo()/tempW.getMaxAmmo() > w.getAmmo()/w.getMaxAmmo()) {
                                                    tempW = w;
                                                }
                                            }
        				}
        			}
        		}
        	}
                weapon = tempW;
        	if(weapon == null) {
        		setTarget(chooseTarget());
        	}
    	}
    }

    ShipAPI chooseTarget() {
        if(needsRefit()) {
            returning = true;
            return carrier;
        } else returning = false;

        if(carrier.getShipTarget() != null
                && carrier.getOwner() == carrier.getShipTarget().getOwner()
                && !carrier.getShipTarget().isDrone()
                && !carrier.getShipTarget().isFighter()) {
            return carrier.getShipTarget();
        }

        weapon = null;
        reloadAmount = 0;
        LinkedList<ShipAPI> finalList = new LinkedList<ShipAPI>();

        for(Iterator<ShipAPI> iter = Global.getCombatEngine().getShips().iterator(); iter.hasNext();) {
            ShipAPI s = (ShipAPI)iter.next();
            float d = MathUtils.getDistance(carrier, s);
            if(!(s.isFighter() || s.isDrone() || s.isHulk() || d > range) && s.getOwner() == carrier.getOwner()) {
            	ShipAPI mainbodys = s;
            	ShipAPI mainbodyc = carrier;
            	if(s.isStationModule()) {
            		mainbodys = s.getParentStation();
            	}
            	if(carrier.isStationModule()) {
            		mainbodyc = carrier.getParentStation();
            	}
            	if(mainbodys != mainbodyc) {
            		if(finalList.isEmpty()) {
                		finalList.add(s);
                	}else {
                		int i = 0;
                		boolean added = false;
                		for(ShipAPI ss : finalList){
                			float dd = MathUtils.getDistance(carrier, ss);
                			if(d < dd) {
                				finalList.add(i, s);
                				added = true;
                				break;
                			}
                			i++;
                		}
                		if(!added) {
                			finalList.add(s);
                		}
                	}
            	}
            }
        }


        WeaponAPI tempW = null;
        ShipAPI tempS = null;
        for(ShipAPI s : finalList) {//now runs through the list from closest to furthest checkling for reloadable guns to reload that cannot regenerate ammo by themselves
        	List<WeaponAPI> weapons = s.getAllWeapons();
        	for(WeaponAPI w : weapons) {
        		if(w.usesAmmo() && w.getAmmo() < w.getMaxAmmo() && !(w.getSlot().isDecorative() || w.getSlot().isBuiltIn())) {
        			if(w.getSlot().getSlotSize() == WeaponSize.LARGE) {
        				reloadAmount = (int)(w.getMaxAmmo()*AMMO_RESTORE_LARGE);
        			}else if(w.getSlot().getSlotSize() == WeaponSize.MEDIUM) {
        				reloadAmount = (int)(w.getMaxAmmo()*AMMO_RESTORE_MEDIUM);
        			}else {
        				reloadAmount = (int)(w.getMaxAmmo()*AMMO_RESTORE_SMALL);
        			}
        			if(reloadAmount > 0) {
                                        if(w.getAmmoPerSecond() == 0 && w.getAmmo() + reloadAmount <= w.getMaxAmmo()) {
                                            weapon = w;
                                            return s;
        				}else {
                                            if(tempW == null) {
                                                tempW = w;
                                            } else {
                                                if(tempW.getAmmo()/tempW.getMaxAmmo() > w.getAmmo()/w.getMaxAmmo()) {
                                                    tempW = w;
                                                }
                                            }
                                            tempS = s;
        				}
        				
        			}
        		}                        
        	}
                if(tempS != null) break;
        }

        weapon = tempW;
        if(tempS == null) tempS = carrier;
        return tempS;
    }

    void setTarget(ShipAPI t) {
        if(target == t) {
            targetHasChanged = false;
            return;
        }
        targetHasChanged = true;
        target = t;
        this.ship.setShipTarget(t);
    }

    void goToDestination() {
        Vector2f to = StolenUtils.toAbsolute(target, targetOffset); 
        Vector2f toEst = new Vector2f(to);
        if(doingMx) {
        	if(arms != null) {
        		armsFrame++;
        		if(armsFrame > 15) {
        			returning = true;
                                ship.setMass(origMass *0.20f);
        			doingMx = false;
            		ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getMaxFlux());
            		Global.getSoundPlayer().playSound("reloaddrone", MathUtils.getRandomNumberInRange(0.95f, 1.05f) * 1f,1f, ship.getLocation(), ship.getVelocity());
        		}
        	}
        } else {
            float mySpeed = (float)Math.max(1,Math.sqrt(ship.getVelocity().lengthSquared()));
            float distToTarget = MathUtils.getDistance(to, ship.getLocation());
            float estTimeTilHit = distToTarget / mySpeed;
            if(target == carrier && distToCarrier < 1.0f || ship.isLanding() == true) {
                float f= 1.0f-Math.min(1,distToCarrier);
                if(returning == false) f = f*0.1f;
                turnToward(target.getFacing());
                ship.getLocation().x = (to.x * (f*0.1f) + ship.getLocation().x * (2 - f*0.1f)) / 2;
                ship.getLocation().y = (to.y * (f*0.1f) + ship.getLocation().y * (2 - f*0.1f)) / 2;
                ship.getVelocity().x = (target.getVelocity().x * f + ship.getVelocity().x * (2 - f)) / 2;
                ship.getVelocity().y = (target.getVelocity().y * f + ship.getVelocity().y * (2 - f)) / 2;
            } else {
                //have to copy the vectors because scale() would otherwise mess up the ship's velocity.
                Vector2f vTarget = (Vector2f)new Vector2f(target.getVelocity().x + (target.getVelocity().x - targetLastVelocity.x),
                        target.getVelocity().y + (target.getVelocity().y - targetLastVelocity.y));
                Vector2f vShip = (Vector2f)new Vector2f(ship.getVelocity().x + (ship.getVelocity().x - shipLastVelocity.x) ,
                        ship.getVelocity().y + (ship.getVelocity().y - shipLastVelocity.y));
                
                Vector2f estTargetPosChange = (Vector2f)new Vector2f(vTarget).scale(estTimeTilHit);
                Vector2f.add(to, estTargetPosChange, toEst);

                distToTarget = MathUtils.getDistance(toEst, ship.getLocation());
                estTimeTilHit = distToTarget / mySpeed;
                estTargetPosChange = (Vector2f)new Vector2f(vTarget).scale(estTimeTilHit*0.9f);
                Vector2f.sub(estTargetPosChange,(Vector2f)new Vector2f(vShip).scale(estTimeTilHit*0.9f), estTargetPosChange);
                Vector2f.add(to, estTargetPosChange, toEst);
                //if target flies towards us fast, try to converge directly instead.
                float angleDifToEst = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),to), VectorUtils.getAngle(ship.getLocation(), toEst)));
                if(angleDifToEst < 140) {
                    to = toEst;
                }
                /*                
                MagicRender.singleframe(
                    Global.getSettings().getSprite("graphics/warroom/waypoint.png"),
                    to,
                    new Vector2f(8.0f, 8.0f),
                    0.0f,
                    Color.RED,
                    false
                );*/
                 Vector2f velocityVector = new Vector2f(ship.getLocation());
                 Vector2f.add(velocityVector, vShip, velocityVector);
                float angleDif = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),velocityVector), VectorUtils.getAngle(ship.getLocation(), to)));
                if(angleDif > 90 && mySpeed > 60) {
                    decelerate();
                } else {   
                    accelerate(); 
                }
                 if(angleDif > 10){ 
                    turnToward(to);
                }
                strafeToward(to);
            }//*/
            shipLastVelocity = new Vector2f(ship.getVelocity());
            targetLastVelocity =  new Vector2f(target.getVelocity());
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

    @Override
    public void init() {
    	carrier = ship.getWing().getSourceShip();
    	target = carrier;
    	armsFrame = 0;
    	targetOffset = StolenUtils.toRelative(carrier, carrier.getLocation());
    	range = ship.getWing().getRange();
    	List<WeaponAPI> weapons = ship.getAllWeapons();
    	if(weapons != null && !weapons.isEmpty()) {
    		for(WeaponAPI w : weapons) {
    			if(w.getSlot().isDecorative()) {
    				arms = w;
    				return;
    			}
    		}
    	}
    }
}
