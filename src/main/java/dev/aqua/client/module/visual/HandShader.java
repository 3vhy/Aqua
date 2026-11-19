package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * First-person arm styling (Fern "Hand Chams", R1/R2): keep your skin, tint it, or draw the arm as a
 * flat theme-coloured topographic shape. Only your own first-person arm is affected.
 */
public final class HandShader extends Module {
    public enum Style implements EnumSetting.Displayable {
        TINT("Tinted skin"), TOPOGRAPHIC("Topographic"), SOLID("Solid colour");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    public static final Identifier TOPO = Identifier.of("aqua", "dynamic/hand_topographic");
    public static final Identifier SOLID_TEX = Identifier.of("aqua", "dynamic/hand_solid");
    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.TOPOGRAPHIC));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFD6F4F7, false, false));
    private final NumberSetting opacity = add(new NumberSetting("opacity", "Opacity", 1.0, 0.2, 1.0, 0.05));
    private boolean texturesReady;

    public HandShader() {
        super("hand_shader", "Hand Shader", "Restyles your first-person arm.", Category.VISUALS, false,
                "hand chams", "arm", "hand colour");
    }

    public Style style() {
        return style.get();
    }

    public int tint() {
        int c = color.color();
        return Colors.withAlpha(style.is(Style.TINT) ? Colors.lerp(0xFFFFFFFF, c, 0.6f) : c, opacity.f());
    }

    /** Lazily builds the 64x64 pattern textures (render thread). */
    public Identifier texture() {
        if (!texturesReady) {
            texturesReady = true;
            NativeImage topo = new NativeImage(64, 64, false);
            NativeImage solid = new NativeImage(64, 64, false);
            for (int yy = 0; yy < 64; yy++) {
                for (int xx = 0; xx < 64; xx++) {
                    double v = Math.sin(xx * 0.21) * 1.3 + Math.cos(yy * 0.17 + xx * 0.05) * 1.1 + Math.sin((xx + yy) * 0.11) * 0.9;
                    double f = v * 2.2 - Math.floor(v * 2.2);
                    boolean line = f < 0.12 || f > 0.88;
                    topo.setColorArgb(xx, yy, line ? 0xFF1A2228 : 0xFFFFFFFF);
                    solid.setColorArgb(xx, yy, 0xFFFFFFFF);
                }
            }
            MinecraftClient.getInstance().getTextureManager().registerTexture(TOPO, new NativeImageBackedTexture(() -> "aqua_hand_topo", topo));
            MinecraftClient.getInstance().getTextureManager().registerTexture(SOLID_TEX, new NativeImageBackedTexture(() -> "aqua_hand_solid", solid));
        }
        return style.is(Style.TOPOGRAPHIC) ? TOPO : SOLID_TEX;
    }
}
