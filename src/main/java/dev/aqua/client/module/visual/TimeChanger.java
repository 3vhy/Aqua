package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;

/** Client-side visual time of day (R1 "Time Changer"). Does not affect the server. */
public final class TimeChanger extends Module {
    private final NumberSetting time = add(new NumberSetting("time", "Time of day", 6000, 0, 23999, 100)
            .describe("0 = sunrise, 6000 = noon, 12000 = sunset, 18000 = midnight"));
    private final BoolSetting cycle = add(new BoolSetting("cycle", "Animate", false));
    private final NumberSetting speed = add(new NumberSetting("speed", "Cycle speed", 20, 1, 400, 1).unit("x").visibleWhen(cycle::on));
    private double offset;

    public TimeChanger() {
        super("time_changer", "Time Changer", "Sets a visual time of day just for you.", Category.VISUALS, false,
                "time", "day", "night", "sunset");
    }

    @Override
    public void onTick() {
        if (cycle.on()) {
            offset = (offset + speed.d()) % 24000.0;
        } else {
            offset = 0;
        }
    }

    public long timeOfDay() {
        return (long) ((time.d() + offset) % 24000.0);
    }
}
