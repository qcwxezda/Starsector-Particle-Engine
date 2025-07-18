#version 430 core

in vec2 tex_coord;
in vec4 color;
out vec4 frag_color;

uniform sampler2D texSampler;
uniform bool useTexture;

void main() {
    frag_color = useTexture ? texture(texSampler, tex_coord) * color : vec4(color.xyz, color.w*(1.f-sqrt(2.f*distance(tex_coord, vec2(0.5f, 0.5f)))));
}