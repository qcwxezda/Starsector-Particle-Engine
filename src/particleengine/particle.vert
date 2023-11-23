#version 330

layout (location = 0) in float tracked_emitter_index;
// first 2 are particle's location at t=0, last 2 are emitter's location
layout (location = 1) in vec4 pos_emitter_pos;
layout (location = 2) in float emitter_forward_dir;
// first 2 are velocity vector, last 2 are acceleration vector
layout (location = 3) in vec4 vel_acc;
// amplitude, frequency, and phase of sinusoidal motion along global x axis
layout (location = 4) in vec3 sinusoid_x;
// amplitude, frequency, and phase of sinusoidal motion along global y axis
layout (location = 5) in vec3 sinusoid_y;
// elements are direction, angular velocity, and angular acceleration
layout (location = 6) in vec3 angle_data;
// circle the initial position with the given angular velocity and acceleration
layout (location = 7) in vec2 radial_data;
// elements are scale, growth rate, and growth acceleration
layout (location = 8) in vec3 size_data_x;
layout (location = 9) in vec3 size_data_y;
// starting color and color shift are all in hsva
// hue is in degrees
layout (location = 10) in vec4 color_start;
layout (location = 11) in vec4 color_shift;
// fade in, fade out, starting time, ending time
layout (location = 12) in vec4 fade_time_data;

uniform mat4 projection;
uniform float time;
uniform vec2 textureScale;

layout (std140) uniform TrackedEmitters {
  vec4 locations[1024];
};

const vec2 vert_locs[4] = vec2[] (
  vec2(0., 0.),
  vec2(1., 0.),
  vec2(0., 1.),
  vec2(1., 1.)
);

out vec2 tex_coord;
out vec4 color;

mat2 rot_mat(float angle) {
  return mat2(cos(angle), sin(angle), -sin(angle), cos(angle));
}

// source: http://lolengine.net/blog/2013/07/27/rgb-to-hsv-in-glsl
vec4 to_rgba(vec4 hsva) {
  vec4 hsva_c = vec4(hsva.x, clamp(hsva.yzw, 0.f, 1.f));
  vec4 K = vec4(1.f, 2.f / 3.f, 1.f / 3.f, 3.f);
  vec3 p = abs(fract(hsva_c.xxx / 360.f + K.xyz) * 6.0 - K.www);
  return vec4(hsva_c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), hsva_c.y), hsva_c.w);
}

vec2 get_location(int index) {
  int d = int(index / 2);
  return locations[d].xy * ((index + 1) % 2) + locations[d].zw * (index % 2);
}

void main() {
  float lifetime = fade_time_data.w - fade_time_data.z;
  float elapsed = time - fade_time_data.z;
  vec2 emitter_pos = pos_emitter_pos.zw;
  vec2 emitter_offset = tracked_emitter_index < 0 ? vec2(0, 0) : get_location(int(tracked_emitter_index)) - emitter_pos;

  float revolution_angle = elapsed*radial_data.x + 0.5f* elapsed*elapsed*radial_data.y;

  vec2 particle_pos = pos_emitter_pos.xy + elapsed*vel_acc.xy + 0.5f*elapsed*elapsed*vel_acc.zw;
  particle_pos += vec2(sinusoid_x.x * sin(sinusoid_x.y * elapsed + sinusoid_x.z), sinusoid_y.x * sin(sinusoid_y.y * elapsed + sinusoid_y.z));
  // so that new_pos = pos at t = 0
  particle_pos -= vec2(sinusoid_x.x * sin(sinusoid_x.z), sinusoid_y.x * sin(sinusoid_y.z));
  particle_pos = rot_mat(revolution_angle + emitter_forward_dir) * (particle_pos - emitter_pos) + emitter_pos;
  particle_pos += emitter_offset;

  float facing_angle = angle_data.x + elapsed*angle_data.y + 0.5f*elapsed*elapsed*angle_data.z;
  vec2 vert_loc = vert_locs[gl_VertexID];
  vec2 size = vec2(size_data_x.x + elapsed*size_data_x.y + 0.5f*elapsed*elapsed*size_data_x.z, size_data_y.x + elapsed*size_data_y.y + 0.5*elapsed*elapsed*size_data_y.z);
  vec2 vert_pos = rot_mat(facing_angle + emitter_forward_dir) * (size*vert_loc - size/2.f);

  gl_Position = projection * vec4(vert_pos.x + particle_pos.x, vert_pos.y + particle_pos.y, 1.f, 1.f);

  tex_coord = vert_loc * textureScale;

  float alpha = min(1.f / fade_time_data.x * elapsed, min(1.f, lifetime / fade_time_data.y - elapsed / fade_time_data.y));
  vec4 dead_color = vec4(0.f, 0.f, 0.f, 0.f);
  vec4 alive_color = to_rgba(color_start + elapsed * color_shift);
  color = mix(alive_color, dead_color, float(elapsed > lifetime || size.x <= 0 || size.y <= 0)) * vec4(1.f, 1.f, 1.f, alpha);
}