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

    /** Forces class loading so the static pipelines are registered before shaders compile. */
    public static void init() {
    }
}
