package particleengine;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.util.Comparator;

class ParticleType implements Comparable<ParticleType> {
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

    static final Comparator<SpriteAPI> spriteComparator = new Comparator<SpriteAPI>() {
        @Override
        public int compare(SpriteAPI a, SpriteAPI b) {
            if (a == null && b != null) return -1;
            if (a != null && b == null) return 1;
            if (a == null) return 0;
            // Just sort on id at this point
            return Integer.compare(a.getTextureId(), b.getTextureId());
        }
    };

    @Override
    public int compareTo(ParticleType otherType) {
        if (layer != otherType.layer) return layer.compareTo(otherType.layer);
        // sprite is used as a branching condition in fragment shader, so sort on this first
        int spriteComparison = spriteComparator.compare(sprite, otherType.sprite);
        if (spriteComparison != 0) {
            return spriteComparison;
        }
        if (sfactor != otherType.sfactor) return Integer.compare(sfactor, otherType.sfactor);
        if (dfactor != otherType.dfactor) return Integer.compare(dfactor, otherType.dfactor);
        return Integer.compare(blendMode, otherType.blendMode);
    }

//    @Override
//    public boolean equals(Object o) {
//        if (!(o instanceof ParticleType)) {
//            return false;
//        }
//
//        ParticleType other = (ParticleType) o;
//        if (other.sprite == null && sprite != null) return false;
//        if (other.sprite != null && sprite == null) return false;
//        return (sprite == null || sprite.getTextureId() == other.sprite.getTextureId())
//                && sfactor == other.sfactor
//                && dfactor == other.dfactor
//                && blendMode == other.blendMode
//                && layer == other.layer;
//    }
//
//    @Override
//    public int hashCode() {
//        int hash = 1;
//        if (sprite != null) {
//            hash = 31 * hash + sprite.getTextureId();
//        }
//        hash = 31 * hash + sfactor;
//        hash = 31 * hash + dfactor;
//        hash = 31 * hash + blendMode;
//        hash = 31 * hash + layer.ordinal();
//        return hash;
//    }

}
