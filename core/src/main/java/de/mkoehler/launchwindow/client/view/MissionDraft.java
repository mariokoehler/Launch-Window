package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.LaunchRequest;
import de.mkoehler.launchwindow.rules.MissionAssessment;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionStaffing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A mission being planned: the rocket for each launch and who fills each seat and console. Kept
 * on the session per configuration, so it survives moving between the planning screens; nothing
 * is sent until the launch. Starts from the first rocket that can fly each launch and
 * {@link MissionStaffing#autoAssign}'s staffing. Slot lists always match the layouts' lengths,
 * with {@code null} for an empty or unused slot.
 *
 * @author Mario Koehler
 */
public final class MissionDraft {

    private final MissionConfiguration configuration;
    private final List<String> rocketIds;
    private final List<String> crewSlotIds;
    private final List<String> controlSlotIds;

    private MissionDraft(MissionConfiguration configuration) {
        this.configuration = configuration;
        this.rocketIds = new ArrayList<>(Collections.nCopies(configuration.launches().size(), (String) null));
        this.crewSlotIds = new ArrayList<>(Collections.nCopies(configuration.crewSlots().size(), (String) null));
        this.controlSlotIds = new ArrayList<>(Collections.nCopies(configuration.controlSlots().size(), (String) null));
    }

    /**
     * Starts a draft: the first rocket that can fly each launch, and Auto-assign's staffing.
     *
     * @param configuration the configuration
     * @param state         the player's state
     * @param catalog       the content pack
     * @return the draft
     */
    public static MissionDraft start(MissionConfiguration configuration, PlayerSnapshot state, ContentCatalog catalog) {
        MissionDraft draft = new MissionDraft(configuration);
        draft.refit(state, catalog);
        draft.autoAssign(state);
        return draft;
    }

    /**
     * Returns a copy, to keep showing what was sent after the draft itself is gone.
     *
     * @return the copy
     */
    public MissionDraft copy() {
        MissionDraft copy = new MissionDraft(configuration);
        Collections.copy(copy.rocketIds, rocketIds);
        Collections.copy(copy.crewSlotIds, crewSlotIds);
        Collections.copy(copy.controlSlotIds, controlSlotIds);
        return copy;
    }

    /**
     * Returns the configuration.
     *
     * @return the configuration
     */
    public MissionConfiguration configuration() {
        return configuration;
    }

    /**
     * Returns the chosen rockets, one per launch, {@code null} where none can fly it and {@code ""}
     * for an air drop, which needs none.
     *
     * @return the rockets, unmodifiable
     */
    public List<String> rocketIds() {
        return Collections.unmodifiableList(rocketIds);
    }

    /**
     * Returns the crew seats' holders.
     *
     * @return the ids, unmodifiable, {@code null} for an empty seat
     */
    public List<String> crewSlotIds() {
        return Collections.unmodifiableList(crewSlotIds);
    }

    /**
     * Returns the control slots' holders.
     *
     * @return the ids, unmodifiable, {@code null} for an empty or unused slot
     */
    public List<String> controlSlotIds() {
        return Collections.unmodifiableList(controlSlotIds);
    }

    /**
     * Picks a rocket for a launch.
     *
     * @param launch   the launch, from 0
     * @param rocketId the rocket
     */
    public void setRocket(int launch, String rocketId) {
        rocketIds.set(launch, rocketId);
    }

    /**
     * Puts someone in a crew seat, or empties it; a person can only hold one slot, so they leave
     * any other.
     *
     * @param slot     the seat
     * @param personId the astronaut, or {@code null} to empty it
     */
    public void setCrew(int slot, String personId) {
        vacate(personId);
        crewSlotIds.set(slot, personId);
    }

    /**
     * Puts someone in a control slot, or empties it; they leave any other slot.
     *
     * @param slot     the slot
     * @param personId the person, or {@code null} to empty it
     */
    public void setControl(int slot, String personId) {
        vacate(personId);
        controlSlotIds.set(slot, personId);
    }

    private void vacate(String personId) {
        if (personId == null) {
            return;
        }
        Collections.replaceAll(crewSlotIds, personId, null);
        Collections.replaceAll(controlSlotIds, personId, null);
    }

    /**
     * Empties every seat and console.
     */
    public void clear() {
        Collections.fill(crewSlotIds, null);
        Collections.fill(controlSlotIds, null);
    }

    /**
     * Replaces the staffing with {@link MissionStaffing#autoAssign}'s.
     *
     * @param state the player's state
     */
    public void autoAssign(PlayerSnapshot state) {
        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(state, configuration);
        for (int i = 0; i < crewSlotIds.size(); i++) {
            crewSlotIds.set(i, i < staffing.crewSlotIds().size() ? staffing.crewSlotIds().get(i) : null);
        }
        for (int i = 0; i < controlSlotIds.size(); i++) {
            controlSlotIds.set(i, i < staffing.controlSlotIds().size() ? staffing.controlSlotIds().get(i) : null);
        }
    }

    /**
     * Brings the draft up to date with a new state: people who aren't free any more leave their
     * slots, and a rocket that can't fly its launch any more gives way to the first that can.
     *
     * @param state   the player's state
     * @param catalog the content pack
     */
    public void refit(PlayerSnapshot state, ContentCatalog catalog) {
        Set<String> free = new HashSet<>();
        Stream.concat(state.getAstronauts().stream(), state.getMissionControlOfficers().stream())
            .filter(MissionLaunch::isAvailable).map(Personnel::getId).forEach(free::add);
        crewSlotIds.replaceAll(id -> id != null && free.contains(id) ? id : null);
        controlSlotIds.replaceAll(id -> id != null && free.contains(id) ? id : null);
        for (int slot = 0; slot < controlSlotIds.size(); slot++) {
            if (!configuration.usedControlSlots().contains(slot)) {
                controlSlotIds.set(slot, null);
            }
        }
        for (int launch = 0; launch < rocketIds.size(); launch++) {
            if (!configuration.launches().get(launch).launchType().needsRocket()) {
                rocketIds.set(launch, "");
                continue;
            }
            List<Missions.RocketChoice> choices = Missions.rocketChoices(configuration.launches().get(launch), state,
                catalog);
            String current = rocketIds.get(launch);
            boolean stillGood = choices.stream().anyMatch(choice -> choice.eligible() && choice.rocket().id()
                .equals(current));
            if (!stillGood) {
                rocketIds.set(launch, choices.stream().filter(Missions.RocketChoice::eligible)
                    .map(choice -> choice.rocket().id()).findFirst().orElse(null));
            }
        }
    }

    /**
     * Returns the people free for a slot: of the slot's type, idle and available, and not in
     * another slot of this mission. Best at the slot's skill first.
     *
     * @param kind  the slot's type
     * @param value how each person would score in the slot
     * @param state the player's state
     * @return the people
     */
    public List<? extends Personnel> freeFor(StaffingSlot.Kind kind, java.util.function.ToDoubleFunction<Personnel> value,
                                             PlayerSnapshot state) {
        Set<String> taken = new HashSet<>(crewSlotIds);
        taken.addAll(controlSlotIds);
        List<? extends Personnel> roster = kind == StaffingSlot.Kind.ASTRONAUT ? state.getAstronauts()
            : state.getMissionControlOfficers();
        return roster.stream()
            .filter(MissionLaunch::isAvailable)
            .filter(person -> !taken.contains(person.getId()))
            .sorted(java.util.Comparator.comparingDouble(value).reversed())
            .toList();
    }

    /**
     * Returns the launch as it would be sent.
     *
     * @return the request
     */
    public LaunchRequest request() {
        return new LaunchRequest(configuration.id(), crewSlotIds, controlSlotIds,
            rocketIds.stream().map(id -> id == null ? "" : id).toList());
    }

    /**
     * Returns the odds of this plan ({@link MissionAssessment}).
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the odds
     */
    public MissionAssessment.Assessment assess(PlayerSnapshot state, ContentCatalog catalog) {
        return MissionAssessment.assess(state, catalog, configuration, crewSlotIds, controlSlotIds, rocketIds);
    }

    /**
     * Returns whether every launch has a rocket.
     *
     * @return whether rocketed
     */
    public boolean hasRockets() {
        return rocketIds.stream().allMatch(java.util.Objects::nonNull);
    }
}
