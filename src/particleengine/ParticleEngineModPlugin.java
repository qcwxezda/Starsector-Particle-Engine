package particleengine;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.OpenGLException;

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
            JSONObject modInfo = Global.getSettings().loadJSON("particleengine_settings.json");
            enabled = modInfo.getBoolean("enabled");
            savedEmittersDirectory = modInfo.getString("savedEmittersDirectory");
        }
        catch (IOException | JSONException e) {
            log.error("Could not read savedEmittersDirectory in mod_info.json. Writing emitters to file will be disabled.", e);
        }

        if (enabled && !GLContext.getCapabilities().OpenGL43) {
            throw new OpenGLException("(Particle Engine) The current OpenGL context doesn't support OpenGL 4.3, which is required to use Particle Engine.\n\nIf your system otherwise supports OpenGL 4.3, this could be because it lacks compatibility profile support, which is required for simultaneous use of legacy features (used by vanilla) and core features (used by Particle Engine).\n\nYou may still use mods that depend on Particle Engine by setting enabled to false in particleengine_settings.json, though visual effects depending on this mod will be absent.\n");
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
