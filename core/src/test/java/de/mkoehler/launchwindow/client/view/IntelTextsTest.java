package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.rules.IntelFinding;
import de.mkoehler.launchwindow.rules.IntelligenceFound;
import de.mkoehler.launchwindow.rules.IntelligenceLevelChanged;
import de.mkoehler.launchwindow.rules.TurnReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Intelligence Office's words ({@code ui-spec.md} &sect;4.15): the footer chip, the
 * level's way to its target, the odds against a rival, and the turn report's section.
 *
 * @author Mario Koehler
 */
class IntelTextsTest {

    @Test
    void theFooterShowsTheLevelTheTargetAndTheActions() {
        PlayerSnapshot state = new PlayerSnapshot("A", Archetype.NASA, 0, 0, 0, 1);
        assertEquals("L0", IntelTexts.footer(state));
        state.getIntelligence().setTargetLevel(1);
        assertEquals("L0 → 1", IntelTexts.footer(state));
        state.getIntelligence().setLevel(2);
        state.getIntelligence().setTargetLevel(3);
        state.getIntelligence().setActionsUsed(1);
        assertEquals("L2 → 3 · 1 / 2", IntelTexts.footer(state));
        assertEquals("Reaches level 3 in 1 season", IntelTexts.reaches(state));
        state.getIntelligence().setTargetLevel(2);
        assertNull(IntelTexts.reaches(state));
        assertNull(IntelTexts.onTheWay(state));
    }

    @Test
    void oddsAreExactOnlyOnceTheRivalsLevelIsKnown() {
        assertEquals("40–85 %", IntelTexts.odds(null));
        RivalFolder folder = new RivalFolder("Sever");
        assertEquals("40–85 %", IntelTexts.odds(folder));
        folder.learnLevel(1, 30, RivalFolder.FROM_A_CATCH);
        assertEquals("70 %", IntelTexts.odds(folder));
        assertEquals("6 %", IntelTexts.caughtOdds(folder));
        assertTrue(IntelTexts.oddsSource(folder).endsWith("learned when you caught their agent"));
    }

    @Test
    void theReportListsLevelChangesThenFindings() {
        IntelFinding caught = new IntelFinding(IntelFinding.Kind.CAUGHT, "op1", IntelOperation.Kind.ASSESSMENT, "k",
            "Sever", IntelArea.RND, null, null, null, 2);
        TurnReport report = new TurnReport(TurnReport.VERSION, 5, 0, 0, 0, 0, List.of(), List.of(),
            List.of(new IntelligenceFound(caught), new IntelligenceLevelChanged(1, 0, 2, true)));
        List<TurnReportView.Line> lines = TurnReportView.intelligence(report);
        assertEquals(2, lines.size());
        assertEquals(-1, lines.get(0).tone(), "a level lost to debt is bad news");
        assertEquals("Sever · R&D", lines.get(1).title());
        assertTrue(lines.get(1).text().contains("level 2"));
        assertTrue(TurnReportView.intelligence(new TurnReport(TurnReport.VERSION, 5, 0, 0, 0, 0, List.of(), List.of(),
            List.of())).isEmpty());
    }
}
