package dev.aqua.client.render.font;

import dev.aqua.client.AquaClient;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import java.awt.Font;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Smooth UI font (Inter, SIL OFL 1.1) rendered from per-pixel-size glyph atlases.
 * Sizes are given in GUI units; glyphs are rasterised for the effective physical pixel size
 * (GUI scale x pose scale) and snapped to the physical pixel grid.
 */
public final class AquaFont {
    public enum Weight {
        REGULAR("Inter-Regular.ttf"), MEDIUM("Inter-Medium.ttf"), SEMIBOLD("Inter-SemiBold.ttf"), BOLD("Inter-Bold.ttf");

        final String file;

        Weight(String file) {
            this.file = file;
        }
    }

    private static final Map<Weight, Font> FONTS = new EnumMap<>(Weight.class);
    private static final Map<Weight, Int2ObjectOpenHashMap<GlyphAtlas>> ATLASES = new EnumMap<>(Weight.class);
    private static final Vector2f TMP = new Vector2f();
    private static float[] quadBuffer = new float[8 * 64];

    private AquaFont() {}

    /** Loads font files. Called once during client initialisation (never per frame). */
    public static void load() {
        for (Weight w : Weight.values()) {
            try (InputStream in = AquaFont.class.getResourceAsStream("/assets/aqua/fonts/" + w.file)) {
                if (in == null) {
                    throw new IllegalStateException("missing font " + w.file);
                }
                FONTS.put(w, Font.createFont(Font.TRUETYPE_FONT, in));
            } catch (Exception e) {
                AquaClient.LOGGER.error("Could not load font {}, falling back to SansSerif", w.file, e);
                FONTS.put(w, new Font(Font.SANS_SERIF, w.ordinal() >= 2 ? Font.BOLD : Font.PLAIN, 12));
            }
            ATLASES.put(w, new Int2ObjectOpenHashMap<>());
        }
    }

    /** Frees the GUI atlases (GUI scale change); the 64 px world-text atlases are kept. */
    public static void invalidate() {
        for (Int2ObjectOpenHashMap<GlyphAtlas> map : ATLASES.values()) {
            var it = map.int2ObjectEntrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                if (e.getIntKey() != 64) {
                    e.getValue().close();
                    it.remove();
                }
            }
        }
    }

    /** Builds atlases for the sizes the UI uses at the current GUI scale (outside of frame rendering). */
    public static void prewarm(float... guiSizes) {
        int scale = guiScale();
        for (Weight w : Weight.values()) {
            for (float s : guiSizes) {
                atlas(w, Math.max(4, Math.round(s * scale)));
            }
        }
    }

    static int guiScale() {
        return Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor());
    }

    static GlyphAtlas atlas(Weight w, int px) {
        Int2ObjectOpenHashMap<GlyphAtlas> map = ATLASES.get(w);
        GlyphAtlas a = map.get(px);
        if (a == null) {
            a = new GlyphAtlas(FONTS.get(w), px, "font_" + w.name() + "_" + px);
            map.put(px, a);
        }
        return a;
    }

    private static float poseScale(Matrix3x2f m) {
        return (float) Math.sqrt(m.m00 * m.m00 + m.m01 * m.m01);
    }

    /** Width in GUI units. */
    public static float width(String text, float size, Weight w) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int scale = guiScale();
        int px = Math.max(4, Math.round(size * scale));
        GlyphAtlas a = atlas(w, px);
        return a.width(text) / scale;
    }

    /** Line height (ascent + descent) in GUI units. */
    public static float height(float size, Weight w) {
        int scale = guiScale();
        int px = Math.max(4, Math.round(size * scale));
        GlyphAtlas a = atlas(w, px);
        return a.lineHeight / scale;
    }

    /** Cap height approximation used for vertical centring. */
    public static float capHeight(float size) {
        return size * 0.727f;
    }

    /**
     * Draws text whose line box top-left is at (x, y). Returns the advance in GUI units.
     */
    public static float draw(DrawContext ctx, String text, float x, float y, float size, Weight w, int color) {
        if (text == null || text.isEmpty() || (color >>> 24) == 0) {
            return 0;
        }
        Matrix3x2f pose = new Matrix3x2f(ctx.getMatrices());
        float ps = poseScale(pose);
        if (ps <= 0.0001f) {
            return 0;
        }
        int scale = guiScale();
        float effective = scale * ps;
        int px = Math.max(4, Math.round(size * effective));
        GlyphAtlas a = atlas(w, px);
        float unitsPerPx = 1f / effective;
        float baseline = y + a.ascent * unitsPerPx;

        // Snap the origin to the physical pixel grid.
        pose.transformPosition(x, baseline, TMP);
        float sx = Math.round(TMP.x * scale) / (float) scale;
        float sy = Math.round(TMP.y * scale) / (float) scale;
        Matrix3x2f inv = new Matrix3x2f(pose).invert();
        inv.transformPosition(sx, sy, TMP);
        float ox = TMP.x;
        float oy = TMP.y;

        int len = text.length();
        if (quadBuffer.length < len * 8) {
            quadBuffer = new float[len * 8 * 2];
        }
        float pen = 0;
        int currentPage = -1;
        int n = 0;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < len; i++) {
            GlyphAtlas.Glyph g = a.glyph(text.charAt(i));
            if (g.page() >= 0) {
                if (currentPage != -1 && g.page() != currentPage && n > 0) {
                    flush(ctx, pose, a, currentPage, n, color, minX, minY, maxX, maxY);
                    n = 0;
                    minX = minY = Float.MAX_VALUE;
                    maxX = maxY = -Float.MAX_VALUE;
                }
                currentPage = g.page();
                float gx0 = ox + (Math.round(pen) + g.offsetX()) * unitsPerPx;
                float gy0 = oy + g.offsetY() * unitsPerPx;
                float gx1 = gx0 + g.width() * unitsPerPx;
                float gy1 = gy0 + g.height() * unitsPerPx;
                int o = n * 8;
                quadBuffer[o] = gx0;
                quadBuffer[o + 1] = gy0;
                quadBuffer[o + 2] = gx1;
                quadBuffer[o + 3] = gy1;
                quadBuffer[o + 4] = g.u0();
                quadBuffer[o + 5] = g.v0();
                quadBuffer[o + 6] = g.u1();
                quadBuffer[o + 7] = g.v1();
                n++;
                minX = Math.min(minX, gx0);
                minY = Math.min(minY, gy0);
                maxX = Math.max(maxX, gx1);
                maxY = Math.max(maxY, gy1);
            }
            pen += g.advance();
        }
        if (n > 0) {
            flush(ctx, pose, a, currentPage, n, color, minX, minY, maxX, maxY);
        }
        return pen * unitsPerPx;
    }

    private static void flush(DrawContext ctx, Matrix3x2f pose, GlyphAtlas a, int page, int n, int color,
                              float minX, float minY, float maxX, float maxY) {
        float[] copy = new float[n * 8];
        System.arraycopy(quadBuffer, 0, copy, 0, n * 8);
        ctx.state.addSimpleElement(new TextRunState(pose, a.page(page).setup(), copy, n, color,
                minX, minY, maxX, maxY, ctx.scissorStack.peekLast()));
    }

    /** Trims text with an ellipsis so it fits within maxWidth GUI units. */
    public static String ellipsize(String text, float maxWidth, float size, Weight w) {
        if (width(text, size, w) <= maxWidth) {
            return text;
        }
        String ell = "…";
        float ew = width(ell, size, w);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (width(sb.toString(), size, w) + ew > maxWidth) {
                sb.setLength(Math.max(0, sb.length() - 1));
                break;
            }
        }
        return sb + ell;
    }

    /** Resolves a weight's atlas for world-space rendering (pixel size explicitly chosen). */
    public static GlyphAtlas worldAtlas(Weight w, int px) {
        return atlas(w, px);
    }
}
