package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Replaces your username with an alias wherever text is drawn on your screen: chat, tab list,
 * scoreboard, nametags, menus and Aqua's HUD. Nothing is changed for other players.
 */
public final class NameProtect extends Module {
    private final StringSetting alias = add(new StringSetting("alias", "Shown name", "You", 16));
    private static volatile String realName;
    private static volatile String replacement;

    public NameProtect() {
        super("name_protect", "Name Protect", "Hides your username everywhere on your screen.", Category.QOL, false,
                "name protect", "hide name", "nick", "streamer", "privacy");
        alias.onChange(v -> refresh());
    }

    public String alias() {
        return alias.get().isBlank() ? "You" : alias.get();
    }

    @Override
    protected void onEnable() {
        refresh();
    }

    @Override
    protected void onDisable() {
        realName = null;
    }

    private void refresh() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (isEnabled() && mc.getSession() != null) {
            realName = mc.getSession().getUsername();
            replacement = alias();
        } else {
            realName = null;
        }
    }

    /** Called for every string the game draws (render thread only). Cheap when the name is absent. */
    public static String filter(String s) {
        String name = realName;
        if (name == null || s == null || s.isEmpty() || name.isEmpty() || s.indexOf(name) < 0) {
            return s;
        }
        if (!MinecraftClient.getInstance().isOnThread()) {
            return s;
        }
        return s.replace(name, replacement);
    }
}
