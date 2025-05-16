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
    for (int i = 0; i < 6; ++i) {
        value += noise(coords) * scale;
        coords *= 3.0;
        scale *= 0.5;
    }
    return value;
}

// Bubble pattern using sin-based distance field
float bubblePattern(vec2 uv) {
    uv *= 10.0;
    float pattern = sin(uv.x + iTime) * sin(uv.y + iTime * 1.2);
    return smoothstep(0.4, 0.6, pattern * 0.5 + 0.5);
}

bool isEdge(vec2 uv) {
    float threshold = 0.1;
    float centerAlpha = texture(u_texture, uv).a;
    if (centerAlpha > threshold) return false;

    float delta = 0.3 / 512.0; // thinner outline
    if (texture(u_texture, uv + vec2(delta, 0.0)).a > threshold) return true;
    if (texture(u_texture, uv + vec2(-delta, 0.0)).a > threshold) return true;
    if (texture(u_texture, uv + vec2(0.0, delta)).a > threshold) return true;
    if (texture(u_texture, uv + vec2(0.0, -delta)).a > threshold) return true;

    return false;
}

void main() {
    vec2 uv = v_texCoord * iResolution;

    float final = 0.0;
    for (int i = 1; i <= 4; ++i) {
        vec2 motion = vec2(fbm(uv + iTime * 0.03 + vec2(float(i))));
        final += fbm(uv + motion);
    }
    final /= 4.0;

    // Light pink gradient
    vec3 pinkA = vec3(1.0, 0.85, 0.9);     // base soft pink
    vec3 pinkB = vec3(1.0, 0.95, 1.0);     // almost white pink
    vec3 pinkFog = mix(pinkA, pinkB, final);

    // Bubble overlay adds to brightness
    float bubble = bubblePattern(v_texCoord * iResolution / 512.0);
    vec3 bubbleColor = vec3(1.0, 0.9, 0.95) * bubble * 0.4;

    vec4 texColor = texture(u_texture, v_texCoord) * v_color;
    bool edge = isEdge(v_texCoord);

    if (edge) {
        FragColor = vec4(1.0, 0.6, 0.8, 0.7); // semi-transparent pink
    } else {
        FragColor = vec4(texColor.rgb * pinkFog + bubbleColor, texColor.a);
    }
}
