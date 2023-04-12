package particleengine;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;

class ParticleType {
    final SpriteAPI sprite;
    final CombatEngineLayers layer;
    final int sfactor, dfactor, blendMode;

    ParticleType(SpriteAPI sprite, int sfactor, int dfactor, int blendMode, CombatEngineLayers layer) {
        this.sprite = sprite;
        this.sfactor = sfactor;
        this.dfactor = dfactor;
        this.blendMode = blendMode;
        this.layer = layer;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParticleType)) {
            return false;
        }

        ParticleType other = (ParticleType) o;
        if (other.sprite == null && sprite != null) return false;
        if (other.sprite != null && sprite == null) return false;
        return (sprite == null || sprite.getTextureId() == other.sprite.getTextureId())
                && sfactor == other.sfactor
                && dfactor == other.dfactor
                && blendMode == other.blendMode
                && layer == other.layer;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        if (sprite != null) {
            hash = 31 * hash + sprite.getTextureId();
        }
        hash = 31 * hash + sfactor;
        hash = 31 * hash + dfactor;
        hash = 31 * hash + blendMode;
        hash = 31 * hash + layer.ordinal();
        return hash;
    }
}
