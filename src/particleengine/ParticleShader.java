package particleengine;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

abstract class ParticleShader extends Shader {
    public static int programId = -1;
    public static int projectionLoc;
    public static int timeLoc;
    public static int useTextureLoc;
    public static int texSamplerLoc;
    public static int textureSizeLoc;
    public static int spriteCenterLoc;
    public static int emitterUniformBlockLoc;
    public static final int emitterUniformBlockBinding = 1;
    public static String
            projectionName = "projection",
            timeName = "time",
            useTextureName = "useTexture",
            textureSizeName = "textureScale",
            texSamplerName = "texSampler",
            spriteCenterName = "spriteCenter",
            emitterUniformBlockName = "TrackedEmitters";

    public static void init(String vertShaderPath, String fragShaderPath) {
        programId = Shader.createProgram(vertShaderPath, fragShaderPath, programId);
        projectionLoc = GL20.glGetUniformLocation(programId, projectionName);
        timeLoc = GL20.glGetUniformLocation(programId, timeName);
        useTextureLoc = GL20.glGetUniformLocation(programId, useTextureName);
        texSamplerLoc = GL20.glGetUniformLocation(programId, texSamplerName);
        textureSizeLoc = GL20.glGetUniformLocation(programId, textureSizeName);
        spriteCenterLoc = GL20.glGetUniformLocation(programId, spriteCenterName);
        emitterUniformBlockLoc = GL31.glGetUniformBlockIndex(programId, emitterUniformBlockName);
        GL31.glUniformBlockBinding(programId, emitterUniformBlockLoc, emitterUniformBlockBinding);
    }
}
