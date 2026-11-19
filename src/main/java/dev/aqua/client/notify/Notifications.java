package dev.aqua.client.notify;

import dev.aqua.client.Aqua;
import dev.aqua.client.config.UiSettings;
import dev.aqua.client.hud.widgets.IslandWidget;
import dev.aqua.client.module.Module;
import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Colors;
import dev.aqua.client.render.Icon;
import dev.aqua.client.render.R2D;
import dev.aqua.client.render.font.AquaFont;
import dev.aqua.client.theme.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Toast notifications rendered either in the HUD Island (Fern style) or as stacked cards. */
public final class Notifications {
    private static final int MAX = 5;
    private final List<Notification> active = new ArrayList<>();

    public void push(Notification n) {
        if (Aqua.ui().notificationStyle.get() == UiSettings.NotificationStyle.OFF) {
            return;
        }
        active.add(0, n);
        while (active.size() > MAX) {
            active.remove(active.size() - 1);
        }
    }

    public void info(String title, String message) {
        push(new Notification(title, message, Icon.INFO, Notification.Kind.INFO, 3500, null, 1f));
    }

    public void success(String title, String message) {
        push(new Notification(title, message, Icon.CHECK, Notification.Kind.SUCCESS, 3500, null, 1f));
    }

    public void warn(String title, String message) {
        push(new Notification(title, message, Icon.INFO, Notification.Kind.WARNING, 6000, null, 1f));
    }

    public void image(String title, String message, @Nullable Identifier thumb, float aspect) {
        push(new Notification(title, message, Icon.CAMERA, Notification.Kind.SUCCESS, 5000, thumb, aspect));
    }

    public void moduleToggled(Module m) {
        if (!Aqua.ui().toggleNotifications.on()) {
            return;
        }
        // Collapse rapid toggles of the same module into the newest state.
        active.removeIf(n -> (n.kind == Notification.Kind.ENABLED || n.kind == Notification.Kind.DISABLED) && n.title.equals(m.name()));
        boolean on = m.isEnabled();
        push(new Notification(m.name(), on ? "enabled" : "disabled", Icon.POWER,
                on ? Notification.Kind.ENABLED : Notification.Kind.DISABLED, 1800, null, 1f));
    }

    /** Newest live notification (used by the Island). */
    @Nullable
    public Notification current() {
        cleanup();
        for (Notification n : active) {
            if (!n.closing()) {
                return n;
            }
        }
        return null;
    }

    private void cleanup() {
        Iterator<Notification> it = active.iterator();
        while (it.hasNext()) {
            if (it.next().expired()) {
                it.remove();
            }
        }
    }

    public static int dotColor(Notification n, Theme t) {
        return switch (n.kind) {
            case ENABLED, SUCCESS -> 0xFF3DDC84;
            case DISABLED -> 0xFFFF5C5C;
            case WARNING -> 0xFFFFB547;
            case INFO -> t.accent();
        };
    }

    /** Renders card-style notifications (bottom right). Island style is drawn by the Island widget. */
    public void render(DrawContext ctx) {
        cleanup();
        if (active.isEmpty()) {
            return;
        }
        UiSettings.NotificationStyle style = Aqua.ui().notificationStyle.get();
        if (style == UiSettings.NotificationStyle.OFF) {
            return;
        }
        IslandWidget island = Aqua.modules().get(IslandWidget.class);
        if (style == UiSettings.NotificationStyle.ISLAND && island != null && island.isEnabled()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        Theme t = Aqua.theme();
        float sw = ctx.getScaledWindowWidth();
        float sh = ctx.getScaledWindowHeight();
        float w = 150;
        float yCursor = sh - 8;
        for (Notification n : active) {
            float a = n.appear();
            boolean hasThumb = n.thumbnail != null;
            float h = hasThumb ? 30 + Math.min(70, (w - 16) / Math.max(0.5f, n.thumbAspect)) : 30;
            float targetY = yCursor - h;
            if (!n.placed) {
                n.slot.snap(targetY);
                n.placed = true;
            }
            n.slot.target(targetY);
            float y = n.slot.get();
            float x = sw - w - 8 + (1f - a) * 30f;
            yCursor -= (h + 5) * Anim.clamp01(a * 1.2f);
            int alpha = Math.round(a * 255);
            if (alpha < 4) {
                continue;
            }
            R2D.shadow(ctx, x, y, w, h, 7, 10, Colors.withAlpha(0x000000, Math.round(a * 90)));
            R2D.round(ctx, x, y, w, h, 7, Colors.fade(t.windowBg(0.92f), a));
            R2D.outline(ctx, x, y, w, h, 7, 0.75f, Colors.fade(t.border(), a));
            R2D.circle(ctx, x + 14, y + 15, 8, Colors.fade(Colors.withAlpha(dotColor(n, t), 0.18f), a));
            R2D.icon(ctx, n.icon, x + 9, y + 10, 10, Colors.fade(dotColor(n, t), a));
            R2D.text(ctx, AquaFont.ellipsize(n.title, w - 36, 6.5f, AquaFont.Weight.SEMIBOLD), x + 27, y + 7, 6.5f,
                    AquaFont.Weight.SEMIBOLD, Colors.fade(t.text(), a));
            R2D.text(ctx, AquaFont.ellipsize(n.message, w - 36, 5.5f, AquaFont.Weight.REGULAR), x + 27, y + 16, 5.5f,
                    AquaFont.Weight.REGULAR, Colors.fade(t.muted(), a));
            if (hasThumb) {
                float tw = w - 16;
                float th = h - 36;
                R2D.texture(ctx, n.thumbnail, x + 8, y + 28, tw, th, Colors.withAlpha(0xFFFFFF, alpha));
            }
            // Remaining-time bar (R3 reference)
            float p = 1f - n.progress();
            R2D.roundCorners(ctx, x + 7, y + h - 2.5f, (w - 14) * p, 1.2f, 0.6f, R2D.ALL, Colors.fade(t.accent(), a));
        }
    }
}
