package particleengine;


import com.fs.starfarer.api.combat.CombatEngineLayers;
import org.lwjgl.opengl.*;

import java.util.EnumSet;

class ParticleRenderer {
    final CombatEngineLayers layer;
    final EnumSet<CombatEngineLayers> activeLayers;
    final ParticleAllocator allocator;
    final Particles owner;

    ParticleRenderer(CombatEngineLayers layer, ParticleAllocator allocator, Particles owner) {
        this.layer = layer;
        this.allocator = allocator;
        this.owner = owner;
        activeLayers = EnumSet.of(layer);
    }

    public void render() {
        ParticleType type = allocator.type;
        GL11.glBlendFunc(type.sfactor, type.dfactor);
        GL14.glBlendEquation(type.blendMode);
        float spriteCenterX = 0.5f, spriteCenterY = 0.5f;
        if (type.sprite != null) {
            int target = 1;
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + target);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, type.sprite.getTextureId());
            GL20.glUniform1i(ParticleShader.texSamplerLoc, target);
            GL20.glUniform2f(ParticleShader.textureSizeLoc, type.sprite.getTextureWidth(), type.sprite.getTextureHeight());
            if (type.sprite.getCenterX() >= 0f) {
                spriteCenterX = type.sprite.getCenterX() / type.sprite.getWidth();
            }
            if (type.sprite.getCenterY() >= 0f) {
                spriteCenterY = type.sprite.getCenterY() / type.sprite.getHeight();
            }
        }
        else {
            GL20.glUniform2f(ParticleShader.textureSizeLoc, 1f, 1f);
        }
        GL20.glUniform2f(ParticleShader.spriteCenterLoc, spriteCenterX, spriteCenterY);
        GL30.glBindVertexArray(allocator.vao);
        GL20.glUniform1i(ParticleShader.useTextureLoc, type.sprite == null ? 0 : 1);
        GL31.glDrawArraysInstanced(
                GL11.GL_TRIANGLE_STRIP,
                0,
                4,
                allocator.bufferPosition / Particles.FLOATS_PER_PARTICLE);
        GL30.glBindVertexArray(0);
        if (type.sprite != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        }
    }
}
