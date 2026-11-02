package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Season;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.net.messages.StandingSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the standing chart shows ({@code ui-spec.md} component 30), without drawing it: the window of
 * turns, the lines with their marks, the axes' ticks, and the end labels pushed apart.
 *
 * @author Mario Koehler
 */
public final class Standing {

    /** Dots on the points up to this many points; plain lines after. */
    public static final int DOTS_UP_TO = 40;

    /** With this few points or fewer the dots grow, and the axis names each season. */
    public static final int BIG_DOTS_UP_TO = 6;

    /** The points a window shows: 30 game years. */
    public static final int WINDOW = 120;

    /** The colours a rival's line cycles through; beyond them the lines repeat, dashed. */
    public static final int RIVAL_COLOURS = 4;

    /** What happened at a point. */
    public enum EventKind {
        /** A declared goal, the first in the game. */
        FIRST,
        /** A declared goal. */
        GOAL,
        /** A crew of the player's was lost. */
        CREW,
        /** A budget review of the player's. */
        REVIEW
    }

    /**
     * Something that happened at a point.
     *
     * @param turn   the turn
     * @param kind   what it was
     * @param colour the line it's on: a rival's colour index, or -1 for the player's
     * @param text   the sentence for the tooltip
     */
    public record Event(long turn, EventKind kind, int colour, String text) {
    }

    /**
     * One line.
     *
     * @param name      the agency, as the race names it
     * @param you       whether it's the player's
     * @param colour    a rival's colour index, or -1 for the player's
     * @param firstTurn the turn of the first value
     * @param values    one value a turn
     * @param goals     its declared goals, as events
     */
    public record Line(String name, boolean you, int colour, long firstTurn, long[] values, List<Event> goals) {

        /**
         * Returns whether the line has a point on a turn.
         *
         * @param turn the turn
         * @return whether it does
         */
        public boolean has(long turn) {
            return turn >= firstTurn && turn < firstTurn + values.length;
        }

        /**
         * Returns the value on a turn.
         *
         * @param turn a turn the line has
         * @return the value
         */
        public long at(long turn) {
            return values[Math.toIntExact(turn - firstTurn)];
        }

        /**
         * Returns the line's last turn.
         *
         * @return the turn
         */
        public long lastTurn() {
            return firstTurn + values.length - 1;
        }
    }

    /**
     * A budget review.
     *
     * @param turn   its turn
     * @param number which review it was, from 1
     */
    public record Review(long turn, int number) {
    }

    /**
     * A whole chart's data.
     *
     * @param from      the first turn shown
     * @param to        the last turn shown
     * @param windowed  whether older turns have dropped off the left
     * @param lines     the prestige lines, rivals first, so the player's is drawn on top
     * @param funds     the player's funds, or {@code null}
     * @param reviews   the player's reviews in the window
     * @param lostCrews the turns in the window the player lost a crew
     */
    public record Model(long from, long to, boolean windowed, List<Line> lines, Line funds, List<Review> reviews,
                        List<Long> lostCrews) {

        /**
         * Returns the points across the chart.
         *
         * @return the count
         */
        public int points() {
            return Math.toIntExact(to - from + 1);
        }

        /**
         * Returns whether there's nothing to draw yet: a line needs two points.
         *
         * @return whether it's empty
         */
        public boolean empty() {
            return points() < 2 || lines.isEmpty();
        }

        /**
         * Returns the player's line.
         *
         * @return the line, or {@code null}
         */
        public Line yours() {
            return lines.stream().filter(Line::you).findFirst().orElse(null);
        }

        /**
         * Returns the prestige lines with a point on a turn, highest first.
         *
         * @param turn the turn
         * @return the lines
         */
        public List<Line> ranked(long turn) {
            return lines.stream().filter(line -> line.has(turn))
                .sorted(Comparator.comparingLong((Line line) -> -line.at(turn))).toList();
        }

        /**
         * Returns what happened on a turn, for lines shown.
         *
         * @param turn     the turn
         * @param everyone whether the rivals' lines are on
         * @return the events
         */
        public List<Event> events(long turn, boolean everyone) {
            List<Event> events = new ArrayList<>();
            for (Line line : lines) {
                if (line.you() || everyone) {
                    line.goals().stream().filter(goal -> goal.turn() == turn).forEach(events::add);
                }
            }
            if (lostCrews.contains(turn)) {
                events.add(new Event(turn, EventKind.CREW, -1, "You lost a crew."));
            }
            reviews.stream().filter(review -> review.turn() == turn).forEach(review -> events.add(
                new Event(turn, EventKind.REVIEW, -1, "Budget review " + review.number() + ".")));
            return events;
        }
    }

    private Standing() {
    }

    /**
     * Builds a chart from the server's curves. A rival's colour goes by the agencies' order, which
     * both messages share, so the race table can match it ({@link #colours}).
     *
     * @param agencies every agency's curves
     * @param rivals   the race's summaries, for the goals, or {@code null}
     * @param whole    whether to show the whole game, never windowed
     * @return the chart
     */
    public static Model of(List<StandingSeries> agencies, List<RivalSummary> rivals, boolean whole) {
        List<RivalSummary> summaries = rivals == null ? List.of() : rivals;
        Map<String, Integer> firsts = firstTurns(summaries);
        List<Line> lines = new ArrayList<>();
        Line yours = null;
        Line funds = null;
        long[] reviewTurns = new long[0];
        long[] lostTurns = new long[0];
        int colour = 0;
        long to = 0L;
        long earliest = Long.MAX_VALUE;
        for (StandingSeries agency : agencies) {
            if (agency.prestige().length == 0) {
                if (!agency.you()) {
                    colour++;
                }
                continue;
            }
            String name = agency.agencyName() + (agency.bot() ? " (bot)" : "");
            int index = agency.you() ? -1 : colour++;
            Line line = new Line(name, agency.you(), index, agency.firstTurn(), agency.prestige(),
                goals(agency, name, index, summaries, firsts));
            to = Math.max(to, line.lastTurn());
            earliest = Math.min(earliest, line.firstTurn());
            if (agency.you()) {
                yours = line;
                funds = new Line("Funds", true, -1, agency.firstTurn(), agency.funds(), List.of());
                reviewTurns = agency.reviewTurns();
                lostTurns = agency.lostCrewTurns();
            } else {
                lines.add(line);
            }
        }
        if (yours != null) {
            lines.add(yours);
        }
        if (lines.isEmpty()) {
            return new Model(0L, 0L, false, List.of(), null, List.of(), List.of());
        }
        boolean windowed = !whole && to - earliest + 1 > WINDOW;
        long from = windowed ? to - WINDOW + 1 : earliest;
        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < reviewTurns.length; i++) {
            if (reviewTurns[i] >= from) {
                reviews.add(new Review(reviewTurns[i], i + 1));
            }
        }
        List<Long> lost = Arrays.stream(lostTurns).filter(turn -> turn >= from).boxed().toList();
        return new Model(from, to, windowed, lines, funds, reviews, lost);
    }

    private static Map<String, Integer> firstTurns(List<RivalSummary> rivals) {
        Map<String, Integer> firsts = new HashMap<>();
        for (RivalSummary rival : rivals) {
            rival.goalTurns().forEach((goal, turn) -> firsts.merge(goal, turn, Math::min));
        }
        return firsts;
    }

    private static List<Event> goals(StandingSeries agency, String name, int colour, List<RivalSummary> rivals,
                                     Map<String, Integer> firsts) {
        List<Event> goals = new ArrayList<>();
        for (RivalSummary rival : rivals) {
            if (rival.playerId().equals(agency.playerId())) {
                rival.goalTurns().forEach((goal, turn) -> {
                    boolean first = turn.equals(firsts.get(goal));
                    goals.add(new Event(turn, first ? EventKind.FIRST : EventKind.GOAL, colour,
                        name + " took " + ContentNames.of(goal) + (first ? ", a First." : ".")));
                });
            }
        }
        return goals;
    }

    /**
     * Returns the colour index of each agency in the race, in the summaries' order, as {@link #of}
     * gives them: -1 for the player's.
     *
     * @param rivals the summaries
     * @return an index per summary
     */
    public static int[] colours(List<RivalSummary> rivals) {
        int[] colours = new int[rivals.size()];
        int next = 0;
        for (int i = 0; i < colours.length; i++) {
            colours[i] = rivals.get(i).you() ? -1 : next++;
        }
        return colours;
    }

    /**
     * Returns a point's date. The first point of a game is its start, before any season.
     *
     * @param turn the turn
     * @return the date
     */
    public static String date(long turn) {
        return turn < 1 ? "Start" : GameDate.ofTurn(turn).label();
    }

    /**
     * Returns an axis's round ticks: the range fitted, always including zero.
     *
     * @param low       the lowest value
     * @param high      the highest value
     * @param intervals about how many intervals to aim for
     * @return the ticks, lowest first
     */
    public static long[] ticks(long low, long high, int intervals) {
        long min = Math.min(low, 0L);
        long max = Math.max(high, 0L);
        long step = step(Math.max(max - min, 1L) / (double) intervals);
        long first = Math.floorDiv(min, step) * step;
        long last = -Math.floorDiv(-max, step) * step;
        if (first == last) {
            last = first + step;
        }
        int count = Math.toIntExact((last - first) / step) + 1;
        long[] ticks = new long[count];
        for (int i = 0; i < count; i++) {
            ticks[i] = first + i * step;
        }
        return ticks;
    }

    private static long step(double rough) {
        double power = Math.pow(10, Math.floor(Math.log10(rough)));
        for (double nice : new double[] {1, 2, 2.5, 5, 10}) {
            if (nice * power >= rough) {
                return Math.max(1L, Math.round(nice * power));
            }
        }
        return Math.round(10 * power);
    }

    /**
     * Returns the turns that get a label on the time axis: each Winter, thinned out until they're far
     * enough apart, or, with few points, every point.
     *
     * @param from   the first turn shown
     * @param to     the last turn shown
     * @param width  the plot's width
     * @param minGap the least distance between labels
     * @return the turns
     */
    public static List<Long> labelledTurns(long from, long to, float width, float minGap) {
        List<Long> turns = new ArrayList<>();
        if (to - from + 1 <= BIG_DOTS_UP_TO) {
            for (long turn = from; turn <= to; turn++) {
                turns.add(turn);
            }
            return turns;
        }
        List<Long> winters = winters(from, to);
        float year = width * Season.values().length / (to - from);
        int every = Math.max(1, (int) Math.ceil(minGap / year));
        for (int i = 0; i < winters.size(); i += every) {
            turns.add(winters.get(i));
        }
        return turns;
    }

    /**
     * Returns every Winter shown, for the axis's year ticks.
     *
     * @param from the first turn shown
     * @param to   the last turn shown
     * @return the turns
     */
    public static List<Long> winters(long from, long to) {
        List<Long> winters = new ArrayList<>();
        for (long turn = Math.max(from, 1L); turn <= to; turn++) {
            if (GameDate.ofTurn(turn).season() == Season.WINTER) {
                winters.add(turn);
            }
        }
        return winters;
    }

    /**
     * Returns a time-axis label: the year, or the whole date with few points.
     *
     * @param turn   the turn
     * @param points the points shown
     * @return the label
     */
    public static String axisLabel(long turn, int points) {
        if (points <= BIG_DOTS_UP_TO || turn < 1) {
            return date(turn);
        }
        return String.valueOf(GameDate.ofTurn(turn).year());
    }

    /**
     * Pushes labels apart, keeping their order: no two closer than the gap, all between the bounds.
     *
     * @param wanted where each label wants to be, along one axis
     * @param gap    the least distance between two
     * @param low    the lowest place allowed
     * @param high   the highest place allowed
     * @return where each goes, in the same order as {@code wanted}
     */
    public static float[] pushApart(float[] wanted, float gap, float low, float high) {
        Integer[] order = new Integer[wanted.length];
        Arrays.setAll(order, i -> i);
        Arrays.sort(order, Comparator.comparingDouble(i -> wanted[i]));
        float[] placed = new float[wanted.length];
        float floor = low;
        for (int index : order) {
            placed[index] = Math.max(wanted[index], floor);
            floor = placed[index] + gap;
        }
        float ceiling = high;
        for (int i = order.length - 1; i >= 0; i--) {
            int index = order[i];
            placed[index] = Math.min(placed[index], ceiling);
            ceiling = placed[index] - gap;
        }
        return placed;
    }

    /**
     * Returns the turn nearest a place across the plot.
     *
     * @param from     the first turn shown
     * @param to       the last turn shown
     * @param fraction how far across, 0 to 1
     * @return the turn
     */
    public static long nearest(long from, long to, float fraction) {
        float clamped = Math.max(0f, Math.min(1f, fraction));
        return from + Math.round(clamped * (to - from));
    }
}
