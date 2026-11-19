package dev.aqua.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import dev.aqua.client.render.font.AquaFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

/**
 * Aqua's immediate-style 2D drawing facade on top of the 1.21.11 GUI render-state system.
 * All coordinates are float GUI units.
 */
public final class R2D {
    public static final int ALL = 15;
    public static final int TOP = 1 | 2;
    public static final int BOTTOM = 4 | 8;
    public static final int LEFT = 1 | 8;
    public static final int RIGHT = 2 | 4;

    private R2D() {}

    private static Matrix3x2f pose(DrawContext ctx) {
        return new Matrix3x2f(ctx.getMatrices());
    }

    public static void rect(DrawContext ctx, float x, float y, float w, float h, int color) {
        round(ctx, x, y, w, h, 0, color);
    }

    public static void round(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        roundCorners(ctx, x, y, w, h, r, ALL, color);
    }

    public static void roundCorners(DrawContext ctx, float x, float y, float w, float h, float r, int mask, int color) {
        if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) {
            return;
        }
        ctx.state.addSimpleElement(new RoundedRectState(pose(ctx), x, y, w, h, r, 0, 0, mask,
                color, color, color, color, ctx.scissorStack.peekLast()));
    }

    public static void roundGradientV(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom) {
        if (w <= 0 || h <= 0) {
            return;
        }
        ctx.state.addSimpleElement(new RoundedRectState(pose(ctx), x, y, w, h, r, 0, 0, ALL,
                top, top, bottom, bottom, ctx.scissorStack.peekLast()));
    }

    public static void roundGradientH(DrawContext ctx, float x, float y, float w, float h, float r, int left, int right) {
        if (w <= 0 || h <= 0) {
            return;
        }
        ctx.state.addSimpleElement(new RoundedRectState(pose(ctx), x, y, w, h, r, 0, 0, ALL,
                left, right, right, left, ctx.scissorStack.peekLast()));
    }

    public static void outline(DrawContext ctx, float x, float y, float w, float h, float r, float width, int color) {
        outlineCorners(ctx, x, y, w, h, r, width, ALL, color);
    }

    public static void outlineCorners(DrawContext ctx, float x, float y, float w, float h, float r, float width, int mask, int color) {
        if (w <= 0 || h <= 0 || Colors.alpha(color) == 0) {
            return;
        }
        ctx.state.addSimpleElement(new RoundedRectState(pose(ctx), x, y, w, h, r, Math.max(0.05f, width), 0, mask,
                color, color, color, color, ctx.scissorStack.peekLast()));
    }

    /** Soft drop shadow / glow around a rounded rectangle. */
    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r, float softness, int color) {
        if (softness <= 0 || Colors.alpha(color) == 0 || !dev.aqua.client.Aqua.ui().shadows()) {
            return;
        }
        ctx.state.addSimpleElement(new RoundedRectState(pose(ctx), x, y, w, h, r, 0, Math.min(64f, softness), ALL,
                color, color, color, color, ctx.scissorStack.peekLast()));
    }

    public static void circle(DrawContext ctx, float cx, float cy, float radius, int color) {
        round(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    public static void ring(DrawContext ctx, float cx, float cy, float radius, float width, int color) {
        outline(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, width, color);
    }

    /** Anti-aliased line segment with round caps. */
    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2, float width, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) {
            return;
        }
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x1, y1);
        ctx.getMatrices().rotate((float) Math.atan2(dy, dx));
        round(ctx, -width / 2f, -width / 2f, len + width, width, width / 2f, color);
        ctx.getMatrices().popMatrix();
    }

    public static void icon(DrawContext ctx, Icon icon, float x, float y, float size, int color) {
        if (Colors.alpha(color) == 0) {
            return;
        }
        Matrix3x2f pose = pose(ctx);
        float ps = (float) Math.sqrt(pose.m00 * pose.m00 + pose.m01 * pose.m01);
        int scale = Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor());
        int px = Math.max(6, Math.round(size * scale * ps));
        IconAtlas atlas = IconAtlas.get(px);
        float u0 = atlas.u0(icon);
        float v0 = atlas.v0(icon);
        float span = atlas.span();
        ctx.state.addSimpleElement(TexQuadState.of(RenderPipelines.GUI_TEXTURED, atlas.setup(), pose,
                x, y, x + size, y + size, u0, v0, u0 + span, v0 + span, color, ctx.scissorStack.peekLast()));
    }

    /** Draws a registered texture with linear filtering (for smooth logo scaling). */
    public static void texture(DrawContext ctx, Identifier id, float x, float y, float w, float h, int color) {
        AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(id);
        TextureSetup setup = TextureSetup.of(tex.getGlTextureView(), RenderSystem.getSamplerCache()
                .get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false));
        ctx.state.addSimpleElement(TexQuadState.of(RenderPipelines.GUI_TEXTURED, setup, pose(ctx),
                x, y, x + w, y + h, 0, 0, 1, 1, color, ctx.scissorStack.peekLast()));
    }

    public static void textureRegion(DrawContext ctx, Identifier id, float x, float y, float w, float h,
                                     float u0, float v0, float u1, float v1, int color) {
        AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(id);
        TextureSetup setup = TextureSetup.of(tex.getGlTextureView(), RenderSystem.getSamplerCache()
                .get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false));
        ctx.state.addSimpleElement(TexQuadState.of(RenderPipelines.GUI_TEXTURED, setup, pose(ctx),
                x, y, x + w, y + h, u0, v0, u1, v1, color, ctx.scissorStack.peekLast()));
    }

    // ----- text -----

    public static float text(DrawContext ctx, String s, float x, float y, float size, AquaFont.Weight w, int color) {
        return AquaFont.draw(ctx, s, x, y, size, w, color);
    }

    /** Draws text vertically centred on cy (using cap height). */
    public static float textMid(DrawContext ctx, String s, float x, float cy, float size, AquaFont.Weight w, int color) {
        float h = AquaFont.height(size, w);
        return AquaFont.draw(ctx, s, x, cy - h / 2f, size, w, color);
    }

    public static float textCentered(DrawContext ctx, String s, float cx, float cy, float size, AquaFont.Weight w, int color) {
        float tw = AquaFont.width(s, size, w);
        return textMid(ctx, s, cx - tw / 2f, cy, size, w, color);
    }

    public static float textRight(DrawContext ctx, String s, float right, float cy, float size, AquaFont.Weight w, int color) {
        float tw = AquaFont.width(s, size, w);
        return textMid(ctx, s, right - tw, cy, size, w, color);
    }

    public static float width(String s, float size, AquaFont.Weight w) {
        return AquaFont.width(s, size, w);
    }

    // ----- scissor helpers -----

    public static void pushClip(DrawContext ctx, float x, float y, float w, float h) {
        ctx.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + w), (int) Math.ceil(y + h));
    }

    public static void popClip(DrawContext ctx) {
        ctx.disableScissor();
    }
}
