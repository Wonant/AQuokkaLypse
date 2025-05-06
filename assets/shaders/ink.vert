#version 140

in  vec2 a_position;
in  vec2 a_texCoord;
in  vec4 a_color;

uniform mat4 u_projTrans;      // set automatically by SpriteBatch

out vec2 v_uv;
out vec4 v_col;

void main() {
    v_uv  = a_texCoord;
    v_col = a_color;           // keeps per‑sprite tint if you use it
    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
}
