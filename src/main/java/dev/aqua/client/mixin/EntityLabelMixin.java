package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aqua.client.Aqua;
import dev.aqua.client.module.qol.Nametags;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nametags: bigger player nametags (scaled around their own position). */
@Mixin(EntityRenderer.class)
public abstract class EntityLabelMixin {
    @WrapOperation(method = "renderLabelIfPresent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V"))
    private void aqua$scaleLabel(OrderedRenderCommandQueue queue, MatrixStack matrices, Vec3d pos, int y, Text text, boolean notSneaking, int light,
                                 double distance, CameraRenderState camera, Operation<Void> original,
                                 @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) EntityRenderState state) {
        Nametags n = Aqua.modules() != null ? Aqua.modules().get(Nametags.class) : null;
        if (n == null || !n.isEnabled() || !(state instanceof PlayerEntityRenderState) || pos == null) {
            original.call(queue, matrices, pos, y, text, notSneaking, light, distance, camera);
            return;
        }
        float s = n.scale();
        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);
        matrices.scale(s, s, s);
        matrices.translate(-pos.x, -pos.y, -pos.z);
        original.call(queue, matrices, pos, y, text, notSneaking, light, distance, camera);
        matrices.pop();
    }
}
