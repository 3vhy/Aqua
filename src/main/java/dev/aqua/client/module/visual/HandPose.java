package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Hand Pose (reference video): where and how your first-person hand and item are held. */
public final class HandPose extends Module {
    private final NumberSetting x = add(new NumberSetting("x", "Left / right", 0, -0.6, 0.6, 0.01));
    private final NumberSetting y = add(new NumberSetting("y", "Down / up", 0, -0.6, 0.6, 0.01));
    private final NumberSetting z = add(new NumberSetting("z", "Back / forward", 0, -0.6, 0.6, 0.01));
    private final NumberSetting pitch = add(new NumberSetting("pitch", "Tilt", 0, -90, 90, 1).unit("°"));
    private final NumberSetting yaw = add(new NumberSetting("yaw", "Turn", 0, -90, 90, 1).unit("°"));
    private final NumberSetting roll = add(new NumberSetting("roll", "Roll", 0, -90, 90, 1).unit("°"));
    private final NumberSetting scale = add(new NumberSetting("scale", "Size", 1.0, 0.4, 1.6, 0.05).unit("x"));
    private final BoolSetting offhand = add(new BoolSetting("offhand", "Mirror for off-hand", true));

    public HandPose() {
        super("hand_pose", "Hand Pose", "Moves, turns and resizes your first-person hand and item.", Category.VISUALS, false,
                "hand", "viewmodel", "item position", "pose", "hand position");
    }

    public void apply(MatrixStack m, boolean mainHand) {
        if (!isEnabled() || (!mainHand && !offhand.on())) {
            return;
        }
        float side = mainHand ? 1 : -1;
        m.translate(x.f() * side, y.f(), -z.f());
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch.f()));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw.f() * side));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll.f() * side));
        float s = scale.f();
        if (s != 1f) {
            m.scale(s, s, s);
        }
    }
}
