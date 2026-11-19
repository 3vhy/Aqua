package dev.aqua.client.util;

import java.util.Locale;

public final class TextUtil {
    private TextUtil() {}

    public static String titleCase(String id) {
        String s = id.replace('_', ' ').replace('/', ' ');
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char c : s.toCharArray()) {
            sb.append(up ? Character.toUpperCase(c) : c);
            up = c == ' ';
        }
        return sb.toString();
    }

    public static String duration(int ticks) {
        if (ticks < 0) {
            return "∞";
        }
        int s = ticks / 20;
        if (s >= 3600) {
            return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
        }
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    public static String fmt2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
