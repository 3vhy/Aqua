package dev.aqua.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.aqua.client.gui.AquaMenuScreen;

/** Optional: Mod Menu's "Configure" button opens Aqua's menu. Only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
    /** Mod Menu's mod list (only called when Mod Menu is installed). */
    public static net.minecraft.client.gui.screen.Screen modsScreen(net.minecraft.client.gui.screen.Screen parent) {
        return ModMenuApi.createModsScreen(parent);
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return dev.aqua.client.gui.Menus::create;
    }
}
