package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.AquaBadge;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

    /** The tab list's dark header, list and footer backgrounds become one frosted glass pane. */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void aqua$glassBackground(net.minecraft.client.gui.DrawContext ctx, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        if (color == Integer.MIN_VALUE && x2 > x1 && y2 > y1 && dev.aqua.client.render.Glass.on() && dev.aqua.client.gui.VanillaStyle.on()) {
            int tint = dev.aqua.client.render.Colors.withAlpha(dev.aqua.client.Aqua.theme().background(), 0.5f);
            dev.aqua.client.render.R2D.glass(ctx, x1, y1, x2 - x1, y2 - y1, 0, dev.aqua.client.render.R2D.ALL, 0.2f, 0xFFFFFFFF, 0x80000000);
            dev.aqua.client.render.R2D.rect(ctx, x1, y1, x2 - x1, y2 - y1, tint);
            dev.aqua.client.gui.VanillaStyle.noteTabPane();
            return;
        }
        original.call(ctx, x1, y1, x2, y2, color);
    }
}
