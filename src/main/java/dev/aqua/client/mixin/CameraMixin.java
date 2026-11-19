package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aqua.client.module.qol.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Freelook: the camera uses its own direction while the player keeps theirs. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw(F)F"))
    private float aqua$freelookYaw(Entity entity, float tickProgress, Operation<Float> original) {
        if (entity == MinecraftClient.getInstance().player && Freelook.on()) {
            return Freelook.get().yaw();
        }
        return original.call(entity, tickProgress);
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPitch(F)F"))
    private float aqua$freelookPitch(Entity entity, float tickProgress, Operation<Float> original) {
        if (entity == MinecraftClient.getInstance().player && Freelook.on()) {
            return Freelook.get().pitch();
        }
        return original.call(entity, tickProgress);
    }
}
