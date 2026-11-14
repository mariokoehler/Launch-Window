package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Offers the people a proposed staffing passes over a slot where their ambition would come true (the user's
 * request, 2026-11-14, {@code ui-spec.md} &sect;4.6.8): Auto-assign and Pull from training pick each slot's best
 * fit, and then ask, one person at a time, whether to give someone ambitious the place of the best fit. The
 * screen asks; this finds the next question on the plan as it stands, so each answer changes the next.
 *
 * <p>Which ambitions a flight serves, and where, is {@link Careers#ambitionsAtStake}'s: a seat in the crew for
 * flying ones, a crew seat or CAPCOM for being part of a First, a console for the controllers' ones. The swap
 * offered is the slot where the ambitious person costs least against the holder, on the slot's own measure (a
 * seat's fit, {@link MissionStaffing#seatFit}, or a console's value, {@link MissionProgress#slotValue}); a holder
 * whose own ambition this flight serves is displaced only when nobody else can be.</p>
 *
 * @author Mario Koehler
 */
public final class AmbitionSwaps {

    private AmbitionSwaps() {
    }

    /**
     * One question: put someone ambitious in a slot instead of its best fit.
     *
     * @param personId      the ambitious person
     * @param kind          their ambition
     * @param ifFirst       whether it only comes true if the flight reaches a goal first
     * @param crew          whether the slot is a crew seat (else a control slot)
     * @param slot          the seat's or control slot's index
     * @param holderId      who holds it in the plan, the best fit
     * @param holderValue   the holder's fit for it, 0 to 100
     * @param personValue   the ambitious person's fit for it, 0 to 100
     * @param holderWants   whether the holder's own ambition this flight serves would be passed over
     * @param fromTraining  whether the ambitious person would leave training for it
     */
    public record Swap(String personId, Ambition.Kind kind, boolean ifFirst, boolean crew, int slot, String holderId,
                       double holderValue, double personValue, boolean holderWants, boolean fromTraining) {
    }

    /**
     * Returns the next question for a plan, or empty when nobody ambitious is passed over who could take a slot.
     *
     * @param snapshot       the player
     * @param configuration  the mission
     * @param crewSlotIds    the plan's crew seats, {@code null} or empty where nobody sits
     * @param controlSlotIds the plan's control slots, likewise
     * @param training       whether people in training may be offered (Pull from training), not only the free
     * @param asked          people already asked about, never asked again
     * @param offered        which slots may change hands, by crew-or-control and index (Pull from training offers
     *                       only the slots it fills, never one the player filled by hand)
     * @return the question
     */
    public static Optional<Swap> next(PlayerSnapshot snapshot, MissionConfiguration configuration,
                                      List<String> crewSlotIds, List<String> controlSlotIds, boolean training,
                                      Collection<String> asked, java.util.function.BiPredicate<Boolean, Integer> offered) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        List<String> crew = present(crewSlotIds);
        List<String> ground = present(controlSlotIds);
        List<Careers.AmbitionAtStake> stakes = Careers.ambitionsAtStake(snapshot, configuration, crew, ground);
        Set<String> wanting = new HashSet<>();
        for (Careers.AmbitionAtStake stake : stakes) {
            if (stake.met()) {
                wanting.add(stake.person().getId());
            }
        }
        for (Careers.AmbitionAtStake stake : stakes) {
            Personnel person = stake.person();
            // Someone already in the plan (CAPCOM, say) isn't moved: their slot would be left empty.
            if (stake.met() || asked.contains(person.getId()) || crew.contains(person.getId())
                    || ground.contains(person.getId())) {
                continue;
            }
            boolean free = MissionLaunch.isAvailable(person);
            if (!free && !(training && MissionStaffing.pullableForMission(person))) {
                continue;
            }
            Swap best = null;
            for (Candidate slot : slots(configuration, stake.kind(), crewSlotIds, controlSlotIds)) {
                String holder = slot.crew() ? crewSlotIds.get(slot.index()) : controlSlotIds.get(slot.index());
                if (holder == null || holder.isEmpty() || !offered.test(slot.crew(), slot.index())) {
                    continue;
                }
                double holderValue = value(snapshot, configuration, slot, holder);
                double personValue = value(snapshot, configuration, slot, person.getId());
                Swap swap = new Swap(person.getId(), stake.kind(), stake.ifFirst(), slot.crew(), slot.index(), holder,
                    holderValue, personValue, wanting.contains(holder), !free);
                if (best == null || better(swap, best)) {
                    best = swap;
                }
            }
            if (best != null) {
                return Optional.of(best);
            }
        }
        return Optional.empty();
    }

    /** A holder without an ambition at stake first, then the smallest loss of fit. */
    private static boolean better(Swap swap, Swap best) {
        if (swap.holderWants() != best.holderWants()) {
            return !swap.holderWants();
        }
        return swap.holderValue() - swap.personValue() < best.holderValue() - best.personValue();
    }

    private record Candidate(boolean crew, int index) {
    }

    /** The slots where an ambition comes true, as {@link Careers#ambitionsAtStake} counts them. */
    private static List<Candidate> slots(MissionConfiguration configuration, Ambition.Kind kind, List<String> crew,
                                         List<String> control) {
        List<Candidate> slots = new ArrayList<>();
        boolean seats = kind == Ambition.Kind.FLY_IN_SPACE || kind == Ambition.Kind.FLY_TO_ORBIT
            || kind == Ambition.Kind.DAYS_IN_ORBIT || kind == Ambition.Kind.JOINT_FLIGHT
            || kind == Ambition.Kind.BE_FIRST;
        if (seats) {
            for (int slot : configuration.usedCrewSlots()) {
                if (slot < crew.size()) {
                    slots.add(new Candidate(true, slot));
                }
            }
        }
        StaffingSlot.Kind consoles = kind == Ambition.Kind.BE_FIRST ? StaffingSlot.Kind.ASTRONAUT
            : kind == Ambition.Kind.CONSOLES_ORBITAL || kind == Ambition.Kind.CONSOLES_FIRST
            ? StaffingSlot.Kind.MISSION_CONTROL : null;
        if (consoles != null) {
            for (int slot : configuration.usedControlSlots()) {
                if (slot < control.size() && configuration.controlSlots().get(slot).kind() == consoles) {
                    slots.add(new Candidate(false, slot));
                }
            }
        }
        return slots;
    }

    private static double value(PlayerSnapshot snapshot, MissionConfiguration configuration, Candidate slot,
                                String personId) {
        if (slot.crew()) {
            Astronaut astronaut = snapshot.getAstronauts().stream().filter(a -> a.getId().equals(personId))
                .findFirst().orElse(null);
            return astronaut == null ? 0 : MissionStaffing.seatFit(astronaut, configuration, slot.index());
        }
        return MissionProgress.slotValue(snapshot, configuration.controlSlots().get(slot.index()), personId);
    }

    private static List<String> present(List<String> ids) {
        return ids.stream().filter(id -> id != null && !id.isEmpty()).toList();
    }
}
