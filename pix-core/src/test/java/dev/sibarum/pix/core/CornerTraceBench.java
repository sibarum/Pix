package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.TraceSettings;
import dev.vexelray.fields.art.Raster;
import dev.vexelray.fields.convert.Convert;
import dev.vexelray.fields.sdg.Png;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.SdgFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * FN-22's prototype measured: each picture traced as the panel's Save does, then fitted, both drawn at {@link #ZOOM}
 * times, with their sizes and times. Not a test: runs only with {@code -Dbench=<out dir>}, and pictures from
 * {@code -Dbench.pictures=<a;b;...>} as well as one drawn here.
 */
@EnabledIfSystemProperty(named = "bench", matches = ".+")
class CornerTraceBench {

    static final int ZOOM = 4;
    static final int COLOURS = 6;

    @Test
    void bench() throws IOException {
        Path out = Path.of(System.getProperty("bench"));
        Files.createDirectories(out);
        List<Path> pictures = new ArrayList<>();
        pictures.add(shapes(out.resolve("shapes.png")));
        for (String p : System.getProperty("bench.pictures", "").split(";")) {
            if (!p.isBlank()) {
                pictures.add(Path.of(p));
            }
        }
        StringBuilder report = new StringBuilder(
                "picture | trace ms | trace bytes | pieces | fitted ms @1 | bytes @1 | fitted ms @0.5 | bytes @0.5\n");
        for (Path p : pictures) {
            Raster r = Traces.raster(p);
            String name = p.getFileName().toString().replaceAll("\\.[^.]+$", "");
            long t0 = System.nanoTime();
            Sdg traced = Traces.trace(r, new TraceSettings(COLOURS, false), true);
            long t1 = System.nanoTime();
            String svg = CornerTrace.svg(traced, CornerFit.Options.SHARP);
            Files.writeString(out.resolve(name + ".fitted.svg"), svg);
            int w = (int) Math.round(traced.width()) * ZOOM, h = (int) Math.round(traced.height()) * ZOOM;
            Files.write(out.resolve(name + ".source.png"), Png.write(r.width(), r.height(), r.rgba()));
            Files.write(out.resolve(name + ".traced.png"), Png.write(w, h, Convert.sdgToRaster(w, h).apply(traced).rgba()));
            report.append(String.format("%s | %d | %d | %d", name, (t1 - t0) / 1_000_000,
                    SdgFile.xml(traced).getBytes(StandardCharsets.UTF_8).length,
                    svg.chars().filter(c -> c == 'L' || c == 'C').count()));
            for (double density : new double[]{1, 0.5}) {
                long t2 = System.nanoTime();
                Sdg fitted;
                try {
                    fitted = CornerTrace.fitted(traced, CornerFit.Options.SHARP, density);
                } catch (RuntimeException e) {
                    report.append(" | failed: ").append(e);
                    continue;
                }
                long t3 = System.nanoTime();
                Files.write(out.resolve(name + ".fitted@" + density + ".png"),
                        Png.write(w, h, Convert.sdgToRaster(w, h).apply(fitted).rgba()));
                Files.writeString(out.resolve(name + ".fitted@" + density + ".sdg"), SdgFile.xml(fitted));
                report.append(String.format(" | %d | %d", (t3 - t2) / 1_000_000,
                        SdgFile.xml(fitted).getBytes(StandardCharsets.UTF_8).length));
            }
            report.append('\n');
        }
        Files.writeString(out.resolve("report.txt"), report);
    }

    /** Hard corners and smooth curves, anti-aliased as a screenshot's are. */
    private static Path shapes(Path to) throws IOException {
        BufferedImage img = new BufferedImage(240, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 240, 160);
        g.setColor(new Color(0xd03030));
        g.fillPolygon(new int[]{20, 100, 60}, new int[]{140, 140, 20}, 3);
        g.setColor(new Color(0x3050c0));
        g.fillOval(120, 20, 50, 50);
        g.setColor(new Color(0x208040));
        g.fillPolygon(new int[]{150, 165, 205, 170, 185, 150, 115, 130, 95, 135}, new int[]{80, 110, 112, 130, 158, 140, 158,
                130, 112, 110}, 10);
        g.setColor(new Color(0x202020));
        g.fillRect(185, 25, 40, 40);
        g.dispose();
        byte[] rgba = new byte[240 * 160 * 4];
        for (int y = 0; y < 160; y++) {
            for (int x = 0; x < 240; x++) {
                int c = img.getRGB(x, y), i = (y * 240 + x) * 4;
                rgba[i] = (byte) (c >> 16);
                rgba[i + 1] = (byte) (c >> 8);
                rgba[i + 2] = (byte) c;
                rgba[i + 3] = (byte) 0xff;
            }
        }
        Files.write(to, Png.write(240, 160, rgba));
        return to;
    }
}
