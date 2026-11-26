package dev.aqua.client.gui;

import dev.aqua.client.render.Colors;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.Setting;
import dev.aqua.client.setting.StringSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/** The setting row used by the classic menu and by content pages. */
public final class SettingRows {
    private SettingRows() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void row(Ui ui, DrawContext ctx, Theme t, String owner, Setting<?> s, float x, float y, float w, float a) {
        String id = owner + "." + s.id();
        boolean hovLabel = ui.hover(x, y, w * 0.5f, 12);
        R2D.textMid(ctx, s.name(), x, y + 6, 5.5f, AquaFont.Weight.MEDIUM, Colors.fade(t.text(), a));
        if (hovLabel && !s.description().isEmpty()) {
            ui.tooltip(s.description());
        }
        float right = x + w;
        if (s instanceof BoolSetting b) {
            boolean nv = ui.toggle(id, right - 17, y + 1.25f, b.on());
            if (nv != b.on()) {
                b.set(nv);
            }
        } else if (s instanceof NumberSetting n) {
            float sw = Math.min(90, w * 0.38f);
            R2D.textRight(ctx, n.format(), right, y + 6, 5f, AquaFont.Weight.MEDIUM, Colors.fade(t.muted(), a));
            ui.slider(id, right - sw - 30, y + 1.5f, sw, n);
        } else if (s instanceof EnumSetting e) {
            ui.dropdown(id, right - 86, y + 0.5f, 86, e);
        } else if (s instanceof ColorSetting c) {
            ui.colorButton(id, right - 22, y + 0.5f, c);
        } else if (s instanceof dev.aqua.client.setting.KeySetting k) {
            boolean bound = k.keybind().isBound();
            ui.keybindBadge(id, right - 46, y + 1, 46, k.display(), bound, false, c -> {
                if (c.cancelled()) {
                    return;
                }
                if (c.cleared()) {
                    k.clear();
                } else {
                    k.setKey(c.type(), c.code());
                }
            }, k::clear);
        } else if (s instanceof StringSetting str) {
            float fw = Math.min(150, w * 0.55f);
            ui.textField(id, right - fw, y, fw, 12, str.get(), "", str.maxLength(), null, str::set, str::set);
        } else if (s instanceof ActionSetting act) {
            float bw = Math.max(44, R2D.width(act.label(), 5.6f, AquaFont.Weight.SEMIBOLD) + 14);
            if (ui.button(id, right - bw, y + 0.5f, bw, 11, act.label(), null, false)) {
                act.run();
            }
        }
    }

}
