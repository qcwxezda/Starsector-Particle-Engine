package particleengine;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.util.*;

/** Particle system implementation.
 * Create a default emitter via {@link #initialize} or a custom emitter by extending {@link IEmitter},
 * set the emitter's properties with methods in {@link Emitter},
 * and generate particles using an emitter with {@link #burst} or {@link #stream}.
 *  */
@SuppressWarnings("unused")
public class Particles extends BaseEveryFrameCombatPlugin implements CombatLayeredRenderingPlugin {
    /** Total number of floats passed into vertex shader per particle */
    static final int FLOATS_PER_PARTICLE;
    static final int FLOAT_SIZE = 4;
    static final int BYTES_PER_PARTICLE;
    /** Size in number of floats per attribute passed into the vertex shader, in layout order. */
    static final int[] VERTEX_ATTRIB_SIZES = new int[] {
            1,   // index of emitter in tracker, if tracked
                 // use for particles that should dynamically
                 // follow their emitter's movements
                 // will be -1 if not tracking any emitter
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
    float currentTime;
    static final float minBurstDelay = 1f / 60f;
    private static final String customDataKey = "particleengine_ParticlesPlugin";
    private final Map<CombatEngineLayers, SortedMap<ParticleType, Pair<ParticleAllocator, ParticleRenderer>>> particleMap = new HashMap<>();
    private final Queue<DeferredAction> doLaterQueue = new PriorityQueue<>();
    private final Set<ParticleStream<? extends IEmitter>> particleStreams = new HashSet<>();
    private final Map<IEmitter, CombatEntityAPI> anchorPoints = new HashMap<>();
    private EmitterBufferHandler trackedEmitterHandler;
    static final Set<Integer> usedVAOs = new HashSet<>();
    static final Set<Integer> usedVBOs = new HashSet<>();
    static final Set<String> loadedTextures = new HashSet<>();

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

    @Override
    public void init(CombatEntityAPI entity) {}

    @Override
    public void cleanup() {}

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
        for (String textureId : loadedTextures) {
            Global.getSettings().unloadTexture(textureId);
        }
        loadedTextures.clear();
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.allOf(CombatEngineLayers.class);
    }

    @Override
    public float getRenderRadius() {
        return 999999999f;
    }


    @Override
    public void advance(float amount) {}

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        GL20.glUseProgram(ParticleShader.programId);
        GL11.glEnable(GL11.GL_BLEND);
        if (CombatEngineLayers.BELOW_PLANETS.equals(layer)) {
            trackedEmitterHandler.updateTrackedEmitters(currentTime);
            fillUniformBuffer();
        }
        GL20.glUniformMatrix4(ParticleShader.projectionLoc, true, Utils.getProjectionMatrix(viewport));
        GL20.glUniform1f(ParticleShader.timeLoc, currentTime);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, trackedEmitterHandler.getUboBufferIndex());
        if (particleMap.containsKey(layer)) {
            for (Pair<ParticleAllocator, ParticleRenderer> p : particleMap.get(layer).values()) {
                if (p.two.layer.equals(layer)) {
                    p.two.render();
                }
            }
        }
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(0);
    }

    private void fillUniformBuffer() {
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, trackedEmitterHandler.getUboBufferIndex());
        FloatBuffer ubo = trackedEmitterHandler.locationsToFloatBuffer();
        ubo.limit(4*trackedEmitterHandler.getHighestFilledPosition() + 4);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, ubo);
        ubo.limit(ubo.capacity());
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    static void removeType(ParticleType type) {
        Particles instance = getInstance();
        if (instance == null) {
            return;
        }

        if (!instance.particleMap.containsKey(type.layer)) {
            return;
        }

        Map<ParticleType, Pair<ParticleAllocator, ParticleRenderer>> subMap = instance.particleMap.get(type.layer);
        Pair<ParticleAllocator, ParticleRenderer> pair = subMap.get(type);
        if (pair == null) {
            return;
        }

        ParticleAllocator allocator = pair.one;
        ParticleRenderer renderer = pair.two;

        GL15.glDeleteBuffers(allocator.vbo);
        GL30.glDeleteVertexArrays(allocator.vao);

        subMap.remove(type);
        if (subMap.isEmpty()) {
            instance.particleMap.remove(type.layer);
        }
    }

    /** This is done automatically and should not be manually called. */
    @Override
    public void init(final CombatEngineAPI engine) {
        clearBuffers();
        this.engine = engine;
        currentTime = 0f;
        trackedEmitterHandler = new EmitterBufferHandler();
        engine.addLayeredRenderingPlugin(this);
        engine.getCustomData().put(customDataKey, this);
    }

    static EmitterBufferHandler getTrackedEmitterHandler() {
        Particles instance = getInstance();
        return instance == null ? null : instance.trackedEmitterHandler;
    }

    /** This is done automatically and should not be manually called. */
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

        for (Iterator<ParticleStream<? extends IEmitter>> iterator = particleStreams.iterator(); iterator.hasNext(); ) {
            ParticleStream<? extends IEmitter> particleStream = iterator.next();
            boolean finished = false;
            if (particleStream.deathTime != null && particleStream.deathTime <= currentTime) {
                finished = true;
            } else if (particleStream.finished) {
                finished = true;
            } else {
                if (anchorPoints.containsKey(particleStream.emitter)) {
                    CombatEntityAPI entity = anchorPoints.get(particleStream.emitter);
                    if (!engine.isEntityInPlay(entity) || entity.isExpired()) {
                        finished = true;
                    }
                }
            }
            if (finished) {
                particleStream.finish();
                iterator.remove();
            } else {
                particleStream.advance(amount);
            }
        }

        Iterator<Map.Entry<IEmitter, CombatEntityAPI>> itr = anchorPoints.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<IEmitter, CombatEntityAPI> entry = itr.next();
            CombatEntityAPI entity = entry.getValue();
            entry.getKey().getLocation().set(entity.getLocation());
            if (!engine.isEntityInPlay(entity) || entity.isExpired()) {
                itr.remove();
            }
        }

        currentTime += amount;
    }

    /**
     * Anchors an {@code emitter} to a {@link CombatEntityAPI}. Sets the {@code emitter}'s location to the {@code entity}'s
     * location every frame until the entity is no longer in play. Will also kill any particle streams using an anchored
     * emitter if that emitter's anchor entity is no longer in play. <br>
     * By default, particles will only take into account their emitter's location at the time of generation. To change this
     * behavior so that particles follow their emitter throughout their lifecycle, use {@link IEmitter#enableDynamicAnchoring()}.
     *
     * @param emitter Emitter to anchor
     * @param entity The combat entity to anchor to
     */
    public static void anchorEmitter(IEmitter emitter, CombatEntityAPI entity) {
        Particles instance = getInstance();
        if (instance == null) return;

        instance.anchorPoints.put(emitter, entity);
    }

    /**
     * Same as {@link #initialize(Vector2f, SpriteAPI, int, int, int)}, but uses a default particle sprite instead
     * of sampling a texture.
     */
    public static Emitter initialize(Vector2f location, int sfactor, int dfactor, int blendMode) {
        return initialize(location, (SpriteAPI) null, sfactor, dfactor, blendMode);
    }

    /** Same as  {@link #initialize(Vector2f, SpriteAPI, int, int, int)}, but uses the default
     * additive blending and a default particle sprite. */
    public static Emitter initialize(Vector2f location) {
        return initialize(location, (SpriteAPI) null);
    }

    /**
     *  Initialize an {@link Emitter}. The {@code Emitter}'s location will be set to {@code location}, specified
     *  in world coordinates. The {@code Emitter}'s attributes will be initialized to
     *  default values and should be set in subsequent calls. To generate particles,
     *  use {@link #burst} or {@link #stream}. <br><br>
     *  Note: Does not load {@code sprite}'s texture into memory. Textures not in {@code data/config/settings.json}
     *  are not automatically loaded. Use {@link #initialize(Vector2f, String, int, int, int)} to load
     *  loose textures into memory.
     *
     * @param sprite The texture that the particles should sample when rendered. Must already have
     *               been loaded into memory.
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
    public static Emitter createCopy(Emitter copy, SpriteAPI sprite, int sfactor, int dfactor, int blendMode) {
        Emitter emitter = initialize(copy.location, sprite, sfactor, dfactor, blendMode);
        emitter.setAxis(copy.xAxis);
        emitter.setLocation(copy.location);
        emitter.setLayer(copy.layer);
        emitter.setSyncSize(copy.syncSize);
        emitter.setInactiveBorder(copy.inactiveBorder);
        emitter.life(copy.minLife, copy.maxLife);
        emitter.fadeTime(copy.minFadeIn, copy.maxFadeIn, copy.minFadeOut, copy.maxFadeOut);
        emitter.offset(copy.minOffset, copy.maxOffset);
        emitter.velocity(copy.minVelocity, copy.maxVelocity);
        emitter.acceleration(copy.minAcceleration, copy.maxAcceleration);
        emitter.circleOffset(copy.minPositionSpread, copy.maxPositionSpread);
        emitter.circleVelocity(copy.minVelocitySpread, copy.maxVelocitySpread);
        emitter.circleAcceleration(copy.minAccelerationSpread, copy.maxAccelerationSpread);
        emitter.facing(copy.minTheta, copy.maxTheta);
        emitter.turnRate(copy.minW, copy.maxW);
        emitter.turnAcceleration(copy.minAlpha, copy.maxAlpha);
        emitter.size(copy.minSizeDataX[0], copy.maxSizeDataX[0], copy.minSizeDataY[0], copy.maxSizeDataY[0]);
        emitter.growthRate(copy.minSizeDataX[1], copy.maxSizeDataX[1], copy.minSizeDataY[1], copy.maxSizeDataY[1]);
        emitter.growthAcceleration(copy.minSizeDataX[2], copy.maxSizeDataX[2], copy.minSizeDataY[2], copy.maxSizeDataY[2]);
        emitter.colorHSVA(copy.startColor);
        emitter.randomHSVA(copy.startColorRandom);
        emitter.colorShiftHSVA(copy.minColorShift, copy.maxColorShift);
        emitter.radialVelocity(copy.minRadialVelocity, copy.maxRadialVelocity);
        emitter.radialAcceleration(copy.minRadialAcceleration, copy.maxRadialAcceleration);
        emitter.revolutionRate(copy.minRadialW, copy.maxRadialW);
        emitter.revolutionAcceleration(copy.minRadialAlpha, copy.maxRadialAlpha);
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
     * Same as {@link #createCopy(Emitter, SpriteAPI, int, int, int)}, but also copies {@code copy}'s
     * blending settings.
     *
     * @param copy {@link Emitter} whose properties to copy
     * @param sprite Texture to use for the new emitter
     * @return An emitter with the same properties as {@code copy}
     */
    public static Emitter createCopy(Emitter copy, SpriteAPI sprite) {
        return createCopy(copy, sprite, copy.sfactor, copy.dfactor, copy.blendMode);
    }

    /**
     * Same as {@link #createCopy(Emitter, SpriteAPI, int, int, int)}, but also copies {@code copy}'s
     * sprite and blending settings.
     *
     * @param copy {@link Emitter} whose properties to copy
     * @return An emitter with the same properties as {@code copy}
     */
    public static Emitter createCopy(Emitter copy) {
        return createCopy(copy, copy.sprite);
    }

     /** Same as {@link #initialize(Vector2f, SpriteAPI, int, int, int)}, but uses
     * additive blending by default. */
    public static Emitter initialize(Vector2f location, SpriteAPI sprite) {
        return initialize(location, sprite, GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL14.GL_FUNC_ADD);
    }

    /** Takes the sprite location instead of a SpriteAPI object.
     * Will also load the texture into memory if it hasn't already been loaded.
     * All textures loaded this way are automatically unloaded periodically and at the start of each combat.
     *
     * @see #initialize(Vector2f, SpriteAPI, int, int, int) */
    public static Emitter initialize(Vector2f location, String spriteLoc, int sfactor, int dfactor, int blendMode) {
        SpriteAPI sprite = Utils.getLoadedSprite(spriteLoc);
        return initialize(location, sprite, sfactor, dfactor, blendMode);
    }

    /**
     *  Uses default additive blending.
     *  @see #initialize(Vector2f, String, int, int, int) */
    public static Emitter initialize(Vector2f location, String spriteLoc) {
        return initialize(location, spriteLoc, GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL14.GL_FUNC_ADD);
    }

    /**
     * Generates an instantaneous burst of particles.
     *
     * @param emitter {@link IEmitter} to use.
     * @param count Number of particles to generate.
     *
     * @return Whether the particles were successfully generated. If {@code false}, this generally means that
     * the combat engine is {@code null}.
     */
    public static boolean burst(IEmitter emitter, int count) {
        return burst(emitter, count, 0);
    }

    static boolean burst(IEmitter emitter, int count, int startIndex) {
        if (count <= 0) return true;

        Particles instance = getInstance();
        if (instance == null) {
            return false;
        }

        ParticleType type = new ParticleType(
                emitter.getSprite(),
                emitter.getBlendSourceFactor(),
                emitter.getBlendDestinationFactor(),
                emitter.getBlendFunc(),
                emitter.getLayer());

        SortedMap<ParticleType, Pair<ParticleAllocator, ParticleRenderer>> subMap = instance.particleMap.get(type.layer);
        if (subMap == null) {
            subMap = new TreeMap<>();
            instance.particleMap.put(type.layer, subMap);
        }
        Pair<ParticleAllocator, ParticleRenderer> pair = subMap.get(type);

        ParticleAllocator allocator;
        if (pair == null) {
            allocator = new ParticleAllocator(type);
            ParticleRenderer renderer = new ParticleRenderer(
                    emitter.getLayer(),
                    allocator,
                    instance);
            subMap.put(type, new Pair<>(allocator, renderer));
        } else {
            allocator = pair.one;
        }

        allocator.allocateParticles(emitter, count, startIndex, instance.currentTime, instance.engine.getViewport());
        return true;
    }

    /** Action that can be performed before each generation in a {@link #stream(IEmitter, int, float, float, StreamAction)} call. */
    public interface StreamAction<T extends IEmitter> {
        /**
         * @param emitter {@link IEmitter} that is about to generate particles.
         * @return If {@code false}, the stream will stop generating particles. Otherwise, has no effect.
         */
        boolean apply(T emitter);
    }

    /**
     *  Generates a continuous stream of particles.
     *
     * @param emitter {@link IEmitter} to use.
     * @param particlesPerBurst Number of particles that should be generated at once.
     * @param particlesPerSecond Total number of particles generated per second.
     * @param duration Amount of time this particle stream should last. If negative, the stream will never expire.
     */
    public static void stream(final IEmitter emitter, int particlesPerBurst, float particlesPerSecond, float duration) {
        stream(emitter, particlesPerBurst, particlesPerSecond, duration, null);
    }


    /**
     *  Generates a continuous stream of particles.
     *
     * @param emitter {@link IEmitter} to use.
     * @param particlesPerBurst Number of particles that should be generated at once.
     * @param particlesPerSecond Total number of particles generated per second.
     * @param maxDuration Maximum amount of time this particle stream should last. If negative, the stream
     *                    will not naturally expire.
     * @param doBeforeGenerating Custom function that's called immediately before each particle generation sequence in this stream.
     *                           Returning {@code false} will end the stream.
     */
    public static <T extends IEmitter> void stream(
            final T emitter,
            int particlesPerBurst,
            float particlesPerSecond,
            float maxDuration,
            @Nullable StreamAction<T> doBeforeGenerating) {
        final Particles instance = getInstance();
        if (instance == null) {
            return;
        }
        stream(emitter, particlesPerBurst, particlesPerSecond, maxDuration, doBeforeGenerating, null);
    }

    /**
     *  Generates a continuous stream of particles.
     *
     * @param emitter {@link IEmitter} to use.
     * @param particlesPerBurst Number of particles that should be generated at once.
     * @param particlesPerSecond Total number of particles generated per second.
     * @param maxDuration Maximum amount of time this particle stream should last. If negative, the stream
     *                    will not naturally expire.
     * @param doBeforeGenerating Custom function that's called immediately before each particle generation sequence in this stream.
     *                           Returning {@code false} will end the stream.
     * @param doWhenFinished Custom function that's called when the stream expires, which can occur either naturally, if
     *                       doBeforeGenerating returns {@code false}, or if the stream's emitter's anchor point is removed from play.
     *                       The return value is not used.
     */
    public static <T extends IEmitter> void stream(
            final T emitter,
            int particlesPerBurst,
            float particlesPerSecond,
            float maxDuration,
            @Nullable StreamAction<T> doBeforeGenerating,
            @Nullable StreamAction<T> doWhenFinished) {
        final Particles instance = getInstance();
        if (instance == null) {
            return;
        }

        instance.particleStreams.add(new ParticleStream<>(
                emitter,
                particlesPerBurst,
                particlesPerSecond,
                maxDuration < 0 ? null : instance.currentTime + maxDuration,
                doBeforeGenerating,
                doWhenFinished
        ));
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
