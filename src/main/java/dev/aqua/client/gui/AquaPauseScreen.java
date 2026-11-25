package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.hud.widgets.WatermarkWidget;
import dev.aqua.client.module.Module;
import dev.aqua.client.music.MusicPlayer;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import dev.aqua.client.util.Session;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.toast.NowPlayingToast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aqua's pause menu. It is the vanilla pause menu underneath: vanilla builds its buttons (and other
 * mods add theirs, such as Mod Menu's "Mods"), and those same buttons keep their actions, keyboard
 * navigation and narration. Aqua only moves them onto a glass card and draws them in its own
 * style, then adds an Aqua Settings button, your starred modules as quick toggles, the session time
 * and what is playing.
 *
 * <p>It extends {@link GameMenuScreen}, so code that checks for the pause menu keeps working.
 */
public final class AquaPauseScreen extends GameMenuScreen {
    private static final float PAD = 10;
    private static final float HEADER = 30;

    private final Ui ui = new Ui();
    private final Anim open = new Anim(0f, 200f);
    private final List<ClickableWidget> buttons = new ArrayList<>();
    private ClickableWidget primary;
    private ClickableWidget exit;
    private boolean laidOut;
    private float cardX, cardY, cardW, cardH;
    private float aquaY, quickY;
    private List<Module> quick = List.of();

    public AquaPauseScreen(boolean showMenu) {
        super(showMenu);
        open.target(1f);
    }

    @Override
    protected void init() {
        super.init();
        // Other mods add their buttons after init, so the layout is made on the first frame.
        laidOut = false;
    }

    /** The vanilla buttons on this screen (used by the in-game tests). */
    public List<ClickableWidget> menuButtons() {
        if (!laidOut) {
            layout();
        }
        return buttons;
    }

    private void layout() {
        laidOut = true;
        buttons.clear();
        for (Element e : children()) {
            if (e instanceof ClickableWidget w && !(e instanceof TextWidget)) {
                buttons.add(w);
            } else if (e instanceof TextWidget tw) {
                tw.visible = false;
            }
        }
        quick = new ArrayList<>();
        for (Module m : Aqua.modules().all()) {
            if (Aqua.ui().isStarred(m.id())) {
                quick.add(m);
            }
        }
        if (buttons.isEmpty()) {
            return;
        }
        buttons.sort(Comparator.comparingInt(ClickableWidget::getY).thenComparingInt(ClickableWidget::getX));
        primary = buttons.get(0);
        exit = buttons.get(buttons.size() - 1);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (ClickableWidget w : buttons) {
            minX = Math.min(minX, w.getX());
            minY = Math.min(minY, w.getY());
            maxX = Math.max(maxX, w.getX() + w.getWidth());
            maxY = Math.max(maxY, w.getY() + w.getHeight());
        }
        float gridW = maxX - minX;
        float gridH = maxY - minY;
        float rowGap = 24;
        float quickH = quickRowsHeight(gridW);
        cardW = gridW + PAD * 2;
        cardH = PAD + HEADER + gridH + rowGap + quickH + PAD;
        cardX = Math.round((width - cardW) / 2f);
        cardY = Math.round(Math.max(6, (height - cardH) / 2f - 6));
        int dx = Math.round(cardX + PAD - minX);
        int dy = Math.round(cardY + PAD + HEADER - minY);
        for (ClickableWidget w : buttons) {
            w.setX(w.getX() + dx);
            // Everything under the first row moves down one row to make room for Aqua Settings.
            w.setY(w.getY() + dy + (w.getY() > minY ? (int) rowGap : 0));
        }
        aquaY = primary.getY() + primary.getHeight() + 4;
        quickY = cardY + PAD + HEADER + gridH + rowGap + 6;
    }

    private float quickRowsHeight(float w) {
        if (quick.isEmpty()) {
            return 14;
        }
        int rows = 1;
        float x = 0;
        for (Module m : quick) {
            float cw = chipWidth(m);
            if (x > 0 && x + cw > w) {
                rows++;
                x = 0;
            }
            x += cw + 4;
        }
        return 12 + Math.min(3, rows) * 15;
    }

    private static float chipWidth(Module m) {
        return R2D.width(m.name(), 5.2f, AquaFont.Weight.SEMIBOLD) + 18;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!shouldShowMenu()) {
            return;
        }
        boolean glass = Glass.on();
        ctx.fill(0, 0, width, height, Colors.withAlpha(0x05080B, Math.round((glass ? 60 : 110) * open.get())));
        Glass.backdrop(ctx, glass ? Aqua.ui().blur.on() : client.options.getMenuBackgroundBlurrinessValue() > 0);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!laidOut) {
            layout();
        }
        Theme t = Aqua.theme();
        if (!shouldShowMenu()) {
            String s = getTitle().getString();
            float w = R2D.width(s, 6f, AquaFont.Weight.SEMIBOLD) + 16;
            Glass.toast(ctx, width / 2f - w / 2f, 6, w, 15, 7.5f, 1f);
            R2D.textCentered(ctx, s, width / 2f, 13.5f, 6f, AquaFont.Weight.SEMIBOLD, t.text());
            return;
        }
        ui.begin(ctx, mouseX, mouseY);
        float a = Anim.easeOutCubic(open.get());
        Glass.window(ctx, cardX, cardY, cardW, cardH, 12, a);

        // Header: emblem, title and what you are playing
        float hx = cardX + PAD;
        float hy = cardY + PAD;
        R2D.shadow(ctx, hx, hy + 1, 15, 15, 7.5f, 9, Colors.withAlpha(t.accent(), 0.3f * a));
        R2D.texture(ctx, WatermarkWidget.EMBLEM, hx, hy + 1, 15, 15, Colors.withAlpha(0xFFFFFF, Math.round(255 * a)));
        R2D.text(ctx, getTitle().getString(), hx + 20, hy + 1, 8.5f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
        R2D.text(ctx, subtitle(), hx + 20, hy + 12, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        String time = Session.durationMs() >= 0 ? Session.format(Session.durationMs()) : "";
        if (!time.isEmpty()) {
            float tw = R2D.width(time, 5.2f, AquaFont.Weight.SEMIBOLD);
            R2D.icon(ctx, Icon.CLOCK, cardX + cardW - PAD - tw - 9, hy + 3, 6.5f, Colors.fade(t.muted(), a));
            R2D.text(ctx, time, cardX + cardW - PAD - tw, hy + 3.5f, 5.2f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
        }
        Glass.divider(ctx, cardX + PAD, cardY + PAD + HEADER - 7, cardW - PAD * 2, a);

        // The vanilla (and modded) buttons, in Aqua's style
        for (ClickableWidget w : buttons) {
            if (!w.visible) {
                continue;
            }
            drawButton(ctx, t, w, a);
        }
        // Aqua Settings, under Back to Game
        if (primary != null) {
            float bx = primary.getX(), bw = primary.getWidth(), bh = 20;
            boolean hov = ui.hover(bx, aquaY, bw, bh);
            float ha = ui.anim("aqua_settings", 120, 0).target(hov).get();
            Glass.chip(ctx, bx, aquaY, bw, bh, 6, a, ha);
            float lw = R2D.width("Aqua Settings", 6f, AquaFont.Weight.SEMIBOLD) + 10;
            R2D.icon(ctx, Icon.DROP, bx + bw / 2f - lw / 2f, aquaY + bh / 2f - 3.5f, 7, Colors.fade(t.accent(), a));
            R2D.textMid(ctx, "Aqua Settings", bx + bw / 2f - lw / 2f + 10, aquaY + bh / 2f, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            if (ui.clickedIn(bx, aquaY, bw, bh)) {
                UiSounds.click(1f);
                client.setScreen(new AquaMenuScreen(this));
                ui.end();
                return;
            }
        }
        quickToggles(ctx, t, a);
        nowPlaying(ctx, t, a);
        ui.drawTooltip(width, height);
        ui.end();
        if (shouldShowNowPlayingToast()) {
            NowPlayingToast.draw(ctx, textRenderer);
        }
    }

    private String subtitle() {
        if (client.isInSingleplayer() && client.getServer() != null) {
            return "Singleplayer • " + client.getServer().getSaveProperties().getLevelName();
        }
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().isRealm()) {
            return "Realms";
        }
        return "Multiplayer";
    }

    private void drawButton(DrawContext ctx, Theme t, ClickableWidget w, float a) {
        float x = w.getX(), y = w.getY(), bw = w.getWidth(), bh = w.getHeight();
        boolean active = w.active;
        boolean hov = active && ui.hover(x, y, bw, bh);
        float ha = ui.anim("pb" + System.identityHashCode(w), 120, 0).target(hov).get();
        float fade = a * (active ? 1f : 0.45f);
        if (w == primary) {
            Glass.accent(ctx, x, y, bw, bh, 6, fade, ha);
        } else {
            Glass.chip(ctx, x, y, bw, bh, 6, fade, ha);
            if (w == exit && ha > 0.01f) {
                R2D.round(ctx, x, y, bw, bh, 6, Colors.withAlpha(0xFFFF5A5F, 0.18f * ha * a));
                R2D.outline(ctx, x, y, bw, bh, 6, 0.6f, Colors.withAlpha(0xFFFF5A5F, 0.55f * ha * a));
            }
        }
        if (w.isFocused()) {
            R2D.outline(ctx, x - 1, y - 1, bw + 2, bh + 2, 7, 0.8f, Colors.fade(t.accent(), a));
        }
        int fg = w == primary ? t.onAccent() : (w == exit && ha > 0.01f ? Colors.lerp(t.text(), 0xFFFFB3B5, ha) : t.text());
        String label = AquaFont.ellipsize(w.getMessage().getString(), bw - 8, 6f, AquaFont.Weight.SEMIBOLD);
        R2D.textCentered(ctx, label, x + bw / 2f, y + bh / 2f, 6f, AquaFont.Weight.SEMIBOLD, Colors.fade(fg, fade));
    }

    private void quickToggles(DrawContext ctx, Theme t, float a) {
        float x0 = cardX + PAD;
        float w = cardW - PAD * 2;
        R2D.textMid(ctx, "QUICK TOGGLES", x0, quickY + 2, 4.4f, AquaFont.Weight.BOLD, Colors.fade(Colors.withAlpha(t.muted(), 0.8f), a));
        if (quick.isEmpty()) {
            R2D.textRight(ctx, "Star modules in Aqua Settings to see them here", x0 + w, quickY + 2, 4.6f, AquaFont.Weight.MEDIUM,
                    Colors.fade(t.muted(), a));
            return;
        }
        float x = x0;
        float y = quickY + 8;
        int row = 0;
        for (Module m : quick) {
            float cw = chipWidth(m);
            if (x > x0 && x + cw > x0 + w) {
                if (++row >= 3) {
                    break;
                }
                x = x0;
                y += 15;
            }
            boolean on = m.isEnabled();
            boolean hov = ui.hover(x, y, cw, 12);
            float ha = ui.anim("qt" + m.id(), 120, 0).target(hov).get();
            float en = ui.anim("qte" + m.id(), 160, on ? 1 : 0).target(on).get();
            Glass.chip(ctx, x, y, cw, 12, 6, a, ha);
            if (en > 0.01f) {
                R2D.round(ctx, x, y, cw, 12, 6, Colors.withAlpha(t.accent(), 0.2f * en * a));
                R2D.outline(ctx, x, y, cw, 12, 6, 0.6f, Colors.withAlpha(t.accent(), 0.6f * en * a));
            }
            R2D.circle(ctx, x + 7, y + 6, 2, Colors.fade(Colors.lerp(Colors.withAlpha(t.muted(), 0.6f), t.accent(), en), a));
            R2D.textMid(ctx, m.name(), x + 12, y + 6, 5.2f, AquaFont.Weight.SEMIBOLD, Colors.fade(Colors.lerp(t.muted(), t.text(), 0.4f + en * 0.6f), a));
            if (ui.clickedIn(x, y, cw, 12)) {
                m.toggle();
                UiSounds.toggle(!on);
            }
            x += cw + 4;
        }
    }

    /** A small glass card under the menu with the song that is playing (Aqua music or Spotify). */
    private void nowPlaying(DrawContext ctx, Theme t, float a) {
        MusicPlayer mp = MusicPlayer.get();
        if (mp == null || mp.current() == null) {
            return;
        }
        var tr = mp.current();
        float w = cardW;
        float h = 24;
        float x = cardX;
        float y = cardY + cardH + 6;
        if (y + h > height - 2) {
            return;
        }
        Glass.toast(ctx, x, y, w, h, 9, a);
        var cover = mp.cover();
        if (cover != null) {
            R2D.texture(ctx, cover, x + 5, y + 4, 16, 16, Colors.withAlpha(0xFFFFFF, Math.round(255 * a)));
        } else {
            R2D.roundGradientV(ctx, x + 5, y + 4, 16, 16, 4, Colors.fade(t.accent(), a), Colors.fade(t.elevated(), a));
            R2D.icon(ctx, Icon.NOTE, x + 9, y + 8, 8, Colors.fade(t.text(), a));
        }
        float tx = x + 26;
        float room = w - 26 - 30;
        R2D.text(ctx, AquaFont.ellipsize(tr.title(), room, 5.6f, AquaFont.Weight.SEMIBOLD), tx, y + 5, 5.6f, AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
        String artist = tr.artist().isEmpty() ? (mp.isExternal() ? mp.sourceApp() : "Unknown artist") : tr.artist();
        R2D.text(ctx, AquaFont.ellipsize(artist, room, 4.8f, AquaFont.Weight.MEDIUM), tx, y + 13.5f, 4.8f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
        if (ui.iconButton("pm_play", x + w - 24, y + 5, 14, mp.isPlaying() ? Icon.PAUSE : Icon.PLAY, t.text(), mp.isPlaying() ? "Pause" : "Play")) {
            mp.togglePause();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        ui.onClick(click.button());
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        ui.onRelease(click.button());
        return super.mouseReleased(click);
    }
}
