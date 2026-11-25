#version 330

// Aqua Client - frosted glass surface: the blurred frame behind the panel, shown through a
// signed-distance rounded rectangle, with a little extra colour so it reads as glass, not fog.
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 localPos;
flat in vec4 rectParams;
flat in vec2 extraParams;

out vec4 fragColor;

float cornerRadius(vec2 p, float r, float mask) {
    int m = int(mask);
    int bit = p.x < 0.0 ? (p.y < 0.0 ? 1 : 8) : (p.y < 0.0 ? 2 : 4);
    return (m & bit) != 0 ? r : 0.0;
}

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 halfSize = rectParams.xy;
    float radius = min(cornerRadius(localPos, rectParams.z, extraParams.y), min(halfSize.x, halfSize.y));
    float d = sdRoundBox(localPos, halfSize, radius);
    float aa = max(length(vec2(dFdx(d), dFdy(d))), 1.0e-4);
    float alpha = clamp(0.5 - d / aa, 0.0, 1.0);
    if (alpha <= 0.002) {
        discard;
    }
    vec2 uv = gl_FragCoord.xy / max(ScreenSize, vec2(1.0));
    vec3 c = texture(Sampler0, uv).rgb;
    // Vibrancy: a touch more saturation, as frosted glass does.
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(l), c, 1.0 + rectParams.w);
    // Tame bright backdrops (daylight sky, snow) so light text on the glass stays readable.
    c *= 1.0 - 0.5 * smoothstep(0.22, 0.95, l);
    // Dither so the soft blur never shows bands.
    c += (hash(gl_FragCoord.xy) - 0.5) / 255.0;
    fragColor = vec4(c * vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
