package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.post.PostProcessShader;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class RelativityDriveStats extends BaseShipSystemScript {

    public final float TIME_MULT_IN_START = 0.75f;
    public final float TIME_MULT_IN_END = 0.25f;
    public final float TIME_MULT_ACTIVE_START = 25f;
    public final float TIME_MULT_ACTIVE_END = 1f;

    private static final Vector2f ZERO = new Vector2f();

    private final IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);
    private boolean started = false;
    private boolean fired = false;
    private WaveDistortion wave = null;
    private SoundAPI activateSound = null;

    private static final String postProcessKey = "relativitydrive_pp";
    
    //code based on Celerity Drive system by Dark Revenant (Interstellar Imperium)
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }
        ShipAPI ship = (ShipAPI) stats.getEntity();

        Color JITTER_COLOR = new Color(255, 75, 215, 100);
        Color JITTER_UNDER_COLOR = new Color(255, 75, 225, 200);
        Color AFTERIMAGE_COLOR = new Color(255, 75, 135);
        float afterImageSpread = 0.5f;
        float afterImageJitter = 0.5f;
        float afterImageDuration = 0.15f;
        float afterImageIntensity = 0.2f;
        float jitterIntensity = 1f;
        float jitterUnderIntensity = 0.5f;

        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        if (Global.getCombatEngine().isPaused()) {
            amount = 0f;
        }

        float shipRadius = effectiveRadius(ship);
        Vector2f offset = new Vector2f(-6f, 0f);
        VectorUtils.rotate(offset, ship.getFacing(), offset);
        Vector2f centerLocation = Vector2f.add(ship.getLocation(), offset, new Vector2f());

        switch (state) {
            case IN: {
                float startSize = shipRadius * 0.5f;
                float endSize = shipRadius * 0.75f;
                if (!started) {
                    Global.getCombatEngine().spawnExplosion(centerLocation, ZERO, JITTER_UNDER_COLOR, shipRadius * 2f, 0.1f);                    

                    float lifetime = 3f;

                    wave = new WaveDistortion(centerLocation, ZERO);
                    wave.setSize(endSize);
                    wave.setIntensity(endSize * 0.2f);
                    wave.flip(true);
                    wave.fadeInSize(lifetime * endSize / (endSize - startSize));
                    wave.fadeInIntensity(lifetime);
                    wave.setSize(startSize);
                    wave.setLifetime(0f);
                    wave.setAutoFadeIntensityTime(0.1f);
                    wave.setAutoFadeSizeTime(0.1f);
                    DistortionShader.addDistortion(wave);

                    started = true;
                }

                float shipTimeMult = lerp(TIME_MULT_IN_START, TIME_MULT_IN_END, effectLevel);

                float damperEffectLevel = effectLevel;
                jitterIntensity *= 0.75f;
                jitterUnderIntensity *= 0.75f;

                if (wave != null) {
                    wave.setLocation(centerLocation);
                }
                stats.getTimeMult().modifyMult(id, shipTimeMult);

                float realEffectLevel = lerp(0.5f, 1f, effectLevel);
                Color jitterColor = new Color(JITTER_COLOR.getRed(), JITTER_COLOR.getGreen(), JITTER_COLOR.getBlue(),
                        clamp255(Math.round(jitterIntensity * realEffectLevel * JITTER_COLOR.getAlpha())));
                Color jitterUnderColor = new Color(JITTER_UNDER_COLOR.getRed(), JITTER_UNDER_COLOR.getGreen(), JITTER_UNDER_COLOR.getBlue(),
                        clamp255(Math.round(jitterUnderIntensity * damperEffectLevel * JITTER_UNDER_COLOR.getAlpha())));
                ship.setJitter(this, jitterColor, 1f, 1, 0f, jitterIntensity * 10f * realEffectLevel);
                ship.setJitterUnder(this, jitterUnderColor, 1f, 10, 0f, 7f + jitterUnderIntensity * (10f * damperEffectLevel));

                interval.advance((amount / shipTimeMult));
                if (interval.intervalElapsed()) {
                    float randRange = (float) Math.sqrt(shipRadius);

                    Vector2f randLoc = MathUtils.getRandomPointInCircle(ZERO, randRange * afterImageSpread);
                    Vector2f vel = new Vector2f(ship.getVelocity());
                    vel.scale(-1f);

                    Color afterImageColor = new Color(AFTERIMAGE_COLOR.getRed(), AFTERIMAGE_COLOR.getGreen(), AFTERIMAGE_COLOR.getBlue(),
                            clamp255(Math.round(effectLevel * afterImageIntensity * AFTERIMAGE_COLOR.getAlpha())));
                    ship.addAfterimage(afterImageColor, randLoc.x, randLoc.y, vel.x, vel.y, randRange * afterImageJitter,
                            0.05f * afterImageDuration, 0.2f * afterImageDuration * shipTimeMult, 0.05f * afterImageDuration * shipTimeMult, true, false, false);

                    for (int i = 0; i < 2; i++) {
                        float particleSpawnAngle = MathUtils.getRandomNumberInRange(0f, 360f);
                        float particleSpawnDist = lerp(startSize * 3f, endSize * 3f, effectLevel);
                        float particleDuration = lerp(0.5f, 0.3f, effectLevel);
                        Vector2f particlePoint = MathUtils.getPointOnCircumference(centerLocation, particleSpawnDist, particleSpawnAngle);
                        Vector2f particleVel = new Vector2f((-particleSpawnDist * 2f / 3f) / particleDuration, 0f);
                        VectorUtils.rotate(particleVel, particleSpawnAngle, particleVel);
                        Global.getCombatEngine().addHitParticle(particlePoint, particleVel, 5f, 1f, particleDuration, AFTERIMAGE_COLOR);
                    }
                }
                
                break;
            }
            case OUT: {
                if (!fired) {
                	activateSound = Global.getSoundPlayer().playSound("ed_mag2", 0.5f, 0.85f, centerLocation, ZERO);
                    Global.getCombatEngine().spawnExplosion(centerLocation, ZERO, JITTER_COLOR, shipRadius * 4f, 0.2f);

                    float startSize = shipRadius * 1.5f;
                    float endSize = (shipRadius * 2f) + 400f;
                    RippleDistortion ripple = new RippleDistortion(centerLocation, ZERO);
                    ripple.setSize(endSize);
                    ripple.setIntensity(endSize * 0.05f);
                    ripple.setFrameRate(60f / 0.3f);
                    ripple.fadeInSize(0.3f * endSize / (endSize - startSize));
                    ripple.fadeOutIntensity(0.3f);
                    ripple.setSize(startSize);
                    DistortionShader.addDistortion(ripple);

                    int numParticles = Math.round(shipRadius);
                    for (int i = 0; i < numParticles; i++) {
                        float particleSpawnAngle = MathUtils.getRandomNumberInRange(0f, 360f);
                        float particleDuration = MathUtils.getRandomNumberInRange(0.5f, 1f);
                        Vector2f particlePoint = MathUtils.getPointOnCircumference(centerLocation, shipRadius, particleSpawnAngle);
                        Vector2f particleVel = new Vector2f(shipRadius * 4f, 0f);
                        VectorUtils.rotate(particleVel, particleSpawnAngle, particleVel);
                        Global.getCombatEngine().addHitParticle(particlePoint, particleVel, 10f, 1f, particleDuration, AFTERIMAGE_COLOR);
                    }
                    fired = true;
                }

                float effectSqrt = (float) Math.sqrt(effectLevel);
                float shipTimeMult = lerp(TIME_MULT_ACTIVE_END, TIME_MULT_ACTIVE_START, effectSqrt);
                stats.getTimeMult().modifyMult(id, shipTimeMult);
                stats.getVentRateMult().modifyMult(id, Math.min(1f, 2f / shipTimeMult));
                stats.getCRLossPerSecondPercent().modifyMult(id, 2f);
                String globalId = id + "_" + ship.getId();
                if (Global.getCombatEngine().getPlayerShip() == ship) {
                    PostProcessShader.setNoise(false, lerp(0f, 0.5f, effectSqrt));
                    PostProcessShader.setSaturation(false, lerp(1f, 0f, Math.max(0f, Math.min(1f, (effectSqrt - 0.15f) * 8f))));
                    PostProcessShader.setLightness(false, lerp(1f, 1.5f, effectSqrt));
                    Global.getCombatEngine().getTimeMult().modifyMult(globalId, 1f / shipTimeMult);
                    Global.getCombatEngine().getCustomData().put(postProcessKey, new Object());
                    if (activateSound != null) {
                        activateSound.setPitch(lerp(2.5f, 0.5f, effectSqrt));
                        activateSound.setVolume(lerp(0f, 1f, effectSqrt));
                        activateSound.setLocation(centerLocation.x, centerLocation.y);
                    }
                    Global.getSoundPlayer().playLoop("ed_tick", this, TIME_MULT_ACTIVE_START / shipTimeMult, lerp(0.5f, effectSqrt, Math.min(1f, 4f * (1f - effectLevel))), centerLocation, ZERO);
                } else {
                    Global.getCombatEngine().getTimeMult().unmodify(globalId);
                    if (Global.getCombatEngine().getCustomData().containsKey(postProcessKey)) {
                        Global.getCombatEngine().getCustomData().remove(postProcessKey);
                        PostProcessShader.resetDefaults();
                    }
                    if (activateSound != null) {
                        activateSound.setVolume(lerp(0f, 1f, effectSqrt));
                        activateSound.setLocation(centerLocation.x, centerLocation.y);
                    }
                }                

                Color jitterColor = new Color(JITTER_COLOR.getRed(), JITTER_COLOR.getGreen(), JITTER_COLOR.getBlue(),
                        clamp255(Math.round(jitterIntensity * effectSqrt * JITTER_COLOR.getAlpha())));
                Color jitterUnderColor = new Color(JITTER_UNDER_COLOR.getRed(), JITTER_UNDER_COLOR.getGreen(), JITTER_UNDER_COLOR.getBlue(),
                        clamp255(Math.round(jitterUnderIntensity * effectSqrt * JITTER_UNDER_COLOR.getAlpha())));
                ship.setJitter(this, jitterColor, 1f, 1, 5f, jitterIntensity * (8f * effectSqrt));
                ship.setJitterUnder(this, jitterUnderColor, 1f, 10, 5f, jitterUnderIntensity * (12f * effectSqrt));

                interval.advance(amount);
                if (interval.intervalElapsed()) {
                    float randRange = (float) Math.sqrt(shipRadius);

                    Vector2f randLoc = MathUtils.getRandomPointInCircle(ZERO, randRange * afterImageSpread);
                    Vector2f vel = new Vector2f(ship.getVelocity());
                    vel.scale(-1f);

                    Color afterImageColor = new Color(AFTERIMAGE_COLOR.getRed(), AFTERIMAGE_COLOR.getGreen(), AFTERIMAGE_COLOR.getBlue(),
                            clamp255(Math.round(effectSqrt * afterImageIntensity * AFTERIMAGE_COLOR.getAlpha())));
                    ship.addAfterimage(afterImageColor, randLoc.x, randLoc.y, vel.x, vel.y, randRange * afterImageJitter,
                            0.05f * afterImageDuration, 0.2f * afterImageDuration * shipTimeMult, 0.05f * afterImageDuration * shipTimeMult, true, false, false);
                }
                break;
            }
            default:
                break;
        }
    }
    
    public static int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
    }
    
    public static float lerp(float x, float y, float alpha) {
        return (1f - alpha) * x + alpha * y;
    }
    
    public static float effectiveRadius(ShipAPI ship) {
        if (ship.getSpriteAPI() == null || ship.isPiece()) {
            return ship.getCollisionRadius();
        } else {
            float fudgeFactor = 1.5f;
            return ((ship.getSpriteAPI().getWidth() / 2f) + (ship.getSpriteAPI().getHeight() / 2f)) * 0.5f * fudgeFactor;
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }
        ShipAPI ship = (ShipAPI) stats.getEntity();

        stats.getTimeMult().unmodify(id);
        stats.getVentRateMult().unmodify(id);
        stats.getCRLossPerSecondPercent().unmodify(id);
        String globalId = id + "_" + ship.getId();
        Global.getCombatEngine().getTimeMult().unmodify(globalId);
        if (Global.getCombatEngine().getCustomData().containsKey(postProcessKey)) {
            Global.getCombatEngine().getCustomData().remove(postProcessKey);
            PostProcessShader.resetDefaults();
        }

        started = false;
        fired = false;
        if (wave != null) {
            DistortionShader.removeDistortion(wave);
            wave = null;
        }
        if (activateSound != null) {
            activateSound.stop();
            activateSound = null;
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        switch (state) {
            case IN: {
                if (index == 0) {
                    return new StatusData("time flow altered", true);
                }
                break;
            }
            case OUT: {
                if (index == 0) {
                    return new StatusData("time flow altered", false);
                }
                break;
            }
            default:
                break;
        }
        return null;
    }
}
