package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.aqua.client.render.GlassBlur;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes the blurred frame behind Aqua's glass surfaces at the right moments of the GUI draw. */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void aqua$glassStart(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        dev.aqua.client.render.PostFx.apply();
        GlassBlur.onGuiRenderStart();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void aqua$glassEnd(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        GlassBlur.onGuiRenderEnd();
    }

    @WrapOperation(method = "renderPreparedDraws", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderBlur()V"))
    private void aqua$glassBackdrop(GameRenderer renderer, Operation<Void> original) {
        if (!GlassBlur.onBlurLayer()) {
            original.call(renderer);
        }
    }
}
