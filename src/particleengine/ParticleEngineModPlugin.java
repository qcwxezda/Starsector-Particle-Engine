package particleengine;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

@SuppressWarnings("unused")
public class ParticleEngineModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        ParticleShader.init("particle.vert", "particle.frag");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().addTransientListener(new CleanupScript(false));
    }
}
