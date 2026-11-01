package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.Commercial;
import de.mkoehler.launchwindow.content.CommercialClients;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.ActiveContract;
import de.mkoehler.launchwindow.model.ContractListing;
import de.mkoehler.launchwindow.model.ContractRecord;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.ContractLineUnlocked;
import de.mkoehler.launchwindow.rules.ContractNews;
import de.mkoehler.launchwindow.rules.ContractOutcome;
import de.mkoehler.launchwindow.rules.ContractSettled;
import de.mkoehler.launchwindow.rules.Contracts;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * What the Contracts screen, Headquarters' card, the hub and the turn report say about commercial
 * contracts ({@code ui-spec.md} &sect;4.17 and component 27, {@code design.md} &sect;2.11). Reads the
 * rules ({@link Contracts}) against the agency's state; never ports them.
 *
 * @author Mario Koehler
 */
public final class ContractsView {

    private ContractsView() {
    }

    /**
     * A listing as the agency sees it.
     *
     * @param listing    the listing
     * @param client     the client's name
     * @param title      the order's title
     * @param story      the order's story
     * @param commercial the program the agency would fly it with, or the line's first of its side
     * @param launchCost the launch cost now, with the cheapest rocket the agency could use
     * @param outcome    whether the agency could apply, or why not
     * @param applied    whether the agency's application this turn is for it
     */
    public record Offer(ContractListing listing, String client, String title, String story, Commercial commercial,
                        long launchCost, ContractOutcome outcome, boolean applied) {

        /**
         * Returns whether the agency can fly it at all: the line is unlocked and a program open. A
         * listing it can fly but not apply for now (an active contract) keeps its look.
         *
         * @return whether it can
         */
        public boolean flyable() {
            return outcome == ContractOutcome.APPLIED || outcome == ContractOutcome.CONTRACT_ACTIVE;
        }

        /**
         * Returns the fee at a tier.
         *
         * @param tier the tier, 1 to 3
         * @return the fee
         */
        public long fee(int tier) {
            return Contracts.fee(launchCost, tier);
        }
    }

    /**
     * Returns the board as the agency sees it: the listings it can fly first, in board order.
     *
     * @param state   the agency
     * @param catalog the content pack
     * @return the offers
     */
    public static List<Offer> board(PlayerSnapshot state, ContentCatalog catalog) {
        List<Offer> offers = new ArrayList<>();
        for (ContractListing listing : state.getContractBoard()) {
            offers.add(offer(state, listing, catalog));
        }
        offers.sort(Comparator.comparing((Offer offer) -> !offer.flyable()));
        return offers;
    }

    /**
     * Returns one listing as the agency sees it.
     *
     * @param state   the agency
     * @param listing the listing
     * @param catalog the content pack
     * @return the offer
     */
    public static Offer offer(PlayerSnapshot state, ContractListing listing, ContentCatalog catalog) {
        Optional<CommercialClients.Client> client = CommercialClients.standard()
            .byId(listing.clientFor(state.getArchetype()));
        Commercial commercial = programFor(state, listing.lineId(), catalog);
        long cost = commercial == null ? 0
            : Contracts.launchCost(state, commercial.contractConfigurationId(), catalog);
        boolean applied = state.getContractApplication() != null
            && state.getContractApplication().listingId().equals(listing.id());
        return new Offer(listing, client.map(CommercialClients.Client::name).orElse("A client"),
            client.map(CommercialClients.Client::title).orElse(ContentNames.of(listing.lineId())),
            client.map(CommercialClients.Client::story).orElse(""), commercial, cost,
            Contracts.check(state, listing.id(), null, catalog), applied);
    }

    /**
     * Returns the program the agency would fly a line's contracts with: its first qualifying one,
     * else the one whose unlock set it has flown most of, else the line's first of its side.
     *
     * @param state   the agency
     * @param lineId  the line
     * @param catalog the content pack
     * @return the program's block, or {@code null} if its side has none in the line
     */
    public static Commercial programFor(PlayerSnapshot state, String lineId, ContentCatalog catalog) {
        List<Commercial> qualifying = Contracts.qualifyingPrograms(state, lineId, catalog);
        if (!qualifying.isEmpty()) {
            return qualifying.getFirst();
        }
        return Contracts.programsOfLine(state, lineId, catalog).stream()
            .max(Comparator.comparingInt(commercial -> Contracts.flownOfSet(state, commercial)))
            .orElse(null);
    }

    /**
     * Returns the reason an agency can't apply, in the card's foot, with its icon.
     *
     * @param offer   the offer
     * @param state   the agency
     * @param catalog the content pack
     * @return icon and text, or {@code null} when it can apply
     */
    public static String[] reason(Offer offer, PlayerSnapshot state, ContentCatalog catalog) {
        String line = ContentNames.of(offer.listing().lineId());
        String program = offer.commercial() == null ? "a program" : ContentNames.of(offer.commercial().programId());
        return switch (offer.outcome()) {
            case APPLIED, WITHDRAWN -> null;
            case CONTRACT_ACTIVE -> new String[] {"document", "You have an active contract: one at a time."};
            case NOT_QUALIFIED -> new String[] {"lock", "Needs the " + line + " line: fly " + program
                + "'s marked flights"};
            case PROGRAM_NOT_OPEN -> new String[] {"open_programs", "Open " + program + " to fly it"};
            case UNKNOWN_LISTING -> new String[] {"clock", "No longer on the board."};
        };
    }

    /**
     * Words the flight a listing flies: "Flies: Explorer 1 · Orbital flight".
     *
     * @param offer the offer
     * @return the words
     */
    public static String flies(Offer offer) {
        return offer.commercial() == null ? "" : "Flies: " + ContentNames.of(offer.commercial().contractConfigurationId());
    }

    /**
     * Words when a listing was posted and how long it stays: "Posted Spring 1963 · expires in 2 turns".
     *
     * @param listing  the listing
     * @param openTurn the turn open now
     * @return the words
     */
    public static String posted(ContractListing listing, long openTurn) {
        long left = listing.lastTurn() - openTurn + 1;
        return "Posted " + GameDate.ofTurn(listing.postedTurn()).label() + " · "
            + (left <= 1 ? "last turn to apply" : "expires in " + left + " turns");
    }

    /**
     * Returns whether the hub's Headquarters marker shows the gold dot: a listing the agency can apply
     * for is new this turn, and it hasn't applied anywhere.
     *
     * @param state    the agency
     * @param catalog  the content pack
     * @param openTurn the turn open now
     * @return whether
     */
    public static boolean newOffers(PlayerSnapshot state, ContentCatalog catalog, long openTurn) {
        return state.getContractApplication() == null && state.getActiveContract() == null
            && state.getContractBoard().stream().anyMatch(listing -> listing.postedTurn() == openTurn
                && Contracts.check(state, listing.id(), null, catalog) == ContractOutcome.APPLIED);
    }

    /**
     * Words the reminder on the hub's Headquarters marker (the user's idea, 2026-10-31): an awarded contract
     * with no mission launched for it, which also shows the gold dot until one is.
     *
     * @param state    the agency
     * @param openTurn the turn open now
     * @return the hover line, or {@code null} when no contract waits
     */
    public static String launchReminder(PlayerSnapshot state, long openTurn) {
        ActiveContract contract = state.getActiveContract();
        if (contract == null || contract.missionId() != null) {
            return null;
        }
        return "Contract to launch: " + title(contract, state) + " · " + seasonsLeft(contract, openTurn);
    }

    /**
     * Returns Headquarters' Contracts card: the eyebrow and the state row.
     *
     * @param state    the agency
     * @param catalog  the content pack
     * @param openTurn the turn open now
     * @return eyebrow, state line, and "gold" when the state row is gold (new offers), else ""
     */
    public static String[] card(PlayerSnapshot state, ContentCatalog catalog, long openTurn) {
        List<Offer> offers = board(state, catalog);
        long flyable = offers.stream().filter(offer -> offer.outcome() == ContractOutcome.APPLIED).count();
        String eyebrow = offers.size() + " on offer · " + flyable + " you can fly";
        ActiveContract contract = state.getActiveContract();
        if (contract != null) {
            return new String[] {eyebrow, "Active: " + title(contract, state) + " · "
                + (contract.missionId() == null ? "launch by " + GameDate.ofTurn(contract.launchByTurn()).label()
                : "flying"), ""};
        }
        if (state.getContractApplication() != null) {
            String title = Contracts.listing(state, state.getContractApplication().listingId())
                .map(listing -> offer(state, listing, catalog).title()).orElse("a listing");
            return new String[] {eyebrow, "Applied: " + title + " · decided when the turn resolves", ""};
        }
        boolean fresh = newOffers(state, catalog, openTurn);
        return new String[] {eyebrow, fresh ? "New contracts on offer" : flyable > 0
            ? "Apply for one a turn; it's awarded when the turn resolves" : "Nothing you can fly on offer",
            fresh ? "gold" : ""};
    }

    /**
     * Returns an active contract's order title, from its client.
     *
     * @param contract the contract
     * @param state    the agency
     * @return the title
     */
    public static String title(ActiveContract contract, PlayerSnapshot state) {
        return CommercialClients.standard().byId(contract.clientId()).map(CommercialClients.Client::title)
            .orElse(ContentNames.of(contract.lineId()));
    }

    /**
     * Returns an active contract's client's name.
     *
     * @param clientId the client
     * @return the name
     */
    public static String client(String clientId) {
        return CommercialClients.standard().byId(clientId).map(CommercialClients.Client::name).orElse("A client");
    }

    /**
     * The steps of an active contract, for the stepper: awarded, scheduled (on the launch pad this
     * turn), flying, paid.
     *
     * @param contract the contract
     * @param state    the agency
     * @return the index of the current step, 1 to 3 (paid is the report's)
     */
    public static int step(ActiveContract contract, PlayerSnapshot state) {
        if (contract.missionId() == null) {
            return 1;
        }
        var mission = state.getInProgressMissions().get(contract.missionId());
        return mission == null || mission.getCurrentSeasonIndex() == 0 ? 2 : 3;
    }

    /**
     * Words the seasons left to launch a contract: "2 seasons left", "last season".
     *
     * @param contract the contract
     * @param openTurn the turn open now
     * @return the words
     */
    public static String seasonsLeft(ActiveContract contract, long openTurn) {
        long left = contract.launchByTurn() - openTurn + 1;
        return left <= 1 ? "last season" : left + " seasons left";
    }

    /**
     * A line of the agency's record.
     *
     * @param lineId    the line
     * @param record    the record
     * @param points    the record's points
     * @param score     the score now, or {@code null} when no program of the line is open
     * @param weakest   the weakest component's name, for the tooltip, or {@code null}
     * @param weakestReliability its reliability, rounded
     * @param openHint  "Open Mercury" when no program of the line is open, else {@code null}
     */
    public record RecordLine(String lineId, ContractRecord record, int points, Integer score, String weakest,
                             int weakestReliability, String openHint) {
    }

    /**
     * Returns the agency's record, one line per unlocked line.
     *
     * @param state   the agency
     * @param catalog the content pack
     * @return the lines
     */
    public static List<RecordLine> record(PlayerSnapshot state, ContentCatalog catalog) {
        List<RecordLine> lines = new ArrayList<>();
        for (String line : Contracts.unlockedLines(state, catalog)) {
            ContractRecord record = Contracts.record(state, line);
            int points = Contracts.recordPoints(record);
            List<Commercial> qualifying = Contracts.qualifyingPrograms(state, line, catalog);
            if (qualifying.isEmpty()) {
                String program = Contracts.programsOfLine(state, line, catalog).stream()
                    .filter(commercial -> Contracts.unlocked(state, commercial)).findFirst()
                    .map(commercial -> ContentNames.of(commercial.programId())).orElse("a program");
                lines.add(new RecordLine(line, record, points, null, null, 0, "Open " + program));
                continue;
            }
            Commercial commercial = qualifying.getFirst();
            Contracts.Weakest weakest = Contracts.weakest(state, commercial.contractConfigurationId(), catalog);
            lines.add(new RecordLine(line, record, points, Contracts.score(state, commercial.programId(), catalog),
                weakest.componentId() == null ? "no rocket" : ContentNames.of(weakest.componentId()),
                (int) Math.round(weakest.reliability()), null));
        }
        return lines;
    }

    /**
     * Words a record for the score's tooltip: "Record (2 flown)", "Record (1 flown, 1 lapsed)".
     *
     * @param record the record
     * @return the words
     */
    public static String recordWords(ContractRecord record) {
        List<String> parts = new ArrayList<>();
        if (record.flown() > 0) {
            parts.add(record.flown() + " flown");
        }
        if (record.failed() > 0) {
            parts.add(record.failed() + " failed");
        }
        if (record.lapsed() > 0) {
            parts.add(record.lapsed() + " lapsed");
        }
        return parts.isEmpty() ? "Record" : "Record (" + String.join(", ", parts) + ")";
    }

    /**
     * Words a signed number of points: "+6", "−3", "0".
     *
     * @param points the points
     * @return the words
     */
    public static String signed(int points) {
        return points > 0 ? "+" + points : points < 0 ? "−" + -points : "0";
    }

    /**
     * Words a tier's chip: "Tier 1 · +60 %".
     *
     * @param tier the tier
     * @return the words
     */
    public static String tierLabel(int tier) {
        return "Tier " + tier + " · +" + Contracts.MARGIN_PERCENTS.get(tier - 1) + " %";
    }

    /**
     * Words money as the contract screens do: "$ 2,340".
     *
     * @param amount the amount
     * @return the words
     */
    public static String money(long amount) {
        return String.format(Locale.ROOT, "$ %,d", amount);
    }

    /**
     * The agency's own contract news in a turn report, for its Contracts card.
     *
     * @param kind  what happened
     * @param title the order's title
     * @param lines what the card says, line by line
     * @param value the card's figure ("+ $ 2,340", "Tier 1"), or empty
     * @param tone  1 good, 0 neutral, -1 bad
     */
    public record ReportCard(ContractSettled.Kind kind, String title, List<String> lines, String value, int tone) {
    }

    /**
     * Returns the agency's own contract events in a report as cards.
     *
     * @param report the report
     * @return the cards
     */
    public static List<ReportCard> reportCards(TurnReport report) {
        List<ReportCard> cards = new ArrayList<>();
        for (ResolutionEvent event : report.events()) {
            if (!(event instanceof ContractSettled settled)) {
                continue;
            }
            String title = CommercialClients.standard().byId(settled.clientId()).map(CommercialClients.Client::title)
                .orElse(ContentNames.of(settled.lineId()));
            String client = client(settled.clientId());
            String flight = settled.configurationId() == null ? "" : ContentNames.of(settled.configurationId());
            cards.add(switch (settled.kind()) {
                case AWARDED -> new ReportCard(settled.kind(), title, List.of(client + " · "
                    + ContentNames.of(settled.lineId()), "Fee " + money(settled.fee()) + " on success. Flies: " + flight
                    + ". Launch within " + Contracts.LAUNCH_WINDOW_TURNS + " seasons."),
                    "Tier " + settled.tier() + " of " + Contracts.TIERS, 1);
                case MISSED -> new ReportCard(settled.kind(), title, List.of(client + " · "
                    + ContentNames.of(settled.lineId()), "Agencies with a better record won it."), "", 0);
                case VOIDED -> new ReportCard(settled.kind(), title, List.of(client + " · "
                    + ContentNames.of(settled.lineId()), "Your application came to nothing: "
                    + ContentNames.of(settled.programId()) + " was closed before the turn resolved."), "", 0);
                case PAID -> new ReportCard(settled.kind(), title, List.of("The client paid the fee: "
                    + money(settled.fee()) + ", a profit of " + money(settled.fee() - settled.launchCost())
                    + " on the launch cost.", "Your record in " + ContentNames.of(settled.lineId()) + ": "
                    + signed(settled.recordChange()) + "."), "+ " + money(settled.funds()), 1);
                case REFUNDED -> new ReportCard(settled.kind(), title, List.of("The flight failed. The insurer "
                    + "refunded the launch cost, " + money(settled.funds()) + "."
                    + (settled.prestige() > 0 ? " The lost crew cost another " + settled.prestige() + " prestige."
                    : ""), "Your record in " + ContentNames.of(settled.lineId()) + ": "
                    + signed(settled.recordChange()) + "."), "+ " + money(settled.funds()), -1);
                case LAPSED -> new ReportCard(settled.kind(), title, List.of("It wasn't launched in its window, so "
                    + "the client went elsewhere: " + settled.prestige() + " prestige, and it counts as a failure.",
                    "Your record in " + ContentNames.of(settled.lineId()) + ": " + signed(settled.recordChange())
                    + "."), "−" + settled.prestige() + " prestige", -1);
            });
        }
        return cards;
    }

    /**
     * Returns the report's public contract news, for its news card: awards, listings posted, and
     * the lines the agency unlocked.
     *
     * @param report the report
     * @return the lines
     */
    public static List<TurnReportView.Line> news(TurnReport report) {
        List<TurnReportView.Line> lines = new ArrayList<>();
        for (ResolutionEvent event : report.events()) {
            if (event instanceof ContractNews news && news.kind() == ContractNews.Kind.AWARDED) {
                String title = CommercialClients.standard().byId(news.clientId()).map(CommercialClients.Client::title)
                    .orElse("A contract in " + ContentNames.of(news.lineId()));
                lines.add(new TurnReportView.Line("Contract awarded", title + " goes to " + and(news.winners()) + ".",
                    "", 0));
            } else if (event instanceof ContractNews news) {
                lines.add(new TurnReportView.Line("New contracts", news.count() + (news.count() == 1
                    ? " new contract is" : " new contracts are") + " on the board in Headquarters.", "", 0));
            } else if (event instanceof ContractLineUnlocked unlocked && unlocked.newLine()) {
                lines.add(new TurnReportView.Line("A contract line opens", "Clients now pay you to fly "
                    + ContentNames.of(unlocked.lineId()).toLowerCase(Locale.ROOT) + " flights while "
                    + ContentNames.of(unlocked.programId()) + " is open. See the board in Headquarters.", "", 1));
            }
        }
        return lines;
    }

    private static String and(List<String> names) {
        if (names.size() < 2) {
            return String.join("", names);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.getLast();
    }

    /**
     * Words component 27's unlock tooltip: what the marked flights open, and how many are flown.
     *
     * @param commercial the program's block
     * @param state      the agency
     * @param catalog    the content pack
     * @return the text
     */
    public static String unlockTip(Commercial commercial, PlayerSnapshot state, ContentCatalog catalog) {
        String line = ContentNames.of(Contracts.lineOf(commercial.programId(), catalog));
        if (Contracts.unlocked(state, commercial)) {
            return "Every marked flight has succeeded: " + line + " is a contract line for you.";
        }
        return "Opens commercial contracts for " + line + " once every marked flight has succeeded: "
            + Contracts.flownOfSet(state, commercial) + " of " + commercial.unlockConfigurationIds().size() + " flown.";
    }

    /**
     * Words a program card's contract line: "Commercial line: Earth-orbit satellites · 1 of 2 marked
     * flights flown", or "· unlocked".
     *
     * @param commercial the program's block
     * @param state      the agency
     * @param catalog    the content pack
     * @return the words
     */
    public static String programLine(Commercial commercial, PlayerSnapshot state, ContentCatalog catalog) {
        String line = "Commercial line: " + ContentNames.of(Contracts.lineOf(commercial.programId(), catalog));
        return Contracts.unlocked(state, commercial) ? line + " · unlocked"
            : line + " · " + Contracts.flownOfSet(state, commercial) + " of "
                + commercial.unlockConfigurationIds().size() + " marked flights flown";
    }
}
