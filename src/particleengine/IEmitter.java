package particleengine;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;

/** Emitter interface. Extend to make custom particle emitters. */
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
     * @return Source blend factor, e.g. {@code GL11.GL_SRC_ALPHA}.
     */
    public abstract int getBlendSourceFactor();
    /**
     * @return Destination blend factor, e.g. {@code GL11.GL_ONE_MINUS_SRC_ALPHA}.
     */
    public abstract int getBlendDestinationFactor();

    /**
     * @return Blending operation, e.g. {@code GL14.GL_FUNC_ADD}.
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
    public abstract float getRenderRadius();

    /**
     * Construct parameters for a particle. Doesn't actually generate the particle. Use {@link Particles#burst}
     * to generate particles from an emitter.
     *
     * @param id Index of the particle in the burst or stream
     * @return Object containing the necessary data for particle generation
     */
    protected abstract ParticleData initParticle(int id);

    protected final FloatBuffer generate(int count, int startIndex, float startTime, ViewportAPI viewport) {
        if (!Utils.isInViewport(getLocation(), viewport, getRenderRadius())) {
            return null;
        }
        FloatBuffer buffer = BufferUtils.createFloatBuffer(count * Particles.FLOATS_PER_PARTICLE);
        for (int i = 0; i < count; i++) {
            ParticleData data = initParticle(startIndex + i);
            if (data != null) {
                maxLife = Math.max(maxLife, data.life);
                data.addToFloatBuffer(this, startTime, buffer);
            }
        }
        buffer.flip();
        return buffer;
    }

}
