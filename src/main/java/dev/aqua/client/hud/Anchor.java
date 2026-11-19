package dev.aqua.client.hud;

/** Nine-point screen anchor; widgets keep their offset from it when the window or GUI scale changes. */
public enum Anchor {
    TOP_LEFT(0, 0), TOP_CENTER(0.5f, 0), TOP_RIGHT(1, 0),
    MIDDLE_LEFT(0, 0.5f), CENTER(0.5f, 0.5f), MIDDLE_RIGHT(1, 0.5f),
    BOTTOM_LEFT(0, 1), BOTTOM_CENTER(0.5f, 1), BOTTOM_RIGHT(1, 1);

    public final float fx;
    public final float fy;

    Anchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }

    /** Picks the anchor whose region contains the widget's centre. */
    public static Anchor nearest(float cx, float cy, float sw, float sh) {
        int col = cx < sw / 3f ? 0 : (cx > sw * 2f / 3f ? 2 : 1);
        int row = cy < sh / 3f ? 0 : (cy > sh * 2f / 3f ? 2 : 1);
        return values()[row * 3 + col];
    }
}
