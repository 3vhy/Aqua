package dev.aqua.client.notify;

import dev.aqua.client.render.Anim;
import dev.aqua.client.render.Icon;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class Notification {
    public enum Kind { ENABLED, DISABLED, INFO, SUCCESS, WARNING }

    public final String title;
    public final String message;
    public final Icon icon;
    public final Kind kind;
    public final long created = System.currentTimeMillis();
    public final long durationMs;
    @Nullable public final Identifier thumbnail;
    public final float thumbAspect;
    final Anim appear = new Anim(0f, 220f);
    final Anim slot = new Anim(0f, 200f);
    boolean placed;

    public Notification(String title, String message, Icon icon, Kind kind, long durationMs, @Nullable Identifier thumbnail, float thumbAspect) {
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.kind = kind;
        this.durationMs = durationMs;
        this.thumbnail = thumbnail;
        this.thumbAspect = thumbAspect;
    }

    public float progress() {
        return Math.min(1f, (System.currentTimeMillis() - created) / (float) durationMs);
    }

    public boolean expired() {
        return System.currentTimeMillis() - created > durationMs + 400;
    }

    public boolean closing() {
        return System.currentTimeMillis() - created > durationMs;
    }

    public float appear() {
        appear.target(closing() ? 0f : 1f);
        return appear.get();
    }
}
