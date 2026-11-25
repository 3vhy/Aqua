package dev.aqua.client.util;

import dev.aqua.client.AquaClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records every error Aqua catches and recovers from (a widget, effect or module that failed), so
 * nothing fails silently. The in-game tests require this list to stay empty.
 */
public final class Problems {
    private static final List<String> LIST = Collections.synchronizedList(new ArrayList<>());

    private Problems() {}

    public static void report(String what, Throwable t) {
        String line = what + ": " + t;
        LIST.add(line);
        AquaClient.LOGGER.error("{}", what, t);
    }

    public static List<String> all() {
        synchronized (LIST) {
            return List.copyOf(LIST);
        }
    }

    public static int count() {
        return LIST.size();
    }
}
