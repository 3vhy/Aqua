package dev.aqua.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/** Render pipelines owned by Aqua. Registered once during client initialisation. */
public final class AquaPipelines {
    private AquaPipelines() {}

    /** Anti-aliased rounded rectangles, borders and soft shadows for the GUI. */
    public static final RenderPipeline ROUNDED = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(Identifier.of("aqua", "pipeline/rounded"))
                    .withVertexShader(Identifier.of("aqua", "core/rounded"))
                    .withFragmentShader(Identifier.of("aqua", "core/rounded"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    /** World-space translucent coloured geometry, depth tested, no depth write, no culling. */
    public static final RenderPipeline WORLD_COLOR = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("aqua", "pipeline/world_color"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build());

    /** Opaque shaded geometry for 3D cosmetics (characters, knives, hats): depth tested and written. */
    public static final RenderPipeline WORLD_SOLID = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("aqua", "pipeline/world_solid"))
                    .withoutBlend()
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(true)
                    .withCull(false)
                    .build());

    /** World-space additive glow geometry (particles, halos, trails). */
    public static final RenderPipeline WORLD_GLOW = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("aqua", "pipeline/world_glow"))
                    .withBlend(BlendFunction.LIGHTNING)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build());

    /** World-space textured translucent quads (sky dome, glowing text), depth tested. */
    public static final RenderPipeline WORLD_TEXTURED = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of("aqua", "pipeline/world_textured"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build());

    /** World-space textured additive quads (text glow). */
    public static final RenderPipeline WORLD_TEXTURED_GLOW = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of("aqua", "pipeline/world_textured_glow"))
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build());

    /** Frosted glass surface: the blurred frame (Sampler0) shown through a rounded rectangle. */
    public static final RenderPipeline GLASS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                    .withLocation(Identifier.of("aqua", "pipeline/glass"))
                    .withVertexShader(Identifier.of("aqua", "core/rounded"))
                    .withFragmentShader(Identifier.of("aqua", "core/glass"))
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    public static final RenderPipeline GLASS_DOWNSAMPLE = blurPass("glass_downsample", "DOWNSAMPLE");
    public static final RenderPipeline GLASS_BLUR_H = blurPass("glass_blur_h", "BLUR_H");
    public static final RenderPipeline GLASS_BLUR_V = blurPass("glass_blur_v", "BLUR_V");

    private static RenderPipeline blurPass(String name, String define) {
        return RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
                        .withLocation(Identifier.of("aqua", "pipeline/" + name))
                        .withVertexShader(Identifier.ofVanilla("core/screenquad"))
                        .withFragmentShader(Identifier.of("aqua", "core/glass_blur"))
                        .withShaderDefine(define)
                        .withShaderDefine("SPREAD", 1.25f)
                        .withSampler("InSampler")
                        .withoutBlend()
                        .build());
    }

    public static final RenderPipeline POST_COPY = postPass("post_copy", null, false);
    public static final RenderPipeline POST_GRADE = postPass("post_grade", "GRADE", false);
    public static final RenderPipeline POST_MIX = postPass("post_mix", "MIX", true);

    private static RenderPipeline postPass(String name, String define, boolean prev) {
        RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
                .withLocation(Identifier.of("aqua", "pipeline/" + name))
                .withVertexShader(Identifier.ofVanilla("core/screenquad"))
                .withFragmentShader(Identifier.of("aqua", "core/post_fx"))
                .withSampler("InSampler")
                .withoutBlend();
        if (define != null) {
            b.withShaderDefine(define);
        }
        if (prev) {
            b.withSampler("PrevSampler");
            b.withUniform("AquaMix", net.minecraft.client.gl.UniformType.UNIFORM_BUFFER);
        }
        if ("GRADE".equals(define)) {
            b.withUniform("AquaGrade", net.minecraft.client.gl.UniformType.UNIFORM_BUFFER);
        }
        return RenderPipelines.register(b.build());
    }

    /** Forces class loading so the static pipelines are registered before shaders compile. */
    public static void init() {
    }
}
