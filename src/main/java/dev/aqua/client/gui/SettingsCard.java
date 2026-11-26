package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.click.ClickStyle;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.Setting;
import dev.aqua.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The module settings card of the panel menu (opened with right-click), laid out like the
 * reference video: a back button, the module name and its switch, the description, then the
 * Settings section (Show in HUD, Toggle Message and the module's own settings, with Reset all), the
 * Keybind section and the Config section with Copy Config and Paste Config.
 */
final class SettingsCard {
    static final float W = 153;
    static final float H = 252;
    private static final float ROW = 15.5f;

    final Module module;
    private final Anim appear = new Anim(0f, 170f);
    private final Anim scroll = new Anim(0f, 120f);
    private float scrollTarget;
    private float contentH;
    private boolean closeRequested;
    @Nullable private Runnable afterFrame;

    /** Extra buttons for modules that have a page or editor of their own. */
    record Extra(String label, Icon icon, Runnable action) {}

    private final List<Extra> extras;

    SettingsCard(Module module, List<Extra> extras) {
        this.module = module;
        this.extras = extras;
        appear.target(1f);
    }

    void requestClose() {
        closeRequested = true;
        appear.target(0f);
    }

    boolean closed() {
        return closeRequested && appear.get() < 0.02f;
    }

    /** Draws the card centred at (cx, cy); returns an action to run after the frame, if any. */
    @Nullable
    Runnable render(Ui ui, DrawContext ctx, float cx, float cy, float alpha) {
        afterFrame = null;
        float ap = Anim.easeOutCubic(appear.get());
        float a = alpha * ap;
        float x = cx - W / 2f;
        float y = cy - H / 2f + (1f - ap) * 8f;
        int card = ClickStyle.card();
        R2D.shadow(ctx, x, y + 3, W, H, 9, 18, Colors.withAlpha(0x000000, 0.45f * a));
        R2D.round(ctx, x, y, W, H, 9, Colors.fade(card, a));

        // Header: back, title, switch
        boolean backHov = ui.hover(x + 5.8f, y + 5.8f, 12.4f, 12.4f);
        R2D.circle(ctx, x + 12, y + 12, 6.2f, Colors.withAlpha(Colors.lerp(0xFF0A0E26, 0xFF30365F, backHov ? 1f : 0f), 0.75f * a));
        R2D.icon(ctx, Icon.CHEVRON_LEFT, x + 8.6f, y + 8.6f, 6.8f, Colors.fade(ClickStyle.CARD_TEXT, a));
        if (ui.clickedIn(x + 5.8f, y + 5.8f, 12.4f, 12.4f)) {
            UiSounds.click(0.9f);
            requestClose();
        }
        R2D.textMid(ctx, AquaFont.ellipsize(module.name(), W - 50, 7.2f, AquaFont.Weight.SEMIBOLD), x + 22, y + 12, 7.2f,
                AquaFont.Weight.SEMIBOLD, Colors.fade(ClickStyle.CARD_TEXT, a));
        boolean on = cardSwitch(ui, ctx, "card_on", x + W - 23, y + 8.25f, module.isEnabled(), a);
        if (on != module.isEnabled()) {
            module.setEnabled(on, false);
        }
        List<String> desc = wrap(module.description(), W - 16, 4.9f, AquaFont.Weight.MEDIUM);
        float dy = y + 22.5f;
        for (String line : desc) {
            R2D.textMid(ctx, line, x + 8, dy, 4.9f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.cardMuted(), a));
            dy += 6.4f;
        }
        float divY = dy + 1.5f;
        R2D.rect(ctx, x + 8, divY, W - 16, 0.5f, Colors.fade(ClickStyle.CARD_LINE, a));

        // Scrollable body
        float top = divY + 2;
        float bottom = y + H - 8;
        float viewH = bottom - top;
        float scrolled = scroll.target(scrollTarget).get();
        ui.pushClip(x + 2, top, W - 4, viewH);
        float cy2 = top + 3 - scrolled;
        float lx = x + 8;
        float rw = W - 16;

        // Settings section
        R2D.textMid(ctx, "Settings", lx, cy2 + 5, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.cardMuted(), a));
        if (chip(ui, ctx, "card_reset", x + W - 8 - 24, cy2 + 1, 24, 8, "Reset all", a)) {
            module.resetToDefaults();
            Aqua.notifications().info(module.name(), "Settings reset to defaults");
        }
        cy2 += 10;
        boolean hud = cardSwitch(ui, ctx, "card_hud", lx + rw - 15, rowControlY(cy2), module.showInModuleList(), a);
        rowLabel(ctx, "Show in HUD", lx, cy2, module.showInModuleList() != module.defaultShowInModuleList(), a);
        if (hud != module.showInModuleList()) {
            module.setShowInModuleList(hud);
        }
        cy2 = rowDivider(ctx, lx, rw, cy2, a);
        boolean msg = cardSwitch(ui, ctx, "card_msg", lx + rw - 15, rowControlY(cy2), module.toggleMessage(), a);
        rowLabel(ctx, "Toggle Message", lx, cy2, !module.toggleMessage(), a);
        if (msg != module.toggleMessage()) {
            module.setToggleMessage(msg);
        }
        cy2 = rowDivider(ctx, lx, rw, cy2, a);
        for (Setting<?> s : module.settings()) {
            if (!s.isVisible()) {
                continue;
            }
            cy2 = settingRow(ui, ctx, s, lx, cy2, rw, a);
        }
        for (Extra e : extras) {
            if (darkButton(ui, ctx, "card_x" + e.label(), lx, cy2 + 2, rw, 11.5f, e.label(), e.icon(), a)) {
                afterFrame = e.action();
            }
            cy2 += 15;
        }

        // Keybind section
        cy2 += 3;
        R2D.textMid(ctx, "Keybind", lx, cy2 + 5, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.cardMuted(), a));
        cy2 += 10;
        rowLabel(ctx, "Key", lx, cy2, module.bind().isBound() && !Objects.equals(module.bind().display(), module.defaultBind().display()), a);
        bindChip(ui, ctx, lx + rw - 46, rowControlY(cy2) - 1, 46, 9.5f, a);
        cy2 += ROW;

        // Config section
        cy2 += 3;
        R2D.textMid(ctx, "Config", lx, cy2 + 5, 4.6f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.cardMuted(), a));
        cy2 += 10;
        float bw = (rw - 4) / 2f;
        if (blueButton(ui, ctx, "card_copy", lx, cy2 + 1, bw, 12, "Copy Config", a)) {
            MinecraftClient.getInstance().keyboard.setClipboard(module.exportConfig());
            Aqua.notifications().info(module.name(), "Config copied to the clipboard");
        }
        if (darkButton(ui, ctx, "card_paste", lx + bw + 4, cy2 + 1, bw, 12, "Paste Config", null, a)) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (module.importConfig(clip)) {
                Aqua.notifications().info(module.name(), "Config pasted");
            } else {
                Aqua.notifications().warn(module.name(), "The clipboard does not hold a config for this module");
            }
        }
        cy2 += 16;
        contentH = cy2 + scrolled - top;
        ui.popClip();

        // Scrolling
        float max = Math.max(0, contentH - viewH);
        if (ui.scroll != 0 && ui.hover(x, top, W, viewH)) {
            scrollTarget -= ui.scroll * 20;
            ui.scroll = 0;
        }
        scrollTarget = Math.max(0, Math.min(max, scrollTarget));
        if (max > 0) {
            float th = Math.max(14, viewH * viewH / contentH);
            float ty = top + (viewH - th) * (scrolled / max);
            R2D.round(ctx, x + W - 4.2f, ty, 1.6f, th, 0.8f, Colors.fade(0x55FFFFFF, a));
        }
        // Clicks outside the card close it (popups and key capture first).
        if (ui.popup == null && ui.capturing == null && ui.clicked && !ui.hover(x, y, W, H)) {
            ui.clicked = false;
            requestClose();
        }
        return afterFrame;
    }

    private static float rowControlY(float rowY) {
        return rowY + ROW / 2f - 3.75f;
    }

    private static void rowLabel(DrawContext ctx, String label, float x, float y, boolean changed, float a) {
        if (changed) {
            R2D.circle(ctx, x - 3.6f, y + ROW / 2f, 1.1f, Colors.fade(ClickStyle.blue(), a));
        }
        R2D.textMid(ctx, AquaFont.ellipsize(label, 70, 5.5f, AquaFont.Weight.MEDIUM), x, y + ROW / 2f, 5.5f, AquaFont.Weight.MEDIUM,
                Colors.fade(ClickStyle.CARD_TEXT, a));
    }

    private static float rowDivider(DrawContext ctx, float x, float w, float y, float a) {
        R2D.rect(ctx, x, y + ROW - 0.25f, w, 0.5f, Colors.fade(ClickStyle.CARD_LINE, a));
        return y + ROW;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private float settingRow(Ui ui, DrawContext ctx, Setting<?> s, float x, float y, float w, float a) {
        String id = "card." + module.id() + "." + s.id();
        boolean changed = !Objects.equals(s.get(), s.defaultValue());
        if (s instanceof ActionSetting act) {
            if (darkButton(ui, ctx, id, x, y + 2, w, 11.5f, act.label(), null, a)) {
                act.run();
            }
            return y + 15;
        }
        rowLabel(ctx, s.name(), x, y, changed, a);
        if (ui.hover(x, y, 70, ROW) && !s.description().isEmpty()) {
            ui.tooltip(s.description());
        }
        float right = x + w;
        float cy = y + ROW / 2f;
        if (s instanceof BoolSetting b) {
            boolean nv = cardSwitch(ui, ctx, id, right - 15, cy - 3.75f, b.on(), a);
            if (nv != b.on()) {
                b.set(nv);
            }
        } else if (s instanceof NumberSetting n) {
            String v = n.format();
            float vw = R2D.width(v, 4.8f, AquaFont.Weight.MEDIUM);
            R2D.textRight(ctx, v, right, cy, 4.8f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.cardMuted(), a));
            float sw = Math.max(26, 60 - Math.max(0, vw - 14));
            cardSlider(ui, ctx, id, right - vw - 4 - sw, cy, sw, n, a);
        } else if (s instanceof EnumSetting e) {
            cardDropdown(ui, ctx, id, right - 62, cy - 5, 62, 10, e, a);
        } else if (s instanceof ColorSetting c) {
            float sx = right - 18, sy = cy - 4.25f;
            boolean hov = ui.hover(sx, sy, 18, 8.5f);
            R2D.round(ctx, sx, sy, 18, 8.5f, 3, Colors.fade(ClickStyle.CARD_DARK, a));
            R2D.round(ctx, sx + 1.5f, sy + 1.5f, 15, 5.5f, 2, Colors.fade(c.color() | 0xFF000000, a));
            R2D.outline(ctx, sx, sy, 18, 8.5f, 3, 0.5f, Colors.withAlpha(0xFFFFFF, (hov ? 0.5f : 0.18f) * a));
            if (ui.clickedIn(sx, sy, 18, 8.5f)) {
                UiSounds.click(1f);
                ui.popup = new Ui.ColorPopup(sx + 18 - 110, sy + 10, c, sx, sy, 18, 8.5f);
            }
        } else if (s instanceof dev.aqua.client.setting.KeySetting k) {
            keyChip(ui, ctx, id, k, right - 46, cy - 4.75f, 46, 9.5f, a);
        } else if (s instanceof StringSetting str) {
            float fw = Math.min(78, w * 0.55f);
            float fx = right - fw, fy = cy - 5;
            boolean focus = id.equals(ui.focus);
            R2D.round(ctx, fx, fy, fw, 10, 3, Colors.fade(ClickStyle.CARD_DARK, a));
            R2D.outline(ctx, fx, fy, fw, 10, 3, 0.5f, Colors.withAlpha(focus ? ClickStyle.blue() : 0x30FFFFFF, a));
            ui.bareFields = true;
            ui.textField(id, fx, fy, fw, 10, str.get(), "", str.maxLength(), null, str::set, str::set);
            ui.bareFields = false;
        }
        return rowDivider(ctx, x, w, y, a);
    }

    // ------------------------------------------------------------------ controls in the card's style

    static boolean cardSwitch(Ui ui, DrawContext ctx, String id, float x, float y, boolean on, float a) {
        float w = 15, h = 7.5f;
        boolean hov = ui.hover(x - 2, y - 2, w + 4, h + 4);
        float p = ui.anim(id + "#t", 150, on ? 1 : 0).target(on).get();
        int track = Colors.lerp(Colors.lerp(ClickStyle.cardTrack(), 0xFF3B4370, hov ? 0.25f : 0f), ClickStyle.blue(), p);
        R2D.round(ctx, x, y, w, h, h / 2f, Colors.fade(track, a));
        float k = h - 2.4f;
        float kx = x + 1.2f + (w - k - 2.4f) * Anim.easeInOutCubic(p);
        R2D.circle(ctx, kx + k / 2f, y + h / 2f, k / 2f, Colors.fade(0xFFFFFFFF, a));
        if (ui.clickedIn(x - 2, y - 2, w + 4, h + 4)) {
            UiSounds.toggle(!on);
            return !on;
        }
        return on;
    }

    private static void cardSlider(Ui ui, DrawContext ctx, String id, float x, float cy, float w, NumberSetting n, float a) {
        boolean hov = ui.hover(x - 2, cy - 4, w + 4, 8);
        if (ui.clicked && hov) {
            ui.active = id;
            ui.clicked = false;
        }
        boolean dragging = id.equals(ui.active) && ui.down;
        if (dragging) {
            n.setNormalized((ui.mx - x) / w);
        }
        if (hov && ui.scroll != 0 && ui.active == null) {
            n.set(n.get() + Math.signum(ui.scroll) * n.step());
            ui.scroll = 0;
        }
        float p = ui.anim(id + "#v", 80, (float) n.normalized()).target((float) n.normalized()).get();
        R2D.round(ctx, x, cy - 1.3f, w, 2.6f, 1.3f, Colors.fade(ClickStyle.cardTrack(), a));
        R2D.round(ctx, x, cy - 1.3f, Math.max(2.6f, w * p), 2.6f, 1.3f, Colors.fade(ClickStyle.blue(), a));
        float kr = 2.9f + (hov || dragging ? 0.5f : 0f);
        R2D.circle(ctx, x + w * p, cy, kr, Colors.fade(0xFFFFFFFF, a));
    }

    private static <E extends Enum<E>> void cardDropdown(Ui ui, DrawContext ctx, String id, float x, float y, float w, float h,
                                                         EnumSetting<E> s, float a) {
        boolean open = ui.popup instanceof CardDropdown<?> d && d.setting == s;
        boolean hov = ui.hover(x, y, w, h);
        R2D.round(ctx, x, y, w, h, 3, Colors.fade(ClickStyle.CARD_DARK, a));
        R2D.outline(ctx, x, y, w, h, 3, 0.6f, Colors.withAlpha(open ? ClickStyle.blue() : (hov ? 0x55FFFFFF : 0x30FFFFFF), a));
        R2D.textMid(ctx, AquaFont.ellipsize(EnumSetting.display(s.get()), w - 14, 4.9f, AquaFont.Weight.MEDIUM), x + 4.5f, y + h / 2f,
                4.9f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.CARD_TEXT, a));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x + w - 6.5f, y + h / 2f);
        if (open) {
            ctx.getMatrices().rotate((float) Math.PI);
        }
        R2D.icon(ctx, Icon.CHEVRON_DOWN, -2.7f, -2.7f, 5.4f, Colors.fade(ClickStyle.cardMuted(), a));
        ctx.getMatrices().popMatrix();
        if (ui.clickedIn(x, y, w, h)) {
            UiSounds.click(1f);
            ui.popup = open ? null : new CardDropdown<>(x, y + h + 1, w, s, x, y, w, h);
        }
    }

    private static boolean chip(Ui ui, DrawContext ctx, String id, float x, float y, float w, float h, String label, float a) {
        boolean hov = ui.hover(x, y, w, h);
        R2D.round(ctx, x, y, w, h, 2.8f, Colors.fade(Colors.lerp(ClickStyle.CARD_DARK, 0xFF2B3150, hov ? 1f : 0f), a));
        R2D.textCentered(ctx, label, x + w / 2f, y + h / 2f, 4.4f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.CARD_TEXT, a));
        if (ui.clickedIn(x, y, w, h)) {
            UiSounds.click(0.9f);
            return true;
        }
        return false;
    }

    static boolean darkButton(Ui ui, DrawContext ctx, String id, float x, float y, float w, float h, String label, @Nullable Icon icon, float a) {
        boolean hov = ui.hover(x, y, w, h);
        float ha = ui.anim(id + "#h", 110, 0).target(hov).get();
        R2D.round(ctx, x, y, w, h, 3.2f, Colors.fade(Colors.lerp(ClickStyle.CARD_DARK, 0xFF262C45, ha), a));
        float tw = R2D.width(label, 5.2f, AquaFont.Weight.MEDIUM) + (icon != null ? 8 : 0);
        float tx = x + (w - tw) / 2f;
        if (icon != null) {
            R2D.icon(ctx, icon, tx, y + h / 2f - 3, 6, Colors.fade(ClickStyle.CARD_TEXT, a));
            tx += 8;
        }
        R2D.textMid(ctx, label, tx, y + h / 2f, 5.2f, AquaFont.Weight.MEDIUM, Colors.fade(ClickStyle.CARD_TEXT, a));
        if (ui.clickedIn(x, y, w, h)) {
            UiSounds.click(1f);
            return true;
        }
        return false;
    }

    private static boolean blueButton(Ui ui, DrawContext ctx, String id, float x, float y, float w, float h, String label, float a) {
        boolean hov = ui.hover(x, y, w, h);
        float ha = ui.anim(id + "#h", 110, 0).target(hov).get();
        R2D.round(ctx, x, y, w, h, 3.2f, Colors.fade(Colors.lerp(ClickStyle.blue(), Colors.brighten(ClickStyle.blue(), 0.15f), ha), a));
        R2D.textCentered(ctx, label, x + w / 2f, y + h / 2f, 5.2f, AquaFont.Weight.MEDIUM, Colors.fade(0xFFFFFFFF, a));
        if (ui.clickedIn(x, y, w, h)) {
            UiSounds.click(1f);
            return true;
        }
        return false;
    }

    private void bindChip(Ui ui, DrawContext ctx, float x, float y, float w, float h, float a) {
        String id = "card_bind_" + module.id();
        boolean cap = id.equals(ui.capturing);
        boolean hov = ui.hover(x, y, w, h);
        R2D.round(ctx, x, y, w, h, 3, Colors.fade(Colors.lerp(ClickStyle.CARD_DARK, 0xFF262C45, hov ? 1f : 0f), a));
        if (cap) {
            R2D.outline(ctx, x, y, w, h, 3, 0.6f, Colors.fade(ClickStyle.blue(), a));
        }
        String text = cap ? "Press a key" : (module.bind().isBound() ? module.bind().display() : "None");
        R2D.textCentered(ctx, AquaFont.ellipsize(text, w - 6, 4.8f, AquaFont.Weight.MEDIUM), x + w / 2f, y + h / 2f, 4.8f,
                AquaFont.Weight.MEDIUM, Colors.fade(module.bind().isBound() || cap ? ClickStyle.CARD_TEXT : ClickStyle.cardMuted(), a));
        if (ui.clickedIn(x, y, w, h)) {
            UiSounds.click(1.1f);
            ui.startCapture(id, c -> {
                if (c.cancelled()) {
                    return;
                }
                if (c.cleared()) {
                    module.bind().clear();
                } else {
                    module.bind().set(c.type(), c.code());
                    List<String> conf = Aqua.keybinds().conflicts(module.bind(), module);
                    if (!conf.isEmpty()) {
                        Aqua.notifications().warn("Key conflict", module.bind().display() + " is also used by " + String.join(", ", conf));
                    }
                }
                Aqua.config().markDirty();
            });
        }
        if (ui.rightClickedIn(x, y, w, h)) {
            module.bind().clear();
            Aqua.config().markDirty();
        }
        if (hov && !cap) {
            ui.tooltip("Click to bind • right-click to clear");
        }
    }

    static void keyChip(Ui ui, DrawContext ctx, String id, dev.aqua.client.setting.KeySetting k, float x, float y, float w, float h, float a) {
        boolean cap = id.equals(ui.capturing);
        boolean hov = ui.hover(x, y, w, h);
        R2D.round(ctx, x, y, w, h, 3, Colors.fade(Colors.lerp(ClickStyle.CARD_DARK, 0xFF262C45, hov ? 1f : 0f), a));
        if (cap) {
            R2D.outline(ctx, x, y, w, h, 3, 0.6f, Colors.fade(ClickStyle.blue(), a));
        }
        boolean bound = k.keybind().isBound();
        R2D.textCentered(ctx, AquaFont.ellipsize(cap ? "Press a key" : k.display(), w - 6, 4.8f, AquaFont.Weight.MEDIUM), x + w / 2f, y + h / 2f,
                4.8f, AquaFont.Weight.MEDIUM, Colors.fade(bound || cap ? ClickStyle.CARD_TEXT : ClickStyle.cardMuted(), a));
        if (ui.clickedIn(x, y, w, h)) {
            UiSounds.click(1.1f);
            ui.startCapture(id, c -> {
                if (c.cancelled()) {
                    return;
                }
                if (c.cleared()) {
                    k.clear();
                } else {
                    k.setKey(c.type(), c.code());
                }
            });
        }
        if (ui.rightClickedIn(x, y, w, h)) {
            k.clear();
        }
    }

    /** Word-wraps text to a width. */
    static List<String> wrap(String text, float width, float size, AquaFont.Weight w) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String trial = line.isEmpty() ? word : line + " " + word;
            if (R2D.width(trial, size, w) > width && !line.isEmpty()) {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(trial);
            }
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    /** The card's dropdown list: dark, with the chosen value in blue and a tick. */
    static final class CardDropdown<E extends Enum<E>> extends Ui.Popup {
        final EnumSetting<E> setting;

        CardDropdown(float x, float y, float w, EnumSetting<E> s, float ax, float ay, float aw, float ah) {
            super(x, y, w, 3 + s.values().length * 10.5f, ax, ay, aw, ah);
            this.setting = s;
        }

        @Override
        void render(Ui ui, boolean hovered) {
            ui.inPopupRender = true;
            float a = appear.get();
            R2D.shadow(ui.ctx, x, y + 1.5f, w, h, 4, 8, Colors.withAlpha(0x000000, 0.45f * a));
            R2D.round(ui.ctx, x, y, w, h, 4, Colors.fade(0xFF161B26, a));
            E[] values = setting.values();
            for (int i = 0; i < values.length; i++) {
                float ry = y + 1.5f + i * 10.5f;
                boolean hov = ui.hover(x + 1.5f, ry, w - 3, 10.5f);
                boolean sel = values[i] == setting.get();
                if (hov) {
                    R2D.round(ui.ctx, x + 1.5f, ry, w - 3, 10.5f, 2.5f, Colors.fade(0x14FFFFFF, a));
                }
                R2D.textMid(ui.ctx, AquaFont.ellipsize(EnumSetting.display(values[i]), w - 16, 4.9f, AquaFont.Weight.MEDIUM), x + 5, ry + 5.25f,
                        4.9f, AquaFont.Weight.MEDIUM, Colors.fade(sel ? ClickStyle.blue() : ClickStyle.CARD_TEXT, a));
                if (sel) {
                    R2D.icon(ui.ctx, Icon.CHECK, x + w - 9.5f, ry + 2.5f, 5.5f, Colors.fade(ClickStyle.blue(), a));
                }
                if (ui.clickedIn(x + 1.5f, ry, w - 3, 10.5f)) {
                    setting.set(values[i]);
                    UiSounds.click(1.1f);
                    ui.popup = null;
                }
            }
            ui.inPopupRender = false;
        }
    }
}
