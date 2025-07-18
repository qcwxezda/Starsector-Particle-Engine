package particleengine;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.util.vector.Vector2f;

import java.nio.FloatBuffer;
import java.util.*;

class EmitterBufferHandler {

    /**  Specified in the number of emitters, each of which is 16 bytes. */
    static final int MAX_BUFFER_SIZE = 10000;

    /** Fraction of most stale emitters to remove when the buffer is full. */
    static final float REMOVE_WHEN_FULL_FRAC = 0.5f;
    final PriorityQueue<Integer> freePositions = new PriorityQueue<>();
    final SortedSet<Integer> filledPositions = new TreeSet<>(Collections.reverseOrder());
    final IEmitter[] trackedEmitters = new IEmitter[MAX_BUFFER_SIZE];
    static final int ssboBufferIndex;
    static final FloatBuffer emitterLocations;

    static {
        ssboBufferIndex = GL15.glGenBuffers();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssboBufferIndex);
        emitterLocations = BufferUtils.createFloatBuffer(4*MAX_BUFFER_SIZE);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, emitterLocations, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ParticleShader.trackedEmitterBinding, ssboBufferIndex);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    EmitterBufferHandler() {
        for (int i = 0; i < MAX_BUFFER_SIZE; i++) {
            freePositions.add(i);
        }
    }

    void updateTrackedEmitters(float currentCampaignTime, float currentCombatTime) {
        for (Iterator<Integer> iterator = filledPositions.iterator(); iterator.hasNext(); ) {
            int i = iterator.next();
            IEmitter emitter = trackedEmitters[i];
            Vector2f emitterLocation = emitter.getLocation();
            emitterLocations.put(4 * i, emitterLocation.x);
            emitterLocations.put(4 * i + 1, emitterLocation.y);
            emitterLocations.put(4 * i + 2, emitter.getXDir() * Misc.RAD_PER_DEG);
            emitterLocations.put(4 * i + 3, emitter.isSmoothDynamic() ? 1f : 0f);

            // Check if the emitter is dead
            if (emitter.lastCampaignParticleDeathTime < currentCampaignTime &&
                    (emitter.lastCombatParticleDeathTime < currentCombatTime || !Particles.isCombat())) {
                emitter.untrack();
                freePositions.add(i);
                iterator.remove();
                trackedEmitters[i] = null;
            }
        }
    }

    int trackEmitter(IEmitter emitter) {
        if (freePositions.isEmpty()) {
            // No free slots are available, we have to remove some emitters
            // Sort by staleness -- last particle death time
            List<Integer> sortedFilledPositions = new ArrayList<>(filledPositions);
            sortedFilledPositions.sort((a, b) -> Float.compare(
                    trackedEmitters[a].getLastParticleDeathTime(),
                    trackedEmitters[b].getLastParticleDeathTime()));

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

    int getSSBOBufferIndex() {
        return ssboBufferIndex;
    }

    FloatBuffer locationsToFloatBuffer() {
        return emitterLocations;
    }
}
