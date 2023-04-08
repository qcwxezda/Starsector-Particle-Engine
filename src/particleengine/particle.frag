#version 420

in vec2 tex_coord;
in vec4 color;
out vec4 frag_color;

layout (binding = 0) uniform sampler2D tex;

void main() {
    frag_color = texture(tex, tex_coord) * color;
}