package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.Commercial;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.ActiveContract;
import de.mkoehler.launchwindow.model.ContractApplication;
import de.mkoehler.launchwindow.model.ContractListing;
import de.mkoehler.launchwindow.model.ContractRecord;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>Commercial contracts ({@code design.md} &sect;2.11, the user's idea, 2026-10-11): once an agency
 * has flown every configuration of a program's unlock set, the program's category is a contract line
 * for it, and clients pay it to fly the program's contract configuration again. Contracts pay money,
 * never prestige: on success the client pays the fee, the launch cost plus the tier's margin; on
 * failure the insurer refunds the launch cost and the failure costs its prestige as usual.</p>
 *
 * <p>The board and the awards are settled once a turn for every agency ({@link ContractMarket}); this
 * class holds one agency's side: what it qualifies for, its score, its application, and its contract
 * from the award to the flight's end.</p>
 *
 * <p>Every number here is our own first-pass default, unconfirmed (2026-10-11).</p>
 *
 * @author Mario Koehler
 */
public final class Contracts {

    /** Each tier's margin over the launch cost, in percent, tier 1 first. */
    public static final List<Integer> MARGIN_PERCENTS = List.of(60, 40, 25);
    /** The tiers a listing awards; a fourth applicant and later get nothing. */
    public static final int TIERS = 3;
    /** What a contract flown successfully adds to the line's record. */
    public static final int RECORD_FLOWN_POINTS = 3;
    /** What a failed or lapsed contract adds to the line's record (it takes points away). */
    public static final int RECORD_FAILED_POINTS = -6;
    /** The record's points stay within this, either way. */
    public static final int RECORD_CAP = 15;
    /** The turns an awarded contract has to launch in, the award's own not counted. */
    public static final int LAUNCH_WINDOW_TURNS = 3;
    /** What a lapsed contract costs in prestige. */
    public static final int LAPSE_PRESTIGE = 10;
    /** What a crew lost on a contract flight costs in prestige, on top of the normal rules. */
    public static final int CREW_LOSS_PRESTIGE = 50;
    /** The turns a listing stays on the board unless awarded. */
    public static final int LISTING_TURNS = 3;

    private Contracts() {
    }

    /**
     * Returns whether an agency has flown every configuration of a program's unlock set.
     *
     * @param snapshot   the agency
     * @param commercial the program's commercial block
     * @return whether the set is complete
     */
    public static boolean unlocked(PlayerSnapshot snapshot, Commercial commercial) {
        return flownOfSet(snapshot, commercial) == commercial.unlockConfigurationIds().size();
    }

    /**
     * Returns how many of a program's unlock set the agency has flown successfully.
     *
     * @param snapshot   the agency
     * @param commercial the program's commercial block
     * @return the count
     */
    public static int flownOfSet(PlayerSnapshot snapshot, Commercial commercial) {
        return (int) commercial.unlockConfigurationIds().stream()
            .filter(id -> snapshot.getSuccessfulFlightCounts().getOrDefault(id, 0) > 0).count();
    }

    /**
     * Returns the commercial block of a configuration's program when the configuration is in its
     * unlock set: the flights that open a contract line (component 27's outline marker).
     *
     * @param configurationId the configuration
     * @param catalog         the content pack
     * @return the block, or empty
     */
    public static Optional<Commercial> unlockSetOf(String configurationId, ContentCatalog catalog) {
        return catalog.missionConfiguration(configurationId)
            .flatMap(configuration -> catalog.commercial(configuration.programId()))
            .filter(commercial -> commercial.unlockConfigurationIds().contains(configurationId));
    }

    /**
     * Returns a program's contract line, its category.
     *
     * @param programId the program
     * @param catalog   the content pack
     * @return the line
     */
    public static String lineOf(String programId, ContentCatalog catalog) {
        return catalog.program(programId).orElseThrow().categoryId();
    }

    /**
     * Returns the programs of the agency's side with contracts in a line, in the pack's order.
     *
     * @param snapshot the agency
     * @param lineId   the line
     * @param catalog  the content pack
     * @return the programs' blocks
     */
    public static List<Commercial> programsOfLine(PlayerSnapshot snapshot, String lineId, ContentCatalog catalog) {
        return catalog.commercials().stream()
            .filter(commercial -> {
                Program program = catalog.program(commercial.programId()).orElseThrow();
                return program.categoryId().equals(lineId) && program.availableTo(snapshot.getArchetype());
            })
            .toList();
    }

    /**
     * Returns the lines the agency has unlocked, for good, in the pack's order.
     *
     * @param snapshot the agency
     * @param catalog  the content pack
     * @return the lines
     */
    public static List<String> unlockedLines(PlayerSnapshot snapshot, ContentCatalog catalog) {
        Set<String> lines = new LinkedHashSet<>();
        for (Commercial commercial : catalog.commercials()) {
            Program program = catalog.program(commercial.programId()).orElseThrow();
            if (program.availableTo(snapshot.getArchetype()) && unlocked(snapshot, commercial)) {
                lines.add(program.categoryId());
            }
        }
        return List.copyOf(lines);
    }

    /**
     * Returns the programs the agency can fly a line's contracts with now: unlocked and open.
     *
     * @param snapshot the agency
     * @param lineId   the line
     * @param catalog  the content pack
     * @return the programs' blocks, in the pack's order
     */
    public static List<Commercial> qualifyingPrograms(PlayerSnapshot snapshot, String lineId,
            ContentCatalog catalog) {
        return programsOfLine(snapshot, lineId, catalog).stream()
            .filter(commercial -> unlocked(snapshot, commercial))
            .filter(commercial -> snapshot.getOpenedProgramIds().contains(commercial.programId()))
            .toList();
    }

    /**
     * Returns the lines the agency qualifies for now: unlocked, with a program in them open.
     *
     * @param snapshot the agency
     * @param catalog  the content pack
     * @return the lines, in the pack's order
     */
    public static List<String> qualifiedLines(PlayerSnapshot snapshot, ContentCatalog catalog) {
        return unlockedLines(snapshot, catalog).stream()
            .filter(line -> !qualifyingPrograms(snapshot, line, catalog).isEmpty())
            .toList();
    }

    /**
     * Returns the programs whose unlock set the agency has completed.
     *
     * @param snapshot the agency
     * @param catalog  the content pack
     * @return the programs, in the pack's order
     */
    public static Set<String> unlockedPrograms(PlayerSnapshot snapshot, ContentCatalog catalog) {
        Set<String> programs = new LinkedHashSet<>();
        for (Commercial commercial : catalog.commercials()) {
            if (catalog.program(commercial.programId()).orElseThrow().availableTo(snapshot.getArchetype())
                    && unlocked(snapshot, commercial)) {
                programs.add(commercial.programId());
            }
        }
        return programs;
    }

    /**
     * Reports the unlock sets a flight completed, for {@link ContractLineUnlocked}.
     *
     * @param before   the programs unlocked before the flight counted ({@link #unlockedPrograms})
     * @param snapshot the agency, the flight counted
     * @param catalog  the content pack
     * @param events   the list to append the events to
     */
    static void reportUnlocks(Set<String> before, PlayerSnapshot snapshot, ContentCatalog catalog,
            List<ResolutionEvent> events) {
        Set<String> lines = new LinkedHashSet<>();
        before.forEach(program -> lines.add(lineOf(program, catalog)));
        for (String program : unlockedPrograms(snapshot, catalog)) {
            if (!before.contains(program)) {
                String line = lineOf(program, catalog);
                events.add(new ContractLineUnlocked(line, program, lines.add(line)));
            }
        }
    }

    /**
     * Returns the points a record adds to a score: {@link #RECORD_FLOWN_POINTS} per contract flown,
     * {@link #RECORD_FAILED_POINTS} per failed or lapsed one, within {@link #RECORD_CAP} either way.
     *
     * @param record the record
     * @return the points
     */
    public static int recordPoints(ContractRecord record) {
        int points = record.flown() * RECORD_FLOWN_POINTS
            + (record.failed() + record.lapsed()) * RECORD_FAILED_POINTS;
        return Math.max(-RECORD_CAP, Math.min(RECORD_CAP, points));
    }

    /**
     * Returns the agency's record in a line.
     *
     * @param snapshot the agency
     * @param lineId   the line
     * @return the record, {@link ContractRecord#NONE} before any
     */
    public static ContractRecord record(PlayerSnapshot snapshot, String lineId) {
        return snapshot.getContractRecords().getOrDefault(lineId, ContractRecord.NONE);
    }

    /**
     * The weakest component a contract flight would use, and its reliability.
     *
     * @param componentId the component, or {@code null} when a launch has no rocket the agency could use
     * @param reliability its reliability now, 0 to 100
     */
    public record Weakest(String componentId, double reliability) {
    }

    /**
     * Returns the weakest component a configuration's flight would use: its payloads, and for each
     * launch the agency's most reliable open rocket that can fly it. Crew and controllers
     * don't count: clients judge the hardware.
     *
     * @param snapshot        the agency
     * @param configurationId the configuration
     * @param catalog         the content pack
     * @return the weakest component
     */
    public static Weakest weakest(PlayerSnapshot snapshot, String configurationId, ContentCatalog catalog) {
        MissionConfiguration configuration = catalog.missionConfiguration(configurationId).orElseThrow();
        Weakest weakest = null;
        for (MissionConfiguration.Launch launch : configuration.launches()) {
            for (String payloadId : launch.payloadComponentIds()) {
                weakest = weaker(weakest, new Weakest(payloadId, reliability(snapshot, payloadId)));
            }
            if (launch.launchType().needsRocket()) {
                Weakest rocket = catalog.missionComponents().stream()
                    .filter(MissionComponent::hasRocketSpec)
                    .filter(component -> active(snapshot, component.id()))
                    .filter(component -> MissionLaunch.canLift(component, launch, catalog))
                    .map(component -> new Weakest(component.id(), reliability(snapshot, component.id())))
                    .max(Comparator.comparingDouble(Weakest::reliability))
                    .orElse(new Weakest(null, 0));
                weakest = weaker(weakest, rocket);
            }
        }
        return weakest == null ? new Weakest(null, 0) : weakest;
    }

    private static boolean active(PlayerSnapshot snapshot, String componentId) {
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        return owned != null && owned.isActive();
    }

    private static Weakest weaker(Weakest so, Weakest candidate) {
        return so == null || candidate.reliability() < so.reliability() ? candidate : so;
    }

    private static double reliability(PlayerSnapshot snapshot, String componentId) {
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        return owned == null ? 0 : owned.getCurrentReliability();
    }

    /**
     * Returns an agency's score for a line's contract flown with one program: the weakest
     * component's reliability, rounded, plus the record's points. The highest score wins tier 1.
     *
     * @param snapshot  the agency
     * @param programId the program
     * @param catalog   the content pack
     * @return the score
     */
    public static int score(PlayerSnapshot snapshot, String programId, ContentCatalog catalog) {
        Commercial commercial = catalog.commercial(programId).orElseThrow();
        return (int) Math.round(weakest(snapshot, commercial.contractConfigurationId(), catalog).reliability())
            + recordPoints(record(snapshot, lineOf(programId, catalog)));
    }

    /**
     * Returns what a configuration's launch would cost the agency with the cheapest rocket it could
     * fly it on: the configuration's cost plus, per launch, the cheapest of its open rockets that can
     * fly it, or the cheapest of its side's when none of its open ones can.
     *
     * @param snapshot        the agency
     * @param configurationId the configuration
     * @param catalog         the content pack
     * @return the launch cost
     */
    public static long launchCost(PlayerSnapshot snapshot, String configurationId, ContentCatalog catalog) {
        MissionConfiguration configuration = catalog.missionConfiguration(configurationId).orElseThrow();
        long cost = configuration.cost();
        for (MissionConfiguration.Launch launch : configuration.launches()) {
            if (!launch.launchType().needsRocket()) {
                continue;
            }
            List<MissionComponent> rockets = catalog.missionComponents().stream()
                .filter(MissionComponent::hasRocketSpec)
                .filter(rocket -> rocket.availableTo(snapshot.getArchetype()))
                .filter(rocket -> MissionLaunch.canLift(rocket, launch, catalog))
                .toList();
            List<MissionComponent> own = rockets.stream()
                .filter(rocket -> active(snapshot, rocket.id())).toList();
            cost += (own.isEmpty() ? rockets : own).stream().mapToLong(MissionComponent::unitCost).min().orElse(0);
        }
        return cost;
    }

    /**
     * Returns a tier's fee for a launch cost: the cost plus the tier's margin.
     *
     * @param launchCost the launch cost
     * @param tier       the tier, 1 to {@link #TIERS}
     * @return the fee
     */
    public static long fee(long launchCost, int tier) {
        return launchCost * (100 + MARGIN_PERCENTS.get(tier - 1)) / 100;
    }

    /**
     * Returns the listing on the agency's board with this id.
     *
     * @param snapshot  the agency
     * @param listingId the listing
     * @return the listing, or empty
     */
    public static Optional<ContractListing> listing(PlayerSnapshot snapshot, String listingId) {
        return snapshot.getContractBoard().stream().filter(listing -> listing.id().equals(listingId)).findFirst();
    }

    /**
     * Says why the agency can't apply for a listing through a program, or {@link ContractOutcome#APPLIED}
     * when it can. With no program named, any qualifying one of the line will do. Whether it can fly
     * the listing at all comes before its active contract, so the board dims what it can't fly.
     *
     * @param snapshot  the agency
     * @param listingId the listing
     * @param programId the program, or {@code null} for any
     * @param catalog   the content pack
     * @return the outcome
     */
    public static ContractOutcome check(PlayerSnapshot snapshot, String listingId, String programId,
            ContentCatalog catalog) {
        ContractListing listing = listing(snapshot, listingId).orElse(null);
        if (listing == null) {
            return ContractOutcome.UNKNOWN_LISTING;
        }
        List<Commercial> unlocked = programsOfLine(snapshot, listing.lineId(), catalog).stream()
            .filter(commercial -> programId == null || commercial.programId().equals(programId))
            .filter(commercial -> unlocked(snapshot, commercial))
            .toList();
        if (unlocked.isEmpty()) {
            return ContractOutcome.NOT_QUALIFIED;
        }
        boolean open = unlocked.stream()
            .anyMatch(commercial -> snapshot.getOpenedProgramIds().contains(commercial.programId()));
        if (!open) {
            return ContractOutcome.PROGRAM_NOT_OPEN;
        }
        return snapshot.getActiveContract() != null ? ContractOutcome.CONTRACT_ACTIVE : ContractOutcome.APPLIED;
    }

    /**
     * Applies for a listing, replacing any application this turn, or withdraws it. Sealed until
     * the turn resolves; one application per turn.
     *
     * @param snapshot  the agency; changed only when the outcome is {@link ContractOutcome#APPLIED}
     *                  or {@link ContractOutcome#WITHDRAWN}
     * @param listingId the listing, or {@code null} to withdraw
     * @param programId the program to fly it with, or {@code null} for the line's first qualifying one
     * @param catalog   the content pack
     * @return the outcome
     */
    public static ContractOutcome apply(PlayerSnapshot snapshot, String listingId, String programId,
            ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        if (listingId == null || listingId.isBlank()) {
            snapshot.setContractApplication(null);
            return ContractOutcome.WITHDRAWN;
        }
        ContractOutcome outcome = check(snapshot, listingId, programId, catalog);
        if (outcome != ContractOutcome.APPLIED) {
            return outcome;
        }
        String lineId = listing(snapshot, listingId).orElseThrow().lineId();
        String program = programId != null ? programId
            : qualifyingPrograms(snapshot, lineId, catalog).getFirst().programId();
        snapshot.setContractApplication(new ContractApplication(listingId, program));
        return ContractOutcome.APPLIED;
    }

    /**
     * Returns whether launching a configuration now would fly the agency's contract: the first
     * launch of the contract's configuration while it is active and on the ground is the contract
     * flight, with no choice about it.
     *
     * @param snapshot        the agency
     * @param configurationId the configuration about to launch
     * @return whether it would
     */
    public static boolean wouldFlyContract(PlayerSnapshot snapshot, String configurationId) {
        ActiveContract contract = snapshot.getActiveContract();
        return contract != null && contract.missionId() == null
            && contract.configurationId().equals(configurationId);
    }

    /**
     * Marks a mission just launched as the contract flight, if it is one.
     *
     * @param snapshot the agency
     * @param mission  the mission, already in flight
     */
    static void launched(PlayerSnapshot snapshot, InProgressMission mission) {
        if (wouldFlyContract(snapshot, mission.getMissionConfigurationId())) {
            mission.setContract(true);
            snapshot.setActiveContract(snapshot.getActiveContract().withMission(mission.getId()));
        }
    }

    /**
     * Settles the contract when its mission is called off: a scrub before the flight puts the
     * contract back on the ground while its window lasts; a flight called off in flight ends it as
     * a failure, with neither fee nor the insurer's refund.
     *
     * @param snapshot the agency
     * @param mission  the mission called off
     * @param unflown  whether it was scrubbed before flying
     */
    static void calledOff(PlayerSnapshot snapshot, InProgressMission mission, boolean unflown) {
        ActiveContract contract = snapshot.getActiveContract();
        if (!mission.isContract() || contract == null || !mission.getId().equals(contract.missionId())) {
            return;
        }
        if (unflown) {
            snapshot.setActiveContract(contract.withMission(null));
            return;
        }
        snapshot.getContractRecords().merge(contract.lineId(), ContractRecord.NONE.withFailed(),
            (was, one) -> was.withFailed());
        snapshot.setActiveContract(null);
    }

    /**
     * Settles a contract flight that has ended: the client's fee on success, else the insurer's refund
     * of the launch cost, plus {@link #CREW_LOSS_PRESTIGE} if the crew was lost. The flight's own
     * failure prestige was already taken by the mission's steps.
     *
     * @param snapshot  the agency
     * @param mission   the mission that ended
     * @param succeeded whether it completed without a failing step
     * @param crewLost  whether its crew was lost
     * @param events    the list to append the {@link ContractSettled} event to
     */
    static void flightEnded(PlayerSnapshot snapshot, InProgressMission mission, boolean succeeded, boolean crewLost,
            List<ResolutionEvent> events) {
        ActiveContract contract = snapshot.getActiveContract();
        if (!mission.isContract() || contract == null || !mission.getId().equals(contract.missionId())) {
            return;
        }
        long funds = succeeded ? contract.fee() : mission.getLaunchCost();
        long prestige = 0;
        int pointsBefore = recordPoints(record(snapshot, contract.lineId()));
        snapshot.setFunds(snapshot.getFunds() + funds);
        if (crewLost) {
            prestige = Math.min(snapshot.getPrestige(), CREW_LOSS_PRESTIGE);
            snapshot.setPrestige(snapshot.getPrestige() - prestige);
        }
        snapshot.getContractRecords().merge(contract.lineId(),
            succeeded ? ContractRecord.NONE.withFlown() : ContractRecord.NONE.withFailed(),
            (was, one) -> succeeded ? was.withFlown() : was.withFailed());
        snapshot.setActiveContract(null);
        events.add(new ContractSettled(succeeded ? ContractSettled.Kind.PAID : ContractSettled.Kind.REFUNDED,
            contract.listingId(), contract.lineId(), contract.clientId(), contract.programId(),
            contract.configurationId(), contract.tier(), contract.fee(), funds, prestige, mission.getId(),
            mission.getLaunchCost(), recordPoints(record(snapshot, contract.lineId())) - pointsBefore));
    }

    /**
     * One agency's contract market this turn, as {@link ContractMarket} settled it.
     *
     * @param findings the agency's findings: its award or miss, and the news
     * @param board    the board for the coming turn, or {@code null} to leave the agency's as it is
     *                 (bot playouts and tests run without a market)
     * @param posted   how many listings were posted for the coming turn
     */
    public record Turn(List<ContractFinding> findings, List<ContractListing> board, int posted) {

        /** No market: nothing found, the board left as it is. */
        public static final Turn NONE = new Turn(List.of(), null, 0);

        /**
         * Copies the lists.
         */
        public Turn {
            findings = findings == null ? List.of() : List.copyOf(findings);
            board = board == null ? null : List.copyOf(board);
        }
    }

    /**
     * The agency's contract phase: a contract not launched in its window lapses, the turn's award or
     * miss is applied, the application is cleared and the board set for the coming turn. Runs after
     * the missions, so a contract flown to its end this season has already paid.
     *
     * @param snapshot the agency
     * @param turn     the agency's part of the market
     * @param gameTurn the turn being resolved
     * @return the events
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, Turn turn, int gameTurn) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(turn, "turn must not be null");
        List<ResolutionEvent> events = new ArrayList<>();
        ActiveContract contract = snapshot.getActiveContract();
        if (contract != null && contract.missionId() == null && gameTurn >= contract.launchByTurn()) {
            long prestige = Math.min(snapshot.getPrestige(), LAPSE_PRESTIGE);
            int pointsBefore = recordPoints(record(snapshot, contract.lineId()));
            snapshot.setPrestige(snapshot.getPrestige() - prestige);
            snapshot.getContractRecords().merge(contract.lineId(), ContractRecord.NONE.withLapsed(),
                (was, one) -> was.withLapsed());
            snapshot.setActiveContract(null);
            events.add(new ContractSettled(ContractSettled.Kind.LAPSED, contract.listingId(), contract.lineId(),
                contract.clientId(), contract.programId(), contract.configurationId(), contract.tier(),
                contract.fee(), 0, prestige, null, 0,
                recordPoints(record(snapshot, contract.lineId())) - pointsBefore));
        }
        for (ContractFinding finding : turn.findings()) {
            ContractListing listing = finding.listing();
            String client = listing.clientFor(snapshot.getArchetype());
            switch (finding.kind()) {
                case AWARDED -> {
                    snapshot.setActiveContract(new ActiveContract(listing.id(), listing.lineId(), client,
                        finding.programId(), finding.configurationId(), finding.tier(), finding.fee(), gameTurn,
                        gameTurn + LAUNCH_WINDOW_TURNS, null));
                    events.add(new ContractSettled(ContractSettled.Kind.AWARDED, listing.id(), listing.lineId(),
                        client, finding.programId(), finding.configurationId(), finding.tier(), finding.fee(), 0, 0,
                        null, 0, 0));
                }
                case MISSED, VOIDED -> events.add(new ContractSettled(finding.kind() == ContractFinding.Kind.MISSED
                        ? ContractSettled.Kind.MISSED : ContractSettled.Kind.VOIDED, listing.id(), listing.lineId(),
                    client, finding.programId(), finding.configurationId(), 0, 0, 0, 0, null, 0, 0));
                case NEWS -> events.add(new ContractNews(ContractNews.Kind.AWARDED, listing.id(), listing.lineId(),
                    client, finding.winners(), 0));
            }
        }
        if (turn.board() != null) {
            snapshot.setContractApplication(null);
            snapshot.setContractBoard(turn.board());
            if (turn.posted() > 0) {
                events.add(new ContractNews(ContractNews.Kind.POSTED, null, null, null, List.of(), turn.posted()));
            }
        }
        return events;
    }
}
