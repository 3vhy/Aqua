package dev.aqua.client.module;

import dev.aqua.client.render.Icon;

public enum Category {
    HUD("HUD", "On-screen widgets", Icon.LAYOUT),
    VISUALS("Visuals", "How the world looks", Icon.SPARKLE),
    COSMETICS("Cosmetics", "Effects and wearables", Icon.STAR),
    QOL("Quality of Life", "Comfort features", Icon.SLIDERS),
    PERFORMANCE("Performance", "Effect budgets and limits", Icon.GAUGE);

    public final String title;
    public final String subtitle;
    public final Icon icon;

    Category(String title, String subtitle, Icon icon) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }
}
