package particleengine;

import org.lwjgl.opengl.GL20;

abstract class ParticleShader extends Shader {
    public static int programId = -1;
    public static int projectionLoc, timeLoc, resolutionLoc, useTextureLoc, texSamplerLoc;
    public static String projectionName = "projection", timeName = "time", useTextureName = "useTexture", texSamplerName = "texSampler";

    public static void init(String vertShaderPath, String fragShaderPath) {
        programId = Shader.createProgram(vertShaderPath, fragShaderPath, programId);
        projectionLoc = GL20.glGetUniformLocation(programId, projectionName);
        timeLoc = GL20.glGetUniformLocation(programId, timeName);
        useTextureLoc = GL20.glGetUniformLocation(programId, useTextureName);
        texSamplerLoc = GL20.glGetUniformLocation(programId, texSamplerName);
    }
}
