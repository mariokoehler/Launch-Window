package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelCatch;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.IntelligenceState;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.model.TraitState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Stream;

/**
 * The intelligence service ({@code design.md} &sect;2.9, Intelligence): a level from 0 to 3 that moves
 * one step a season toward its target, as many actions a season as the level, and passives that
 * never cost an action. It buys information, never damage. Every number here is a default, to be
 * tuned in play.
 *
 * <p>The background check answers at once, the game's only action that changes something mid-turn;
 * agency assessments and personnel files are queued and settled at the resolution, once for every
 * agency ({@link IntelligenceBoard}), and this class's season pass applies an agency's share.</p>
 *
 * @author Mario Koehler
 */
public final class Intelligence {

    /** The highest level. */
    public static final int MAX_LEVEL = 3;
    /** Cost per season at each level, paid with the fixed costs. */
    public static final long[] COST_PER_SEASON = {0, 150, 350, 700};
    /** Each level's name. */
    public static final List<String> LEVEL_NAMES = List.of("No service", "Security office", "Analysis section",
        "Field network");
    /** The level the background check needs. */
    public static final int CHECK_LEVEL = 1;
    /** The level an agency assessment needs. */
    public static final int ASSESSMENT_LEVEL = 2;
    /** The level a personnel file needs. */
    public static final int PERSONNEL_FILE_LEVEL = 3;
    /** The passive chance per candidate, from level 1, that a trait (or "no notable traits") shows. */
    public static final int PASSIVE_CANDIDATE_PERCENT = 20;
    /** The passive chance per season of a tidbit about a random rival, by level. */
    public static final int[] TIDBIT_PERCENT = {0, 0, 25, 40};
    /** A targeted operation's success against counter-intelligence 0. */
    public static final int SUCCESS_BASE_PERCENT = 85;
    /** Success lost per level of the target's counter-intelligence. */
    public static final int SUCCESS_PER_LEVEL = 15;
    /** The chance a failed operation is caught, against counter-intelligence 0. */
    public static final int CATCH_BASE_PERCENT = 10;
    /** The catch chance added per level of the target's counter-intelligence. */
    public static final int CATCH_PER_LEVEL = 10;
    /** The prestige an agency loses when its agent is caught. */
    public static final long CAUGHT_PRESTIGE = 20;
    /** The tidbits kept per folder; older ones drop off. */
    public static final int TIDBITS_KEPT = 12;
    /**
     * The personnel files kept per folder; the oldest drops off. Folders travel in the player's
     * state, which isn't gzipped, so every list in them is capped (Kryo's 64 KB object buffer).
     */
    public static final int FILES_KEPT = 8;
    /** The catches kept per folder, both ways; the oldest drops off. */
    public static final int CATCHES_KEPT = 10;

    private static final long CANDIDATE_SALT = 0x1A7E_5EC0_C0DE_1961L;

    /**
     * What an intelligence action came to.
     */
    public enum Outcome {
        /** The target level was set. */
        TARGET_SET,
        /** The operation was queued. */
        QUEUED,
        /** The operation was cancelled and its action given back. */
        CANCELLED,
        /** The background check answered. */
        CHECKED,
        /** The target level isn't between 0 and 3. */
        INVALID_LEVEL,
        /** The service's level is too low for this. */
        LEVEL_TOO_LOW,
        /** Every action this season is used. */
        NO_ACTION_LEFT,
        /** No candidate with that id. */
        UNKNOWN_CANDIDATE,
        /** An earlier check already found everything there is. */
        NOTHING_LEFT_TO_FIND,
        /** No queued operation with that id. */
        UNKNOWN_OPERATION,
        /** The operation is missing its rival, area or group. */
        INVALID_OPERATION
    }

    private Intelligence() {
    }

    /**
     * Returns what a level costs a season.
     *
     * @param level the level
     * @return the cost
     */
    public static long cost(int level) {
        return COST_PER_SEASON[Math.clamp(level, 0, MAX_LEVEL)];
    }

    /**
     * Returns the actions an agency has left this season.
     *
     * @param snapshot the agency
     * @return the actions left, never negative
     */
    public static int actionsLeft(PlayerSnapshot snapshot) {
        IntelligenceState state = snapshot.getIntelligence();
        return Math.max(0, state.getLevel() - state.getActionsUsed());
    }

    /**
     * Returns a targeted operation's chance of success against a counter-intelligence level.
     *
     * @param targetLevel the target's level
     * @return the chance, in percent
     */
    public static int successPercent(int targetLevel) {
        return SUCCESS_BASE_PERCENT - SUCCESS_PER_LEVEL * Math.clamp(targetLevel, 0, MAX_LEVEL);
    }

    /**
     * Returns the chance that a failed operation is caught.
     *
     * @param targetLevel the target's level
     * @return the chance, in percent
     */
    public static int catchPercent(int targetLevel) {
        return CATCH_BASE_PERCENT + CATCH_PER_LEVEL * Math.clamp(targetLevel, 0, MAX_LEVEL);
    }

    /**
     * Returns the chance an operation is caught at all: it fails, and the failure is caught.
     *
     * @param targetLevel the target's level
     * @return the chance, in percent, rounded
     */
    public static int caughtOverallPercent(int targetLevel) {
        return Math.round((100 - successPercent(targetLevel)) * catchPercent(targetLevel) / 100f);
    }

    /**
     * Returns how wide a learned range is either side: tighter the further the spy's level is over
     * the target's.
     *
     * @param spyLevel    the spying agency's level
     * @param targetLevel the target's level
     * @return the half-width, in points
     */
    public static int halfWidth(int spyLevel, int targetLevel) {
        return Math.clamp(5 + 5 * (targetLevel - spyLevel + 2), 2, 15);
    }

    /**
     * Sets the level the service moves toward. It takes effect from the next resolution, one step a
     * season.
     *
     * @param snapshot the agency
     * @param target   the target level, 0 to 3
     * @return the outcome
     */
    public static Outcome setTarget(PlayerSnapshot snapshot, int target) {
        if (target < 0 || target > MAX_LEVEL) {
            return Outcome.INVALID_LEVEL;
        }
        snapshot.getIntelligence().setTargetLevel(target);
        return Outcome.TARGET_SET;
    }

    /**
     * Queues an operation for the resolution, using one action.
     *
     * @param snapshot the agency
     * @param kind     the operation
     * @param targetId the rival's player id
     * @param area     an assessment's area
     * @param group    a personnel file's group, by {@link PersonnelGroup} name
     * @param personId a personnel file's named person, or {@code null}
     * @return the outcome
     */
    public static Outcome queue(PlayerSnapshot snapshot, IntelOperation.Kind kind, String targetId, IntelArea area,
            String group, String personId) {
        if (kind == null || targetId == null || targetId.isBlank()
                || kind == IntelOperation.Kind.ASSESSMENT && area == null
                || kind == IntelOperation.Kind.PERSONNEL_FILE && !isGroup(group)) {
            return Outcome.INVALID_OPERATION;
        }
        IntelligenceState state = snapshot.getIntelligence();
        int needed = kind == IntelOperation.Kind.ASSESSMENT ? ASSESSMENT_LEVEL : PERSONNEL_FILE_LEVEL;
        if (state.getLevel() < needed) {
            return Outcome.LEVEL_TOO_LOW;
        }
        if (actionsLeft(snapshot) == 0) {
            return Outcome.NO_ACTION_LEFT;
        }
        boolean assessment = kind == IntelOperation.Kind.ASSESSMENT;
        state.getQueued().add(new IntelOperation(state.allocateOperationId(), kind, targetId,
            assessment ? area : null, assessment ? null : group, assessment || personId == null || personId.isBlank()
                ? null : personId));
        state.setActionsUsed(state.getActionsUsed() + 1);
        return Outcome.QUEUED;
    }

    /**
     * Cancels a queued operation and gives its action back.
     *
     * @param snapshot    the agency
     * @param operationId the operation
     * @return the outcome
     */
    public static Outcome cancel(PlayerSnapshot snapshot, String operationId) {
        IntelligenceState state = snapshot.getIntelligence();
        boolean removed = state.getQueued().removeIf(operation -> operation.id().equals(operationId));
        if (!removed) {
            return Outcome.UNKNOWN_OPERATION;
        }
        state.setActionsUsed(Math.max(0, state.getActionsUsed() - 1));
        return Outcome.CANCELLED;
    }

    /**
     * Runs a background check on a candidate: it answers at once, revealing one trait the player
     * doesn't know yet, or "no notable traits". Which trait comes from the candidate's own seed and
     * how many are already revealed, so replaying the action always gives the same answer. It can't
     * be undone, and it never fails.
     *
     * @param snapshot    the agency
     * @param candidateId the candidate
     * @return the outcome
     */
    public static Outcome backgroundCheck(PlayerSnapshot snapshot, String candidateId) {
        Optional<Personnel> found = candidates(snapshot)
            .filter(candidate -> candidate.getId().equals(candidateId)).findFirst();
        if (found.isEmpty()) {
            return Outcome.UNKNOWN_CANDIDATE;
        }
        if (snapshot.getIntelligence().getLevel() < CHECK_LEVEL) {
            return Outcome.LEVEL_TOO_LOW;
        }
        Personnel candidate = found.get();
        if (candidate.isIntelExhausted()) {
            return Outcome.NOTHING_LEFT_TO_FIND;
        }
        if (actionsLeft(snapshot) == 0) {
            return Outcome.NO_ACTION_LEFT;
        }
        IntelligenceState state = snapshot.getIntelligence();
        state.setActionsUsed(state.getActionsUsed() + 1);
        int revealed = (int) candidate.getTraits().stream().filter(TraitState::revealed).count();
        if (!revealOne(candidate, new SplittableRandom(seed(snapshot.getAgencyName(), candidate.getId())
                ^ CANDIDATE_SALT * (revealed + 2)))) {
            candidate.setIntelExhausted(true);
        }
        candidate.setIntelFind(Personnel.FIND_CHECK);
        return Outcome.CHECKED;
    }

    /**
     * Rolls the passive find for a fresh candidate, from level 1: one in five shows a trait, or "no
     * notable traits". Rolled from the candidate's own seed, never the season's stream, so the pool
     * draws exactly as it would without a service.
     *
     * @param snapshot  the agency
     * @param candidate the candidate, traits assigned
     */
    static void passiveFind(PlayerSnapshot snapshot, Personnel candidate) {
        if (snapshot.getIntelligence().getLevel() < CHECK_LEVEL) {
            return;
        }
        SplittableRandom random = new SplittableRandom(seed(snapshot.getAgencyName(), candidate.getId())
            ^ CANDIDATE_SALT);
        if (random.nextInt(100) >= PASSIVE_CANDIDATE_PERCENT) {
            return;
        }
        if (!revealOne(candidate, random)) {
            candidate.setIntelExhausted(true);
        }
        candidate.setIntelFind(Personnel.FIND_PASSIVE);
    }

    private static boolean revealOne(Personnel person, SplittableRandom random) {
        List<TraitState> traits = new ArrayList<>(person.getTraits());
        List<Integer> hidden = new ArrayList<>();
        for (int i = 0; i < traits.size(); i++) {
            if (!traits.get(i).revealed()) {
                hidden.add(i);
            }
        }
        if (hidden.isEmpty()) {
            return false;
        }
        int index = hidden.get(random.nextInt(hidden.size()));
        traits.set(index, traits.get(index).reveal());
        person.setTraits(traits);
        return true;
    }

    /**
     * Runs the intelligence part of the season pass, before the budget: applies this agency's
     * findings, clears the queue, moves the level one step (down if the agency is in debt, else
     * toward the target), and frees the season's actions. At level 0 with nothing to apply it does
     * nothing at all.
     *
     * @param snapshot the agency
     * @param findings this agency's share of the turn's findings
     * @param gameTurn the turn being resolved
     * @return the events: one {@link IntelligenceFound} per finding, then any level change
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, List<IntelFinding> findings,
            int gameTurn) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        IntelligenceState state = snapshot.getIntelligence();
        List<ResolutionEvent> events = new ArrayList<>();
        Set<String> settled = new HashSet<>();
        for (IntelFinding finding : findings == null ? List.<IntelFinding>of() : findings) {
            apply(snapshot, finding, gameTurn);
            if (finding.operationId() != null) {
                settled.add(finding.operationId());
            }
            events.add(new IntelligenceFound(finding));
        }
        for (IntelOperation operation : state.getQueued()) {
            if (!settled.contains(operation.id())) {
                events.add(new IntelligenceFound(IntelFinding.about(IntelFinding.Kind.NOT_SETTLED, operation,
                    null, -1)));
            }
        }
        state.getQueued().clear();
        state.setActionsUsed(0);
        int from = state.getLevel();
        boolean debt = snapshot.getFunds() < 0 && from > 0;
        int to = debt ? from - 1 : from + Integer.signum(state.getTargetLevel() - from);
        if (to != from) {
            state.setLevel(to);
            events.add(new IntelligenceLevelChanged(from, to, state.getTargetLevel(), debt));
        }
        return events;
    }

    private static void apply(PlayerSnapshot snapshot, IntelFinding finding, int gameTurn) {
        if (finding.rivalId() == null) {
            return;
        }
        RivalFolder folder = snapshot.getIntelligence().folder(finding.rivalId(), finding.rivalAgency());
        switch (finding.kind()) {
            case DOSSIER -> {
                folder.getDossiers().put(finding.dossier().area(), finding.dossier());
                if (finding.level() >= 0) {
                    folder.learnLevel(finding.level(), gameTurn, RivalFolder.FROM_DOSSIER);
                }
            }
            case PERSONNEL_FILE -> {
                folder.getPersonnelFiles().removeIf(file -> file.personId().equals(finding.file().personId()));
                folder.getPersonnelFiles().add(finding.file());
                trim(folder.getPersonnelFiles(), FILES_KEPT);
            }
            case TIDBIT -> {
                folder.getTidbits().add(finding.tidbit());
                trim(folder.getTidbits(), TIDBITS_KEPT);
            }
            case CAUGHT -> {
                snapshot.setPrestige(Math.max(0, snapshot.getPrestige() - CAUGHT_PRESTIGE));
                folder.getCatches().add(new IntelCatch(gameTurn, true, finding.operationKind(), finding.area(),
                    finding.level()));
                folder.learnLevel(finding.level(), gameTurn, RivalFolder.FROM_BEING_CAUGHT);
                trim(folder.getCatches(), CATCHES_KEPT);
            }
            case CAUGHT_THEIRS -> {
                folder.getCatches().add(new IntelCatch(gameTurn, false, finding.operationKind(), finding.area(),
                    finding.level()));
                folder.learnLevel(finding.level(), gameTurn, RivalFolder.FROM_A_CATCH);
                trim(folder.getCatches(), CATCHES_KEPT);
            }
            case FAILED, NO_TARGET, NOT_SETTLED -> {
                // Nothing learned.
            }
        }
    }

    private static void trim(List<?> list, int kept) {
        while (list.size() > kept) {
            list.removeFirst();
        }
    }

    /**
     * Returns every candidate in the agency's three pools.
     *
     * @param snapshot the agency
     * @return the candidates
     */
    static Stream<Personnel> candidates(PlayerSnapshot snapshot) {
        return Stream.of(snapshot.getAstronautCandidates(), snapshot.getSetSpecialistCandidates(),
            snapshot.getMissionControlCandidates()).flatMap(List::stream);
    }

    private static boolean isGroup(String group) {
        if (group == null) {
            return false;
        }
        for (PersonnelGroup value : PersonnelGroup.values()) {
            if (value.name().equals(group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mixes a string key into a seed, the same on every machine.
     *
     * @param agency the agency
     * @param key    the key
     * @return the seed
     */
    static long seed(String agency, String key) {
        long hash = 1125899906842597L;
        String text = agency + "/" + key;
        for (int i = 0; i < text.length(); i++) {
            hash = 31 * hash + text.charAt(i);
        }
        return new SplittableRandom(hash).nextLong();
    }
}
