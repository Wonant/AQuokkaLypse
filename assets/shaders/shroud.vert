#version 140

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;

uniform mat4 u_projTrans;

out vec4 v_color;
out vec2 v_texCoord;

void main() {
    v_color = a_color;
    v_texCoord = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}
