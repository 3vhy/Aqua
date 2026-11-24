package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.visual.LowOverlays;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Fire: draws the first-person burning overlay lower on the screen. */
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void aqua$lowFire(MatrixStack matrices, VertexConsumerProvider vcp, Sprite sprite, CallbackInfo ci) {
        LowOverlays low = Aqua.modules() != null ? Aqua.modules().get(LowOverlays.class) : null;
        if (low != null && low.fireOffset() > 0) {
            matrices.translate(0f, -low.fireOffset(), 0f);
        }
    }
}
