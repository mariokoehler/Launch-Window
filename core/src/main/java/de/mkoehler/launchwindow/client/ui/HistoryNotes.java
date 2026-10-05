package de.mkoehler.launchwindow.client.ui;

/**
 * The texts of the history notes that belong to the client's own screens, rather than to a
 * content pack ({@code ui-spec.md} component 15). Notes on programs, configurations and
 * components are in the pack's display text instead ({@code ContentStrings}, gap G4). Written in
 * our own words from real-world facts, never from BASPM's historical text ({@code design.md} &sect;1.2).
 *
 * @author Mario Koehler
 */
public final class HistoryNotes {

    /** On the Join Game screen's Director field: the first note a player sees. */
    public static final String DIRECTOR = "Both agencies here have a Director, but in reality the two jobs were very "
        + "different. NASA’s Administrator was a public figure, appointed by the President, confirmed by the "
        + "Senate, and questioned by Congress. The man who drove the Soviet program, Sergei Korolev, was a state "
        + "secret. Until his death in 1966, the world knew him only as “the Chief Designer.”";

    /**
     * Not instantiable; this class only holds texts.
     */
    private HistoryNotes() {
    }
}
