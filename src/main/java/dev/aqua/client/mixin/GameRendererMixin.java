package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.qol.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void aqua$frame(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        AquaClient.onFrame();
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void aqua$zoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        Zoom zoom = Aqua.modules() != null ? Aqua.modules().get(Zoom.class) : null;
        if (zoom != null) {
            float f = zoom.fovFactor();
            if (f != 1f) {
                cir.setReturnValue(cir.getReturnValue() * f);
            }
        }
    }
}
