package dev.aqua.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Rasterises the whole {@link Icon} set once per physical pixel size into a single texture. */
public final class IconAtlas {
    private static final int COLS = 8;
    private static final Int2ObjectOpenHashMap<IconAtlas> CACHE = new Int2ObjectOpenHashMap<>();

    final int cell;
    final int size;
    final int texSize;
    final NativeImageBackedTexture texture;
    final Identifier id;
    TextureSetup setup;

    private IconAtlas(int px) {
        this.size = px;
        this.cell = px + 2;
        int rows = (Icon.values().length + COLS - 1) / COLS;
        int need = Math.max(COLS, rows) * cell;
        int t = 16;
        while (t < need) {
            t <<= 1;
        }
        this.texSize = t;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, t, t, true);
        image.fillRect(0, 0, t, t, 0);
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        for (Icon icon : Icon.values()) {
            Graphics2D g = img.createGraphics();
            g.setComposite(java.awt.AlphaComposite.Clear);
            g.fillRect(0, 0, px, px);
            g.setComposite(java.awt.AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.scale(px / 24.0, px / 24.0);
            // Keep the stroke at least ~1.1 physical pixels so tiny icons stay legible.
            float strokeUnits = Math.max(1.9f, 1.15f * 24f / px);
            g.setStroke(new java.awt.BasicStroke(strokeUnits, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.setColor(Color.WHITE);
            icon.draw(g);
            g.dispose();
            int ox = (icon.ordinal() % COLS) * cell + 1;
            int oy = (icon.ordinal() / COLS) * cell + 1;
            for (int y = 0; y < px; y++) {
                for (int x = 0; x < px; x++) {
                    int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                    image.setColorArgb(ox + x, oy + y, (a << 24) | 0xFFFFFF);
                }
            }
        }
        this.texture = new NativeImageBackedTexture(() -> "aqua_icons_" + px, image);
        this.id = Identifier.of("aqua", "dynamic/icons_" + px);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
    }

    static IconAtlas get(int px) {
        IconAtlas a = CACHE.get(px);
        if (a == null) {
            a = new IconAtlas(px);
            CACHE.put(px, a);
        }
        return a;
    }

    TextureSetup setup() {
        if (setup == null) {
            setup = TextureSetup.of(texture.getGlTextureView(), RenderSystem.getSamplerCache()
                    .get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false));
        }
        return setup;
    }

    float u0(Icon icon) {
        return ((icon.ordinal() % COLS) * cell + 1) / (float) texSize;
    }

    float v0(Icon icon) {
        return ((icon.ordinal() / COLS) * cell + 1) / (float) texSize;
    }

    float span() {
        return size / (float) texSize;
    }

    /** Builds the atlases for common icon sizes at a GUI scale (called on scale change, not per frame). */
    public static void prewarm(int guiScale, float... sizes) {
        for (float s : sizes) {
            get(Math.max(6, Math.round(s * guiScale)));
        }
    }

    public static void invalidate() {
        for (IconAtlas a : CACHE.values()) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(a.id);
        }
        CACHE.clear();
    }
}
