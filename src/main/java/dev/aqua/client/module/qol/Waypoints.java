package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.render.world.Fx;
import dev.aqua.client.render.world.FxLayers;
import dev.aqua.client.render.world.WorldEffect;
import dev.aqua.client.render.world.WorldText;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.KeySetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.StringSetting;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Waypoints (reference video): places you mark yourself, shown with a light beam, their name and
 * distance. They are stored per world/server and dimension on this computer only.
 */
public final class Waypoints extends Module implements WorldEffect {
    public record Point(String name, double x, double y, double z, String dim, String world) {}

    /** Separators that survive the text setting (which drops control characters). */
    private static final String ROW = "¶";
    private static final String FIELD = "§";

    private static String clean(String s) {
        return s.replace(ROW, " ").replace(FIELD, " ");
    }

    private final StringSetting data = add(new StringSetting("data", "Saved waypoints", "", 20000).visibleWhen(() -> false));
    private final StringSetting nextName = add(new StringSetting("name", "Name for new waypoints", "Home", 32));
    private final KeySetting addKey = add(new KeySetting("add_key", "Add waypoint key"));
    private final ActionSetting addNow = add(new ActionSetting("add", "Add waypoint here", "Add", this::addHere));
    private final ActionSetting removeNearest = add(new ActionSetting("remove", "Remove nearest", "Remove", this::removeNearest));
    private final ActionSetting clear = add(new ActionSetting("clear", "Clear this world", "Clear", this::clearWorld));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", 0xFF52E4F2, false, true));
    private final BoolSetting beam = add(new BoolSetting("beam", "Light beam", true));
    private final BoolSetting distance = add(new BoolSetting("distance", "Show distance", true));
    private final NumberSetting maxDistance = add(new NumberSetting("max_distance", "Show within", 5000, 50, 100000, 50).unit(" m"));

    public Waypoints() {
        super("waypoints", "Waypoints", "Mark places and see them with a beam, their name and distance.", Category.QOL, false,
                "waypoint", "marker", "home", "beacon", "base");
    }

    @Override
    public void onKeySetting(KeySetting k) {
        if (k == addKey) {
            addHere();
        }
    }

    private String worldKey() {
        if (mc.getCurrentServerEntry() != null) {
            return "server:" + mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
        }
        if (mc.getServer() != null) {
            return "world:" + mc.getServer().getSaveProperties().getLevelName();
        }
        return "unknown";
    }

    private String dim() {
        return mc.world != null ? mc.world.getRegistryKey().getValue().toString() : "";
    }

    public List<Point> all() {
        List<Point> l = new ArrayList<>();
        for (String row : data.get().split(ROW, -1)) {
            String[] f = row.split(FIELD, -1);
            if (f.length != 6) {
                continue;
            }
            try {
                l.add(new Point(f[0], Double.parseDouble(f[1]), Double.parseDouble(f[2]), Double.parseDouble(f[3]), f[4], f[5]));
            } catch (NumberFormatException ignored) {
                // skip a damaged row
            }
        }
        return l;
    }

    private void save(List<Point> l) {
        StringBuilder b = new StringBuilder();
        for (Point p : l) {
            if (!b.isEmpty()) {
                b.append(ROW);
            }
            b.append(clean(p.name)).append(FIELD).append(p.x).append(FIELD).append(p.y).append(FIELD).append(p.z)
                    .append(FIELD).append(p.dim).append(FIELD).append(clean(p.world));
        }
        data.set(b.toString());
    }

    /** Waypoints in the world and dimension you are in. */
    public List<Point> here() {
        String w = worldKey(), d = dim();
        List<Point> l = new ArrayList<>();
        for (Point p : all()) {
            if (p.world.equals(w) && p.dim.equals(d)) {
                l.add(p);
            }
        }
        return l;
    }

    public void addHere() {
        if (mc.player == null) {
            return;
        }
        List<Point> l = all();
        String n = nextName.get().isBlank() ? "Waypoint" : nextName.get().trim();
        int count = 1;
        for (Point p : l) {
            if (p.name.startsWith(n)) {
                count++;
            }
        }
        String name = count > 1 ? n + " " + count : n;
        l.add(new Point(name, Math.floor(mc.player.getX()) + 0.5, Math.floor(mc.player.getY()), Math.floor(mc.player.getZ()) + 0.5, dim(), worldKey()));
        save(l);
        Aqua.notifications().info("Waypoint added", name);
    }

    public void removeNearest() {
        if (mc.player == null) {
            return;
        }
        List<Point> l = all();
        Point best = null;
        double bd = Double.MAX_VALUE;
        for (Point p : here()) {
            double d = mc.player.squaredDistanceTo(p.x, p.y, p.z);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        if (best != null) {
            l.remove(best);
            save(l);
            Aqua.notifications().info("Waypoint removed", best.name);
        }
    }

    public void clearWorld() {
        List<Point> l = all();
        l.removeAll(here());
        save(l);
    }

    @Override
    public void renderWorld(Fx fx, VertexConsumerProvider vcp) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        VertexConsumer vc = vcp.getBuffer(FxLayers.GLOW);
        int c = color.color();
        for (Point p : here()) {
            double dist = Math.sqrt(mc.player.squaredDistanceTo(p.x, p.y, p.z));
            if (dist > maxDistance.d()) {
                continue;
            }
            if (beam.on()) {
                fx.line(vc, p.x, p.y, p.z, p.x, p.y + 64, p.z, 0.12f, Colors.fade(c, 0.55f));
            }
            fx.ring(vc, p.x, p.y + 0.03, p.z, 0.6f, 0.08f, c, 28);
            String label = p.name + (distance.on() ? "  •  " + (dist >= 1000 ? String.format(Locale.ROOT, "%.1fkm", dist / 1000) : Math.round(dist) + "m") : "");
            float h = (float) Math.max(0.3, Math.min(8, dist * 0.03));
            WorldText.draw(fx, vcp, label, p.x, p.y + 2.2 + h, p.z, h, AquaFont.Weight.SEMIBOLD, 0xFFFFFFFF, c, 0.6f);
        }
    }
}
