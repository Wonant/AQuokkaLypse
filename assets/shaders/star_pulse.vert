#version 140

in vec4 a_position;    // x,y,0,1
in vec2 a_texCoord0;   // uv
in vec4 a_color;       // tint

uniform mat4 u_projTrans;  // camera projection*view

out vec2 v_uv;
out vec4 v_color;

void main() {
    v_uv       = a_texCoord0;
    v_color    = a_color;
    gl_Position = u_projTrans * a_position;
}
