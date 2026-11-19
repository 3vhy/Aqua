package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.EnumSetting;

/** Client-side visual weather (R1 "Weather Changer"). */
public final class WeatherChanger extends Module {
    public enum Weather implements EnumSetting.Displayable {
        CLEAR("Clear"), RAIN("Rain"), THUNDER("Thunderstorm");
        final String l;

        Weather(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Weather> weather = add(new EnumSetting<>("weather", "Weather", Weather.CLEAR));

    public WeatherChanger() {
        super("weather_changer", "Weather Changer", "Shows the weather you choose instead of the server's.", Category.VISUALS, false,
                "weather", "rain", "clear", "thunder");
    }

    public float rain() {
        return weather.is(Weather.CLEAR) ? 0f : 1f;
    }

    public float thunder() {
        return weather.is(Weather.THUNDER) ? 1f : 0f;
    }
}
