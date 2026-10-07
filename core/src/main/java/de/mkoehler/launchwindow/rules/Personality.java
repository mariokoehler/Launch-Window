package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.stream.Stream;

/**
 * Employees' personalities ({@code design.md} &sect;2.2(d)): the traits a candidate is born with,
 * the hint a former colleague gives about them, how two people's rapport tends to drift, and the
 * copy of the player's state that's safe to send to the client.
 *
 * <p><b>Separate random streams.</b> Traits are drawn from a stream seeded by the agency and the
 * candidate's id, and a pair's affinity from one seeded by the agency and both ids, never from the
 * season's shared random source, so adding personalities shifts no other draw (seeded tests, the
 * bot playouts and the screenshots stay as they were).</p>
 *
 * <p>Every number is our own first-pass default, accepted for play testing (user, 2026-10-07).</p>
 *
 * @author Mario Koehler
 */
public final class Personality {

    /** The chance of a candidate having no trait, one, or two, in percent. */
    public static final int[] TRAIT_COUNT_PERCENT = {20, 50, 30};

    /** The fewest work seasons before a trait shows. */
    public static final int REVEAL_MIN_SEASONS = 2;

    /** The most work seasons before a trait shows; after this, there's nothing more to discover. */
    public static final int REVEAL_MAX_SEASONS = 4;

    /** The chance, in percent, that a candidate with traits still gets a bland hint. */
    public static final int BLAND_HINT_PERCENT = 25;

    /** A pair's rapport is shown to the player once it passes this, either way. */
    public static final double RAPPORT_SHOWN_AT = 30;

    /** How far a pair's rapport moves per shared work season, times their affinity. */
    public static final double RAPPORT_DRIFT = 5;

    /** The bounds of a pair's rapport. */
    public static final double RAPPORT_MAX = 100;

    /** What each Easy-going member adds to a pair's affinity. */
    public static final double AFFINITY_EASY_GOING = 0.5;

    /** What each Abrasive member adds to a pair's affinity. */
    public static final double AFFINITY_ABRASIVE = -0.6;

    /** What each Team player adds to a pair's affinity. */
    public static final double AFFINITY_TEAM_PLAYER = 0.2;

    /** What each Loner adds to a pair's affinity. */
    public static final double AFFINITY_LONER = -0.2;

    /** What two people who both must be the best add to their affinity. */
    public static final double AFFINITY_TWO_WHO_MUST_BE_BEST = -0.4;

    /** The hints that fit no trait in particular. */
    public static final List<String> BLAND_HINTS = List.of("reliable", "pleasant", "hard-working");

    /** Every telling hint and the traits it fits; each fits several, so no hint gives a trait away. */
    public static final Map<String, List<Trait>> HINTS = hints();

    private Personality() {
    }

    private static Map<String, List<Trait>> hints() {
        Map<String, List<Trait>> hints = new LinkedHashMap<>();
        hints.put("driven", List.of(Trait.WORKAHOLIC, Trait.AMBITIOUS, Trait.MUST_BE_THE_BEST));
        hints.put("calm", List.of(Trait.STEADY, Trait.COOL_UNDER_PRESSURE, Trait.LONER));
        hints.put("sociable", List.of(Trait.EASY_GOING, Trait.TEAM_PLAYER, Trait.GLORY_SEEKER));
        hints.put("exacting", List.of(Trait.PERFECTIONIST, Trait.ABRASIVE, Trait.MUST_BE_THE_BEST));
        hints.put("independent", List.of(Trait.LONER, Trait.RESTLESS, Trait.ABRASIVE));
        hints.put("keen-to-learn", List.of(Trait.QUICK_STUDY, Trait.AMBITIOUS, Trait.MENTOR));
        hints.put("patient", List.of(Trait.MENTOR, Trait.STEADY, Trait.PERFECTIONIST));
        return Collections.unmodifiableMap(hints);
    }

    /**
     * Gives a fresh candidate their traits and their hint, drawn from the candidate's own stream.
     *
     * @param snapshot  the hiring agency
     * @param candidate the candidate, with their id set
     * @param group     the candidate's group
     */
    public static void assign(PlayerSnapshot snapshot, Personnel candidate, PersonnelGroup group) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        SplittableRandom random = new SplittableRandom(seed(snapshot.getAgencyName(), candidate.getId()));
        List<Trait> pool = new ArrayList<>(Trait.forGroup(group.name()));
        int roll = random.nextInt(100);
        int count = roll < TRAIT_COUNT_PERCENT[0] ? 0 : roll < TRAIT_COUNT_PERCENT[0] + TRAIT_COUNT_PERCENT[1] ? 1 : 2;
        List<TraitState> traits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Trait trait = pool.remove(random.nextInt(pool.size()));
            traits.add(new TraitState(trait, random.nextInt(REVEAL_MIN_SEASONS, REVEAL_MAX_SEASONS + 1), false));
        }
        candidate.setTraits(traits);
        candidate.setHint(hintFor(traits, random));
    }

    private static String hintFor(List<TraitState> traits, SplittableRandom random) {
        if (traits.isEmpty() || random.nextInt(100) < BLAND_HINT_PERCENT) {
            return BLAND_HINTS.get(random.nextInt(BLAND_HINTS.size()));
        }
        Trait trait = traits.get(random.nextInt(traits.size())).trait();
        List<String> fitting = HINTS.entrySet().stream().filter(entry -> entry.getValue().contains(trait))
            .map(Map.Entry::getKey).toList();
        return fitting.get(random.nextInt(fitting.size()));
    }

    /**
     * Returns the affinity a pair gets when they first share a team: a draw from -1 to +1 from the
     * pair's own stream, pushed by their traits, clamped to -1..+1.
     *
     * @param snapshot the agency
     * @param a        one of them
     * @param b        the other
     * @return the affinity
     */
    public static double affinity(PlayerSnapshot snapshot, Personnel a, Personnel b) {
        String first = a.getId().compareTo(b.getId()) <= 0 ? a.getId() : b.getId();
        String second = first.equals(a.getId()) ? b.getId() : a.getId();
        SplittableRandom random = new SplittableRandom(seed(snapshot.getAgencyName(), first + "|" + second));
        double value = random.nextDouble(-1.0, 1.0);
        for (Personnel person : List.of(a, b)) {
            if (has(person, Trait.EASY_GOING)) {
                value += AFFINITY_EASY_GOING;
            }
            if (has(person, Trait.ABRASIVE)) {
                value += AFFINITY_ABRASIVE;
            }
            if (has(person, Trait.TEAM_PLAYER)) {
                value += AFFINITY_TEAM_PLAYER;
            }
            if (has(person, Trait.LONER)) {
                value += AFFINITY_LONER;
            }
        }
        if (has(a, Trait.MUST_BE_THE_BEST) && has(b, Trait.MUST_BE_THE_BEST)) {
            value += AFFINITY_TWO_WHO_MUST_BE_BEST;
        }
        return Math.max(-1.0, Math.min(1.0, value));
    }

    /**
     * Returns whether someone has a trait, shown or not. The rules use the real traits; only the
     * client's previews are limited to the revealed ones.
     *
     * @param person the person
     * @param trait  the trait
     * @return whether they have it
     */
    public static boolean has(Personnel person, Trait trait) {
        return person.getTraits().stream().anyMatch(state -> state.trait() == trait);
    }

    /**
     * Returns whether the player knows someone has a trait.
     *
     * @param person the person
     * @param trait  the trait
     * @return whether it's revealed
     */
    public static boolean known(Personnel person, Trait trait) {
        return person.getTraits().stream().anyMatch(state -> state.trait() == trait && state.revealed());
    }

    /**
     * Returns whether there's nothing more to learn about someone: every trait they could have
     * would have shown by now.
     *
     * @param person the person
     * @return whether they're fully known
     */
    public static boolean fullyKnown(Personnel person) {
        return person.getWorkSeasons() >= REVEAL_MAX_SEASONS;
    }

    /**
     * Returns the pair record for two people, if they've ever shared a team.
     *
     * @param snapshot the agency
     * @param a        one id
     * @param b        the other
     * @return the pair, or {@code null}
     */
    public static Rapport rapport(PlayerSnapshot snapshot, String a, String b) {
        for (Rapport pair : snapshot.getRapports()) {
            if (pair.isPair(a, b)) {
                return pair;
            }
        }
        return null;
    }

    /**
     * Returns a copy of the player's state that's safe to send to the client: hidden traits,
     * pairs' affinities, rapport the player hasn't seen yet and candidates' traits are removed
     * ({@code design.md} &sect;2.2(d)), so they can't be read out of the client.
     *
     * @param snapshot the player's real state
     * @param mapper   the mapper the state is saved with, used for the copy
     * @return the copy
     */
    public static PlayerSnapshot forClient(PlayerSnapshot snapshot, ObjectMapper mapper) {
        PlayerSnapshot copy;
        try {
            copy = mapper.readValue(mapper.writeValueAsBytes(snapshot), PlayerSnapshot.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not copy the player's state", e);
        }
        Stream.of(copy.getAstronauts(), copy.getSetSpecialists(), copy.getMissionControlOfficers())
            .flatMap(List::stream)
            .forEach(person -> person.setTraits(person.getTraits().stream().filter(TraitState::revealed)
                .map(state -> new TraitState(state.trait(), 0, true)).toList()));
        Stream.of(copy.getAstronautCandidates(), copy.getSetSpecialistCandidates(), copy.getMissionControlCandidates())
            .flatMap(List::stream)
            .forEach(candidate -> candidate.setTraits(candidate.getTraits().stream().filter(TraitState::revealed)
                .map(state -> new TraitState(state.trait(), 0, true)).toList()));
        copy.setRapports(copy.getRapports().stream().filter(Rapport::shown)
            .map(pair -> new Rapport(pair.first(), pair.second(), pair.rapport(), 0, pair.seasonsTogether(), true))
            .toList());
        return copy;
    }

    private static long seed(String agency, String key) {
        long high = Objects.hashCode(agency);
        long low = Objects.hashCode(key);
        return high << 32 ^ (low & 0xffffffffL) ^ 0x5DEECE66DL;
    }
}
