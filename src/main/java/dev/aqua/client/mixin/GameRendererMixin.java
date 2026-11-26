package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.qol.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aqua.client.hud.HudFallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
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

    /** Custom Fov: the base field of view instead of the options value. */
    @WrapOperation(method = "getFov", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 0))
    private Object aqua$customFov(net.minecraft.client.option.SimpleOption<?> option, Operation<Object> original) {
        Object v = original.call(option);
        dev.aqua.client.module.visual.CustomFov cf = Aqua.modules() != null ? Aqua.modules().get(dev.aqua.client.module.visual.CustomFov.class) : null;
        if (cf != null && cf.isEnabled() && v instanceof Integer) {
            return cf.fov();
        }
        return v;
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

    /** Draws Aqua's HUD directly when another mod stops Fabric's HUD layers from running. */
    @WrapOperation(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void aqua$hudFallback(InGameHud hud, DrawContext ctx, RenderTickCounter tickCounter, Operation<Void> original) {
        HudFallback.beforeVanillaHud();
        original.call(hud, ctx, tickCounter);
        HudFallback.afterVanillaHud(ctx);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderDeferredSubtitles()V", shift = At.Shift.AFTER))
    private void aqua$hudFallbackEnd(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        HudFallback.endOfHud();
    }
}
