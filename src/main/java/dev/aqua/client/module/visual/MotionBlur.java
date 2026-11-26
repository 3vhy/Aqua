package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/**
 * Motion Blur (reference video): blends the last frames together while you turn, so fast turns
 * smear softly. Standing still or walking straight stays sharp.
 */
public final class MotionBlur extends Module {
    private final NumberSetting strength = add(new NumberSetting("strength", "Strength", 0.6, 0.1, 0.9, 0.05));
    private float lastYaw = Float.NaN;
    private float lastPitch;
    private float amount;

    public MotionBlur() {
        super("motion_blur", "Motion Blur", "Adds motion blur when you turn.", Category.VISUALS, false, "motion blur", "blur", "smooth");
    }

    /** How much of the previous frame to keep this frame (0 = none). Called once per frame. */
    public float frameAmount() {
        if (mc.player == null) {
            return 0f;
        }
        float yaw = mc.gameRenderer.getCamera().getYaw(), pitch = mc.gameRenderer.getCamera().getPitch();
        float turn = 0f;
        if (!Float.isNaN(lastYaw)) {
            float dy = Math.abs(((yaw - lastYaw) % 360 + 540) % 360 - 180);
            turn = dy + Math.abs(pitch - lastPitch);
        }
        lastYaw = yaw;
        lastPitch = pitch;
        float target = Math.min(1f, turn / 6f) * strength.f();
        amount += (target - amount) * 0.5f;
        return amount;
    }

    @Override
    protected void onDisable() {
        lastYaw = Float.NaN;
        amount = 0f;
    }
}
