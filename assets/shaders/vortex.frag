// vortex.frag
#version 140

in  vec2  v_texCoord;
out vec4  fragColor;

uniform vec3   iResolution;   // px width,height,unused
uniform float  iTime;         // seconds
uniform sampler2D iChannel0;  // noise or detail map
uniform sampler2D iChannel1;  // optional second noise

// tweak these to taste
const float BASE_RADIUS  = 0.5;   // circle radius in UV‐space
const float TENDRIL_PERT = 0.15;  // how much the radius wiggles
const float FADE_WIDTH   = 0.1;   // how wide the fade band is

const vec4 waterColor = vec4(0.62,0.1,0.1,1.0);
const vec3 lightDir   = normalize(vec3(10.0,15.0,5.0));

// a cheap hash for tendrils
float hash21(vec2 p) {
    return fract(sin(dot(p,vec2(127.1,311.7))) * 43758.5453);
}

// Your height & normal (unchanged)
float height(in vec2 uv) {
    return texture(iChannel0,uv).b
         * texture(iChannel1,uv + vec2(0.0,iTime*0.1)).b;
}
const vec2 NE = vec2(0.05,0.0);
vec3 normal(in vec2 uv) {
    return normalize(vec3(
        height(uv+NE.xy)-height(uv-NE.xy),
        0.0,
        height(uv+NE.yx)-height(uv-NE.yx)
    ));
}

void main() {
    // 1) center‐and‐aspect‐correct UV in [–.5,+.5]
    vec2 fragCoord = v_texCoord * iResolution.xy;
    vec2 uvn = fragCoord / iResolution.xy - 0.5;
    uvn.y *= iResolution.y / iResolution.x;

    float dist = length(uvn);

    // 2) build a noisy circle radius for tendrils
    float ang   = atan(uvn.y, uvn.x);
    float tend  = hash21(vec2(ang*2.0, dist*4.0 - iTime*0.5));
    float radius = BASE_RADIUS + (tend - 0.5)*TENDRIL_PERT;

    // 3) **hard discard** everything outside our wiggle‐circle
    if (dist > radius + FADE_WIDTH) {
        discard;
    }

    // 4) full‐2D swirl: rotate by (angle + dist•k + time)
    float phi = ang + dist*3.0 + iTime*0.3;
    float r   = dist + iTime*0.2;
    vec2 suv  = vec2(cos(phi), sin(phi)) * r;

    // 5) shade & mix
    vec3 n    = normal(suv);
    vec4 lit  = waterColor
               + waterColor * max(dot(lightDir,n),0.0) * 0.15;
    vec4 txt  = texture(iChannel0, suv * 0.8);
    vec4 col  = mix(lit, txt, 0.3);

    // 6) add a glow in the core
    float glow = smoothstep(0.0, BASE_RADIUS*0.6, BASE_RADIUS - dist);
    col.rgb   += vec3(0.2,0.3,0.6) * glow;

    // 7) smooth fade‐out
    float maxDist = 0.5;

    // 2) radial fade from 1.0 at center → 0.0 at maxDist
    float radial = 1.0 - smoothstep(0.0, maxDist, dist);

    // 3) time fade from 0 at iTime=0 → 1 at iTime=0.5
    float timeFade = smoothstep(0.0, 0.5, iTime);

    // 4) combined alpha
    float a = radial * timeFade;
    col.a = a;

    // 5) discard fully‐transparent fragments
    if (col.a < 0.01) discard;

    fragColor = col;
}
