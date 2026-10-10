package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.rules.PortraitCatalog.Portrait;
import de.mkoehler.launchwindow.rules.PortraitCatalog.Sex;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Gives a new candidate a portrait and a name and starting age that match it (gap G5,
 * {@code ui-spec.md} &sect;6). The portrait is picked first, from {@link PortraitCatalog}, for the
 * agency and the candidate's personnel group; the age is drawn from the portrait's age range, and the
 * name from the name lists for the portrait's ethnicity and sex, so a Korean-American woman's portrait
 * never gets "Peter Smith".
 *
 * <p>The name lists are in the classpath resource {@code personnel/names.json}, one entry per ethnicity
 * key, written for Launch Window (never taken from BASPM's data, {@code design.md} &sect;1.2). A surname
 * with a feminine form is written {@code masculine/feminine} ({@code Ozerov/Ozerova},
 * {@code Kazlauskas/Kazlauskienė}); one without is written once and used for both. Surnames of
 * well-known people of the real space programs are left out on purpose (checked against the cosmonaut
 * and astronaut rosters, the chief designers and flight directors, 2026-10-04 and 2026-10-05), and so are
 * the surnames of famous political figures, so no candidate turns up as a famous person. Since first
 * names and surnames are drawn independently, a famous full name can appear even when neither half is
 * famous on its own (Booker + Washington); {@code PersonnelNamesTest.noFamousFullName} lists the ones
 * found so far. Check new names both ways before adding them.</p>
 *
 * <p>Everything is drawn from the candidate's own random stream, seeded from the candidate's id and the
 * agency, not from the hiring pool's stream, in a fixed order: portrait, age, first name, surname.</p>
 *
 * @author Mario Koehler
 */
public final class PersonnelNames {

    /** The classpath resource the name lists are read from. */
    public static final String RESOURCE = "personnel/names.json";

    /**
     * How often a name that's already taken is drawn again before one is kept anyway. The lists are long
     * enough that this only runs out when every name in an ethnicity's lists is in use.
     */
    public static final int NAME_DRAWS = 50;

    private static final Map<String, NameLists> NAMES = load();

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PersonnelNames() {
    }

    /**
     * Returns a new candidate's portrait, name and starting age.
     *
     * <p>The portrait avoids, in this order of importance, those in {@code taken} (the current roster,
     * the memorial, and the batch being generated) and those in {@code recent} (the pool being replaced,
     * so the job market looks fresh). When every portrait for the group is avoided, {@code recent} is
     * given up first, then {@code taken}, so a long game reuses faces rather than failing.</p>
     *
     * @param candidateId the candidate's id
     * @param archetype   the hiring agency's archetype
     * @param agencyName  the hiring agency's name, so two agencies' candidate number 12 differ
     * @param group       the candidate's personnel group
     * @param taken       portrait ids to avoid if at all possible
     * @param recent      portrait ids to avoid if there are enough others
     * @return the candidate's identity
     * @throws IllegalStateException if the catalog has no portrait for the agency and group
     */
    public static Identity forCandidate(String candidateId, Archetype archetype, String agencyName,
            PersonnelGroup group, Set<String> taken, Set<String> recent) {
        return forCandidate(candidateId, archetype, agencyName, group, taken, recent, Set.of());
    }

    /**
     * Returns a new candidate's portrait, name and starting age, with a name nobody in {@code takenNames}
     * has (the player's whole roster, the memorial and the batch so far), so an agency never has two
     * Joseph Sobczaks. A taken name is drawn again from the same stream, up to {@link #NAME_DRAWS} times,
     * so a candidate whose first name is free gets exactly the name it always would.
     *
     * @param candidateId the candidate's id
     * @param archetype   the hiring agency's archetype
     * @param agencyName  the hiring agency's name, so two agencies' candidate number 12 differ
     * @param group       the candidate's personnel group
     * @param taken       portrait ids to avoid if at all possible
     * @param recent      portrait ids to avoid if there are enough others
     * @param takenNames  full names to avoid
     * @return the candidate's identity
     * @throws IllegalStateException if the catalog has no portrait for the agency and group
     */
    public static Identity forCandidate(String candidateId, Archetype archetype, String agencyName,
            PersonnelGroup group, Set<String> taken, Set<String> recent, Set<String> takenNames) {
        Objects.requireNonNull(candidateId, "candidateId must not be null");
        Objects.requireNonNull(archetype, "archetype must not be null");
        Objects.requireNonNull(group, "group must not be null");
        long seed = 0x5DEECE66DL ^ ((long) candidateId.hashCode() << 32) ^ (agencyName == null ? 0 : agencyName.hashCode())
            ^ ((long) archetype.ordinal() << 16);
        SplittableRandom random = new SplittableRandom(seed);

        List<Portrait> all = PortraitCatalog.of(archetype, group);
        if (all.isEmpty()) {
            throw new IllegalStateException("no portrait for " + archetype + " " + group);
        }
        List<Portrait> choices = without(all, taken, recent);
        if (choices.isEmpty()) {
            choices = without(all, taken, Set.of());
        }
        if (choices.isEmpty()) {
            choices = all;
        }
        Portrait portrait = choices.get(random.nextInt(choices.size()));
        int age = portrait.minAge() + random.nextInt(portrait.maxAge() - portrait.minAge() + 1);
        NameLists names = namesFor(portrait.ethnicity());
        String name = null;
        for (int draw = 0; draw < NAME_DRAWS && (name == null || takenNames.contains(name)); draw++) {
            String first = pick(random, portrait.sex() == Sex.FEMALE ? names.female() : names.male());
            name = first + " " + surname(pick(random, names.surnames()), portrait.sex());
        }
        return new Identity(portrait.id(), name, age);
    }

    /**
     * Returns the name lists for one ethnicity key.
     *
     * @param ethnicity the key, as in {@code personnel/portraits.json}
     * @return the lists
     * @throws IllegalStateException if {@code personnel/names.json} has no lists for the key
     */
    public static NameLists namesFor(String ethnicity) {
        NameLists names = NAMES.get(ethnicity);
        if (names == null) {
            throw new IllegalStateException("no names for ethnicity " + ethnicity);
        }
        return names;
    }

    /**
     * Returns every ethnicity key's name lists.
     *
     * @return the lists, by key
     */
    public static Map<String, NameLists> all() {
        return NAMES;
    }

    /**
     * Returns a surname entry's form for a sex: {@code Ozerov/Ozerova} gives Ozerova for a woman, a
     * one-form entry such as {@code Beridze} gives itself for both.
     *
     * @param entry the surname entry from {@code personnel/names.json}
     * @param sex   the person's sex
     * @return the surname
     */
    static String surname(String entry, Sex sex) {
        int slash = entry.indexOf('/');
        if (slash < 0) {
            return entry;
        }
        return sex == Sex.FEMALE ? entry.substring(slash + 1) : entry.substring(0, slash);
    }

    private static List<Portrait> without(List<Portrait> portraits, Set<String> taken, Set<String> recent) {
        List<Portrait> result = new ArrayList<>();
        for (Portrait portrait : portraits) {
            if (!taken.contains(portrait.id()) && !recent.contains(portrait.id())) {
                result.add(portrait);
            }
        }
        return result;
    }

    private static String pick(SplittableRandom random, List<String> names) {
        return names.get(random.nextInt(names.size()));
    }

    private static Map<String, NameLists> load() {
        try (InputStream in = PersonnelNames.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("no classpath resource " + RESOURCE);
            }
            return Map.copyOf(new ObjectMapper().readValue(in, new TypeReference<Map<String, NameLists>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + RESOURCE, e);
        }
    }

    /**
     * A candidate's portrait, name and starting age, chosen together.
     *
     * @param portraitId the portrait id
     * @param name       the full name, such as "Galina Ozerova"
     * @param age        the starting age, inside the portrait's age range
     */
    public record Identity(String portraitId, String name, int age) {
    }

    /**
     * The name lists for one ethnicity.
     *
     * @param male     men's first names
     * @param female   women's first names
     * @param surnames surnames, a gendered one written {@code masculine/feminine}
     */
    public record NameLists(List<String> male, List<String> female, List<String> surnames) {

        /**
         * Checks that no list is missing or empty.
         */
        public NameLists {
            if (male == null || male.isEmpty() || female == null || female.isEmpty() || surnames == null
                    || surnames.isEmpty()) {
                throw new IllegalArgumentException("every name list must be present and non-empty");
            }
            male = List.copyOf(male);
            female = List.copyOf(female);
            surnames = List.copyOf(surnames);
        }
    }
}
