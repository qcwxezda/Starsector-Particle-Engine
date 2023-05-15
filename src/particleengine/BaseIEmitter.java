package particleengine;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

/**
 * Partial implementation of {@link IEmitter} that omits only the particle generation function.
 * All other properties are set to default values and may be overwritten as required.
 */
@SuppressWarnings("unused")
public abstract class BaseIEmitter extends IEmitter {
    @Override
    public Vector2f getLocation() {
        return new Vector2f();
    }

    @Override
    public SpriteAPI getSprite() {
        return null;
    }

    @Override
    public float getXDir() {
        return 0f;
    }

    @Override
    public int getBlendSourceFactor() {
        return GL11.GL_SRC_ALPHA;
    }

    @Override
    public int getBlendDestinationFactor() {
        return GL11.GL_ONE;
    }

    @Override
    public int getBlendFunc() {
        return GL14.GL_FUNC_ADD;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return CombatEngineLayers.ABOVE_PARTICLES_LOWER;
    }

    @Override
    public float getRenderRadius() {
        return 500f;
    }
}
