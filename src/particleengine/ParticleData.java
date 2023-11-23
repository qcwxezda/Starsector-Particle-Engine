package particleengine;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.FloatBuffer;

/** Pre-generation data for a single particle. */
@SuppressWarnings("UnusedReturnValue")
public class ParticleData {
    /**
     * Initial location of this particle relative to the parent emitter, along that emitter's axes.
     */
    private final Vector2f offset = new Vector2f();

    /**
     * Initial velocity of this particle along its parent emitter's axes.
     */
    private final Vector2f velocity = new Vector2f();

    /**
     * Acceleration of this particle along its parent emitter's axes.
     */
    private final Vector2f acceleration = new Vector2f();

    /**
     * Amplitude, frequency, and phase of sinusoidal motion along emitter's x-axis. Phase in degrees.
     */
    private float sinXAmp = 0f, sinXFreq = 0f, sinXPhase = 0f;
    /**
     * Amplitude, frequency, and phase of sinusoidal motion along emitter's y-axis. Phase in degrees.
     */
    private float sinYAmp = 0f, sinYFreq = 0f, sinYPhase = 0f;
    /**
     * Initial facing angle, turn rate, and turn acceleration. In degrees.
     */
    private float facing = 0f, turnRate = 0f, turnAcceleration = 0f;
    /**
     * Revolution rate and acceleration about the emitter's origin. In degrees.
     */
    private float revolutionRate = 0f, revolutionAcceleration = 0f;
    /**
     * Initial size, growth rate, and growth acceleration.
     */
    private Vector2f size = new Vector2f(25f, 25f), growthRate = new Vector2f(), growthAcceleration = new Vector2f();
    /**
     * Starting color in HSVA.
     */
    private float[] color = new float[]{0f, 1f, 1f, 1f};
    /**
     * Color shift over time in HSVA.
     */
    private final float[] colorShift = new float[]{0f, 0f, 0f, 0f};
    /**
     * Vector containing fade in and fade out times.
     */
    private Vector2f fadeTime = new Vector2f();
    /**
     * How long the particle will last.
     */
    float life = 1f;
    static final float twoPi = 2f * (float) Math.PI;

    /**
     * Initializes a {@code ParticleData} with default properties.
     */
    public ParticleData() {
    }

    /**
     * @param offset Initial location of this particle relative to the parent emitter, along that emitter's axes.
     * @return {@code this}
     */
    public ParticleData offset(Vector2f offset) {
        this.offset.set(offset);
        return this;
    }

    /**
     * @param velocity Initial velocity of this particle along its parent emitter's axes.
     * @return {@code this}
     */
    public ParticleData velocity(Vector2f velocity) {
        this.velocity.set(velocity);
        return this;
    }

    /**
     * @param acceleration Acceleration of this particle along its parent emitter's axes.
     * @return {@code this}
     */
    public ParticleData acceleration(Vector2f acceleration) {
        this.acceleration.set(acceleration);
        return this;
    }

    /**
     * @param amplitude Amplitude of sinusoidal motion along emitter's x-axis, in world units.
     * @param frequency Frequency of sinusoidal motion along emitter's x-axis, in hertz.
     * @param phase Phase of sinusoidal motion along emitter's x-axis, in degrees.
     * @return {@code this}
     */
    public ParticleData sinusoidalXMotion(float amplitude, float frequency, float phase) {
        sinXAmp = amplitude;
        sinXFreq = frequency;
        sinXPhase = phase;
        return this;
    }

    /**
     * @param amplitude Amplitude of sinusoidal motion along emitter's y-axis, in world units.
     * @param frequency Frequency of sinusoidal motion along emitter's y-axis, in hertz.
     * @param phase Phase of sinusoidal motion along emitter's y-axis, in degrees.
     * @return {@code this}
     */
    public ParticleData sinusoidalYMotion(float amplitude, float frequency, float phase) {
        sinYAmp = amplitude;
        sinYFreq = frequency;
        sinYPhase = phase;
        return this;
    }

    /**
     * @param facing Initial facing angle. {@code 0} degrees is to the right.
     * @return {@code this}
     */
    public ParticleData facing(float facing) {
        this.facing = facing;
        return this;
    }

    /**
     * @param turnRate Initial rotation rate about the particle's center point.
     * @return {@code this}
     */
    public ParticleData turnRate(float turnRate) {
        this.turnRate = turnRate;
        return this;
    }

    /**
     * @param turnAcceleration Rotation acceleration about the particle's center point.
     * @return {@code this}
     */
    public ParticleData turnAcceleration(float turnAcceleration) {
        this.turnAcceleration = turnAcceleration;
        return this;
    }

    /**
     * @param revolutionRate Initial revolution rate about the emitter's location.
     * @return {@code this}
     */
    public ParticleData revolutionRate(float revolutionRate) {
        this.revolutionRate = revolutionRate;
        return this;
    }

    /**
     * @param revolutionAcceleration Revolution acceleratino about the emitter's location.
     * @return {@code this}
     */
    public ParticleData revolutionAcceleration(float revolutionAcceleration) {
        this.revolutionAcceleration = revolutionAcceleration;
        return this;
    }

    /**
     * @param xSize Width, in world units.
     * @param ySize Height, in world units.
     * @return {@code this}
     */
    public ParticleData size(float xSize, float ySize) {
        this.size = new Vector2f(xSize, ySize);
        return this;
    }

    /**
     * @param xGrowthRate Growth rate along the x-axis.
     * @param yGrowthRate Growth rate along the y-axis.
     * @return {@code this}
     */
    public ParticleData growthRate(float xGrowthRate, float yGrowthRate) {
        this.growthRate = new Vector2f(xGrowthRate, yGrowthRate);
        return this;
    }

    /**
     * @param xGrowthAcceleration Growth acceleration along the x-axis.
     * @param yGrowthAcceleration Growth acceleration along the y-axis.
     * @return {@code this}
     */
    public ParticleData growthAcceleration(float xGrowthAcceleration, float yGrowthAcceleration) {
        this.growthAcceleration = new Vector2f(xGrowthAcceleration, yGrowthAcceleration);
        return this;
    }

    /**
     * @param r Initial red channel value between {@code 0} and {@code 1}.
     * @param g Initial green channel value between {@code 0} and {@code 1}.
     * @param b Initial blue channel value between {@code 0} and {@code 1}.
     * @param a Initial alpha channel value between {@code 0} and {@code 1}.
     * @return {@code this}
     */
    public ParticleData color(float r, float g, float b, float a) {
        Utils.toHSVA(new float[]{r, g, b, a}, this.color);
        return this;
    }

    /**
     * @param color {@link Color} object to set this particle's color to.
     * @return {@code this}
     */
    public ParticleData color(Color color) {
        Utils.toHSVA(color.getRGBComponents(null), this.color);
        return this;
    }

    /**
     * @param color 4-element float array containing RGBA color data.
     * @return {@code this}
     */
    public ParticleData color(float[] color) {
        Utils.toHSVA(color, this.color);
        return this;
    }

    ParticleData colorHSVA(float[] color) {
        this.color = color;
        return this;
    }

    /**
     * @param hueShift Hue shift amount, in degrees per second.
     * @return {@code this}
     */
    public ParticleData hueShift(float hueShift) {
        colorShift[0] = hueShift;
        return this;
    }

    /**
     * @param saturationShift Saturation shift amount per second. Saturation is defined between {@code 0} and {@code 1}.
     * @return {@code this}
     */
    public ParticleData saturationShift(float saturationShift) {
        colorShift[1] = saturationShift;
        return this;
    }

    /**
     * @param colorValueShift Color value shift amount per second. Color value is defined between {@code 0} and {@code 1}.
     * @return {@code this}
     */
    public ParticleData colorValueShift(float colorValueShift) {
        colorShift[2] = colorValueShift;
        return this;
    }

    /**
     * @param alphaShift Alpha shift amount per second. ALpha is defined between {@code 0} and {@code 1}.
     * @return {@code this}
     */
    public ParticleData alphaShift(float alphaShift) {
        colorShift[3] = alphaShift;
        return this;
    }

    /**
     * @param fadeIn Fade in time. An alpha value multiplier linearly ramps up from particle's start of life.
     * @param fadeOut Fade out time. An alpha value multiplier linearly ramps down to particle's end of life.
     * @return {@code this}
     */
    public ParticleData fadeTime(float fadeIn, float fadeOut) {
        this.fadeTime = new Vector2f(fadeIn, fadeOut);
        return this;
    }

    /**
     * @param life Total duration particle will last for.
     * @return {@code this}
     */
    public ParticleData life(float life) {
        this.life = life;
        return this;
    }

    final void addToFloatBuffer(IEmitter emitter, float startTime, FloatBuffer buffer) {
        Vector2f emitterLocation = emitter.getLocation();
        buffer.put(emitter.getIndexInTracker() + 0.5f)
            .put(offset.x)
            .put(offset.y)
            .put(emitterLocation.x)
            .put(emitterLocation.y)
            .put(emitter.getXDir() * Misc.RAD_PER_DEG)
            .put(velocity.x)
            .put(velocity.y)
            .put(acceleration.x)
            .put(acceleration.y)
            .put(sinXAmp)
            .put(sinXFreq * twoPi)
            .put(sinXPhase * Misc.RAD_PER_DEG)
            .put(sinYAmp)
            .put(sinYFreq * twoPi)
            .put(sinYPhase * Misc.RAD_PER_DEG)
            .put(facing * Misc.RAD_PER_DEG)
            .put(turnRate * Misc.RAD_PER_DEG)
            .put(turnAcceleration * Misc.RAD_PER_DEG)
            .put(revolutionRate * Misc.RAD_PER_DEG)
            .put(revolutionAcceleration * Misc.RAD_PER_DEG)
            .put(size.x)
            .put(growthRate.x)
            .put(growthAcceleration.x)
            .put(size.y)
            .put(growthRate.y)
            .put(growthAcceleration.y)
            .put(color[0])
            .put(color[1])
            .put(color[2])
            .put(color[3])
            .put(colorShift[0])
            .put(colorShift[1])
            .put(colorShift[2])
            .put(colorShift[3])
            .put(fadeTime.x)
            .put(fadeTime.y)
            .put(startTime)
            .put(startTime + life);
    }
}
