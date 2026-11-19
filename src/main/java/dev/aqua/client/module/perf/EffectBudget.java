package dev.aqua.client.module.perf;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.BoolSetting;

/** Shows and limits the cost of Aqua's own cosmetic effects. */
public final class EffectBudget extends Module {
    private final BoolSetting pauseInMenus = add(new BoolSetting("pause_menus", "Pause world effects behind menus", true)
            .describe("Skips Aqua world effects while a full-screen Aqua menu is open"));

    public EffectBudget() {
        super("effect_budget", "Effect Budget", "Limits Aqua's particles and effects (see Settings > Effects quality).",
                Category.PERFORMANCE, true, "performance", "particles", "budget", "quality");
        add(new ActionSetting("clear", "Active Aqua particles", "Clear now", Particles::clear));
        hideFromModuleList();
    }

    public boolean pauseInMenus() {
        return isEnabled() && pauseInMenus.on();
    }

    public String status() {
        return Particles.count() + " / " + Particles.budget() + " particles • quality " + Aqua.ui().quality.get().displayName();
    }
}
