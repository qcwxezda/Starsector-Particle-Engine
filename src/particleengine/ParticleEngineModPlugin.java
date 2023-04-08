package particleengine;

import com.fs.starfarer.api.BaseModPlugin;

@SuppressWarnings("unused")
public class ParticleEngineModPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        ParticleShader.init("particle.vert", "particle.frag");
    }
}
