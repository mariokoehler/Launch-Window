package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.net.NetworkConstants;

import java.nio.file.Path;

/**
 * How big a player's state gets on the wire over a long game ({@code design.md} &sect;3.10): the
 * bot playouts played to 80 seasons, the client's stripped copy sent as a {@code FetchStateResponse}
 * through Kryo each season, against Kryo's 64 KB object buffer. The state isn't gzipped (the turn
 * report is), so this is the margin left.
 *
 * <p>Run: {@code mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.StateSizes}</p>
 *
 * @author Mario Koehler
 */
public final class StateSizes {

    private StateSizes() {
    }

    /**
     * Plays 200 games of 80 seasons and prints the state's size every eight seasons.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        ContentCatalog catalog = ContentPackLoader.load(Path.of("assets", "content", "early-era"));
        ContentPackPlayouts.SizeReport report = new ContentPackPlayouts(catalog).sizes(200, 80, 42L);
        int buffer = NetworkConstants.OBJECT_BUFFER_SIZE;
        System.out.println(report.games() + " games; the state a client receives, in bytes (Kryo buffer "
            + buffer + "):");
        int largestEver = 0;
        for (int season = 0; season < report.largest().length; season++) {
            largestEver = Math.max(largestEver, report.largest()[season]);
            if ((season + 1) % 8 == 0 || season == 0) {
                System.out.printf("  season %2d: average %,6.0f, largest %,6d (%.0f %% of the buffer)%n", season + 1,
                    report.average()[season], report.largest()[season], 100.0 * report.largest()[season] / buffer);
            }
        }
        System.out.printf("Largest seen: %,d bytes, %.0f %% of the buffer%n", largestEver, 100.0 * largestEver / buffer);
    }
}
