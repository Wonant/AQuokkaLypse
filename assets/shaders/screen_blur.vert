#version 140
in  vec2 a_position;
in  vec2 a_texCoord;
out vec2 v_uv;
uniform mat4 u_projTrans;

void main() {
    v_uv       = a_texCoord;
    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
}
