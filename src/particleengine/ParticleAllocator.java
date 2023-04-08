package particleengine;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

class ParticleAllocator {
    /**
     * If the fraction of dead particles in {@code combinedBuffer} is greater than this number,
     * will reallocate the buffer.
     */
    static final float REFACTOR_EMPTY_FRACTION = 0.5f;
    FloatBuffer combinedBuffer;
    int deadParticles = 0;
    final int vao, vbo;

    /**
     * Sets up an empty buffer with {@value ParticleEngine#INITIAL_BUFFER_SIZE} elements.
     */
    public ParticleAllocator() {
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        ParticleEngine.usedVAOs.add(vao);
        ParticleEngine.usedVBOs.add(vbo);

        int offset = 0;
        for (int i = 0; i < ParticleEngine.VERTEX_ATTRIB_SIZES.length; i++) {
            GL20.glEnableVertexAttribArray(i);
            GL20.glVertexAttribPointer(i, ParticleEngine.VERTEX_ATTRIB_SIZES[i], GL11.GL_FLOAT, false, ParticleEngine.BYTES_PER_PARTICLE, offset);
            GL33.glVertexAttribDivisor(i, 1);
            offset += ParticleEngine.VERTEX_ATTRIB_SIZES[i] * ParticleEngine.FLOAT_SIZE;
        }

        combinedBuffer = BufferUtils.createFloatBuffer(ParticleEngine.INITIAL_BUFFER_SIZE);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, combinedBuffer, GL15.GL_STREAM_DRAW);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    void registerParticleDeath(int count, float currentTime) {
        deadParticles += count;

        int totalParticles = combinedBuffer.position() / ParticleEngine.FLOATS_PER_PARTICLE;
        if (deadParticles >= REFACTOR_EMPTY_FRACTION * totalParticles) {
            reallocateBuffer(currentTime);
        }
    }

    private void reallocateBuffer(float currentTime) {
        combinedBuffer.flip();
        float[] temp = new float[combinedBuffer.limit()];

        int count = 0;
        for (int i = 0; i < temp.length; i += ParticleEngine.FLOATS_PER_PARTICLE) {
            // If the particle's time to die is less than the current time, it's no longer being
            // rendered. We can therefore prune it from the buffer.
            float deathTime = combinedBuffer.get(i + ParticleEngine.DEATH_TIME_LOC);
            combinedBuffer.position(i);
            if (deathTime > currentTime) {
                combinedBuffer.get(temp, count, ParticleEngine.FLOATS_PER_PARTICLE);
                count += ParticleEngine.FLOATS_PER_PARTICLE;
            }
        }
        int newSize = ParticleEngine.INITIAL_BUFFER_SIZE;
        while (newSize < count) {
            newSize <<= 1;
        }
        FloatBuffer newCombinedBuffer = BufferUtils.createFloatBuffer(newSize);
        newCombinedBuffer.put(temp, 0, count);
        combinedBuffer = newCombinedBuffer;

        // The size of the buffer put into the native call is limit() - position(). We want to allocate
        // the entire buffer, so temporarily set the position to 0.
        int oldPosition = combinedBuffer.position();
        combinedBuffer.position(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, combinedBuffer, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        combinedBuffer.position(oldPosition);

        deadParticles = 0;
    }

    void allocateParticles(Cluster cluster, float currentTime) {
        FloatBuffer buffer = cluster.generate(currentTime);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        // Current buffer is big enough to fit this cluster
        if (combinedBuffer.position() + buffer.limit() <= combinedBuffer.capacity()) {
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, (long) combinedBuffer.position()*ParticleEngine.FLOAT_SIZE, buffer);
            combinedBuffer.put(buffer);
        }
        // Current buffer is too small, we must create a new buffer
        else {
            int newSize = combinedBuffer.capacity();
            while (newSize < combinedBuffer.position() + buffer.limit()) {
                newSize <<= 1;
            }
            FloatBuffer newCombinedBuffer = BufferUtils.createFloatBuffer(newSize);
            combinedBuffer.flip();

            newCombinedBuffer.put(combinedBuffer);
            newCombinedBuffer.put(buffer);
            combinedBuffer = newCombinedBuffer;

            // The size of the buffer put into the native call is limit() - position(). We want to allocate
            // the entire buffer, so temporarily set the position to 0.
            int oldPosition = combinedBuffer.position();
            combinedBuffer.position(0);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, combinedBuffer, GL15.GL_STREAM_DRAW);
            combinedBuffer.position(oldPosition);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
}
