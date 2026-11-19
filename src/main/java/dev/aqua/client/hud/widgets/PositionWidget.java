package dev.aqua.client.hud.widgets;

import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.hud.Pill;
import dev.aqua.client.module.qol.StreamerMode;
import dev.aqua.client.render.Icon;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.util.TextUtil;
import dev.aqua.client.Aqua;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** "x y z • server • 34.15 BPS" (Fern R1 0:00) with facing (R4 0:44) and biome. */
public final class PositionWidget extends HudWidget {
    public enum Format implements EnumSetting.Displayable {
        BLOCK("Block (609 -35 985)"), DECIMAL("Decimal (609.4 -35.0 985.1)");
        final String l;

        Format(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Format> format = add(new EnumSetting<>("format", "Coordinate format", Format.BLOCK));
    private final BoolSetting facing = add(new BoolSetting("facing", "Facing letter", false).describe("Prefix with N/E/S/W"));
    private final BoolSetting biome = add(new BoolSetting("biome", "Biome", false));
    private final BoolSetting server = add(new BoolSetting("server", "Server address", true));
    private final BoolSetting speed = add(new BoolSetting("speed", "Speed (blocks/s)", true));

    private Vec3d lastPos;
    private long lastTime;
    private double bps;
    private BlockPos biomePos;
    private String biomeName = "";

    public PositionWidget() {
        super("position", "Position", "Coordinates, facing, biome, server and speed.", Icon.GLOBE, true, Anchor.TOP_LEFT, 5, 21,
                "coordinates", "xyz", "biome", "speed", "bps", "server", "direction");
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        Vec3d p = mc.player.getEntityPos();
        long now = System.nanoTime();
        if (lastPos != null) {
            double dt = (now - lastTime) / 1.0e9;
            if (dt > 0) {
                double dx = p.x - lastPos.x;
                double dz = p.z - lastPos.z;
                double inst = Math.sqrt(dx * dx + dz * dz) / dt;
                bps = bps * 0.6 + inst * 0.4;
            }
        }
        lastPos = p;
        lastTime = now;
    }

    private String biomeName() {
        BlockPos bp = mc.player.getBlockPos();
        if (!bp.equals(biomePos)) {
            biomePos = bp;
            biomeName = mc.world.getBiome(bp).getKey().map(k -> TextUtil.titleCase(k.getValue().getPath())).orElse("Unknown");
        }
        return biomeName;
    }

    private Pill pill(boolean preview) {
        Pill p = Pill.create().icon(Icon.GLOBE);
        if (mc.player == null || mc.world == null) {
            p.value(preview ? "128 64 -256" : "N/A");
            if (server.on()) {
                p.dot().value(preview ? "play.example.net" : "N/A");
            }
            if (speed.on()) {
                p.dot().value("4.32").unit("BPS");
            }
            return p;
        }
        StreamerMode sm = Aqua.modules().get(StreamerMode.class);
        boolean hideCoords = sm != null && sm.isEnabled() && sm.hideCoordinates();
        String coords;
        if (hideCoords) {
            coords = "Hidden";
        } else if (format.is(Format.BLOCK)) {
            BlockPos b = mc.player.getBlockPos();
            coords = b.getX() + " " + b.getY() + " " + b.getZ();
        } else {
            Vec3d v = mc.player.getEntityPos();
            coords = TextUtil.fmt1(v.x) + " " + TextUtil.fmt1(v.y) + " " + TextUtil.fmt1(v.z);
        }
        if (facing.on()) {
            Direction d = mc.player.getHorizontalFacing();
            p.muted(d.getId().substring(0, 1).toUpperCase());
        }
        p.value(coords);
        if (biome.on()) {
            p.dot().value(biomeName());
        }
        if (server.on()) {
            String addr;
            if (mc.isInSingleplayer()) {
                addr = "Singleplayer";
            } else if (mc.getCurrentServerEntry() != null) {
                addr = mc.getCurrentServerEntry().address;
                if (sm != null && sm.isEnabled() && sm.hideServer()) {
                    addr = "Hidden";
                }
            } else {
                addr = "N/A";
            }
            p.dot().value(addr);
        }
        if (speed.on()) {
            p.dot().value(TextUtil.fmt2(bps)).unit("BPS");
        }
        return p;
    }

    @Override
    public float width(boolean preview) {
        return pill(preview).width();
    }

    @Override
    public float height(boolean preview) {
        return Pill.HEIGHT;
    }

    @Override
    public void draw(DrawContext ctx, boolean preview, float alpha) {
        pill(preview).draw(ctx, 0, 0, showBackground(), bg(alpha), alpha);
    }
}
