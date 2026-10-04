package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * A date on the game calendar shared by every player: a year and a season. Each game turn is one
 * season, and turn 1 is {@link #START_SEASON} {@value #START_YEAR}, so the date follows from the
 * game turn alone ({@code design.md} &sect;5 M5, "Game info").
 *
 * <p><b>The start is an unconfirmed default</b> (2026-10-04): BASPM's own campaign starts in
 * winter 1955 with four periods a year ({@code baspm.md} &sect;2.5), and Launch Window's numbers
 * are BASPM-close by the user's direction.</p>
 *
 * @param year   the calendar year
 * @param season the season within the year
 * @author Mario Koehler
 */
public record GameDate(int year, Season season) {

    /** The year of game turn 1. */
    public static final int START_YEAR = 1955;

    /** The season of game turn 1. */
    public static final Season START_SEASON = Season.WINTER;

    private static final int SEASONS_PER_YEAR = Season.values().length;

    /**
     * Checks the parts.
     *
     * @throws NullPointerException if the season is {@code null}
     */
    public GameDate {
        Objects.requireNonNull(season, "season must not be null");
    }

    /**
     * Returns the date of a game turn.
     *
     * @param gameTurn the game turn, from 1
     * @return its date
     * @throws IllegalArgumentException if the turn is below 1
     */
    public static GameDate ofTurn(long gameTurn) {
        if (gameTurn < 1) {
            throw new IllegalArgumentException("Game turns start at 1: " + gameTurn);
        }
        long seasonsSinceStart = START_SEASON.ordinal() + (gameTurn - 1);
        int year = Math.toIntExact(START_YEAR + seasonsSinceStart / SEASONS_PER_YEAR);
        return new GameDate(year, Season.values()[(int) (seasonsSinceStart % SEASONS_PER_YEAR)]);
    }

    /**
     * Returns the date as shown to the player.
     *
     * @return for example "Winter 1955"
     */
    public String label() {
        return season.label() + " " + year;
    }
}
