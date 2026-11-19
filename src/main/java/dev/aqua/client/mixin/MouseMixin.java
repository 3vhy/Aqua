package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.Zoom;
import dev.aqua.client.util.CpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void aqua$clicks(long window, MouseInput input, int action, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (Aqua.keybinds() == null || window != mc.getWindow().getHandle()) {
            return;
        }
        if (action == InputUtil.GLFW_PRESS && mc.currentScreen == null) {
            // Real player input only: counted from GLFW button events while in game.
            CpsTracker.click(input.button());
            Aqua.keybinds().onInput(InputUtil.Type.MOUSE, input.button(), true);
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void aqua$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Zoom zoom = Aqua.modules() != null ? Aqua.modules().get(Zoom.class) : null;
        if (zoom != null && zoom.onScroll(vertical)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "updateMouse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 0))
    private Object aqua$zoomSensitivity(Object original) {
        Zoom zoom = Aqua.modules() != null ? Aqua.modules().get(Zoom.class) : null;
        if (zoom != null && original instanceof Double d) {
            return d * zoom.sensitivityFactor();
        }
        return original;
    }
}
