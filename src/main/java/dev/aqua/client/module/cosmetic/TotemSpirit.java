package dev.aqua.client.module.cosmetic;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.Particles;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Totem Spirit (reference video): when a totem saves someone, a glowing spirit shaped like the
 * totem rises from them, spreads its wings and fades. Only you see it.
 */
public final class TotemSpirit extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFFD86B, false, false));
    private final BoolSetting others = add(new BoolSetting("others", "Also for other players", true));

    private record Spirit(Vec3d pos, long start) {}

    private final List<Spirit> spirits = new ArrayList<>();
    private int popped;

    public TotemSpirit() {
        super("totem_spirit", "Totem Spirit", "A glowing totem spirit rises when a totem of undying saves someone.", Category.COSMETICS, false,
                "totem", "spirit", "pop", "undying");
    }

    public int popped() {
        return popped;
    }

    public void onPop(Entity e) {
        if (!others.on() && e != mc.player) {
            return;
        }
        popped++;
        spirits.add(new Spirit(e.getEntityPos(), System.currentTimeMillis()));
        for (int i = 0; i < 18; i++) {
            double a = Math.random() * Math.PI * 2;
            Particles.spawn(e.getX() + Math.cos(a) * 0.4, e.getY() + 1, e.getZ() + Math.sin(a) * 0.4, (float) Math.cos(a) * 0.6f, 1.2f,
                    (float) Math.sin(a) * 0.6f, 1.2f, 0.06f, color.color(), -0.6f, 1.2f);
        }
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        long now = System.currentTimeMillis();
        spirits.removeIf(s -> now - s.start > 2200);
        if (spirits.isEmpty()) {
            return;
        }
        fx.sparks = vcp.getBuffer(FxLayers.spark());
        VertexConsumer glow = vcp.getBuffer(FxLayers.GLOW);
        int c = color.color();
        for (Spirit s : spirits) {
            float t = (now - s.start) / 2200f;
            float fade = Math.min(1f, Math.min(t * 6f, (1f - t) * 2.5f));
            float rise = t * 2.4f;
            float wings = Math.min(1f, t * 3f);
            fx.matrices.push();
            fx.matrices.translate(s.pos.x - fx.cam.x, s.pos.y + 0.9 + rise - fx.cam.y, s.pos.z - fx.cam.z);
            fx.matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180f - mc.gameRenderer.getCamera().getYaw()));
            var m = fx.matrices.peek().getPositionMatrix();
            int col = Colors.fade(c, 0.55f * fade);
            // body and head
            quad(glow, m, -0.16f, -0.35f, 0.16f, 0.25f, col);
            quad(glow, m, -0.2f, 0.25f, 0.2f, 0.62f, Colors.fade(c, 0.6f * fade));
            // wings spreading
            float wl = 0.55f * wings;
            quad(glow, m, -0.16f - wl, -0.05f, -0.16f, 0.22f, Colors.fade(c, 0.4f * fade));
            quad(glow, m, 0.16f, -0.05f, 0.16f + wl, 0.22f, Colors.fade(c, 0.4f * fade));
            // eyes
            quad(glow, m, -0.12f, 0.42f, -0.04f, 0.5f, Colors.fade(0xFF2EE6A0, fade));
            quad(glow, m, 0.04f, 0.42f, 0.12f, 0.5f, Colors.fade(0xFF2EE6A0, fade));
            fx.matrices.pop();
            fx.spark(s.pos.x, s.pos.y + 1.1 + rise, s.pos.z, 1.1f, Colors.fade(c, 0.18f * fade));
        }
    }

    private static void quad(VertexConsumer vc, org.joml.Matrix4f m, float x0, float y0, float x1, float y1, int c) {
        vc.vertex(m, x0, y0, 0).color(c);
        vc.vertex(m, x1, y0, 0).color(c);
        vc.vertex(m, x1, y1, 0).color(c);
        vc.vertex(m, x0, y1, 0).color(c);
    }

    @Override
    protected void onDisable() {
        spirits.clear();
    }
}
