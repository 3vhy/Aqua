package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.AquaBadge;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aqua Badge in the tab list. */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void aqua$badge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        AquaBadge badge = Aqua.modules() != null ? Aqua.modules().get(AquaBadge.class) : null;
        if (badge != null && badge.inTab()) {
            cir.setReturnValue(AquaBadge.decorate(cir.getReturnValue(), entry.getProfile().id()));
        }
    }
}
