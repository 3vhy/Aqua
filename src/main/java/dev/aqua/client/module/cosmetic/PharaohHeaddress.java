package dev.aqua.client.module.cosmetic;

import dev.aqua.client.cosmetic.Mesh;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.BoolSetting;

/**
 * Pharaoh Headdress (reference video): a striped royal headdress with a gold band, side flaps and a
 * snake emblem, drawn on your own player model. Only you see it.
 */
public final class PharaohHeaddress extends Module {
    private final ColorSetting stripe = add(new ColorSetting("stripe", "Stripe colour", 0xFF1F4FB8, false, false));
    private final ColorSetting gold = add(new ColorSetting("gold", "Gold colour", 0xFFE8B83A, false, false));
    private final BoolSetting emblem = add(new BoolSetting("emblem", "Snake emblem", true));
    private static long drawn;

    public PharaohHeaddress() {
        super("pharaoh_headdress", "Pharaoh Headdress", "Puts a pharaoh headdress on your player model.", Category.COSMETICS, false,
                "pharaoh", "headdress", "egypt", "hat", "nemes");
    }

    public static long drawn() {
        return drawn;
    }

    /** Head space, pixels (head spans x -4..4, y -8..0, z -4..4; front is -z). */
    public void draw(Mesh m, float t) {
        drawn++;
        int s = stripe.color(), g = gold.color();
        int dark = Colors.darken(s, 0.25f);
        // Cap over the head: alternating stripes front to back.
        for (int i = 0; i < 9; i++) {
            float z0 = -4.6f + i * 1.02f;
            int c = i % 2 == 0 ? g : s;
            m.box(-4.6f, -8.7f, z0, 4.6f, -8.2f, z0 + 1.02f, c);
        }
        // Sides of the cap down to the ears.
        for (int i = 0; i < 6; i++) {
            float y0 = -8.2f + i * 1.05f;
            int c = i % 2 == 0 ? s : g;
            m.box(-4.65f, y0, -4.2f, -4.15f, y0 + 1.05f, 4.6f, c);
            m.box(4.15f, y0, -4.2f, 4.65f, y0 + 1.05f, 4.6f, c);
        }
        // Back cloth.
        for (int i = 0; i < 8; i++) {
            float y0 = -8.2f + i * 1.05f;
            m.box(-4.6f, y0, 4.15f, 4.6f, y0 + 1.05f, 4.65f, i % 2 == 0 ? s : g);
        }
        // Gold band across the forehead.
        m.box(-4.7f, -6.4f, -4.75f, 4.7f, -5.6f, -4.15f, g);
        // Side flaps (lappets) falling in front of the shoulders, widening downwards.
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 7; i++) {
                float y0 = -2.2f + i * 1.0f;
                float w = 1.6f + i * 0.18f;
                float x0 = side < 0 ? -4.7f : 4.7f - w;
                int c = i % 2 == 0 ? g : s;
                m.box(x0, y0, -3.2f, x0 + w, y0 + 1.0f, -1.6f, c);
            }
            float xe = side < 0 ? -4.7f : 3.2f;
            m.box(xe, 4.8f, -3.25f, xe + 1.5f, 5.3f, -1.55f, dark);
        }
        if (emblem.on()) {
            m.box(-0.45f, -8.2f, -5.2f, 0.45f, -5.8f, -4.7f, g);
            m.box(-0.9f, -8.9f, -5.3f, 0.9f, -8.2f, -4.8f, g);
            m.glow(-0.3f, -8.7f, -5.45f, 0.3f, -8.3f, -5.3f, 0xFFFF4D4D);
        }
    }
}
