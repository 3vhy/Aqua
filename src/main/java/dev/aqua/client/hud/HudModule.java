package dev.aqua.client.hud;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;

/**
 * HUD (reference video): Aqua's on-screen widgets as a whole (module list, coordinates and status
 * overlays). Switch it off to hide every Aqua HUD element at once; each widget keeps its own
 * on/off state. Layout and global HUD options are in the HUD tab.
 */
public final class HudModule extends Module {
    public HudModule() {
        super("hud", "HUD", "Module list, coordinates and status overlays.", Category.HUD, true, "hud", "overlay", "widgets");
        hideFromModuleList();
    }
}
