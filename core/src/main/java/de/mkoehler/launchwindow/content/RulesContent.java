package de.mkoehler.launchwindow.content;

import java.util.Objects;

/**
 * A pack's authored rules content beyond the program tree: the R&amp;D decisions, the PR press stories
 * and the commercial clients. Each is one of the pack's hashed, frozen files
 * ({@link ContentPackLoader#FILES}), so a server update never changes them under a running game
 * (review.md 2.2); the rules read them through {@link ContentCatalog#rules()}.
 *
 * @param decisions the R&amp;D decisions
 * @param press     the PR press stories
 * @param clients   the commercial clients
 * @author Mario Koehler
 */
public record RulesContent(Decisions decisions, PrPress press, CommercialClients clients) {

    /** None of them: what a catalog built in code holds unless given some. */
    public static final RulesContent EMPTY = new RulesContent(Decisions.EMPTY, PrPress.EMPTY, CommercialClients.EMPTY);

    /**
     * Checks the parts.
     *
     * @throws NullPointerException if a part is {@code null}
     */
    public RulesContent {
        Objects.requireNonNull(decisions, "decisions must not be null");
        Objects.requireNonNull(press, "press must not be null");
        Objects.requireNonNull(clients, "clients must not be null");
    }

    /**
     * Returns this content with other decisions.
     *
     * @param replacement the decisions
     * @return the content
     */
    public RulesContent withDecisions(Decisions replacement) {
        return new RulesContent(replacement, press, clients);
    }

    /**
     * Returns this content with other press stories.
     *
     * @param replacement the stories
     * @return the content
     */
    public RulesContent withPress(PrPress replacement) {
        return new RulesContent(decisions, replacement, clients);
    }

    /**
     * Returns this content with other commercial clients.
     *
     * @param replacement the clients
     * @return the content
     */
    public RulesContent withClients(CommercialClients replacement) {
        return new RulesContent(decisions, press, replacement);
    }
}
