package particleengine;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.FloatBuffer;

/**
 * An abstraction of a set of particles of the same {@link ParticleType}. Contains instructions on how to generate
 * particles, but does not store any actual particle data. To generate particles using a {@code Cluster}, call {@link Particles#generate}
 * on a {@code Cluster} after setting the {@code Cluster}'s data.
 */
public class Cluster {
    final int sfactor, dfactor, blendMode, count;
    final SpriteAPI sprite;
    float minLife = 1f, maxLife = 1f;

    private final Vector2f position = new Vector2f(), velocity = new Vector2f(), acceleration = new Vector2f(), offset = new Vector2f();
    /**
     * Random point in circle of radius positionSpread is added to position. Same for velocity and acceleration.
     */
    private float positionSpread = 0f, velocitySpread = 0f, accelerationSpread = 0f;
    /**
     * Alpha is angular acceleration, NOT image alpha
     */
    private float minTheta = 0f, maxTheta = 0f, minW = 0f, maxW = 0f, minAlpha = 0f, maxAlpha = 0f;
    private float minSize = 25f, maxSize = 25f, minSizeV = 0f, maxSizeV = 0f, minSizeA = 0f, maxSizeA = 0f;
    private float minFadeIn = 0.1f, maxFadeIn = 0.1f, minFadeOut = 0.1f, maxFadeOut = 0.1f;
    private final float[] minStartColor = new float[] {1f, 1f, 1f, 1f}, maxStartColor = new float[] {1f, 1f, 1f, 1f};
    private final float[] minEndColor = new float[] {0f, 0f, 0f, 0f}, maxEndColor = new float[] {0f, 0f, 0f, 0f};
    /**
     * Velocity and acceleration amounts pointed outwards. No effect if positionSpread is 0.
     */
    private float minRadialVelocity = 0f, maxRadialVelocity = 0f, minRadialAcceleration = 0f, maxRadialAcceleration = 0f;
    private float minRadialW = 0f, maxRadialW = 0f, minRadialAlpha = 0f, maxRadialAlpha = 0f;
    private float minSinXAmplitude = 0f, maxSinXAmplitude = 0f, minSinXFrequency = 0f, maxSinXFrequency = 0f, minSinXPhase = 0f, maxSinXPhase = 0f;
    private float minSinYAmplitude = 0f, maxSinYAmplitude = 0f, minSinYFrequency = 0f, maxSinYFrequency = 0f, minSinYPhase = 0f, maxSinYPhase = 0f;
    private final Vector2f xAxis = new Vector2f(1f, 0f);

    Cluster(
            int count,
            SpriteAPI sprite,
            int sfactor,
            int dfactor,
            int blendMode) {
        this.count = count;
        this.sfactor = sfactor; // blending
        this.dfactor = dfactor; // blending
        this.blendMode = blendMode; // blending
        this.sprite = sprite;
    }

    /**
     * Sets the minimum and maximum lifetime of particles in this {@code Cluster}. Particles exceeding their lifespan
     * are no longer rendered and deleted at some later time.
     *
     * @param minLife Minimum lifetime, in seconds
     * @param maxLife Maximum lifetime, in seconds
     */
    public void setLife(float minLife, float maxLife) {
        this.minLife = minLife;
        this.maxLife = maxLife;
    }


    /**
     * Sets the fade-in and fade-out times for particles in this cluster. Particles' alpha values rise linearly from
     * {@code 0} to {@code 1} within the fade-in window, and fall linearly from {@code 1} to {@code 0} within
     * the fade-out window.
     *
     * @param minFadeIn  Minimum fade-in time, in seconds
     * @param maxFadeIn  Maximum fade-in time, in seconds
     * @param minFadeOut Minimum fade-out time, in seconds
     * @param maxFadeOut Maximum fade-out time, in seconds
     */
    public void setFadeTime(float minFadeIn, float maxFadeIn, float minFadeOut, float maxFadeOut) {
        this.minFadeIn = minFadeIn;
        this.minFadeOut = minFadeOut;
        this.maxFadeIn = maxFadeIn;
        this.maxFadeOut = maxFadeOut;
    }

    /**
     * Uniformly sets the initial position, velocity, and acceleration of every particle in this cluster.
     * To randomize particles' position data, use e.g. {@link Cluster#setPositionSpread}.
     *
     * @param position     Initial position, in world units, absolute. To set positive relative to
     *                     the cluster's axes, use {@link Cluster#setOffset}.
     * @param velocity     Initial velocity, in world units / s. Relative to this cluster's axes.
     * @param acceleration Acceleration, in world units / s^2. Relative to this cluster's axes.
     */
    public void setPosVelAcc(Vector2f position, Vector2f velocity, Vector2f acceleration) {
        this.position.set(position);
        this.velocity.set(velocity);
        this.acceleration.set(acceleration);
    }

    /**
     * Uniformly sets the initial position of every particle in this cluster. The position is defined in the global
     * coordinate system. To set position relative to this cluster's axes, use {@link Cluster#setOffset}.
     *
     * @param x Initial x-position, in absolute world units.
     * @param y Initial y-position, in absolute world units.
     */
    public void setPosition(float x, float y) {
        this.position.set(x, y);
    }

    /** @see Cluster#setPosition(float, float) */
    public void setPosition(Vector2f position) {
        this.position.set(position);
    }

    /**
     * Uniformly sets the initial velocity of every particle in this cluster. The velocity is defined in the cluster's
     * local coordinate system, set using {@link Cluster#setAxes}.
     *
     * @param x Initial x-velocity. Relative to this cluster's axes.
     * @param y Initial y-velocity. Relative to this cluster's axes.
     */
    public void setVelocity(float x, float y) {
        this.velocity.set(x, y);
    }

    /** @see Cluster#setVelocity(float, float) */
    public void setVelocity(Vector2f velocity) {
        this.velocity.set(velocity);
    }

    /**
     * Uniformly sets the initial acceleration of every particle in this cluster. The acceleration is defined in the cluster's
     * local coordinate system, set using {@link Cluster#setAxes}.
     *
     * @param x Initial x-acceleration. Relative to this cluster's axes.
     * @param y Initial y-acceleration. Relative to this cluster's axes.
     */
    public void setAcceleration(float x, float y) {
        this.acceleration.set(x, y);
    }

    /** @see Cluster#setAcceleration(float, float) */
    public void setAcceleration(Vector2f acceleration) {
        this.acceleration.set(acceleration);
    }

    /**
     *  Sets the position of this cluster relative to its axes. This is added to the cluster's absolute
     *  position. Use {@link Cluster#setAxes} to set a cluster's axes.
     *
     * @param x Offset along this cluster's x-axis.
     * @param y Offset along this cluster's y-axis.
     */
    public void setOffset(float x, float y) {
        this.offset.set(x, y);
    }

    /** Sets the axes of this cluster. All of this cluster's attributes, except for its {@code position}, are relative
     *  to its axes. Defaults to global axes, i.e. {@code (0, 1)} and {@code (1, 0)}.
     *
     * @param xAxis The cluster's x-axis. Doesn't need to be normalized. If degenerate, will be set to the default
     *              value {@code (0, 1)}. The cluster's y-axis will automatically be set to the
     *              unit vector that is counterclockwise perpendicular to {@code xAxis}.
     */
    public void setAxes(Vector2f xAxis) {
        if (xAxis.lengthSquared() <= 0f) {
            this.xAxis.set(1f, 0f);
            return;
        }

        xAxis.normalise(this.xAxis);
    }

    /**
     * Adds randomness to the positions of particles generated by this cluster. Generated particles will have a random
     * vector inside a circle of radius {@code spread} added to its position.
     *
     * @param spread Maximum radius of circle inside which a random vector will be chosen to be added to
     *                       each particle's position. This vector varies per particle.
     */
    public void setPositionSpread(float spread) {
        this.positionSpread = spread;
    }

    /**
     * Adds randomness to the velocities of particles generated by this cluster. Generated particles will have a random
     * vector inside a circle of radius {@code spread} added to its velocity.
     *
     * @param spread Maximum radius of circle inside which a random vector will be chosen to be added to
     *                       each particle's velocity. This vector varies per particle.
     */
    public void setVelocitySpread(float spread) {
        this.velocitySpread = spread;
    }

    /**
     * Adds randomness to the accelerations of particles generated by this cluster. Generated particles will have a random
     * vector inside a circle of radius {@code positionSpread} added to its acceleration.
     *
     * @param spread Maximum radius of circle inside which a random vector will be chosen to be added to
     *                       each particle's acceleration. This vector varies per particle.
     */
    public void setAccelerationSpread(float spread) {
        this.accelerationSpread = spread;
    }

    /** Sets minimum and maximum initial facing angles for this cluster. Each particle generated by this cluster will
     *  have an initial facing randomly chosen between the two given angles. A facing direction of {@code 0} is
     *  oriented along this cluster's x-axis.
     *
     * @param minAngle Minimum initial facing direction, in degrees.
     * @param maxAngle Maximum initial facing direction, in degrees.
     */
    public void setAngle(float minAngle, float maxAngle) {
        minTheta = minAngle;
        maxTheta = maxAngle;
    }

    /** Sets minimum and maximum initial turn rates for this cluster. Each particle generated by this cluster will
     *  have an initial turn rate randomly chosen between the two given numbers.
     * @param minRate Minimum initial turn rate, in degrees / s.
     * @param maxRate Maximum initial turn rate, in degrees / s.
     */
    public void setTurnRate(float minRate, float maxRate) {
        minW = minRate;
        maxW = maxRate;
    }

    /** Sets minimum and maximum turn rate accelerations for this cluster. Each particle generated by this cluster will have
     * a turn rate acceleration randomly chosen between the two given numbers.
      * @param minAcceleration Minimum initial turn rate acceleration, in degrees / s^2.
     * @param maxAcceleration Maximum initial turn rate acceleration, in degrees / s^2.
     */
    public void setTurnAcceleration(float minAcceleration, float maxAcceleration) {
        minAlpha = minAcceleration;
        maxAlpha = maxAcceleration;
    }

    /**
     * Sets the size of particles generated by this cluster. Each particle is given a size randomly chosen between
     * {@code minSize} and {@code maxSize}.
     *
     * @param minSize Minimum initial particle size, in world units.
     * @param maxSize Maximum initial particle size, in world units.
     */
    public void setSize(float minSize, float maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    /**
     * Sets the initial growth rate of particles generated by this cluster. Each particle is given
     * a growth rate randomly chosen between {@code minRate} and {@code maxRate}.
     *
     * @param minRate Minimum initial particle growth rate, in world units / s.
     * @param maxRate Maximum initial particle growth rate, in world units / s.
     */
    public void setGrowthRate(float minRate, float maxRate) {
        minSizeV = minRate;
        maxSizeV = maxRate;
    }

    /**
     * Sets the growth acceleration of particles generated by this cluster. Each particle is
     * given a growth acceleration randomly chosen between {@code minAcceleration} and {@code maxAcceleration}.
     *
     * @param minAcceleration Minimum particle growth acceleration, in world units / s^2.
     * @param maxAcceleration Maximum particle growth acceleration, in world units / s^2.
     */
    public void setGrowthAcceleration(float minAcceleration, float maxAcceleration) {
        minSizeA = minAcceleration;
        maxSizeA = maxAcceleration;
    }

    /**
     * Sets the initial color of every particle generated by this cluster.
     * @param r Initial red value, between 0 and 1.
     * @param g Initial green value, between 0 and 1.
     * @param b Initial blue value, between 0 and 1.
     * @param a Initial alpha value, between 0 and 1.
     */
    public void setStartColor(float r, float g, float b, float a) {
        minStartColor[0] = maxStartColor[0] = r;
        minStartColor[1] = maxStartColor[1] = g;
        minStartColor[2] = maxStartColor[2] = b;
        minStartColor[3] = maxStartColor[3] = a;
    }

    /** @see Cluster#setStartColor(float, float, float, float) */
    public void setStartColor(Color color) {
        color.getComponents(minStartColor);
        color.getComponents(maxStartColor);
    }

    /**
     * Sets the initial color of every particle generated by this cluster. Each particle's starting red value will be randomly
     * chosen between {@code minR} and {@code maxR}. The particle's other starting color channels are similarly chosen.
     * @param minR Minimum initial red value, between 0 and 1.
     * @param maxR Maximum initial red value, between 0 and 1.
     * @param minG Minimum initial green value, between 0 and 1.
     * @param maxG Maximum initial green value, between 0 and 1.
     * @param minB Minimum initial blue value, between 0 and 1.
     * @param maxB Maximum initial blue value, between 0 and 1.
     * @param minA Minimum initial alpha value, between 0 and 1.
     * @param maxA Maximum initial alpha value, between 0 and 1.
     */
    public void setStartColor(float minR, float maxR, float minG, float maxG, float minB, float maxB, float minA, float maxA) {
        minStartColor[0] = minR;
        maxStartColor[0] = maxR;
        minStartColor[1] = minG;
        maxStartColor[1] = maxG;
        minStartColor[2] = minB;
        maxStartColor[2] = maxB;
        minStartColor[3] = minA;
        maxStartColor[3] = maxA;
    }

    /**
     * @see Cluster#setStartColor(float, float, float, float, float, float, float, float)
     */
    public void setStartColor(Color minColor, Color maxColor) {
        minColor.getComponents(minStartColor);
        maxColor.getComponents(maxStartColor);
    }

    /**
     * Sets the ending color of every particle generated by this cluster. Each color channel of a particle
     * changes linearly from its initial value to its final value over the course of the particle's lifetime.
     *
     * @param r Ending red value, between 0 and 1.
     * @param g Ending green value, between 0 and 1.
     * @param b Ending blue value, between 0 and 1.
     * @param a Ending alpha value, between 0 and 1.
     */
    public void setEndColor(float r, float g, float b, float a) {
        minEndColor[0] = maxEndColor[0] = r;
        minEndColor[1] = maxEndColor[1] = g;
        minEndColor[2] = maxEndColor[2] = b;
        minEndColor[3] = maxEndColor[3] = a;
    }

    /**
     * @see Cluster#setEndColor(float, float, float, float) 
     */
    public void setEndColor(Color color) {
        color.getComponents(minEndColor);
        color.getComponents(maxEndColor);
    }

    /**
     * Sets the ending color of every particle generated by this cluster. Each particle's ending red value will be randomly
     * chosen between {@code minR} and {@code maxR}. The particle's other ending color channels are similarly chosen. Each
     * color channel of a particle changes linearly from its initial value to its final value over the course of the
     * particle's lifetime.
     *
     * @param minR Minimum ending red value, between 0 and 1.
     * @param maxR Maximum ending red value, between 0 and 1.
     * @param minG Minimum ending green value, between 0 and 1.
     * @param maxG Maximum ending green value, between 0 and 1.
     * @param minB Minimum ending blue value, between 0 and 1.
     * @param maxB Maximum ending blue value, between 0 and 1.
     * @param minA Minimum ending alpha value, between 0 and 1.
     * @param maxA Maximum ending alpha value, between 0 and 1.
     */
    public void setEndColor(float minR, float maxR, float minG, float maxG, float minB, float maxB, float minA, float maxA) {
        minEndColor[0] = minR;
        maxEndColor[0] = maxR;
        minEndColor[1] = minG;
        maxEndColor[1] = maxG;
        minEndColor[2] = minB;
        maxEndColor[2] = maxB;
        minEndColor[3] = minA;
        maxEndColor[3] = maxA;
    }

    /**
     * @see Cluster#setEndColor(float, float, float, float, float, float, float, float)
     */
    public void setEndColor(Color minColor, Color maxColor) {
        minColor.getComponents(minEndColor);
        maxColor.getComponents(maxEndColor);
    }

    /**
     * Adds velocity in the direction from the cluster's initial {@code position} to each individual particle's randomized position.
     * This effect causes particles to be pushed outwards from the cluster's {@code position}. Does not take the cluster's
     * {@code offset} into account.
     * No effect on clusters with no {@code offset} or {@code positionSpread}.
     *
     * @param minVelocity Minimum outward radial velocity, in world units / s.
     * @param maxVelocity Maximum outward radial velocity, in world units / s.
     */
    public void setRadialVelocity(float minVelocity, float maxVelocity) {
        this.minRadialVelocity = minVelocity;
        this.maxRadialVelocity = maxVelocity;
    }

    /**
     * Adds acceleration in the direction from the initial {@code position} to each individual particle's randomized position.
     * Does not take the cluster's {@code offset} into account.
     *
     * @param minAcceleration Minimum outward radial acceleration, in world units / s^2.
     * @param maxAcceleration Maximum outward radial acceleration, in world units / s^2.
     * @see Cluster#setRadialVelocity(float, float)
     */
    public void setRadialAcceleration(float minAcceleration, float maxAcceleration) {
        this.minRadialAcceleration = minAcceleration;
        this.maxRadialAcceleration = maxAcceleration;
    }

    /**
     * Causes particles to revolve around the cluster's {@code position}. Does not take the cluster's {@code offset}
     * into account. This effect is applied after all other movement effects.
     *
     * @param minRate     Minimum initial revolution velocity, in degrees / s.
     * @param maxRate     Maximum initial revolution velocity, in degrees / s.
     * @param minAcceleration Minimum revolution acceleration, in degrees / s^2.
     * @param maxAcceleration Maximum revolution acceleration, in degrees / s^2.
     */
    public void setRadialRevolution(float minRate, float maxRate, float minAcceleration, float maxAcceleration) {
        this.minRadialW = minRate;
        this.maxRadialW = maxRate;
        this.minRadialAlpha = minAcceleration;
        this.maxRadialAlpha = maxAcceleration;
    }

    /**
     * Adds periodic motion along the cluster's x-axis. For each particle, if its phase is non-zero, will also
     * translate that particle so that its initial position is unchanged at {@code t = 0}.
     *
     * @param minAmplitude Minimum amplitude of periodic motion along the cluster's x-axis, in world units.
     * @param maxAmplitude Maximum amplitude of periodic motion along the cluster's x-axis, in world units.
     * @param minFrequency Minimum number of complete cycles per second along the cluster's x-axis.
     * @param maxFrequency Maximum of complete cycles per second along the cluster's x-axis.
     * @param minPhase     Minimum initial phase of periodic motion along the cluster's x-axis, in degrees.
     * @param maxPhase     Maximum initial phase of periodic motion along the cluster's x-axis, in degrees.
     */
    public void setSinusoidalMotionX(float minAmplitude, float maxAmplitude, float minFrequency, float maxFrequency, float minPhase, float maxPhase) {
        this.minSinXAmplitude = minAmplitude;
        this.maxSinXAmplitude = maxAmplitude;
        this.minSinXFrequency = minFrequency;
        this.maxSinXFrequency = maxFrequency;
        this.minSinXPhase = minPhase;
        this.maxSinXPhase = maxPhase;
    }

    /**
     * @param minAmplitude Minimum amplitude of periodic motion along the cluster's y-axis, in world units.
     * @param maxAmplitude Maximum amplitude of periodic motion along the cluster's  y-axis, in world units.
     * @param minFrequency Minimum number of complete cycles per second along the cluster's y-axis.
     * @param maxFrequency Maximum of complete cycles per second along the cluster's y-axis.
     * @param minPhase     Minimum initial phase of periodic motion along the cluster's y-axis, in degrees.
     * @param maxPhase     Maximum initial phase of periodic motion along the cluster's y-axis, in degrees.
     * @see Cluster#setSinusoidalMotionX(float, float, float, float, float, float)
     */
    public void setSinusoidalMotionY(float minAmplitude, float maxAmplitude, float minFrequency, float maxFrequency, float minPhase, float maxPhase) {
        this.minSinYAmplitude = minAmplitude;
        this.maxSinYAmplitude = maxAmplitude;
        this.minSinYFrequency = minFrequency;
        this.maxSinYFrequency = maxFrequency;
        this.minSinYPhase = minPhase;
        this.maxSinYPhase = maxPhase;
    }

    FloatBuffer generate(float startTime) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(count * Particles.FLOATS_PER_PARTICLE);
        float twoPi = 2f * (float) Math.PI;
        float emitterAngle = Misc.getAngleInDegrees(xAxis) * Misc.RAD_PER_DEG;

        for (int i = 0; i < count; i++) {
            Vector2f newPos = new Vector2f();
            Vector2f.add(position, Utils.randomPointInCircle(newPos, positionSpread), newPos);
            Vector2f.add(newPos, offset, newPos);
            Vector2f radialDir = Misc.getDiff(newPos, position);

            Vector2f newVel = new Vector2f();
            Vector2f.add(velocity, Utils.randomPointInCircle(newVel, velocitySpread), newVel);
            if (radialDir.lengthSquared() > 0f) {
                radialDir.normalise();
                radialDir.scale(Utils.randBetween(minRadialVelocity, maxRadialVelocity));
                Vector2f.add(newVel, radialDir, newVel);
            }

            Vector2f newAcc = new Vector2f();
            Vector2f.add(acceleration, Utils.randomPointInCircle(newAcc, accelerationSpread), newAcc);
            if (radialDir.lengthSquared() > 0f) {
                radialDir.normalise();
                radialDir.scale(Utils.randBetween(minRadialAcceleration, maxRadialAcceleration));
                Vector2f.add(newAcc, radialDir, newAcc);
            }

//            Vector2f newAmplitudes = Utils.toStandardBasis(
//                    new Vector2f(Utils.randBetween(minSinXAmplitude, maxSinXAmplitude),
//                            Utils.randBetween(minSinYAmplitude, maxSinYAmplitude)),
//                    xAxis, yAxis);
            float newSinXAmplitude = Utils.randBetween(minSinXAmplitude, maxSinXAmplitude);
            float newSinXFrequency = Utils.randBetween(minSinXFrequency, maxSinXFrequency) * twoPi;
            float newSinXPhase = Utils.randBetween(minSinXPhase, maxSinXPhase) * Misc.RAD_PER_DEG;
            float newSinYAmplitude = Utils.randBetween(minSinYAmplitude, maxSinYAmplitude);
            float newSinYFrequency = Utils.randBetween(minSinYFrequency, maxSinYFrequency) * twoPi;
            float newSinYPhase = Utils.randBetween(minSinYPhase, maxSinYPhase) * Misc.RAD_PER_DEG;

            float newTheta = Utils.randBetween(minTheta, maxTheta) * Misc.RAD_PER_DEG;
            float newW = Utils.randBetween(minW, maxW) * Misc.RAD_PER_DEG;
            float newAlpha = Utils.randBetween(minAlpha, maxAlpha) * Misc.RAD_PER_DEG;

            float newRadialW = Utils.randBetween(minRadialW, maxRadialW) * Misc.RAD_PER_DEG;
            float newRadialAlpha = Utils.randBetween(minRadialAlpha, maxRadialAlpha) * Misc.RAD_PER_DEG;

            float newSize = Utils.randBetween(minSize, maxSize);
            float newSizeV = Utils.randBetween(minSizeV, maxSizeV);
            float newSizeA = Utils.randBetween(minSizeA, maxSizeA);

            float[] newStartColor = new float[4], newEndColor = new float[4];
            for (int j = 0; j < 4; j++) {
                newStartColor[j] = Utils.randBetween(minStartColor[j], maxStartColor[j]);
                newEndColor[j] = Utils.randBetween(minEndColor[j], maxEndColor[j]);
            }

            float newFadeIn = Utils.randBetween(minFadeIn, maxFadeIn);
            float newFadeOut = Utils.randBetween(minFadeOut, maxFadeOut);
            float newLife = Utils.randBetween(minLife, maxLife);

            buffer.put(
                    new float[]{
                            newPos.x,
                            newPos.y,
                            position.x,
                            position.y,
                            emitterAngle,
                            newVel.x,
                            newVel.y,
                            newAcc.x,
                            newAcc.y,
                            newSinXAmplitude,
                            newSinXFrequency,
                            newSinXPhase,
                            newSinYAmplitude,
                            newSinYFrequency,
                            newSinYPhase,
                            newTheta,
                            newW,
                            newAlpha,
                            newRadialW,
                            newRadialAlpha,
                            newSize,
                            newSizeV,
                            newSizeA,
                            newStartColor[0],
                            newStartColor[1],
                            newStartColor[2],
                            newStartColor[3],
                            newEndColor[0],
                            newEndColor[1],
                            newEndColor[2],
                            newEndColor[3],
                            newFadeIn,
                            newFadeOut,
                            startTime,
                            startTime + newLife
                    }
            );
        }
        buffer.flip();
        return buffer;
    }
}
