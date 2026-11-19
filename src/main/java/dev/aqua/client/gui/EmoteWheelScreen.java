package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Emotes;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Radial emote picker. Click a segment, press 1-6, or Escape to close. */
public final class EmoteWheelScreen extends Screen {
    private final Anim open = new Anim(0f, 160f);

    public EmoteWheelScreen() {
        super(Text.literal("Emotes"));
        open.target(1f);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, Colors.withAlpha(0x000000, Math.round(70 * open.get())));
    }

    private int hovered(double mx, double my) {
        float cx = width / 2f, cy = height / 2f;
        double dx = mx - cx, dy = my - cy;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 18 || d > 80) {
            return -1;
        }
        int n = Emotes.Emote.values().length;
        double a = (Math.atan2(dy, dx) + Math.PI / 2 + Math.PI * 2 + Math.PI / n) % (Math.PI * 2);
        return (int) (a / (Math.PI * 2) * n);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Theme t = Aqua.theme();
        float a = Anim.easeOutCubic(open.get());
        float cx = width / 2f, cy = height / 2f;
        float r = 62 * (0.9f + 0.1f * a);
        R2D.shadow(ctx, cx - r, cy - r, r * 2, r * 2, r, 18, Colors.withAlpha(0x000000, 0.4f * a));
        R2D.circle(ctx, cx, cy, r, Colors.fade(t.windowBg(0.9f), a));
        R2D.ring(ctx, cx, cy, r, 0.7f, Colors.fade(t.border(), a));
        R2D.circle(ctx, cx, cy, 17, Colors.fade(Colors.withAlpha(t.panel(), 0.95f), a));
        Emotes.Emote[] all = Emotes.Emote.values();
        int hov = hovered(mouseX, mouseY);
        for (int i = 0; i < all.length; i++) {
            double ang = -Math.PI / 2 + i * Math.PI * 2 / all.length;
            float ex = cx + (float) Math.cos(ang) * r * 0.66f;
            float ey = cy + (float) Math.sin(ang) * r * 0.66f;
            boolean h = i == hov;
            if (h) {
                R2D.circle(ctx, ex, ey, 17, Colors.fade(Colors.withAlpha(t.accent(), 0.25f), a));
            }
            R2D.textCentered(ctx, all[i].label, ex, ey - 2, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(h ? t.accent() : t.text(), a));
            R2D.textCentered(ctx, String.valueOf(i + 1), ex, ey + 6, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        }
        R2D.icon(ctx, Icon.SMILE, cx - 5, cy - 5, 10, Colors.fade(t.accent(), a));
        R2D.textCentered(ctx, "Only you can see Aqua emotes", cx, cy + r + 10, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
    }

    private void choose(int i) {
        if (i >= 0 && i < Emotes.Emote.values().length) {
            Emotes.play(Emotes.Emote.values()[i]);
            UiSounds.click(1.2f);
        }
        close();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int h = hovered(click.x(), click.y());
        if (h >= 0) {
            choose(h);
        } else {
            close();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (k >= GLFW.GLFW_KEY_1 && k <= GLFW.GLFW_KEY_6) {
            choose(k - GLFW.GLFW_KEY_1);
            return true;
        }
        if (Aqua.keybinds().emoteWheel.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }
}
