#version 420

// first 2 are particle's location at t=0, last 2 are emitter's location
layout (location = 0) in vec4 pos_emitter_pos;
layout (location = 1) in float emitter_forward_dir;
// first 2 are velocity vector, last 2 are acceleration vector
layout (location = 2) in vec4 vel_acc;
// amplitude, frequency, and phase of sinusoidal motion along global x axis
layout (location = 3) in vec3 sinusoid_x;
// amplitude, frequency, and phase of sinusoidal motion along global y axis
layout (location = 4) in vec3 sinusoid_y;
// elements are direction, angular velocity, and angular acceleration
layout (location = 5) in vec3 angle_data;
// circle the initial position with the given angular velocity and acceleration
layout (location = 6) in vec2 radial_data;
// elements are scale, growth rate, and growth acceleration
layout (location = 7) in vec3 size_data;
layout (location = 8) in vec4 color_start;
layout (location = 9) in vec4 color_end;
// fade in, fade out, starting time, ending time
layout (location = 10) in vec4 fade_time_data;

uniform mat4 projection;
uniform float time;

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

void main() {
  float lifetime = fade_time_data.w - fade_time_data.z;
  float elapsed = time - fade_time_data.z;
  vec2 emitter_pos = pos_emitter_pos.zw;

  float revolution_angle = elapsed*radial_data.x + 0.5f* elapsed*elapsed*radial_data.y;

  vec2 particle_pos = pos_emitter_pos.xy + elapsed*vel_acc.xy + 0.5f*elapsed*elapsed*vel_acc.zw;
  particle_pos += vec2(sinusoid_x.x * sin(sinusoid_x.y * elapsed + sinusoid_x.z), sinusoid_y.x * sin(sinusoid_y.y * elapsed + sinusoid_y.z));
  // so that new_pos = pos at t = 0
  particle_pos -= vec2(sinusoid_x.x * sin(sinusoid_x.z), sinusoid_y.x * sin(sinusoid_y.z));
  particle_pos = rot_mat(revolution_angle + emitter_forward_dir) * (particle_pos - emitter_pos) + emitter_pos;

  float facing_angle = angle_data.x + elapsed*angle_data.y + 0.5f*elapsed*elapsed*angle_data.z;
  vec2 vert_loc = vert_locs[gl_VertexID];
  float size = size_data.x + elapsed*size_data.y + 0.5f* elapsed*elapsed*size_data.z;
  vec2 vert_pos = rot_mat(facing_angle + emitter_forward_dir) * vec2(size * vert_loc.x - size /2.f, size * vert_loc.y - size /2.f);

  gl_Position = projection * vec4(vert_pos.x + particle_pos.x, vert_pos.y + particle_pos.y, 1.f, 1.f);

  tex_coord = vert_loc;

  float alpha = min(1.f / fade_time_data.x * elapsed, min(1.f, lifetime / fade_time_data.y - elapsed / fade_time_data.y));
  vec4 dead_color = vec4(0.f, 0.f, 0.f, 0.f);
  vec4 alive_color = clamp(vec4(mix(color_start, color_end, elapsed / lifetime)), 0.f, 1.f) * vec4(1.f, 1.f, 1.f, alpha);
  color = mix(alive_color, dead_color, float(elapsed > lifetime || size <= 0));
  //color = (elapsed > lifetime || size <= 0) ? vec4(0.f, 0.f, 0.f, 0.f) : clamp(vec4(mix(color_start, color_end, elapsed / lifetime)), 0.f, 1.f) * vec4(1.f, 1.f, 1.f, alpha);
}