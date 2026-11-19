package dev.aqua.client.render.world;

import net.minecraft.client.render.VertexConsumerProvider;

/** A module that draws in the world. */
public interface WorldEffect {
    enum Phase { SKY, WORLD }

    default Phase phase() {
        return Phase.WORLD;
    }

    void renderWorld(Fx fx, VertexConsumerProvider vcp);
}
