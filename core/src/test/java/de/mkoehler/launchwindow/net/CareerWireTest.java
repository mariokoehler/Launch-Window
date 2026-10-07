package de.mkoehler.launchwindow.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.rules.AmbitionSettled;
import de.mkoehler.launchwindow.rules.Careers;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.TurnReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sends phase 4's data where it travels ({@code design.md} &sect;2.2(d)): an astronaut with an
 * ambition and a career, and a memorial entry with its person page, through the state fetch; and an
 * ambition's report line through a saved turn report.
 *
 * @author Mario Koehler
 */
class CareerWireTest {

    @Test
    void careersAndTheMemorialsPageSurviveTheStateFetch() {
        PlayerSnapshot state = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 1, 2, 3, 9);
        Astronaut astronaut = new Astronaut("p1", "Ana Reyes", 30, 70, 50);
        astronaut.setAmbition(new Ambition(Ambition.Kind.FLY_TO_ORBIT, "", 4, 6, 7));
        astronaut.setLoyalty(66);
        astronaut.getCareer().add(new CareerEntry(3, CareerEntry.Kind.HIRED, "", "calm", "", 0, 0));
        state.getAstronauts().add(astronaut);
        Astronaut lost = new Astronaut("p2", "Lost Pilot", 33, 70, 50);
        lost.setTraits(List.of(new TraitState(Trait.GLORY_SEEKER, 0, true)));
        lost.getCareer().add(new CareerEntry(2, CareerEntry.Kind.HIRED, "", "sociable", "", 0, 0));
        state.getMemorial().add(Careers.memorial(lost, 8, MemorialEntry.Cause.MISSION, "vostok-orbital", "retrofire")
            .withCareer(new CareerEntry(8, CareerEntry.Kind.FLIGHT, "vostok-orbital", "CREW", "retrofire", 0, 0)));

        PlayerSnapshot copy = FetchStateResponse.success(state).state();

        Astronaut sent = copy.getAstronauts().getFirst();
        assertEquals(astronaut.getAmbition(), sent.getAmbition());
        assertEquals(66, sent.getLoyalty());
        assertEquals(astronaut.getCareer(), sent.getCareer());
        MemorialEntry entry = copy.getMemorial().getFirst();
        assertEquals("p2", entry.personnelId());
        assertEquals(List.of(Trait.GLORY_SEEKER), entry.traits());
        assertEquals(2, entry.career().size());
        assertEquals(state.getMemorial().getFirst().skills(), entry.skills());
    }

    @Test
    void anAmbitionsReportLineSurvivesTheReportStore() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TurnReport report = new TurnReport(TurnReport.VERSION, 9, 0, 0, 0, 0, List.of(), List.of(), List.of(
            new AmbitionSettled(PersonnelGroup.SET, "p3", "Engineer", Ambition.Kind.COMPONENT_CAP, "vostok-capsule",
                AmbitionSettled.Outcome.PASSED_OVER, 3, -5, -5)));
        TurnReport copy = mapper.readValue(mapper.writeValueAsString(report), TurnReport.class);
        assertEquals(report.events(), copy.events());
    }
}
