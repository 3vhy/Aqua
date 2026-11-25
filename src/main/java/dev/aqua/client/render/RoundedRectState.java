package dev.aqua.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/**
 * GUI render state for an anti-aliased rounded rectangle drawn by {@link AquaPipelines#ROUNDED}.
 * Coordinates are GUI units (floats); the shader derives its anti-aliasing width from
 * screen-space derivatives so edges stay one physical pixel wide at every GUI scale.
 */
public class RoundedRectState implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final float x;
    private final float y;
    private final float w;
    private final float h;
    private final float radius;
    private final float border;
    private final float softness;
    private final int cornerMask;
    private final int cTopLeft;
    private final int cTopRight;
    private final int cBottomRight;
    private final int cBottomLeft;
    @Nullable private final ScreenRect scissor;
    @Nullable private final ScreenRect bounds;

    public RoundedRectState(Matrix3x2f pose, float x, float y, float w, float h, float radius, float border, float softness,
                            int cornerMask, int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft,
                            @Nullable ScreenRect scissor) {
        this.pose = pose;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.radius = radius;
        this.border = border;
        this.softness = softness;
        this.cornerMask = cornerMask;
        this.cTopLeft = cTopLeft;
        this.cTopRight = cTopRight;
        this.cBottomRight = cBottomRight;
        this.cBottomLeft = cBottomLeft;
        this.scissor = scissor;
        float pad = softness > 0 ? softness : 1f;
        int bx = (int) Math.floor(x - pad);
        int by = (int) Math.floor(y - pad);
        int bw = (int) Math.ceil(w + pad * 2) + 1;
        int bh = (int) Math.ceil(h + pad * 2) + 1;
        ScreenRect r = new ScreenRect(bx, by, Math.max(1, bw), Math.max(1, bh)).transformEachVertex(pose);
        this.bounds = scissor != null ? scissor.intersection(r) : r;
    }

    @Override
    public void setupVertices(VertexConsumer v) {
        float pad = softness > 0 ? softness : 1.5f;
        float hw = w / 2f;
        float hh = h / 2f;
        float cx = x + hw;
        float cy = y + hh;
        int ehw = clampShort(hw * 8f);
        int ehh = clampShort(hh * 8f);
        int er = clampShort(radius * 8f);
        int eb = clampShort(border * 8f);
        float nSoft = Math.min(1f, softness / 64f);
        float nMask = cornerMask / 127f;
        float x0 = x - pad;
        float y0 = y - pad;
        float x1 = x + w + pad;
        float y1 = y + h + pad;
        put(v, x0, y0, cx, cy, cTopLeft, ehw, ehh, er, eb, nSoft, nMask);
        put(v, x0, y1, cx, cy, cBottomLeft, ehw, ehh, er, eb, nSoft, nMask);
        put(v, x1, y1, cx, cy, cBottomRight, ehw, ehh, er, eb, nSoft, nMask);
        put(v, x1, y0, cx, cy, cTopRight, ehw, ehh, er, eb, nSoft, nMask);
    }

    private void put(VertexConsumer v, float px, float py, float cx, float cy, int color, int ehw, int ehh, int er, int eb,
                     float nSoft, float nMask) {
        v.vertex(pose, px, py)
                .color(color)
                .texture(px - cx, py - cy)
                .overlay(ehw, ehh)
                .light(er, eb)
                .normal(nSoft, nMask, 0f);
    }

    private static int clampShort(float v) {
        return (int) Math.max(0, Math.min(32767, Math.round(v)));
    }

    @Override
    public RenderPipeline pipeline() {
        return AquaPipelines.ROUNDED;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
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
