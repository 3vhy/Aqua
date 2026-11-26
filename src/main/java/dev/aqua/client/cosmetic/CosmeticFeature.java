package dev.aqua.client.cosmetic;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.cosmetic.Cape;
import dev.aqua.client.module.cosmetic.Halo;
import dev.aqua.client.module.cosmetic.Hats;
import dev.aqua.client.module.cosmetic.Wings;
import dev.aqua.client.render.world.FxLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

/** Renders Aqua's wearable cosmetics; only states carrying a {@link Look} (your own player) are affected. */
public final class CosmeticFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public CosmeticFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle,
                       float limbDistance) {
        Look look = Look.of(state);
        if (look == null || state.invisible || Aqua.modules() == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        float dark = mc.world != null ? mc.world.getAmbientDarkness() / 11f : 0f;
        float lf = Mesh.lightFactor(light, dark);
        float age = state.age;
        float t = age / 20f;
        PlayerEntityModel model = getContextModel();

        if (look.wings() != null && !state.isGliding) {
            Wings wings = Aqua.modules().get(Wings.class);
            Wings.Style style = look.wings();
            boolean moving = state.limbSwingAmplitude > 0.1f;
            withPart(matrices, model.body, () -> queue.submitCustom(matrices, FxLayers.SOLID,
                    (entry, vc) -> wings.draw(new Mesh(entry, vc, lf), style, age, moving)));
        }
        if (look.cape() != null && !state.isGliding) {
            Cape cape = Aqua.modules().get(Cape.class);
            Cape.Style style = look.cape();
            float swing = 6f + state.field_53537 / 2f + state.field_53536;
            float side = state.field_53538;
            withPart(matrices, model.body, () -> queue.submitCustom(matrices, FxLayers.SOLID,
                    (entry, vc) -> cape.draw(new Mesh(entry, vc, lf), style, t, swing, side)));
        }
        if (look.hat() != null) {
            Hats hats = Aqua.modules().get(Hats.class);
            Hats.Style style = look.hat();
            withPart(matrices, model.head, () -> {
                queue.submitCustom(matrices, FxLayers.SOLID, (entry, vc) -> hats.drawSolid(new Mesh(entry, vc, lf), style, t));
                if (hats.usesTranslucent(style)) {
                    queue.submitCustom(matrices, FxLayers.COLOR, (entry, vc) -> hats.drawTranslucent(new Mesh(entry, vc, 1f), style, t));
                }
            });
        }
        dev.aqua.client.module.cosmetic.PharaohHeaddress ph = Aqua.modules().get(dev.aqua.client.module.cosmetic.PharaohHeaddress.class);
        if (ph != null && ph.isEnabled()) {
            withPart(matrices, model.head, () -> queue.submitCustom(matrices, FxLayers.SOLID, (entry, vc) -> ph.draw(new Mesh(entry, vc, lf), t)));
        }
        if (look.halo()) {
            Halo halo = Aqua.modules().get(Halo.class);
            matrices.push();
            model.head.applyTransform(matrices);
            queue.submitCustom(matrices, FxLayers.GLOW, (entry, vc) -> halo.draw(entry, vc, age));
            matrices.pop();
        }
    }

    /** Pushes the part transform and pixel scale, runs the action, pops. */
    private static void withPart(MatrixStack matrices, net.minecraft.client.model.ModelPart part, Runnable action) {
        matrices.push();
        part.applyTransform(matrices);
        matrices.scale(1f / 16f, 1f / 16f, 1f / 16f);
        action.run();
        matrices.pop();
    }
}
