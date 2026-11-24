package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.widgets.WatermarkWidget;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Aqua title screen: emblem + wordmark intro (inspired by R4's logo reveal), restrained animated
 * background, and buttons wired to the real vanilla screens.
 */
public final class AquaTitleScreen extends TitleScreen {
    private final Ui ui = new Ui();
    private final Anim intro = new Anim(0f, 900f);
    private final long openedAt = System.currentTimeMillis();
    private int focused = -1;
    private static boolean firstShown;
    /** One-shot request to show the vanilla title screen (consumed by the screen-swap mixin). */
    public static boolean vanillaRequested;
    private final boolean playIntro;

    private record Entry(String label, Icon icon, Runnable action) {}

    public AquaTitleScreen() {
        // Extends TitleScreen so that code checking "instanceof TitleScreen" keeps working.
        super(false, null);
        playIntro = !firstShown;
        firstShown = true;
        intro.snap(playIntro ? 0f : 1f).target(1f);
    }

    private Entry[] entries() {
        return new Entry[]{
                new Entry("Singleplayer", Icon.USER, () -> client.setScreen(new SelectWorldScreen(this))),
                new Entry("Multiplayer", Icon.GLOBE, () -> {
                    if (!client.isMultiplayerEnabled()) {
                        Aqua.notifications().warn("Multiplayer", "Multiplayer is disabled for this account");
                        return;
                    }
                    client.setScreen(client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
                }),
                new Entry("Options", Icon.GEAR, () -> client.setScreen(new OptionsScreen(this, client.options))),
                new Entry("Aqua Settings", Icon.DROP, () -> client.setScreen(new AquaMenuScreen(this))),
                new Entry("Quit Game", Icon.POWER, () -> client.scheduleStop())
        };
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        // Aqua draws its own controls; vanilla title widgets are intentionally not added.
    }

    @Override
    public void tick() {
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Theme t = Aqua.theme();
        float time = (System.currentTimeMillis() - openedAt) / 1000f;
        R2D.roundGradientV(ctx, 0, 0, width, height, 0, Colors.darken(t.background(), 0.2f), Colors.lerp(t.background(), t.panel(), 0.6f));
        // Slow drifting soft glows (cheap: three SDF shapes per frame).
        float motion = Aqua.ui().reducedMotion.on() ? 0f : 1f;
        for (int i = 0; i < 3; i++) {
            float phase = time * 0.05f * motion + i * 2.1f;
            float cx = width * (0.25f + 0.5f * (0.5f + 0.5f * (float) Math.sin(phase * 1.3f + i)));
            float cy = height * (0.3f + 0.4f * (0.5f + 0.5f * (float) Math.cos(phase + i * 0.7f)));
            float r = Math.min(width, height) * (0.18f + 0.05f * i);
            int c = i == 1 ? t.accent2() : t.accent();
            R2D.shadow(ctx, cx - r / 2f, cy - r / 2f, r, r, r / 2f, 60, Colors.withAlpha(c, 0.10f));
        }
        // Gentle wave lines along the bottom (matches the emblem's wave)
        for (int k = 0; k < 2; k++) {
            float base = height - 22 - k * 9;
            float prevX = 0;
            float prevY = base;
            for (int x = 0; x <= width; x += 6) {
                float y = base + (float) Math.sin(x * 0.02f + time * 0.6f * motion + k) * 3f;
                if (x > 0) {
                    R2D.line(ctx, prevX, prevY, x, y, 0.8f, Colors.withAlpha(t.accent(), 0.10f - k * 0.04f));
                }
                prevX = x;
                prevY = y;
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Theme t = Aqua.theme();
        ui.begin(ctx, mouseX, mouseY);
        float p = intro.get();
        float logoA = Anim.easeOutCubic(Anim.clamp01(p * 1.6f));
        float textA = Anim.easeOutCubic(Anim.clamp01((p - 0.25f) * 1.8f));
        float btnA = Anim.easeOutCubic(Anim.clamp01((p - 0.45f) * 2.2f));

        float cx = width / 2f;
        float top = Math.max(14, height * 0.17f);
        float es = Math.min(46, height * 0.13f) * (0.85f + 0.15f * logoA);
        R2D.shadow(ctx, cx - es / 2f, top + 4, es, es, es / 2f, 24, Colors.withAlpha(t.accent(), 0.22f * logoA));
        R2D.texture(ctx, WatermarkWidget.EMBLEM, cx - es / 2f, top, es, es, Colors.withAlpha(0xFFFFFF, Math.round(255 * logoA)));
        float wy = top + es + 6;
        String word = "AQUA";
        float size = Math.min(20, height * 0.06f);
        float spacing = size * 0.35f * (1.6f - 0.6f * textA);
        float ww = 0;
        for (char c : word.toCharArray()) {
            ww += AquaFont.width(String.valueOf(c), size, AquaFont.Weight.BOLD) + spacing;
        }
        ww -= spacing;
        float x = cx - ww / 2f;
        for (char c : word.toCharArray()) {
            String s = String.valueOf(c);
            R2D.text(ctx, s, x, wy, size, AquaFont.Weight.BOLD, Colors.withAlpha(t.text(), textA));
            x += AquaFont.width(s, size, AquaFont.Weight.BOLD) + spacing;
        }
        String sub = "C L I E N T   ·   1 . 2 1 . 1 1";
        R2D.textCentered(ctx, sub, cx, wy + size * 1.25f + 3, 4.8f, AquaFont.Weight.SEMIBOLD, Colors.withAlpha(t.accent(), textA));
        // R7: a short greeting (Name Protect's alias is used when it is on).
        String who = client != null && client.getSession() != null
                ? dev.aqua.client.module.qol.NameProtect.filter(client.getSession().getUsername()) : "";
        if (!who.isEmpty()) {
            R2D.textCentered(ctx, greeting() + ", " + who, cx, wy + size * 1.25f + 11, 5f, AquaFont.Weight.MEDIUM,
                    Colors.withAlpha(t.muted(), textA * 0.9f));
        }

        // Buttons
        Entry[] es2 = entries();
        float bw = 130;
        float bh = 15;
        float gap = 4;
        float by = Math.min(height - (bh + gap) * es2.length - 26, wy + size * 1.25f + 24);
        for (int i = 0; i < es2.length; i++) {
            Entry e = es2[i];
            float bx = cx - bw / 2f;
            float yy = by + i * (bh + gap) + (1f - btnA) * 8f;
            boolean hov = ui.hover(bx, yy, bw, bh) || focused == i;
            float ha = ui.anim("tb" + i, 140, 0).target(hov).get();
            boolean primary = i == 0;
            int fill = primary ? Colors.lerp(t.accent(), t.accent2(), ha * 0.3f)
                    : Colors.lerp(Colors.withAlpha(t.panel(), 0.85f), Colors.withAlpha(t.elevated(), 0.95f), ha);
            R2D.shadow(ctx, bx, yy, bw, bh, 5, 6, Colors.withAlpha(0x000000, 0.25f * btnA));
            R2D.round(ctx, bx, yy, bw, bh, 5, Colors.fade(fill, btnA));
            if (!primary) {
                R2D.outline(ctx, bx, yy, bw, bh, 5, 0.6f, Colors.fade(Colors.withAlpha(Colors.lerp(t.border(), t.accent(), ha), 0.8f), btnA));
            }
            int fg = primary ? t.onAccent() : t.text();
            R2D.icon(ctx, e.icon(), bx + 9, yy + bh / 2f - 3.5f, 7, Colors.fade(fg, btnA));
            R2D.textCentered(ctx, e.label(), cx + 3, yy + bh / 2f, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(fg, btnA));
            if (btnA > 0.5f && ui.clickedIn(bx, yy, bw, bh)) {
                UiSounds.click(1f);
                e.action().run();
                ui.end();
                return;
            }
        }

        // Small corner actions
        float ix = 6;
        float iy = height - 18;
        if (ui.iconButton("lang", ix, iy, 13, Icon.GLOBE, t.muted(), "Language")) {
            client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager()));
        }
        if (ui.iconButton("access", ix + 15, iy, 13, Icon.EYE, t.muted(), "Accessibility")) {
            client.setScreen(new AccessibilityOptionsScreen(this, client.options));
        }
        if (ui.iconButton("vanilla", ix + 30, iy, 13, Icon.LAYOUT, t.muted(), "Vanilla title screen")) {
            vanillaRequested = true;
            client.setScreen(new TitleScreen(false, null));
        }
        float tx = ix + 48;
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("modmenu")) {
            if (ui.iconButton("mods", ix + 45, iy, 13, Icon.BOX, t.muted(), "Mods")) {
                client.setScreen(dev.aqua.client.compat.ModMenuIntegration.modsScreen(this));
            }
            tx += 15;
        }
        R2D.text(ctx, "Minecraft " + SharedConstants.getGameVersion().name() + " • Fabric", tx, iy + 1, 4.8f,
                AquaFont.Weight.MEDIUM, Colors.withAlpha(t.muted(), 0.85f));
        R2D.text(ctx, "Not an official Minecraft product.", tx, iy + 7.5f, 4.4f, AquaFont.Weight.REGULAR,
                Colors.withAlpha(t.muted(), 0.6f));
        String right = "Aqua " + Aqua.VERSION;
        R2D.textRight(ctx, right, width - 7, height - 11, 5f, AquaFont.Weight.MEDIUM, Colors.withAlpha(t.muted(), 0.85f));

        ui.drawTooltip(width, height);
        ui.end();
        Aqua.notifications().render(ctx);
    }

    @Override
    public void removed() {
    }

    @Override
    public void onDisplayed() {
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        focused = -1;
        ui.onClick(click.button());
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        ui.onRelease(click.button());
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int n = entries().length;
        switch (input.key()) {
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_TAB -> {
                focused = (focused + 1 + n) % n;
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                focused = (focused - 1 + n) % n;
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (focused >= 0) {
                    entries()[focused].action().run();
                    return true;
                }
            }
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        return true;
    }

    private static String greeting() {
        int h = java.time.LocalTime.now().getHour();
        return h < 5 ? "Up late" : h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
    }
}
