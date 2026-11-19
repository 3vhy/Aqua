package dev.aqua.client.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.cosmetic.Halo;
import dev.aqua.client.module.cosmetic.Wings;
import dev.aqua.client.render.world.FxLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

/** Renders Aqua's wearable cosmetics on your own player only (they are not networked). */
public final class CosmeticFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public CosmeticFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle,
                       float limbDistance) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (Aqua.modules() == null || mc.player == null || state.id != mc.player.getId() || state.invisible) {
            return;
        }
        Wings wings = Aqua.modules().get(Wings.class);
        if (wings != null && wings.isEnabled() && !state.isGliding) {
            matrices.push();
            getContextModel().body.applyTransform(matrices);
            boolean moving = limbDistance > 0.1f || !mc.player.isOnGround();
            float age = state.age;
            queue.submitCustom(matrices, FxLayers.COLOR, (entry, vc) -> wings.draw(entry, vc, age, moving));
            matrices.pop();
        }
        Halo halo = Aqua.modules().get(Halo.class);
        if (halo != null && halo.isEnabled()) {
            matrices.push();
            getContextModel().head.applyTransform(matrices);
            float age = state.age;
            queue.submitCustom(matrices, FxLayers.GLOW, (entry, vc) -> halo.draw(entry, vc, age));
            matrices.pop();
        }
    }
}
