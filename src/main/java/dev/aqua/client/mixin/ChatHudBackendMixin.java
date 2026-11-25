package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.ChatStyle;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Chat line backgrounds, closed and open chat (ChatHud's two drawing backends): recoloured, or frosted glass. */
@Mixin(targets = {"net.minecraft.client.gui.hud.ChatHud$Hud", "net.minecraft.client.gui.hud.ChatHud$Interactable"})
public abstract class ChatHudBackendMixin {
    @WrapOperation(method = "fill", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void aqua$background(DrawContext ctx, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        ChatStyle s = Aqua.modules() != null ? Aqua.modules().get(ChatStyle.class) : null;
        if (s == null || !s.isEnabled()) {
            original.call(ctx, x1, y1, x2, y2, color);
        } else if (!s.drawGlass(ctx, x1, y1, x2, y2, color)) {
            original.call(ctx, x1, y1, x2, y2, s.recolor(color));
        }
    }
}
