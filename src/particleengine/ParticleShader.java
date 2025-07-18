package particleengine;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

abstract class ParticleShader extends Shader {
    public static int programId = -1;
    public static int projectionLoc;
    public static int timeLoc;
    public static int useTextureLoc;
    public static int texSamplerLoc;
    public static int textureSizeLoc;
    public static int spriteCenterLoc;
    public static int viewportAlphaLoc;
    public static int trackedEmitterBlockLoc;
    public static final int trackedEmitterBinding = 1;
    public static String
            projectionName = "projection",
            timeName = "time",
            useTextureName = "useTexture",
            textureSizeName = "textureScale",
            texSamplerName = "texSampler",
            spriteCenterName = "spriteCenter",
            viewportAlphaName = "viewportAlpha",
            trackedEmittersName = "TrackedEmitters";

    public static void init(String vertShaderPath, String fragShaderPath) {
        programId = Shader.createProgram(vertShaderPath, fragShaderPath, programId);
        projectionLoc = GL20.glGetUniformLocation(programId, projectionName);
        timeLoc = GL20.glGetUniformLocation(programId, timeName);
        useTextureLoc = GL20.glGetUniformLocation(programId, useTextureName);
        texSamplerLoc = GL20.glGetUniformLocation(programId, texSamplerName);
        textureSizeLoc = GL20.glGetUniformLocation(programId, textureSizeName);
        spriteCenterLoc = GL20.glGetUniformLocation(programId, spriteCenterName);
        viewportAlphaLoc = GL20.glGetUniformLocation(programId, viewportAlphaName);
        trackedEmitterBlockLoc = GL43.glGetProgramResourceIndex(programId, GL43.GL_SHADER_STORAGE_BLOCK, trackedEmittersName);
        GL43.glShaderStorageBlockBinding(programId, trackedEmitterBlockLoc, trackedEmitterBinding);
    }
}
