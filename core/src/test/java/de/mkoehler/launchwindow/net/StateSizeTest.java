package de.mkoehler.launchwindow.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TraitState;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.rules.Careers;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.Personality;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a big agency's state fits Kryo's 64 KB object buffer with room to spare: every roster
 * at its building's top cap, all three candidate pools, a shown rapport for many pairs, every career
 * record full (phase 4), and the intelligence folders of {@link IntelligenceWireTest} at their caps.
 * Sent as an object graph this measured 34 KB (2026-10-07), so the state now travels gzipped
 * ({@link FetchStateResponse}): about
 * 17 KB, 22 KB with full career records. The bot playouts stay far smaller ({@code StateSizes} in
 * dev-tools), because bots hire only what they need.
 *
 * @author Mario Koehler
 */
class StateSizeTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static int size(PlayerSnapshot state) {
        Kryo kryo = new Kryo();
        MessageRegistry.register(kryo);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Output output = new Output(bytes)) {
            kryo.writeObject(output, FetchStateResponse.success(Personality.forClient(state, new ObjectMapper())));
        }
        return bytes.size();
    }

    private static int cap(Building building) {
        return CATALOG.buildings().capacity(building, CATALOG.buildings().of(building).maxLevel());
    }

    @Test
    void aFullAgencyFitsTheBufferWithRoomToSpare() {
        PlayerSnapshot state = new PlayerSnapshot("Rival Agency Number One", Archetype.SOVIET, 20_000, 9_000, 3_000, 80);
        SplittableRandom random = new SplittableRandom(3);
        List<Astronaut> astronauts = new ArrayList<>();
        List<SetSpecialist> engineers = new ArrayList<>();
        List<MissionControlOfficer> controllers = new ArrayList<>();
        while (astronauts.size() < cap(Building.ASTRONAUT_CENTER) || engineers.size() < cap(Building.SET_CENTER)
                || controllers.size() < cap(Building.MISSION_CONTROL)) {
            HiringPool.refreshCandidates(state, random);
            state.getAstronautCandidates().stream().limit(cap(Building.ASTRONAUT_CENTER) - astronauts.size())
                .forEach(astronauts::add);
            state.getSetSpecialistCandidates().stream().limit(cap(Building.SET_CENTER) - engineers.size())
                .forEach(engineers::add);
            state.getMissionControlCandidates().stream().limit(cap(Building.MISSION_CONTROL) - controllers.size())
                .forEach(controllers::add);
        }
        state.setAstronauts(astronauts);
        state.setSetSpecialists(engineers);
        state.setMissionControlOfficers(controllers);
        HiringPool.refreshCandidates(state, random);
        List<Personnel> everyone = new ArrayList<>();
        everyone.addAll(astronauts);
        everyone.addAll(engineers);
        everyone.addAll(controllers);
        for (Personnel person : everyone) {
            person.setTraits(person.getTraits().stream().map(TraitState::reveal).toList());
        }
        List<Rapport> rapports = new ArrayList<>();
        for (int i = 0; i < everyone.size() && rapports.size() < 300; i++) {
            for (int j = i + 1; j < everyone.size() && rapports.size() < 300; j++) {
                rapports.add(new Rapport(everyone.get(i).getId(), everyone.get(j).getId(), 45, 0.5, 9, true));
            }
        }
        state.setRapports(rapports);
        // Phase 4: every career record full (design.md 2.2(d)), with varied lines.
        CareerEntry.Kind[] kinds = CareerEntry.Kind.values();
        for (Personnel person : everyone) {
            for (int i = 0; i < Careers.CAREER_KEPT; i++) {
                person.getCareer().add(new CareerEntry(i + 1, kinds[i % kinds.length], "vostok-orbital-duration-" + i,
                    i % 2 == 0 ? "CREW" : "rd:vostok-capsule", i % 3 == 0 ? "retrofire" : "", 12.5 + i, i));
            }
        }
        int withoutIntelligence = size(state);
        IntelligenceWireTest.fillFolders(state, 5);
        int full = size(state);
        assertTrue(full < NetworkConstants.OBJECT_BUFFER_SIZE / 2, everyone.size() + " employees, "
            + rapports.size() + " rapports: " + withoutIntelligence + " bytes, " + full + " with five full folders");
    }
}
