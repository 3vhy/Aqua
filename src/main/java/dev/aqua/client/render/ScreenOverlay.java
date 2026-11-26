package dev.aqua.client.render;

import net.minecraft.client.gui.DrawContext;

/** A module that draws over the game view (below Aqua's HUD widgets). */
public interface ScreenOverlay {
    void renderOverlay(DrawContext ctx, float tickDelta);
}
