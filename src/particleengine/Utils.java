package particleengine;

import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.Objects;

class Utils {
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

    public static float randBetween(float a, float b) {
        return Misc.random.nextFloat() * (b - a) + a;
    }

    public static Vector2f randomPointInCircle(Vector2f center, float radius) {
        float theta = Misc.random.nextFloat() * 2f *  (float) Math.PI;
        float r = radius * (float) Math.sqrt(Misc.random.nextFloat());
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

    public static int nearestBiggerPowerOfTwo(long n, int smallest) {
        int r = smallest;
        while (r < n) {
            r <<= 1;
            if (r < 0) {
                return Integer.MAX_VALUE;
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
}