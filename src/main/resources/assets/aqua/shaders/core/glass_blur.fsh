#version 330

// Aqua Client - frosted glass backdrop blur (one pass of a small separable Gaussian).
// DOWNSAMPLE: averages a 4x4 block of the full-size frame into one texel.
// BLUR_H / BLUR_V: 9-tap Gaussian (5 bilinear fetches) along one axis.
uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 px = 1.0 / vec2(textureSize(InSampler, 0));
#ifdef DOWNSAMPLE
    vec3 c = texture(InSampler, texCoord + px * vec2(-1.0, -1.0)).rgb;
    c += texture(InSampler, texCoord + px * vec2(1.0, -1.0)).rgb;
    c += texture(InSampler, texCoord + px * vec2(-1.0, 1.0)).rgb;
    c += texture(InSampler, texCoord + px * vec2(1.0, 1.0)).rgb;
    fragColor = vec4(c * 0.25, 1.0);
#else
#ifdef BLUR_H
    vec2 dir = vec2(px.x, 0.0) * SPREAD;
#else
    vec2 dir = vec2(0.0, px.y) * SPREAD;
#endif
    vec3 c = texture(InSampler, texCoord).rgb * 0.2270270270;
    c += texture(InSampler, texCoord + dir * 1.3846153846).rgb * 0.3162162162;
    c += texture(InSampler, texCoord - dir * 1.3846153846).rgb * 0.3162162162;
    c += texture(InSampler, texCoord + dir * 3.2307692308).rgb * 0.0702702703;
    c += texture(InSampler, texCoord - dir * 3.2307692308).rgb * 0.0702702703;
    fragColor = vec4(c, 1.0);
#endif
}
