package de.mkoehler.launchwindow.devtools.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * <p>How long the race runs, and how long players wait between flights, with R&amp;D tuned
 * (design.md 2.6). Plays the bot that works ahead on a copy of a pack whose components research
 * faster ({@code rdFactor} times every R&amp;D band) and start more reliable ({@code startShare} of
 * the way from their baseline to their R&amp;D ceiling), over a grid of both.</p>
 *
 * <p>A lull is a run of seasons without a launch: with no flight to plan, the turn report has
 * R&amp;D progress and the odd event, and the player has nothing to decide.</p>
 *
 * <p>Run with
 * {@code mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.RaceLulls}
 * from the repo root ({@code -Dexec.args=early-era} for the early race).</p>
 *
 * @author Mario Koehler
 */
public final class RaceLulls {

    private static final double[] RD_FACTORS = {1.0, 1.5, 2.0, 3.0};
    private static final double[] START_SHARES = {0.0, 0.25, 0.5};

    private RaceLulls() {
    }

    /**
     * Loads a copy of a pack with its R&amp;D tuned.
     *
     * @param pack       the pack's folder
     * @param rdFactor   the factor on every component's R&amp;D bands
     * @param startShare how far each component starts from its baseline toward its R&amp;D ceiling, 0 to 1
     * @return the tuned catalog
     */
    public static ContentCatalog tuned(Path pack, double rdFactor, double startShare) {
        try {
            Path copy = Files.createTempDirectory("lw-tuned-pack");
            ObjectMapper mapper = new ObjectMapper();
            for (String file : ContentPackLoader.FILES) {
                Path source = pack.resolve(file);
                if (!Files.exists(source)) {
                    continue;
                }
                if (!file.equals(ContentPackLoader.MISSION_COMPONENTS_FILE)) {
                    Files.copy(source, copy.resolve(file));
                    continue;
                }
                ArrayNode components = (ArrayNode) mapper.readTree(source.toFile());
                for (var node : components) {
                    ObjectNode component = (ObjectNode) node;
                    ArrayNode bands = (ArrayNode) component.get("rdSlotBandMaxAmounts");
                    for (int i = 0; i < bands.size(); i++) {
                        bands.set(i, bands.get(i).asDouble() * rdFactor);
                    }
                    double baseline = component.get("baselineReliability").asDouble();
                    double ceiling = component.get("maxRdReliability").asDouble();
                    component.put("baselineReliability", baseline + startShare * Math.max(0, ceiling - baseline));
                }
                mapper.writeValue(copy.resolve(file).toFile(), components);
            }
            return ContentPackLoader.load(copy);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Plays 200 games per grid cell and prints the race's length and its lulls.
     *
     * @param args the pack's folder under {@code assets/content}, {@code standard} if none is given; then
     *             {@code ahead} (the default) or {@code side} for the bot; then an R&amp;D factor and a start
     *             share to print one cell in full, and optionally its seed
     */
    public static void main(String[] args) {
        Path pack = Path.of("assets", "content", args.length > 0 ? args[0] : "standard");
        ContentPackPlayouts.Bot bot = args.length > 1 && args[1].startsWith("side")
            ? ContentPackPlayouts.Bot.SIDE_MISSIONS : ContentPackPlayouts.Bot.AHEAD;
        // "ahead+contracts" or "side+contracts" plays with commercial contracts (design.md 2.11), three
        // equally strong rivals on every listing.
        int rivals = args.length > 1 && args[1].endsWith("+contracts") ? 3 : -1;
        if (args.length >= 4) {
            // One cell in full: where its seasons go.
            ContentPackPlayouts harness = new ContentPackPlayouts(
                tuned(pack, Double.parseDouble(args[2]), Double.parseDouble(args[3])), bot).withContracts(rivals);
            ContentPackPlayouts.Report report = harness.run(200, 240, args.length > 4 ? Long.parseLong(args[4]) : 42L);
            System.out.println("Stalls per game: " + report.stalls().entrySet().stream()
                .map(e -> e.getKey() + " " + String.format(Locale.ROOT, "%.1f", e.getValue() / 200.0)).toList());
            System.out.println("Seasons as the target, per game of its agency: " + report.targetSeasons().entrySet()
                .stream().map(e -> e.getKey() + " " + String.format(Locale.ROOT, "%.1f", e.getValue() / 100.0))
                .toList());
            System.out.println("Lowest funds " + report.lowestFunds() + ", debt conversions "
                + report.debtConversions());
            System.out.println("Side launches: " + report.sideKinds() + " " + report.sideLaunches());
            System.out.println("Side flights passed over, per game: " + report.sideRejections().entrySet().stream()
                .map(e -> e.getKey() + " " + String.format(Locale.ROOT, "%.1f", e.getValue() / 200.0)).toList());
            print(report, Double.parseDouble(args[2]), Double.parseDouble(args[3]));
            System.out.println("Contracts over 200 games: " + harness.contracts() + ", profit per game "
                + harness.contractProfit() / 200);
            return;
        }
        System.out.println("R&D x, start: completion median NASA / Soviet (done of 200) | launches per game | "
            + "gap between launches median / 90th pct | gaps of 5+ / 10+ seasons | longest gap median | "
            + "seasons without a launch | side launches per game");
        for (double rdFactor : RD_FACTORS) {
            for (double startShare : START_SHARES) {
                ContentPackPlayouts harness = new ContentPackPlayouts(tuned(pack, rdFactor, startShare), bot)
                    .withContracts(rivals);
                int seasons = harness.declaredGoals().equals(ContentPackPlayouts.MOON_DECLARED_GOALS) ? 240 : 80;
                ContentPackPlayouts.Report report = harness.run(200, seasons, 42L);
                print(report, rdFactor, startShare);
            }
        }
    }

    private static void print(ContentPackPlayouts.Report report, double rdFactor, double startShare) {
        List<Integer> gaps = report.launchGaps().stream().sorted().toList();
        int done = report.completedByArchetype().values().stream().mapToInt(Integer::intValue).sum();
        int launches = gaps.size();
        int idle = gaps.stream().mapToInt(Integer::intValue).sum();
        System.out.printf(Locale.ROOT,
            "x%.1f, %2.0f %%: %s / %s (%d) | %.1f | %d / %d | %.0f %% / %.0f %% | %d | %.0f %% | %.1f%n",
            rdFactor, startShare * 100, median(report, Archetype.NASA), median(report, Archetype.SOVIET),
            done, launches / (double) Math.max(1, done), percentile(gaps, 50), percentile(gaps, 90),
            100.0 * gaps.stream().filter(gap -> gap >= 5).count() / Math.max(1, launches),
            100.0 * gaps.stream().filter(gap -> gap >= 10).count() / Math.max(1, launches),
            percentile(report.longestGaps().stream().sorted().toList(), 50),
            100.0 * idle / Math.max(1, idle + launches),
            report.sideLaunches().values().stream().mapToInt(Integer::intValue).sum() / 200.0);
    }

    private static String median(ContentPackPlayouts.Report report, Archetype archetype) {
        List<Integer> seasons = report.completionSeasons().getOrDefault(archetype, List.of());
        return seasons.isEmpty() ? "-" : String.valueOf(percentile(seasons.stream().sorted().toList(), 50));
    }

    private static int percentile(List<Integer> sorted, int percent) {
        return sorted.isEmpty() ? 0 : sorted.get(Math.min(sorted.size() - 1, sorted.size() * percent / 100));
    }
}
