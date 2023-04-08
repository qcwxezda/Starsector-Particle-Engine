package particleengine;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.IOException;

abstract class Shader {

    protected static int createProgram(String vertShaderPath, String fragShaderPath, int existingProgramId) {
        try {

            if (existingProgramId > -1) {
                delete(existingProgramId);
            }

            int programId = GL20.glCreateProgram();
            int vertShaderId = attachShader(GL20.GL_VERTEX_SHADER, programId, vertShaderPath);
            int fragShaderId = attachShader(GL20.GL_FRAGMENT_SHADER, programId, fragShaderPath);
            GL20.glLinkProgram(programId);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException("(Particle Engine) Failure to link shader program");
            }
            GL20.glDetachShader(programId, vertShaderId);
            GL20.glDetachShader(programId, fragShaderId);
            GL20.glDeleteShader(vertShaderId);
            GL20.glDeleteShader(fragShaderId);
            return programId;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void delete(int id) {
        GL20.glDeleteProgram(id);
    }

    protected static int attachShader(int target, int program, String filePath) throws IOException {
        int id = GL20.glCreateShader(target);
        GL20.glShaderSource(id, Utils.readFile(filePath));
        GL20.glCompileShader(id);

        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("(Particle Engine) Failure to compile shader");
        }

        GL20.glAttachShader(program, id);
        return id;
    }
}
