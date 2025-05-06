#version 140

uniform float u_time;       // elapsed time
uniform float u_flashDur;   // duration of the initial star burst
uniform float u_glowInt;    // base glow strength
uniform int   u_spikes;     // number of star points
uniform vec2  u_dir;        // normalized bullet direction

in  vec2 v_uv;              // [0,1] across the quad
out vec4 fragColor;

// 0→1 over [0,1], 1→0 over [1,∞)
float falloff(float x) {
    return smoothstep(1.0, 0.0, x);
}

void main() {
    vec2  pos   = (v_uv - 0.5) * 2.0;
    float dist  = length(pos);
    float ang   = atan(pos.y, pos.x);

    float tStar = clamp(u_time / u_flashDur,      0.0, 1.0);
    float tGlow = clamp((u_time - u_flashDur)/u_flashDur, 0.0, 1.0);

    float ramp     = sin(tStar * 3.14159);
    float spikes   = pow(abs(cos(float(u_spikes)*ang)), 12.0);
    float star     = ramp * spikes * falloff(dist);

    float glowRad  = 0.6;
    float glowBase = falloff(dist/glowRad) * u_glowInt;
    glowBase       = pow(glowBase, 1.3) * tGlow;

    vec2  dir      = normalize(u_dir);
    float along    = dot(pos, -dir);             // positive behind the bullet
    float side     = dot(pos, vec2(-dir.y, dir.x));
    float flameLen = 0.5;                        // how long the flames reach
    float flameWd  = 0.3;                        // how wide they are

    float flameMain = smoothstep(0.0, flameLen, along)
                     * smoothstep(flameWd, 0.0, abs(side) * (1.0 + along/flameLen));

    float edgeFade = falloff(dist);
    float flames   = flameMain * edgeFade * (1.0 - tStar);

    float effect   = mix(star, glowBase, tGlow) + flames;
    effect        *= 2.0;                         // make it pop

    vec3 purple    = vec3(0.85, 0.1, 1.0);
    fragColor      = vec4(purple * effect, effect);

    if (fragColor.a < 0.01) discard;
}
