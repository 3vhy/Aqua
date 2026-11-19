package dev.aqua.client.module.qol;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.render.world.WorldText;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Markers at your own recent death positions ("Latest Death", R1 0:13-0:17), adapted: only your own
 * deaths, recorded locally for the current session. Markers are depth tested.
 */
public final class DeathWaypoints extends Module implements WorldEffect {
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFFFF6B6B, false, false));
    private final NumberSetting keep = add(new NumberSetting("keep", "Remember", 3, 1, 10, 1).unit(" deaths"));

    private record Death(Vec3d pos, RegistryKey<World> dim, long time) {}

    private final List<Death> deaths = new ArrayList<>();
    private boolean dead;

    public DeathWaypoints() {
        super("death_waypoints", "Death Waypoints", "Marks where you last died so you can find your items.", Category.QOL, true,
                "waypoint", "death", "grave", "marker");
        hideFromModuleList();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        boolean isDead = mc.currentScreen instanceof DeathScreen || mc.player.isDead();
        if (isDead && !dead) {
            deaths.add(0, new Death(mc.player.getEntityPos(), mc.world.getRegistryKey(), System.currentTimeMillis()));
            while (deaths.size() > keep.i()) {
                deaths.remove(deaths.size() - 1);
            }
        }
        dead = isDead;
    }

    public int count() {
        return deaths.size();
    }

    public void clear() {
        deaths.clear();
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
        for (int i = 0; i < deaths.size(); i++) {
            Death d = deaths.get(i);
            if (d.dim != mc.world.getRegistryKey()) {
                continue;
            }
            double dist = Math.sqrt(d.pos.squaredDistanceTo(mc.player.getEntityPos()));
            int c = Colors.fade(color.color(), i == 0 ? 0.9f : 0.5f);
            fx.line(vc, d.pos.x, d.pos.y, d.pos.z, d.pos.x, d.pos.y + 40, d.pos.z, 0.08f, Colors.fade(c, 0.6f));
            fx.ring(vc, d.pos.x, d.pos.y + 0.03, d.pos.z, 0.7f, 0.1f, c, 28);
            String label = (i == 0 ? "Latest Death" : "Death " + (i + 1)) + "  •  " + (dist >= 1000 ? String.format("%.1fkm", dist / 1000) : Math.round(dist) + "m");
            float h = (float) Math.max(0.35, Math.min(6, dist * 0.035));
            WorldText.draw(fx, vcp, label, d.pos.x, d.pos.y + 2.2 + h, d.pos.z, h, AquaFont.Weight.SEMIBOLD, 0xFFFFFFFF, color.color(), 0.6f);
        }
    }
}
