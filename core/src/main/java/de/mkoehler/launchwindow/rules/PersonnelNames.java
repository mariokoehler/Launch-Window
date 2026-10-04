package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Archetype;

import java.util.List;
import java.util.SplittableRandom;

/**
 * Names for new candidates (gap G5, {@code ui-spec.md} &sect;6), from name lists written for Launch
 * Window (never taken from BASPM's data, {@code design.md} &sect;1.2), one set per archetype.
 * Surnames of well-known people of the real space programs are left out on purpose (checked against
 * the cosmonaut and astronaut rosters and the chief designers, 2026-10-04), so no candidate turns up
 * as a famous cosmonaut, astronaut or designer. Check new names the same way before adding them.
 *
 * <p>A name is drawn from its own random stream, seeded from the candidate's id and the agency, not
 * from the hiring pool's stream: one more draw there would shift every skill roll after it.</p>
 *
 * @author Mario Koehler
 */
public final class PersonnelNames {

    private static final List<String> NASA_FIRST = List.of(
        "Arthur", "Barbara", "Calvin", "Dorothy", "Earl", "Frances", "Gordon", "Harriet", "Irving", "June",
        "Kenneth", "Lorraine", "Marvin", "Nancy", "Oscar", "Phyllis", "Ralph", "Shirley", "Theodore", "Vera",
        "Walter", "Evelyn", "Howard", "Margaret", "Leonard", "Ruth", "Floyd", "Doris", "Clifford", "Helen");
    private static final List<String> NASA_LAST = List.of(
        "Abernathy", "Brennan", "Castellano", "Delgado", "Eriksen", "Fairbanks", "Gallagher", "Hartley", "Ingram",
        "Jablonski", "Kowalczyk", "Lindqvist", "Mancini", "Novak", "Okonkwo", "Pemberton", "Quigley", "Ramirez",
        "Sandoval", "Takahashi", "Underwood", "Vasquez", "Whitfield", "Yamada", "Zielinski", "Haverford", "Marsh",
        "Prescott", "Ashworth", "Osei");

    private static final List<String> SOVIET_FIRST_MALE = List.of(
        "Anatoly", "Boris", "Dmitri", "Fyodor", "Gennady", "Igor", "Kirill", "Lev", "Mikhail", "Nikolai", "Oleg",
        "Pavel", "Roman", "Sergei", "Timur", "Vadim", "Yakov", "Arkady", "Grigory", "Stanislav");
    private static final List<String> SOVIET_FIRST_FEMALE = List.of(
        "Alla", "Galina", "Irina", "Larisa", "Lyudmila", "Marina", "Nadezhda", "Olga", "Raisa", "Svetlana",
        "Tamara", "Yelena", "Zoya", "Vera", "Nina");
    /** Surnames in their masculine form; the feminine form is derived (-ov to -ova, -in to -ina, -sky to -skaya). */
    private static final List<String> SOVIET_LAST = List.of(
        "Arkhipov", "Belousov", "Danilov", "Fedorchuk", "Gorelov", "Ignatyev", "Kalinin", "Lavrentyev", "Molchanov",
        "Nesterov", "Orlov", "Pankratov", "Rudenko", "Savchenko", "Tarasov", "Ulyanin", "Vorontsov", "Zhdanov",
        "Kovalenko", "Melnikov", "Shubin", "Yermakov", "Zaitsev", "Polyansky", "Kornilov", "Barinov");

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PersonnelNames() {
    }

    /**
     * Returns a candidate's name.
     *
     * @param candidateId the candidate's id
     * @param archetype   the hiring agency's archetype
     * @param agencyName  the hiring agency's name, so two agencies' candidate number 12 differ
     * @return a full name, such as "Galina Orlova"
     */
    public static String forCandidate(String candidateId, Archetype archetype, String agencyName) {
        long seed = 0x5DEECE66DL ^ ((long) candidateId.hashCode() << 32) ^ (agencyName == null ? 0 : agencyName.hashCode())
            ^ ((long) archetype.ordinal() << 16);
        SplittableRandom random = new SplittableRandom(seed);
        if (archetype == Archetype.SOVIET) {
            boolean female = random.nextInt(4) == 0;
            String first = pick(random, female ? SOVIET_FIRST_FEMALE : SOVIET_FIRST_MALE);
            String last = pick(random, SOVIET_LAST);
            return first + " " + (female ? feminine(last) : last);
        }
        return pick(random, NASA_FIRST) + " " + pick(random, NASA_LAST);
    }

    private static String pick(SplittableRandom random, List<String> names) {
        return names.get(random.nextInt(names.size()));
    }

    /**
     * Returns a Russian surname's feminine form.
     *
     * @param masculine the masculine form
     * @return the feminine form
     */
    static String feminine(String masculine) {
        if (masculine.endsWith("sky")) {
            return masculine.substring(0, masculine.length() - 3) + "skaya";
        }
        if (masculine.endsWith("ov") || masculine.endsWith("ev") || masculine.endsWith("in")) {
            return masculine + "a";
        }
        return masculine;
    }
}
