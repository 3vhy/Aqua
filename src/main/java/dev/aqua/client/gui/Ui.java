package dev.aqua.client.gui;

import dev.aqua.client.Aqua;
import dev.aqua.client.module.Keybind;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.EnumSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Small immediate-mode UI toolkit shared by Aqua's screens. Screens forward input events; each frame
 * controls are drawn and react to the queued input. Popups (dropdowns, colour pickers) draw last and
 * block input underneath them; Escape closes the popup before anything else.
 */
public final class Ui {
    public DrawContext ctx;
    public float mx;
    public float my;
    public boolean down;
    private boolean clickQueued;
    private boolean rightQueued;
    public boolean clicked;
    public boolean rightClicked;
    public boolean released;
    private float scrollQueued;
    public float scroll;
    private final ArrayDeque<KeyInput> keyQueue = new ArrayDeque<>();
    private final ArrayDeque<CharInput> charQueue = new ArrayDeque<>();
    public final List<KeyInput> keys = new ArrayList<>();
    public final List<CharInput> chars = new ArrayList<>();

    @Nullable public String active;      // control being dragged
    @Nullable public String focus;       // focused text field
    @Nullable public String capturing;   // keybind capture in progress
    @Nullable private Consumer<KeyCapture> captureCallback;
    @Nullable public Popup popup;
    private float clipX0 = -1e9f, clipY0 = -1e9f, clipX1 = 1e9f, clipY1 = 1e9f;
    private final ArrayDeque<float[]> clipStack = new ArrayDeque<>();
    private final Map<String, Anim> anims = new HashMap<>();
    private final Map<String, StringBuilder> textBuffers = new HashMap<>();
    private final Map<String, Integer> cursors = new HashMap<>();

    public record KeyCapture(InputUtil.Type type, int code, boolean cleared, boolean cancelled) {}

    // ------------------------------------------------------------------ input plumbing

    public void onClick(int button) {
        if (capturing != null) {
            // Mouse buttons can be bound too (except plain left click, which confirms nothing).
            if (button != 0) {
                finishCapture(new KeyCapture(InputUtil.Type.MOUSE, button, false, false));
                return;
            }
        }
        if (button == 0) {
            clickQueued = true;
            down = true;
        } else if (button == 1) {
            rightQueued = true;
        }
    }

    public void onRelease(int button) {
        if (button == 0) {
            down = false;
            released = true;
        }
    }

    public void onScroll(double amount) {
        scrollQueued += (float) amount;
    }

    /** Returns true if the key was consumed by the UI (capture, focus or popup). */
    public boolean onKey(KeyInput input) {
        if (capturing != null) {
            int k = input.key();
            if (k == GLFW.GLFW_KEY_ESCAPE) {
                finishCapture(new KeyCapture(InputUtil.Type.KEYSYM, Keybind.NONE, false, true));
            } else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) {
                finishCapture(new KeyCapture(InputUtil.Type.KEYSYM, Keybind.NONE, true, false));
            } else {
                finishCapture(new KeyCapture(InputUtil.Type.KEYSYM, k, false, false));
            }
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (popup != null) {
                popup = null;
                UiSounds.click(0.8f);
                return true;
            }
            if (focus != null) {
                focus = null;
                return true;
            }
            return false;
        }
        keyQueue.add(input);
        return focus != null || popup != null;
    }

    public boolean onChar(CharInput input) {
        charQueue.add(input);
        return focus != null;
    }

    private void finishCapture(KeyCapture c) {
        Consumer<KeyCapture> cb = captureCallback;
        capturing = null;
        captureCallback = null;
        if (cb != null) {
            cb.accept(c);
        }
        UiSounds.click(1.2f);
    }

    public void startCapture(String id, Consumer<KeyCapture> cb) {
        capturing = id;
        captureCallback = cb;
        focus = null;
    }

    /** Begins a frame with the mouse already transformed into the caller's coordinate space. */
    public void begin(DrawContext ctx, float mouseX, float mouseY) {
        this.ctx = ctx;
        this.mx = mouseX;
        this.my = mouseY;
        clicked = clickQueued;
        rightClicked = rightQueued;
        clickQueued = false;
        rightQueued = false;
        scroll = scrollQueued;
        scrollQueued = 0;
        keys.clear();
        keys.addAll(keyQueue);
        keyQueue.clear();
        chars.clear();
        chars.addAll(charQueue);
        charQueue.clear();
        clipStack.clear();
        clipX0 = clipY0 = -1e9f;
        clipX1 = clipY1 = 1e9f;
        if (popup != null && (clicked || rightClicked) && !popup.contains(mx, my)) {
            // Clicking outside a popup closes it and swallows the click.
            if (!popup.containsAnchor(mx, my)) {
                popup = null;
            } else {
                popup = null;
            }
            clicked = false;
            rightClicked = false;
        }
        if (clicked && focus != null) {
            // Losing focus is decided by the field itself; clicks elsewhere clear it in end().
            focusClickPending = true;
        }
    }

    private boolean focusClickPending;
    private boolean focusHitThisFrame;

    public void end() {
        if (popup != null) {
            Popup p = popup;
            boolean c = clicked;
            boolean inside = p.contains(mx, my);
            p.render(this, inside);
            if (c && inside) {
                clicked = false;
            }
        }
        if (focusClickPending && !focusHitThisFrame) {
            focus = null;
        }
        focusClickPending = false;
        focusHitThisFrame = false;
        if (released) {
            active = null;
            released = false;
        }
    }

    // ------------------------------------------------------------------ helpers

    public Anim anim(String id, float durationMs, float initial) {
        return anims.computeIfAbsent(id, k -> new Anim(initial, durationMs));
    }

    public void pushClip(float x, float y, float w, float h) {
        clipStack.push(new float[]{clipX0, clipY0, clipX1, clipY1});
        clipX0 = Math.max(clipX0, x);
        clipY0 = Math.max(clipY0, y);
        clipX1 = Math.min(clipX1, x + w);
        clipY1 = Math.min(clipY1, y + h);
        R2D.pushClip(ctx, x, y, w, h);
    }

    public void popClip() {
        float[] c = clipStack.pop();
        clipX0 = c[0];
        clipY0 = c[1];
        clipX1 = c[2];
        clipY1 = c[3];
        R2D.popClip(ctx);
    }

    /** Hover test that respects clipping and popups. */
    public boolean hover(float x, float y, float w, float h) {
        if (mx < clipX0 || my < clipY0 || mx >= clipX1 || my >= clipY1) {
            return false;
        }
        if (popup != null && !inPopupRender && popup.contains(mx, my)) {
            return false;
        }
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    boolean inPopupRender;

    /** True once when the area is clicked. Consumes the click. */
    public boolean clickedIn(float x, float y, float w, float h) {
        if (clicked && hover(x, y, w, h)) {
            clicked = false;
            return true;
        }
        return false;
    }

    public boolean rightClickedIn(float x, float y, float w, float h) {
        if (rightClicked && hover(x, y, w, h)) {
            rightClicked = false;
            return true;
        }
        return false;
    }

    public Theme t() {
        return Aqua.theme();
    }

    // ------------------------------------------------------------------ controls

    /** Rounded text button. */
    public boolean button(String id, float x, float y, float w, float h, String label, @Nullable Icon icon, boolean primary) {
        Theme t = t();
        boolean hov = hover(x, y, w, h);
        float ha = anim(id + "#h", 120, 0).target(hov).get();
        int base = primary ? t.accent() : t.elevated();
        int fill = primary ? Colors.lerp(base, t.accent2(), ha * 0.35f) : Colors.lerp(Colors.withAlpha(base, 0.85f), Colors.brighten(base, 0.12f), ha);
        R2D.round(ctx, x, y, w, h, Math.min(5, h / 2f), fill);
        if (!primary) {
            R2D.outline(ctx, x, y, w, h, Math.min(5, h / 2f), 0.6f, Colors.withAlpha(t.border(), 0.6f + ha * 0.4f));
        }
        int fg = primary ? t.onAccent() : t.text();
        float size = 5.6f;
        float tw = AquaFont.width(label, size, AquaFont.Weight.SEMIBOLD) + (icon != null ? 9 : 0);
        float cx = x + (w - tw) / 2f;
        if (icon != null) {
            R2D.icon(ctx, icon, cx, y + h / 2f - 3.5f, 7, fg);
            cx += 9;
        }
        R2D.textMid(ctx, label, cx, y + h / 2f, size, AquaFont.Weight.SEMIBOLD, fg);
        if (clickedIn(x, y, w, h)) {
            UiSounds.click(1f);
            return true;
        }
        return false;
    }

    public boolean iconButton(String id, float x, float y, float size, Icon icon, int color, @Nullable String tooltip) {
        Theme t = t();
        boolean hov = hover(x, y, size, size);
        float ha = anim(id + "#h", 120, 0).target(hov).get();
        if (ha > 0.01f) {
            R2D.round(ctx, x, y, size, size, size / 3.2f, Colors.withAlpha(t.elevated(), 0.9f * ha));
        }
        float is = size * 0.6f;
        R2D.icon(ctx, icon, x + (size - is) / 2f, y + (size - is) / 2f, is, Colors.lerp(color, t.text(), ha * 0.6f));
        if (hov && tooltip != null) {
            tooltip(tooltip);
        }
        if (clickedIn(x, y, size, size)) {
            UiSounds.click(1f);
            return true;
        }
        return false;
    }

    private String pendingTooltip;

    public void tooltip(String text) {
        pendingTooltip = text;
    }

    public void drawTooltip(float maxW, float maxH) {
        if (pendingTooltip == null) {
            return;
        }
        Theme t = t();
        String s = pendingTooltip;
        pendingTooltip = null;
        float w = AquaFont.width(s, 5.2f, AquaFont.Weight.MEDIUM) + 8;
        float x = Math.min(mx + 8, maxW - w - 2);
        float y = Math.min(my + 10, maxH - 12);
        R2D.round(ctx, x, y, w, 10, 3, Colors.withAlpha(t.background(), 0.96f));
        R2D.outline(ctx, x, y, w, 10, 3, 0.5f, t.border());
        R2D.textMid(ctx, s, x + 4, y + 5, 5.2f, AquaFont.Weight.MEDIUM, t.text());
    }

    /** iOS-style switch; returns the (possibly toggled) value. */
    public boolean toggle(String id, float x, float y, boolean on) {
        Theme t = t();
        float w = 17;
        float h = 9.5f;
        boolean hov = hover(x - 2, y - 2, w + 4, h + 4);
        float p = anim(id + "#t", 160, on ? 1 : 0).target(on).get();
        float ha = anim(id + "#h", 120, 0).target(hov).get();
        int track = Colors.lerp(Colors.brighten(t.elevated(), 0.12f + ha * 0.06f), t.accent(), p);
        R2D.round(ctx, x, y, w, h, h / 2f, track);
        float knob = h - 3;
        float kx = x + 1.5f + (w - knob - 3) * Anim.easeInOutCubic(p);
        R2D.shadow(ctx, kx, y + 1.5f, knob, knob, knob / 2f, 2.5f, 0x40000000);
        R2D.circle(ctx, kx + knob / 2f, y + h / 2f, knob / 2f, Colors.lerp(t.text(), 0xFFFFFFFF, p));
        if (clickedIn(x - 2, y - 2, w + 4, h + 4)) {
            UiSounds.toggle(!on);
            return !on;
        }
        return on;
    }

    /** Horizontal slider bound to a number setting. Returns true while being changed. */
    public boolean slider(String id, float x, float y, float w, NumberSetting s) {
        Theme t = t();
        float h = 9;
        boolean hov = hover(x - 2, y, w + 4, h);
        if (clicked && hov) {
            active = id;
            clicked = false;
        }
        boolean dragging = id.equals(active) && down;
        if (dragging) {
            s.setNormalized((mx - x) / w);
        }
        if (hov && scroll != 0 && active == null) {
            s.set(s.get() + Math.signum(scroll) * s.step());
            scroll = 0;
        }
        float ha = anim(id + "#h", 120, 0).target(hov || dragging).get();
        float p = anim(id + "#v", 90, (float) s.normalized()).target((float) s.normalized()).get();
        float cy = y + h / 2f;
        R2D.round(ctx, x, cy - 1.25f, w, 2.5f, 1.25f, Colors.brighten(t.elevated(), 0.15f));
        R2D.round(ctx, x, cy - 1.25f, Math.max(2.5f, w * p), 2.5f, 1.25f, t.accent());
        float kr = 3.2f + ha * 0.6f;
        R2D.circle(ctx, x + w * p, cy, kr, 0xFFFFFFFF);
        R2D.ring(ctx, x + w * p, cy, kr, 0.8f, Colors.withAlpha(t.accent(), 0.9f));
        return dragging;
    }

    /** Dropdown button for an enum setting; opens a popup list. */
    public <E extends Enum<E>> void dropdown(String id, float x, float y, float w, EnumSetting<E> s) {
        Theme t = t();
        float h = 11;
        boolean hov = hover(x, y, w, h);
        float ha = anim(id + "#h", 120, 0).target(hov).get();
        R2D.round(ctx, x, y, w, h, 3.5f, Colors.lerp(t.elevated(), Colors.brighten(t.elevated(), 0.1f), ha));
        R2D.outline(ctx, x, y, w, h, 3.5f, 0.6f, t.border());
        String label = AquaFont.ellipsize(EnumSetting.display(s.get()), w - 16, 5.4f, AquaFont.Weight.MEDIUM);
        R2D.textMid(ctx, label, x + 5, y + h / 2f, 5.4f, AquaFont.Weight.MEDIUM, t.text());
        R2D.icon(ctx, Icon.CHEVRON_DOWN, x + w - 10, y + h / 2f - 3, 6, t.muted());
        if (clickedIn(x, y, w, h)) {
            UiSounds.click(1f);
            popup = new DropdownPopup<>(x, y + h + 2, Math.max(w, 70), s, x, y, w, h);
        }
    }

    /** Small colour swatch that opens the colour picker. */
    public void colorButton(String id, float x, float y, ColorSetting s) {
        Theme t = t();
        float w = 22;
        float h = 11;
        boolean hov = hover(x, y, w, h);
        R2D.round(ctx, x, y, w, h, 3.5f, Colors.brighten(t.elevated(), hov ? 0.18f : 0.08f));
        // Checker for alpha preview
        R2D.round(ctx, x + 2, y + 2, w - 4, h - 4, 2.2f, 0xFF808080);
        R2D.roundCorners(ctx, x + 2, y + 2, (w - 4) / 2f, (h - 4) / 2f, 2.2f, 1, 0xFFC8C8C8);
        R2D.roundCorners(ctx, x + 2 + (w - 4) / 2f, y + 2 + (h - 4) / 2f, (w - 4) / 2f, (h - 4) / 2f, 2.2f, 4, 0xFFC8C8C8);
        R2D.round(ctx, x + 2, y + 2, w - 4, h - 4, 2.2f, s.color());
        if (s.syncTheme()) {
            R2D.icon(ctx, Icon.PALETTE, x + w / 2f - 3, y + h / 2f - 3, 6, t.onAccent());
        }
        if (clickedIn(x, y, w, h)) {
            UiSounds.click(1f);
            popup = new ColorPopup(x + w - 110, y + h + 2, s, x, y, w, h);
        }
    }

    /** Raw colour button (theme editor) with a setter. */
    public void colorButton(String id, float x, float y, int color, IntConsumer setter) {
        Theme t = t();
        float w = 22;
        float h = 11;
        boolean hov = hover(x, y, w, h);
        R2D.round(ctx, x, y, w, h, 3.5f, Colors.brighten(t.elevated(), hov ? 0.18f : 0.08f));
        R2D.round(ctx, x + 2, y + 2, w - 4, h - 4, 2.2f, color);
        if (clickedIn(x, y, w, h)) {
            UiSounds.click(1f);
            popup = new ColorPopup(x + w - 110, y + h + 2, color, setter, x, y, w, h);
        }
    }

    /** Keybind badge ("Bind" when empty). Clicking starts capture; right-click clears. */
    public void keybindBadge(String id, float x, float y, float w, String label, boolean bound, boolean conflict,
                             Consumer<KeyCapture> onCapture, Runnable onClear) {
        Theme t = t();
        float h = 10;
        boolean cap = id.equals(capturing);
        boolean hov = hover(x, y, w, h);
        float ha = anim(id + "#h", 120, 0).target(hov || cap).get();
        int fill = cap ? Colors.withAlpha(t.accent(), 0.25f) : Colors.lerp(t.elevated(), Colors.brighten(t.elevated(), 0.12f), ha);
        R2D.round(ctx, x, y, w, h, 3, fill);
        int border = conflict ? 0xFFFFB547 : (cap ? t.accent() : Colors.withAlpha(t.border(), 0.8f));
        R2D.outline(ctx, x, y, w, h, 3, 0.6f, border);
        String text = cap ? "Press a key" : (bound ? label : "Bind");
        text = AquaFont.ellipsize(text, w - 6, 4.8f, AquaFont.Weight.SEMIBOLD);
        R2D.textCentered(ctx, text, x + w / 2f, y + h / 2f, 4.8f, AquaFont.Weight.SEMIBOLD,
                cap ? t.accent() : (bound ? t.text() : t.muted()));
        if (clickedIn(x, y, w, h)) {
            UiSounds.click(1.1f);
            startCapture(id, onCapture);
        }
        if (rightClickedIn(x, y, w, h)) {
            onClear.run();
            UiSounds.click(0.8f);
        }
        if (hov && !cap) {
            tooltip(conflict ? "Conflicts with another binding • right-click to clear" : "Click to bind • right-click to clear");
        }
    }

    /** Single-line text field. Returns the current text; calls onSubmit with Enter. */
    public String textField(String id, float x, float y, float w, float h, String value, String placeholder, int maxLen,
                            @Nullable Icon icon, @Nullable Consumer<String> onChange, @Nullable Consumer<String> onSubmit) {
        Theme t = t();
        StringBuilder sb = textBuffers.computeIfAbsent(id, k -> new StringBuilder(value));
        boolean focused = id.equals(focus);
        if (!focused && !sb.toString().equals(value)) {
            sb.setLength(0);
            sb.append(value);
        }
        boolean hov = hover(x, y, w, h);
        if (clicked && hov) {
            focus = id;
            focused = true;
            focusHitThisFrame = true;
            clicked = false;
            cursors.put(id, sb.length());
        } else if (focused && focusClickPending && hov) {
            focusHitThisFrame = true;
        }
        float fa = anim(id + "#f", 140, 0).target(focused).get();
        R2D.round(ctx, x, y, w, h, Math.min(4.5f, h / 2.4f), Colors.lerp(Colors.withAlpha(t.background(), 0.85f), Colors.darken(t.elevated(), 0.2f), fa));
        R2D.outline(ctx, x, y, w, h, Math.min(4.5f, h / 2.4f), 0.6f, Colors.lerp(Colors.withAlpha(t.border(), 0.7f), t.accent(), fa));
        float tx = x + 5;
        if (icon != null) {
            R2D.icon(ctx, icon, x + 4.5f, y + h / 2f - 3.25f, 6.5f, t.muted());
            tx += 8;
        }
        boolean changed = false;
        if (focused) {
            int cur = Math.min(cursors.getOrDefault(id, sb.length()), sb.length());
            for (CharInput c : chars) {
                String str = c.asString();
                if (sb.length() + str.length() <= maxLen && c.codepoint() >= 32) {
                    sb.insert(cur, str);
                    cur += str.length();
                    changed = true;
                }
            }
            for (KeyInput k : keys) {
                int key = k.key();
                if (key == GLFW.GLFW_KEY_BACKSPACE && cur > 0) {
                    sb.deleteCharAt(cur - 1);
                    cur--;
                    changed = true;
                } else if (key == GLFW.GLFW_KEY_DELETE && cur < sb.length()) {
                    sb.deleteCharAt(cur);
                    changed = true;
                } else if (key == GLFW.GLFW_KEY_LEFT) {
                    cur = Math.max(0, cur - 1);
                } else if (key == GLFW.GLFW_KEY_RIGHT) {
                    cur = Math.min(sb.length(), cur + 1);
                } else if (key == GLFW.GLFW_KEY_HOME) {
                    cur = 0;
                } else if (key == GLFW.GLFW_KEY_END) {
                    cur = sb.length();
                } else if (k.isPaste()) {
                    String clip = MinecraftClient.getInstance().keyboard.getClipboard().replaceAll("\\p{Cntrl}", "");
                    int room = maxLen - sb.length();
                    if (room > 0) {
                        clip = clip.length() > room ? clip.substring(0, room) : clip;
                        sb.insert(cur, clip);
                        cur += clip.length();
                        changed = true;
                    }
                } else if (k.isSelectAll()) {
                    cur = sb.length();
                } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    if (onSubmit != null) {
                        onSubmit.accept(sb.toString());
                    }
                    focus = null;
                }
            }
            chars.clear();
            keys.removeIf(k -> true);
            cursors.put(id, cur);
        }
        String text = sb.toString();
        pushClip(x + 2, y, w - 4, h);
        float size = Math.min(6f, h * 0.48f);
        if (text.isEmpty() && !focused) {
            R2D.textMid(ctx, placeholder, tx, y + h / 2f, size, AquaFont.Weight.MEDIUM, t.muted());
        } else {
            int cur = Math.min(cursors.getOrDefault(id, text.length()), text.length());
            float before = AquaFont.width(text.substring(0, cur), size, AquaFont.Weight.MEDIUM);
            float shift = Math.max(0, before - (w - (tx - x) - 8));
            R2D.textMid(ctx, text, tx - shift, y + h / 2f, size, AquaFont.Weight.MEDIUM, t.text());
            if (focused && (System.currentTimeMillis() / 530) % 2 == 0) {
                R2D.rect(ctx, tx - shift + before, y + h / 2f - size * 0.55f, 0.6f, size * 1.1f, t.accent());
            }
        }
        popClip();
        if (changed && onChange != null) {
            onChange.accept(text);
        }
        return text;
    }

    public void setText(String id, String value) {
        StringBuilder sb = textBuffers.computeIfAbsent(id, k -> new StringBuilder());
        sb.setLength(0);
        sb.append(value);
        cursors.put(id, value.length());
    }

    // ------------------------------------------------------------------ popups

    public abstract static class Popup {
        final float x, y, w;
        float h;
        final float ax, ay, aw, ah;
        final Anim appear = new Anim(0f, 140f);

        Popup(float x, float y, float w, float h, float ax, float ay, float aw, float ah) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.ax = ax;
            this.ay = ay;
            this.aw = aw;
            this.ah = ah;
            appear.target(1f);
        }

        boolean contains(float px, float py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        boolean containsAnchor(float px, float py) {
            return px >= ax && py >= ay && px < ax + aw && py < ay + ah;
        }

        abstract void render(Ui ui, boolean hovered);

        void frame(Ui ui) {
            Theme t = ui.t();
            float a = appear.get();
            R2D.shadow(ui.ctx, x, y, w, h, 5, 10, Colors.withAlpha(0x000000, Math.round(110 * a)));
            R2D.round(ui.ctx, x, y, w, h, 5, Colors.withAlpha(t.panel(), Math.round(250 * a)));
            R2D.outline(ui.ctx, x, y, w, h, 5, 0.6f, Colors.fade(t.border(), a));
        }
    }

    static final class DropdownPopup<E extends Enum<E>> extends Popup {
        private final EnumSetting<E> setting;

        DropdownPopup(float x, float y, float w, EnumSetting<E> s, float ax, float ay, float aw, float ah) {
            super(x, y, w, 4 + s.values().length * 11, ax, ay, aw, ah);
            this.setting = s;
        }

        @Override
        void render(Ui ui, boolean hovered) {
            ui.inPopupRender = true;
            frame(ui);
            Theme t = ui.t();
            E[] values = setting.values();
            for (int i = 0; i < values.length; i++) {
                float ry = y + 2 + i * 11;
                boolean hov = ui.hover(x + 2, ry, w - 4, 11);
                boolean sel = values[i] == setting.get();
                if (hov || sel) {
                    R2D.round(ui.ctx, x + 2, ry, w - 4, 11, 3, sel ? Colors.withAlpha(t.accent(), 0.22f) : Colors.withAlpha(t.elevated(), 0.9f));
                }
                R2D.textMid(ui.ctx, EnumSetting.display(values[i]), x + 7, ry + 5.5f, 5.4f, AquaFont.Weight.MEDIUM, sel ? t.accent() : t.text());
                if (sel) {
                    R2D.icon(ui.ctx, Icon.CHECK, x + w - 11, ry + 2.5f, 6, t.accent());
                }
                if (ui.clickedIn(x + 2, ry, w - 4, 11)) {
                    setting.set(values[i]);
                    UiSounds.click(1.1f);
                    ui.popup = null;
                }
            }
            ui.inPopupRender = false;
        }
    }

    static final class ColorPopup extends Popup {
        @Nullable private final ColorSetting setting;
        @Nullable private final IntConsumer setter;
        private float hue;
        private float sat;
        private float val;
        private float alpha;
        private String dragging;

        ColorPopup(float x, float y, ColorSetting s, float ax, float ay, float aw, float ah) {
            super(Math.max(2, x), y, 110, s.allowAlpha() ? 104 : 94, ax, ay, aw, ah);
            this.setting = s;
            this.setter = null;
            init(s.color());
        }

        ColorPopup(float x, float y, int color, IntConsumer setter, float ax, float ay, float aw, float ah) {
            super(Math.max(2, x), y, 110, 84, ax, ay, aw, ah);
            this.setting = null;
            this.setter = setter;
            init(color);
        }

        private void init(int c) {
            float[] hsv = Colors.toHsv(c);
            hue = hsv[0];
            sat = hsv[1];
            val = hsv[2];
            alpha = Colors.a(c);
        }

        private void apply() {
            int c = Colors.hsv(hue, sat, val, Math.round(alpha * 255));
            if (setting != null) {
                if (setting.syncTheme()) {
                    setting.setSyncTheme(false);
                }
                setting.set(c);
            } else if (setter != null) {
                setter.accept(c | 0xFF000000);
            }
        }

        @Override
        void render(Ui ui, boolean hovered) {
            ui.inPopupRender = true;
            frame(ui);
            Theme t = ui.t();
            DrawContext ctx = ui.ctx;
            float px = x + 6, py = y + 6, sw = w - 12, sh = 52;
            // Saturation/value square: hue gradient left->right white->hue, then black overlay top->bottom.
            int hueColor = Colors.hsv(hue, 1, 1, 255);
            R2D.roundGradientH(ctx, px, py, sw, sh, 3, 0xFFFFFFFF, hueColor);
            R2D.roundGradientV(ctx, px, py, sw, sh, 3, 0x00000000, 0xFF000000);
            if (ui.clicked && ui.hover(px, py, sw, sh)) {
                dragging = "sv";
                ui.clicked = false;
            }
            // Hue bar
            float hy = py + sh + 5;
            int segs = 6;
            for (int i = 0; i < segs; i++) {
                int c0 = Colors.hsv(i / (float) segs, 1, 1, 255);
                int c1 = Colors.hsv((i + 1) / (float) segs, 1, 1, 255);
                int mask = i == 0 ? R2D.LEFT : (i == segs - 1 ? R2D.RIGHT : 0);
                ctx.state.addSimpleElement(new dev.aqua.client.render.RoundedRectState(new org.joml.Matrix3x2f(ctx.getMatrices()),
                        px + sw * i / segs, hy, sw / segs + 0.4f, 5, 2.5f, 0, 0, mask, c0, c1, c1, c0, ctx.scissorStack.peekLast()));
            }
            if (ui.clicked && ui.hover(px, hy - 1, sw, 7)) {
                dragging = "h";
                ui.clicked = false;
            }
            float ay2 = hy + 9;
            boolean hasAlpha = setting != null && setting.allowAlpha();
            if (hasAlpha) {
                R2D.roundGradientH(ctx, px, ay2, sw, 5, 2.5f, Colors.withAlpha(Colors.hsv(hue, sat, val, 255), 0), Colors.hsv(hue, sat, val, 255));
                R2D.outline(ctx, px, ay2, sw, 5, 2.5f, 0.5f, t.border());
                if (ui.clicked && ui.hover(px, ay2 - 1, sw, 7)) {
                    dragging = "a";
                    ui.clicked = false;
                }
                ay2 += 9;
            }
            if (dragging != null) {
                if (!ui.down) {
                    dragging = null;
                } else {
                    switch (dragging) {
                        case "sv" -> {
                            sat = Anim.clamp01((ui.mx - px) / sw);
                            val = 1f - Anim.clamp01((ui.my - py) / sh);
                        }
                        case "h" -> hue = Math.min(0.999f, Anim.clamp01((ui.mx - px) / sw));
                        case "a" -> alpha = Anim.clamp01((ui.mx - px) / sw);
                        default -> {
                        }
                    }
                    apply();
                }
            }
            // Markers
            R2D.ring(ctx, px + sat * sw, py + (1 - val) * sh, 2.6f, 0.9f, 0xFFFFFFFF);
            R2D.round(ctx, px + hue * sw - 1, hy - 1, 2, 7, 1, 0xFFFFFFFF);
            int cur = Colors.hsv(hue, sat, val, Math.round(alpha * 255));
            R2D.round(ctx, px, ay2, 12, 9, 2.5f, cur);
            R2D.textMid(ctx, Colors.hex(cur, hasAlpha), px + 16, ay2 + 4.5f, 5.2f, AquaFont.Weight.MEDIUM, t.text());
            if (setting != null) {
                float bx = x + w - 30;
                boolean sync = setting.syncTheme();
                boolean nv = ui.toggle("cp_sync", bx, ay2, sync);
                R2D.textRight(ctx, "Theme", bx - 3, ay2 + 4.75f, 4.8f, AquaFont.Weight.MEDIUM, t.muted());
                if (nv != sync) {
                    setting.setSyncTheme(nv);
                    init(setting.color());
                }
            }
            ui.inPopupRender = false;
        }
    }
}
