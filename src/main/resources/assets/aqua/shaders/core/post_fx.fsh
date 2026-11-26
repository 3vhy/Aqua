#version 330

// Aqua Client - full-screen passes over the game image: COPY, GRADE (Color Grading) and MIX
// (Motion Blur).
uniform sampler2D InSampler;
#ifdef MIX
uniform sampler2D PrevSampler;
layout(std140) uniform AquaMix {
    vec4 MixParams; // x: amount of the previous frame
};
#endif
#ifdef GRADE
layout(std140) uniform AquaGrade {
    vec4 GradeA; // saturation, contrast, brightness, vignette
    vec4 GradeB; // tint rgb, strength
};
#endif

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
#ifdef MIX
    vec3 p = texture(PrevSampler, texCoord).rgb;
    c = mix(c, p, clamp(MixParams.x, 0.0, 0.92));
#endif
#ifdef GRADE
    vec3 o = c;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(l), c, GradeA.x);
    c = (c - 0.5) * GradeA.y + 0.5;
    c += GradeA.z;
    c *= GradeB.rgb;
    vec2 d = texCoord - 0.5;
    float v = smoothstep(0.85, 0.2, length(d * vec2(1.0, 0.9)) * 1.25);
    c *= mix(1.0, v, GradeA.w);
    c = mix(o, clamp(c, 0.0, 1.0), GradeB.a);
#endif
    fragColor = vec4(c, 1.0);
}
