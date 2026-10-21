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

    /** On the PR duty dialog, for a NASA-style agency. */
    public static final String PR_NASA = "In 1959 the Mercury Seven sold the exclusive rights to their personal "
        + "stories to Life magazine for $500,000, shared among the seven. Life put them and their families on its "
        + "covers, and their tours, parades and speeches helped keep the public and Congress behind the space "
        + "program.";

    /** On the PR duty dialog, for a Soviet-style agency. */
    public static final String PR_SOVIET = "After his flight, Yuri Gagarin spent much of the next two years "
        + "travelling the world as the face of Soviet achievement, visiting more than thirty countries. After the "
        + "fatal Soyuz 1 flight in 1967 he was barred from spaceflight as too valuable to lose. He never flew in "
        + "space again, and died in a jet training flight in 1968.";

    /** On mission assembly's Mission control card, when the mission has a CAPCOM (checked 2026-10-08). */
    public static final String CAPCOM = "CAPCOM stands for capsule communicator, a name from Project Mercury, when "
        + "spacecraft were called capsules. During Mercury, CAPCOMs at tracking stations around the world, some of "
        + "them astronauts, talked to each flight as it passed overhead. Later a single CAPCOM in Mission Control "
        + "spoke to the crew for everyone, usually a fellow astronaut, who knew best what the crew was facing. On "
        + "Apollo 11 it was Charlie Duke, so relieved at the landing that he stumbled over the name Tranquility.";

    /** On the replay console's air-to-ground label, beside the radio calls' beeps (user's text, checked 2026-10-18). */
    public static final String QUINDAR = "Those iconic beeps in Apollo mission recordings weren't sound effects. Named "
        + "after a broadcast equipment manufacturer, Quindar tones remotely pressed and released the equivalent of a "
        + "walkie-talkie's transmit button, switching ground-station radio transmitters on and off via ordinary "
        + "telephone lines. A tiny piece of engineering that helped humanity reach the Moon.";

    /** On the Intelligence Office's offer dialog, beside its title (checked 2026-10-08 and 2026-10-21). */
    public static final String POACHING = "Both sides began with poached engineers. After the war about 120 of "
        + "Wernher von Braun's Peenemünde team came to Texas under Operation Paperclip. Before dawn on 22 October "
        + "1946, in Operation Osoaviakhim, the Soviets put German specialists and their families on trains to the "
        + "USSR without warning. The core of the rocket group ended up on an island in Lake Seliger, where they "
        + "helped build the R-1, a copy of the V-2.";

    /**
     * Not instantiable; this class only holds texts.
     */
    private HistoryNotes() {
    }
}
