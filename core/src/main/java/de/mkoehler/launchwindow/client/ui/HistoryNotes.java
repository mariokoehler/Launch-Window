package de.mkoehler.launchwindow.client.ui;

/**
 * The texts of the history notes that belong to the client's own screens, rather than to a
 * content pack ({@code ui-spec.md} component 15; notes on programs and components will come
 * with the pack's display text, gap G4). Written in our own words from real-world facts, never
 * from BASPM's historical text ({@code design.md} &sect;1.2).
 *
 * @author Mario Koehler
 */
public final class HistoryNotes {

    /** On the Join Game screen's Director field: the first note a player sees. */
    public static final String DIRECTOR = "Both agencies here have a Director, but in reality the two jobs were very "
        + "different. NASA’s Administrator was a public figure, appointed by the President, confirmed by the "
        + "Senate, and questioned by Congress. The man who drove the Soviet program, Sergei Korolev, was a state "
        + "secret. Until his death in 1966, the world knew him only as “the Chief Designer.”";

    /** On Program details for Vostok (the Headquarters mockup's text). */
    public static final String VOSTOK = "On 12 April 1961, Vostok 1 carried Yuri Gagarin once around the Earth in 108 "
        + "minutes. He did not land in the capsule: he ejected at about 7 km and came down under his own parachute. "
        + "Soviet officials kept that quiet for years, because the rules for an official flight record required the "
        + "pilot to land with his craft.";

    private static final java.util.Map<String, String> PROGRAMS = java.util.Map.of("vostok", VOSTOK);

    /**
     * Not instantiable; this class only holds texts.
     */
    private HistoryNotes() {
    }

    /**
     * Returns a program's history note, until the content pack carries its own (gap G4).
     *
     * @param programId the program's id
     * @return the note, or {@code null} if it has none
     */
    public static String forProgram(String programId) {
        return PROGRAMS.get(programId);
    }
}
