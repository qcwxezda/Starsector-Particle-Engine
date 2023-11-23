package particleengine;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.util.*;

class EmitterBufferHandler {
    /** Should equal the size of {@code locations} in {@code particle.vert}.
     *  Specified in number of emitters, each of which needs two floats of storage. */
    static final int MAX_BUFFER_SIZE = 1024;

    /** Fraction of most stale emitters to remove when the buffer is full. */
    static final float REMOVE_WHEN_FULL_FRAC = 0.5f;
    final PriorityQueue<Integer> freePositions = new PriorityQueue<>();
    final SortedSet<Integer> filledPositions = new TreeSet<>(Collections.reverseOrder());
    final IEmitter[] trackedEmitters = new IEmitter[MAX_BUFFER_SIZE];
    static final int uboBufferIndex;
    static final FloatBuffer emitterLocations;

    static {
        uboBufferIndex = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uboBufferIndex);
        emitterLocations = BufferUtils.createFloatBuffer(4*MAX_BUFFER_SIZE);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, emitterLocations, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, ParticleShader.emitterUniformBlockBinding, uboBufferIndex);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    EmitterBufferHandler() {
        for (int i = 0; i < MAX_BUFFER_SIZE; i++) {
            freePositions.add(i);
        }
    }

    void updateTrackedEmitters(float currentTime) {
        for (Iterator<Integer> iterator = filledPositions.iterator(); iterator.hasNext(); ) {
            int i = iterator.next();
            IEmitter emitter = trackedEmitters[i];
            Vector2f emitterLocation = emitter.getLocation();
            emitterLocations.put(4 * i, emitterLocation.x);
            emitterLocations.put(4 * i + 1, emitterLocation.y);
            emitterLocations.put(4 * i + 2, emitter.getXDir() * Misc.RAD_PER_DEG);

            // Check if the emitter is dead
            if (emitter.lastParticleDeathTime < currentTime) {
                emitter.untrack();
                freePositions.add(i);
                iterator.remove();
            }
        }
    }

    int trackEmitter(IEmitter emitter) {
        if (freePositions.isEmpty()) {
            // No free slots are available, we have to remove some emitters
            // Sort by staleness -- last particle death time
            List<Integer> sortedFilledPositions = new ArrayList<>(filledPositions);
            Collections.sort(sortedFilledPositions, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Float.compare(
                            trackedEmitters[a].lastParticleDeathTime,
                            trackedEmitters[b].lastParticleDeathTime);
                }
            });

            int numToRemove = (int) Math.ceil(REMOVE_WHEN_FULL_FRAC *  sortedFilledPositions.size());
            for (int i = 0; i < numToRemove; i++) {
                int j = sortedFilledPositions.get(i);
                filledPositions.remove(j);
                freePositions.add(j);
            }
        }

        int index = freePositions.remove();
        filledPositions.add(index);
        trackedEmitters[index] = emitter;

        return index;
    }

    int getHighestFilledPosition() {
        if (filledPositions.isEmpty()) return -1;
        return filledPositions.iterator().next();
    }

    int getUboBufferIndex() {
        return uboBufferIndex;
    }

    FloatBuffer locationsToFloatBuffer() {
        return emitterLocations;
    }
}
