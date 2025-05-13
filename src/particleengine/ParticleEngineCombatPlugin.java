package particleengine;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;

@SuppressWarnings("unused")
public class ParticleEngineCombatPlugin extends BaseEveryFrameCombatPlugin {
    @Override
    public void init(CombatEngineAPI engine) {
        if (ParticleEngineModPlugin.particlesInstance != null) {
            ParticleEngineModPlugin.particlesInstance.resetCombatData();
            engine.addLayeredRenderingPlugin(ParticleEngineModPlugin.particlesInstance);
        }
    }
}
