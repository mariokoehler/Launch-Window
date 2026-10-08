package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.PersonnelFile;
import de.mkoehler.launchwindow.model.Retention;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.OfferResult;
import de.mkoehler.launchwindow.rules.OfferSettled;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.Poaching;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests what the poaching screens say ({@code ui-spec.md} &sect;4.16).
 *
 * @author Mario Koehler
 */
class PoachTextsTest {

    @Test
    void reportLinesForBothSides() {
        TurnReportView.Line matched = PoachTexts.reportLine(new OfferSettled(false, "k", "NASA", "p1", "Ivan Kozlov",
            "MISSION_CONTROL", 50, OfferResult.Outcome.MATCHED, 36, 54, 0, "DEFAULT", 50));
        assertEquals("NASA offered Ivan Kozlov +50 %: matched. Salary now 54 a season.", matched.title());
        assertEquals("Matched", matched.tag());
        TurnReportView.Line left = PoachTexts.reportLine(new OfferSettled(false, "k", "Meridian Rocket Society", "p2",
            "Rein Saar", "SET", 100, OfferResult.Outcome.JOINED, 44, 88, 0, "LET_GO", -1));
        assertEquals("Meridian Rocket Society offered Rein Saar +100 %: Saar left for Meridian Rocket Society. Set to "
            + "“Let go”, so no match was made.", left.title());
        assertEquals(-1, left.tone());
        TurnReportView.Line joined = PoachTexts.reportLine(new OfferSettled(true, "k", "NASA", "p9", "Ruth Feldman",
            "SET", 50, OfferResult.Outcome.JOINED, 38, 57, 57, "", -1));
        assertEquals("Your offer to Ruth Feldman of NASA (+50 %): joined you. Salary 57 a season; signing bonus of 57 "
            + "paid.", joined.title());
        TurnReportView.Line full = PoachTexts.reportLine(new OfferSettled(true, "k", "NASA", "p9", "Lucille Banks",
            "MISSION_CONTROL", 100, OfferResult.Outcome.LAPSED_NO_ROOM, 42, 84, 0, "", -1));
        assertEquals("Your offer to Lucille Banks of NASA (+100 %): lapsed, no room in Mission Control.", full.title());
    }

    @Test
    void careerLinesBeforeAMoveNameTheEarlierAgency() {
        List<CareerEntry> career = List.of(
            new CareerEntry(1, CareerEntry.Kind.HIRED, "", "", "", 0, 0),
            new CareerEntry(5, CareerEntry.Kind.JOINED, "NASA", "38", "", 57, 50),
            new CareerEntry(6, CareerEntry.Kind.TRAIT, "STEADY", "", "", 0, 0),
            new CareerEntry(9, CareerEntry.Kind.JOINED, "Meridian", "57", "", 72, 25));
        assertEquals("NASA", PoachTexts.earlierAgency(career, 0));
        assertEquals("Meridian", PoachTexts.earlierAgency(career, 1));
        assertEquals("Meridian", PoachTexts.earlierAgency(career, 2));
        assertNull(PoachTexts.earlierAgency(career, 3));
    }

    @Test
    void reasonsAndStandingOrders() {
        PersonnelFile file = new PersonnelFile("p5", "Kozlov", "", "SET", 34, 10, List.of(), null, "Low", null, false,
            36, 2, null);
        assertTrue(PoachTexts.blocker(Poaching.Blocker.TOO_NEW, file, "NASA").startsWith(
            "With the agency under 4 seasons: joined NASA in "));
        assertEquals("Salary 36 → 54 if accepted, and a signing bonus of 54.", PoachTexts.ifAccepted(file, 50));
        assertEquals("Offers up to +25 % are matched. A bigger one is Kozlov's to decide.",
            PoachTexts.retentionCaption(Retention.DEFAULT, 25, "Ivan Kozlov"));
        SetSpecialist kept = new SetSpecialist("p1", "Gayane Martirosyan", 41, 70, 50);
        kept.setRetention(Retention.KEEP);
        SetSpecialist gone = new SetSpecialist("p2", "Rein Saar", 41, 70, 50);
        gone.setRetention(Retention.LET_GO);
        assertEquals("2 engineers set otherwise: Martirosyan keeps at any price, Saar is let go.",
            PoachTexts.setOtherwise(List.of(kept, gone, new SetSpecialist("p3", "X Y", 30, 70, 50)), PersonnelGroup.SET));
    }
}
