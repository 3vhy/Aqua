package dev.aqua.client.hud;

import com.google.gson.JsonObject;
import dev.aqua.client.Aqua;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Owns HUD widgets, the global HUD options and HUD layout persistence. */
public final class HudManager {
    private final List<HudWidget> widgets = new ArrayList<>();
    /** Set when the layout is fresh (new profile / reset); the first frame arranges the Fern cluster. */
    public boolean needsArrange = true;
    private final List<Setting<?>> settings = new ArrayList<>();
    public final NumberSetting scale = add(new NumberSetting("scale", "HUD scale", 1.0, 0.5, 2.0, 0.05).unit("x"));
    public final BoolSetting backgrounds = add(new BoolSetting("backgrounds", "Element backgrounds", true));
    public final BoolSetting hideInDebug = add(new BoolSetting("hide_debug", "Hide while F3 is open", true));
    public final BoolSetting snapping = add(new BoolSetting("snapping", "Editor snapping", true));
    public final NumberSetting grid = add(new NumberSetting("grid", "Editor grid size", 8, 4, 32, 1).unit(" px"));

    private <S extends Setting<?>> S add(S s) {
        settings.add(s);
        s.setDirtyHook(() -> Aqua.config().markDirty());
        return s;
    }

    public List<Setting<?>> settings() {
        return settings;
    }

    public void register(HudWidget w) {
        widgets.add(w);
    }

    public List<HudWidget> widgets() {
        return Collections.unmodifiableList(widgets);
    }

    public float globalScale() {
        return scale.f();
    }

    public boolean backgrounds() {
        return backgrounds.on();
    }

    /** Renders every enabled widget; called from the HUD layer. */
    public void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) {
            return;
        }
        if (hideInDebug.on() && mc.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        if (mc.currentScreen instanceof dev.aqua.client.hud.editor.HudEditorScreen) {
            return;
        }
        float sw = ctx.getScaledWindowWidth();
        float sh = ctx.getScaledWindowHeight();
        if (needsArrange) {
            dev.aqua.client.hud.editor.HudEditorScreen.autoArrange();
        }
        // The vanilla tab list is drawn at the top centre; widgets there step aside while it is open.
        boolean tab = mc.options.playerListKey.isPressed() && mc.currentScreen == null && mc.world != null;
        float tabFade = tabAnim.target(tab).get();
        for (HudWidget w : widgets) {
            if (!w.isEnabled() || !w.hasContent()) {
                continue;
            }
            float a = 1f;
            if (tabFade > 0.001f) {
                w.layout(sw, sh, false);
                boolean topCentre = w.sy < sh * 0.25f && w.sx < sw / 2f + 40 && w.sx + w.sw > sw / 2f - 40;
                if (topCentre) {
                    a = 1f - tabFade;
                    if (a <= 0.01f) {
                        continue;
                    }
                }
            }
            drawWidget(ctx, w, sw, sh, false, a);
        }
    }

    private final dev.aqua.client.render.Anim tabAnim = new dev.aqua.client.render.Anim(0f, 140f);

    public void drawWidget(DrawContext ctx, HudWidget w, float sw, float sh, boolean preview, float alpha) {
        try {
            w.layout(sw, sh, preview);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(w.sx, w.sy);
            float s = w.totalScale();
            ctx.getMatrices().scale(s, s);
            w.draw(ctx, preview, alpha * w.opacity());
            ctx.getMatrices().popMatrix();
        } catch (Throwable t) {
            ctx.getMatrices().popMatrix();
            dev.aqua.client.AquaClient.LOGGER.error("HUD widget {} failed to render; disabling it", w.id(), t);
            w.setEnabled(false, false);
        }
    }

    public void resetAll() {
        for (Setting<?> s : settings) {
            s.reset();
        }
        for (HudWidget w : widgets) {
            w.resetLayout();
        }
        needsArrange = true;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonObject g = new JsonObject();
        for (Setting<?> s : settings) {
            g.add(s.id(), s.toJson());
        }
        o.add("options", g);
        JsonObject l = new JsonObject();
        for (HudWidget w : widgets) {
            l.add(w.id(), w.layoutJson());
        }
        o.add("layout", l);
        o.addProperty("arranged", !needsArrange);
        return o;
    }

    public void fromJson(JsonObject o) {
        if (o.has("options") && o.get("options").isJsonObject()) {
            JsonObject g = o.getAsJsonObject("options");
            for (Setting<?> s : settings) {
                if (g.has(s.id())) {
                    s.fromJson(g.get(s.id()));
                }
            }
        }
        if (o.has("layout") && o.get("layout").isJsonObject()) {
            needsArrange = o.has("arranged") && !o.get("arranged").getAsBoolean();
            JsonObject l = o.getAsJsonObject("layout");
            for (HudWidget w : widgets) {
                if (l.has(w.id()) && l.get(w.id()).isJsonObject()) {
                    w.layoutFromJson(l.getAsJsonObject(w.id()));
                }
            }
        }
    }

    /** Small "SAMPLE" tag drawn on widgets showing preview data in the editor. */
    public static void sampleTag(DrawContext ctx, HudWidget w) {
        float tw = dev.aqua.client.render.font.AquaFont.width("SAMPLE", 4.2f, dev.aqua.client.render.font.AquaFont.Weight.BOLD);
        float x = w.sx + w.sw - tw - 5;
        float y = w.sy - 7.5f;
        if (y < 0) {
            y = w.sy + w.sh + 1.5f;
        }
        dev.aqua.client.render.R2D.round(ctx, x - 2.5f, y, tw + 5, 6.5f, 2.5f, Colors.withAlpha(Aqua.theme().accent(), 0.9f));
        dev.aqua.client.render.R2D.textMid(ctx, "SAMPLE", x, y + 3.25f, 4.2f, dev.aqua.client.render.font.AquaFont.Weight.BOLD,
                Aqua.theme().onAccent());
        Anim.clamp01(0);
    }
}
