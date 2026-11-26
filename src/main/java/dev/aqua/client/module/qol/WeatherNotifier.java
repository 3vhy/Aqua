package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;

/** Weather Notifier (reference video): a toast when it starts or stops raining, or a storm begins. */
public final class WeatherNotifier extends Module {
    private final BoolSetting rainStart = add(new BoolSetting("rain_start", "Rain starts", true));
    private final BoolSetting rainStop = add(new BoolSetting("rain_stop", "Rain stops", true));
    private final BoolSetting thunder = add(new BoolSetting("thunder", "Thunderstorms", true));
    private Boolean wasRaining;
    private Boolean wasThunder;
    private int shown;

    public WeatherNotifier() {
        super("weather_notifier", "Weather Notifier", "Shows a toast when it starts raining.", Category.QOL, false,
                "weather", "rain", "notify", "thunder");
    }

    /** Toasts shown (used by the in-game tests). */
    public int shown() {
        return shown;
    }

    @Override
    public void onTick() {
        if (mc.world == null) {
            wasRaining = null;
            wasThunder = null;
            return;
        }
        boolean rain = mc.world.getRainGradient(1f) > 0.2f;
        boolean storm = mc.world.getThunderGradient(1f) > 0.6f;
        if (wasRaining != null) {
            if (rain && !wasRaining && rainStart.on()) {
                toast("It started raining.");
            } else if (!rain && wasRaining && rainStop.on()) {
                toast("It stopped raining.");
            }
            if (storm && !wasThunder && thunder.on()) {
                toast("A thunderstorm started.");
            }
        }
        wasRaining = rain;
        wasThunder = storm;
    }

    private void toast(String text) {
        shown++;
        Aqua.notifications().info("Weather Changed!", text);
    }

    @Override
    protected void onDisable() {
        wasRaining = null;
        wasThunder = null;
    }
}
