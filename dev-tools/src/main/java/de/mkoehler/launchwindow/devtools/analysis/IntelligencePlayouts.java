package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.IntelFinding;
import de.mkoehler.launchwindow.rules.Intelligence;
import de.mkoehler.launchwindow.rules.IntelligenceBoard;
import de.mkoehler.launchwindow.rules.IntelligenceFound;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.ResolutionEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * What the intelligence service costs and brings ({@code design.md} &sect;2.9): agencies that each
 * hold one target level play a game's worth of seasons against each other, spending every action
 * every season (assessments of random rivals; at level 3 one personnel file a season), and the
 * report shows, per level, what it cost, what it learned, and how often its agents were caught. Only
 * the intelligence rules run (the board and the service's season step, the passive candidate find
 * on a fresh pool); the rest of the season pass doesn't affect them.
 *
 * <p>Run: {@code mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.IntelligencePlayouts}</p>
 *
 * @author Mario Koehler
 */
public final class IntelligencePlayouts {

    private static final int GAMES = 1_000;
    private static final int SEASONS = 24;

    private final ContentCatalog catalog;

    private IntelligencePlayouts(ContentCatalog catalog) {
        this.catalog = catalog;
    }

    /** One level's totals over every game, per agency holding it. */
    private static final class Totals {
        int agencies;
        long cost;
        int actions;
        int dossiers;
        int files;
        int tidbits;
        int failed;
        int caught;
        int caughtTheirs;
        long prestigeLost;
        int candidates;
        int passiveFinds;
        int seasonsWithLevel;
        int knownRivalSeasons;
        int rivalSeasons;
    }

    private Map<String, Totals> run(int[] targets, long seed) {
        Random random = new Random(seed);
        Map<String, Totals> totals = new LinkedHashMap<>();
        for (int game = 0; game < GAMES; game++) {
            Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
            for (int i = 0; i < targets.length; i++) {
                PlayerSnapshot agency = new PlayerSnapshot("Agency " + game + "-" + i,
                    i % 2 == 0 ? Archetype.NASA : Archetype.SOVIET, 25_000, 1_000, 3_000, 1);
                HiringPool.refreshCandidates(agency, new java.util.SplittableRandom(random.nextLong()));
                agency.setAstronauts(new ArrayList<>(agency.getAstronautCandidates().subList(0, 6)));
                agency.setSetSpecialists(new ArrayList<>(agency.getSetSpecialistCandidates().subList(0, 8)));
                agency.setMissionControlOfficers(new ArrayList<>(agency.getMissionControlCandidates().subList(0, 6)));
                // Some hardware and programs, so every area has something to find.
                for (String component : List.of("r7-sputnik", "sputnik-1-satellite", "vostok-capsule")) {
                    agency.getMissionComponents().put(component,
                        new de.mkoehler.launchwindow.model.OwnedMissionComponent(70, 50));
                }
                agency.getOpenedProgramIds().addAll(List.of("sputnik-1", "vostok"));
                Intelligence.setTarget(agency, targets[i]);
                agencies.put("k" + i, agency);
                totals.computeIfAbsent("L" + targets[i], key -> new Totals()).agencies++;
            }
            for (int season = 1; season <= SEASONS; season++) {
                for (Map.Entry<String, PlayerSnapshot> entry : agencies.entrySet()) {
                    queueEverything(entry.getKey(), entry.getValue(), agencies, random);
                }
                Map<String, List<IntelFinding>> findings = IntelligenceBoard.settle(agencies, catalog,
                    random.nextLong(), season);
                for (int i = 0; i < targets.length; i++) {
                    String key = "k" + i;
                    PlayerSnapshot agency = agencies.get(key);
                    Totals total = totals.get("L" + targets[i]);
                    long prestige = agency.getPrestige();
                    List<ResolutionEvent> events = Intelligence.advanceSeason(agency,
                        findings.getOrDefault(key, List.of()), season);
                    total.prestigeLost += prestige - agency.getPrestige();
                    for (ResolutionEvent event : events) {
                        if (event instanceof IntelligenceFound found) {
                            switch (found.finding().kind()) {
                                case DOSSIER -> total.dossiers++;
                                case PERSONNEL_FILE -> total.files++;
                                case TIDBIT -> total.tidbits++;
                                case FAILED -> total.failed++;
                                case CAUGHT -> total.caught++;
                                case CAUGHT_THEIRS -> total.caughtTheirs++;
                                default -> { }
                            }
                        }
                    }
                    int level = agency.getIntelligence().getLevel();
                    total.cost += Intelligence.cost(level);
                    if (level == targets[i]) {
                        total.seasonsWithLevel++;
                    }
                    HiringPool.refreshCandidates(agency, new java.util.SplittableRandom(random.nextLong()));
                    total.candidates += (int) (agency.getAstronautCandidates().size()
                        + agency.getSetSpecialistCandidates().size() + agency.getMissionControlCandidates().size());
                    total.passiveFinds += (int) java.util.stream.Stream.of(agency.getAstronautCandidates(),
                        agency.getSetSpecialistCandidates(), agency.getMissionControlCandidates())
                        .flatMap(List::stream).filter(candidate -> candidate.getIntelFind() != null).count();
                    for (String rival : agencies.keySet()) {
                        if (!rival.equals(key)) {
                            total.rivalSeasons++;
                            RivalFolder folder = agency.getIntelligence().getFolders().get(rival);
                            if (folder != null && folder.getKnownLevel() >= 0) {
                                total.knownRivalSeasons++;
                            }
                        }
                    }
                }
            }
        }
        return totals;
    }

    /** Spends every action: assessments of random rivals and areas, and at level 3 one personnel file. */
    private static void queueEverything(String self, PlayerSnapshot agency, Map<String, PlayerSnapshot> agencies,
            Random random) {
        List<String> rivals = agencies.keySet().stream().filter(key -> !key.equals(self)).toList();
        int level = agency.getIntelligence().getLevel();
        if (level < Intelligence.ASSESSMENT_LEVEL) {
            return;
        }
        boolean file = level >= Intelligence.PERSONNEL_FILE_LEVEL;
        while (Intelligence.actionsLeft(agency) > 0) {
            String rival = rivals.get(random.nextInt(rivals.size()));
            if (file) {
                Intelligence.queue(agency, IntelOperation.Kind.PERSONNEL_FILE, rival, null,
                    PersonnelGroup.values()[random.nextInt(3)].name(), null);
                file = false;
            } else {
                Intelligence.queue(agency, IntelOperation.Kind.ASSESSMENT, rival,
                    IntelArea.values()[random.nextInt(3)], null, null);
            }
        }
    }

    private static void print(String title, Map<String, Totals> totals) {
        System.out.println(title);
        System.out.println("  level  cost/season  dossiers  files  tidbits  failed  caught  prestige lost"
            + "  theirs caught  rival levels known  passive finds/pool");
        for (Map.Entry<String, Totals> entry : totals.entrySet()) {
            Totals t = entry.getValue();
            double perAgency = t.agencies;
            int actions = t.dossiers + t.files + t.failed + t.caught;
            System.out.printf("  %-5s  %,11.0f  %8.1f  %5.1f  %7.1f  %6.1f  %6.1f  %13.1f  %13.1f  %17.0f%%  %18.1f%n",
                entry.getKey(), t.cost / perAgency / SEASONS, t.dossiers / perAgency, t.files / perAgency,
                t.tidbits / perAgency, t.failed / perAgency, t.caught / perAgency, t.prestigeLost / perAgency,
                t.caughtTheirs / perAgency, 100.0 * t.knownRivalSeasons / Math.max(1, t.rivalSeasons),
                t.passiveFinds / (double) (t.agencies * SEASONS));
            if (actions > 0) {
                System.out.printf("         operations: %.0f%% succeeded, %.0f%% caught%n",
                    100.0 * (t.dossiers + t.files) / actions, 100.0 * t.caught / actions);
            }
        }
        System.out.println("  (totals per agency over " + SEASONS + " seasons, " + GAMES + " games; funding starts at "
            + "3,000 a season)");
    }

    /**
     * Plays the intelligence-only games and prints the reports.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        ContentCatalog catalog = ContentPackLoader.load(Path.of("assets", "content", "early-era"));
        IntelligencePlayouts playouts = new IntelligencePlayouts(catalog);
        print("Four agencies at targets 0, 1, 2 and 3:", playouts.run(new int[] {0, 1, 2, 3}, 7L));
        print("Four agencies, all at target 3 (an arms race):", playouts.run(new int[] {3, 3, 3, 3}, 7L));
        print("Four agencies, all at target 2:", playouts.run(new int[] {2, 2, 2, 2}, 7L));
    }
}
