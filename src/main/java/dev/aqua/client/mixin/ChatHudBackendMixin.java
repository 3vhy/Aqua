package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.ChatStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Recolours chat line backgrounds (ChatHud's in-game drawing backend). */
@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$Hud")
public abstract class ChatHudBackendMixin {
    @ModifyArg(method = "fill", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), index = 4)
    private int aqua$background(int color) {
        ChatStyle s = Aqua.modules() != null ? Aqua.modules().get(ChatStyle.class) : null;
        return s != null && s.isEnabled() ? s.recolor(color) : color;
    }
}
