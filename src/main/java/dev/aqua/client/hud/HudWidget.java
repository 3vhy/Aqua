package dev.aqua.client.hud;

import com.google.gson.JsonObject;
import dev.aqua.client.Aqua;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;

/** A draggable HUD element. Layout values are part of the active profile. */
public abstract class HudWidget extends Module {
    private final Anchor defaultAnchor;
    private final float defaultX;
    private final float defaultY;
    private final Icon icon;

    private Anchor anchor;
    private float offX;
    private float offY;
    private float scale = 1f;
    private float opacity = 1f;
    private boolean locked;

    // Cached screen rectangle from the last layout pass (GUI units, scaled size).
    public float sx, sy, sw, sh;

    protected final BoolSetting background;
    protected final NumberSetting bgOpacity;

    protected HudWidget(String id, String name, String description, Icon icon, boolean enabledByDefault,
                        Anchor anchor, float x, float y, String... keywords) {
        super(id, name, description, Category.HUD, enabledByDefault, keywords);
        this.icon = icon;
        this.defaultAnchor = anchor;
        this.defaultX = x;
        this.defaultY = y;
        this.anchor = anchor;
        this.offX = x;
        this.offY = y;
        hideFromModuleList();
        background = add(new BoolSetting("background", "Background", true).describe("Draw the translucent capsule behind this widget"));
        bgOpacity = add(new NumberSetting("bg_opacity", "Background opacity", 0.72, 0.0, 1.0, 0.02)
                .visibleWhen(background::on));
    }

    public Icon icon() {
        return icon;
    }

    /** Unscaled content width (GUI units). */
    public abstract float width(boolean preview);

    /** Unscaled content height (GUI units). */
    public abstract float height(boolean preview);

    /** Draws at local origin (0,0); the manager applies translation, scale and opacity. */
    public abstract void draw(DrawContext ctx, boolean preview, float alpha);

    /** Whether the widget currently has anything to show outside the editor. */
    public boolean hasContent() {
        return true;
    }

    public boolean showBackground() {
        return background.on() && Aqua.hud().backgrounds();
    }

    public int bg(float alpha) {
        return Colors.fade(Aqua.theme().hudBg((float) bgOpacity.d()), alpha * opacity);
    }

    public Anchor anchor() {
        return anchor;
    }

    public float offX() {
        return offX;
    }

    public float offY() {
        return offY;
    }

    public float scale() {
        return scale;
    }

    public float opacity() {
        return opacity;
    }

    public boolean locked() {
        return locked;
    }

    public void setLocked(boolean l) {
        locked = l;
        Aqua.config().markDirty();
    }

    public void setScale(float s) {
        scale = Math.max(0.4f, Math.min(3f, Math.round(s * 20f) / 20f));
        Aqua.config().markDirty();
    }

    public void setOpacity(float o) {
        opacity = Math.max(0.1f, Math.min(1f, o));
        Aqua.config().markDirty();
    }

    public void setLayout(Anchor a, float x, float y) {
        anchor = a;
        offX = x;
        offY = y;
        Aqua.config().markDirty();
    }

    /** Effective scale including the global HUD scale. */
    public float totalScale() {
        return scale * Aqua.hud().globalScale();
    }

    /** Computes the on-screen rectangle, clamped to stay visible. */
    public void layout(float screenW, float screenH, boolean preview) {
        float s = totalScale();
        float w = width(preview) * s;
        float h = height(preview) * s;
        float x = anchor.fx * screenW + offX - anchor.fx * w;
        float y = anchor.fy * screenH + offY - anchor.fy * h;
        x = Math.max(0, Math.min(screenW - w, x));
        y = Math.max(0, Math.min(screenH - h, y));
        sx = x;
        sy = y;
        sw = w;
        sh = h;
    }

    /** Converts an absolute top-left position into anchor-relative offsets (choosing the nearest anchor). */
    public void moveTo(float x, float y, float screenW, float screenH) {
        Anchor a = Anchor.nearest(x + sw / 2f, y + sh / 2f, screenW, screenH);
        float ox = x - a.fx * screenW + a.fx * sw;
        float oy = y - a.fy * screenH + a.fy * sh;
        setLayout(a, ox, oy);
    }

    public void resetLayout() {
        anchor = defaultAnchor;
        offX = defaultX;
        offY = defaultY;
        scale = 1f;
        opacity = 1f;
        locked = false;
        Aqua.config().markDirty();
    }

    @Override
    public void resetToDefaults() {
        super.resetToDefaults();
        resetLayout();
    }

    public JsonObject layoutJson() {
        JsonObject o = new JsonObject();
        o.addProperty("anchor", anchor.name());
        o.addProperty("x", offX);
        o.addProperty("y", offY);
        o.addProperty("scale", scale);
        o.addProperty("opacity", opacity);
        o.addProperty("locked", locked);
        return o;
    }

    public void layoutFromJson(JsonObject o) {
        try {
            Anchor a = Anchor.valueOf(o.get("anchor").getAsString());
            float x = o.get("x").getAsFloat();
            float y = o.get("y").getAsFloat();
            if (Float.isFinite(x) && Float.isFinite(y) && Math.abs(x) < 10000 && Math.abs(y) < 10000) {
                anchor = a;
                offX = x;
                offY = y;
            }
            if (o.has("scale")) {
                float s = o.get("scale").getAsFloat();
                if (Float.isFinite(s)) {
                    scale = Math.max(0.4f, Math.min(3f, s));
                }
            }
            if (o.has("opacity")) {
                float op = o.get("opacity").getAsFloat();
                if (Float.isFinite(op)) {
                    opacity = Math.max(0.1f, Math.min(1f, op));
                }
            }
            if (o.has("locked")) {
                locked = o.get("locked").getAsBoolean();
            }
        } catch (Exception ignored) {
            // keep defaults
        }
    }
}
