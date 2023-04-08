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

/** An instanced particle emitter implementation.
 * Create particle clusters ("blueprints" for a set of particles) via {@link ParticleEngine#initializeParticles}.
 * Set the clusters' data via methods in {@link Cluster}.
 * Then, generate particles according the cluster's data using {@link ParticleEngine#generateParticles}.
 *  */
public class ParticleEngine extends BaseEveryFrameCombatPlugin {

    /** In number of floats. */
    static final int INITIAL_BUFFER_SIZE = 1024;

    /** Total number of floats passed into vertex shader per particle */
    static final int FLOATS_PER_PARTICLE;
    static final int FLOAT_SIZE = 4;
    static final int BYTES_PER_PARTICLE;
    /** Size in number of floats per attribute passed into the vertex shader, in layout order. */
    static final int[] VERTEX_ATTRIB_SIZES = new int[] {
            2,   // position
            4,   // velocity and acceleration
            3,   // sinusoidal motion in x
            3,   // sinusoidal motion in y
            3,   // angular data
            3,   // radial turning data
            3,   // size data
            4,   // starting color
            4,   // ending color
            4    // fade and time data
    };

    static final int DEATH_TIME_LOC;

    static {
        int count = 0;
        for (int size : VERTEX_ATTRIB_SIZES) {
            count += size;
        }
        FLOATS_PER_PARTICLE = count;

        BYTES_PER_PARTICLE = FLOATS_PER_PARTICLE * FLOAT_SIZE;
        // Ensure that the death time is always the last value in each vertex!
        DEATH_TIME_LOC = FLOATS_PER_PARTICLE - 1;
    }

    private CombatEngineAPI engine;
    private float currentTime;
    private static final String customDataKey = "wpnxt_ParticlesPlugin";
    private static final Logger log = Global.getLogger(ParticleEngine.class);
    private final Map<ParticleType, ParticleAllocator> particleMap = new HashMap<>();
    private final Queue<ParticleDeathData> deadParticleCounter = new PriorityQueue<>();
    static final Set<Integer> usedVAOs = new HashSet<>();
    static final Set<Integer> usedVBOs = new HashSet<>();


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

        ParticleDeathData deathData;
        // Only call registerParticleDeath once per frame per allocator to avoid this edge case where two clusters
        // die at the same time:
        //    - First dead cluster registers and causes a reallocation, which deletes the second dead cluster
        //    - After the reallocation, deadParticles is 0, but the second dead cluster registers, incrementing
        //      deadParticles even though it's already been purged from the buffer.
        Map<ParticleType, Integer> totalDeadParticles = new HashMap<>();
        while ((deathData = deadParticleCounter.peek()) != null && deathData.deathTime <= currentTime) {
            Integer count = totalDeadParticles.get(deathData.type);
            totalDeadParticles.put(deathData.type, count == null ? deathData.count : count + deathData.count);
            deadParticleCounter.poll();
        }

        for (Map.Entry<ParticleType, Integer> entry : totalDeadParticles.entrySet()) {
            ParticleAllocator allocator = particleMap.get(entry.getKey());
            if (allocator != null) {
                allocator.registerParticleDeath(entry.getValue(), currentTime);

                // If the allocator has no particles, then delete it and free the memory used
                if (allocator.combinedBuffer.position() == 0) {
                    GL30.glDeleteVertexArrays(allocator.vao);
                    GL15.glDeleteBuffers(allocator.vbo);
                    usedVAOs.remove(allocator.vao);
                    usedVBOs.remove(allocator.vbo);
                    particleMap.remove(entry.getKey());
                }
            }
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
            type.sprite.bindTexture();
            GL30.glBindVertexArray(allocator.vao);
            GL20.glUniformMatrix4(ParticleShader.projectionLoc, true, Utils.getProjectionMatrix(viewport));
            GL20.glUniform1f(ParticleShader.timeLoc, currentTime);
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, allocator.combinedBuffer.position() / FLOATS_PER_PARTICLE);
            GL30.glBindVertexArray(0);
        }
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(0);
    }

    /**
     *  Initialize a {@link Cluster}. The {@code Cluster}'s attributes will be initialized to
     *  default values and should be set in subsequent calls. In particular, {@code position} defaults to {@code (0, 0)} and should
     *  therefore always be manually set using {@link Cluster#setPositionData}.
     *  Note that this call does not generate particles or allocate any GPU memory. To actually create the particles,
     *  use {@link ParticleEngine#generateParticles(Cluster)}.
     * @param count Number of particles in the {@code Cluster}.
     * @param sprite The texture that the particles should sample when rendered.
     * @param sfactor Source blend factor.
     * @param dfactor Destination blend factor.
     * @param blendMode Blending operation.
     * @return A default {@link Cluster} object
     */
    public static Cluster initializeParticles(int count, SpriteAPI sprite, int sfactor, int dfactor, int blendMode) {
        return new Cluster(count, sprite, sfactor, dfactor, blendMode);
    }


     /** Same as {@link ParticleEngine#initializeParticles(int, SpriteAPI, int, int, int)}, but uses the default
     * additive blending. */
    public static Cluster initializeParticles(int count, SpriteAPI sprite) {
        return initializeParticles(count, sprite, GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL14.GL_FUNC_ADD);
    }

    /**
     * Generates particles according to the data in {@code cluster}. Before calling this method, initialize a particle
     * cluster with {@link ParticleEngine#initializeParticles} and set the cluster's data via methods in {@link Cluster},
     * e.g. {@link Cluster#setPositionData}.
     *
     * @param cluster {@link Cluster} object to generate
     */
    public static void generateParticles(Cluster cluster) {
        ParticleEngine particleEngine = getInstance();
        if (particleEngine == null) {
            log.warn("Failed to generate a particle cluster because the particle engine couldn't be found. Check that Global.getCombatEngine() isn't null.");
            return;
        }

        ParticleType type = new ParticleType(cluster.sprite, cluster.sfactor, cluster.dfactor, cluster.blendMode);
        ParticleAllocator allocator = particleEngine.particleMap.get(type);

        if (allocator == null) {
            allocator = new ParticleAllocator();
            particleEngine.particleMap.put(type, allocator);
        }

        allocator.allocateParticles(cluster, particleEngine.currentTime);
        particleEngine.deadParticleCounter.add(
                new ParticleDeathData(
                        type,
                        cluster.count,
                        particleEngine.currentTime + cluster.maxLife
                ));
    }

    /**
     *  Retrieves the {@code ParticleEngine} attached to the current combat engine.
     *
     * @return A singleton {@code ParticleEngine}, or {@code null} if no combat engine exists.
     */
    private static ParticleEngine getInstance() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return null;
        }

        return (ParticleEngine) engine.getCustomData().get(customDataKey);
    }

    private static class ParticleDeathData implements Comparable<ParticleDeathData> {
        private final ParticleType type;
        private final int count;
        private final float deathTime;

        private ParticleDeathData(ParticleType type, int count, float deathTime) {
            this.type = type;
            this.count = count;
            this.deathTime = deathTime;
        }

        @Override
        public int compareTo(ParticleDeathData o) {
            return Float.compare(deathTime, o.deathTime);
        }
    }

}
