package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.config.UiSettings;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

/** Opens Aqua's menu in the style chosen under Settings (panels by default). */
public final class Menus {
    private Menus() {}

    public static Screen create(@Nullable Screen parent) {
        if (Aqua.ui() != null && Aqua.ui().menuStyle.get() == UiSettings.MenuStyle.CLASSIC) {
            return new AquaMenuScreen(parent);
        }
        return new ClickGuiScreen(parent);
    }

    /** True for either menu style. */
    public static boolean isMenu(@Nullable Screen s) {
        return s instanceof AquaMenuScreen || s instanceof ClickGuiScreen;
    }
}
