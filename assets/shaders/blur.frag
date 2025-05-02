#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;
uniform sampler2D u_texture;
uniform float resolution;
uniform float radius;

void main() {
    float blurSize = radius / resolution;
    vec4 sum = vec4(0.0);

    sum += texture2D(u_texture, v_texCoord + vec2(-4.0*blurSize, 0.0)) * 0.05;
    sum += texture2D(u_texture, v_texCoord + vec2(-3.0*blurSize, 0.0)) * 0.09;
    sum += texture2D(u_texture, v_texCoord + vec2(-2.0*blurSize, 0.0)) * 0.12;
    sum += texture2D(u_texture, v_texCoord + vec2(-1.0*blurSize, 0.0)) * 0.15;
    sum += texture2D(u_texture, v_texCoord) * 0.18;
    sum += texture2D(u_texture, v_texCoord + vec2(1.0*blurSize, 0.0)) * 0.15;
    sum += texture2D(u_texture, v_texCoord + vec2(2.0*blurSize, 0.0)) * 0.12;
    sum += texture2D(u_texture, v_texCoord + vec2(3.0*blurSize, 0.0)) * 0.09;
    sum += texture2D(u_texture, v_texCoord + vec2(4.0*blurSize, 0.0)) * 0.05;

    gl_FragColor = sum;
}








