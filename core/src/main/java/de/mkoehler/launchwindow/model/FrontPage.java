package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A printed front page for one of the great Firsts ({@code ui-spec.md} &sect;4.18, G14): which First,
 * the turn it was reached in, the story drawn for it, and who made it. Printed once by the server in
 * the resolution where the First was reached, kept in the game's {@code frontpages.json}, and sent to
 * every player. Agency names only, never account ids.
 *
 * @param id             the page's id, {@code <turn>-<goal-id>}
 * @param goalId         the First's goal
 * @param gameTurn       the turn it was reached in
 * @param storyId        the story's id in {@code front-pages.json}
 * @param weather        the ear's weather line, drawn with the story
 * @param foldedGoalIds  other Firsts reached by the same flights, told on this page (race order)
 * @param achievers      the agencies that made it, more than one for a tie
 * @param rivals         up to two other agencies the article names
 * @author Mario Koehler
 */
public record FrontPage(String id, String goalId, int gameTurn, String storyId, String weather,
                        List<String> foldedGoalIds, List<Achiever> achievers, List<String> rivals) {

    /**
     * Copies the lists into {@link ArrayList}s, which Kryo can rebuild.
     */
    public FrontPage {
        foldedGoalIds = foldedGoalIds == null ? new ArrayList<>() : new ArrayList<>(foldedGoalIds);
        achievers = achievers == null ? new ArrayList<>() : new ArrayList<>(achievers);
        rivals = rivals == null ? new ArrayList<>() : new ArrayList<>(rivals);
    }

    /**
     * Returns a page's id.
     *
     * @param gameTurn the turn
     * @param goalId   the First's goal
     * @return the id
     */
    public static String idOf(int gameTurn, String goalId) {
        return gameTurn + "-" + goalId;
    }

    /**
     * Returns whether the page tells a First, its own or folded in.
     *
     * @param goal the goal
     * @return whether it does
     */
    public boolean tells(String goal) {
        return goalId.equals(goal) || foldedGoalIds.contains(goal);
    }

    /**
     * Returns whether an agency is among the page's achievers.
     *
     * @param agencyName the agency
     * @return whether it made the First
     */
    public boolean madeBy(String agencyName) {
        return achievers.stream().anyMatch(a -> a.agencyName().equals(agencyName));
    }

    /**
     * One agency that made the First.
     *
     * @param agencyName   the agency's name
     * @param directorName its director's name
     * @param crewNames    the crew of the flight that made it, in seat order; empty for an uncrewed First
     */
    public record Achiever(String agencyName, String directorName, List<String> crewNames) {

        /**
         * Copies the list into an {@link ArrayList}, which Kryo can rebuild.
         */
        public Achiever {
            crewNames = crewNames == null ? new ArrayList<>() : new ArrayList<>(crewNames);
        }
    }
}
