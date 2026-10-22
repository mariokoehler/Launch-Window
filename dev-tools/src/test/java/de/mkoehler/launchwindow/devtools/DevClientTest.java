package de.mkoehler.launchwindow.devtools;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.SubmitStatementRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link DevClient}'s pure helpers -
 * {@link DevClient#main(String[])} itself is an interactive console/socket
 * loop and is exercised manually against a running server instead, matching
 * {@code ServerLauncher}'s own untested bootstrap entry point in this
 * codebase.</p>
 *
 * @author Mario Koehler
 */
class DevClientTest {

    /**
     * Verifies that a plain whole number parses correctly.
     */
    @Test
    void parseFundsAmountParsesAWholeNumber() {
        assertEquals(5_000L, DevClient.parseFundsAmount("5000"));
    }

    /**
     * Verifies that surrounding whitespace is tolerated.
     */
    @Test
    void parseFundsAmountTrimsWhitespace() {
        assertEquals(42L, DevClient.parseFundsAmount("  42  "));
    }

    /**
     * Verifies the react command: a stance in any case with the rest of the line as the quote, a withdrawal,
     * and the usage for anything else.
     */
    @Test
    void parseReactReadsAStanceAQuoteAndAWithdrawal() {
        assertEquals(new SubmitStatementRequest(7, "6-lunar-probe", FrontPage.Stance.NO_COMMENT, ""),
            DevClient.parseReact(7, "react 6-lunar-probe no-comment"));
        assertEquals(new SubmitStatementRequest(7, "6-lunar-probe", FrontPage.Stance.POLITE, "Well  flown, all."),
            DevClient.parseReact(7, "  react 6-lunar-probe Polite Well  flown, all."));
        assertEquals(new SubmitStatementRequest(7, "6-lunar-probe", null, ""),
            DevClient.parseReact(7, "react 6-lunar-probe withdraw"));
        assertThrows(IllegalArgumentException.class, () -> DevClient.parseReact(7, "react 6-lunar-probe"));
        assertThrows(IllegalArgumentException.class, () -> DevClient.parseReact(7, "react 6-lunar-probe smug"));
    }

    /**
     * Verifies that a non-numeric argument is rejected.
     */
    @Test
    void parseFundsAmountRejectsNonNumericInput() {
        assertThrows(NumberFormatException.class, () -> DevClient.parseFundsAmount("not-a-number"));
    }

    /**
     * Verifies that the formatted summary includes every field a player
     * would want to see, including roster sizes.
     */
    @Test
    void formatSnapshotIncludesEveryField() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Hanseatic Space Agency", Archetype.SOVIET, 4_000, 10, 500, 2);

        String formatted = DevClient.formatSnapshot(snapshot);

        assertTrue(formatted.contains("Hanseatic Space Agency"));
        assertTrue(formatted.contains("SOVIET"));
        assertTrue(formatted.contains("4000"));
        assertTrue(formatted.contains("10"));
        assertTrue(formatted.contains("500"));
        assertTrue(formatted.contains("astronauts: 0"));
        assertTrue(formatted.contains("SET: 0"));
        assertTrue(formatted.contains("mission control: 0"));
    }
}
