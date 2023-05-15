package particleengine;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;

public abstract class IEmitter {

    protected float maxLife = 0f;

    /**
     * @return Absolute location of this emitter, in world coordinates.
     * */
    public abstract Vector2f getLocation();

    /**
     * @return Sprite that particles from this emitter will have. Can be {@code null}, in which a default particle
     * will be used.
     */
    public abstract SpriteAPI getSprite();

    /**
     * @return Angle, in degrees, that this emitter's x-axis faces. If {@code 0}, then this emitter's
     * axes coincides with global axes (x-axis facing to the right).
     */
    public abstract float getXDir();

    /**
     * @return Source blend factor.
     */
    public abstract int getBlendSourceFactor();
    /**
     * @return Destination blend factor.
     */
    public abstract int getBlendDestinationFactor();

    /**
     * @return Blending operation, e.g. ADD, SUBTRACT, REVERSE_SUBTRACT.
     */
    public abstract int getBlendFunc();

    /**
     * @return Layer particles should be rendered on.
     */
    public abstract CombatEngineLayers getLayer();

    /**
     * @return Distance from edge of screen beyond which particles are culled before generation.
     * This applies to the emitter's location rather than individual particles' locations.
     */
    public float getRenderRadius() {
        return 500f;
    }

    /**
     * Construct parameters for a particle. Doesn't actually generate the particle. Use {@link Particles#burst}
     * to generate particles from an emitter.
     *
     * @param id Index of the particle in the burst
     * @return Object containing the necessary data for particle generation
     */
    public abstract ParticleData initParticle(int id);

    protected final FloatBuffer generate(int count, float startTime, ViewportAPI viewport) {
        if (!Utils.isInViewport(getLocation(), viewport, getRenderRadius())) {
            return null;
        }
        FloatBuffer buffer = BufferUtils.createFloatBuffer(count * Particles.FLOATS_PER_PARTICLE);
        for (int i = 0; i < count; i++) {
            ParticleData data = initParticle(i);
            maxLife = Math.max(maxLife, data.life);
            data.addToFloatBuffer(this, startTime, buffer);
        }
        buffer.flip();
        return buffer;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class ParticleData {
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
        private float life = 1f;

        /**
         * Initializes a {@code ParticleData} with default properties.
         */
        public ParticleData() {
        }

        public ParticleData offset(Vector2f offset) {
            this.offset.set(offset);
            return this;
        }

        public ParticleData velocity(Vector2f velocity) {
            this.velocity.set(velocity);
            return this;
        }

        public ParticleData acceleration(Vector2f acceleration) {
            this.acceleration.set(acceleration);
            return this;
        }

        public ParticleData sinusoidalXMotion(float amplitude, float frequency, float phase) {
            sinXAmp = amplitude;
            sinXFreq = frequency;
            sinXPhase = phase;
            return this;
        }

        public ParticleData sinusoidalYMotion(float amplitude, float frequency, float phase) {
            sinYAmp = amplitude;
            sinYFreq = frequency;
            sinYPhase = phase;
            return this;
        }

        public ParticleData facing(float facing) {
            this.facing = facing;
            return this;
        }

        public ParticleData turnRate(float turnRate) {
            this.turnRate = turnRate;
            return this;
        }

        public ParticleData turnAcceleration(float turnAcceleration) {
            this.turnAcceleration = turnAcceleration;
            return this;
        }

        public ParticleData revolutionRate(float revolutionRate) {
            this.revolutionRate = revolutionRate;
            return this;
        }

        public ParticleData revolutionAcceleration(float revolutionAcceleration) {
            this.revolutionAcceleration = revolutionAcceleration;
            return this;
        }

        public ParticleData size(float xSize, float ySize) {
            this.size = new Vector2f(xSize, ySize);
            return this;
        }

        public ParticleData growthRate(float xGrowthRate, float yGrowthRate) {
            this.growthRate = new Vector2f(xGrowthRate, yGrowthRate);
            return this;
        }

        public ParticleData growthAcceleration(float xGrowthAcceleration, float yGrowthAcceleration) {
            this.growthAcceleration = new Vector2f(xGrowthAcceleration, yGrowthAcceleration);
            return this;
        }

        public ParticleData color(float r, float g, float b, float a) {
            Utils.toHSVA(new float[] {r, g, b, a}, this.color);
            return this;
        }

        ParticleData colorHSVA(float[] color) {
            this.color = color;
            return this;
        }

        public ParticleData hueShift(float hueShift) {
            colorShift[0] = hueShift;
            return this;
        }

        public ParticleData saturationShift(float saturationShift) {
            colorShift[1] = saturationShift;
            return this;
        }

        public ParticleData colorValueShift(float colorValueShift) {
            colorShift[2] = colorValueShift;
            return this;
        }

        public ParticleData alphaShift(float alphaShift) {
            colorShift[3] = alphaShift;
            return this;
        }

        public ParticleData fadeTime(float fadeIn, float fadeOut) {
            this.fadeTime = new Vector2f(fadeIn, fadeOut);
            return this;
        }

        public ParticleData life(float life) {
            this.life = life;
            return this;
        }

        private void addToFloatBuffer(IEmitter emitter, float startTime, FloatBuffer buffer) {
            Vector2f absolutePosition = new Vector2f(emitter.getLocation().x + offset.x, emitter.getLocation().y + offset.y);
            buffer.put(absolutePosition.x)
                    .put(absolutePosition.y)
                    .put(emitter.getLocation().x)
                    .put(emitter.getLocation().y)
                    .put(emitter.getXDir()*Misc.RAD_PER_DEG)
                    .put(velocity.x)
                    .put(velocity.y)
                    .put(acceleration.x)
                    .put(acceleration.y)
                    .put(sinXAmp)
                    .put(sinXFreq)
                    .put(sinXPhase)
                    .put(sinYAmp)
                    .put(sinYFreq)
                    .put(sinYPhase)
                    .put(facing)
                    .put(turnRate)
                    .put(turnAcceleration)
                    .put(revolutionRate)
                    .put(revolutionAcceleration)
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
}
