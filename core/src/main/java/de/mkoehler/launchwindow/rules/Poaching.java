package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.IntelligenceState;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PersonnelFile;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Retention;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Poaching ({@code design.md} &sect;2.2(d), phase 5): an offer to a rival's engineer or flight
 * controller, made from a personnel file, and the standing orders that answer one. Never astronauts.
 *
 * <p>An offer is queued like an intelligence operation and settled with the turn's intelligence,
 * once for every agency ({@link IntelligenceBoard}); each agency's own season pass then applies its
 * side ({@link #applyMade}, {@link #applyReceived}). Nobody has to be online to react: the target
 * agency's answer is its standing order, and the person's is a roll from the offer's own seed.</p>
 *
 * <p>Every number here is our own first-pass default, unconfirmed (2026-10-08).</p>
 *
 * @author Mario Koehler
 */
public final class Poaching {

    /** The raises an offer can make, in percent over the person's salary. */
    public static final List<Integer> RAISE_PERCENTS = List.of(25, 50, 100);
    /** The base chance, in percent, that a person of middling loyalty takes each raise. */
    public static final List<Integer> ACCEPT_BASE_PERCENTS = List.of(15, 30, 50);
    /** The agency defaults a player can pick: match offers up to this raise; 0 never matches. */
    public static final List<Integer> MATCH_PERCENTS = List.of(0, 25, 50, 100);
    /** A new agency's default, for every group. */
    public static final int DEFAULT_MATCH_PERCENT = 25;
    /** The groups whose people can be approached. Never astronauts (user, 2026-10-08). */
    public static final List<PersonnelGroup> GROUPS = List.of(PersonnelGroup.SET, PersonnelGroup.MISSION_CONTROL);
    /** The seasons a person must have been with their agency to be approached. */
    public static final int MIN_TENURE_SEASONS = 4;
    /** The seasons after an offer before anyone may approach the same person again. */
    public static final int APPROACH_COOLDOWN_SEASONS = 4;
    /** Offers an agency may make a season. */
    public static final int OFFERS_PER_SEASON = 1;
    /** The loyalty at which the base chance applies as it stands; at 100 nobody leaves. */
    public static final int LOYALTY_PIVOT = 50;
    /** What a disappointed person adds to the chance, in points. */
    public static final int DISAPPOINTED_POINTS = 20;
    /** Morale below this adds {@link #LOW_MORALE_POINTS}. */
    public static final int LOW_MORALE_BELOW = 40;
    /** What low morale adds to the chance, in points. */
    public static final int LOW_MORALE_POINTS = 10;
    /** What a Restless person adds to the chance, in points. */
    public static final int RESTLESS_POINTS = 10;
    /** What a Steady person adds to the chance, in points (it takes some away). */
    public static final int STEADY_POINTS = -10;
    /** The chance never goes above this. */
    public static final int ACCEPT_MAX_PERCENT = 90;
    /** The loyalty a matched offer adds. */
    public static final int MATCH_LOYALTY = 5;

    private static final ObjectMapper COPIER = new ObjectMapper();

    /**
     * What a poaching action came to.
     */
    public enum Outcome {
        /** The offer was queued. */
        QUEUED,
        /** The standing order was set. */
        RETENTION_SET,
        /** The service's level is too low: offers need a personnel file. */
        LEVEL_TOO_LOW,
        /** There's no personnel file on that person in that rival's folder. */
        NO_FILE,
        /** Astronauts can't be approached. */
        CANT_APPROACH_GROUP,
        /** The raise isn't one of {@link #RAISE_PERCENTS}. */
        INVALID_RAISE,
        /** This season's offer is already made. */
        OFFER_USED,
        /** The percentage isn't one of {@link #MATCH_PERCENTS}, or the group can't be poached. */
        INVALID_RETENTION,
        /** No such employee. */
        UNKNOWN_EMPLOYEE
    }

    /**
     * Why a person in a file can't be approached now, as far as the file tells.
     */
    public enum Blocker {
        /** An astronaut. */
        ASTRONAUT,
        /** With their agency under {@link #MIN_TENURE_SEASONS} seasons. */
        TOO_NEW,
        /** Someone made them an offer within the last {@link #APPROACH_COOLDOWN_SEASONS} seasons. */
        APPROACHED_RECENTLY
    }

    private Poaching() {
    }

    // ---- Offers

    /**
     * Queues an offer to a person in one of the agency's personnel files, for the resolution. It uses
     * no intelligence action, but there's one a season. Whether the person can be approached is
     * settled at the resolution; a file only tells as of its date.
     *
     * @param snapshot     the agency making it
     * @param rivalId      the rival's player id
     * @param personId     the person's id in the rival's agency
     * @param raisePercent the raise, one of {@link #RAISE_PERCENTS}
     * @return the outcome
     */
    public static Outcome queueOffer(PlayerSnapshot snapshot, String rivalId, String personId, int raisePercent) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        IntelligenceState state = snapshot.getIntelligence();
        if (state.getLevel() < Intelligence.PERSONNEL_FILE_LEVEL) {
            return Outcome.LEVEL_TOO_LOW;
        }
        PersonnelFile file = file(snapshot, rivalId, personId);
        if (file == null) {
            return Outcome.NO_FILE;
        }
        if (!canBeApproached(file.group())) {
            return Outcome.CANT_APPROACH_GROUP;
        }
        if (!RAISE_PERCENTS.contains(raisePercent)) {
            return Outcome.INVALID_RAISE;
        }
        if (queuedOffers(snapshot).size() >= OFFERS_PER_SEASON) {
            return Outcome.OFFER_USED;
        }
        state.getQueued().add(new IntelOperation(state.allocateOperationId(), IntelOperation.Kind.OFFER, rivalId,
            null, file.group(), personId, raisePercent));
        return Outcome.QUEUED;
    }

    /**
     * Returns the offers queued this season.
     *
     * @param snapshot the agency
     * @return the offers, in queue order
     */
    public static List<IntelOperation> queuedOffers(PlayerSnapshot snapshot) {
        return snapshot.getIntelligence().getQueued().stream()
            .filter(operation -> operation.kind() == IntelOperation.Kind.OFFER).toList();
    }

    /**
     * Returns the agency's file on a rival's person.
     *
     * @param snapshot the agency
     * @param rivalId  the rival's player id
     * @param personId the person's id in the rival's agency
     * @return the file, or {@code null}
     */
    public static PersonnelFile file(PlayerSnapshot snapshot, String rivalId, String personId) {
        if (rivalId == null || personId == null) {
            return null;
        }
        RivalFolder folder = snapshot.getIntelligence().getFolders().get(rivalId);
        if (folder == null) {
            return null;
        }
        return folder.getPersonnelFiles().stream().filter(file -> file.personId().equals(personId)).findFirst()
            .orElse(null);
    }

    /**
     * Returns whether a group's people can be approached at all.
     *
     * @param group the group, by {@link PersonnelGroup} name
     * @return whether they can
     */
    public static boolean canBeApproached(String group) {
        return GROUPS.stream().anyMatch(value -> value.name().equals(group));
    }

    /**
     * Returns why the person in a file can't be approached now, as far as the file tells, or
     * {@code null} if nothing in it stands in the way. The resolution decides for certain.
     *
     * @param file        the file
     * @param currentTurn the turn the next resolution settles
     * @return the blocker, or {@code null}
     */
    public static Blocker blocker(PersonnelFile file, int currentTurn) {
        if (!canBeApproached(file.group())) {
            return Blocker.ASTRONAUT;
        }
        if (file.tenureSeasons() + Math.max(0, currentTurn - file.turn()) < MIN_TENURE_SEASONS) {
            return Blocker.TOO_NEW;
        }
        if (file.lastApproachedTurn() != null && currentTurn - file.lastApproachedTurn() < APPROACH_COOLDOWN_SEASONS) {
            return Blocker.APPROACHED_RECENTLY;
        }
        return null;
    }

    /**
     * Returns the first turn a person approached at a turn can be approached again.
     *
     * @param approachedTurn the turn they were approached
     * @return the turn
     */
    public static int approachableAgain(int approachedTurn) {
        return approachedTurn + APPROACH_COOLDOWN_SEASONS;
    }

    /**
     * Returns the salary an offer makes: the old one raised, in whole funds.
     *
     * @param salary       the person's salary
     * @param raisePercent the raise
     * @return the new salary
     */
    public static long offeredSalary(double salary, int raisePercent) {
        return Math.round(salary * (100 + raisePercent) / 100.0);
    }

    /**
     * Returns the signing bonus an offer costs if the person comes: one season of the new salary.
     *
     * @param newSalary the new salary
     * @return the bonus
     */
    public static long signingBonus(double newSalary) {
        return Math.round(newSalary);
    }

    // ---- The person's answer

    /**
     * Returns the chance, in percent, that a person takes an offer their agency didn't match.
     *
     * @param raisePercent the raise
     * @param loyalty      their loyalty, 0 to 100
     * @param morale       their morale
     * @param disappointed whether an ambition of theirs ran out lately
     * @param traits       their traits
     * @return the chance, 0 to {@link #ACCEPT_MAX_PERCENT}
     */
    public static int acceptPercent(int raisePercent, int loyalty, int morale, boolean disappointed,
            Collection<Trait> traits) {
        double chance = base(raisePercent) * loyaltyFactor(loyalty);
        chance += disappointed ? DISAPPOINTED_POINTS : 0;
        chance += morale < LOW_MORALE_BELOW ? LOW_MORALE_POINTS : 0;
        chance += traitPoints(traits);
        return (int) Math.clamp(Math.round(chance), 0, ACCEPT_MAX_PERCENT);
    }

    /**
     * Returns the chance a file lets the poacher see: a range over the file's loyalty band, with what
     * the file shows (disappointment, the one trait). Morale and traits the file doesn't show aren't
     * counted; the screens say so.
     *
     * @param raisePercent the raise
     * @param file         the file
     * @return the lowest and highest chance, in percent
     */
    public static int[] answerRange(int raisePercent, PersonnelFile file) {
        int[] band = loyaltyBounds(file.loyalty());
        List<Trait> known = file.trait() == null ? List.of() : List.of(file.trait());
        double extra = (file.disappointed() ? DISAPPOINTED_POINTS : 0) + traitPoints(known);
        double low = base(raisePercent) * loyaltyFactor(band[1]) + extra;
        double high = base(raisePercent) * loyaltyFactor(band[0]) + extra;
        return new int[] {(int) Math.clamp(Math.round(low), 0, ACCEPT_MAX_PERCENT),
            (int) Math.clamp(Math.round(high), 0, ACCEPT_MAX_PERCENT)};
    }

    /**
     * Returns the loyalty a band covers.
     *
     * @param band "Low", "Middling" or "High" ({@link Careers#loyaltyBand}); anything else is the
     *             whole scale
     * @return the lowest and highest loyalty
     */
    public static int[] loyaltyBounds(String band) {
        return switch (band == null ? "" : band) {
            case "Low" -> new int[] {0, 39};
            case "Middling" -> new int[] {40, 69};
            case "High" -> new int[] {70, 100};
            default -> new int[] {0, 100};
        };
    }

    private static double base(int raisePercent) {
        int index = RAISE_PERCENTS.indexOf(raisePercent);
        return index < 0 ? 0 : ACCEPT_BASE_PERCENTS.get(index);
    }

    private static double loyaltyFactor(int loyalty) {
        return Math.max(0, 100 - loyalty) / (double) LOYALTY_PIVOT;
    }

    private static int traitPoints(Collection<Trait> traits) {
        int points = 0;
        for (Trait trait : traits) {
            points += trait == Trait.RESTLESS ? RESTLESS_POINTS : trait == Trait.STEADY ? STEADY_POINTS : 0;
        }
        return points;
    }

    // ---- Standing orders

    /**
     * Returns the agency's default for a group: offers up to this raise are matched.
     *
     * @param snapshot the agency
     * @param group    the group
     * @return the percentage, 0 for never
     */
    public static int matchPercent(PlayerSnapshot snapshot, PersonnelGroup group) {
        return snapshot.getRetentionMatchPercent().getOrDefault(group.name(), DEFAULT_MATCH_PERCENT);
    }

    /**
     * Sets the agency's default for a group.
     *
     * @param snapshot the agency
     * @param group    the group, an engineer's or controller's
     * @param percent  one of {@link #MATCH_PERCENTS}
     * @return the outcome
     */
    public static Outcome setDefault(PlayerSnapshot snapshot, PersonnelGroup group, int percent) {
        if (group == null || !GROUPS.contains(group) || !MATCH_PERCENTS.contains(percent)) {
            return Outcome.INVALID_RETENTION;
        }
        snapshot.getRetentionMatchPercent().put(group.name(), percent);
        return Outcome.RETENTION_SET;
    }

    /**
     * Sets how the agency answers offers to one person.
     *
     * @param snapshot  the agency
     * @param group     the person's group, an engineer's or controller's
     * @param personId  the person
     * @param retention the standing order
     * @return the outcome
     */
    public static Outcome setRetention(PlayerSnapshot snapshot, PersonnelGroup group, String personId,
            Retention retention) {
        if (group == null || !GROUPS.contains(group) || retention == null) {
            return Outcome.INVALID_RETENTION;
        }
        Personnel person = roster(snapshot, group).stream().filter(p -> p.getId().equals(personId)).findFirst()
            .orElse(null);
        if (person == null) {
            return Outcome.UNKNOWN_EMPLOYEE;
        }
        person.setRetention(retention);
        return Outcome.RETENTION_SET;
    }

    /**
     * Returns whether the agency matches an offer to one of its people.
     *
     * @param snapshot     the agency
     * @param person       the person
     * @param group        their group
     * @param raisePercent the raise offered
     * @return whether it's matched
     */
    public static boolean matches(PlayerSnapshot snapshot, Personnel person, PersonnelGroup group, int raisePercent) {
        return switch (person.getRetention()) {
            case KEEP -> true;
            case LET_GO -> false;
            case DEFAULT -> raisePercent <= matchPercent(snapshot, group);
        };
    }

    // ---- Settling (once a turn, for every agency)

    /**
     * Returns why a person can't be approached at the resolution, or {@code null} if they can.
     *
     * @param person   the person
     * @param group    their group
     * @param gameTurn the turn being resolved
     * @return the reason, or {@code null}
     */
    static OfferResult.Outcome ineligible(Personnel person, PersonnelGroup group, int gameTurn) {
        boolean busy = person.getAssignmentState() == AssignmentState.IN_TRAINING
            || group == PersonnelGroup.MISSION_CONTROL && person.getAssignmentState() == AssignmentState.ASSIGNED;
        boolean recent = person.getLastApproachedTurn() != null
            && gameTurn - person.getLastApproachedTurn() < APPROACH_COOLDOWN_SEASONS;
        return !GROUPS.contains(group) || busy || recent || person.getTenureSeasons() < MIN_TENURE_SEASONS
            ? OfferResult.Outcome.LAPSED_NOT_ELIGIBLE : null;
    }

    /**
     * Returns whether an agency has room for one more in a group's roster.
     *
     * @param snapshot the agency
     * @param group    the group
     * @param arriving people already coming to it this turn
     * @param catalog  the content pack, for the buildings
     * @return whether there's room
     */
    static boolean hasRoom(PlayerSnapshot snapshot, PersonnelGroup group, int arriving, ContentCatalog catalog) {
        Building building = group == PersonnelGroup.SET ? Building.SET_CENTER : Building.MISSION_CONTROL;
        return roster(snapshot, group).size() + arriving < BuildingUpgrades.capacity(snapshot, building,
            catalog.buildings());
    }

    /**
     * Returns a copy of a person, to carry to the poacher untouched by the target's own pass.
     *
     * @param person the person
     * @param <T>    their class
     * @return the copy
     */
    @SuppressWarnings("unchecked")
    static <T extends Personnel> T copy(T person) {
        try {
            return (T) COPIER.readValue(COPIER.writeValueAsBytes(person), person.getClass());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not copy a person", e);
        }
    }

    // ---- Applying (each agency's own pass)

    /**
     * Applies an offer this agency made: someone who came over joins the roster with a new id, the
     * signing bonus is paid, and the file notes the approach.
     *
     * @param snapshot the agency
     * @param finding  the finding
     * @param gameTurn the turn being resolved
     * @return the report's line
     */
    static OfferSettled applyMade(PlayerSnapshot snapshot, IntelFinding finding, int gameTurn) {
        OfferResult offer = finding.offer();
        String personId = offer.personId();
        PersonnelFile file = file(snapshot, finding.rivalId(), offer.personId());
        RivalFolder folder = snapshot.getIntelligence().getFolders().get(finding.rivalId());
        if (offer.outcome() == OfferResult.Outcome.JOINED) {
            Personnel recruit = offer.engineer() != null ? copy(offer.engineer()) : copy(offer.controller());
            personId = snapshot.allocatePersonnelId();
            arrive(recruit, personId, file == null ? null : file.trait(), offer, finding.rivalAgency(), gameTurn);
            if (recruit instanceof SetSpecialist engineer) {
                snapshot.getSetSpecialists().add(engineer);
            } else {
                snapshot.getMissionControlOfficers().add((MissionControlOfficer) recruit);
            }
            snapshot.setFunds(snapshot.getFunds() - offer.signingBonus());
            if (folder != null) {
                // They're ours now: no file, and no offer card, on our own employee.
                folder.getPersonnelFiles().removeIf(f -> f.personId().equals(offer.personId()));
            }
        } else if (file != null && folder != null && (offer.outcome() == OfferResult.Outcome.MATCHED
                || offer.outcome() == OfferResult.Outcome.TURNED_DOWN)) {
            double salary = offer.outcome() == OfferResult.Outcome.MATCHED ? offer.newSalary() : file.salary();
            int index = folder.getPersonnelFiles().indexOf(file);
            folder.getPersonnelFiles().set(index, new PersonnelFile(file.personId(), file.name(), file.portraitId(),
                file.group(), file.age(), file.turn(), file.skills(), file.trait(), file.loyalty(), file.ambition(),
                file.disappointed(), salary, file.tenureSeasons(), gameTurn));
        }
        String name = offer.personName().isBlank() && file != null ? file.name() : offer.personName();
        return new OfferSettled(true, finding.rivalId(), finding.rivalAgency(), personId, name,
            offer.group(), offer.raisePercent(), offer.outcome(), offer.oldSalary(), offer.newSalary(),
            offer.outcome() == OfferResult.Outcome.JOINED ? offer.signingBonus() : 0, "", -1);
    }

    private static void arrive(Personnel recruit, String id, Trait filedTrait, OfferResult offer, String fromAgency,
            int gameTurn) {
        recruit.setId(id);
        // A new agency gets to know them again: only the trait the file showed is known.
        recruit.setTraits(recruit.getTraits().stream()
            .map(state -> new TraitState(state.trait(), state.revealAt(), state.trait() == filedTrait)).toList());
        recruit.setWorkSeasons(0);
        recruit.setProjectId(null);
        recruit.setProjectSeasons(0);
        recruit.setAssignmentState(AssignmentState.IDLE);
        recruit.setAssignedMissionId(null);
        recruit.setTrainingKind(TrainingKind.NONE);
        recruit.setTrainingSeasonsCompleted(0);
        recruit.setTrainingSeasonsTotal(0);
        recruit.setStintComponentId(null);
        recruit.setStintSeasons(0);
        recruit.setStintGain(0);
        recruit.setTenureSeasons(0);
        recruit.setLoyalty(Careers.LOYALTY_START);
        recruit.setLastApproachedTurn(gameTurn);
        recruit.setRetention(Retention.DEFAULT);
        recruit.setIntelFind(null);
        recruit.setSalary(offer.newSalary());
        if (recruit instanceof SetSpecialist engineer) {
            engineer.setRdTargetComponentId(null);
            engineer.setRdSlot(null);
        }
        recruit.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.JOINED, fromAgency,
            String.valueOf(Math.round(offer.oldSalary())), "", offer.newSalary(), offer.raisePercent()));
    }

    /**
     * Applies a rival's offer to one of this agency's people: a match raises their salary for good,
     * a refusal is noted, and someone who leaves is taken off the roster (an R&amp;D slot frees up
     * as on dismissal). If they already left this season in another way, only the line remains.
     *
     * @param snapshot the agency
     * @param finding  the finding
     * @param gameTurn the turn being resolved
     * @return the report's line
     */
    static OfferSettled applyReceived(PlayerSnapshot snapshot, IntelFinding finding, int gameTurn) {
        OfferResult offer = finding.offer();
        PersonnelGroup group = PersonnelGroup.valueOf(offer.group());
        List<? extends Personnel> roster = roster(snapshot, group);
        Personnel person = roster.stream().filter(p -> p.getId().equals(offer.personId())).findFirst().orElse(null);
        Retention retention = person == null ? Retention.DEFAULT : person.getRetention();
        if (person != null) {
            switch (offer.outcome()) {
                case MATCHED -> {
                    person.setSalary(offer.newSalary());
                    person.setLoyalty(Math.clamp(person.getLoyalty() + MATCH_LOYALTY, 0, 100));
                    person.setLastApproachedTurn(gameTurn);
                    person.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.OFFER, finding.rivalAgency(),
                        String.valueOf(Math.round(offer.oldSalary())), OfferResult.Outcome.MATCHED.name(), offer.newSalary(), offer.raisePercent()));
                }
                case TURNED_DOWN -> {
                    person.setLastApproachedTurn(gameTurn);
                    person.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.OFFER, finding.rivalAgency(), "",
                        OfferResult.Outcome.TURNED_DOWN.name(), person.getSalary(), offer.raisePercent()));
                }
                case JOINED -> roster.remove(person);
                default -> {
                    // The target is never told of an offer that lapsed.
                }
            }
        }
        return new OfferSettled(false, finding.rivalId(), finding.rivalAgency(), offer.personId(), offer.personName(),
            offer.group(), offer.raisePercent(), offer.outcome(), offer.oldSalary(), offer.newSalary(), 0,
            retention.name(), retention == Retention.DEFAULT ? matchPercent(snapshot, group) : -1);
    }

    static List<? extends Personnel> roster(PlayerSnapshot snapshot, PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> snapshot.getAstronauts();
            case SET -> snapshot.getSetSpecialists();
            case MISSION_CONTROL -> snapshot.getMissionControlOfficers();
        };
    }

    /**
     * Returns the person's traits, for the answer's roll.
     *
     * @param person the person
     * @return the traits, shown or not
     */
    static List<Trait> traits(Personnel person) {
        List<Trait> traits = new ArrayList<>();
        person.getTraits().forEach(state -> traits.add(state.trait()));
        return traits;
    }

    /**
     * Returns the offers queued by every agency, highest raise first, then by agency and offer id,
     * so two offers to one person settle the same way whatever order agencies resolve in.
     *
     * @param agencies every agency, by player id, sorted
     * @return the offers, each with its agency's id
     */
    static List<Map.Entry<String, IntelOperation>> allOffers(Map<String, PlayerSnapshot> agencies) {
        List<Map.Entry<String, IntelOperation>> offers = new ArrayList<>();
        agencies.forEach((id, agency) -> queuedOffers(agency).forEach(offer -> offers.add(Map.entry(id, offer))));
        offers.sort(java.util.Comparator.<Map.Entry<String, IntelOperation>>comparingInt(
                entry -> -entry.getValue().raisePercent())
            .thenComparing(Map.Entry::getKey).thenComparing(entry -> entry.getValue().id()));
        return offers;
    }
}
