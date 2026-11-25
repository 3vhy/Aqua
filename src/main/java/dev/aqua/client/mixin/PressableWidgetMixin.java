package dev.aqua.client.mixin;

import dev.aqua.client.gui.VanillaStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/** Minecraft's standard buttons in Aqua's style (the label is still drawn by vanilla). */
@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin {
    @Shadow
    @Nullable
    private Supplier<Boolean> focusOverride;

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void aqua$drawButton(DrawContext ctx, CallbackInfo ci) {
        if (!VanillaStyle.on()) {
            return;
        }
        ClickableWidget self = (ClickableWidget) (Object) this;
        boolean selected = focusOverride != null && Boolean.TRUE.equals(focusOverride.get());
        VanillaStyle.button(ctx, self, ((ClickableWidgetAccessor) self).aqua$alpha(), selected);
        ci.cancel();
    }
}
