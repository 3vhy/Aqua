package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.aqua.client.gui.VanillaStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Minecraft's text boxes in Aqua's style: a recessed box with an accent ring while typing. */
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void aqua$box(DrawContext ctx, RenderPipeline pipeline, Identifier id, int x, int y, int w, int h, Operation<Void> original) {
        if (!VanillaStyle.on()) {
            original.call(ctx, pipeline, id, x, y, w, h);
            return;
        }
        VanillaStyle.textBox(ctx, (ClickableWidget) (Object) this);
    }
}
