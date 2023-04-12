package particleengine;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.SortedSet;
import java.util.TreeSet;

class ParticleAllocator {
    /** In number of floats. */
    static final int INITIAL_BUFFER_SIZE = 4096, MAX_BUFFER_SIZE = 2 << 29 - 1;
    /**
     * If the fraction of alive particles in {@code combinedBuffer} is less than this number,
     * will reallocate the buffer.
     */
    static final float REFACTOR_FILL_FRACTION = 0.5f;
    int bufferSize = INITIAL_BUFFER_SIZE, bufferPosition = 0;
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

        FloatBuffer initialBuffer = BufferUtils.createFloatBuffer(INITIAL_BUFFER_SIZE);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, initialBuffer, GL15.GL_DYNAMIC_DRAW);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    void registerParticleCreation(final AllocatedClusterData clusterData) {
        int count = clusterData.sizeInFloats / Particles.FLOATS_PER_PARTICLE;
        particleCount += count;
        // See if we can merge this cluster with the previously generated one
        if (lastAllocated != null
                && lastAllocated.generationTime == clusterData.generationTime
                && lastAllocated.deathTime == clusterData.deathTime
                && lastAllocated.locationInBuffer + lastAllocated.sizeInFloats == clusterData.locationInBuffer) {
            lastAllocated.updateSize(lastAllocated.sizeInFloats + count*Particles.FLOATS_PER_PARTICLE);
        }
        else {
            allocatedClusters.add(clusterData);
            lastAllocated = clusterData;
            Particles.doLater(new Particles.Action() {
                @Override
                public void perform() {
                    registerParticleDeath(clusterData);
                }
            }, clusterData.deathTime - clusterData.generationTime);
        }
    }

    void registerParticleDeath(AllocatedClusterData clusterData) {
        allocatedClusters.remove(clusterData);
        particleCount -= clusterData.sizeInFloats / Particles.FLOATS_PER_PARTICLE;
        // Delete this allocator if there are no particles left
        if (particleCount <= 0) {
            Particles.removeType(type);
            return;
        }

        int particleCountInBuffer = bufferPosition / Particles.FLOATS_PER_PARTICLE;
        if (particleCount < REFACTOR_FILL_FRACTION * particleCountInBuffer) {
            reallocateBuffer();
        }
    }

    private void reallocateBuffer() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        long numBytes = (long) bufferPosition * Particles.FLOAT_SIZE;
        ByteBuffer buffer = GL30.glMapBufferRange(
                GL15.GL_ARRAY_BUFFER,
                0,
                numBytes,
                GL30.GL_MAP_READ_BIT | GL30.GL_MAP_WRITE_BIT,
                null);
        buffer.order(ByteOrder.nativeOrder());

        FloatBuffer fb1 = buffer.asFloatBuffer();
        FloatBuffer fb2 = fb1.duplicate();

        fb2.position(0);
        for (AllocatedClusterData clusterData : allocatedClusters) {
            fb1.limit(clusterData.locationInBuffer + clusterData.sizeInFloats);
            fb1.position(clusterData.locationInBuffer);
            clusterData.updateLocation(fb2.position());
            fb2.put(fb1);
        }
        bufferPosition = fb2.position();

        GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    void allocateParticles(Emitter emitter, int count, float startTime) {
        FloatBuffer buffer = emitter.generate(count, startTime);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        long requiredSize = bufferPosition + buffer.limit();
        int allocatedLocation = bufferPosition;
        // Current buffer is big enough to fit this emitter
        if (requiredSize <= bufferSize) {
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, (long) bufferPosition*Particles.FLOAT_SIZE, buffer);
        }
        // Current buffer is too small, we must create a new buffer
        else {
            // Too many particles, can't allocate
            if (bufferSize >= MAX_BUFFER_SIZE) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                return;
            }

            int newSize = Utils.nearestBiggerPowerOfTwo(requiredSize, INITIAL_BUFFER_SIZE, MAX_BUFFER_SIZE);

            FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newSize);
            if (bufferPosition > 0) {
                ByteBuffer existingBuffer = GL30.glMapBufferRange(
                        GL15.GL_ARRAY_BUFFER,
                        0,
                        (long) bufferPosition * Particles.FLOAT_SIZE,
                        GL30.GL_MAP_READ_BIT | GL30.GL_MAP_WRITE_BIT,
                        null);
                existingBuffer.order(ByteOrder.nativeOrder());
                newBuffer.put(existingBuffer.asFloatBuffer());
                GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
            }
            newBuffer.put(buffer);
            // Store the whole buffer
            newBuffer.position(0);
            newBuffer.limit(newBuffer.capacity());
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newBuffer, GL15.GL_DYNAMIC_DRAW);
            bufferSize = newSize;
        }
        bufferPosition = (int) requiredSize;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        AllocatedClusterData clusterData =
                new AllocatedClusterData(
                        allocatedLocation,
                        count * Particles.FLOATS_PER_PARTICLE,
                        startTime,
                        startTime + emitter.maxLife);
        registerParticleCreation(clusterData);
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
