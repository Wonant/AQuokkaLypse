// vortex.frag
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;
uniform sampler2D u_texture;
uniform vec2  u_center;     // in pixels
uniform vec2  u_resolution; // screen size in pixels
uniform float u_time;       // time in seconds
uniform float u_radius;     // vortex radius in pixels

void main() {
  // convert uv→pixel
  vec2 pos = v_uv * u_resolution;
  vec2 d   = pos - u_center;
  float len = length(d);

  if (len < u_radius) {
    float pct   = (u_radius - len) / u_radius;
    float angle = u_time * 5.0 * pct;
    float s = sin(angle), c = cos(angle);
    // rotate the offset vector
    d = mat2(c,-s, s,c) * d;
  }

  vec2 finalUv = (u_center + d) / u_resolution;
  gl_FragColor = texture2D(u_texture, finalUv);
}
d
