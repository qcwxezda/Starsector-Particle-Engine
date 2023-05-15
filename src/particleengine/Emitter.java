package particleengine;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.PrintWriter;

/**
 * Default particle generator that implements {@link IEmitter}.
 * Initialize an {@code Emitter} using {@link Particles#createCopy}. After setting the emitter's properties,
 * generate particles using {@link Particles#burst} or {@link Particles#stream}.
 */
@SuppressWarnings("unused")
public class Emitter extends IEmitter {
    private static final Logger log = Logger.getLogger(Emitter.class);
    int sfactor, dfactor, blendMode;
    SpriteAPI sprite;
    float minLife = 1f, maxLife = 1f;

    final Vector2f location = new Vector2f();
    final Vector2f
            minOffset = new Vector2f(),
            maxOffset = new Vector2f(),
            minVelocity = new Vector2f(),
            maxVelocity = new Vector2f(),
            minAcceleration = new Vector2f(),
            maxAcceleration = new Vector2f();
    /**
     * Random point in ring with between radii is added to position. Same for velocity and acceleration.
     */
    float minPositionSpread = 0f, maxPositionSpread = 0f,
            minVelocitySpread = 0f, maxVelocitySpread = 0f,
            minAccelerationSpread = 0f, maxAccelerationSpread = 0f;
    /**
     * Alpha is angular acceleration, NOT image alpha
     */
    float minTheta = 0f, maxTheta = 0f, minW = 0f, maxW = 0f, minAlpha = 0f, maxAlpha = 0f;
    final float[]
            minSizeDataX = new float[] {25f, 0f, 0f},
            maxSizeDataX = new float[] {25f, 0f, 0f},
            minSizeDataY = new float[] {25f, 0f, 0f},
            maxSizeDataY = new float[] {25f, 0f, 0f};
    float minFadeIn = 0f, maxFadeIn = 0f, minFadeOut = 0f, maxFadeOut = 0f;
    /** All color data in hsva */
    final float[]
            startColor = new float[] {0f, 0f, 1f, 1f},
            startColorRandom = new float[] {0f, 0f, 0f, 0f},
            minColorShift = new float[] {0f, 0f, 0f, 0f},
            maxColorShift = new float[] {0f, 0f, 0f, 0f};
    /**
     * Velocity and acceleration amounts pointed outwards. No effect if positionSpread is 0.
     */
    float minRadialVelocity = 0f, maxRadialVelocity = 0f, minRadialAcceleration = 0f, maxRadialAcceleration = 0f;
    float minRadialW = 0f, maxRadialW = 0f, minRadialAlpha = 0f, maxRadialAlpha = 0f;
    float minSinXAmplitude = 0f, maxSinXAmplitude = 0f, minSinXFrequency = 0f, maxSinXFrequency = 0f, minSinXPhase = 0f, maxSinXPhase = 0f;
    float minSinYAmplitude = 0f, maxSinYAmplitude = 0f, minSinYFrequency = 0f, maxSinYFrequency = 0f, minSinYPhase = 0f, maxSinYPhase = 0f;
    final Vector2f xAxis = new Vector2f(1f, 0f);
    boolean syncSize = false;
    CombatEngineLayers layer = CombatEngineLayers.ABOVE_PARTICLES_LOWER;
    float inactiveBorder = 500f;

    Emitter(
            Vector2f location,
            SpriteAPI sprite,
            int sfactor,
            int dfactor,
            int blendMode) {
        this.location.set(location);
        this.sfactor = sfactor; // blending
        this.dfactor = dfactor; // blending
        this.blendMode = blendMode; // blending
        this.sprite = sprite;
    }

    /**
     * Sets the "render radius" of the emitter. Formally, if an emitter is outside the box {@code [-amount,W+amount]x[-amount,H+amount]}
     * relative to the viewport when particles would have been generated, that generate call is ignored.
     *
     * @param amount Border amount, in world units.
     */
    public void setInactiveBorder(float amount) {
        this.inactiveBorder = amount;
    }

    /**
     * Sets the rendering layer of this emitter. See {@link com.fs.starfarer.api.combat.CombatEngineLayers} for an
     * ordered list of possible values.
     *
     * @param layer Combat layer to render particles to
     */
    public void setLayer(CombatEngineLayers layer) {
        this.layer = layer;
    }

    /**
     * Sets this emitter's location. To set the starting position of particles relative to this emitter,
     * use {@link #offset}.
     *
     * @param x Initial x-position, in absolute world units.
     * @param y Initial y-position, in absolute world units.
     */
    public void setLocation(float x, float y) {
        this.location.set(x, y);
    }

    /** @see #setLocation(float, float) */
    public void setLocation(Vector2f location) {
        this.location.set(location);
    }

    /** Sets the axes of this emitter's coordinate system.
     *  Defaults to global axes, i.e. {@code (0, 1)} and {@code (1, 0)}.
     *
     * @param xAxis The emitter's x-axis. Doesn't need to be normalized. If degenerate, will be set to the default
     *              value {@code (0, 1)}. The emitter's y-axis will automatically be set to the
     *              unit vector that is counterclockwise perpendicular to {@code xAxis}.
     */
    public void setAxis(Vector2f xAxis) {
        if (xAxis.lengthSquared() <= 0f) {
            this.xAxis.set(1f, 0f);
            return;
        }
        xAxis.normalise(this.xAxis);
    }

    /** @see #setAxis(Vector2f) */
    public void setAxis(float dir) {
        xAxis.set(Misc.getUnitVectorAtDegreeAngle(dir));
    }

    /**
     * Sets the sprite used by this emitter.
     * @param sprite Sprite to use. Can be {@code null}.
     */
    public void setSprite(SpriteAPI sprite) {
        this.sprite = sprite;
    }

    /**
     * Sets the sprite used by this emitter.
     * @param loc Location of sprite to use. If {@code null}, will set the sprite to {@code null}.
     */
    public void setSprite(String loc) {
        this.sprite = Utils.getLoadedSprite(loc);
    }

    /**
     * Sets the blend source factor, destination factor, and blending function.
     * @param sfactor Source factor, e.g. {@link org.lwjgl.opengl.GL11#GL_SRC_ALPHA}.
     * @param dfactor Destination factor, e.g. {@link org.lwjgl.opengl.GL11#GL_ONE_MINUS_SRC_ALPHA}.
     * @param blendMode Blending operator, e.g. {@link org.lwjgl.opengl.GL14#GL_FUNC_ADD}.
     */
    public void setBlendMode(int sfactor, int dfactor, int blendMode) {
        this.sfactor = sfactor;
        this.dfactor = dfactor;
        this.blendMode = blendMode;
    }

    /**
     * Sets the minimum and maximum lifetime of particles generated by this {@code Emitter}.
     * Particles exceeding their lifespan are not rendered.
     *
     * @param minLife Minimum lifetime, in seconds.
     * @param maxLife Maximum lifetime, in seconds.
     */
    public void life(float minLife, float maxLife) {
        this.minLife = Math.min(minLife, maxLife);
        this.maxLife = maxLife;
    }


    /**
     * Sets the fade-in and fade-out times for particles generated by this  Particles' alpha values rise linearly from
     * {@code 0} to {@code 1} within the fade-in window, and fall linearly from {@code 1} to {@code 0} within
     * the fade-out window.
     *
     * @param minFadeIn  Minimum fade-in time, in seconds.
     * @param maxFadeIn  Maximum fade-in time, in seconds.
     * @param minFadeOut Minimum fade-out time, in seconds.
     * @param maxFadeOut Maximum fade-out time, in seconds.
     */
    public void fadeTime(float minFadeIn, float maxFadeIn, float minFadeOut, float maxFadeOut) {
        this.minFadeIn = minFadeIn;
        this.minFadeOut = minFadeOut;
        this.maxFadeIn = maxFadeIn;
        this.maxFadeOut = maxFadeOut;
    }

    /**
     * Sets the position of generated particles relative to this emitter's location. This position is defined
     * along the emitter's axes.
     *
     * @param minX Minimum offset of particles along the emitter's x-axis in world units.
     * @param maxX Maximum offset of particles along the emitter's x-axis in world units.
     * @param minY Minimum offset of particles along the emitter's y-axis in world units.
     * @param maxY Maximum offset of particles along the emitter's y-axis in world units.
     */
    public void offset(float minX, float maxX, float minY, float maxY) {
        this.minOffset.set(new Vector2f(minX, minY));
        this.maxOffset.set(new Vector2f(maxX, maxY));
    }

    /**
     *  Sets the component-wise minimum and maximum positions of generated particles relative to this emitter's location,
     *  along the emitter's axes.
     *
     * @param min Minimum x and y offsets.
     * @param max Maximum x and y offsets.
     */
    public void offset(Vector2f min, Vector2f max) {
        this.minOffset.set(min);
        this.maxOffset.set(max);
    }

    /**
     * Sets the velocity of generated particles along the emitter's axes.
     *
     * @param minX Minimum velocity of particles along the emitter's x-axis in world units / s.
     * @param maxX Maximum velocity of particles along the emitter's x-axis in world units / s.
     * @param minY Minimum velocity of particles along the emitter's y-axis in world units / s.
     * @param maxY Maximum velocity of particles along the emitter's y-axis in world units / s.
     */
    public void velocity(float minX, float maxX, float minY, float maxY) {
        this.minVelocity.set(new Vector2f(minX, minY));
        this.maxVelocity.set(new Vector2f(maxX, maxY));
    }

    /**
     *  Sets the component-wise minimum and maximum velocities of generated particles along the emitter's axes.
     *
     * @param min Minimum x and y velocities.
     * @param max Maximum x and y velocities.
     */
    public void velocity(Vector2f min, Vector2f max) {
        this.minVelocity.set(min);
        this.maxVelocity.set(max);
    }

    /**
     * Sets the acceleration of generated particles along the emitter's axes.
     *
     * @param minX Minimum acceleration of particles along the emitter's x-axis in world units / s^2.
     * @param maxX Maximum acceleration of particles along the emitter's x-axis in world units / s^2.
     * @param minY Minimum acceleration of particles along the emitter's y-axis in world units / s^2.
     * @param maxY Maximum acceleration of particles along the emitter's y-axis in world units / s^2.
     */
    public void acceleration(float minX, float maxX, float minY, float maxY) {
        this.minAcceleration.set(new Vector2f(minX, minY));
        this.maxAcceleration.set(new Vector2f(maxX, maxY));
    }

    /**
     *  Sets the component-wise minimum and maximum accelerations of generated particles along the emitter's axes.
     *
     * @param min Minimum x and y accelerations.
     * @param max Maximum x and y accelerations.
     */
    public void acceleration(Vector2f min, Vector2f max) {
        this.minAcceleration.set(min);
        this.maxAcceleration.set(max);
    }

    /**
     * Adds circular randomness to the positions of particles generated by this  Generated particles will have a random
     * vector inside an annulus with inner radius {@code innerRadius} and outer radius {@code outerRadius} added to their positions.
     *
     * @param innerRadius Inner radius of annulus inside which a random vector will be chosen to be added to each
     *                  particle's starting position.
     * @param outerRadius Outer radius of annulus inside which a random vector will be chosen to be added to each
     *                  particle's starting position.
     */
    public void circleOffset(float innerRadius, float outerRadius) {
        minPositionSpread = innerRadius;
        maxPositionSpread = outerRadius;
    }

    /**
     * Adds circular randomness to the velocities of particles generated by this  Generated particles will have a random
     * vector inside an annulus with inner radius {@code innerRadius} and outer radius {@code outerRadius} added to their velocities.
     *
     * @param innerRadius Inner radius of annulus inside which a random vector will be chosen to be added to each
     *                  particle's initial velocity.
     * @param outerRadius Outer radius of annulus inside which a random vector will be chosen to be added to each
     *                  particle's initial velocity.
     */
    public void circleVelocity(float innerRadius, float outerRadius) {
        minVelocitySpread = innerRadius;
        maxVelocitySpread = outerRadius;
    }

    /**
     * Adds circular randomness to the accelerations of particles generated by this  Generated particles will have a random
     * vector inside an annulus with inner radius {@code innerRadius} and outer radius {@code outerRadius} added to their accelerations.
     *
     * @param innerRadius Inner radius of annulus inside which a random vector will be chosen to be added to each
     *                  particle's initial acceleration.
     * @param outerRadius Outer radius of annulus inside which a random vector will be chosen to be added to each
     *                  particle's initial acceleration.
     */
    public void circleAcceleration(float innerRadius, float outerRadius) {
        minAccelerationSpread = innerRadius;
        maxAccelerationSpread = outerRadius;
    }

    /** Sets minimum and maximum initial facing angles of particles generated by this 
     *  Each particle will have an initial facing randomly chosen between the two given angles. A
     *  facing direction of {@code 0} is oriented along this emitter's x-axis.
     *
     * @param minAngle Minimum initial facing direction, in degrees.
     * @param maxAngle Maximum initial facing direction, in degrees.
     */
    public void facing(float minAngle, float maxAngle) {
        minTheta = minAngle;
        maxTheta = maxAngle;
    }

    /** Sets minimum and maximum initial turn rates for particles generated by this  Each particle will
     *  have an initial turn rate randomly chosen between the two given numbers.
     * @param minRate Minimum initial turn rate, in degrees / s.
     * @param maxRate Maximum initial turn rate, in degrees / s.
     */
    public void turnRate(float minRate, float maxRate) {
        minW = minRate;
        maxW = maxRate;
    }

    /** Sets minimum and maximum turn rate accelerations for particles generated by this  Each particle will have
     * a turn rate acceleration randomly chosen between the two given numbers.
      * @param minAcceleration Minimum initial turn rate acceleration, in degrees / s^2.
     * @param maxAcceleration Maximum initial turn rate acceleration, in degrees / s^2.
     */
    public void turnAcceleration(float minAcceleration, float maxAcceleration) {
        minAlpha = minAcceleration;
        maxAlpha = maxAcceleration;
    }

    /**
     * Sets the size of particles generated by this  Each particle is given a size randomly chosen between
     * {@code minSize} and {@code maxSize}.
     *
     * @param minSize Minimum initial particle size, in world units.
     * @param maxSize Maximum initial particle size, in world units.
     */
    public void size(float minSize, float maxSize) {
        minSizeDataX[0] = minSizeDataY[0] = minSize;
        maxSizeDataX[0] = maxSizeDataY[0] = maxSize;
    }

    /**
     * Sets the initial growth rate of particles generated by this  Each particle is given
     * a growth rate randomly chosen between {@code minRate} and {@code maxRate}.
     *
     * @param minRate Minimum initial particle growth rate, in world units / s.
     * @param maxRate Maximum initial particle growth rate, in world units / s.
     */
    public void growthRate(float minRate, float maxRate) {
        minSizeDataX[1] = minSizeDataY[1] = minRate;
        maxSizeDataX[1] = maxSizeDataY[1] = maxRate;
    }

    /**
     * Sets the growth acceleration of particles generated by this  Each particle is
     * given a growth acceleration randomly chosen between {@code minAcceleration} and {@code maxAcceleration}.
     *
     * @param minAcceleration Minimum particle growth acceleration, in world units / s^2.
     * @param maxAcceleration Maximum particle growth acceleration, in world units / s^2.
     */
    public void growthAcceleration(float minAcceleration, float maxAcceleration) {
        minSizeDataX[2] = minSizeDataY[2] = minAcceleration;
        maxSizeDataX[2] = maxSizeDataY[2] = maxAcceleration;
    }

    /** Same as {@link #size(float, float)}, but allows specification of x-scale and y-scale separately. */
    public void size(float minXSize, float maxXSize, float minYSize, float maxYSize) {
        minSizeDataX[0] = minXSize;
        maxSizeDataX[0] = maxXSize;
        minSizeDataY[0] = minYSize;
        maxSizeDataY[0] = maxYSize;
    }

    /** Same as {@link #growthRate(float, float)}, but allows specification of x-scale and y-scale separately. */
    public void growthRate(float minXRate, float maxXRate, float minYRate, float maxYRate) {
        minSizeDataX[1] = minXRate;
        maxSizeDataX[1] = maxXRate;
        minSizeDataY[1] = minYRate;
        maxSizeDataY[1] = maxYRate;
    }

    /** Same as {@link #growthAcceleration(float, float)}, but allows specification of x-scale and y-scale separately. */
    public void growthAcceleration(float minXAcceleration, float maxXAcceleration, float minYAcceleration, float maxYAcceleration) {
        minSizeDataX[2] = minXAcceleration;
        maxSizeDataX[2] = maxXAcceleration;
        minSizeDataY[2] = minYAcceleration;
        maxSizeDataY[2] = maxYAcceleration;
    }

    /**
     * Sets whether generated particles should always have the same {@code x} and {@code y} scale. If this is set,
     * then only the emitter's {@code x} values will be taken into account for size, growth rate, and growth acceleration.
     *
     * @param sync If {@code true}, ensures that generated particles have the same {@code x} and {@code y} sizes,
     *             growth rates, and growth accelerations. If {@code false}, these properties will have their {@code x}
     *             and {@code y} values set independently.
     */
    public void setSyncSize(boolean sync) {
        syncSize = sync;
    }

    /**
     * Sets the initial color of particles generated by this 
     * @param r Initial red value, between 0 and 1.
     * @param g Initial green value, between 0 and 1.
     * @param b Initial blue value, between 0 and 1.
     * @param a Initial alpha value, between 0 and 1.
     */
    public void color(float r, float g, float b, float a) {
        Utils.toHSVA(new float[] {r, g, b, a}, startColor);
    }

    /** @see #color(float, float, float, float) */
    public void color(Color color) {
        float[] rgba = color.getComponents(null);
        Utils.toHSVA(rgba, startColor);
    }
    
    /** @see #color(float, float, float, float) */
    public void color(float[] rgba) {
        Utils.toHSVA(rgba, startColor);
    }

    /**
     * Sets the initial color of particles generated by this emitter in HSVA space.
     *
     * @param hsva 4-element float array containing hue, saturation, color value, and alpha.
     */
    public void colorHSVA(float[] hsva) {
        startColor[0] = hsva[0];
        startColor[1] = hsva[1];
        startColor[2] = hsva[2];
        startColor[3] = hsva[3];
    }

    /**
     * Randomizes the initial color of particles generated by this  Each particle has its initial hue modified
     * by a number in the range {@code [-h/2, h/2]}. The particle's other hsva channels are similarly randomized.
     * Resulting hue values outside {@code [0, 360)}
     * wrap around. Resulting saturation, value, and alpha values are clamped to {@code [0, 1]}.
     *
     * @param h Hue randomization, in degrees. Hue is between {@code 0} and {@code 360}.
     * @param s Saturation randomization. Saturation is between {@code 0} and {@code 1}.
     * @param v Value randomization. Value is between {@code 0} and {@code 1}.
     * @param a Alpha randomization. Alpha is between {@code 0} and {@code 1}.
     */
    public void randomHSVA(float h, float s, float v, float a) {
        startColorRandom[0] = h;
        startColorRandom[1] = s;
        startColorRandom[2] = v;
        startColorRandom[3] = a;
    }

    /** @see #randomHSVA(float, float, float, float) */
    public void randomHSVA(float[] hsva) {
        startColorRandom[0] = hsva[0];
        startColorRandom[1] = hsva[1];
        startColorRandom[2] = hsva[2];
        startColorRandom[3] = hsva[3];
    }

    /**
     * Adds color shift to particles, allowing individual particles to change color over time. If the resulting
     * hue value falls outside {@code [0, 360)}, it will wrap around. Resulting saturation, value, and alpha values
     * are clamped to {@code [0, 1]}.
     *
     * @param h Hue shift per second, in degrees
     * @param s Saturation shift per second
     * @param v Value shift per second
     * @param a Alpha shift per second
     */
    public void colorShiftHSVA(float h, float s, float v, float a) {
        minColorShift[0] = maxColorShift[0] = h;
        minColorShift[1] = maxColorShift[1] = s;
        minColorShift[2] = maxColorShift[2] = v;
        minColorShift[3] = maxColorShift[3] = a;
    }
    
    /** @see #colorShiftHSVA(float, float, float, float) */
    public void colorShiftHSVA(float[] hsva) {
        minColorShift[0] = maxColorShift[0] = hsva[0];
        minColorShift[1] = maxColorShift[1] = hsva[1];
        minColorShift[2] = maxColorShift[2] = hsva[2];
        minColorShift[3] = maxColorShift[3] = hsva[3];
    }

    /**
     * Adds color shift to particles, allowing individual particles to change color over time.
     * Each particle is given a random hue velocity between {@code minH} and {@code maxH},
     * a random saturation velocity between {@code minS} and {@code maxS},
     * a random value velocity between {@code minV} and {@code maxV},
     * and a random alpha velocity between {@code minA} and {@code maxA}.
     * If the resulting hue value falls outside {@code [0, 360)}, it will wrap around. Resulting saturation, value, and alpha values
     * are clamped to {@code [0, 1]}.
     *
     * @param minH Minimum hue shift per second, in degrees
     * @param maxH Maximum hue shift per second, in degrees
     * @param minS Minimum saturation shift per second
     * @param maxS Maximum saturation shift per second
     * @param minV Minimum value shift per second
     * @param maxV Maximum value shift per second
     * @param minA Minimum alpha shift per second
     * @param maxA Maximum alpha shift per second
     */
    public void colorShiftHSVA(float minH, float maxH, float minS, float maxS, float minV, float maxV, float minA, float maxA) {
        minColorShift[0] = minH;
        minColorShift[1] = minS;
        minColorShift[2] = minV;
        minColorShift[3] = minA;
        maxColorShift[0] = maxH;
        maxColorShift[1] = maxS;
        maxColorShift[2] = maxV;
        maxColorShift[3] = maxA;
    }

    /** @see #colorShiftHSVA(float, float, float, float, float, float, float, float) */
    public void colorShiftHSVA(float[] minHSVA, float[] maxHSVA) {
        minColorShift[0] = minHSVA[0];
        minColorShift[1] = minHSVA[1];
        minColorShift[2] = minHSVA[2];
        minColorShift[3] = minHSVA[3];
        maxColorShift[0] = maxHSVA[0];
        maxColorShift[1] = maxHSVA[1];
        maxColorShift[2] = maxHSVA[2];
        maxColorShift[3] = maxHSVA[3];
    }

    /** @see #colorShiftHSVA(float, float, float, float, float, float, float, float) */
    public void hueShift(float minH, float maxH) {
        minColorShift[0] = minH;
        maxColorShift[0] = maxH;
    }

    /** @see #colorShiftHSVA(float, float, float, float, float, float, float, float) */
    public void saturationShift(float minS, float maxS) {
        minColorShift[1] = minS;
        maxColorShift[1]=  maxS;
    }

    /** @see #colorShiftHSVA(float, float, float, float, float, float, float, float) */
    public void colorValueShift(float minV, float maxV) {
        minColorShift[2] = minV;
        maxColorShift[2] = maxV;
    }

    /** @see #colorShiftHSVA(float, float, float, float, float, float, float, float) */
    public void alphaShift(float minA, float maxA) {
        minColorShift[3] = minA;
        maxColorShift[3] = maxA;
    }

    /**
     * Adds velocity in the direction from the emitter's {@code location} to each individual particle's randomized position.
     * This effect causes particles to be pushed outwards from the emitter's {@code location}.
     * No effect on emitters with no {@code offset} or {@code positionSpread}.
     *
     * @param minVelocity Minimum outward radial velocity, in world units / s.
     * @param maxVelocity Maximum outward radial velocity, in world units / s.
     */
    public void radialVelocity(float minVelocity, float maxVelocity) {
        this.minRadialVelocity = minVelocity;
        this.maxRadialVelocity = maxVelocity;
    }

    /**
     * Adds acceleration in the direction from the emitter's {@code location} to each individual particle's randomized position.
     *
     * @param minAcceleration Minimum outward radial acceleration, in world units / s^2.
     * @param maxAcceleration Maximum outward radial acceleration, in world units / s^2.
     * @see #radialVelocity(float, float)
     */
    public void radialAcceleration(float minAcceleration, float maxAcceleration) {
        this.minRadialAcceleration = minAcceleration;
        this.maxRadialAcceleration = maxAcceleration;
    }

    /**
     * Causes particles to revolve around the emitter's {@code location} at the time of generation. Note that
     * updates to the emitter's location after a particle has already been generated are not received.
     * This effect is applied after all other movement effects.
     *
     * @param minRate     Minimum initial revolution velocity, in degrees / s.
     * @param maxRate     Maximum initial revolution velocity, in degrees / s.
     */
    public void revolutionRate(float minRate, float maxRate) {
        minRadialW = minRate;
        maxRadialW = maxRate;
    }

    /**
     * @param minAcceleration Minimum revolution acceleration, in degrees / s^2.
     * @param maxAcceleration Maximum revolution acceleration, in degrees / s^2.
     * @see #revolutionRate
     */
    public void revolutionAcceleration(float minAcceleration, float maxAcceleration) {
        minRadialAlpha = minAcceleration;
        maxRadialAlpha = maxAcceleration;
    }

    /**
     * Adds periodic motion along the emitter's x-axis. For each particle, if its phase is non-zero, will also
     * translate that particle so that its initial position is unchanged at {@code t = 0}.
     *
     * @param minAmplitude Minimum amplitude of periodic motion along the emitter's x-axis, in world units.
     * @param maxAmplitude Maximum amplitude of periodic motion along the emitter's x-axis, in world units.
     * @param minFrequency Minimum number of complete cycles per second along the emitter's x-axis.
     * @param maxFrequency Maximum of complete cycles per second along the emitter's x-axis.
     * @param minPhase     Minimum initial phase of periodic motion along the emitter's x-axis, in degrees.
     * @param maxPhase     Maximum initial phase of periodic motion along the emitter's x-axis, in degrees.
     */
    public void sinusoidalMotionX(float minAmplitude, float maxAmplitude, float minFrequency, float maxFrequency, float minPhase, float maxPhase) {
        this.minSinXAmplitude = minAmplitude;
        this.maxSinXAmplitude = maxAmplitude;
        this.minSinXFrequency = minFrequency;
        this.maxSinXFrequency = maxFrequency;
        this.minSinXPhase = minPhase;
        this.maxSinXPhase = maxPhase;
    }

    /**
     * @param minAmplitude Minimum amplitude of periodic motion along the emitter's y-axis, in world units.
     * @param maxAmplitude Maximum amplitude of periodic motion along the emitter's  y-axis, in world units.
     * @param minFrequency Minimum number of complete cycles per second along the emitter's y-axis.
     * @param maxFrequency Maximum of complete cycles per second along the emitter's y-axis.
     * @param minPhase     Minimum initial phase of periodic motion along the emitter's y-axis, in degrees.
     * @param maxPhase     Maximum initial phase of periodic motion along the emitter's y-axis, in degrees.
     * @see #sinusoidalMotionX(float, float, float, float, float, float)
     */
    public void sinusoidalMotionY(float minAmplitude, float maxAmplitude, float minFrequency, float maxFrequency, float minPhase, float maxPhase) {
        this.minSinYAmplitude = minAmplitude;
        this.maxSinYAmplitude = maxAmplitude;
        this.minSinYFrequency = minFrequency;
        this.maxSinYFrequency = maxFrequency;
        this.minSinYPhase = minPhase;
        this.maxSinYPhase = maxPhase;
    }

    @Override
    public Vector2f getLocation() {
        return location;
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    public float getXDir() {
        return Misc.getAngleInDegrees(xAxis);
    }

    @Override
    public int getBlendSourceFactor() {
        return sfactor;
    }

    @Override
    public int getBlendDestinationFactor() {
        return dfactor;
    }

    @Override
    public int getBlendFunc() {
        return blendMode;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return layer;
    }

    @Override
    public float getRenderRadius() {
        return inactiveBorder;
    }

    @Override
    protected ParticleData initParticle(int id) {
        ParticleData data = new ParticleData();
        float twoPi = 2f * (float) Math.PI;

        Vector2f newPos = new Vector2f(
                Utils.randBetween(minOffset.x, maxOffset.x),
                Utils.randBetween(minOffset.y, maxOffset.y));
        Vector2f.add(newPos, Utils.randomPointInRing(new Vector2f(), minPositionSpread, maxPositionSpread), newPos);
        data.offset(newPos);

        Vector2f newVel = new Vector2f(
                Utils.randBetween(minVelocity.x, maxVelocity.x),
                Utils.randBetween(minVelocity.y, maxVelocity.y));
        Vector2f.add(newVel, Utils.randomPointInRing(new Vector2f(), minVelocitySpread, maxVelocitySpread), newVel);
        if (newPos.lengthSquared() > 0f) {
            newPos.normalise();
            newPos.scale(Utils.randBetween(minRadialVelocity, maxRadialVelocity));
            Vector2f.add(newVel, newPos, newVel);
        }
        data.velocity(newVel);

        Vector2f newAcc = new Vector2f(
                Utils.randBetween(minAcceleration.x, maxAcceleration.x),
                Utils.randBetween(minAcceleration.y, maxAcceleration.y));
        Vector2f.add(newAcc, Utils.randomPointInRing(new Vector2f(), minAccelerationSpread, maxAccelerationSpread), newAcc);
        if (newPos.lengthSquared() > 0f) {
            newPos.normalise();
            newPos.scale(Utils.randBetween(minRadialAcceleration, maxRadialAcceleration));
            Vector2f.add(newAcc, newPos, newAcc);
        }
        data.acceleration(newAcc);

        float newSinXAmplitude = Utils.randBetween(minSinXAmplitude, maxSinXAmplitude);
        float newSinXFrequency = Utils.randBetween(minSinXFrequency, maxSinXFrequency) * twoPi;
        float newSinXPhase = Utils.randBetween(minSinXPhase, maxSinXPhase) * Misc.RAD_PER_DEG;
        data.sinusoidalXMotion(newSinXAmplitude, newSinXFrequency, newSinXPhase);

        float newSinYAmplitude = Utils.randBetween(minSinYAmplitude, maxSinYAmplitude);
        float newSinYFrequency = Utils.randBetween(minSinYFrequency, maxSinYFrequency) * twoPi;
        float newSinYPhase = Utils.randBetween(minSinYPhase, maxSinYPhase) * Misc.RAD_PER_DEG;
        data.sinusoidalYMotion(newSinYAmplitude, newSinYFrequency, newSinYPhase);

        float newTheta = Utils.randBetween(minTheta, maxTheta) * Misc.RAD_PER_DEG;
        data.facing(newTheta);
        float newW = Utils.randBetween(minW, maxW) * Misc.RAD_PER_DEG;
        data.turnRate(newW);
        float newAlpha = Utils.randBetween(minAlpha, maxAlpha) * Misc.RAD_PER_DEG;
        data.turnAcceleration(newAlpha);

        float newRadialW = Utils.randBetween(minRadialW, maxRadialW) * Misc.RAD_PER_DEG;
        data.revolutionRate(newRadialW);
        float newRadialAlpha = Utils.randBetween(minRadialAlpha, maxRadialAlpha) * Misc.RAD_PER_DEG;
        data.revolutionAcceleration(newRadialAlpha);

        float[] newSizeDataX = new float[3];
        float[] newSizeDataY = new float[3];
        for (int j = 0; j < 3; j++) {
            newSizeDataX[j] = Utils.randBetween(minSizeDataX[j], maxSizeDataX[j]);
            newSizeDataY[j] = syncSize ? newSizeDataX[j] : Utils.randBetween(minSizeDataY[j], maxSizeDataY[j]);
        }
        data.size(newSizeDataX[0], newSizeDataY[0]);
        data.growthRate(newSizeDataX[1], newSizeDataY[1]);
        data.growthAcceleration(newSizeDataX[2], newSizeDataY[2]);

        float[] newStartColor = new float[4];
        for (int j = 0; j < 4; j++) {
            newStartColor[j] = startColor[j] + Utils.randBetween(-startColorRandom[j]/2f, startColorRandom[j]/2f);
        }
        data.colorHSVA(newStartColor);

        float[] newColorShift = new float[4];
        for (int j = 0; j < 4; j++) {
            newColorShift[j] = Utils.randBetween(minColorShift[j], maxColorShift[j]);
        }
        data.hueShift(newColorShift[0]).saturationShift(newColorShift[1]).colorValueShift(newColorShift[2]).alphaShift(newColorShift[3]);

        float newFadeIn = Utils.randBetween(minFadeIn, maxFadeIn);
        float newFadeOut = Utils.randBetween(minFadeOut, maxFadeOut);
        float newLife = Utils.randBetween(minLife, maxLife);
        data.fadeTime(newFadeIn, newFadeOut).life(newLife);

        return data;
    }

    /**
     * Loads properties from a JSON object.
     *
     * @param json JSON object to load from.
     * @throws JSONException if the provided {@code json} is malformed.
     */
    public void fromJSON(JSONObject json) throws JSONException {
        float[] life = Utils.readJSONArrayOrFloat(json, "life", new float[] {1f, 1f});
        life(life[0], life[1]);
        float[] fadeIn = Utils.readJSONArrayOrFloat(json, "fadeIn", new float[] {0f, 0f});
        float[] fadeOut = Utils.readJSONArrayOrFloat(json, "fadeOut", new float[] {0f, 0f});
        fadeTime(fadeIn[0], fadeIn[1], fadeOut[0], fadeOut[1]);
        float[] offsetX = Utils.readJSONArrayOrFloat(json, "offsetX", new float[] {0f, 0f});
        float[] offsetY = Utils.readJSONArrayOrFloat(json, "offsetY", new float[] {0f, 0f});
        offset(offsetX[0], offsetX[1], offsetY[0], offsetY[1]);
        float[] velocityX = Utils.readJSONArrayOrFloat(json, "velocityX", new float[] {0f, 0f});
        float[] velocityY = Utils.readJSONArrayOrFloat(json, "velocityY", new float[] {0f, 0f});
        velocity(velocityX[0], velocityX[1], velocityY[0], velocityY[1]);
        float[] accelerationX = Utils.readJSONArrayOrFloat(json, "accelerationX", new float[] {0f, 0f});
        float[] accelerationY = Utils.readJSONArrayOrFloat(json, "accelerationY", new float[] {0f, 0f});
        acceleration(accelerationX[0], accelerationX[1], accelerationY[0], accelerationY[1]);
        float[] circleOffset = Utils.readJSONArrayOrFloat(json, "circleOffset", new float[] {0f, 0f});
        circleOffset(circleOffset[0], circleOffset[1]);
        float[] circleVelocity = Utils.readJSONArrayOrFloat(json, "circleVelocity", new float[] {0f, 0f});
        circleVelocity(circleVelocity[0], circleVelocity[1]);
        float[] circleAcceleration = Utils.readJSONArrayOrFloat(json, "circleAcceleration", new float[] {0f, 0f});
        circleAcceleration(circleAcceleration[0], circleAcceleration[1]);
        float[] facing = Utils.readJSONArrayOrFloat(json, "facing", new float[] {0f, 0f});
        facing(facing[0], facing[1]);
        float[] turnRate = Utils.readJSONArrayOrFloat(json, "turnRate", new float[] {0f, 0f});
        turnRate(turnRate[0], turnRate[1]);
        float[] turnAcceleration = Utils.readJSONArrayOrFloat(json, "turnAcceleration", new float[] {0f, 0f});
        turnAcceleration(turnAcceleration[0], turnAcceleration[1]);
        float[] sizeX = Utils.readJSONArrayOrFloat(json, "sizeX", new float[] {25f, 25f});
        float[] sizeY = Utils.readJSONArrayOrFloat(json, "sizeY", new float[] {25f, 25f});
        size(sizeX[0], sizeX[1], sizeY[0], sizeY[1]);
        float[] growthRateX = Utils.readJSONArrayOrFloat(json, "growthRateX", new float[] {0f, 0f});
        float[] growthRateY = Utils.readJSONArrayOrFloat(json, "growthRateY", new float[] {0f, 0f});
        growthRate(growthRateX[0], growthRateX[1], growthRateY[0], growthRateY[1]);
        float[] growthAccelerationX = Utils.readJSONArrayOrFloat(json, "growthAccelerationX", new float[] {0f, 0f});
        float[] growthAccelerationY = Utils.readJSONArrayOrFloat(json, "growthAccelerationY", new float[] {0f, 0f});
        growthAcceleration(growthAccelerationX[0], growthAccelerationX[1], growthAccelerationY[0], growthAccelerationY[1]);
        float[] color = Utils.readJSONArrayOrFloat(json, "color", new float[] {1f, 1f, 1f, 1f}, false);
        color(color[0], color[1], color[2], color[3]);
        float[] randomHSVA = Utils.readJSONArrayOrFloat(json, "randomHSVA", new float[] {0f, 0f, 0f, 0f}, false);
        randomHSVA(randomHSVA);
        float[] hueShift = Utils.readJSONArrayOrFloat(json, "hueShift", new float[] {0f, 0f});
        hueShift(hueShift[0], hueShift[1]);
        float[] saturationShift = Utils.readJSONArrayOrFloat(json, "saturationShift", new float[] {0f, 0f});
        saturationShift(saturationShift[0], saturationShift[1]);
        float[] colorValueShift = Utils.readJSONArrayOrFloat(json, "colorValueShift", new float[] {0f, 0f});
        colorValueShift(colorValueShift[0], colorValueShift[1]);
        float[] alphaShift = Utils.readJSONArrayOrFloat(json, "alphaShift", new float[] {0f, 0f});
        alphaShift(alphaShift[0], alphaShift[1]);
        float[] radialVelocity = Utils.readJSONArrayOrFloat(json, "radialVelocity", new float[] {0f, 0f});
        radialVelocity(radialVelocity[0], radialVelocity[1]);
        float[] radialAcceleration = Utils.readJSONArrayOrFloat(json, "radialAcceleration", new float[] {0f, 0f});
        radialAcceleration(radialAcceleration[0], radialAcceleration[1]);
        float[] revolutionRate = Utils.readJSONArrayOrFloat(json, "revolutionRate", new float[] {0f, 0f});
        revolutionRate(revolutionRate[0], revolutionRate[1]);
        float[] revolutionAcceleration = Utils.readJSONArrayOrFloat(json, "revolutionAcceleration", new float[] {0f, 0f});
        revolutionAcceleration(revolutionAcceleration[0], revolutionAcceleration[1]);
        float[] sinXAmplitude = Utils.readJSONArrayOrFloat(json, "sinXAmplitude", new float[] {0f, 0f});
        float[] sinXFrequency = Utils.readJSONArrayOrFloat(json, "sinXFrequency", new float[] {0f, 0f});
        float[] sinXPhase = Utils.readJSONArrayOrFloat(json, "sinXPhase", new float[] {0f, 0f});
        sinusoidalMotionX(sinXAmplitude[0], sinXAmplitude[1], sinXFrequency[0], sinXFrequency[1], sinXPhase[0], sinXPhase[1]);
        float[] sinYAmplitude = Utils.readJSONArrayOrFloat(json, "sinYAmplitude", new float[] {0f, 0f});
        float[] sinYFrequency = Utils.readJSONArrayOrFloat(json, "sinYFrequency", new float[] {0f, 0f});
        float[] sinYPhase = Utils.readJSONArrayOrFloat(json, "sinYPhase", new float[] {0f, 0f});
        sinusoidalMotionY(sinYAmplitude[0], sinYAmplitude[1], sinYFrequency[0], sinYFrequency[1], sinYPhase[0], sinYPhase[1]);
    }

    /**
     *  Writes this emitter's properties to a file with the given name. The directory is specified in
     *  {@code particleengine.json}.
     *
     * @param name File name to use
     */
    public void writeJSON(String name) {
        String directory = ParticleEngineModPlugin.savedEmittersDirectory;
        if (directory == null) {
            log.warn("Saving disabled as [savedEmittersDirectory] couldn't be found in particleengine.json.");
            return;
        }
        String fileLoc = directory + "/" + name;
        if (!fileLoc.endsWith(".json")) {
            fileLoc += ".json";
        }
        JSONObject json = new JSONObject();
        try {
            putIfNotDefault(json, "life", minLife, maxLife, 1f);
            putIfNotDefault(json, "fadeIn", minFadeIn, maxFadeIn, 0f);
            putIfNotDefault(json, "fadeOut", minFadeOut, maxFadeOut, 0f);
            putIfNotDefault(json, "offsetX", minOffset.x, maxOffset.x, 0f);
            putIfNotDefault(json, "offsetY", minOffset.y, maxOffset.y, 0f);
            putIfNotDefault(json, "velocityX", minVelocity.x, maxVelocity.x, 0f);
            putIfNotDefault(json, "velocityY", minVelocity.y, maxVelocity.y, 0f);
            putIfNotDefault(json, "accelerationX", minAcceleration.x, maxAcceleration.x, 0f);
            putIfNotDefault(json, "accelerationY", minAcceleration.y, maxAcceleration.y, 0f);
            putIfNotDefault(json, "circleOffset", minPositionSpread, maxPositionSpread, 0f);
            putIfNotDefault(json, "circleVelocity", minVelocitySpread, maxVelocitySpread, 0f);
            putIfNotDefault(json, "circleAcceleration", minAccelerationSpread, maxAccelerationSpread, 0f);
            putIfNotDefault(json, "facing", minTheta, maxTheta, 0f);
            putIfNotDefault(json, "turnRate", minW, maxW, 0f);
            putIfNotDefault(json, "turnAcceleration", minAlpha, maxAlpha, 0f);
            putIfNotDefault(json, "sizeX", minSizeDataX[0], maxSizeDataX[0], 25f);
            putIfNotDefault(json, "sizeY", minSizeDataY[0], maxSizeDataY[0], 25f);
            putIfNotDefault(json, "growthRateX", minSizeDataX[1], maxSizeDataX[1], 0f);
            putIfNotDefault(json, "growthRateY", minSizeDataY[1], maxSizeDataY[1], 0f);
            putIfNotDefault(json, "growthAccelerationX", minSizeDataX[2], maxSizeDataX[2], 0f);
            putIfNotDefault(json, "growthAccelerationY", minSizeDataY[2], maxSizeDataY[2], 0f);
            float[] color = new float[4];
            Utils.toRGBA(startColor, color);
            json.put("color", new JSONArray(color));
            float[] randomHSVA = startColorRandom;
            if (randomHSVA[0] != 0f || randomHSVA[1] != 0f || randomHSVA[2] != 0f || randomHSVA[3] != 0f) {
                json.put("randomHSVA", new JSONArray(randomHSVA));
            }
            putIfNotDefault(json, "hueShift", minColorShift[0], maxColorShift[0],0f);
            putIfNotDefault(json, "saturationShift", minColorShift[1], maxColorShift[1],0f);
            putIfNotDefault(json, "colorValueShift", minColorShift[2], maxColorShift[2],0f);
            putIfNotDefault(json, "alphaShift", minColorShift[3], maxColorShift[3],0f);
            putIfNotDefault(json, "radialVelocity", minRadialVelocity, maxRadialVelocity, 0f);
            putIfNotDefault(json, "radialAcceleration", minRadialAcceleration, maxRadialAcceleration, 0f);
            putIfNotDefault(json, "revolutionRate", minRadialW, maxRadialW, 0f);
            putIfNotDefault(json, "revolutionAcceleration", minRadialAlpha, maxRadialAlpha, 0f);
            putIfNotDefault(json, "sinXAmplitude", minSinXAmplitude, maxSinXAmplitude, 0f);
            putIfNotDefault(json, "sinXFrequency", minSinXFrequency, maxSinXFrequency, 0f);
            putIfNotDefault(json, "sinXPhase", minSinXPhase, maxSinXPhase, 0f);
            putIfNotDefault(json, "sinYAmplitude", minSinYAmplitude, maxSinYAmplitude, 0f);
            putIfNotDefault(json, "sinYFrequency", minSinYFrequency, maxSinYFrequency, 0f);
            putIfNotDefault(json, "sinYPhase", minSinYPhase, maxSinYPhase, 0f);

        } catch (JSONException ignore) {}
        try (PrintWriter writer = new PrintWriter(fileLoc)) {
            log.info(String.format("Writing emitter properties to %s", fileLoc));
            writer.write(json.toString(4));
        }
        catch (Exception e) {
            log.error("Failed to write an emitter to file. Does the directory exist?", e);
        }
    }

    private void putIfNotDefault(JSONObject json, String name, float min, float max, float defaultValue) throws JSONException {
        if (min == defaultValue && max == defaultValue) return;
        json.put(name, Utils.toFloatOrPairArray(min, max));
    }
}
