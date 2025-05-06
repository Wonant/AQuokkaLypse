#version 140
in  vec2 v_uv;
out vec4 fragColor;

// Your scene already rendered into this texture
uniform sampler2D u_scene;
// Screen size in pixels
uniform vec2      u_resolution;
// How much to dim the scene (0=black,1=normal)
uniform float     u_dim;
// Light colour (e.g. warm white)
uniform vec3      u_lightColor;
// Light centre in UV space (0–1)
uniform vec2      u_lightPos;
// Radius of falloff (in UV distance)
uniform float     u_lightRadius;
// Strength of the bloom/glow
uniform float     u_bloomStrength;
// How big the blur kernel is (in px)
uniform float     u_bloomRadius;

void main() {
    // 1) basic scene sample + dim
    vec3 sceneCol = texture(u_scene,v_uv).rgb;
    vec3 base     = sceneCol * u_dim;

    float d    = distance(v_uv, u_lightPos);
    float glow = smoothstep(u_lightRadius, 0.0, d);


    vec3 bloom = vec3(0.0);
    float cnt  = 0.0;
    // we’ll do a simple 3×3 sample
    for(int xo=-1; xo<=1; ++xo){
      for(int yo=-1; yo<=1; ++yo){
        vec2 off = vec2(xo,yo) * (u_bloomRadius / u_resolution);
        float g2 = texture(u_scene, v_uv + off).r * glow;
        bloom += g2 * u_lightColor;  // tint while we blur
        cnt   += 1.0;
      }
    }
    bloom = bloom / cnt * u_bloomStrength;

    // 4) composite: dimmed base + direct glow + blurred bloom
    vec3  direct   = u_lightColor * glow;
    vec3  colorOut = base + direct + bloom;

    fragColor = vec4(colorOut,1.0);
}
