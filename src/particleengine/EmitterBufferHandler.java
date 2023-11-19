package particleengine;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.util.*;

class EmitterBufferHandler {
    /** Should be twice the size of {@code locations} in {@code particle.vert}.
     *  Specified in number of emitters, each of which needs two floats of storage. */
    static final int MAX_BUFFER_SIZE = 2048;

    /** Fraction of most stale emitters to remove when the buffer is full. */
    static final float REMOVE_WHEN_FULL_FRAC = 0.5f;
    final PriorityQueue<Integer> freePositions = new PriorityQueue<>();
    final Set<Integer> filledPositions = new HashSet<>();
    final IEmitter[] trackedEmitters = new IEmitter[MAX_BUFFER_SIZE];
    final int bufferIndex;

    EmitterBufferHandler() {
        for (int i = 0; i < MAX_BUFFER_SIZE; i++) {
            freePositions.add(i);
        }

        bufferIndex = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, bufferIndex);
        FloatBuffer initialBuffer = BufferUtils.createFloatBuffer(2*MAX_BUFFER_SIZE);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, initialBuffer, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, ParticleShader.emitterUniformBlockBinding, bufferIndex);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    static void freeEmitter(int index) {
        final EmitterBufferHandler instance = Particles.getTrackedEmitterHandler();
        if (instance == null) return;
        if (index < 0) return;

        instance.filledPositions.remove(index);
        instance.freePositions.add(index);
    }

    static int trackEmitter(IEmitter emitter) {
        final EmitterBufferHandler instance = Particles.getTrackedEmitterHandler();
        if (instance == null) return -1;

        if (instance.freePositions.isEmpty()) {
            // No free slots are available, we have to remove some emitters
            // Sort by staleness -- last particle death time
            List<Integer> sortedFilledPositions = new ArrayList<>(instance.filledPositions);
            Collections.sort(sortedFilledPositions, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Float.compare(
                            instance.trackedEmitters[a].lastParticleDeathTime,
                            instance.trackedEmitters[b].lastParticleDeathTime);
                }
            });

            int numToRemove = (int) (REMOVE_WHEN_FULL_FRAC *  sortedFilledPositions.size());
            for (int i = 0; i < numToRemove; i++) {
                instance.filledPositions.remove(i);
                instance.freePositions.add(i);
            }
        }

        int index = instance.freePositions.remove();
        instance.filledPositions.add(index);
        instance.trackedEmitters[index] = emitter;

        return index;
    }

    int getBufferIndex() {
        EmitterBufferHandler instance = Particles.getTrackedEmitterHandler();
        return instance == null ? 0 : instance.bufferIndex;
    }

    FloatBuffer locationsToFloatBuffer() {
        EmitterBufferHandler instance = Particles.getTrackedEmitterHandler();
        if (instance == null) return null;

        FloatBuffer buffer = BufferUtils.createFloatBuffer(2*MAX_BUFFER_SIZE);
        for (int i : instance.filledPositions) {
            Vector2f emitterLocation = instance.trackedEmitters[i].getLocation();
            buffer.put(2 * i, emitterLocation.x);
            buffer.put(2 * i + 1, emitterLocation.y);
        }
        return buffer;
    }
}
