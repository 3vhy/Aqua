#version 330

// Aqua Client - signed-distance rounded rectangle (vertex stage)
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;     // local coordinate relative to the rectangle centre (GUI units)
in ivec2 UV1;    // half extents * 8
in ivec2 UV2;    // corner radius * 8, border width * 8
in vec3 Normal;  // x: softness / 64, y: corner mask / 127

out vec4 vertexColor;
out vec2 localPos;
flat out vec4 rectParams;
flat out vec2 extraParams;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    localPos = UV0;
    rectParams = vec4(vec2(UV1) / 8.0, vec2(UV2) / 8.0);
    extraParams = vec2(Normal.x * 64.0, floor(Normal.y * 127.0 + 0.5));
}
