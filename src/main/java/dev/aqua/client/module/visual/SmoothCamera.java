package dev.aqua.client.module.visual;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/**
 * Smooth Camera (R6 "SmoothCamera"): mouse movement eases in and out instead of snapping. Every bit
 * of mouse movement is still applied - it is only spread over a few frames - so aim is not changed.
 */
public final class SmoothCamera extends Module {
    private final NumberSetting smoothness = add(new NumberSetting("smoothness", "Smoothness", 0.4, 0.05, 1.0, 0.05)
            .describe("Higher is smoother and slower to settle"));

    private double pendingX;
    private double pendingY;

    public SmoothCamera() {
        super("smooth_camera", "Smooth Camera", "Eases your mouse movement for a cinematic camera.", Category.VISUALS, false,
                "smooth camera", "smoothcamera", "cinematic", "mouse smoothing", "smooth");
    }

    /** Takes this frame's raw mouse movement and returns the part to apply now; the rest follows over the next frames. */
    private double totalIn;
    private double totalOut;

    /** Total mouse movement received and passed on (used by the in-game tests). */
    public double[] totals() {
        return new double[]{totalIn, totalOut};
    }

    public double[] smooth(double dx, double dy, double seconds) {
        totalIn += Math.abs(dx);
        pendingX += dx;
        pendingY += dy;
        double speed = 4 + (1 - smoothness.d()) * 40;
        double k = 1 - Math.exp(-seconds * speed);
        double outX = pendingX * k, outY = pendingY * k;
        totalOut += Math.abs(outX);
        pendingX -= outX;
        pendingY -= outY;
        if (Math.abs(pendingX) < 1e-3) {
            pendingX = 0;
        }
        if (Math.abs(pendingY) < 1e-3) {
            pendingY = 0;
        }
        return new double[]{outX, outY};
    }

    public void reset() {
        pendingX = 0;
        pendingY = 0;
    }

    @Override
    protected void onDisable() {
        reset();
    }
}
