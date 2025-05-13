package particleengine;

import com.fs.graphics.LayeredRenderable;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.combat.CombatViewport;

import java.util.EnumSet;
import java.util.Objects;

public class ParticlesCampaignRenderer implements LayeredRenderable<CampaignEngineLayers, CombatViewport> {

    Particles owner;

    public ParticlesCampaignRenderer(Particles owner) {
        this.owner = owner;
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return EnumSet.allOf(CampaignEngineLayers.class);
    }

    @Override
    public void render(CampaignEngineLayers layer, CombatViewport viewport) {
        if (owner.particleMap.containsKey(layer)) {
            owner.preRender(viewport);
            for (var entry : owner.particleMap.get(layer).entrySet()) {
                var loc = entry.getKey().campaignLocation();
                if (loc != null && !Objects.equals(loc.getId(), Utils.getPlayerContainingLocation().getId())) {
                    continue;
                }

                var p = entry.getValue();
                if (p.two.layer().equals(layer)) {
                    p.two.render(viewport);
                }
            }
            owner.postRender();
        }
    }

    @Override
    public int hashCode() {
        return 1657659445;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ParticlesCampaignRenderer;
    }
}
