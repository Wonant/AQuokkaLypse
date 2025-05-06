#version 140

in  vec2 v_uv;          // [0,1] across the quad
out vec4 fragColor;

uniform float u_time;       // seconds since spawn
uniform vec2  u_resolution; // (quadWidth, quadHeight) in screen units
uniform vec2  u_dir;        // normalized bullet direction (x,y)

// 2D value noise + FBM
float hash(vec2 p) {
    return fract(sin(dot(p,vec2(127.1,311.7))) * 43758.5453);
}
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    float a = hash(i+vec2(0,0)), b = hash(i+vec2(1,0));
    float c = hash(i+vec2(0,1)), d = hash(i+vec2(1,1));
    vec2 u = f*f*(3.0-2.0*f);
    return mix(a,b,u.x) + (c-a)*u.y*(1.0-u.x) + (d-b)*u.x*u.y;
}
float fbm(vec2 p) {
    float v=0.;
    v += 0.50*noise(p);    p = p*2.03 + 1.7;
    v += 0.25*noise(p);    p = p*2.01 - 3.3;
    v += 0.125*noise(p);   p = p*2.02 + 2.5;
    v += 0.0625*noise(p);
    return v;
}

void main() {
    // 1) Base UV → centered & aspect
    vec2 pos = v_uv - 0.5;
    float aspect = u_resolution.x/u_resolution.y;
    pos.x *= aspect;

    // 2) drift smoke opposite bullet direction and upward over time
    vec2 drift = -u_dir * (u_time*0.2) + vec2(0, u_time*0.1);
    vec2 sampleUV = pos + drift;

    // 3) noise filaments
    float n = fbm(sampleUV * 2.5);
    float tendril = smoothstep(0.45,0.50,n) - smoothstep(0.50,0.55,n);

    // 4) radial fade
    float r = length(pos)*2.0;
    float fade = exp(-r*r*2.0);

    // 5) time fade (smoke dissipates)
    float life = clamp(1.0 - u_time*0.5, 0.0, 1.0);

    // 6) final alpha & color
    float alpha = tendril * fade * life;
    vec3  color = vec3(0.6,0.2,0.9) * tendril;  // bright purple

    if (alpha < 0.01) discard;
    fragColor = vec4(color, alpha);
}
