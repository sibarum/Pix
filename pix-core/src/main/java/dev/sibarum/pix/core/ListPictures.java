package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.ListFolder;
import dev.sibarum.pix.core.Messages.Listed;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.codec.video.Videos;
import dev.vexelray.framework.api.Job;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The pictures in a folder, in the order they are stepped through, on the offload lane. The only place Rasterware lists a
 * folder.
 *
 * <p>By name, ignoring case, with runs of digits compared as numbers, so {@code IMG_9} comes before {@code IMG_10} as it
 * does in Explorer. Hidden files are left out. Which files are pictures is a guess from the name; the decoder decides
 * when the file is read. A folder that cannot be read answers empty.
 */
@Job
public final class ListPictures {

    public Listed run(ListFolder l) {
        return new Listed(l.folder(), pictures(l.folder()));
    }

    static List<Path> pictures(Path folder) {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> in = Files.newDirectoryStream(folder)) {
            for (Path p : in) {
                if ((Pictures.named(p) || Videos.named(p)) && Files.isRegularFile(p) && !hidden(p)) {
                    out.add(p);
                }
            }
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        out.sort(Comparator.comparing(p -> p.getFileName().toString(), ListPictures::natural));
        return out;
    }

    private static boolean hidden(Path p) {
        try {
            return Files.isHidden(p);
        } catch (IOException e) {
            return false;
        }
    }

    /** Names compared ignoring case, with each run of digits compared as the number it spells. */
    static int natural(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            char x = a.charAt(i);
            char y = b.charAt(j);
            if (Character.isDigit(x) && Character.isDigit(y)) {
                int si = i;
                int sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) {
                    i++;
                }
                while (j < b.length() && Character.isDigit(b.charAt(j))) {
                    j++;
                }
                String da = number(a.substring(si, i));
                String db = number(b.substring(sj, j));
                int c = da.length() != db.length() ? Integer.compare(da.length(), db.length()) : da.compareTo(db);
                if (c != 0) {
                    return c;
                }
            } else {
                int c = Character.compare(Character.toLowerCase(x), Character.toLowerCase(y));
                if (c != 0) {
                    return c;
                }
                i++;
                j++;
            }
        }
        int c = Integer.compare(a.length() - i, b.length() - j);
        return c != 0 ? c : a.compareTo(b);
    }

    /** A run of digits without its leading zeros, so that its length orders it. */
    private static String number(String digits) {
        int k = 0;
        while (k < digits.length() - 1 && digits.charAt(k) == '0') {
            k++;
        }
        return digits.substring(k);
    }
}
