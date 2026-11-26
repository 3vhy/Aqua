package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;

/** Auto Jump (reference video): Minecraft's own auto-jump option, switched from the menu. */
public final class AutoJump extends Module {
    private Boolean before;

    public AutoJump() {
        super("auto_jump", "Auto Jump", "Minecraft's auto-jump: step up single blocks automatically while walking.", Category.QOL, false,
                "auto jump", "jump", "step");
        hideFromModuleList();
    }

    @Override
    protected void onEnable() {
        if (mc.options != null) {
            before = mc.options.getAutoJump().getValue();
            mc.options.getAutoJump().setValue(true);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) {
            mc.options.getAutoJump().setValue(before != null ? before : false);
        }
        before = null;
    }
}
