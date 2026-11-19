package dev.aqua.client.render.world;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;

import java.util.LinkedHashMap;

/**
 * Aqua's own vertex consumer provider for world effects. Every layer gets a dedicated buffer, so
 * switching between layers never ends another layer's buffer (the shared fallback buffer of an
 * Immediate would). Allocators are created lazily, once per layer, and reused every frame.
 */
public final class FxBuffers {
    private static final LinkedHashMap<RenderLayer, BufferAllocator> BUFFERS = new LinkedHashMap<>();
    private static VertexConsumerProvider.Immediate immediate;

    private FxBuffers() {}

    private static final class Provider implements VertexConsumerProvider {
        @Override
        public net.minecraft.client.render.VertexConsumer getBuffer(RenderLayer layer) {
            if (!BUFFERS.containsKey(layer)) {
                BUFFERS.put(layer, new BufferAllocator(Math.max(4096, Math.min(layer.getExpectedBufferSize(), 1 << 20))));
            }
            return immediate().getBuffer(layer);
        }
    }

    private static final Provider PROVIDER = new Provider();

    private static VertexConsumerProvider.Immediate immediate() {
        if (immediate == null) {
            immediate = VertexConsumerProvider.immediate(BUFFERS, new BufferAllocator(4096));
        }
        return immediate;
    }

    public static VertexConsumerProvider provider() {
        return PROVIDER;
    }

    /** Draws everything recorded since the last flush. */
    public static void flush() {
        immediate().draw();
    }
}
