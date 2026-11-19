package dev.aqua.client.render.world;

import dev.aqua.client.render.Colors;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.render.font.GlyphAtlas;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Camera-facing text in the world using Aqua's smooth font atlas (with an optional additive glow). */
public final class WorldText {
    private static final int PX = 64;

    private WorldText() {}

    /**
     * Draws text centred at (x, y, z). {@code height} is the cap height in blocks.
     * {@code glow} 0..1 adds a soft additive halo.
     */
    public static void draw(Fx fx, VertexConsumerProvider vcp, String text, double x, double y, double z, float height,
                            AquaFont.Weight weight, int color, int glowColor, float glow) {
        GlyphAtlas atlas = AquaFont.worldAtlas(weight, PX);
        float unit = height / (PX * 0.73f);
        float total = atlas.width(text) * unit;
        Matrix4f m = fx.matrices.peek().getPositionMatrix();
        Vector3f r = fx.right;
        Vector3f u = fx.up;
        float cx = (float) (x - fx.cam.x);
        float cy = (float) (y - fx.cam.y);
        float cz = (float) (z - fx.cam.z);
        float pen = -total / 2f;
        float baseY = -height / 2f;
        for (int pass = glow > 0 ? 0 : 1; pass < 2; pass++) {
            float penX = pen;
            for (int i = 0; i < text.length(); i++) {
                GlyphAtlas.Glyph g = atlas.glyph(text.charAt(i));
                if (g.page() >= 0) {
                    VertexConsumer vc = vcp.getBuffer(pass == 0 ? FxLayers.texturedGlow(atlas.page(g.page()).id())
                            : FxLayers.textured(atlas.page(g.page()).id()));
                    float gx0 = penX + g.offsetX() * unit;
                    float gx1 = gx0 + g.width() * unit;
                    // Glyph offsets are relative to the baseline (negative = above it); world +y is up.
                    float gy0 = baseY - g.offsetY() * unit;
                    float gy1 = gy0 - g.height() * unit;
                    if (pass == 0) {
                        float spread = height * 0.08f;
                        for (int k = 0; k < 4; k++) {
                            float ox = (k % 2 == 0 ? -1 : 1) * spread;
                            float oy = (k < 2 ? -1 : 1) * spread;
                            quad(vc, m, r, u, cx, cy, cz, gx0 + ox, gy0 + oy, gx1 + ox, gy1 + oy, g, Colors.fade(glowColor, 0.22f * glow));
                        }
                    } else {
                        quad(vc, m, r, u, cx, cy, cz, gx0, gy0, gx1, gy1, g, color);
                    }
                }
                penX += g.advance() * unit;
            }
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vector3f r, Vector3f u, float cx, float cy, float cz,
                             float x0, float y0, float x1, float y1, GlyphAtlas.Glyph g, int color) {
        vtx(vc, m, r, u, cx, cy, cz, x0, y0, g.u0(), g.v0(), color);
        vtx(vc, m, r, u, cx, cy, cz, x0, y1, g.u0(), g.v1(), color);
        vtx(vc, m, r, u, cx, cy, cz, x1, y1, g.u1(), g.v1(), color);
        vtx(vc, m, r, u, cx, cy, cz, x1, y0, g.u1(), g.v0(), color);
    }

    private static void vtx(VertexConsumer vc, Matrix4f m, Vector3f r, Vector3f u, float cx, float cy, float cz,
                            float lx, float ly, float tu, float tv, int color) {
        vc.vertex(m, cx + r.x * lx + u.x * ly, cy + r.y * lx + u.y * ly, cz + r.z * lx + u.z * ly).texture(tu, tv).color(color);
    }
}
