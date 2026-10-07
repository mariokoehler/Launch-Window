package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.actions.AssignResearchAction;
import de.mkoehler.launchwindow.actions.ReleaseResearchAction;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.persistence.PlayerAction;

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
            // A revealed Perfectionist on the team raises the cap (design.md 2.2(d)).
            List<SetSpecialist> team = slots.stream().filter(java.util.Objects::nonNull).toList();
            return owned.getCurrentReliability() >= Math.min(component.maxReliability(),
                component.maxRdReliability() + de.mkoehler.launchwindow.rules.TraitEffects.capBonus(team, true));
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
        return cards(state, catalog, false);
    }

    /**
     * Builds a board's cards, in the content pack's order: the payload board (SET Centre) or the
     * rocket board (VAB), which work the same way.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @param rockets whether to build the rocket board
     * @return the cards
     */
    public static List<Card> cards(PlayerSnapshot state, ContentCatalog catalog, boolean rockets) {
        List<Card> cards = new ArrayList<>();
        for (MissionComponent component : catalog.missionComponents()) {
            OwnedMissionComponent owned = state.getMissionComponents().get(component.id());
            if (component.hasRocketSpec() != rockets || owned == null || !owned.isActive()) {
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
    /**
     * Returns the actions that seat a team on a component, best first: releases for whoever sits in
     * the wrong slot or isn't in the team, then assignments in team order. The server seats each new
     * researcher in the lowest free slot, so team member {@code i} ends up in slot {@code i}, the best
     * in the richest. Engineers already in their right slot are left alone.
     *
     * @param card the component's card, with who sits where now
     * @param team the team, best first, at most four ({@code ResearchPlanning.bestTeam})
     * @return the actions, in order; empty if the team already sits that way
     */
    public static List<PlayerAction> autoAssignActions(Card card, List<SetSpecialist> team) {
        List<PlayerAction> actions = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            SetSpecialist sitting = card.slots().get(slot);
            if (sitting != null && (slot >= team.size() || !team.get(slot).getId().equals(sitting.getId()))) {
                actions.add(new ReleaseResearchAction(sitting.getId()));
            }
        }
        for (int slot = 0; slot < team.size(); slot++) {
            SetSpecialist sitting = card.slots().get(slot);
            if (sitting == null || !team.get(slot).getId().equals(sitting.getId())) {
                actions.add(new AssignResearchAction(team.get(slot).getId(), card.component().id()));
            }
        }
        return actions;
    }

    public static List<SetSpecialist> assignable(PlayerSnapshot state, MissionComponent component) {
        return state.getSetSpecialists().stream()
            .filter(specialist -> !SetDepartment.isBusy(specialist))
            .sorted(Comparator.comparingDouble(
                (SetSpecialist specialist) -> SetDepartment.skill(specialist, component.associatedSkill())).reversed())
            .toList();
    }
}
