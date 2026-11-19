package dev.aqua.client.render.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import dev.aqua.client.render.AquaPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Render layers for Aqua's world-space effects (all depth tested, none write depth). */
public final class FxLayers {
    public static final RenderLayer GLOW = RenderLayer.of("aqua_glow",
            RenderSetup.builder(AquaPipelines.WORLD_GLOW).translucent().expectedBufferSize(262144).build());
    public static final RenderLayer SOLID = RenderLayer.of("aqua_solid",
            RenderSetup.builder(AquaPipelines.WORLD_SOLID).expectedBufferSize(131072).build());
    public static final RenderLayer COLOR = RenderLayer.of("aqua_color",
            RenderSetup.builder(AquaPipelines.WORLD_COLOR).translucent().expectedBufferSize(131072).build());
    private static final Map<Identifier, RenderLayer> TEXTURED = new HashMap<>();
    private static final Map<Identifier, RenderLayer> TEXTURED_GLOW = new HashMap<>();

    public static final Identifier SPARK = Identifier.of("aqua", "dynamic/spark");
    private static boolean sparkReady;

    private FxLayers() {}

    /** Additive layer with a soft round falloff texture for particles (texture built once). */
    public static RenderLayer spark() {
        if (!sparkReady) {
            sparkReady = true;
            net.minecraft.client.texture.NativeImage img = new net.minecraft.client.texture.NativeImage(64, 64, false);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    double dx = (x + 0.5) / 32.0 - 1, dy = (y + 0.5) / 32.0 - 1;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    double core = Math.max(0, 1 - d / 0.35);
                    double halo = Math.max(0, 1 - d);
                    double a = Math.min(1, core * 0.85 + halo * halo * 0.45);
                    img.setColorArgb(x, y, ((int) Math.round(a * 255) << 24) | 0xFFFFFF);
                }
            }
            net.minecraft.client.MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(SPARK, new net.minecraft.client.texture.NativeImageBackedTexture(() -> "aqua_spark", img));
        }
        return texturedGlow(SPARK);
    }

    public static RenderLayer textured(Identifier tex) {
        return TEXTURED.computeIfAbsent(tex, t -> RenderLayer.of("aqua_textured",
                RenderSetup.builder(AquaPipelines.WORLD_TEXTURED).texture("Sampler0", t, FxLayers::linear).translucent()
                        .expectedBufferSize(65536).build()));
    }

    public static RenderLayer texturedGlow(Identifier tex) {
        return TEXTURED_GLOW.computeIfAbsent(tex, t -> RenderLayer.of("aqua_textured_glow",
                RenderSetup.builder(AquaPipelines.WORLD_TEXTURED_GLOW).texture("Sampler0", t, FxLayers::linear).translucent()
                        .expectedBufferSize(65536).build()));
    }

    private static net.minecraft.client.gl.GpuSampler linear() {
        return RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);
    }
}
