#version 140

in  vec2 v_uv;
out vec4 fragColor;

// The rendered UI-camera texture
uniform sampler2D u_scene;
// Screen resolution (px)
uniform vec2      u_resolution;
// Overall dim factor (<1 → darker)
uniform float     u_dim;
// RGB tint to multiply (e.g. warm sunset)
uniform vec3      u_tint;
// Player’s world X position
uniform float     u_playerX;
// How fast the rays drift per unit X
uniform float     u_raySpeed;
// How many rays across the screen
uniform float     u_rayCount;
// Width of each ray (in [0,1] of stripe pattern)
uniform float     u_rayWidth;
// How strong the rays blend over the scene
uniform float     u_rayStrength;

void main() {
    // 1) sample the base UI scene
    vec3 scene = texture(u_scene, v_uv).rgb;

    // 2) dim & tint
    vec3 base = scene * u_dim;
    base = mix(base, base * u_tint, 0.2);

    // 3) build a moving vertical‐stripe pattern
    //    normalize to aspect‐correct X
    float aspect = u_resolution.x/u_resolution.y;
    float x = (v_uv.x - 0.5)*aspect + 0.5;
    // drift stripes by playerX
    float p = x * u_rayCount + u_playerX * u_raySpeed;
    // stripe intensity: 0→1 in a triangular/abs(sin) shape
    float stripe = abs(sin(p * 3.14159));
    // sharpen into narrow beams
    float beam = smoothstep(1.0 - u_rayWidth, 1.0, stripe);
    // final ray mask
    float mask = beam * u_rayStrength;

    // 4) ray color (warm pale light)
    vec3 rayColor = vec3(1.0,0.85,0.6);

    // 5) composite
    vec3 color = base + rayColor * mask;

    fragColor = vec4(color, 1.0);
}
