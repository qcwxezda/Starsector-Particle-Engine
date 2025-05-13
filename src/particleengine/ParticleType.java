package particleengine;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.util.Comparator;

/**
 * T should be one of {@link CampaignEngineLayers} or {@link CombatEngineLayers}.
 */
record ParticleType(SpriteAPI sprite, int sfactor, int dfactor, int blendMode, Object layer, LocationAPI campaignLocation) implements Comparable<ParticleType> {

    static final Comparator<SpriteAPI> spriteComparator = (a, b) -> {
        if (a == null && b != null) return -1;
        if (a != null && b == null) return 1;
        if (a == null) return 0;
        // Just sort on id at this point
        return Integer.compare(a.getTextureId(), b.getTextureId());
    };

    @Override
    public int compareTo(ParticleType otherType) {
        boolean a1 = layer instanceof CombatEngineLayers;
        boolean a2 = otherType.layer instanceof CombatEngineLayers;
        boolean b1 = layer instanceof CampaignEngineLayers;
        boolean b2 = otherType.layer instanceof CampaignEngineLayers;
        if (a1) {
            if (a2) {
                int cmp = ((CombatEngineLayers) layer).compareTo((CombatEngineLayers) otherType.layer);
                if (cmp != 0) return cmp;
            }
            else return -1;
        } else if (b1) {
            if (b2) {
                int cmp = ((CampaignEngineLayers) layer).compareTo((CampaignEngineLayers) otherType.layer);
                if (cmp != 0) return cmp;
            }
            else return 1;
        } else {
            throw new RuntimeException("ParticleType.layer must be either a CombatEngineLayers or CampaignEngineLayers");
        }

        // sprite is used as a branching condition in fragment shader, so sort on this first
        int spriteComparison = spriteComparator.compare(sprite, otherType.sprite);
        if (spriteComparison != 0) {
            return spriteComparison;
        }
        if (sfactor != otherType.sfactor) return Integer.compare(sfactor, otherType.sfactor);
        if (dfactor != otherType.dfactor) return Integer.compare(dfactor, otherType.dfactor);
        if (blendMode != otherType.blendMode) return Integer.compare(blendMode, otherType.blendMode);
        if (campaignLocation == null && otherType.campaignLocation == null) { return 0; }
        if (campaignLocation == null) { return -1; }
        if (otherType.campaignLocation == null) { return 1; }
        return campaignLocation.getId().compareTo(otherType.campaignLocation.getId());
    }
}
