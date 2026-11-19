package dev.aqua.client.module.visual;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * First-person hand/item presentation (R1 "Swing Speed", R3 "Animations"): visual swing length,
 * swing style, equip animation, hand position/scale and an inspect animation. Purely cosmetic -
 * the attack cooldown, reach and hit detection are untouched.
 */
public final class Animations extends Module {
    public enum Swing implements EnumSetting.Displayable {
        VANILLA("Vanilla"), SMOOTH("Smooth"), PUNCH("Punch");
        final String l;

        Swing(String l) {
            this.l = l;
        }

        public String displayName() {
            return l;
        }
    }

    private final NumberSetting swingSpeed = add(new NumberSetting("swing_speed", "Swing length", 1.0, 0.5, 2.0, 0.05).unit("x")
            .describe("Visual only: >1 makes the swing animation slower, <1 faster"));
    private final EnumSetting<Swing> swing = add(new EnumSetting<>("swing", "Swing style", Swing.VANILLA));
    private final BoolSetting equip = add(new BoolSetting("equip", "Equip animation", true).describe("Lowering/raising when switching items"));
    private final NumberSetting x = add(new NumberSetting("x", "Hand X", 0, -0.5, 0.5, 0.01));
    private final NumberSetting y = add(new NumberSetting("y", "Hand Y", 0, -0.5, 0.5, 0.01));
    private final NumberSetting z = add(new NumberSetting("z", "Hand Z", 0, -0.5, 0.5, 0.01));
    private final NumberSetting scale = add(new NumberSetting("scale", "Hand scale", 1.0, 0.5, 1.5, 0.05).unit("x"));
    private final BoolSetting inspect = add(new BoolSetting("inspect", "Inspect animation", true)
            .describe("Press the Inspect key (default Y) to admire your held item"));

    private long inspectStart = -1;
    private ItemStack inspectStack = ItemStack.EMPTY;
    private int inspectSlot = -1;

    public Animations() {
        super("animations", "Animations", "Swing, equip, hand position and item inspect animations.", Category.VISUALS, false,
                "swing speed", "1.7 animations", "item position", "viewmodel", "inspect");
    }

    public int swingDuration(int vanilla) {
        return Math.max(1, Math.round(vanilla * swingSpeed.f()));
    }

    public float swingProgress(float p) {
        return switch (swing.get()) {
            case SMOOTH -> Anim.easeInOutCubic(p);
            case PUNCH -> p < 0.5f ? Anim.easeOutCubic(p * 2f) * 0.5f : 0.5f + (p - 0.5f);
            default -> p;
        };
    }

    public float equipProgress(float p) {
        return equip.on() ? p : 0f;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        while (Aqua.keybinds().inspect.wasPressed()) {
            if (inspect.on() && mc.currentScreen == null && !mc.player.getMainHandStack().isEmpty()) {
                inspectStart = System.currentTimeMillis();
                inspectStack = mc.player.getMainHandStack();
                inspectSlot = mc.player.getInventory().getSelectedSlot();
            }
        }
        // Cancel the inspect when the slot or item changes, the item is used, a menu opens or the perspective changes.
        if (inspectStart >= 0 && (mc.player.getInventory().getSelectedSlot() != inspectSlot
                || !ItemStack.areItemsEqual(inspectStack, mc.player.getMainHandStack())
                || mc.player.isUsingItem() || mc.player.handSwinging || mc.currentScreen != null
                || !mc.options.getPerspective().isFirstPerson())) {
            inspectStart = -1;
        }
    }

    /** Applies offsets, scale and the inspect pose to the first-person item transform. */
    public void apply(MatrixStack m, boolean mainHand) {
        m.translate(x.f() * (mainHand ? 1 : -1), y.f(), z.f());
        float s = scale.f();
        if (s != 1f) {
            m.scale(s, s, s);
        }
        if (mainHand && inspectStart >= 0) {
            float t = (System.currentTimeMillis() - inspectStart) / 2200f;
            if (t >= 1f) {
                inspectStart = -1;
                return;
            }
            float env = Math.min(1f, Math.min(t * 5f, (1f - t) * 5f));
            float e = Anim.easeInOutCubic(env);
            m.translate(-0.12f * e, 0.08f * e, 0.05f * e);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-35f * e + (float) Math.sin(t * Math.PI * 2) * 25f * e));
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(20f * e));
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) Math.sin(t * Math.PI) * 15f * e));
        }
    }
}
