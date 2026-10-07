package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Teams and what working together does to people ({@code design.md} &sect;2.2(d)). A team is the
 * engineers on one component's R&amp;D, or the crew and controllers of one mission in flight;
 * someone working alone is a team of one.
 *
 * <p>The season pass captures the teams after departures and before missions resolve (a mission
 * that finishes releases its crew), and {@link #advanceSeason} runs after R&amp;D: everyone in a
 * team gets a work season, each pair's rapport drifts, and traits show. Anyone who left meanwhile
 * (lost on a mission) is skipped.</p>
 *
 * @author Mario Koehler
 */
public final class Teams {

    /** A team's kind, as stored in events. */
    public static final String RD = "rd";

    /** A team's kind, as stored in events. */
    public static final String MISSION = "mission";

    private Teams() {
    }

    /**
     * One team this season.
     *
     * @param kind      {@link #RD} or {@link #MISSION}
     * @param subjectId the component (R&amp;D) or mission configuration
     * @param memberIds the members' ids, in a stable order
     */
    public record Team(String kind, String subjectId, List<String> memberIds) {

        /**
         * Copies the members.
         */
        public Team {
            Objects.requireNonNull(kind, "kind must not be null");
            memberIds = List.copyOf(memberIds);
        }
    }

    /**
     * Returns this season's teams, as the player's state stands: each component with engineers on
     * its R&amp;D, and each mission in flight with its crew and controllers.
     *
     * @param snapshot the player
     * @param catalog  the content pack
     * @return the teams, R&amp;D first in roster order, then missions in their order
     */
    public static List<Team> capture(PlayerSnapshot snapshot, ContentCatalog catalog) {
        List<Team> teams = new ArrayList<>();
        java.util.Map<String, List<String>> research = new java.util.LinkedHashMap<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            String target = specialist.getRdTargetComponentId();
            Integer slot = specialist.getRdSlot();
            if (specialist.getAssignmentState() != AssignmentState.ASSIGNED || target == null || slot == null
                || slot < 0 || slot >= MissionComponent.RD_SLOT_COUNT
                || snapshot.getMissionComponents().get(target) == null || catalog.missionComponent(target).isEmpty()) {
                continue;
            }
            research.computeIfAbsent(target, key -> new ArrayList<>()).add(specialist.getId());
        }
        research.forEach((component, members) -> teams.add(new Team(RD, component, members)));
        for (InProgressMission mission : snapshot.getInProgressMissions().values()) {
            Set<String> members = new LinkedHashSet<>();
            Stream.concat(mission.getCrewSlotIds().stream(), mission.getControlSlotIds().stream())
                .filter(Objects::nonNull).forEach(members::add);
            if (!members.isEmpty()) {
                teams.add(new Team(MISSION, mission.getMissionConfigurationId(), new ArrayList<>(members)));
            }
        }
        return teams;
    }

    /**
     * Returns the people in a team who are still with the agency.
     *
     * @param snapshot the player
     * @param team     the team
     * @return the members, in the team's order
     */
    public static List<Personnel> members(PlayerSnapshot snapshot, Team team) {
        List<Personnel> members = new ArrayList<>();
        for (String id : team.memberIds()) {
            Personnel person = find(snapshot, id);
            if (person != null) {
                members.add(person);
            }
        }
        return members;
    }

    /**
     * Runs the season's teamwork: work seasons, rapport and reveals.
     *
     * @param snapshot the player
     * @param teams    the teams captured earlier this season ({@link #capture})
     * @return the reveals and newly shown relationships
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, List<Team> teams) {
        List<ResolutionEvent> events = new ArrayList<>();
        dropDeparted(snapshot);
        Set<String> counted = new HashSet<>();
        for (Team team : teams) {
            List<Personnel> members = members(snapshot, team);
            for (Personnel person : members) {
                if (counted.add(person.getId())) {
                    person.setWorkSeasons(person.getWorkSeasons() + 1);
                }
            }
            for (int i = 0; i < members.size(); i++) {
                for (int j = i + 1; j < members.size(); j++) {
                    drift(snapshot, team, members.get(i), members.get(j), events);
                }
            }
            for (Personnel person : members) {
                revealDue(person, team, events);
            }
        }
        return events;
    }

    private static void drift(PlayerSnapshot snapshot, Team team, Personnel a, Personnel b,
            List<ResolutionEvent> events) {
        List<Rapport> pairs = snapshot.getRapports();
        int index = -1;
        for (int k = 0; k < pairs.size(); k++) {
            if (pairs.get(k).isPair(a.getId(), b.getId())) {
                index = k;
                break;
            }
        }
        Rapport pair = index >= 0 ? pairs.get(index) : newPair(snapshot, a, b);
        double rapport = Math.max(-Personality.RAPPORT_MAX, Math.min(Personality.RAPPORT_MAX,
            pair.rapport() + Personality.RAPPORT_DRIFT * pair.affinity()));
        boolean shows = !pair.shown() && Math.abs(rapport) >= Personality.RAPPORT_SHOWN_AT;
        Rapport updated = new Rapport(pair.first(), pair.second(), rapport, pair.affinity(), pair.seasonsTogether() + 1,
            pair.shown() || shows);
        if (index >= 0) {
            pairs.set(index, updated);
        } else {
            pairs.add(updated);
        }
        if (shows) {
            Personnel first = a.getId().equals(updated.first()) ? a : b;
            Personnel second = first == a ? b : a;
            events.add(new RelationshipShown(first.getId(), first.getName(), second.getId(), second.getName(),
                rapport > 0, team.kind(), team.subjectId()));
            // The shortcuts: a clash gives an Abrasive member away, a friendship an Easy-going one.
            Trait tell = rapport > 0 ? Trait.EASY_GOING : Trait.ABRASIVE;
            for (Personnel person : List.of(first, second)) {
                reveal(person, tell, team, events);
            }
        }
    }

    private static Rapport newPair(PlayerSnapshot snapshot, Personnel a, Personnel b) {
        String first = a.getId().compareTo(b.getId()) <= 0 ? a.getId() : b.getId();
        String second = first.equals(a.getId()) ? b.getId() : a.getId();
        return new Rapport(first, second, 0, Personality.affinity(snapshot, a, b), 0, false);
    }

    private static void revealDue(Personnel person, Team team, List<ResolutionEvent> events) {
        for (TraitState state : List.copyOf(person.getTraits())) {
            if (!state.revealed() && person.getWorkSeasons() >= state.revealAt()) {
                reveal(person, state.trait(), team, events);
            }
        }
    }

    private static void reveal(Personnel person, Trait trait, Team team, List<ResolutionEvent> events) {
        List<TraitState> traits = new ArrayList<>(person.getTraits());
        for (int k = 0; k < traits.size(); k++) {
            TraitState state = traits.get(k);
            if (state.trait() == trait && !state.revealed()) {
                traits.set(k, state.reveal());
                person.setTraits(traits);
                events.add(new TraitRevealed(groupOf(person), person.getId(), person.getName(), trait, team.kind(),
                    team.subjectId(), person.getWorkSeasons()));
                return;
            }
        }
    }

    private static void dropDeparted(PlayerSnapshot snapshot) {
        snapshot.getRapports().removeIf(pair -> find(snapshot, pair.first()) == null
            || find(snapshot, pair.second()) == null);
    }

    /**
     * Finds an employee in any roster.
     *
     * @param snapshot the player
     * @param id       the id
     * @return the employee, or {@code null}
     */
    public static Personnel find(PlayerSnapshot snapshot, String id) {
        return Stream.of(snapshot.getAstronauts(), snapshot.getSetSpecialists(), snapshot.getMissionControlOfficers())
            .flatMap(List::stream).filter(person -> person.getId().equals(id)).findFirst().orElse(null);
    }

    private static PersonnelGroup groupOf(Personnel person) {
        return switch (person) {
            case de.mkoehler.launchwindow.model.Astronaut astronaut -> PersonnelGroup.ASTRONAUT;
            case SetSpecialist specialist -> PersonnelGroup.SET;
            default -> PersonnelGroup.MISSION_CONTROL;
        };
    }
}
