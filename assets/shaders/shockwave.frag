#version 140
in  vec2 v_texCoords;

uniform sampler2D sceneTex;   // bound to texture unit 0
uniform vec2  center;         // shock‑origin in NDC (0–1)
uniform float time;           // elapsed time

out vec4 fragColor;

void main() {
    vec3 shockParams = vec3(10.0, 0.8, 0.1);

    float offset      = fract(time);        // 0→1 every second
    float CurrentTime = time * offset;

    float d = distance(v_texCoords, center);
    vec2 uv = v_texCoords;

    if (d <= CurrentTime + shockParams.z &&
        d >= CurrentTime - shockParams.z) {

        float diff    = d - CurrentTime;
        float powDiff = 0.0;
        if (d > 0.05)
            powDiff = 1.0 - pow(abs(diff * shockParams.x),
                                 shockParams.y);

        float diffTime = diff * powDiff;
        vec2 dir       = normalize(v_texCoords - center);
        uv += (dir * diffTime) /
              (CurrentTime * d * 40.0);
    }

    fragColor = texture(sceneTex, uv);
}
