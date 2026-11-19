package dev.aqua.client.music.external;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads values out of the text form of a GVariant dictionary as printed by {@code gdbus call}, for
 * example {@code 'xesam:title': <'Song'>}, {@code 'xesam:artist': <['A', 'B']>} or
 * {@code 'mpris:length': <uint64 240000000>}.
 */
final class GVariantText {
    private GVariantText() {}

    private static int valueStart(String text, String key) {
        for (String quote : new String[]{"'", "\""}) {
            String marker = quote + key + quote + ": <";
            int i = text.indexOf(marker);
            if (i >= 0) {
                return i + marker.length();
            }
        }
        return -1;
    }

    /** A string, object path or signature value; null when the key is missing. */
    @Nullable
    static String string(String text, String key) {
        int i = valueStart(text, key);
        if (i < 0) {
            return null;
        }
        for (String prefix : new String[]{"objectpath ", "signature ", "@s ", "@o "}) {
            if (text.startsWith(prefix, i)) {
                i += prefix.length();
            }
        }
        Parsed p = literal(text, i);
        return p == null ? null : p.value;
    }

    /** A list of strings ({@code ['a', 'b']}); empty when missing. */
    static List<String> strings(String text, String key) {
        List<String> out = new ArrayList<>();
        int i = valueStart(text, key);
        if (i < 0) {
            return out;
        }
        if (text.startsWith("@as ", i)) {
            i += 4;
        }
        if (i >= text.length() || text.charAt(i) != '[') {
            Parsed single = literal(text, i);
            if (single != null) {
                out.add(single.value);
            }
            return out;
        }
        i++;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == ']') {
                break;
            }
            if (c == '\'' || c == '"') {
                Parsed p = literal(text, i);
                if (p == null) {
                    break;
                }
                out.add(p.value);
                i = p.end;
            } else {
                i++;
            }
        }
        return out;
    }

    private static final Pattern NUMBER = Pattern.compile("\\G(?:@?[a-z][a-z0-9]* )?(-?\\d+)");

    /** An integer value such as {@code <uint64 5>}, {@code <int64 -1>} or {@code <5>}. */
    static long number(String text, String key, long missing) {
        int i = valueStart(text, key);
        if (i < 0) {
            return missing;
        }
        Matcher m = NUMBER.matcher(text);
        if (m.find(i) && m.start() == i) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                return missing;
            }
        }
        return missing;
    }

    private record Parsed(String value, int end) {}

    /** Parses a quoted GVariant string starting at {@code i} (single or double quotes, backslash escapes). */
    @Nullable
    private static Parsed literal(String text, int i) {
        if (i >= text.length()) {
            return null;
        }
        char q = text.charAt(i);
        if (q != '\'' && q != '"') {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int j = i + 1;
        while (j < text.length()) {
            char c = text.charAt(j);
            if (c == q) {
                return new Parsed(sb.toString(), j + 1);
            }
            if (c == '\\' && j + 1 < text.length()) {
                char e = text.charAt(++j);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'u' -> {
                        if (j + 4 < text.length()) {
                            sb.append((char) Integer.parseInt(text.substring(j + 1, j + 5), 16));
                            j += 4;
                        }
                    }
                    case 'U' -> {
                        if (j + 8 < text.length()) {
                            sb.appendCodePoint(Integer.parseInt(text.substring(j + 1, j + 9), 16));
                            j += 8;
                        }
                    }
                    default -> sb.append(e);
                }
            } else {
                sb.append(c);
            }
            j++;
        }
        return null;
    }
}
