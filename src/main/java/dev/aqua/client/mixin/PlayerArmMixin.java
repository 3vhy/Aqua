package dev.aqua.client.mixin;

import dev.aqua.client.Aqua;
import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.cosmetic.character.Characters;
import dev.aqua.client.module.visual.ThreeDHand;
import dev.aqua.client.render.world.FxLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 3D Hand: the first-person arm is drawn as your 3D Character's arm. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerArmMixin {
    @Inject(method = "renderArm", at = @At("HEAD"), cancellable = true)
    private void aqua$characterArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, ModelPart arm,
                                   boolean sleeveVisible, CallbackInfo ci) {
        ThreeDHand h = Aqua.modules() != null ? Aqua.modules().get(ThreeDHand.class) : null;
        Characters c = h != null ? h.character() : null;
        if (c == null) {
            return;
        }
        PlayerEntityModel model = (PlayerEntityModel) ((PlayerEntityRenderer) (Object) this).getModel();
        boolean right = arm == model.rightArm;
        arm.resetTransform();
        arm.visible = true;
        model.leftArm.roll = -0.1F;
        model.rightArm.roll = 0.1F;
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        MinecraftClient mc = MinecraftClient.getInstance();
        float dark = mc.world != null ? mc.world.getAmbientDarkness() / 11f : 0f;
        float lf = Mesh.lightFactor(light, dark);
        matrices.push();
        arm.applyTransform(matrices);
        matrices.scale(1f / 16f, 1f / 16f, 1f / 16f);
        queue.submitCustom(matrices, FxLayers.SOLID, (entry, vc) -> {
            Mesh m = new Mesh(entry, vc, lf);
            if (right) {
                c.def.rightArm(m, t);
            } else {
                c.def.leftArm(m, t);
            }
        });
        matrices.pop();
        ThreeDHand.noteDrawn();
        ci.cancel();
    }
}
