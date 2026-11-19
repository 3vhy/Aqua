package dev.aqua.client.render.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.GpuSampler;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Anti-aliased glyph atlas for one font face at one physical pixel size. Glyphs are rasterised with
 * Java2D (headless) exactly once, packed into 512x512 pages and sampled with linear filtering, so text
 * is crisp at every GUI scale. Must only be used on the render thread.
 */
public final class GlyphAtlas {
    private static final int PAGE = 512;
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private static int textureCounter;
    /** Coverage gamma: light UI text on dark surfaces reads heavier and brighter with a slight boost. */
    private static final int[] GAMMA = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            GAMMA[i] = (int) Math.round(Math.pow(i / 255.0, 0.78) * 255.0);
        }
    }

    public record Glyph(int page, float u0, float v0, float u1, float v1, int width, int height,
                        float offsetX, float offsetY, float advance) {}

    public static final class Page {
        final NativeImage image;
        final NativeImageBackedTexture texture;
        final Identifier id;
        TextureSetup setup;
        boolean dirty;

        Page(String name) {
            this.image = new NativeImage(NativeImage.Format.RGBA, PAGE, PAGE, true);
            this.image.fillRect(0, 0, PAGE, PAGE, 0x00FFFFFF);
            this.texture = new NativeImageBackedTexture(() -> name, image);
            this.id = Identifier.of("aqua", "dynamic/" + name.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
        }

        public TextureSetup setup() {
            if (dirty) {
                texture.upload();
                dirty = false;
            }
            if (setup == null) {
                setup = TextureSetup.of(texture.getGlTextureView(), LINEAR.get());
            }
            return setup;
        }

        public Identifier id() {
            if (dirty) {
                texture.upload();
                dirty = false;
            }
            return id;
        }
    }

    static final java.util.function.Supplier<GpuSampler> LINEAR = () -> RenderSystem.getSamplerCache()
            .get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);

    private final Font font;
    private final int pixelSize;
    private final String name;
    private final Char2ObjectOpenHashMap<Glyph> glyphs = new Char2ObjectOpenHashMap<>();
    private final List<Page> pages = new ArrayList<>();
    private int penX = 1;
    private int penY = 1;
    private int rowHeight;
    public final float ascent;
    public final float descent;
    public final float lineHeight;

    GlyphAtlas(Font baseFont, int pixelSize, String name) {
        this.font = baseFont.deriveFont((float) pixelSize);
        this.pixelSize = pixelSize;
        this.name = name;
        LineMetrics lm = font.getLineMetrics("Hgjy", FRC);
        this.ascent = lm.getAscent();
        this.descent = lm.getDescent();
        this.lineHeight = lm.getAscent() + lm.getDescent();
        // Pre-build printable ASCII + Latin-1 so normal HUD text never rasterises during a frame.
        for (char c = 32; c < 127; c++) {
            glyph(c);
        }
        for (char c = 160; c < 256; c++) {
            glyph(c);
        }
        for (char c : "•·…–—←↑→↓⌘✓★♪♫°×".toCharArray()) {
            glyph(c);
        }
    }

    public int pixelSize() {
        return pixelSize;
    }

    public Page page(int index) {
        return pages.get(index);
    }

    public Glyph glyph(char c) {
        Glyph g = glyphs.get(c);
        if (g != null) {
            return g;
        }
        g = rasterize(c);
        glyphs.put(c, g);
        return g;
    }

    private Glyph rasterize(char c) {
        if (!font.canDisplay(c)) {
            if (c != '?') {
                Glyph q = glyph('?');
                return q;
            }
        }
        GlyphVector gv = font.createGlyphVector(FRC, new char[]{c});
        float advance = (float) gv.getGlyphMetrics(0).getAdvanceX();
        Rectangle bounds = gv.getPixelBounds(FRC, 0, 0);
        if (bounds.width <= 0 || bounds.height <= 0 || Character.isWhitespace(c)) {
            return new Glyph(-1, 0, 0, 0, 0, 0, 0, 0, 0, advance);
        }
        int pad = 1;
        int gw = bounds.width + pad * 2;
        int gh = bounds.height + pad * 2;
        BufferedImage img = new BufferedImage(gw, gh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setColor(Color.WHITE);
        g2.drawGlyphVector(gv, -bounds.x + pad, -bounds.y + pad);
        g2.dispose();

        if (penX + gw + 1 > PAGE) {
            penX = 1;
            penY += rowHeight + 1;
            rowHeight = 0;
        }
        if (pages.isEmpty() || penY + gh + 1 > PAGE) {
            pages.add(new Page(name + "_" + (textureCounter++)));
            penX = 1;
            penY = 1;
            rowHeight = 0;
        }
        Page page = pages.get(pages.size() - 1);
        for (int yy = 0; yy < gh; yy++) {
            for (int xx = 0; xx < gw; xx++) {
                int a = GAMMA[(img.getRGB(xx, yy) >>> 24) & 0xFF];
                page.image.setColorArgb(penX + xx, penY + yy, (a << 24) | 0xFFFFFF);
            }
        }
        page.dirty = true;
        Glyph glyph = new Glyph(pages.size() - 1,
                penX / (float) PAGE, penY / (float) PAGE,
                (penX + gw) / (float) PAGE, (penY + gh) / (float) PAGE,
                gw, gh, bounds.x - pad, bounds.y - pad, advance);
        penX += gw + 1;
        rowHeight = Math.max(rowHeight, gh);
        return glyph;
    }

    public float width(String text) {
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            w += glyph(text.charAt(i)).advance();
        }
        return w;
    }

    static Rectangle2D stringBounds(Font font, String s) {
        return font.getStringBounds(s, FRC);
    }

    void close() {
        for (Page p : pages) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(p.id);
        }
        pages.clear();
        glyphs.clear();
    }
}
