#version 140
#define TAU 6.28318530718

uniform sampler2D u_texture;       // tile texture bound by SpriteBatch
uniform vec2      iResolution;     // screen size (pixels)
uniform vec2      u_center;        // ink origin (pixels)
uniform float     iTime;           // seconds since start

/* tweakables */
const float SPEED      = 0.46;          // spread speed
const float GRAY       = 0.7;           // 1 = full mono, 0 = full colour
const float DISCARD_BR = 0.97;          // brightness above -> discard
/* ---------- */

in  vec2 v_uv;
in  vec4 v_col;                        // per‑sprite tint
out vec4 fragColor;

/* helpers */
float rand(vec2 n){ return fract(sin(dot(n,vec2(12.9898,4.1414)))*43758.5453);}
float noise(vec2 p){
    vec2 ip=floor(p), u=fract(p); u=u*u*(3.0-2.0*u);
    return mix(mix(rand(ip),rand(ip+vec2(1,0)),u.x),
               mix(rand(ip+vec2(0,1)),rand(ip+vec2(1,1)),u.x),u.y);
}
float fbm(vec2 p,int o){ float a=1.,n=0.,nm=0.; for(int i=0;i<o;i++){n+=noise(p)*a;nm+=a;p*=2.;a*=.5;}return n/nm;}

void main() {
    /* ── 1.  sample tile & desaturate ─────────────────────────────── */
    vec3 texCol = texture(u_texture, v_uv).rgb;
    float g     = dot(texCol, vec3(0.299,0.587,0.114));
    texCol      = mix(texCol, vec3(g), GRAY);

    /* ── 2.  radial ink mask in screen space ───────────────────────── */
    vec2 d  = (gl_FragCoord.xy - u_center) / iResolution.y; // square coords
    float t = iTime * SPEED;  t *= t;                       // accelerating

    float angle = atan(d.y,d.x) + fbm(d*4.0,2)*0.5;
    vec2  p     = vec2(cos(angle), sin(angle));

    float l     = dot(d/t, d/t) - (fbm(normalize(d)*3.0,2)-0.5);
    float ink   = fbm(p*8.0,2) + 1.5 - l;
    float mask  = clamp(ink, 0.0, 1.0);

    /* ── 3.  final colour -------------------------------------------------- */
    vec3 finalCol = mix(vec3(1.0), texCol, mask) * v_col.rgb;

    /* brightness test (luminance) */
    float brightness = dot(finalCol, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > DISCARD_BR || brightness < 0.05) discard;

    // invert maybe
    vec3 invCol = vec3(1.0) - finalCol;

    fragColor = vec4(invCol, 1.0);
}
