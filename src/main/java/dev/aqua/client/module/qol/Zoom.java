package dev.aqua.client.module.qol;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;

/** Smooth, configurable zoom while the zoom key is held (default C). */
public final class Zoom extends Module {
    private final NumberSetting factor = add(new NumberSetting("factor", "Zoom", 4, 1.5, 12, 0.5).unit("x"));
    private final BoolSetting smooth = add(new BoolSetting("smooth", "Smooth transition", true));
    private final NumberSetting speed = add(new NumberSetting("speed", "Transition time", 160, 40, 600, 10).unit(" ms")
            .visibleWhen(this::isSmooth));
    private final BoolSetting scroll = add(new BoolSetting("scroll", "Scroll to adjust", true));
    private final BoolSetting sensitivity = add(new BoolSetting("sensitivity", "Lower mouse sensitivity", true));
    private final BoolSetting cinematic = add(new BoolSetting("cinematic", "Cinematic camera while zoomed", false));

    private final Anim anim = new Anim(1f, 160f);
    private float scrollFactor = 1f;
    private boolean wasHeld;
    private boolean cinematicBefore;

    public Zoom() {
        super("zoom", "Zoom", "Smooth zoom while holding the zoom key.", Category.QOL, true, "zoom", "spyglass", "optifine zoom");
        hideFromModuleList();
    }

    private boolean isSmooth() {
        return smooth.on();
    }

    public boolean held() {
        return isEnabled() && mc.currentScreen == null && Aqua.keybinds().zoom.isPressed();
    }

    @Override
    public void onTick() {
        boolean h = held();
        if (h && !wasHeld) {
            scrollFactor = 1f;
            if (cinematic.on()) {
                cinematicBefore = mc.options.smoothCameraEnabled;
                mc.options.smoothCameraEnabled = true;
            }
        } else if (!h && wasHeld && cinematic.on()) {
            mc.options.smoothCameraEnabled = cinematicBefore;
        }
        wasHeld = h;
    }

    @Override
    protected void onDisable() {
        if (wasHeld && cinematic.on()) {
            mc.options.smoothCameraEnabled = cinematicBefore;
        }
        wasHeld = false;
    }

    /** Multiplier applied to the vanilla field of view. */
    public float fovFactor() {
        if (!isEnabled()) {
            return 1f;
        }
        float target = held() ? 1f / Math.max(1f, factor.f() * scrollFactor) : 1f;
        anim.duration(smooth.on() ? speed.f() : 0f).target(target);
        return anim.get();
    }

    public float sensitivityFactor() {
        if (!isEnabled() || !sensitivity.on()) {
            return 1f;
        }
        float f = anim.get();
        return f < 0.999f ? Math.max(0.1f, f) : 1f;
    }

    public boolean onScroll(double amount) {
        if (!held() || !scroll.on() || amount == 0) {
            return false;
        }
        scrollFactor *= amount > 0 ? 1.2f : 1f / 1.2f;
        scrollFactor = Math.max(0.3f, Math.min(4f, scrollFactor));
        return true;
    }
}
