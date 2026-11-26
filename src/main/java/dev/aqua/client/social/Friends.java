package dev.aqua.client.social;

import dev.aqua.client.Aqua;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Your friends list (Friends tab of the menu). Friends get a highlight colour on their name in the
 * tab list. It is stored with your settings and never sent anywhere.
 */
public final class Friends {
    private Friends() {}

    public static final int COLOR = 0xFF5BE38C;

    private static Set<String> read() {
        Set<String> s = new LinkedHashSet<>();
        for (String n : Aqua.ui().friends.get().split(",")) {
            if (!n.isBlank()) {
                s.add(n.trim());
            }
        }
        return s;
    }

    public static List<String> all() {
        return new ArrayList<>(read());
    }

    public static boolean isFriend(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (String n : read()) {
            if (n.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** Adds a name; returns false if it is not a valid player name or already listed. */
    public static boolean add(String name) {
        String n = name.trim();
        if (!n.matches("[A-Za-z0-9_]{1,16}") || isFriend(n)) {
            return false;
        }
        Set<String> s = read();
        s.add(n);
        Aqua.ui().friends.set(String.join(",", s));
        return true;
    }

    public static void remove(String name) {
        Set<String> s = read();
        s.removeIf(n -> n.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)));
        Aqua.ui().friends.set(String.join(",", s));
    }
}
