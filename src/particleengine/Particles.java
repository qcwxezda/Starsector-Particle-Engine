package particleengine;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

/** Particle system implementation.
 * Create an emitter via {@link Particles#initialize}, set the emitter's properties with methods in
 * {@link Emitter}, and generate particles using an emitter with {@link Particles#burst} or {@link Particles#stream}.
 *  */
public class Particles extends BaseEveryFrameCombatPlugin {

    /** Total number of floats passed into vertex shader per particle */
    static final int FLOATS_PER_PARTICLE;
    static final int FLOAT_SIZE = 4;
    static final int BYTES_PER_PARTICLE;
    /** Size in number of floats per attribute passed into the vertex shader, in layout order. */
    static final int[] VERTEX_ATTRIB_SIZES = new int[] {
            4,   // position and emitter position
            1,   // emitter forward direction
            4,   // velocity and acceleration
            3,   // sinusoidal motion in x
            3,   // sinusoidal motion in y
            3,   // angular data
            2,   // radial revolution data
            3,   // x-size data
            3,   // y-size data
            4,   // starting color
            4,   // color shift
            4    // fade and time data
    };

    static {
        int count = 0;
        for (int size : VERTEX_ATTRIB_SIZES) {
            count += size;
        }
        FLOATS_PER_PARTICLE = count;

        BYTES_PER_PARTICLE = FLOATS_PER_PARTICLE * FLOAT_SIZE;
    }

    private CombatEngineAPI engine;
    private float currentTime;
    static final float minBurstDelay = 1f / 60f;
    private static final String customDataKey = "particleengine_ParticlesPlugin";
    private final Map<ParticleType, ParticleAllocator> particleMap = new HashMap<>();
    private final Queue<DeferredAction> doLaterQueue = new PriorityQueue<>();
    static final Set<Integer> usedVAOs = new HashSet<>();
    static final Set<Integer> usedVBOs = new HashSet<>();

    interface Action {
        void perform();
    }

    private static class DeferredAction implements Comparable<DeferredAction> {

        private final float time;
        private final Action action;

        private DeferredAction(Action action, float time) {
            this.action = action;
            this.time = time;
        }

        @Override
        public int compareTo(DeferredAction o) {
            return Float.compare(time, o.time);
        }
    }

    static void doLater(Action action, float delay) {
        Particles instance = getInstance();
        if (instance == null) {
            return;
        }

        instance.doLaterQueue.add(new DeferredAction(action, instance.currentTime + delay));
    }

    static void doAtTime(Action action, float time) {
        Particles instance = getInstance();
        if (instance == null) {
            return;
        }

        instance.doLaterQueue.add(new DeferredAction(action, time));
    }

    static void clearBuffers() {
        // Clear used VAOs and VBOs, in case the last combat ended with particles still on screen
        for (int vao : usedVAOs) {
            GL30.glDeleteVertexArrays(vao);
        }
        for (int vbo : usedVBOs) {
            GL15.glDeleteBuffers(vbo);
        }
        usedVAOs.clear();
        usedVBOs.clear();
    }

    static void removeAllocator(ParticleType type) {
        Particles instance = getInstance();
        if (instance == null) {
            return;
        }

        ParticleAllocator allocator = instance.particleMap.get(type);
        if (allocator == null) {
            return;
        }

        GL15.glDeleteBuffers(allocator.vbo);
        GL30.glDeleteVertexArrays(allocator.vao);
        instance.particleMap.remove(type);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        currentTime = 0f;

        engine.getCustomData().put(customDataKey, this);
        clearBuffers();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        DeferredAction firstItem;
        while ((firstItem = doLaterQueue.peek()) != null && firstItem.time <= currentTime) {
            doLaterQueue.poll();
            firstItem.action.perform();
        }

        currentTime += amount;
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        GL20.glUseProgram(ParticleShader.programId);
        GL11.glEnable(GL11.GL_BLEND);
        for (Map.Entry<ParticleType, ParticleAllocator> entry : particleMap.entrySet()) {
            ParticleType type = entry.getKey();
            ParticleAllocator allocator = entry.getValue();
            GL11.glBlendFunc(type.sfactor, type.dfactor);
            GL14.glBlendEquation(type.blendMode);
            if (type.sprite != null) {
                int target = 1;
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + target);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, type.sprite.getTextureId());
                GL20.glUniform1i(ParticleShader.texSamplerLoc, target);
            }
            GL30.glBindVertexArray(allocator.vao);
            GL20.glUniformMatrix4(ParticleShader.projectionLoc, true, Utils.getProjectionMatrix(viewport));
            GL20.glUniform1f(ParticleShader.timeLoc, currentTime);
            GL20.glUniform1i(ParticleShader.useTextureLoc, type.sprite == null ? 0 : 1);
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, allocator.bufferPosition / FLOATS_PER_PARTICLE);
            GL30.glBindVertexArray(0);
            if (type.sprite != null) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
            }
        }
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(0);
    }

    /**
     * Same as {@link Particles#initialize(Vector2f, SpriteAPI, int, int, int)}, but uses a default particle sprite instead
     * of sampling a texture.
     */
    public static Emitter initialize(Vector2f location, int sfactor, int dfactor, int blendMode) {
        return initialize(location, null, sfactor, dfactor, blendMode);
    }

    /** Same as  {@link Particles#initialize(Vector2f, SpriteAPI, int, int, int)}, but uses the default
     * additive blending and a default particle sprite. */
    public static Emitter initialize(Vector2f location) {
        return initialize(location,null);
    }

    /**
     *  Initialize an {@link Emitter}. The {@code Emitter}'s location will be set to {@code location}, specified
     *  in world coordinates. The {@code Emitter}'s attributes will be initialized to
     *  default values and should be set in subsequent calls. To generate particles,
     *  use {@link Particles#burst} or {@link Particles#stream}.
     *
     * @param sprite The texture that the particles should sample when rendered.
     * @param sfactor Source blend factor.
     * @param dfactor Destination blend factor.
     * @param blendMode Blending operation.
     * @return A default {@link Emitter}
     */
    public static Emitter initialize(Vector2f location, SpriteAPI sprite, int sfactor, int dfactor, int blendMode) {
        return new Emitter(location, sprite, sfactor, dfactor, blendMode);
    }

    /**
     * Initializes an {@link Emitter} with the given sprite and blend mode settings, and then copies
     * all of {@code copy}'s properties to it. Changes in properties of the resulting emitter are not reflected in
     * the original copy, nor vice versa.
     *
     * @param copy {@link Emitter} whose properties to copy.
     * @param sprite Texture to use for the new emitter.
     * @param sfactor Source blend factor.
     * @param dfactor Destination blend factor.
     * @param blendMode Blending operation.
     * @return An {@link Emitter} with the same properties as {@code copy}.
     */
    public static Emitter initialize(Emitter copy, SpriteAPI sprite, int sfactor, int dfactor, int blendMode) {
        Emitter emitter = initialize(copy.location, sprite, sfactor, dfactor, blendMode);
        emitter.life(copy.minLife, copy.maxLife);
        emitter.fadeTime(copy.minFadeIn, copy.maxFadeIn, copy.minFadeOut, copy.maxFadeOut);
        emitter.setAxis(copy.xAxis);
        emitter.setLocation(copy.location);
        emitter.offset(copy.minOffset, copy.maxOffset);
        emitter.velocity(copy.minVelocity, copy.maxVelocity);
        emitter.acceleration(copy.minAcceleration, copy.maxAcceleration);
        emitter.circularPositionSpread(copy.positionSpread);
        emitter.circularVelocitySpread(copy.velocitySpread);
        emitter.circularAccelerationSpread(copy.accelerationSpread);
        emitter.facing(copy.minTheta, copy.maxTheta);
        emitter.turnRate(copy.minW, copy.maxW);
        emitter.turnAcceleration(copy.minAlpha, copy.maxAlpha);
        emitter.size(copy.minSizeDataX[0], copy.maxSizeDataX[0], copy.minSizeDataY[0], copy.maxSizeDataY[0]);
        emitter.growthRate(copy.minSizeDataX[1], copy.maxSizeDataX[1], copy.minSizeDataY[1], copy.maxSizeDataY[1]);
        emitter.growthAcceleration(copy.minSizeDataX[2], copy.maxSizeDataX[2], copy.minSizeDataY[2], copy.maxSizeDataY[2]);
        emitter.color(copy.startColor);
        emitter.randomColor(copy.startColorRandom);
        emitter.hsvaShift(copy.minColorShift, copy.maxColorShift);
        emitter.radialVelocity(copy.minRadialVelocity, copy.maxRadialVelocity);
        emitter.radialAcceleration(copy.minRadialAcceleration, copy.maxRadialAcceleration);
        emitter.radialRevolution(copy.minRadialW, copy.maxRadialW, copy.minRadialAlpha, copy.maxRadialAlpha);
        emitter.sinusoidalMotionX(
                copy.minSinXAmplitude,
                copy.maxSinXAmplitude,
                copy.minSinXFrequency,
                copy.maxSinXFrequency,
                copy.minSinXPhase,
                copy.maxSinXPhase);
        emitter.sinusoidalMotionY(
                copy.minSinYAmplitude,
                copy.maxSinYAmplitude,
                copy.minSinYFrequency,
                copy.maxSinYFrequency,
                copy.minSinYPhase,
                copy.maxSinYPhase);
        return emitter;
    }

    /**
     * Same as {@link Particles#initialize(Emitter, SpriteAPI, int, int, int)}, but also copies {@code copy}'s
     * blending settings.
     *
     * @param copy {@link Emitter} whose properties to copy
     * @param sprite Texture to use for the new emitter
     * @return An emitter with the same properties as {@code copy}
     */
    public static Emitter initialize(Emitter copy, SpriteAPI sprite) {
        return initialize(copy, sprite, copy.sfactor, copy.dfactor, copy.blendMode);
    }

    /**
     * Same as {@link Particles#initialize(Emitter, SpriteAPI, int, int, int)}, but also copies {@code copy}'s
     * sprite and blending settings.
     *
     * @param copy {@link Emitter} whose properties to copy
     * @return An emitter with the same properties as {@code copy}
     */
    public static Emitter initialize(Emitter copy) {
        return initialize(copy, copy.sprite);
    }

     /** Same as {@link Particles#initialize(Vector2f, SpriteAPI, int, int, int)}, but uses
     * additive blending by default. */
    public static Emitter initialize(Vector2f location, SpriteAPI sprite) {
        return initialize(location, sprite, GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL14.GL_FUNC_ADD);
    }

    /**
     * Generates an instantaneous burst of particles.
     *
     * @param emitter {@link Emitter} to use.
     * @param count number of particles to generate.
     *
     * @return Whether the particles were successfully generated. If {@code false}, this generally means that
     * the combat engine is {@code null}.
     */
    public static boolean burst(Emitter emitter, int count) {
        Particles particleEngine = getInstance();
        if (particleEngine == null) {
             return false;
        }

        ParticleType type = new ParticleType(emitter.sprite, emitter.sfactor, emitter.dfactor, emitter.blendMode);
        ParticleAllocator allocator = particleEngine.particleMap.get(type);

        if (allocator == null) {
            allocator = new ParticleAllocator(type);
            particleEngine.particleMap.put(type, allocator);
        }

        allocator.allocateParticles(emitter, count, particleEngine.currentTime);
        return true;
    }

    /**
     *  Generates a continuous stream of particles.
     *
     * @param emitter {@link Emitter} to use.
     * @param particlesPerBurst Number of particles that should be generated at once.
     * @param particlesPerSecond Total number of particles generated per second.
     * @param duration Amount of time this particle stream should last.
     */
    public static void stream(final Emitter emitter, int particlesPerBurst, float particlesPerSecond, float duration) {
        stream(emitter, particlesPerBurst, particlesPerSecond, duration, null);
    }

    /** Custom action that can be performed before each generation in a {@link Particles#stream(Emitter, int, float, float, StreamAction)} call. */
    public interface StreamAction {
        /**
         * @param emitter The {@link Emitter} that was initially fed to the {@link Particles#stream(Emitter, int, float, float, StreamAction)} call.
         * @return If {@code false}, the stream will stop generating particles. Otherwise, has no effect.
         */
        boolean apply(Emitter emitter);
    }

    /**
     *  Generates a continuous stream of particles.
     *
     * @param emitter {@link Emitter} to use.
     * @param particlesPerBurst Number of particles that should be generated at once.
     * @param particlesPerSecond Total number of particles generated per second.
     * @param maxDuration Maximum amount of time this particle stream should last.
     * @param doBeforeGenerating Custom function that's called immediately before each particle generation sequence in this stream.
     *                           Returning {@code false} will end the stream. Can be {@code null}.
     */
    public static void stream(
            final Emitter emitter,
            int particlesPerBurst,
            float particlesPerSecond,
            final float maxDuration,
            final StreamAction doBeforeGenerating) {
        final Particles instance = getInstance();
        if (instance == null) {
            return;
        }

        final int newParticlesPerBurst;
        final float newBurstDelay;

        float burstDelay = particlesPerBurst / particlesPerSecond;
        if (burstDelay >= minBurstDelay) {
            newParticlesPerBurst = particlesPerBurst;
            newBurstDelay = burstDelay;
        } else {
            newParticlesPerBurst = (int) Math.floor(particlesPerSecond * minBurstDelay);
            newBurstDelay = newParticlesPerBurst / particlesPerSecond;
        }

        final float startTime = instance.currentTime;
        doAtTime(new Action() {
            float lastBurstTime = startTime;
            @Override
            public void perform() {
                if (instance.currentTime <= startTime + maxDuration
                        && (doBeforeGenerating == null || doBeforeGenerating.apply(emitter))
                        && burst(emitter, newParticlesPerBurst)) {
                    doAtTime(this, lastBurstTime + newBurstDelay);
                    lastBurstTime += newBurstDelay;
                }
            }
        }, startTime);
    }

    /**
     *  Retrieves the {@code Particles} attached to the current combat engine.
     *
     * @return A singleton {@code Particles}, or {@code null} if no combat engine exists.
     */
    private static Particles getInstance() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return null;
        }

        return (Particles) engine.getCustomData().get(customDataKey);
    }
}
