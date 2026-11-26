package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ActionSetting;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.KeySetting;

import java.util.Locale;

/** Coord Snapper (reference video): copies your coordinates to the clipboard with a key. */
public final class CoordSnapper extends Module {
    public enum Format implements EnumSetting.Displayable {
        BLOCK("Block (x y z)"), PRECISE("Precise (x.x y.y z.z)"), COMMAS("With commas");
        final String l;

        Format(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final KeySetting key = add(new KeySetting("key", "Copy key"));
    private final EnumSetting<Format> format = add(new EnumSetting<>("format", "Format", Format.BLOCK));
    private final BoolSetting dimension = add(new BoolSetting("dimension", "Include dimension", false));
    private final ActionSetting now = add(new ActionSetting("copy_now", "Copy now", "Copy", this::copy));

    public CoordSnapper() {
        super("coord_snapper", "Coord Snapper", "Copies your coordinates to the clipboard with a key.", Category.QOL, false,
                "coordinates", "coords", "copy", "clipboard", "position");
    }

    @Override
    public void onKeySetting(KeySetting k) {
        if (k == key) {
            copy();
        }
    }

    /** Text for the current position. */
    public String text() {
        if (mc.player == null) {
            return "";
        }
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        String s = switch (format.get()) {
            case BLOCK -> (int) Math.floor(x) + " " + (int) Math.floor(y) + " " + (int) Math.floor(z);
            case PRECISE -> String.format(Locale.ROOT, "%.1f %.1f %.1f", x, y, z);
            case COMMAS -> (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z);
        };
        if (dimension.on() && mc.world != null) {
            s += " (" + mc.world.getRegistryKey().getValue().getPath().replace('_', ' ') + ")";
        }
        return s;
    }

    public void copy() {
        String t = text();
        if (t.isEmpty()) {
            return;
        }
        mc.keyboard.setClipboard(t);
        Aqua.notifications().info("Coordinates copied", t);
    }
}
