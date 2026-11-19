package dev.aqua.client.render.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/** One batched run of glyph quads that share an atlas page. */
final class TextRunState implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final TextureSetup setup;
    private final float[] quads; // x0, y0, x1, y1, u0, v0, u1, v1 per glyph
    private final int count;
    private final int color;
    @Nullable private final ScreenRect scissor;
    @Nullable private final ScreenRect bounds;

    TextRunState(Matrix3x2f pose, TextureSetup setup, float[] quads, int count, int color,
                 float minX, float minY, float maxX, float maxY, @Nullable ScreenRect scissor) {
        this.pose = pose;
        this.setup = setup;
        this.quads = quads;
        this.count = count;
        this.color = color;
        this.scissor = scissor;
        ScreenRect r = new ScreenRect((int) Math.floor(minX), (int) Math.floor(minY),
                Math.max(1, (int) Math.ceil(maxX - minX) + 1), Math.max(1, (int) Math.ceil(maxY - minY) + 1))
                .transformEachVertex(pose);
        this.bounds = scissor != null ? scissor.intersection(r) : r;
    }

    @Override
    public void setupVertices(VertexConsumer v) {
        for (int i = 0; i < count; i++) {
            int o = i * 8;
            float x0 = quads[o], y0 = quads[o + 1], x1 = quads[o + 2], y1 = quads[o + 3];
            float u0 = quads[o + 4], v0 = quads[o + 5], u1 = quads[o + 6], v1 = quads[o + 7];
            v.vertex(pose, x0, y0).texture(u0, v0).color(color);
            v.vertex(pose, x0, y1).texture(u0, v1).color(color);
            v.vertex(pose, x1, y1).texture(u1, v1).color(color);
            v.vertex(pose, x1, y0).texture(u1, v0).color(color);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }

    @Override
    public TextureSetup textureSetup() {
        return setup;
    }

    @Override
    public @Nullable ScreenRect scissorArea() {
        return scissor;
    }

    @Override
    public @Nullable ScreenRect bounds() {
        return bounds;
    }
}
