package dev.aqua.client.music;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses timed lyric files ("[mm:ss.xx] text"), including multiple stamps per line and [offset:]. */
public final class LrcParser {
    private static final Pattern STAMP = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");
    private static final Pattern OFFSET = Pattern.compile("\\[offset:\\s*([+-]?\\d+)\\s*]", Pattern.CASE_INSENSITIVE);

    private LrcParser() {}

    public static List<LyricLine> parse(String content) {
        List<LyricLine> out = new ArrayList<>();
        long offset = 0;
        for (String raw : content.split("\\r?\\n")) {
            String line = raw.trim();
            Matcher off = OFFSET.matcher(line);
            if (off.find()) {
                try {
                    offset = Long.parseLong(off.group(1));
                } catch (NumberFormatException ignored) {
                }
                continue;
            }
            Matcher m = STAMP.matcher(line);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find() && m.start() == end) {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                long frac = 0;
                if (m.group(3) != null) {
                    String f = m.group(3);
                    frac = Long.parseLong(f) * (f.length() == 1 ? 100 : f.length() == 2 ? 10 : 1);
                }
                times.add(min * 60000 + sec * 1000 + frac);
                end = m.end();
            }
            if (times.isEmpty()) {
                continue;
            }
            String text = line.substring(end).trim();
            if (text.length() > 200) {
                text = text.substring(0, 200);
            }
            for (long t : times) {
                out.add(new LyricLine(Math.max(0, t - offset), text));
            }
        }
        out.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs()));
        return out;
    }
}
