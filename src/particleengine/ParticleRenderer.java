package particleengine;

import com.fs.starfarer.api.combat.ViewportAPI;
import org.lwjgl.opengl.*;


record ParticleRenderer(Object layer, ParticleAllocator allocator, Particles owner) {
    public void render(ViewportAPI viewport) {
        ParticleType type = allocator.type;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(type.sfactor(), type.dfactor());
        GL14.glBlendEquation(type.blendMode());

        float spriteCenterX = 0.5f, spriteCenterY = 0.5f;
        boolean hasTexture = type.sprite() != null && type.sprite().getTextureId() > 0;
        if (hasTexture) {
            int target = 1;
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + target);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, type.sprite().getTextureId());
            GL20.glUniform1i(ParticleShader.texSamplerLoc, target);
            GL20.glUniform2f(ParticleShader.textureSizeLoc, type.sprite().getTexWidth(), type.sprite().getTexHeight());
            if (type.sprite().getCenterX() >= 0f) {
                spriteCenterX = type.sprite().getCenterX() / type.sprite().getWidth();
            }
            if (type.sprite().getCenterY() >= 0f) {
                spriteCenterY = type.sprite().getCenterY() / type.sprite().getHeight();
            }
        } else {
            GL20.glUniform2f(ParticleShader.textureSizeLoc, 1f, 1f);
        }
        GL20.glUniform2f(ParticleShader.spriteCenterLoc, spriteCenterX, spriteCenterY);
        GL20.glUniform1f(ParticleShader.viewportAlphaLoc, viewport.getAlphaMult());
        GL30.glBindVertexArray(allocator.vao);
        GL20.glUniform1i(ParticleShader.useTextureLoc, !hasTexture ? 0 : 1);
        GL31.glDrawArraysInstanced(
                GL11.GL_TRIANGLE_STRIP,
                0,
                4,
                allocator.bufferPosition / Particles.FLOATS_PER_PARTICLE);
        GL30.glBindVertexArray(0);
        if (hasTexture) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }
    }
}
