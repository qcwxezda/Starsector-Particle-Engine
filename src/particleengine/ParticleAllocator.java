package particleengine;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.util.SortedSet;
import java.util.TreeSet;

class ParticleAllocator {
    /** In number of floats. */
    static final int INITIAL_BUFFER_SIZE = 1024;
    /**
     * If the fraction of alive particles in {@code combinedBuffer} is less than this number,
     * will reallocate the buffer.
     */
    static final float REFACTOR_FILL_FRACTION = 0.5f;
    FloatBuffer combinedBuffer;
    SortedSet<AllocatedClusterData> allocatedClusters = new TreeSet<>();
    AllocatedClusterData lastAllocated = null;
    int particleCount = 0;
    final int vao, vbo;
    final ParticleType type;

    /**
     * Sets up an empty buffer with {@value INITIAL_BUFFER_SIZE} elements.
     */
    public ParticleAllocator(ParticleType type) {
        this.type = type;
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        Particles.usedVAOs.add(vao);
        Particles.usedVBOs.add(vbo);

        int offset = 0;
        for (int i = 0; i < Particles.VERTEX_ATTRIB_SIZES.length; i++) {
            GL20.glEnableVertexAttribArray(i);
            GL20.glVertexAttribPointer(i, Particles.VERTEX_ATTRIB_SIZES[i], GL11.GL_FLOAT, false, Particles.BYTES_PER_PARTICLE, offset);
            GL33.glVertexAttribDivisor(i, 1);
            offset += Particles.VERTEX_ATTRIB_SIZES[i] * Particles.FLOAT_SIZE;
        }

        combinedBuffer = BufferUtils.createFloatBuffer(INITIAL_BUFFER_SIZE);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, combinedBuffer, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    void registerParticleCreation(final AllocatedClusterData clusterData, Cluster cluster) {
        particleCount += cluster.count;
        // See if we can merge this cluster with the previously generated one
        if (lastAllocated != null
                && lastAllocated.generationTime == clusterData.generationTime
                && lastAllocated.deathTime == clusterData.deathTime
                && lastAllocated.locationInBuffer + lastAllocated.sizeInFloats == clusterData.locationInBuffer) {
            lastAllocated.updateSize(lastAllocated.sizeInFloats + cluster.count*Particles.FLOATS_PER_PARTICLE);
        }
        else {
            allocatedClusters.add(clusterData);
            lastAllocated = clusterData;
            Particles.doLater(new Particles.Action() {
                @Override
                public void perform() {
                    registerParticleDeath(clusterData);
                }
            }, cluster.maxLife);
        }
    }

    void registerParticleDeath(AllocatedClusterData clusterData) {
        allocatedClusters.remove(clusterData);
        particleCount -= clusterData.sizeInFloats / Particles.FLOATS_PER_PARTICLE;
        // Delete this allocator if there are no particles left
        if (particleCount <= 0) {
            Particles.removeAllocator(type);
        }

        int particleCountInBuffer = combinedBuffer.position() / Particles.FLOATS_PER_PARTICLE;
        if (particleCount < REFACTOR_FILL_FRACTION * particleCountInBuffer) {
            reallocateBuffer();
        }
    }

    private void reallocateBuffer() {
        combinedBuffer.flip();
        int numFloats = combinedBuffer.limit();
        int newSize = Utils.nearestBiggerPowerOfTwo(numFloats, INITIAL_BUFFER_SIZE);

        FloatBuffer newCombinedBuffer = BufferUtils.createFloatBuffer(newSize);

        for (AllocatedClusterData clusterData : allocatedClusters) {
            combinedBuffer.limit(clusterData.locationInBuffer + clusterData.sizeInFloats);
            combinedBuffer.position(clusterData.locationInBuffer);
            clusterData.updateLocation(newCombinedBuffer.position());
            newCombinedBuffer.put(combinedBuffer);
        }

        combinedBuffer = newCombinedBuffer;

        // The size of the buffer put into the native call is limit() - position(). We want to allocate
        // the entire buffer, so temporarily set the position to 0.
        int oldPosition = combinedBuffer.position();
        combinedBuffer.position(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, combinedBuffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        combinedBuffer.position(oldPosition);
    }

    void allocateParticles(final Cluster cluster, float startTime) {
        FloatBuffer buffer = cluster.generate(startTime);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        long requiredSize = (long) combinedBuffer.position() + buffer.limit();
        int allocatedLocation = combinedBuffer.position();
        // Current buffer is big enough to fit this cluster
        if (requiredSize <= combinedBuffer.capacity()) {
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, (long) combinedBuffer.position()* Particles.FLOAT_SIZE, buffer);
            combinedBuffer.put(buffer);
        }
        // Current buffer is too small, we must create a new buffer
        else {
            int newSize = Utils.nearestBiggerPowerOfTwo(requiredSize, INITIAL_BUFFER_SIZE);
            FloatBuffer newCombinedBuffer = BufferUtils.createFloatBuffer(newSize);
            combinedBuffer.flip();

            newCombinedBuffer.put(combinedBuffer);
            newCombinedBuffer.put(buffer);
            combinedBuffer = newCombinedBuffer;

            // The size of the buffer put into the native call is limit() - position(). We want to allocate
            // the entire buffer, so temporarily set the position to 0.
            int oldPosition = combinedBuffer.position();
            combinedBuffer.position(0);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, combinedBuffer, GL15.GL_STATIC_DRAW);
            combinedBuffer.position(oldPosition);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        AllocatedClusterData clusterData =
                new AllocatedClusterData(
                        allocatedLocation,
                        cluster.count * Particles.FLOATS_PER_PARTICLE,
                        startTime,
                        startTime + cluster.maxLife);
        registerParticleCreation(clusterData, cluster);
    }

    private static class AllocatedClusterData implements Comparable<AllocatedClusterData> {
        private int locationInBuffer, sizeInFloats;
        private final float generationTime, deathTime;

        private AllocatedClusterData(int locationInBuffer, int size, float generationTime, float deathTime) {
            this.locationInBuffer = locationInBuffer;
            this.sizeInFloats = size;
            this.generationTime = generationTime;
            this.deathTime = deathTime;
        }

        private void updateLocation(int newLocationInBuffer) {
            locationInBuffer = newLocationInBuffer;
        }

        private void updateSize(int newSize) {
            sizeInFloats = newSize;
        }

        @Override
        public int compareTo(AllocatedClusterData o) {
            return Integer.compare(locationInBuffer, o.locationInBuffer);
        }
    }
}
