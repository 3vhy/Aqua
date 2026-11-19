package dev.aqua.client.cosmetic.character;

import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.render.world.FxLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

/** Draws a 3D character in place of the player model, posed by the player model's animation. */
public final class CharacterRenderer {
    private static long drawn;

    private CharacterRenderer() {}

    /** How many times a character has been submitted for drawing (used by the in-game tests). */
    public static long drawn() {
        return drawn;
    }

    public static void render(MatrixStack ms, OrderedRenderCommandQueue queue, PlayerEntityModel model, PlayerEntityRenderState state,
                              Characters character) {
        if (state.invisible) {
            return;
        }
        drawn++;
        model.setAngles(state);
        float t = state.age / 20f;
        MinecraftClient mc = MinecraftClient.getInstance();
        float dark = mc.world != null ? mc.world.getAmbientDarkness() / 11f : 0f;
        float light = Mesh.lightFactor(state.light, dark);
        CharacterDef def = character.def;
        ms.push();
        ms.translate(0f, def.bob(t) / 16f, 0f);
        part(ms, queue, null, light, m -> def.root(m, t));
        part(ms, queue, model.body, light, m -> def.body(m, t));
        part(ms, queue, model.head, light, m -> def.head(m, t));
        part(ms, queue, model.rightArm, light, m -> def.rightArm(m, t));
        part(ms, queue, model.leftArm, light, m -> def.leftArm(m, t));
        part(ms, queue, model.rightLeg, light, m -> def.rightLeg(m, t));
        part(ms, queue, model.leftLeg, light, m -> def.leftLeg(m, t));
        ms.pop();
    }

    private static void part(MatrixStack ms, OrderedRenderCommandQueue queue, ModelPart p, float light, Consumer<Mesh> draw) {
        ms.push();
        if (p != null) {
            p.applyTransform(ms);
        }
        ms.scale(1f / 16f, 1f / 16f, 1f / 16f);
        queue.submitCustom(ms, FxLayers.SOLID, (entry, vc) -> draw.accept(new Mesh(entry, vc, light)));
        ms.pop();
    }
}
