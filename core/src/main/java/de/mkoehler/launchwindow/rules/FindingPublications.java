package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Finding;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>The turn's publications of findings, settled once a turn for every agency ({@code design.md}
 * &sect;2.13), like the intelligence board and the contract market: each agency's own season pass then
 * applies its part ({@link Findings#advanceSeason}).</p>
 *
 * <p>It reads every agency's state with the turn's actions applied, before any flight of the turn: a
 * rival counts as helped when it doesn't know the finding then, so a rival that earns the goal itself at
 * the same resolution still counts (unconfirmed: the publisher couldn't have known, and the preview the
 * player saw stays the payment, unless another agency published the same finding at the same resolution,
 * when they split it). An ask for a finding someone published already comes to nothing.</p>
 *
 * @author Mario Koehler
 */
public final class FindingPublications {

    private FindingPublications() {
    }

    /**
     * One publication, as one agency hears of it.
     *
     * @param goalId     the finding
     * @param byYou      whether this agency is one of its publishers
     * @param publishers the publishers' agency names: the others when {@code byYou}, all of them otherwise
     * @param helped     the rivals of a publisher that didn't know it
     * @param rivals     every rival of a publisher
     * @param prestige   what it pays this agency; 0 unless {@code byYou}
     */
    public record News(String goalId, boolean byYou, List<String> publishers, int helped, int rivals,
                       long prestige) {

        /**
         * Copies the publishers.
         */
        public News {
            Objects.requireNonNull(goalId, "goalId must not be null");
            publishers = publishers == null ? List.of() : List.copyOf(publishers);
        }
    }

    /**
     * The turn's publications for every agency.
     *
     * @param news each agency's part, by key
     */
    public record Settlement(Map<String, List<News>> news) {

        /** A turn with no publications. */
        public static final Settlement NONE = new Settlement(Map.of());

        /**
         * Copies the map.
         */
        public Settlement {
            news = news == null ? Map.of() : Map.copyOf(news);
        }

        /**
         * Returns one agency's part, for its season pass.
         *
         * @param key the agency's key
         * @return its part
         */
        public List<News> turnFor(String key) {
            return news.getOrDefault(key, List.of());
        }
    }

    /**
     * What publishing a finding now would pay an agency, if no other agency publishes it at the same
     * resolution.
     *
     * @param prestige the prestige
     * @param helped   the rivals that don't know it
     * @param rivals   every rival
     */
    public record Preview(long prestige, int helped, int rivals) {
    }

    /**
     * Settles the turn's publications.
     *
     * @param agencies every agency with the turn's actions applied, by key; iterated in key order
     * @param catalog  the content pack
     * @return the settlement
     */
    public static Settlement settle(Map<String, PlayerSnapshot> agencies, ContentCatalog catalog) {
        Objects.requireNonNull(agencies, "agencies must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        List<String> keys = agencies.keySet().stream().sorted().toList();
        Map<String, List<News>> news = new LinkedHashMap<>();
        keys.forEach(key -> news.put(key, new ArrayList<>()));
        int rivals = keys.size() - 1;
        for (Finding finding : catalog.findings()) {
            String goalId = finding.goalId();
            if (keys.stream().anyMatch(key -> agencies.get(key).getPublicFindings().containsKey(goalId))) {
                continue;
            }
            List<String> publishers = keys.stream()
                .filter(key -> agencies.get(key).getPendingPublications().contains(goalId))
                .filter(key -> agencies.get(key).hasAchievedOrCancelledGoal(goalId))
                .toList();
            if (publishers.isEmpty()) {
                continue;
            }
            int helped = (int) keys.stream().filter(key -> !Findings.knows(agencies.get(key), goalId)).count();
            Goal goal = catalog.goal(goalId).orElseThrow();
            long each = Math.round(Findings.payment(goal, helped, rivals) / (double) publishers.size());
            List<String> names = publishers.stream().map(key -> agencies.get(key).getAgencyName()).toList();
            for (String key : keys) {
                boolean byYou = publishers.contains(key);
                List<String> others = byYou
                    ? names.stream().filter(name -> !name.equals(agencies.get(key).getAgencyName())).toList() : names;
                news.get(key).add(new News(goalId, byYou, others, helped, rivals, byYou ? each : 0));
            }
        }
        return new Settlement(news);
    }

    /**
     * Previews what publishing a finding now would pay, for the confirmation dialog: as of now, and as
     * if no other agency publishes it at the same resolution.
     *
     * @param key      the asking agency's key
     * @param goalId   the finding
     * @param agencies every agency with today's actions applied, by key, the asking one included
     * @param catalog  the content pack
     * @return the preview; nothing when the goal isn't a finding
     */
    public static Preview preview(String key, String goalId, Map<String, PlayerSnapshot> agencies,
            ContentCatalog catalog) {
        Objects.requireNonNull(agencies, "agencies must not be null");
        Goal goal = catalog.finding(goalId).flatMap(finding -> catalog.goal(finding.goalId())).orElse(null);
        if (goal == null) {
            return new Preview(0, 0, Math.max(0, agencies.size() - 1));
        }
        int rivals = agencies.size() - 1;
        int helped = (int) agencies.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(key))
            .filter(entry -> !Findings.knows(entry.getValue(), goalId))
            .count();
        return new Preview(Findings.payment(goal, helped, rivals), helped, rivals);
    }
}
