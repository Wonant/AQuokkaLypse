#version 140
uniform sampler2D u_scene;
uniform vec2      u_texel;
uniform float     u_strength;

in  vec2 v_uv;
out vec4 fragColor;

vec3 blur9(sampler2D tex, vec2 uv, vec2 texel){
    vec3 sum = vec3(0.0);
    for(int y=-1;y<=1;y++)
        for(int x=-1;x<=1;x++)
            sum += texture(tex, uv + vec2(x,y)*texel).rgb;
    return sum / 9.0;
}

void main() {
    vec3 base  = blur9(u_scene, v_uv, u_texel);
    base       = mix(texture(u_scene, v_uv).rgb, base, 0.7);

    /* FADE IN EFFECT */
    vec3 final = mix(base, base*0.2, u_strength);

    fragColor = vec4(final, 1.0);
}
