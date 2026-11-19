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
    @org.spongepowered.asm.mixin.Shadow
    private double cursorDeltaX;
    @org.spongepowered.asm.mixin.Shadow
    private double cursorDeltaY;
    @org.spongepowered.asm.mixin.Unique
    private long aqua$lastNanos;

    /** Keystrokes mouse indicator: sample the real cursor movement the camera is about to use. */
    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void aqua$mouseMovement(double timeDelta, CallbackInfo ci) {
        long now = System.nanoTime();
        double seconds = aqua$lastNanos == 0 ? 0.016 : Math.min(0.1, (now - aqua$lastNanos) / 1.0e9);
        aqua$lastNanos = now;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null && mc.mouse.isCursorLocked() && (cursorDeltaX != 0 || cursorDeltaY != 0)) {
            dev.aqua.client.util.MouseTracker.onDelta(cursorDeltaX, cursorDeltaY, seconds);
        } else {
            dev.aqua.client.util.MouseTracker.decay(seconds);
        }
    }

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
