#version 140

in  vec2 v_texCoord;
out vec4 fragColor;

uniform vec2   iResolution;
uniform float  iTime;
uniform sampler2D iChannel0;   // your ripple noise / blank
uniform sampler2D iMask;       // the scareEffectTexture

void main() {
    // 1) mask‐check: if the mask is transparent here, kill the pixel
    float m = texture(iMask, v_texCoord).a;
    if (m < 0.01) discard;

    // 2) compute your ripple UV just like before
    vec2 uv = (v_texCoord * iResolution - 0.5 * iResolution)
            / iResolution.y;
    float wave = sin(10.0 * length(uv) - iTime * 5.0);
    float c    = 0.5 + 0.5 * wave;

    // 3) output ripple * mask alpha
    fragColor = vec4(vec3(c), c) * m;
}
