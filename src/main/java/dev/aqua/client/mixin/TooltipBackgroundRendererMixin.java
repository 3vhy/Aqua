package dev.aqua.client.mixin;

import dev.aqua.client.gui.VanillaStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item and widget tooltips in Aqua's style. Tooltips that ask for their own background (an item
 * with a custom tooltip style from a data pack or resource pack) keep it.
 */
@Mixin(TooltipBackgroundRenderer.class)
public abstract class TooltipBackgroundRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void aqua$tooltip(DrawContext ctx, int x, int y, int width, int height, @Nullable Identifier texture, CallbackInfo ci) {
        if (texture == null && VanillaStyle.on()) {
            VanillaStyle.tooltip(ctx, x - 3, y - 3, width + 6, height + 6);
            ci.cancel();
        }
    }
}
