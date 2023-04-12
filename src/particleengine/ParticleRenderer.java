package particleengine;


import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.lwjgl.opengl.*;

import java.util.EnumSet;

class ParticleRenderer implements CombatLayeredRenderingPlugin {

    final CombatEngineLayers layer;
    final EnumSet<CombatEngineLayers> activeLayers;
    final ParticleAllocator allocator;
    final Particles owner;
    boolean expired = false;

    ParticleRenderer(CombatEngineLayers layer, ParticleAllocator allocator, Particles owner) {
        this.layer = layer;
        this.allocator = allocator;
        this.owner = owner;
        activeLayers = EnumSet.of(layer);
    }

    @Override
    public void init(CombatEntityAPI entity) {}

    @Override
    public void cleanup() {}

    @Override
    public boolean isExpired() {
        return expired;
    }

    void setExpired() {
        expired = true;
    }

    @Override
    public void advance(float v) {}

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return activeLayers;
    }

    @Override
    public float getRenderRadius() {
        return 1000000f;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        GL20.glUseProgram(ParticleShader.programId);
        GL11.glEnable(GL11.GL_BLEND);
        ParticleType type = allocator.type;
        GL11.glBlendFunc(type.sfactor, type.dfactor);
        GL14.glBlendEquation(type.blendMode);
        if (type.sprite != null) {
            int target = 1;
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + target);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, type.sprite.getTextureId());
            GL20.glUniform1i(ParticleShader.texSamplerLoc, target);
        }
        GL30.glBindVertexArray(allocator.vao);
        GL20.glUniformMatrix4(ParticleShader.projectionLoc, true, Utils.getProjectionMatrix(viewport));
        GL20.glUniform1f(ParticleShader.timeLoc, owner.currentTime);
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
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(0);
    }
}
