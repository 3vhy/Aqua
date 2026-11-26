package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;

/**
 * Sneak (reference video): pressing the sneak key switches sneaking on and off instead of holding
 * it. Uses Minecraft's own "Sneak: Toggle" option, and puts it back when switched off.
 */
public final class ToggleSneak extends Module {
    private Boolean before;

    public ToggleSneak() {
        super("toggle_sneak", "Sneak", "Toggle sneak: press the sneak key once instead of holding it.", Category.QOL, false,
                "toggle sneak", "sneak", "crouch");
        hideFromModuleList();
    }

    @Override
    protected void onEnable() {
        if (mc.options != null) {
            before = mc.options.getSneakToggled().getValue();
            mc.options.getSneakToggled().setValue(true);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) {
            mc.options.getSneakToggled().setValue(before != null ? before : false);
        }
        before = null;
    }
}
