package de.mkoehler.launchwindow.rules;

/**
 * <p>Marker interface for anything a rules-engine resolver reports having
 * done to a {@link de.mkoehler.launchwindow.model.PlayerSnapshot} during a
 * season-advance pass. Every resolver in {@code de.mkoehler.launchwindow.rules}
 * returns a {@code List<ResolutionEvent>} alongside mutating the snapshot in
 * place - the shared vocabulary a post-turn replay, a season news report, or
 * the Museum (see {@code design.md} &sect;2.2(c)/&sect;3.13) can eventually
 * render without each caller knowing every resolver's own event types.</p>
 *
 * <p>A pure headless-engine return value - not (yet) persisted or sent over
 * the wire, so it needs no Jackson/Kryo registration. Wiring these into
 * persistence/replay is M4 scope, not this one.</p>
 *
 * @author Mario Koehler
 */
public interface ResolutionEvent {
}
