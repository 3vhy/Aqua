package dev.aqua.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/**
 * A rounded rectangle that shows the blurred frame behind it ({@link GlassBlur}). The blur texture
 * is looked up when the GUI is drawn, not when the element is created, so it is always the one
 * made for this frame. If no blur is available the element falls back to a plain tinted fill.
 */
public final class GlassRectState extends RoundedRectState {
    private final GlassBlur.Slot slot;
    private final RoundedRectState fallback;

    public GlassRectState(Matrix3x2f pose, float x, float y, float w, float h, float radius, int cornerMask, float vibrancy,
                          int color, int fallbackColor, GlassBlur.Slot slot, @Nullable ScreenRect scissor) {
        super(pose, x, y, w, h, radius, vibrancy, 0, cornerMask, color, color, color, color, scissor);
        this.slot = slot;
        this.fallback = new RoundedRectState(pose, x, y, w, h, radius, 0, 0, cornerMask,
                fallbackColor, fallbackColor, fallbackColor, fallbackColor, scissor);
    }

    @Override
    public void setupVertices(VertexConsumer v) {
        if (GlassBlur.ready(slot)) {
            super.setupVertices(v);
        } else {
            fallback.setupVertices(v);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return GlassBlur.ready(slot) ? AquaPipelines.GLASS : AquaPipelines.ROUNDED;
    }

    @Override
    public TextureSetup textureSetup() {
        TextureSetup s = GlassBlur.ready(slot) ? GlassBlur.setup(slot) : null;
        return s != null ? s : TextureSetup.empty();
    }
}
