package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.util.Problems;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/**
 * Custom Glint (reference video): recolours the enchantment glint and changes its speed and pattern
 * size. The glint textures are replaced with tinted copies while the module is on and put back
 * when it is switched off.
 */
public final class CustomGlint extends Module {
    private static final Identifier[] GLINTS = {Identifier.ofVanilla("textures/misc/enchanted_glint_item.png"),
            Identifier.ofVanilla("textures/misc/enchanted_glint_armor.png")};
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true).onChange(c -> dirty = true));
    private final NumberSetting speed = add(new NumberSetting("speed", "Speed", 1.0, 0.1, 4.0, 0.05).unit("x"));
    private final NumberSetting size = add(new NumberSetting("size", "Pattern size", 1.0, 0.25, 4.0, 0.05).unit("x"));
    private boolean dirty = true;
    private int applied;
    private int lastColor;

    public CustomGlint() {
        super("custom_glint", "Custom Glint", "Recolours the enchantment glint and changes its speed and pattern size.", Category.VISUALS, false,
                "glint", "enchant", "enchantment", "colour", "color");
    }

    public float speedFactor() {
        return speed.f();
    }

    public float sizeFactor() {
        return 1f / size.f();
    }

    /** Times the tinted glint was uploaded (used by the in-game tests). */
    public int applied() {
        return applied;
    }

    @Override
    public void onTick() {
        int c = color.color();
        if (dirty || c != lastColor) {
            dirty = false;
            lastColor = c;
            tint(c);
        }
    }

    @Override
    protected void onEnable() {
        dirty = true;
    }

    @Override
    protected void onDisable() {
        for (Identifier id : GLINTS) {
            mc.getTextureManager().destroyTexture(id);
        }
        dirty = true;
    }

    private void tint(int argb) {
        float tr = ((argb >> 16) & 255) / 255f, tg = ((argb >> 8) & 255) / 255f, tb = (argb & 255) / 255f;
        for (Identifier id : GLINTS) {
            try (InputStream in = mc.getResourceManager().getResourceOrThrow(id).getInputStream()) {
                NativeImage img = NativeImage.read(in);
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int p = img.getColorArgb(x, y);
                        int a = (p >>> 24) & 255;
                        float l = (0.2126f * ((p >> 16) & 255) + 0.7152f * ((p >> 8) & 255) + 0.0722f * (p & 255)) / 255f;
                        l = Math.min(1f, l * 1.6f);
                        int r = Math.round(tr * l * 255), g = Math.round(tg * l * 255), b = Math.round(tb * l * 255);
                        img.setColorArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                    }
                }
                mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "aqua custom glint", img));
                applied++;
            } catch (Exception e) {
                Problems.report("Custom Glint could not tint " + id, e);
                setEnabled(false, false);
                return;
            }
        }
    }
}
