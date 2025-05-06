#version 140

in  vec4 a_position;
in  vec2 a_texCoord0;
out vec2 v_uv;

uniform mat4 u_projTrans;

void main() {
    v_uv       = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}
