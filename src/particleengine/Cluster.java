package particleengine;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;

/**
 * An abstraction of a set of particles of the same {@link ParticleType}. Contains instructions on how to generate
 * particles, but does not store any actual particle data. To generate particles using a {@code Cluster}, call {@link ParticleEngine#generateParticles}
 * on a {@code Cluster} after setting the {@code Cluster}'s data.
 */
public class Cluster {
    final int sfactor, dfactor, blendMode, count;
    final SpriteAPI sprite;
    float minLife = 1f, maxLife = 1f;

    private Vector2f position = new Vector2f(), velocity = new Vector2f(), acceleration = new Vector2f();
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
    private float[] inColor = new float[]{1f, 1f, 1f, 1f}, outColor = new float[4];
    private float[] inColorSpread = new float[4], outColorSpread = new float[4];
    /**
     * Velocity and acceleration amounts pointed outwards. No effect if positionSpread is 0.
     */
    private float minRadialVelocity = 0f, maxRadialVelocity = 0f, minRadialAcceleration = 0f, maxRadialAcceleration = 0f;
    private float minRadialTheta = 0f, maxRadialTheta = 0f, minRadialW = 0f, maxRadialW = 0f, minRadialAlpha = 0f, maxRadialAlpha = 0f;
    private float minSinXAmplitude = 0f, maxSinXAmplitude = 0f, minSinXFrequency = 0f, maxSinXFrequency = 0f, minSinXPhase = 0f, maxSinXPhase = 0f;
    private float minSinYAmplitude = 0f, maxSinYAmplitude = 0f, minSinYFrequency = 0f, maxSinYFrequency = 0f, minSinYPhase = 0f, maxSinYPhase = 0f;

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
    public void setFade(float minFadeIn, float maxFadeIn, float minFadeOut, float maxFadeOut) {
        this.minFadeIn = minFadeIn;
        this.minFadeOut = minFadeOut;
        this.maxFadeIn = maxFadeIn;
        this.maxFadeOut = maxFadeOut;
    }

    /**
     * Uniformly sets the initial position, velocity, and acceleration of every particle in this cluster.
     * To randomize particles' positions, velocities, or accelerations, use {@link Cluster#setPositionSpreadData}.
     *
     * @param position     Initial position, in world units.
     * @param velocity     Initial velocity, in world units / s.
     * @param acceleration Acceleration, in world units / s^2.
     */
    public void setPositionData(Vector2f position, Vector2f velocity, Vector2f acceleration) {
        this.position = position;
        this.velocity = velocity;
        this.acceleration = acceleration;
    }

    /**
     * Adds randomness to particles' positions, velocities, and accelerations within this cluster.
     * When generated, each particle will have a random vector inside a circle of radius {@code positionSpread}
     * added to its position, a random vector inside a circle of radius {@code velocitySpread} added to its
     * velocity, and a random vector inside a circle of radius {@code accelerationSpread} added to its
     * acceleration.
     *
     * @param positionSpread     Starting position deviation. Varies per particle.
     * @param velocitySpread     Starting velocity deviation. Varies per particle.
     * @param accelerationSpread Acceleration deviation. Varies per particle.
     */
    public void setPositionSpreadData(float positionSpread, float velocitySpread, float accelerationSpread) {
        this.positionSpread = positionSpread;
        this.velocitySpread = velocitySpread;
        this.accelerationSpread = accelerationSpread;
    }

    /**
     * Sets minimum and maximum facing direction, facing velocity, and facing acceleration. Each particle will
     * be given an angle between {@code minTheta} and {@code maxTheta}, an angular velocity between {@code minW}
     * and {@code maxW}, and an angular acceleration between {@code minAlpha} and {@code maxAlpha}. These define
     * the rotation of the image texture about the image center and do not affect the particles' world position
     * in any way.
     *
     * @param minTheta Minimum initial facing direction, in degrees.
     * @param maxTheta Maximum initial facing direction, in degrees.
     * @param minW     Minimum initial facing velocity, in degrees / s.
     * @param maxW     Maximum initial facing velocity, in degrees / s.
     * @param minAlpha Minimum facing acceleration, in degrees / s^2.
     * @param maxAlpha Maximum facing acceleration, in degrees / s^2.
     */
    public void setAngleData(float minTheta, float maxTheta, float minW, float maxW, float minAlpha, float maxAlpha) {
        this.minTheta = minTheta;
        this.maxTheta = maxTheta;
        this.minW = minW;
        this.maxW = maxW;
        this.minAlpha = minAlpha;
        this.maxAlpha = maxAlpha;
    }

    /**
     * Sets minimum and maximum size, growth rate, and growth acceleration in absolute world units. Each particle will be given a size
     * between {@code minSize} and {@code maxSize}, a growth rate between {@code minSizeV} and {@code maxSizeV}, and a
     * growth acceleration between {@code minSizeA} and {@code maxSizeA}.
     *
     * @param minSize  Minimum initial size, in world units.
     * @param maxSize  Maximum initial size, in world units.
     * @param minSizeV Minimum initial growth rate, in world units / s.
     * @param maxSizeV Maximum initial growth rate, in world units / s.
     * @param minSizeA Minimum growth acceleration, in world units / s^2.
     * @param maxSizeA Maximum world acceleration, in world units / s^2.
     */
    public void setSizeData(float minSize, float maxSize, float minSizeV, float maxSizeV, float minSizeA, float maxSizeA) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.minSizeV = minSizeV;
        this.maxSizeV = maxSizeV;
        this.minSizeA = minSizeA;
        this.maxSizeA = maxSizeA;
    }

    /**
     * Sets the starting and ending colors of particles in this cluster. Color shifts linearly from {@code inColor}
     * to {@code outColor} over the course of each particle's lifecycle.
     *
     * @param inColor  4-element float array with RGBA components between 0 and 1. Particles' color at start of life.
     * @param outColor 4-element float array with RGBA components between 0 and 1. Particles' color at end of life.
     */
    public void setColorData(float[] inColor, float[] outColor) {
        this.inColor = inColor;
        this.outColor = outColor;
    }

    /**
     * Randomizes per-particle color. {@code inColorSpread} randomizes each particle's initial color, while
     * {@code outColorSpread} randomizes each particle's terminal color.
     *
     * @param inColorSpread  4-element float array with RGBA components between 0 and 1.
     *                       Perturbs {@code inColor[i]} by a random number in {@code [-inColorSpread[i]/2, outcolorSpread[i]/2]}.
     * @param outColorSpread 4-element float array with RGBA components between 0 and 1.
     *                       Perturbs {@code outColor[i]} by a random number in {@code [-outColorSpread[i]/2, outcolorSpread[i]/2]}.
     */
    public void setColorSpreadData(float[] inColorSpread, float[] outColorSpread) {
        this.inColorSpread = inColorSpread;
        this.outColorSpread = outColorSpread;
    }

    /**
     * For clusters with a non-zero {@code positionSpread}, adds velocity in the direction from the initial {@code position}
     * to each individual particle's randomized position. This effect causes particles to be pushed outwards from the cluster's
     * {@code position}. No effect on clusters with no {@code positionSpread}.
     *
     * @param minRadialVelocity Minimum outward radial velocity, in world units / s.
     * @param maxRadialVelocity Maximum outward radial velocity, in world units / s.
     */
    public void setRadialVelocity(float minRadialVelocity, float maxRadialVelocity) {
        this.minRadialVelocity = minRadialVelocity;
        this.maxRadialVelocity = maxRadialVelocity;
    }

    /**
     * Adds acceleration in the direction from the initial {@code position} to each individual particle's randomized position.
     *
     * @param minRadialAcceleration Minimum outward radial acceleration, in world units / s^2.
     * @param maxRadialAcceleration Maximum outward radial acceleration, in world units / s^2.
     * @see Cluster#setRadialVelocity(float, float)
     */
    public void setRadialAcceleration(float minRadialAcceleration, float maxRadialAcceleration) {
        this.minRadialAcceleration = minRadialAcceleration;
        this.maxRadialAcceleration = maxRadialAcceleration;
    }

    /**
     * Causes particles to revolve around the cluster's {@code position}. This effect is applied after all other
     * movement effects.
     *
     * @param minRadialTheta Minimum initial absolute revolution angle, in degrees.
     * @param maxRadialTheta Maximum initial absolute revolution angle, in degrees.
     * @param minRadialW     Minimum initial absolute revolution velocity, in degrees / s.
     * @param maxRadialW     Maximum initial absolute revolution velocity, in degrees / s.
     * @param minRadialAlpha Minimum absolute revolution acceleration, in degrees / s^2.
     * @param maxRadialAlpha Maximum absolute revolution acceleration, in degrees / s^2.
     */
    public void setRadialRevolution(float minRadialTheta, float maxRadialTheta, float minRadialW, float maxRadialW, float minRadialAlpha, float maxRadialAlpha) {
        this.minRadialTheta = minRadialTheta;
        this.maxRadialTheta = maxRadialTheta;
        this.minRadialW = minRadialW;
        this.maxRadialW = maxRadialW;
        this.minRadialAlpha = minRadialAlpha;
        this.maxRadialAlpha = maxRadialAlpha;
    }

    /**
     * Adds periodic motion along the global world x-axis. For each particle, if its phase is non-zero, will also
     * translate that particle so that its initial position is unchanged at {@code t = 0}.
     *
     * @param minSinXAmplitude Minimum amplitude of periodic motion along global x-axis, in world-coord units.
     * @param maxSinXAmplitude Maximum amplitude of periodic motion along global x-axis, in world-coord units.
     * @param minSinXFrequency Minimum number of complete cycles per second along global x-axis.
     * @param maxSinXFrequency Maximum of complete cycles per second along global x-axis.
     * @param minSinXPhase     Minimum initial phase of periodic motion along global x-axis, in degrees.
     * @param maxSinXPhase     Maximum initial phase of periodic motion along global x-axis, in degrees.
     */
    public void setSinusoidalMotionX(float minSinXAmplitude, float maxSinXAmplitude, float minSinXFrequency, float maxSinXFrequency, float minSinXPhase, float maxSinXPhase) {
        this.minSinXAmplitude = minSinXAmplitude;
        this.maxSinXAmplitude = maxSinXAmplitude;
        this.minSinXFrequency = minSinXFrequency;
        this.maxSinXFrequency = maxSinXFrequency;
        this.minSinXPhase = minSinXPhase;
        this.maxSinXPhase = maxSinXPhase;
    }

    /**
     * @param minSinYAmplitude Minimum amplitude of periodic motion along global y-axis, in world-coord units.
     * @param maxSinYAmplitude Maximum amplitude of periodic motion along global y-axis, in world-coord units.
     * @param minSinYFrequency Minimum number of complete cycles per second along global y-axis.
     * @param maxSinYFrequency Maximum of complete cycles per second along global y-axis.
     * @param minSinYPhase     Minimum initial phase of periodic motion along global y-axis, in degrees.
     * @param maxSinYPhase     Maximum initial phase of periodic motion along global y-axis, in degrees.
     * @see Cluster#setSinusoidalMotionX(float, float, float, float, float, float)
     */
    public void setSinusoidalMotionY(float minSinYAmplitude, float maxSinYAmplitude, float minSinYFrequency, float maxSinYFrequency, float minSinYPhase, float maxSinYPhase) {
        this.minSinYAmplitude = minSinYAmplitude;
        this.maxSinYAmplitude = maxSinYAmplitude;
        this.minSinYFrequency = minSinYFrequency;
        this.maxSinYFrequency = maxSinYFrequency;
        this.minSinYPhase = minSinYPhase;
        this.maxSinYPhase = maxSinYPhase;
    }

    FloatBuffer generate(float currentTime) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(count * ParticleEngine.FLOATS_PER_PARTICLE);
        float twoPi = 2f * (float) Math.PI;
        for (int i = 0; i < count; i++) {
            Vector2f newPos = new Vector2f();
            Vector2f.add(position, Utils.randomPointInCircle(newPos, positionSpread), newPos);
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

            float newSinXAmplitude = Utils.randBetween(minSinXAmplitude, maxSinXAmplitude);
            float newSinXFrequency = Utils.randBetween(minSinXFrequency, maxSinXFrequency) * twoPi;
            float newSinXPhase = Utils.randBetween(minSinXPhase, maxSinXPhase) * Misc.RAD_PER_DEG;
            float newSinYAmplitude = Utils.randBetween(minSinYAmplitude, maxSinYAmplitude);
            float newSinYFrequency = Utils.randBetween(minSinYFrequency, maxSinYFrequency) * twoPi;
            float newSinYPhase = Utils.randBetween(minSinYPhase, maxSinYPhase) * Misc.RAD_PER_DEG;

            float newTheta = Utils.randBetween(minTheta, maxTheta) * Misc.RAD_PER_DEG;
            float newW = Utils.randBetween(minW, maxW) * Misc.RAD_PER_DEG;
            float newAlpha = Utils.randBetween(minAlpha, maxAlpha) * Misc.RAD_PER_DEG;

            float newRadialTheta = Utils.randBetween(minRadialTheta, maxRadialTheta) * Misc.RAD_PER_DEG;
            float newRadialW = Utils.randBetween(minRadialW, maxRadialW) * Misc.RAD_PER_DEG;
            float newRadialAlpha = Utils.randBetween(minRadialAlpha, maxRadialAlpha) * Misc.RAD_PER_DEG;

            float newSize = Utils.randBetween(minSize, maxSize);
            float newSizeV = Utils.randBetween(minSizeV, maxSizeV);
            float newSizeA = Utils.randBetween(minSizeA, maxSizeA);

            float[] newInColor = new float[]{
                    inColor[0] + Utils.randBetween(-inColorSpread[0] / 2f, inColorSpread[0] / 2f),
                    inColor[1] + Utils.randBetween(-inColorSpread[1] / 2f, inColorSpread[1] / 2f),
                    inColor[2] + Utils.randBetween(-inColorSpread[2] / 2f, inColorSpread[2] / 2f),
                    inColor[3] + Utils.randBetween(-inColorSpread[3] / 2f, inColorSpread[3] / 2f),
            };

            float[] newOutColor = new float[]{
                    outColor[0] + Utils.randBetween(-outColorSpread[0] / 2f, outColorSpread[0] / 2f),
                    outColor[1] + Utils.randBetween(-outColorSpread[1] / 2f, outColorSpread[1] / 2f),
                    outColor[2] + Utils.randBetween(-outColorSpread[2] / 2f, outColorSpread[2] / 2f),
                    outColor[3] + Utils.randBetween(-outColorSpread[3] / 2f, outColorSpread[3] / 2f),
            };

            float newFadeIn = Utils.randBetween(minFadeIn, maxFadeIn);
            float newFadeOut = Utils.randBetween(minFadeOut, maxFadeOut);
            float newLife = Utils.randBetween(minLife, maxLife);

            buffer.put(
                    new float[]{
                            newPos.x,
                            newPos.y,
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
                            newRadialTheta,
                            newRadialW,
                            newRadialAlpha,
                            newSize,
                            newSizeV,
                            newSizeA,
                            newInColor[0],
                            newInColor[1],
                            newInColor[2],
                            newInColor[3],
                            newOutColor[0],
                            newOutColor[1],
                            newOutColor[2],
                            newOutColor[3],
                            newFadeIn,
                            newFadeOut,
                            currentTime,
                            currentTime + newLife
                    }
            );
        }
        buffer.flip();
        return buffer;
    }
}
