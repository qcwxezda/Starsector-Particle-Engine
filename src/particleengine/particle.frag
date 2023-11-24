#version 330

in vec2 tex_coord;
in vec4 color;
out vec4 frag_color;

uniform sampler2D texSampler;
uniform bool useTexture;

void main() {
    frag_color = useTexture ? (texture2D(texSampler, tex_coord) * color) : vec4(color.xyz, color.w*(1.f-2.f*distance(vec2(0.5, 0.5), tex_coord)));
}