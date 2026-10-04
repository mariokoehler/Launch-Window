package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What the Payload R&amp;D board shows ({@code ui-spec.md} &sect;4.8.3, component 10): one card per
 * owned, active payload component (no rocket spec; rockets have their own board in the VAB), its
 * reliability, and its four R&amp;D slots. Plain logic, no libGDX.
 *
 * <p>The server always puts a newly assigned specialist into the component's <em>lowest free</em>
 * slot, so only that slot offers "+ Assign".</p>
 *
 * @author Mario Koehler
 */
public final class PayloadRnd {

    /** R&amp;D slots per component. */
    public static final int SLOTS = MissionComponent.RD_SLOT_COUNT;

    /**
     * One card on the board.
     *
     * @param component the component's content definition
     * @param owned     the player's copy of it
     * @param slots     who sits in each of the four slots; {@code null} for an empty slot
     */
    public record Card(MissionComponent component, OwnedMissionComponent owned, List<SetSpecialist> slots) {

        /**
         * Returns whether R&amp;D has reached the component's ceiling (its researchers are then
         * released automatically, and no one can be assigned).
         *
         * @return whether at the ceiling
         */
        public boolean atCap() {
            return owned.getCurrentReliability() >= component.maxRdReliability();
        }

        /**
         * Returns the slot a new assignment would land in.
         *
         * @return the lowest free slot, or -1 if all four are taken or the component is at its ceiling
         */
        public int nextFreeSlot() {
            if (atCap()) {
                return -1;
            }
            for (int slot = 0; slot < slots.size(); slot++) {
                if (slots.get(slot) == null) {
                    return slot;
                }
            }
            return -1;
        }
    }

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PayloadRnd() {
    }

    /**
     * Builds the board's cards, in the content pack's order.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the cards
     */
    public static List<Card> cards(PlayerSnapshot state, ContentCatalog catalog) {
        List<Card> cards = new ArrayList<>();
        for (MissionComponent component : catalog.missionComponents()) {
            OwnedMissionComponent owned = state.getMissionComponents().get(component.id());
            if (component.rocket() != null || owned == null || !owned.isActive()) {
                continue;
            }
            List<SetSpecialist> slots = new ArrayList<>();
            for (int slot = 0; slot < SLOTS; slot++) {
                slots.add(null);
            }
            for (SetSpecialist specialist : state.getSetSpecialists()) {
                Integer slot = specialist.getRdSlot();
                if (specialist.getAssignmentState() == AssignmentState.ASSIGNED
                    && component.id().equals(specialist.getRdTargetComponentId()) && slot != null
                    && slot >= 0 && slot < SLOTS) {
                    slots.set(slot, specialist);
                }
            }
            cards.add(new Card(component, owned, slots));
        }
        return cards;
    }

    /**
     * Returns the engineers who could be assigned to a component: idle and available, best in
     * the component's skill first. <b>Unconfirmed default</b> (2026-10-04): the mockup simply took
     * "the next idle specialist"; here the player picks from this list.
     *
     * @param state     the player's state
     * @param component the component
     * @return the candidates, best first
     */
    public static List<SetSpecialist> assignable(PlayerSnapshot state, MissionComponent component) {
        return state.getSetSpecialists().stream()
            .filter(specialist -> !SetDepartment.isBusy(specialist))
            .sorted(Comparator.comparingDouble(
                (SetSpecialist specialist) -> SetDepartment.skill(specialist, component.associatedSkill())).reversed())
            .toList();
    }
}
