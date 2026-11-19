package dev.aqua.client.gui.pages;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.AquaMenuScreen;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.render.Icon;
import dev.aqua.client.setting.Setting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/** Client-wide interface options, HUD options and the global reset. */
public final class SettingsPage implements Page {
    private long confirmUntil;

    @Override
    public float render(Ui ui, DrawContext ctx, float x, float y, float w, float a, AquaMenuScreen menu) {
        Theme t = Aqua.theme();
        y = section(ui, ctx, t, "Interface", "Motion, blur, sounds and notifications", Aqua.ui().all(), "ui", x, y, w, a, menu);
        y = section(ui, ctx, t, "HUD", "Applies to every widget", Aqua.hud().settings(), "hud", x, y, w, a, menu);
        float h = 34;
        Page.card(ctx, t, x, y, w, h, a);
        Page.heading(ctx, t, "Reset", "Restore the active profile and interface options to defaults", x + 9, y + 6, a);
        boolean confirming = System.currentTimeMillis() < confirmUntil;
        if (ui.button("global_reset", x + w - 9 - 78, y + 11, 78, 12, confirming ? "Click to confirm" : "Reset everything",
                Icon.RESET, confirming)) {
            if (confirming) {
                for (Setting<?> s : Aqua.ui().all()) {
                    s.reset();
                }
                Aqua.config().resetActive();
                Aqua.notifications().success("Aqua", "Everything was reset to defaults");
                confirmUntil = 0;
            } else {
                confirmUntil = System.currentTimeMillis() + 3000;
            }
        }
        return y + h + 4;
    }

    private float section(Ui ui, DrawContext ctx, Theme t, String title, String sub, java.util.List<Setting<?>> settings, String owner,
                          float x, float y, float w, float a, AquaMenuScreen menu) {
        int n = 0;
        for (Setting<?> s : settings) {
            if (s.isVisible()) {
                n++;
            }
        }
        float h = 26 + n * 14 + 4;
        Page.card(ctx, t, x, y, w, h, a);
        Page.heading(ctx, t, title, sub, x + 9, y + 6, a);
        float ry = y + 26;
        for (Setting<?> s : settings) {
            if (!s.isVisible()) {
                continue;
            }
            menu.settingRow(ctx, t, owner, s, x + 9, ry, w - 18, a);
            ry += 14;
        }
        return y + h + 4;
    }
}
