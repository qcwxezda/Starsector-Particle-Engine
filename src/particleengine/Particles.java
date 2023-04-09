package particleengine;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.*;

import java.util.*;

// TODO: make initialize take in the emitter position
// TODO: Change "Cluster" to "Emitter"
// TODO: Remove cluster size from initialization parameters, add it as a generation parameter
// TODO: Fix issue where max of one cluster can be generated per frame

/** An instanced particle emitter implementation.
 * Create particle clusters ("blueprints" for a set of particles) via {@link Particles#initialize}.
 * Set the clusters' data via methods in {@link Cluster}.
 * Then, generate particles according the cluster's data using {@link Particles#generate}.
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
            3,   // size data
            4,   // starting color
            4,   // ending color
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
    private static final String customDataKey = "particleengine_ParticlesPlugin";
    private static final Logger log = Global.getLogger(Particles.class);
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
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, allocator.combinedBuffer.position() / FLOATS_PER_PARTICLE);
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
     * Same as {@link Particles#initialize(int, SpriteAPI, int, int, int)}, but uses a default particle sprite instead
     * of sampling a texture.
     */
    public static Cluster initialize(int count, int sfactor, int dfactor, int blendMode) {
        return initialize(count, null, sfactor, dfactor, blendMode);
    }

    /** Same as  {@link Particles#initialize(int, SpriteAPI, int, int, int)}, but uses the default
     * additive blending and a default particle sprite. */
    public static Cluster initialize(int count) {
        return initialize(count, null);
    }

    /**
     *  Initialize a {@link Cluster}. The {@code Cluster}'s attributes will be initialized to
     *  default values and should be set in subsequent calls. In particular, {@code position} defaults to {@code (0, 0)} and should
     *  therefore always be manually set using {@link Cluster#setPosVelAcc} or {@link Cluster#setPosition}.
     *  Note that this call does not generate particles or allocate any GPU memory. To actually create the particles,
     *  use {@link Particles#generate(Cluster)}.
     * @param count Number of particles in the {@code Cluster}.
     * @param sprite The texture that the particles should sample when rendered.
     * @param sfactor Source blend factor.
     * @param dfactor Destination blend factor.
     * @param blendMode Blending operation.
     * @return A default {@link Cluster} object
     */
    public static Cluster initialize(int count, SpriteAPI sprite, int sfactor, int dfactor, int blendMode) {
        return new Cluster(count, sprite, sfactor, dfactor, blendMode);
    }


     /** Same as {@link Particles#initialize(int, SpriteAPI, int, int, int)}, but uses
     * additive blending by default. */
    public static Cluster initialize(int count, SpriteAPI sprite) {
        return initialize(count, sprite, GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL14.GL_FUNC_ADD);
    }

    /**
     * Generates particles according to the data in {@code cluster}. Before calling this method, initialize a particle
     * cluster with {@link Particles#initialize} and set the cluster's data via methods in {@link Cluster},
     * e.g. {@link Cluster#setPosVelAcc}.
     *
     * @param cluster {@link Cluster} object to generate
     */
    public static void generate(Cluster cluster) {
        Particles particleEngine = getInstance();
        if (particleEngine == null) {
            log.warn("Failed to generate a particle cluster because the particle engine couldn't be found. Check that Global.getCombatEngine() isn't null.");
            return;
        }

        ParticleType type = new ParticleType(cluster.sprite, cluster.sfactor, cluster.dfactor, cluster.blendMode);
        ParticleAllocator allocator = particleEngine.particleMap.get(type);

        if (allocator == null) {
            allocator = new ParticleAllocator(type);
            particleEngine.particleMap.put(type, allocator);
        }

        allocator.allocateParticles(cluster, particleEngine.currentTime);
    }

    /**
     *  Generates a stream of particles according to the data in {@code cluster}.
     * @param cluster {@link Cluster} object to stream
     * @param particlesPerSecond Total number of particles to be generated per second. The engine will generate
     *                           {@code particlesPerSecond / cluster.count} bursts of {@code cluster.count} particles
     *                           per second.
     * @param duration Total amount of time this particle stream should last.
     */
    public static void stream(final Cluster cluster, float particlesPerSecond, float duration) {
        stream(cluster, particlesPerSecond, duration, null);
    }

    /** Custom action that can be performed before each generation in a {@link Particles#stream(Cluster, float, float, StreamAction)} call. */
    public interface StreamAction {
        /**
         * @param cluster The {@link Cluster} that was initially fed to the {@link Particles#stream(Cluster, float, float, StreamAction)} call.
         * @return If {@code false}, the stream will stop generating particles. Otherwise, has no effect.
         */
        boolean apply(Cluster cluster);
    }

    /**
     *  Generates a stream of particles according to the data in {@code cluster}.
     *
     * @param cluster {@link Cluster} object to stream
     * @param particlesPerSecond Total number of particles to be generated per second. The engine will generate
     *                                 {@code particlesPerSecond / cluster.count} bursts of {@code cluster.count} particles
     *                                 per second.
     * @param maxDuration Maximum amount of time this particle stream should last.
     * @param doBeforeGenerating Custom function that's called immediately before each particle generation sequence in this stream.
     *                           Returning {@code false} will end the stream. Can be {@code null}.
     */
    public static void stream(
            final Cluster cluster,
            float particlesPerSecond,
            final float maxDuration,
            final StreamAction doBeforeGenerating) {
        final Particles instance = getInstance();
        if (instance == null) {
            return;
        }

        final float delayBetweenBursts = cluster.count / particlesPerSecond;
        final float startTime = instance.currentTime;
        doLater(new Action() {
            @Override
            public void perform() {
                if (instance.currentTime <= startTime + maxDuration
                        && (doBeforeGenerating == null || doBeforeGenerating.apply(cluster))) {
                    generate(cluster);
                    doLater(this, delayBetweenBursts);
                }
            }
        }, delayBetweenBursts);

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
