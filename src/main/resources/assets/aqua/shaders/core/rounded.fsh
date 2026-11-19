#version 330

// Aqua Client - signed-distance rounded rectangle (fragment stage)
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 vertexColor;
in vec2 localPos;
flat in vec4 rectParams;
flat in vec2 extraParams;

out vec4 fragColor;

float cornerRadius(vec2 p, float r, float mask) {
    // mask bits: 1 = top-left, 2 = top-right, 4 = bottom-right, 8 = bottom-left
    int m = int(mask);
    int bit = p.x < 0.0 ? (p.y < 0.0 ? 1 : 8) : (p.y < 0.0 ? 2 : 4);
    return (m & bit) != 0 ? r : 0.0;
}

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 halfSize = rectParams.xy;
    float radius = min(cornerRadius(localPos, rectParams.z, extraParams.y), min(halfSize.x, halfSize.y));
    float border = rectParams.w;
    float softness = extraParams.x;

    float d = sdRoundBox(localPos, halfSize, radius);
    float alpha;
    if (softness > 0.0) {
        // Soft shadow / glow: smooth falloff outside the shape.
        float t = clamp((d + softness * 0.35) / (softness * 1.35), 0.0, 1.0);
        alpha = 1.0 - t;
        alpha = alpha * alpha * (3.0 - 2.0 * alpha);
    } else {
        float aa = max(length(vec2(dFdx(d), dFdy(d))), 1.0e-4);
        alpha = clamp(0.5 - d / aa, 0.0, 1.0);
        if (border > 0.0) {
            float inner = clamp(0.5 - (d + border) / aa, 0.0, 1.0);
            alpha = max(alpha - inner, 0.0);
        }
    }

    vec4 color = vertexColor;
    color.a *= alpha;
    if (color.a <= 0.002) {
        discard;
    }
    fragColor = color * ColorModulator;
}
