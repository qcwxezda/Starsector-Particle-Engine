package particleengine;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

abstract class Utils {

    private static final Logger log = Logger.getLogger(Utils.class);

    /** 4x4 matrix, translation elements in 3rd dimension, 4th dimension is identity */
    public static FloatBuffer getProjectionMatrix(ViewportAPI viewport) {
        float W = viewport.getVisibleWidth();
        float H = viewport.getVisibleHeight();
        float llx = viewport.getLLX();
        float lly = viewport.getLLY();
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        buf.put(new float[] {2f/W, 0f, -2f*llx/W-1f, 0f, 0f, 2f/H, -2f*lly/H-1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f}).flip();
        return buf;
    }

    public static Object toFloatOrPairArray(float item1, float item2) {
        return item1 == item2 ? item1 : new JSONArray(Arrays.asList(item1, item2));
    }

    /** Size of array is assumed to be the same as size of {@code defaults}. */
    public static float[] readJSONArrayOrFloat(JSONObject json, String name, float[] defaults, boolean allowFloats) throws JSONException {
        if (!json.has(name)) {
            return defaults;
        }
        int length = defaults.length;
        float[] res = new float[length];
        JSONArray array = json.optJSONArray(name);
        if (array == null && !allowFloats) {
            throw new JSONException(String.format("JSON array expected for property [%s]", name));
        }
        else if (array == null) {
            float value = (float) json.getDouble(name);
            for (int i = 0; i < length; i++) {
                res[i] = value;
            }
        }
        else {
            if (array.length() != length) {
                throw new JSONException(String.format("Wrong number of elements in [%s]. Expected: %s, Received: %s", name, length, array.length()));
            }
            for (int i = 0; i < length; i++) {
                res[i] = (float) array.getDouble(i);
            }
        }
        return res;
    }

    public static SpriteAPI getLoadedSprite(String loc, Collection<String> record) {
        SpriteAPI sprite = Global.getSettings().getSprite(loc);
        if (sprite.getTextureId() <= 0) {
            try {
                Global.getSettings().loadTexture(loc);
                sprite = Global.getSettings().getSprite(loc);
                record.add(loc);
            } catch (IOException e) {
                log.warn(String.format("(Particle Engine) Failed to a load texture at location [%s] into memory", loc), e);
                sprite = null;
            }
        }
        return sprite;
    }

    public static float[] readJSONArrayOrFloat(JSONObject json, String name, float[] defaults) throws JSONException {
        return readJSONArrayOrFloat(json, name, defaults, true);
    }

    public static float randBetween(float a, float b) {
        return Misc.random.nextFloat() * (b - a) + a;
    }

    public static Vector2f randomPointInRing(Vector2f center, float inRadius, float outRadius) {
        float theta = Misc.random.nextFloat() * 2f * (float) Math.PI;
        float r = (float) Math.sqrt(Misc.random.nextFloat() * (outRadius*outRadius - inRadius*inRadius) + inRadius*inRadius);
        return new Vector2f(center.x + r*(float)Math.cos(theta), center.y + r*(float)Math.sin(theta));
    }

    public static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(ParticleEngineModPlugin.class.getResourceAsStream(path))));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public static int nearestBiggerPowerOfTwo(long n, int smallest, int biggest) {
        int r = smallest;
        while (r < n) {
            r <<= 1;
            if (r < 0 || r >= biggest) {
                return biggest;
            }
        }
        return r;
    }

    /** {@code {e1, e2}} should be a basis vector pair. */
    public static Vector2f toStandardBasis(Vector2f v, Vector2f e1, Vector2f e2) {
        Vector2f s1 = new Vector2f(e1), s2 = new Vector2f(e2);
        s1.scale(v.x);
        s2.scale(v.y);
        return Vector2f.add(s1, s2, null);
    }

    public static void toRGBA(float[] hsva, float[] dest) {
        float h = hsva[0], s = hsva[1], v = hsva[2], a = hsva[3];
        float c = v*s;
        float x = c*(1f - Math.abs((h/60f) % 2f - 1f));
        float m = v-c;
        float r = 0f, g = 0f, b = 0f;
        switch ((int) Math.floor(h/60f)) {
            case 0: r = c; g = x; break;
            case 1: r = x; g = c; break;
            case 2: g = c; b = x; break;
            case 3: g = x; b = c; break;
            case 4: r = x; b = c; break;
            case 5: r = c; b = x; break;
        }
        dest[0] = r + m;
        dest[1] = g + m;
        dest[2] = b + m;
        dest[3] = a;
    }

    public static void toHSVA(float[] rgba, float[] dest) {
        float r = rgba[0], g = rgba[1], b = rgba[2];
        float CMax = Math.max(r, Math.max(g, b));
        float CMin = Math.min(r, Math.min(g, b));
        float delta = CMax - CMin;

        if (delta == 0f) {
            dest[0] = 0f;
        } else if (CMax == r) {
            dest[0] = 60f*(((g-b)/delta) % 6f);
        } else if (CMax == g) {
            dest[0] = 60f*((b-r)/delta + 2);
        } else {
            dest[0] = 60f*((r-g)/delta + 4);
        }

        dest[1] = CMax == 0f ? 0f : delta / CMax;
        dest[2] = CMax;
        dest[3] = rgba[3];
    }
}
