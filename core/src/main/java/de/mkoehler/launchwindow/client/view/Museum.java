package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.GoalRecord;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.rules.WinCondition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The Museum's wording and order ({@code ui-spec.md} &sect;4.11): the race, achievements, flights
 * and the memorial. Who won comes from the server's result; the race order and the medal table
 * come from {@code core}'s {@link WinCondition} over the rival summaries (gap G3).
 *
 * @author Mario Koehler
 */
public final class Museum {

    private Museum() {
    }

    /**
     * One declared goal's cell in a race row.
     *
     * @param done  whether it's done
     * @param when  the date it was done, or "Not yet"
     * @param first whether no agency did it earlier
     */
    public record Cell(boolean done, String when, boolean first) {
    }

    /**
     * One agency's row in the race.
     *
     * @param place     its place (winners share 1)
     * @param agency    the agency, with "(you)" for the player
     * @param director  its director
     * @param archetype its archetype, as a word
     * @param you       whether it's the player
     * @param winner    whether it won (or shares the win)
     * @param cells     one per declared goal, in declared order
     * @param count     how many declared goals it holds
     * @param finished  the date it finished them all, or "—"
     */
    public record RaceRow(int place, String agency, String director, String archetype, boolean you, boolean winner,
                          List<Cell> cells, int count, String finished) {
    }

    /**
     * Returns the date a turn resolved into, as the race shows it.
     *
     * @param turn the game turn
     * @return the date
     */
    public static String date(int turn) {
        return GameDate.ofTurn(turn).label();
    }

    /**
     * Returns an archetype as a word.
     *
     * @param archetype the archetype
     * @return "NASA" or "Soviet"
     */
    public static String archetype(Archetype archetype) {
        return archetype == Archetype.NASA ? "NASA" : "Soviet";
    }

    /**
     * Returns the race outcome over the rival summaries.
     *
     * @param rivals   every player's summary
     * @param declared the declared goals
     * @return the outcome, keyed by each rival's index as a string
     */
    public static WinCondition.Outcome outcome(List<RivalSummary> rivals, List<String> declared) {
        return WinCondition.evaluateTimes(times(rivals), declared);
    }

    /**
     * Returns the medal table, when several agencies finished at the same turn.
     *
     * @param rivals   every player's summary
     * @param declared the declared goals
     * @return the table, keyed by each rival's index as a string, or empty
     */
    public static Optional<WinCondition.MedalTable> medalTable(List<RivalSummary> rivals, List<String> declared) {
        Map<String, Map<String, Integer>> times = times(rivals);
        List<String> tied = WinCondition.sameTurnFinishers(times, declared);
        return tied.size() < 2 ? Optional.empty() : Optional.of(WinCondition.medalTable(tied, times, declared));
    }

    private static Map<String, Map<String, Integer>> times(List<RivalSummary> rivals) {
        Map<String, Map<String, Integer>> times = new LinkedHashMap<>();
        for (int i = 0; i < rivals.size(); i++) {
            times.put(String.valueOf(i), rivals.get(i).goalTurns());
        }
        return times;
    }

    /**
     * Returns the race table: winners first, then by goals held, then by who finished their last
     * goal earliest.
     *
     * @param rivals   every player's summary
     * @param declared the declared goals
     * @return the rows
     */
    public static List<RaceRow> race(List<RivalSummary> rivals, List<String> declared) {
        Map<String, Integer> firstTurn = new LinkedHashMap<>();
        for (String goalId : declared) {
            rivals.stream().map(rival -> rival.goalTurns().get(goalId)).filter(java.util.Objects::nonNull)
                .min(Integer::compare).ifPresent(turn -> firstTurn.put(goalId, turn));
        }
        List<String> winners = outcome(rivals, declared).winners();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rivals.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.<Integer>comparingInt(i -> winners.contains(String.valueOf(i)) ? 0 : 1)
            .thenComparing(i -> -count(rivals.get(i), declared))
            .thenComparingInt(i -> lastDone(rivals.get(i), declared)));
        List<RaceRow> rows = new ArrayList<>();
        for (int position = 0; position < order.size(); position++) {
            int index = order.get(position);
            RivalSummary rival = rivals.get(index);
            boolean winner = winners.contains(String.valueOf(index));
            List<Cell> cells = new ArrayList<>();
            for (String goalId : declared) {
                Integer turn = rival.goalTurns().get(goalId);
                cells.add(turn == null ? new Cell(false, "Not yet", false)
                    : new Cell(true, date(turn), turn.equals(firstTurn.get(goalId))));
            }
            int count = count(rival, declared);
            rows.add(new RaceRow(winner ? 1 : position + 1, rival.agencyName() + (rival.you() ? " (you)" : ""),
                rival.directorName(), archetype(rival.archetype()), rival.you(), winner, cells, count,
                WinCondition.completionTurn(rival.goalTurns(), declared).map(Museum::date).orElse("—")));
        }
        return rows;
    }

    private static int count(RivalSummary rival, List<String> declared) {
        return (int) declared.stream().filter(rival.goalTurns()::containsKey).count();
    }

    private static int lastDone(RivalSummary rival, List<String> declared) {
        return declared.stream().map(rival.goalTurns()::get).filter(java.util.Objects::nonNull)
            .max(Integer::compare).orElse(Integer.MAX_VALUE);
    }

    /**
     * One goal on the achievements tab.
     *
     * @param goalId   the goal
     * @param name     its name
     * @param done     whether the agency holds it
     * @param how      how it was reached or credited, or "Open"
     * @param first    whether it was first in the game
     * @param prestige the prestige paid, or the open goal's first and later prestige
     * @param when     the date it was done, or {@code ""}
     * @param turn     the turn it was done, for ordering ({@link Integer#MAX_VALUE} when open)
     */
    public record Achievement(String goalId, String name, boolean done, String how, boolean first, String prestige,
                              String when, int turn) {
    }

    /**
     * The achievements tab's summary strip.
     *
     * @param done     goals held
     * @param total    goals in the pack
     * @param firsts   goals held that were first in the game
     * @param credited goals held through the cascade
     * @param prestige prestige paid by goals with a record
     */
    public record AchievementSummary(int done, int total, int firsts, int credited, long prestige) {
    }

    /**
     * Returns every goal of the pack: the held ones first, by turn, then the open ones in pack
     * order. A held goal settled before goal records existed says only "Reached" or "Credited".
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the goals
     */
    public static List<Achievement> achievements(PlayerSnapshot state, ContentCatalog catalog) {
        Map<String, GoalRecord> records = new LinkedHashMap<>();
        for (GoalRecord record : state.getGoalRecords()) {
            records.putIfAbsent(record.goalId(), record);
        }
        List<String> all = catalog.goals().stream().map(Goal::id).toList();
        Map<String, Integer> times = WinCondition.goalTimes(state, all, catalog);
        List<Achievement> rows = new ArrayList<>();
        for (Goal goal : catalog.goals()) {
            String name = ContentNames.of(goal.id());
            if (!state.hasAchievedOrCancelledGoal(goal.id())) {
                rows.add(new Achievement(goal.id(), name, false, "Open", false, String.format(Locale.ROOT,
                    "%,d first · %,d after", goal.firstTimePrestige(), goal.secondTimePrestige()), "",
                    Integer.MAX_VALUE));
                continue;
            }
            GoalRecord record = records.get(goal.id());
            int turn = record != null ? record.turn() : times.getOrDefault(goal.id(), 0);
            rows.add(new Achievement(goal.id(), name, true, how(goal.id(), record, state, catalog),
                record != null && record.firstInGame(),
                record == null ? "" : String.format(Locale.ROOT, "+ %,d", record.prestige()),
                turn > 0 ? date(turn) : "", turn));
        }
        rows.sort(Comparator.comparingInt(Achievement::turn));
        return rows;
    }

    private static String how(String goalId, GoalRecord record, PlayerSnapshot state, ContentCatalog catalog) {
        if (record == null) {
            return state.getCancelledGoalIds().contains(goalId) ? "Credited through the cascade" : "Reached";
        }
        if (record.creditedThroughGoalId() != null) {
            return "Credited through " + ContentNames.of(record.creditedThroughGoalId());
        }
        return catalog.missionConfiguration(record.missionConfigurationId())
            .map(configuration -> "Reached by " + MissionControl.missionName(configuration)).orElse("Reached");
    }

    /**
     * Sums up the achievements.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the summary
     */
    public static AchievementSummary summary(PlayerSnapshot state, ContentCatalog catalog) {
        int done = (int) catalog.goals().stream().filter(goal -> state.hasAchievedOrCancelledGoal(goal.id())).count();
        int credited = (int) catalog.goals().stream()
            .filter(goal -> state.getCancelledGoalIds().contains(goal.id())).count();
        int firsts = (int) state.getGoalRecords().stream().filter(GoalRecord::firstInGame).count();
        long prestige = state.getGoalRecords().stream().mapToLong(GoalRecord::prestige).sum();
        return new AchievementSummary(done, catalog.goals().size(), firsts, credited, prestige);
    }

    /**
     * One configuration on the flights tab.
     *
     * @param configurationId the configuration
     * @param program         its program's name
     * @param name            its name
     * @param count           its completed flights
     * @param crewed          whether it flies a crew
     */
    public record Flight(String configurationId, String program, String name, int count, boolean crewed) {
    }

    /**
     * Returns every configuration that completed a flight, in the pack's order.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the flights
     */
    public static List<Flight> flights(PlayerSnapshot state, ContentCatalog catalog) {
        List<Flight> flights = new ArrayList<>();
        for (MissionConfiguration configuration : catalog.missionConfigurations()) {
            int count = state.getSuccessfulFlightCounts().getOrDefault(configuration.id(), 0);
            if (count > 0) {
                flights.add(new Flight(configuration.id(), ContentNames.of(configuration.programId()),
                    MissionControl.missionName(configuration), count, configuration.requiredAstronautCount() > 0));
            }
        }
        return flights;
    }

    /**
     * One plaque on the memorial.
     *
     * @param name   the astronaut
     * @param line   how and when they were lost, and their age
     * @param detail the mission and step, or the accident's context
     */
    public record Plaque(String name, String line, String detail) {
    }

    /**
     * Returns the memorial's plaques, oldest first.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the plaques
     */
    public static List<Plaque> memorial(PlayerSnapshot state, ContentCatalog catalog) {
        List<Plaque> plaques = new ArrayList<>();
        for (MemorialEntry entry : state.getMemorial()) {
            boolean mission = entry.cause() == MemorialEntry.Cause.MISSION;
            String cause = mission ? "Lost on a mission" : "Died in an accident";
            String detail;
            if (mission) {
                String flight = catalog.missionConfiguration(entry.missionConfigurationId())
                    .map(MissionControl::missionName).orElse(ContentNames.of(entry.missionConfigurationId()));
                detail = "Lost with the crew of " + flight + ", on the " + TurnReportView.stepName(entry.missionConfigurationId(), entry.stepId())
                    .toLowerCase(Locale.ROOT) + " step.";
            } else {
                detail = "Not flying when it happened: reported in that turn's news.";
            }
            plaques.add(new Plaque(entry.name(), cause + ", " + date(entry.turn()) + ", aged " + entry.age() + ".",
                detail));
        }
        return plaques;
    }
}
