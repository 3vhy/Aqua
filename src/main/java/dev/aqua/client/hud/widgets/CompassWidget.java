package dev.aqua.client.hud.widgets;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Heading compass. Adapted from the radar dials in R3/R4: it only shows which way you face,
 * never entities, players or blocks.
 */
public final class CompassWidget extends HudWidget {
    public enum Style implements EnumSetting.Displayable {
        DIAL("Dial"), STRIP("Strip");
        final String l;

        Style(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", Style.DIAL));

    public CompassWidget() {
        super("compass", "Compass", "Shows the direction you are facing.", Icon.COMPASS, false, Anchor.TOP_CENTER, 0, 22,
                "direction", "heading", "north");
    }

    private float yaw(float tickDelta) {
        return mc.player != null ? MathHelper.wrapDegrees(mc.player.getYaw(tickDelta)) : 30f;
    }

    @Override
    public float width(boolean preview) {
        return style.is(Style.DIAL) ? 44 : 120;
    }

    @Override
    public float height(boolean preview) {
        return style.is(Style.DIAL) ? 44 : 16;
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        Theme t = Aqua.theme();
        float yaw = yaw(mc.getRenderTickCounter().getTickProgress(true));
        if (style.is(Style.DIAL)) {
            float r = 22;
            if (showBackground()) {
                R2D.circle(ctx, r, r, r, bg(alpha));
            }
            R2D.ring(ctx, r, r, r - 7, 0.6f, Colors.fade(t.border(), alpha));
            String[] names = {"S", "W", "N", "E"};
            for (int i = 0; i < 4; i++) {
                // Minecraft yaw: 0 = south, 90 = west, 180 = north, -90 = east.
                double a = Math.toRadians(i * 90 - yaw - 90 + 180);
                float x = r + (float) Math.cos(a) * (r - 6.5f);
                float y = r + (float) Math.sin(a) * (r - 6.5f);
                int c = "N".equals(names[i]) ? t.accent() : t.text();
                R2D.textCentered(ctx, names[i], x, y, 5.2f, AquaFont.Weight.SEMIBOLD, Colors.fade(c, alpha));
            }
            R2D.line(ctx, r, r, r, r - 8, 1.1f, Colors.fade(t.accent(), alpha));
            R2D.circle(ctx, r, r, 1.6f, Colors.fade(t.accent(), alpha));
        } else {
            float w = width(preview);
            if (showBackground()) {
                R2D.round(ctx, 0, 0, w, 16, 5, bg(alpha));
            }
            R2D.pushClip(ctx, 2, 0, w - 4, 16);
            float pxPerDeg = 1.1f;
            String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
            for (int i = -16; i <= 16; i++) {
                float deg = Math.round(yaw / 45f) * 45f + i * 45f;
                float dx = (deg - yaw) * pxPerDeg;
                float x = w / 2f + dx;
                if (x < -10 || x > w + 10) {
                    continue;
                }
                int idx = Math.floorMod(Math.round(deg / 45f), 8);
                String n = names[idx];
                float fade = 1f - Math.min(1f, Math.abs(dx) / (w / 2f));
                int c = "N".equals(n) ? t.accent() : t.text();
                R2D.textCentered(ctx, n, x, 8, n.length() > 1 ? 4.6f : 5.6f, AquaFont.Weight.SEMIBOLD, Colors.fade(c, alpha * fade));
            }
            R2D.popClip(ctx);
            R2D.round(ctx, w / 2f - 0.5f, 1.5f, 1, 2.5f, 0.5f, Colors.fade(t.accent(), alpha));
        }
    }
}
