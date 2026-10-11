package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.CommercialClients;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.ContractApplication;
import de.mkoehler.launchwindow.model.ContractListing;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * <p>The contract market, settled once a turn for every agency ({@code design.md} &sect;2.11), like
 * intelligence: the awards from the turn's sealed applications, then the board for the coming turn.
 * Each agency's own season pass then applies its part ({@link Contracts#advanceSeason}).</p>
 *
 * <p>It reads every agency's state with the turn's actions applied for their applications and for
 * whether a program opened or closed this turn, and scores each applicant from its state at the
 * start of the turn ({@code design.md} &sect;3.7's rule for contended resources): an R&amp;D decision
 * taken during the turn can move a reliability, and an application can't be sharpened by it. The board is
 * settled before the season passes, since each agency's copy is saved with its state: a line
 * unlocked by a flight this turn, or an agency freed by a contract ending this turn, is seated at the
 * next resolution.</p>
 *
 * <p>All draws come from the game's seed and the turn, salted, never from a season's stream.</p>
 *
 * @author Mario Koehler
 */
public final class ContractMarket {

    /** The seats a listing has when the board is sized, one per tier. */
    public static final int SEATS = Contracts.TIERS;
    /** One extra listing per this many eligible agencies, on top of one. */
    public static final int AGENCIES_PER_EXTRA = 4;

    private static final long SALT = 0xC0_7AC7_5EA7_1962L;

    private ContractMarket() {
    }

    /**
     * The turn's market for every agency.
     *
     * @param findings       each agency's findings, by key
     * @param board          the board for the coming turn
     * @param usedClientIds  the clients the board has used, so none repeats until a line's have all run
     * @param posted         how many listings were posted for the coming turn
     */
    public record Settlement(Map<String, List<ContractFinding>> findings, List<ContractListing> board,
                             List<String> usedClientIds, int posted) {

        /**
         * Copies the collections.
         */
        public Settlement {
            findings = findings == null ? Map.of() : Map.copyOf(findings);
            board = board == null ? List.of() : List.copyOf(board);
            usedClientIds = usedClientIds == null ? List.of() : List.copyOf(usedClientIds);
        }

        /**
         * Returns one agency's part, for its season pass.
         *
         * @param key the agency's key
         * @return its part
         */
        public Contracts.Turn turnFor(String key) {
            return new Contracts.Turn(findings.getOrDefault(key, List.of()), board, posted);
        }
    }

    /**
     * Settles the turn's market.
     *
     * @param agencies      every agency with the turn's actions applied, by key; iterated in key order
     * @param startOfTurn   every agency at the start of the turn, by key, for the scores; an agency
     *                      missing here is scored from {@code agencies}
     * @param board         the board this turn's applications were made against
     * @param usedClientIds the clients the board has used so far
     * @param catalog       the content pack
     * @param clients       the clients' text, for new listings
     * @param gameSeed      the game's seed
     * @param gameTurn      the turn being resolved
     * @return the settlement
     */
    public static Settlement settle(Map<String, PlayerSnapshot> agencies, Map<String, PlayerSnapshot> startOfTurn,
            List<ContractListing> board, List<String> usedClientIds, ContentCatalog catalog, CommercialClients clients, long gameSeed,
            int gameTurn) {
        Objects.requireNonNull(agencies, "agencies must not be null");
        Objects.requireNonNull(board, "board must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(clients, "clients must not be null");
        SplittableRandom random = new SplittableRandom(new SplittableRandom(gameSeed + gameTurn * 0x9E3779B97F4A7C15L)
            .nextLong() ^ SALT);
        List<String> keys = agencies.keySet().stream().sorted().toList();
        Map<String, List<ContractFinding>> findings = new LinkedHashMap<>();
        keys.forEach(key -> findings.put(key, new ArrayList<>()));

        Set<String> awardedAgencies = new LinkedHashSet<>();
        Set<String> awardedListings = new LinkedHashSet<>();
        List<ContractFinding> news = new ArrayList<>();
        for (ContractListing listing : board) {
            List<Applicant> applicants = new ArrayList<>();
            for (String key : keys) {
                PlayerSnapshot agency = agencies.get(key);
                ContractApplication application = agency.getContractApplication();
                if (application == null || !application.listingId().equals(listing.id())) {
                    continue;
                }
                boolean qualifies = agency.getActiveContract() == null
                    && Contracts.qualifyingPrograms(agency, listing.lineId(), catalog).stream()
                        .anyMatch(commercial -> commercial.programId().equals(application.programId()));
                if (!qualifies) {
                    findings.get(key).add(new ContractFinding(ContractFinding.Kind.VOIDED, listing, 0, 0,
                        application.programId(), null, List.of()));
                    continue;
                }
                applicants.add(new Applicant(key, application.programId(),
                    Contracts.score(startOfTurn.getOrDefault(key, agency), application.programId(), catalog),
                    random.nextLong()));
            }
            if (applicants.isEmpty()) {
                continue;
            }
            applicants.sort(Comparator.comparingInt(Applicant::score).reversed()
                .thenComparingLong(Applicant::draw));
            List<String> winners = new ArrayList<>();
            for (int i = 0; i < applicants.size(); i++) {
                Applicant applicant = applicants.get(i);
                PlayerSnapshot agency = agencies.get(applicant.key());
                String configuration = catalog.commercial(applicant.programId()).orElseThrow()
                    .contractConfigurationId();
                if (i < Contracts.TIERS) {
                    int tier = i + 1;
                    long fee = Contracts.fee(Contracts.launchCost(agency, configuration, catalog), tier);
                    findings.get(applicant.key()).add(new ContractFinding(ContractFinding.Kind.AWARDED, listing,
                        tier, fee, applicant.programId(), configuration, List.of()));
                    awardedAgencies.add(applicant.key());
                    winners.add(agency.getAgencyName());
                } else {
                    findings.get(applicant.key()).add(new ContractFinding(ContractFinding.Kind.MISSED, listing,
                        0, 0, applicant.programId(), configuration, List.of()));
                }
            }
            awardedListings.add(listing.id());
            winners.sort(String.CASE_INSENSITIVE_ORDER);
            news.add(new ContractFinding(ContractFinding.Kind.NEWS, listing, 0, 0, null, null, winners));
        }
        keys.forEach(key -> findings.get(key).addAll(news));

        // The board for the coming turn.
        Map<String, List<String>> lines = new LinkedHashMap<>();
        Set<String> anyQualified = new LinkedHashSet<>();
        for (String key : keys) {
            List<String> qualified = Contracts.qualifiedLines(agencies.get(key), catalog);
            lines.put(key, qualified);
            anyQualified.addAll(qualified);
        }
        List<ContractListing> next = new ArrayList<>(board.stream()
            .filter(listing -> !awardedListings.contains(listing.id()))
            .filter(listing -> listing.lastTurn() > gameTurn)
            .filter(listing -> anyQualified.contains(listing.lineId()))
            .toList());
        List<String> used = new ArrayList<>(usedClientIds == null ? List.of() : usedClientIds);
        int[] posted = {0};
        List<String> eligible = keys.stream()
            .filter(key -> agencies.get(key).getActiveContract() == null && !awardedAgencies.contains(key))
            .filter(key -> !lines.get(key).isEmpty())
            .toList();
        Map<String, Long> draws = new HashMap<>();
        eligible.forEach(key -> draws.put(key, random.nextLong()));
        List<String> order = eligible.stream()
            .sorted(Comparator.comparingInt((String key) -> lines.get(key).size()).thenComparingLong(draws::get))
            .toList();
        Map<String, Integer> seats = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            List<String> mine = lines.get(order.get(i));
            ContractListing seat = next.stream()
                .filter(listing -> mine.contains(listing.lineId()))
                .filter(listing -> seats.getOrDefault(listing.id(), 0) < SEATS)
                .max(Comparator.comparingInt(listing -> seats.getOrDefault(listing.id(), 0)))
                .orElse(null);
            if (seat == null) {
                List<String> unseated = order.subList(i, order.size());
                int most = mine.stream().mapToInt(line -> wanting(line, unseated, lines)).max().orElseThrow();
                List<String> best = mine.stream().filter(line -> wanting(line, unseated, lines) == most).toList();
                seat = post(best.get(random.nextInt(best.size())), clients, random, used, gameTurn, posted);
                next.add(seat);
            }
            seats.merge(seat.id(), 1, Integer::sum);
        }
        int extras = eligible.isEmpty() ? 0 : 1 + eligible.size() / AGENCIES_PER_EXTRA;
        List<String> extraLines = eligible.stream().flatMap(key -> lines.get(key).stream()).distinct().toList();
        while (!extraLines.isEmpty() && next.size() < seats.size() + extras) {
            next.add(post(extraLines.get(random.nextInt(extraLines.size())), clients, random, used, gameTurn,
                posted));
        }
        return new Settlement(findings, next, used, posted[0]);
    }

    private static int wanting(String line, List<String> agencies, Map<String, List<String>> lines) {
        return (int) agencies.stream().filter(key -> lines.get(key).contains(line)).count();
    }

    private static ContractListing post(String line, CommercialClients clients, SplittableRandom random,
            List<String> used, int gameTurn, int[] posted) {
        posted[0]++;
        return new ContractListing("c" + gameTurn + "-" + posted[0], line,
            clients.pick(line, Archetype.NASA, random, used), clients.pick(line, Archetype.SOVIET, random, used),
            gameTurn + 1, gameTurn + Contracts.LISTING_TURNS);
    }

    private record Applicant(String key, String programId, int score, long draw) {
    }
}
