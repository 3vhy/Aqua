package dev.aqua.client.module.visual;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

import java.util.ArrayDeque;
import java.util.Iterator;

/** Rain Splashes (R7 "Weather - heavy rain with real splashes"): ripples and droplets where rain lands. */
public final class RainSplashes extends Module implements WorldEffect {
    private final NumberSetting amount = add(new NumberSetting("amount", "Splashes", 10, 2, 30, 1).unit(" per tick"));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFBFD8FF, false, false));

    private record Ripple(Vec3d pos, long start, float size) {}

    private final ArrayDeque<Ripple> ripples = new ArrayDeque<>();
    private final Random random = Random.create();

    public RainSplashes() {
        super("rain_splashes", "Rain Splashes", "Ripples and droplets where the rain lands around you.", Category.VISUALS, false,
                "rain", "splashes", "weather", "puddles", "ripples", "wet ground");
    }

    @Override
    public void onTick() {
        var world = mc.world;
        var p = mc.player;
        if (world == null || p == null) {
            return;
        }
        float rain = world.getRainGradient(1f);
        if (rain < 0.2f) {
            return;
        }
        int n = Math.round(amount.f() * rain * Math.min(1.5f, Aqua.ui().effectIntensity.f()));
        for (int i = 0; i < n; i++) {
            double x = p.getX() + (random.nextDouble() - 0.5) * 20, z = p.getZ() + (random.nextDouble() - 0.5) * 20;
            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            BlockPos at = BlockPos.ofFloored(x, top, z);
            if (Math.abs(top - p.getY()) > 12 || world.getBiome(at).value().getPrecipitation(at, world.getSeaLevel()) != Biome.Precipitation.RAIN) {
                continue;
            }
            double y = top + 0.02;
            ripples.addLast(new Ripple(new Vec3d(x, y, z), System.currentTimeMillis(), 0.12f + random.nextFloat() * 0.12f));
            if (random.nextFloat() < 0.5f) {
                for (int k = 0; k < 2; k++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    Particles.spawn(x, y, z, (float) Math.cos(a) * 0.6f, 1.2f + random.nextFloat() * 0.8f, (float) Math.sin(a) * 0.6f,
                            0.25f, 0.018f, color.color(), 9f, 1f);
                }
            }
        }
        while (ripples.size() > 300) {
            ripples.removeFirst();
        }
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        long now = System.currentTimeMillis();
        VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
        Iterator<Ripple> it = ripples.iterator();
        while (it.hasNext()) {
            Ripple r = it.next();
            float t = (now - r.start) / 450f;
            if (t >= 1f) {
                it.remove();
                continue;
            }
            fx.ring(vc, r.pos.x, r.pos.y, r.pos.z, r.size * (0.3f + t), 0.02f, Colors.fade(color.color(), (1 - t) * 0.6f), 14);
        }
    }

    /** Ripples currently drawn (used by the in-game tests). */
    public int ripples() {
        return ripples.size();
    }
}
