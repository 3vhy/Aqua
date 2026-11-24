package dev.aqua.client.module.visual;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.SkyDome;
import dev.aqua.client.render.world.SkyTextures;
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
        TOPOGRAPHIC("Topographic"), GRADIENT("Gradient"), NEBULA("Nebula"), AURORA("Aurora"), CELLS("Lava cells");
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
    public enum Nebula implements EnumSetting.Displayable {
        VIOLET("Violet", SkyTextures.Kind.NEBULA_VIOLET, 0xFF2A1048), EMERALD("Emerald", SkyTextures.Kind.NEBULA_EMERALD, 0xFF07301F),
        AZURE("Azure", SkyTextures.Kind.NEBULA_AZURE, 0xFF081C3C), CRIMSON("Crimson", SkyTextures.Kind.NEBULA_CRIMSON, 0xFF380810),
        SILVER("Silver", SkyTextures.Kind.NEBULA_SILVER, 0xFF2A3038);
        final String l;
        final SkyTextures.Kind kind;
        final int fog;

        Nebula(String l, SkyTextures.Kind kind, int fog) {
            this.l = l;
            this.kind = kind;
            this.fog = fog;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Nebula> nebula = add(new EnumSetting<>("nebula", "Nebula colours", Nebula.VIOLET)
            .visibleWhen(() -> mode.is(Mode.NEBULA)));
    private final ColorSetting cells = add(new ColorSetting("cells", "Cell glow colour", 0xFFFF4A12, false, false)
            .visibleWhen(() -> mode.is(Mode.CELLS)));
    private final BoolSetting shootingStars = add(new BoolSetting("shooting_stars", "Shooting stars", true)
            .describe("Streaks of light across the sky now and then (R5, R6 \"Celestial\")"));
    private final BoolSetting dayNight = add(new BoolSetting("day_night", "Darken at night", true));
    private final NumberSetting opacity = add(new NumberSetting("opacity", "Opacity", 1.0, 0.3, 1.0, 0.05)
            .describe("Lower values let the vanilla sun, moon and stars show through"));
    private final BoolSetting matchFog = add(new BoolSetting("match_fog", "Match fog to horizon", true));

    public CustomSky() {
        super("custom_sky", "Custom Sky", "Topographic, gradient, nebula, aurora or lava-cell sky, with shooting stars.", Category.VISUALS, false,
                "sky", "skybox", "topographic", "contour", "nebula", "galaxy", "aurora", "northern lights", "space", "celestial",
                "shooting stars", "lava", "voronoi");
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
        return switch (mode.get()) {
            case NEBULA -> nebula.get().fog;
            case AURORA -> 0xFF061A22;
            case CELLS -> Colors.darken(cells.color(), 0.82f);
            default -> Colors.darken(horizon.color(), 1f - brightness());
        };
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
        SkyTextures.Kind kind = switch (mode.get()) {
            case NEBULA -> nebula.get().kind;
            case AURORA -> SkyTextures.Kind.AURORA;
            case CELLS -> SkyTextures.Kind.CELLS;
            default -> null;
        };
        if (kind != null) {
            int tint = kind == SkyTextures.Kind.CELLS ? cells.color() : 0xFFFFFFFF;
            if (!SkyTextures.render(fx, vcp, kind, radius, Colors.withAlpha(tint, opacity.f()))) {
                // Still generating (first use takes a second or two): show a plain dark gradient meanwhile.
                SkyDome.render(fx, vcp, radius, Colors.withAlpha(0xFF05070C, opacity.f()), Colors.withAlpha(fogColor(), opacity.f()), 0, false, 0f);
            }
        } else {
            SkyDome.render(fx, vcp, radius, isTopo() ? top : top, isTopo() ? Colors.lerp(top, hor, 0.35f) : hor, line, isTopo(), 0f);
        }
        if (shootingStars.on()) {
            shootingStars(fx, vcp, radius * 0.9f);
        }
    }

    private record Streak(float x, float y, float z, float dx, float dy, float dz, long start, float life, float length) {}

    private final java.util.ArrayList<Streak> streaks = new java.util.ArrayList<>();
    private final java.util.Random random = new java.util.Random();
    private long nextStreak;

    private void shootingStars(Fx fx, VertexConsumerProvider vcp, float r) {
        long now = System.currentTimeMillis();
        boolean night = kindIsNight() || brightness() < 0.6f;
        if (now >= nextStreak && streaks.size() < 4) {
            nextStreak = now + (long) ((night ? 1200 : 3500) + random.nextFloat() * (night ? 2600 : 6000));
            double az = random.nextDouble() * Math.PI * 2;
            float el = 0.3f + random.nextFloat() * 0.45f;
            float h = (float) Math.sqrt(1 - el * el);
            float x = (float) Math.cos(az) * h, z = (float) Math.sin(az) * h;
            // Travel mostly sideways and a little downwards across the sky.
            float tx = -z, tz = x;
            float dir = random.nextBoolean() ? 1f : -1f;
            float dy = -0.35f - random.nextFloat() * 0.3f;
            float len = (float) Math.sqrt(1 + dy * dy);
            streaks.add(new Streak(x, el, z, tx * dir / len, dy / len, tz * dir / len, now, 0.7f + random.nextFloat() * 0.6f,
                    0.08f + random.nextFloat() * 0.07f));
        }
        net.minecraft.client.render.VertexConsumer vc = vcp.getBuffer(dev.aqua.client.render.world.FxLayers.GLOW);
        java.util.Iterator<Streak> it = streaks.iterator();
        while (it.hasNext()) {
            Streak s = it.next();
            float t = (now - s.start) / 1000f / s.life;
            if (t >= 1f) {
                it.remove();
                continue;
            }
            float travel = 0.35f * t;
            float hx = s.x + s.dx * travel, hy = s.y + s.dy * travel, hz = s.z + s.dz * travel;
            float tl = s.length * Math.min(1f, t * 4f);
            float alpha = (float) Math.sin(Math.min(1f, t) * Math.PI);
            net.minecraft.util.math.Vec3d head = fx.cam.add(hx * r, hy * r, hz * r);
            net.minecraft.util.math.Vec3d tail = fx.cam.add((hx - s.dx * tl) * r, (hy - s.dy * tl) * r, (hz - s.dz * tl) * r);
            fx.ribbon(vc, tail, head, 0.0f, r * 0.0035f, 0x00FFFFFF, Colors.fade(0xFFFFFFFF, alpha));
            fx.ribbon(vc, tail, head, 0.0f, r * 0.012f, 0x00AEE8FF, Colors.fade(0xFFAEE8FF, alpha * 0.35f));
        }
    }

    private boolean kindIsNight() {
        return mode.is(Mode.NEBULA) || mode.is(Mode.AURORA) || mode.is(Mode.CELLS);
    }
}
