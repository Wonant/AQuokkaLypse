#version 140

in  vec2 v_texCoord;
out vec4 fragColor;

uniform vec3   iResolution;   // overlay width, height, unused
uniform float  iTime;         // seconds since start

// simple helpers
float thc(float a, float b)   { return tanh(a*cos(b)) / tanh(a); }
float h21(vec2 a)             { return fract(sin(dot(a,vec2(12.9898,78.233)))*43758.5453); }
float mlength(vec2 uv)        { return max(abs(uv.x), abs(uv.y)); }

void main(){
    // 1) centered UV
    vec2  fragCoord = v_texCoord * iResolution.xy;
    vec2  uvn       = (fragCoord - 0.5*iResolution.xy) / iResolution.y;
    float distNorm = length(uvn) / 0.5;   // 0=center,1=edge

    // 2) radial growth (0→1 over 0.5s)
    float grow = clamp((iTime - 0.0)/0.5, 0.0, 1.0);
    if (distNorm > grow) discard;

    // 3) swirl → grid
    vec2 uv = uvn;
    uv.y += 0.08 * thc(2.0, 10.0*mlength(uv) + iTime);
    float r = length(uv), a = atan(uv.y, uv.x);
    uv = vec2(r, a * 10.0);

    // 4) “cellular” pattern strength s
    float sc  = 0.5;
    vec2 ipos = floor(sc*uv) + 0.5;
    vec2 fpos = fract(sc*uv) - 0.5;
    float d   = 0.1*(0.5 + 0.5*cos(iTime)) + length(fpos);
    float s   = smoothstep(-0.1,0.1,0.5 - d)
              - smoothstep(-0.1,0.1,0.42 - d);
    s *= (0.5 + 0.5*cos(uv.x + uv.y - iTime))
       * h21(ipos + floor(length(ipos)*100.0 + 0.2*iTime));

    // 5) **taper** by radius: 1.0 at center → 0.0 at edge
    float taper = 1.0 - distNorm;
    s *= taper;

    if (s < 0.01) discard;

    // 6) pure red tendril + glow
    vec3 base = vec3(1.0,0.0,0.0) * 1.3;
    float glow = smoothstep(0.0, 0.3, (0.3 - distNorm)) * s;
    base += vec3(1.0,0.2,0.2) * glow * 2.5;

    fragColor = vec4(base * s, s);
}
