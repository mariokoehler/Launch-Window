package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelActions;
import de.mkoehler.launchwindow.rules.ResearchPlanning;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link PayloadRnd#autoAssignActions} against the real rules: applying the actions in order
 * seats the best team with the best engineer in the richest slot.
 *
 * @author Mario Koehler
 */
class PayloadRndAutoAssignTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final ActionContext CONTEXT = new ActionContext(PACK, BudgetSchedule.baspmClose());
    private static final String ROCKET = "jupiter-c";

    private static SetSpecialist engineer(String id, double rockets) {
        SetSpecialist specialist = new SetSpecialist(id, id, 30, 70, 50);
        specialist.setSkill(SetSkill.ROCKETS, rockets);
        return specialist;
    }

    private static List<String> seated(PlayerSnapshot snapshot) {
        PayloadRnd.Card card = PayloadRnd.cards(snapshot, PACK, true).stream()
            .filter(c -> c.component().id().equals(ROCKET)).findFirst().orElseThrow();
        return card.slots().stream().map(s -> s == null ? "-" : s.getId()).toList();
    }

    private static int autoAssign(PlayerSnapshot snapshot) {
        MissionComponent rocket = PACK.missionComponent(ROCKET).orElseThrow();
        PayloadRnd.Card card = PayloadRnd.cards(snapshot, PACK, true).stream()
            .filter(c -> c.component().id().equals(ROCKET)).findFirst().orElseThrow();
        List<PlayerAction> actions = PayloadRnd.autoAssignActions(card, ResearchPlanning.bestTeam(snapshot, rocket));
        for (PlayerAction action : actions) {
            assertTrue(action.apply(snapshot, CONTEXT).applied(), "every action applies: " + action);
        }
        return actions.size();
    }

    /**
     * Verifies a mixed starting board (a weak engineer in the richest slot, a good one in slot 2) ends
     * with the four best seated best-first, and that a second run changes nothing.
     */
    @Test
    void seatsTheBestTeamBestFirstAndIsStable() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 100_000, 0, 0, 1);
        snapshot.getMissionComponents().put(ROCKET, new OwnedMissionComponent(10, 10));
        snapshot.setSetSpecialists(new ArrayList<>(List.of(engineer("weak", 10), engineer("good", 60),
            engineer("best", 90), engineer("fair", 30), engineer("ok", 40), engineer("poor", 20))));
        PersonnelActions.assignResearch(snapshot, "weak", ROCKET, PACK);
        PersonnelActions.assignResearch(snapshot, "good", ROCKET, PACK);
        assertEquals(List.of("weak", "good", "-", "-"), seated(snapshot));

        autoAssign(snapshot);

        assertEquals(List.of("best", "good", "ok", "fair"), seated(snapshot));
        assertEquals(0, autoAssign(snapshot), "already the best team: nothing to do");
    }
}
