package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.aqua.client.gui.VanillaStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Minecraft's sliders in Aqua's style: an accent-filled track and a slim glowing handle. */
@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin {
    @Shadow
    protected double value;

    private static final String DRAW = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V";

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", target = DRAW, ordinal = 0))
    private void aqua$track(DrawContext ctx, RenderPipeline pipeline, Identifier id, int x, int y, int w, int h, int color, Operation<Void> original) {
        if (!VanillaStyle.on()) {
            original.call(ctx, pipeline, id, x, y, w, h, color);
            return;
        }
        ClickableWidget self = (ClickableWidget) (Object) this;
        VanillaStyle.sliderTrack(ctx, self, value, ((ClickableWidgetAccessor) self).aqua$alpha());
    }

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", target = DRAW, ordinal = 1))
    private void aqua$handle(DrawContext ctx, RenderPipeline pipeline, Identifier id, int x, int y, int w, int h, int color, Operation<Void> original) {
        if (!VanillaStyle.on()) {
            original.call(ctx, pipeline, id, x, y, w, h, color);
            return;
        }
        ClickableWidget self = (ClickableWidget) (Object) this;
        VanillaStyle.sliderHandle(ctx, self, x, ((ClickableWidgetAccessor) self).aqua$alpha());
    }
}
