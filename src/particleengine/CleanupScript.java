package particleengine;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;

public class CleanupScript extends BaseCampaignEventListener {
    public CleanupScript(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        // No way to report simulation finished, so next best place to check buffers to clear is here.
        if (!ParticleEngine.usedVAOs.isEmpty() || !ParticleEngine.usedVBOs.isEmpty()) {
            ParticleEngine.clearBuffers();
        }
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (!ParticleEngine.usedVAOs.isEmpty() || !ParticleEngine.usedVBOs.isEmpty()) {
            ParticleEngine.clearBuffers();
        }
    }
}
