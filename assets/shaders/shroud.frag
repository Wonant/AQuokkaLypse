#version 140
in vec2 v_texCoord;
in vec4 v_color;
uniform sampler2D u_texture;
uniform vec2 iResolution;
uniform float iTime;
out vec4 FragColor;

float rand(vec2 coords) {
    return fract(sin(dot(coords, vec2(56.3456, 78.3456)) * 5.0) * 10000.0);
}

float noise(vec2 coords) {
    vec2 i = floor(coords);
    vec2 f = fract(coords);
    float a = rand(i);
    float b = rand(i + vec2(1.0, 0.0));
    float c = rand(i + vec2(0.0, 1.0));
    float d = rand(i + vec2(1.0, 1.0));
    vec2 cubic = f * f * (3.0 - 2.0 * f);
    return mix(a, b, cubic.x)
         + (c - a) * cubic.y * (1.0 - cubic.x)
         + (d - b) * cubic.x * cubic.y;
}

float fbm(vec2 coords) {
    float value = 0.0;
    float scale = 0.5;
    for (int i = 0; i < 10; ++i) {
        value += noise(coords) * scale;
        coords *= 4.0;
        scale *= 0.5;
    }
    return value;
}

bool isEdge(vec2 uv) {
    float threshold = 0.1;
    float centerAlpha = texture(u_texture, uv).a;
    if (centerAlpha > threshold) return false;

    // Use a smaller delta for thinner outline
    float delta = 0.5 / 512.0; // Reduced from 1.0 to 0.5 for thinner outline

    // Check fewer surrounding pixels for a thinner outline
    // Only check immediate neighbors (not diagonals) for an even thinner effect
    if (texture(u_texture, uv + vec2(delta, 0.0)).a > threshold) return true;
    if (texture(u_texture, uv + vec2(-delta, 0.0)).a > threshold) return true;
    if (texture(u_texture, uv + vec2(0.0, delta)).a > threshold) return true;
    if (texture(u_texture, uv + vec2(0.0, -delta)).a > threshold) return true;

    return false;
}

void main() {
    vec2 uv = v_texCoord * iResolution;
    float final = 0.0;
    for (int i = 1; i <= 5; ++i) {
        vec2 motion = vec2(fbm(uv + iTime * 0.05 + vec2(float(i))));
        final += fbm(uv + motion);
    }
    final /= 5.0;

    // Brighter colors for the inside
    vec3 colorA = vec3(-0.15); // Increased from -0.3 for more brightness
    vec3 colorB = vec3(0.55, 0.5, 0.7) + vec3(0.65); // Increased brightness
    vec3 fogColor = mix(colorA, colorB, final);

    vec4 texColor = texture(u_texture, v_texCoord) * v_color;
    bool edge = isEdge(v_texCoord);

    if (edge) {
        // Semi-transparent white outline (0.8 alpha instead of 1.0)
        FragColor = vec4(1.0, 1.0, 1.0, 0.8);
    } else {
        // Increase the brightness of the content inside
        FragColor = vec4(texColor.rgb * fogColor * 1.2, texColor.a); // Multiplied by 1.2 to increase brightness
    }
}
