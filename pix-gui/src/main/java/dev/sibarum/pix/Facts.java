package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.Pictured;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Details panel says of a picture, row by row ({@link DetailsPanel#ROWS}), worked out from what was read of
 * it: pure, so a test needs no window (docs/details.md).
 */
final class Facts {

    /** A picture's own size in its identity: {@code 883 × 590}. */
    private static final Pattern SIZE = Pattern.compile("(\\d+) × (\\d+)");

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.UK);

    private Facts() {
    }

    /** Format, dimensions, aspect, file size, modified, folder. */
    static List<String> of(Pictured p, ZoneId zone, Path home) {
        String format = format(p.identity());
        int[] size = size(p.identity());
        return List.of(format,
                size == null ? "" : size[0] + " × " + size[1] + " px",
                size == null ? "" : aspect(size[0], size[1]),
                p.bytes() < 0 ? "" : Details.bytes(p.bytes()),
                p.modified() < 0 ? "" : WHEN.format(Instant.ofEpochMilli(p.modified()).atZone(zone)),
                folder(p.path(), home));
    }

    /** The identity without its size: {@code PNG image}, {@code SDG drawing · 5 layers}. */
    static String format(String identity) {
        List<String> parts = new ArrayList<>();
        for (String part : identity.split(" · ")) {
            if (!SIZE.matcher(part).matches()) {
                parts.add(part);
            }
        }
        return String.join(" · ", parts);
    }

    /** The size the identity names, width then height; null with none. */
    static int[] size(String identity) {
        Matcher m = SIZE.matcher(identity);
        return m.find() ? new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))} : null;
    }

    /**
     * {@code w:h} reduced when both stay small ({@code 3:2}, {@code 16:9}, {@code 1:1}); otherwise the longer to the
     * shorter to two places, as {@code 1.50:1} or {@code 1:1.50}.
     */
    static String aspect(int w, int h) {
        if (w <= 0 || h <= 0) {
            return "";
        }
        int g = gcd(w, h);
        if (w / g <= 32 && h / g <= 32) {
            return w / g + ":" + h / g;
        }
        return w >= h ? String.format(Locale.ROOT, "%.2f:1", w / (double) h)
                : String.format(Locale.ROOT, "1:%.2f", h / (double) w);
    }

    /** The folder the picture is in, from the home folder ({@code ~/Pictures}) when it is under it. */
    static String folder(Path path, Path home) {
        Path parent = path.toAbsolutePath().getParent();
        if (parent == null) {
            return "";
        }
        if (home != null && parent.startsWith(home)) {
            Path rel = home.relativize(parent);
            return rel.toString().isEmpty() ? "~" : "~/" + rel.toString().replace('\\', '/');
        }
        return parent.toString();
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
