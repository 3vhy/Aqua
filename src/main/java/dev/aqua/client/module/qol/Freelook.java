package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.EnumSetting;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * Look around your player in third person while you keep moving and facing the same way. Only the
 * camera turns: your player's real direction (what the server and other players see) is unchanged.
 * The camera still stops at walls, so it never shows anything through blocks. Some servers do not
 * allow freelook; check their rules.
 */
public final class Freelook extends Module {
    public enum Mode implements EnumSetting.Displayable {
        HOLD("Hold key"), TOGGLE("Toggle");
        final String l;

        Mode(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("mode", "Key", Mode.HOLD));
    private final BoolSetting front = add(new BoolSetting("front", "View from the front", false)
            .describe("Start freelook looking at your player's face instead of from behind"));
    private final BoolSetting invert = add(new BoolSetting("invert", "Invert up and down", false));

    private boolean active;
    private boolean toggled;
    private float yaw;
    private float pitch;
    private Perspective previous;

    public Freelook() {
        super("freelook", "Freelook", "Hold Left Alt to look around your player while you keep moving the same way.", Category.QOL, true,
                "freelook", "free look", "perspective", "look around", "third person", "camera");
        hideFromModuleList();
    }

    /** True while the camera is turned independently of the player. */
    public static boolean on() {
        Freelook f = Aqua.modules() != null ? Aqua.modules().get(Freelook.class) : null;
        return f != null && f.active;
    }

    public static Freelook get() {
        return Aqua.modules().get(Freelook.class);
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    /** Mouse movement while freelook is on turns the camera instead of the player (same speed as vanilla). */
    public void turn(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) (invert.on() ? -dy : dy) * 0.15f, -90f, 90f);
    }

    @Override
    public void onTick() {
        boolean want = false;
        if (mc.player != null && mc.currentScreen == null) {
            if (mode.is(Mode.HOLD)) {
                want = Aqua.keybinds().freelook.isPressed();
                toggled = false;
            } else {
                while (Aqua.keybinds().freelook.wasPressed()) {
                    toggled = !toggled;
                }
                want = toggled;
            }
        } else if (mc.player == null) {
            toggled = false;
        }
        if (want && !active) {
            start();
        } else if (!want && active && (mode.is(Mode.HOLD) || mc.player == null || !toggled)) {
            stop();
        }
    }

    private void start() {
        Entity p = mc.player;
        active = true;
        yaw = p.getYaw() + (front.on() ? 180f : 0f);
        pitch = front.on() ? -p.getPitch() : p.getPitch();
        previous = mc.options.getPerspective();
        if (previous.isFirstPerson()) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    private void stop() {
        active = false;
        if (previous != null) {
            mc.options.setPerspective(previous);
            previous = null;
        }
    }

    @Override
    protected void onDisable() {
        if (active) {
            stop();
        }
        toggled = false;
    }
}
