package dev.aqua.client.gui;

import dev.aqua.client.setting.Setting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;

/** A screen that can show Aqua's content pages (Wardrobe, Music, Themes, Profiles, Keybinds, Settings). */
public interface PageHost {
    Ui ui();

    /** Draws one setting with the matching control. */
    default void settingRow(DrawContext ctx, Theme t, String owner, Setting<?> s, float x, float y, float w, float a) {
        SettingRows.row(ui(), ctx, t, owner, s, x, y, w, a);
    }
}
