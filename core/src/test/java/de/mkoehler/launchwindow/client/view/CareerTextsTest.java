package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.AmbitionSettled;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the words for careers ({@code design.md} &sect;2.2(d), phase 4): ambitions, statuses, report
 * lines and career lines, with no pronouns for employees anywhere.
 *
 * @author Mario Koehler
 */
class CareerTextsTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final Pattern PRONOUN = Pattern.compile("\\b(he|she|his|her|him|hers|they|their|them)\\b");

    private static void noPronouns(String text) {
        assertFalse(PRONOUN.matcher(text.toLowerCase(Locale.ROOT)).find(), text);
    }

    @Test
    void everyAmbitionAndOutcomeReadsWithoutPronouns() {
        for (Ambition.Kind kind : Ambition.Kind.values()) {
            noPronouns(CareerTexts.ambition(kind, "vostok-capsule"));
            noPronouns(CareerTexts.ambitionHow(kind));
            for (AmbitionSettled.Outcome outcome : AmbitionSettled.Outcome.values()) {
                String[] lines = CareerTexts.report(new AmbitionSettled(PersonnelGroup.SET, "p1", "Ana Reyes", kind,
                    "vostok-capsule", outcome, 3, -5, -5));
                noPronouns(lines[0]);
                noPronouns(lines[1]);
                noPronouns(CareerTexts.line(new CareerEntry(5, CareerEntry.Kind.AMBITION, kind.name(), "vostok-capsule",
                    outcome.name(), 0, 3), CATALOG));
            }
        }
        for (CareerEntry.Kind kind : CareerEntry.Kind.values()) {
            if (kind != CareerEntry.Kind.AMBITION) {
                noPronouns(CareerTexts.line(new CareerEntry(5, kind, kind == CareerEntry.Kind.TRAIT ? "MENTOR"
                    : kind == CareerEntry.Kind.FLIGHT ? "vostok-orbital" : "ROCKETS", kind == CareerEntry.Kind.FLIGHT
                    ? "CREW" : "ADVANCED", "", 1, 2), CATALOG));
            }
        }
    }

    @Test
    void theStatusSaysHowLongOrThatTheyreDisappointed() {
        SetSpecialist engineer = new SetSpecialist("p1", "E", 30, 60, 50);
        assertNull(CareerTexts.status(engineer));
        engineer.setAmbition(new Ambition(Ambition.Kind.TOP_ENGINEER, "", 1, 6, 3));
        assertEquals("1 season left", CareerTexts.status(engineer));
        engineer.setAmbition(null);
        engineer.setDisappointedSeasons(3);
        assertEquals("Disappointed · 3 seasons", CareerTexts.status(engineer));
    }

    @Test
    void careerLinesNameTheirSubject() {
        assertEquals("Turned out to be a natural mentor", CareerTexts.line(new CareerEntry(3, CareerEntry.Kind.TRAIT,
            "MENTOR", "", "", 0, 0), CATALOG));
        for (de.mkoehler.launchwindow.model.Trait trait : de.mkoehler.launchwindow.model.Trait.values()) {
            String line = CareerTexts.line(new CareerEntry(3, CareerEntry.Kind.TRAIT, trait.name(), "", "", 0, 0),
                CATALOG);
            assertEquals("Turned out to be " + TraitTexts.inSentence(trait), line);
            assertFalse(line.matches(".* an? (steady|ambitious|restless|abrasive|easy-going|cool|must).*"), line);
        }
        assertEquals("Hired. A former colleague: “driven”.", CareerTexts.line(new CareerEntry(1, CareerEntry.Kind.HIRED,
            "", "driven", "", 0, 0), CATALOG));
        assertEquals("Ambition came true: become an Expert", CareerTexts.line(new CareerEntry(9,
            CareerEntry.Kind.AMBITION, "BECOME_EXPERT", "", "FULFILLED", 0, 0), CATALOG));
    }
}
