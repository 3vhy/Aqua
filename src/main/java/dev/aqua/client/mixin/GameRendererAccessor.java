package dev.aqua.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Read access to the field of view (used by the in-game tests for Custom Fov). */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("getFov")
    float aqua$getFov(Camera camera, float tickProgress, boolean changingFov);
}
