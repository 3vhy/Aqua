package dev.aqua.client.module.visual;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.SkyDome;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.world.World;

/** Topographic contour sky (Fern R1/R2) or a smooth gradient sky (R3/R4). Purely visual. */
public final class CustomSky extends Module implements WorldEffect {
    public enum Mode implements EnumSetting.Displayable {
        TOPOGRAPHIC("Topographic"), GRADIENT("Gradient");
        final String l;

        Mode(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("mode", "Style", Mode.TOPOGRAPHIC));
    private final ColorSetting sky = add(new ColorSetting("sky", "Sky colour", 0xFFBDEFF4, false, false));
    private final ColorSetting horizon = add(new ColorSetting("horizon", "Horizon colour", 0xFF6FD3E2, false, false));
    private final ColorSetting lines = add(new ColorSetting("lines", "Contour colour", 0xD9101A20, true, false)
            .visibleWhen(() -> isTopo()));
    private final BoolSetting dayNight = add(new BoolSetting("day_night", "Darken at night", true));
    private final NumberSetting opacity = add(new NumberSetting("opacity", "Opacity", 1.0, 0.3, 1.0, 0.05)
            .describe("Lower values let the vanilla sun, moon and stars show through"));
    private final BoolSetting matchFog = add(new BoolSetting("match_fog", "Match fog to horizon", true));

    public CustomSky() {
        super("custom_sky", "Custom Sky", "Replaces the sky with a topographic or gradient design.", Category.VISUALS, false,
                "sky", "skybox", "topographic", "contour");
    }

    private boolean isTopo() {
        return mode.is(Mode.TOPOGRAPHIC);
    }

    @Override
    protected void onEnable() {
        SkyDome.generateAsync();
    }

    @Override
    public Phase phase() {
        return Phase.SKY;
    }

    private float brightness() {
        if (!dayNight.on() || mc.world == null) {
            return 1f;
        }
        if (mc.world.getRegistryKey() != World.OVERWORLD) {
            return 0.75f;
        }
        long time = mc.world.getTimeOfDay() % 24000L;
        // Bright from 0..12000, dim at night with smooth transitions around dusk/dawn.
        return 0.28f + 0.72f * Math.max(0f, Math.min(1f, (float) (Math.cos((time - 6000) / 24000.0 * Math.PI * 2) * 1.6 + 0.5)));
    }

    public int fogColor() {
        return Colors.darken(horizon.color(), 1f - brightness());
    }

    public boolean matchFog() {
        return isEnabled() && matchFog.on();
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.world == null || mc.world.getRegistryKey() == World.END) {
            return;
        }
        float b = brightness();
        int top = Colors.withAlpha(Colors.darken(sky.color(), 1f - b), opacity.f());
        int hor = Colors.withAlpha(Colors.darken(horizon.color(), 1f - b), opacity.f());
        int line = Colors.fade(lines.color(), opacity.f());
        float radius = Math.max(64f, mc.options.getClampedViewDistance() * 16f * 1.7f);
        SkyDome.render(fx, vcp, radius, isTopo() ? top : top, isTopo() ? Colors.lerp(top, hor, 0.35f) : hor, line, isTopo(), 0f);
    }
}
