package dev.aqua.client.module.cosmetic;

import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerSkinType;

/**
 * Glass (R7 "Glass", R8 crystal player): your own player drawn as clear glass with glowing edges.
 * Only you see it; other players are not changed.
 */
public final class GlassSkin extends Module {
    private final ColorSetting tint = add(new ColorSetting("tint", "Glass colour", 0x66CFEFFF, true, false));
    private final ColorSetting edges = add(new ColorSetting("edges", "Edge glow", 0xFFBFE8FF, false, true));
    private final NumberSetting edgeWidth = add(new NumberSetting("edge_width", "Edge width", 0.35, 0.1, 0.8, 0.05));

    private static long drawn;

    public GlassSkin() {
        super("glass", "Glass", "Your player made of clear glass with glowing edges (you only).", Category.COSMETICS, false,
                "glass", "crystal", "transparent", "ghost skin", "see-through");
    }

    public int tint() {
        return tint.color();
    }

    /** Glowing outline of every body part, following the player's pose. */
    public void edges(MatrixStack ms, OrderedRenderCommandQueue queue, PlayerEntityModel model, PlayerEntityRenderState state) {
        drawn++;
        boolean slim = state.skinTextures != null && state.skinTextures.model() == PlayerSkinType.SLIM;
        float aw = slim ? 3 : 4;
        int c = edges.color();
        float w = edgeWidth.f();
        part(ms, queue, model.head, -4, -8, -4, 4, 0, 4, w, c);
        part(ms, queue, model.body, -4, 0, -2, 4, 12, 2, w, c);
        part(ms, queue, model.rightArm, -aw + 1, -2, -2, 1, 10, 2, w, c);
        part(ms, queue, model.leftArm, -1, -2, -2, aw - 1, 10, 2, w, c);
        part(ms, queue, model.rightLeg, -2, 0, -2, 2, 12, 2, w, c);
        part(ms, queue, model.leftLeg, -2, 0, -2, 2, 12, 2, w, c);
    }

    private static void part(MatrixStack ms, OrderedRenderCommandQueue queue, ModelPart p, float x0, float y0, float z0, float x1, float y1,
                             float z1, float w, int c) {
        if (!p.visible) {
            return;
        }
        ms.push();
        p.applyTransform(ms);
        ms.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float g = 0.25f;
        float ax = x0 - g, ay = y0 - g, az = z0 - g, bx = x1 + g, by = y1 + g, bz = z1 + g;
        float h = w / 2f;
        queue.submitCustom(ms, FxLayers.GLOW, (entry, vc) -> {
            Mesh m = new Mesh(entry, vc, 1f);
            int soft = Colors.fade(c, 0.85f);
            for (float y : new float[]{ay, by}) {
                for (float z : new float[]{az, bz}) {
                    m.glow(ax, y - h, z - h, bx, y + h, z + h, soft);
                }
            }
            for (float x : new float[]{ax, bx}) {
                for (float z : new float[]{az, bz}) {
                    m.glow(x - h, ay, z - h, x + h, by, z + h, soft);
                }
                for (float y : new float[]{ay, by}) {
                    m.glow(x - h, y - h, az, x + h, y + h, bz, soft);
                }
            }
        });
        ms.pop();
    }

    /** Frames drawn as glass (used by the in-game tests). */
    public static long drawn() {
        return drawn;
    }
}
