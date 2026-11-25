package dev.aqua.client.hud.editor;

import dev.aqua.client.Aqua;
import dev.aqua.client.gui.Ui;
import dev.aqua.client.gui.UiSounds;
import dev.aqua.client.hud.Anchor;
import dev.aqua.client.hud.HudManager;
import dev.aqua.client.hud.HudWidget;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Glass;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Drag-and-drop HUD layout editor with snapping to screen edges, centres, a grid and other
 * widgets; alignment guides; per-widget scale, opacity, anchor, lock, visibility and reset; and
 * clearly marked SAMPLE data for widgets that have nothing to show right now.
 */
public final class HudEditorScreen extends Screen {
    private static final float SNAP = 4f;
    private final Screen parent;
    private final Ui ui = new Ui();
    @Nullable private HudWidget selected;
    @Nullable private HudWidget dragging;
    private float grabX;
    private float grabY;
    private float dragX;
    private float dragY;
    private boolean moved;
    private boolean showHidden;
    private final List<float[]> guides = new ArrayList<>();
    private final Anim intro = new Anim(0f, 200f);
    private final NumberSetting scaleProxy = new NumberSetting("scale", "Scale", 1, 0.4, 3, 0.05);
    private final NumberSetting opacityProxy = new NumberSetting("opacity", "Opacity", 1, 0.1, 1, 0.05);

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Aqua HUD Editor"));
        this.parent = parent;
        intro.target(1f);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        Aqua.config().save();
        client.setScreen(parent);
    }

    private boolean visibleInEditor(HudWidget w) {
        return w.isEnabled() || showHidden;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Keep the world visible; only a light dim so widgets read clearly.
        if (client.world == null) {
            renderPanoramaBackground(context, deltaTicks);
            context.fill(0, 0, width, height, 0x60000000);
        } else {
            context.fill(0, 0, width, height, 0x40000000);
        }
        Glass.backdrop(context, client.world == null && !Glass.on());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Theme t = Aqua.theme();
        ui.begin(ctx, mouseX, mouseY);
        float sw = width;
        float sh = height;
        float ia = intro.get();

        // Grid + centre lines
        if (Aqua.hud().snapping.on()) {
            float g = Aqua.hud().grid.f();
            int gc = Colors.withAlpha(0xFFFFFF, Math.round(14 * ia));
            for (float x = 0; x < sw; x += g) {
                R2D.rect(ctx, x, 0, 0.5f, sh, gc);
            }
            for (float y = 0; y < sh; y += g) {
                R2D.rect(ctx, 0, y, sw, 0.5f, gc);
            }
        }
        R2D.rect(ctx, sw / 2f - 0.25f, 0, 0.5f, sh, Colors.withAlpha(t.accent(), Math.round(40 * ia)));
        R2D.rect(ctx, 0, sh / 2f - 0.25f, sw, 0.5f, Colors.withAlpha(t.accent(), Math.round(40 * ia)));

        // Drag handling
        guides.clear();
        if (dragging != null) {
            if (ui.down) {
                float nx = ui.mx - grabX;
                float ny = ui.my - grabY;
                if (Math.abs(nx - dragging.sx) > 0.5f || Math.abs(ny - dragging.sy) > 0.5f) {
                    moved = true;
                }
                boolean free = hasAltDown();
                if (Aqua.hud().snapping.on() && !free) {
                    float[] snapped = snap(dragging, nx, ny, sw, sh);
                    nx = snapped[0];
                    ny = snapped[1];
                }
                nx = Math.max(0, Math.min(sw - dragging.sw, nx));
                ny = Math.max(0, Math.min(sh - dragging.sh, ny));
                dragX = nx;
                dragY = ny;
                dragging.moveTo(nx, ny, sw, sh);
            } else {
                dragging = null;
            }
        }

        // Widgets
        HudWidget hovered = null;
        for (HudWidget w : Aqua.hud().widgets()) {
            if (!visibleInEditor(w)) {
                continue;
            }
            float alpha = w.isEnabled() ? 1f : 0.35f;
            Aqua.hud().drawWidget(ctx, w, sw, sh, true, alpha * ia);
            boolean hov = ui.mx >= w.sx && ui.my >= w.sy && ui.mx < w.sx + w.sw && ui.my < w.sy + w.sh;
            if (hov && dragging == null && !overToolbar()) {
                hovered = w;
            }
        }
        for (HudWidget w : Aqua.hud().widgets()) {
            if (!visibleInEditor(w)) {
                continue;
            }
            boolean sel = w == selected;
            boolean hov = w == hovered;
            int c = sel ? t.accent() : (hov ? Colors.withAlpha(t.text(), 0.7f) : Colors.withAlpha(t.text(), 0.22f));
            R2D.outline(ctx, w.sx - 1.5f, w.sy - 1.5f, w.sw + 3, w.sh + 3, 3.5f, sel ? 0.9f : 0.6f, c);
            if (w.locked()) {
                R2D.icon(ctx, Icon.LOCK, w.sx + w.sw - 6, w.sy - 7, 6, t.muted());
            }
            if (!w.hasContent() && w.isEnabled()) {
                HudManager.sampleTag(ctx, w);
            }
            if (sel) {
                String label = w.name() + "  •  " + anchorLabel(w.anchor());
                float tw = AquaFont.width(label, 4.6f, AquaFont.Weight.SEMIBOLD) + 6;
                float ly = w.sy + w.sh + 3;
                if (ly + 8 > sh) {
                    ly = w.sy - 10;
                }
                Glass.popup(ctx, w.sx, ly, tw, 7.5f, 2.5f, 1f);
                R2D.textMid(ctx, label, w.sx + 3, ly + 3.75f, 4.6f, AquaFont.Weight.SEMIBOLD, t.text());
            }
        }
        for (float[] g : guides) {
            if (g[0] == 0) {
                R2D.rect(ctx, g[1] - 0.35f, 0, 0.7f, sh, Colors.withAlpha(t.accent(), 0.9f));
            } else {
                R2D.rect(ctx, 0, g[1] - 0.35f, sw, 0.7f, Colors.withAlpha(t.accent(), 0.9f));
            }
        }

        toolbar(ctx, t, sw);
        if (selected != null) {
            inspector(ctx, t, sw, sh);
        }

        // Click selection / start drag
        if (ui.clicked && hovered != null) {
            ui.clicked = false;
            selected = hovered;
            UiSounds.click(1.05f);
            if (!hovered.locked()) {
                dragging = hovered;
                grabX = ui.mx - hovered.sx;
                grabY = ui.my - hovered.sy;
                moved = false;
            }
        } else if (ui.clicked && !overToolbar() && !overInspector()) {
            ui.clicked = false;
            selected = null;
        }
        if (ui.scroll != 0 && hovered != null && !hovered.locked()) {
            hovered.setScale(hovered.scale() + Math.signum(ui.scroll) * 0.05f);
            ui.scroll = 0;
        }

        for (KeyInput k : ui.keys) {
            handleKey(k);
        }
        ui.keys.clear();
        ui.drawTooltip(sw, sh);
        ui.end();
    }

    private float tbX, tbY, tbW, tbH;
    private float inX, inY, inW, inH;

    private boolean overToolbar() {
        return ui.mx >= tbX && ui.my >= tbY && ui.mx < tbX + tbW && ui.my < tbY + tbH;
    }

    private boolean overInspector() {
        return selected != null && ui.mx >= inX && ui.my >= inY && ui.mx < inX + inW && ui.my < inY + inH;
    }

    private void toolbar(DrawContext ctx, Theme t, float sw) {
        tbW = 238;
        tbH = 18;
        tbX = (sw - tbW) / 2f;
        tbY = 4;
        Glass.window(ctx, tbX, tbY, tbW, tbH, 7, 1f);
        float x = tbX + 4;
        float y = tbY + 3.5f;
        if (ui.button("he_done", x, y, 34, 11, "Done", Icon.CHECK, true)) {
            close();
            return;
        }
        x += 37;
        if (ui.button("he_fern", x, y, 50, 11, "Auto layout", Icon.GRID, false)) {
            autoArrange();
            Aqua.notifications().info("HUD", "Widgets arranged");
        }
        x += 53;
        if (ui.button("he_reset", x, y, 44, 11, "Reset all", Icon.RESET, false)) {
            for (HudWidget w : Aqua.hud().widgets()) {
                w.resetLayout();
            }
            autoArrange();
            Aqua.notifications().info("HUD", "Layout restored to defaults");
        }
        x += 48;
        boolean snap = Aqua.hud().snapping.on();
        R2D.textMid(ctx, "Snap", x, y + 5.5f, 5f, AquaFont.Weight.MEDIUM, t.muted());
        boolean ns = ui.toggle("he_snap", x + 15, y + 0.75f, snap);
        if (ns != snap) {
            Aqua.hud().snapping.set(ns);
        }
        x += 36;
        R2D.textMid(ctx, "Hidden", x, y + 5.5f, 5f, AquaFont.Weight.MEDIUM, t.muted());
        showHidden = ui.toggle("he_hidden", x + 20, y + 0.75f, showHidden);
    }

    private void inspector(DrawContext ctx, Theme t, float sw, float sh) {
        HudWidget w = selected;
        inW = 150;
        inH = 74;
        inX = sw - inW - 6;
        inY = sh - inH - 6;
        if (w.sx + w.sw > inX - 4 && w.sy + w.sh > inY - 4) {
            inX = 6;
        }
        Glass.window(ctx, inX, inY, inW, inH, 8, 1f);
        float x = inX + 7;
        float y = inY + 6;
        R2D.icon(ctx, w.icon(), x, y, 7, t.accent());
        R2D.textMid(ctx, w.name(), x + 10, y + 3.5f, 6f, AquaFont.Weight.SEMIBOLD, t.text());
        boolean en = ui.toggle("he_vis", inX + inW - 24, y - 1, w.isEnabled());
        if (en != w.isEnabled()) {
            w.setEnabled(en, false);
        }
        y += 12;
        R2D.textMid(ctx, "Scale", x, y + 4.5f, 5.2f, AquaFont.Weight.MEDIUM, t.muted());
        scaleProxy.set((double) w.scale());
        if (ui.slider("he_scale", x + 34, y, inW - 68, scaleProxy)) {
            w.setScale(scaleProxy.f());
        }
        R2D.textRight(ctx, String.format("%.2fx", w.scale()), inX + inW - 7, y + 4.5f, 5f, AquaFont.Weight.MEDIUM, t.text());
        y += 11;
        R2D.textMid(ctx, "Opacity", x, y + 4.5f, 5.2f, AquaFont.Weight.MEDIUM, t.muted());
        opacityProxy.set((double) w.opacity());
        if (ui.slider("he_opacity", x + 34, y, inW - 68, opacityProxy)) {
            w.setOpacity(opacityProxy.f());
        }
        R2D.textRight(ctx, Math.round(w.opacity() * 100) + "%", inX + inW - 7, y + 4.5f, 5f, AquaFont.Weight.MEDIUM, t.text());
        y += 12;
        R2D.textMid(ctx, "Anchor", x, y + 5.5f, 5.2f, AquaFont.Weight.MEDIUM, t.muted());
        // 3x3 anchor picker
        float ax = x + 34;
        for (Anchor a : Anchor.values()) {
            int i = a.ordinal();
            float cx = ax + (i % 3) * 6.5f;
            float cy = y + (i / 3) * 4f;
            boolean sel = w.anchor() == a;
            R2D.round(ctx, cx, cy, 5.5f, 3.2f, 1, sel ? t.accent() : Colors.brighten(t.elevated(), 0.15f));
            if (ui.clickedIn(cx, cy, 5.5f, 3.2f)) {
                float ox = w.sx - a.fx * sw + a.fx * w.sw;
                float oy = w.sy - a.fy * sh + a.fy * w.sh;
                w.setLayout(a, ox, oy);
                UiSounds.click(1f);
            }
        }
        boolean bg = w.settings().isEmpty() ? true : (Boolean) w.settings().get(0).get();
        R2D.textMid(ctx, "Background", ax + 26, y + 5.5f, 5.2f, AquaFont.Weight.MEDIUM, t.muted());
        @SuppressWarnings("unchecked")
        dev.aqua.client.setting.Setting<Boolean> bgSetting = (dev.aqua.client.setting.Setting<Boolean>) w.settings().get(0);
        boolean nb = ui.toggle("he_bg", inX + inW - 24, y + 1, bg);
        if (nb != bg) {
            bgSetting.set(nb);
        }
        y += 16;
        float bw = (inW - 14 - 4) / 3f;
        if (ui.button("he_lock", x, y, bw, 11, w.locked() ? "Unlock" : "Lock", w.locked() ? Icon.UNLOCK : Icon.LOCK, false)) {
            w.setLocked(!w.locked());
        }
        if (ui.button("he_rw", x + bw + 2, y, bw, 11, "Reset", Icon.RESET, false)) {
            w.resetLayout();
        }
        if (ui.button("he_center", x + (bw + 2) * 2, y, bw, 11, "Centre", Icon.MOVE, false)) {
            w.layout(sw, sh, true);
            w.moveTo((sw - w.sw) / 2f, w.sy, sw, sh);
        }
    }

    private static String anchorLabel(Anchor a) {
        String s = a.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Snaps a candidate top-left to edges, centres, grid and other widgets; records guides. */
    private float[] snap(HudWidget w, float x, float y, float sw, float sh) {
        float bestDx = SNAP + 1;
        float bestDy = SNAP + 1;
        float rx = x;
        float ry = y;
        float gx = Float.NaN;
        float gy = Float.NaN;
        List<Float> xs = new ArrayList<>();
        List<Float> ys = new ArrayList<>();
        float margin = 4;
        xs.add(margin);
        xs.add(sw - margin);
        xs.add(sw / 2f);
        ys.add(margin);
        ys.add(sh - margin);
        ys.add(sh / 2f);
        for (HudWidget o : Aqua.hud().widgets()) {
            if (o == w || !visibleInEditor(o)) {
                continue;
            }
            xs.add(o.sx);
            xs.add(o.sx + o.sw);
            xs.add(o.sx + o.sw / 2f);
            ys.add(o.sy);
            ys.add(o.sy + o.sh);
            ys.add(o.sy + o.sh / 2f);
            // Gap-aligned neighbours (3 units apart, like the Fern pill rows)
            xs.add(o.sx + o.sw + 3);
            xs.add(o.sx - 3);
            ys.add(o.sy + o.sh + 3);
            ys.add(o.sy - 3);
        }
        float[] edgesX = {0, w.sw / 2f, w.sw};
        float[] edgesY = {0, w.sh / 2f, w.sh};
        for (float target : xs) {
            for (float e : edgesX) {
                float d = Math.abs(x + e - target);
                if (d < bestDx) {
                    bestDx = d;
                    rx = target - e;
                    gx = target;
                }
            }
        }
        for (float target : ys) {
            for (float e : edgesY) {
                float d = Math.abs(y + e - target);
                if (d < bestDy) {
                    bestDy = d;
                    ry = target - e;
                    gy = target;
                }
            }
        }
        if (bestDx > SNAP) {
            float g = Aqua.hud().grid.f();
            rx = Math.round(x / g) * g;
            gx = Float.NaN;
        } else {
            guides.add(new float[]{0, gx});
        }
        if (bestDy > SNAP) {
            float g = Aqua.hud().grid.f();
            ry = Math.round(y / g) * g;
            gy = Float.NaN;
        } else {
            guides.add(new float[]{1, gy});
        }
        return new float[]{rx, ry};
    }

    private void handleKey(KeyInput k) {
        if (selected == null) {
            return;
        }
        float step = k.hasShift() ? 5 : 1;
        float dx = 0;
        float dy = 0;
        switch (k.key()) {
            case GLFW.GLFW_KEY_LEFT -> dx = -step;
            case GLFW.GLFW_KEY_RIGHT -> dx = step;
            case GLFW.GLFW_KEY_UP -> dy = -step;
            case GLFW.GLFW_KEY_DOWN -> dy = step;
            case GLFW.GLFW_KEY_L -> selected.setLocked(!selected.locked());
            case GLFW.GLFW_KEY_R -> selected.resetLayout();
            case GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_DELETE -> selected.setEnabled(!selected.isEnabled(), false);
            default -> {
            }
        }
        if ((dx != 0 || dy != 0) && !selected.locked()) {
            selected.layout(width, height, true);
            selected.moveTo(selected.sx + dx, selected.sy + dy, width, height);
        }
    }

    /** Arranges the top-left Fern cluster in rows, measuring each widget's current width. */
    public static void autoArrange() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        String[][] rows = {{"watermark", "account", "performance"}, {"position", "network", "clock", "cps"}, {"keybinds", "effects"}};
        // Keep the cluster clear of the top-centre Island.
        float limit = sw / 2f - 50;
        for (String centre : new String[]{"island", "music_card"}) {
            if (Aqua.modules().byId(centre) instanceof HudWidget c && c.isEnabled() && c.anchor().fx == 0.5f && c.anchor().fy == 0f) {
                c.layout(sw, sh, true);
                limit = Math.min(limit, c.sx - 6);
            }
        }
        limit = Math.max(limit, sw * 0.3f);
        float y = 5;
        for (String[] row : rows) {
            float x = 5;
            float rowH = 0;
            for (String id : row) {
                if (!(Aqua.modules().byId(id) instanceof HudWidget w) || !w.isEnabled()) {
                    continue;
                }
                w.layout(sw, sh, true);
                if (x > 5 && x + w.sw > limit) {
                    y += rowH + 3;
                    x = 5;
                    rowH = 0;
                }
                w.setLayout(Anchor.TOP_LEFT, x, y);
                x += w.sw + 3;
                rowH = Math.max(rowH, w.sh);
            }
            if (rowH > 0) {
                y += rowH + 3;
            }
        }
        // On small screens the cluster can reach the keystrokes on the left; move them below it.
        if (Aqua.modules().byId("keystrokes") instanceof HudWidget k && k.isEnabled()) {
            k.layout(sw, sh, true);
            if (k.sx < 5 + 120 && k.sy < y + 2 && k.sy + k.sh > 5) {
                float ky = Math.min(y + 4, sh - k.sh - 44);
                k.setLayout(Anchor.TOP_LEFT, 5, Math.max(y, ky));
            }
        }
        Aqua.hud().needsArrange = false;
    }

    // ------------------------------------------------------------------ input forwarding

    private static boolean hasAltDown() {
        long h = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(h, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        ui.onClick(click.button());
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        ui.onRelease(click.button());
        if (dragging != null && !ui.down) {
            dragging = null;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        ui.onScroll(v);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (ui.onKey(input)) {
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (selected != null) {
                selected = null;
                return true;
            }
            close();
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        ui.onChar(input);
        return true;
    }
}
