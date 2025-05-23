package particleengine;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

@SuppressWarnings("unused")
public class ParticleEngineModPlugin extends BaseModPlugin {
    static String savedEmittersDirectory = null;
    private static final Logger log = Logger.getLogger(ParticleEngineModPlugin.class);
    public static boolean enabled = true;
    static Particles particlesInstance = null;

    @Override
    public void onApplicationLoad() {
        try {
            JSONObject modInfo = Global.getSettings().loadJSON("particleengine.json");
            enabled = modInfo.getBoolean("enabled");
            savedEmittersDirectory = modInfo.getString("savedEmittersDirectory");
        }
        catch (IOException | JSONException e) {
            log.error("Could not read savedEmittersDirectory in mod_info.json. Writing emitters to file will be disabled.", e);
        }

        if (enabled) {
            ParticleShader.init("particle.vert", "particle.frag");
            particlesInstance = new Particles();
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        if (enabled) {
            Particles.reset();
            Global.getSector().addTransientScript(particlesInstance);
            Global.getSector().getListenerManager().addListener(particlesInstance, true);
            particlesInstance.reportCurrentLocationChanged(null, Utils.getPlayerContainingLocation());
        }
    }
}
