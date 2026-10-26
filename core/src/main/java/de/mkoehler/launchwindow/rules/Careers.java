package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.LaunchType;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Stream;

/**
 * People's careers ({@code design.md} &sect;2.2(d), phase 4): ambitions said, met, passed over,
 * made impossible or run out; loyalty; and the career record each person's page shows. Runs as its
 * own phase of the season pass after the teams, reading what the season did. Every random draw
 * comes from the person's own seed, never the season's stream. Every number here is our own,
 * accepted as a default (user, 2026-10-08), to tune after the play test.
 *
 * @author Mario Koehler
 */
public final class Careers {

    /** The chance a candidate has an ambition coming: Ambitious. */
    public static final int AMBITION_PERCENT_AMBITIOUS = 100;
    /** The chance a candidate has an ambition coming: Glory seeker. */
    public static final int AMBITION_PERCENT_GLORY_SEEKER = 70;
    /** The chance a candidate has an ambition coming: anyone else. */
    public static final int AMBITION_PERCENT_OTHERS = 25;
    /** The chance of a new ambition after one is settled: Ambitious. */
    public static final int NEXT_PERCENT_AMBITIOUS = 75;
    /** The chance of a new ambition after one is settled: anyone else. */
    public static final int NEXT_PERCENT_OTHERS = 30;
    /** Seasons with the agency before a person says an ambition. */
    public static final int SAY_AFTER_SEASONS = 2;
    /** The patience window's change for Ambitious. */
    public static final int WINDOW_AMBITIOUS = -1;
    /** The patience window's change for Steady. */
    public static final int WINDOW_STEADY = 2;
    /** The shortest patience window. */
    public static final int WINDOW_MIN = 3;
    /**
     * Seasons to spare beyond the projection for "bring a component to its cap" (user, 2026-10-23): the
     * team has to be seated first, and the projection is an average.
     */
    public static final int CAP_SLACK = 2;
    /**
     * The longest window "bring a component to its cap" stretches to; a cap further off than this, with
     * the agency's best team, isn't drawn at all (user, 2026-10-23).
     */
    public static final int CAP_WINDOW_MAX = 20;
    /** Loyalty on hiring. */
    public static final int LOYALTY_START = 50;
    /** Loyalty gained each season with the agency. */
    public static final int LOYALTY_PER_SEASON = 1;
    /** Morale and loyalty when an ambition comes true (Ambitious gets the larger loyalty gain). */
    public static final int FULFILLED_MORALE = 5;
    /** Loyalty when an ambition comes true. */
    public static final int FULFILLED_LOYALTY = 15;
    /** Loyalty when an Ambitious person's ambition comes true. */
    public static final int FULFILLED_LOYALTY_AMBITIOUS = 20;
    /** Morale when passed over. */
    public static final int PASSED_OVER_MORALE = -5;
    /** Loyalty when passed over. */
    public static final int PASSED_OVER_LOYALTY = -5;
    /** Morale when an ambition can't come true any more. */
    public static final int IMPOSSIBLE_MORALE = -2;
    /** Loyalty when an ambition can't come true any more. */
    public static final int IMPOSSIBLE_LOYALTY = -3;
    /** Morale when the patience runs out. */
    public static final int RAN_OUT_MORALE = -10;
    /** Loyalty when the patience runs out. */
    public static final int RAN_OUT_LOYALTY = -15;
    /** Seasons of disappointment after the patience runs out. */
    public static final int DISAPPOINTED_SEASONS = 4;
    /** The career lines kept per person; the oldest drop off, the hiring line stays. */
    public static final int CAREER_KEPT = 30;

    private static final long PENDING_SALT = 0x0A3B_1710_5EED_1959L;
    private static final long DRAW_SALT = 0x0D2A_3017_CAFE_1960L;
    private static final long NEXT_SALT = 0x7E47_0000_AB1E_1961L;

    private Careers() {
    }

    /**
     * Who was on a mission in flight at the start of the season, captured before missions resolve
     * (a finishing mission is gone afterwards).
     *
     * @param instanceId      the mission
     * @param configurationId its configuration
     * @param crewIds         the crew
     * @param groundIds       the ground staff: controllers and CAPCOM astronauts
     * @param componentIds    the rockets and payloads it flies
     */
    public record Flight(String instanceId, String configurationId, List<String> crewIds, List<String> groundIds,
                         List<String> componentIds) {
    }

    /**
     * Captures the missions in flight.
     *
     * @param snapshot the agency
     * @param catalog  the content pack
     * @return the flights
     */
    public static List<Flight> capture(PlayerSnapshot snapshot, ContentCatalog catalog) {
        List<Flight> flights = new ArrayList<>();
        for (InProgressMission mission : snapshot.getInProgressMissions().values()) {
            List<String> components = new ArrayList<>();
            mission.getRocketIds().stream().filter(id -> id != null && !id.isBlank()).forEach(components::add);
            catalog.missionConfiguration(mission.getMissionConfigurationId()).ifPresent(configuration ->
                configuration.launches().forEach(launch -> components.addAll(launch.payloadComponentIds())));
            flights.add(new Flight(mission.getId(), mission.getMissionConfigurationId(), mission.crewIds(),
                mission.groundIds(), components));
        }
        return flights;
    }

    /**
     * Decides, when a candidate is drawn, whether an ambition is coming, from the candidate's own
     * seed. Called after the traits are assigned.
     *
     * @param snapshot  the agency
     * @param candidate the candidate
     */
    static void assignPending(PlayerSnapshot snapshot, Personnel candidate) {
        int percent = Personality.has(candidate, Trait.AMBITIOUS) ? AMBITION_PERCENT_AMBITIOUS
            : Personality.has(candidate, Trait.GLORY_SEEKER) ? AMBITION_PERCENT_GLORY_SEEKER : AMBITION_PERCENT_OTHERS;
        SplittableRandom random = new SplittableRandom(Intelligence.seed(snapshot.getAgencyName(), candidate.getId())
            ^ PENDING_SALT);
        candidate.setAmbitionPending(random.nextInt(100) < percent);
    }

    /**
     * Builds an astronaut's memorial entry, with what the person page needs: skills, the traits the
     * player knew of (never hidden ones), when they joined, and the career record.
     *
     * @param astronaut              the astronaut
     * @param turn                   the game turn
     * @param cause                  how they were lost
     * @param missionConfigurationId the mission, or {@code null}
     * @param stepId                 the step, or {@code null}
     * @return the entry
     */
    public static MemorialEntry memorial(Astronaut astronaut, int turn, MemorialEntry.Cause cause,
            String missionConfigurationId, String stepId) {
        Map<String, Double> skills = new LinkedHashMap<>();
        astronaut.getSkills().forEach((skill, value) -> skills.put(skill.name(), value));
        List<Trait> known = astronaut.getTraits().stream().filter(TraitState::revealed).map(TraitState::trait)
            .toList();
        int joined = astronaut.getCareer().stream().filter(line -> line.kind() == CareerEntry.Kind.HIRED)
            .mapToInt(CareerEntry::turn).findFirst().orElse(-1);
        return new MemorialEntry(astronaut.getName(), astronaut.getAge(), turn, cause, missionConfigurationId, stepId,
            astronaut.getPortraitId(), astronaut.getId(), skills, known, joined, astronaut.getCareer());
    }

    /**
     * Returns a loyalty band, as the person page and intelligence files show it.
     *
     * @param loyalty the loyalty, 0 to 100
     * @return "Low" (below 40), "Middling" (below 70) or "High"
     */
    public static String loyaltyBand(int loyalty) {
        return loyalty < 40 ? "Low" : loyalty < 70 ? "Middling" : "High";
    }

    /**
     * Moves a department's loyalty with a salary change: half the percentage, rounded.
     *
     * @param roster  the department
     * @param percent the salary change, in percent
     */
    static void salaryChanged(List<? extends Personnel> roster, int percent) {
        int change = Math.round(percent / 2f);
        for (Personnel person : roster) {
            person.setLoyalty(Math.clamp(person.getLoyalty() + change, 0, 100));
        }
    }

    /** What the season did, in the terms ambitions and careers need. */
    private static final class Season {
        final Set<String> crewFlew = new HashSet<>();
        /** Crew of a flight's first season only, so one flight passes others over once. */
        final Set<String> crewLaunched = new HashSet<>();
        final Set<String> crewLaunchedOrbital = new HashSet<>();
        final Set<String> crewSecondSeason = new HashSet<>();
        final Set<String> crewLaunchedJoint = new HashSet<>();
        final Set<String> consolesLaunchedOrbital = new HashSet<>();
        final Set<String> crewFlewOrbital = new HashSet<>();
        final Set<String> crewDaysInOrbit = new HashSet<>();
        final Set<String> crewJoint = new HashSet<>();
        final Set<String> aboardFirst = new HashSet<>();
        final Set<String> consolesOrbital = new HashSet<>();
        final Set<String> consolesFirst = new HashSet<>();
        final Set<String> firstComponents = new HashSet<>();
        final Map<String, Set<String>> capped = new HashMap<>();
        final Map<String, String> experts = new HashMap<>();
        final Map<String, Double> researchGain = new HashMap<>();
        final Set<String> astronautIds = new HashSet<>();
        final Set<String> controllerIds = new HashSet<>();
    }

    /**
     * Runs the careers phase: tenure and loyalty, the career record, R&amp;D stints, flights that
     * ended, the memorial's last line, and every ambition.
     *
     * @param snapshot  the agency
     * @param catalog   the content pack
     * @param tally     the game-wide goal counts at the start of the turn
     * @param flights   the missions in flight at the start of the season ({@link #capture})
     * @param events    the season's events so far
     * @param gameTurn  the turn being resolved
     * @return the events: an {@link AmbitionSettled} for each ambition said or settled
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog,
            GoalTally tally, List<Flight> flights, List<ResolutionEvent> events, int gameTurn) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        List<Personnel> everyone = employees(snapshot);
        Map<String, Personnel> byId = new HashMap<>();
        everyone.forEach(person -> byId.put(person.getId(), person));
        Season season = read(snapshot, catalog, flights, events);
        snapshot.getAstronauts().forEach(person -> season.astronautIds.add(person.getId()));
        snapshot.getMissionControlOfficers().forEach(person -> season.controllerIds.add(person.getId()));

        for (Personnel person : everyone) {
            person.setTenureSeasons(person.getTenureSeasons() + 1);
            person.setLoyalty(Math.clamp(person.getLoyalty() + LOYALTY_PER_SEASON, 0, 100));
            if (person.getCareer().isEmpty()) {
                person.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.HIRED, "", person.getHint(), "", 0,
                    0));
            }
        }
        recordEvents(byId, events, flights, gameTurn);
        recordStints(snapshot, season, gameTurn);
        recordFlights(snapshot, byId, flights, events, gameTurn);

        List<ResolutionEvent> out = new ArrayList<>();
        for (Personnel person : everyone) {
            ambition(snapshot, catalog, tally, season, person, gameTurn, out);
            trim(person.getCareer());
        }
        return out;
    }

    private static Season read(PlayerSnapshot snapshot, ContentCatalog catalog, List<Flight> flights,
            List<ResolutionEvent> events) {
        Season season = new Season();
        Map<String, List<MissionStepResolved>> steps = new HashMap<>();
        Set<String> firstMissions = new HashSet<>();
        for (ResolutionEvent event : events) {
            if (event instanceof MissionStepResolved step) {
                steps.computeIfAbsent(step.missionInstanceId(), id -> new ArrayList<>()).add(step);
            } else if (event instanceof GoalSettled goal && goal.firstInGame() && !goal.cancelled()) {
                firstMissions.add(goal.missionInstanceId());
            } else if (event instanceof ResearchComponentMaxed maxed) {
                season.capped.computeIfAbsent(maxed.componentId(), id -> new HashSet<>())
                    .addAll(maxed.releasedPersonnelIds());
            } else if (event instanceof ExpertTrainingCompleted expert) {
                season.experts.put(expert.personnelId(), expert.skillName());
            } else if (event instanceof ResearchCompleted research) {
                season.researchGain.merge(research.personnelId(), research.reliabilityGain(), Double::sum);
            }
        }
        for (Flight flight : flights) {
            List<MissionStepResolved> resolved = steps.getOrDefault(flight.instanceId(), List.of());
            boolean failed = resolved.stream().anyMatch(Careers::failed);
            boolean flew = !resolved.isEmpty() && !failed;
            MissionConfiguration configuration = catalog.missionConfiguration(flight.configurationId()).orElse(null);
            if (firstMissions.contains(flight.instanceId())) {
                season.aboardFirst.addAll(flight.crewIds());
                season.aboardFirst.addAll(flight.groundIds());
                season.consolesFirst.addAll(flight.groundIds());
                season.firstComponents.addAll(flight.componentIds());
            }
            if (!flew || configuration == null || !crewed(configuration)) {
                continue;
            }
            boolean first = resolved.stream().anyMatch(step -> step.seasonIndex() == 0);
            boolean second = resolved.stream().anyMatch(step -> step.seasonIndex() == 1);
            season.crewFlew.addAll(flight.crewIds());
            if (first) {
                season.crewLaunched.addAll(flight.crewIds());
            }
            if (orbital(configuration)) {
                season.crewFlewOrbital.addAll(flight.crewIds());
                season.consolesOrbital.addAll(flight.groundIds());
                if (first) {
                    season.crewLaunchedOrbital.addAll(flight.crewIds());
                    season.consolesLaunchedOrbital.addAll(flight.groundIds());
                }
                boolean later = resolved.stream().anyMatch(step -> step.seasonIndex() >= 1);
                if (later && configuration.durationInSeasons() >= 2) {
                    season.crewDaysInOrbit.addAll(flight.crewIds());
                    if (second) {
                        season.crewSecondSeason.addAll(flight.crewIds());
                    }
                }
            }
            if (configuration.launches().size() >= 2) {
                season.crewJoint.addAll(flight.crewIds());
                if (first) {
                    season.crewLaunchedJoint.addAll(flight.crewIds());
                }
            }
        }
        return season;
    }

    private static boolean failed(MissionStepResolved step) {
        return step.outcome() == MissionStepOutcome.FAILURE || step.outcome() == MissionStepOutcome.CATASTROPHIC;
    }

    private static void recordEvents(Map<String, Personnel> byId, List<ResolutionEvent> events, List<Flight> flights,
            int gameTurn) {
        Map<String, Flight> flightById = new HashMap<>();
        flights.forEach(flight -> flightById.put(flight.instanceId(), flight));
        for (ResolutionEvent event : events) {
            if (event instanceof TraitRevealed revealed) {
                add(byId, revealed.personnelId(), new CareerEntry(gameTurn, CareerEntry.Kind.TRAIT,
                    revealed.trait().name(), "", "", 0, 0));
            } else if (event instanceof RelationshipShown shown) {
                String subject = shown.teamKind() + ":" + shown.subjectId();
                add(byId, shown.firstId(), new CareerEntry(gameTurn, CareerEntry.Kind.RELATIONSHIP, shown.secondName(),
                    subject, "", shown.positive() ? 1 : -1, shown.seasonsTogether()));
                add(byId, shown.secondId(), new CareerEntry(gameTurn, CareerEntry.Kind.RELATIONSHIP, shown.firstName(),
                    subject, "", shown.positive() ? 1 : -1, shown.seasonsTogether()));
            } else if (event instanceof ExpertTrainingCompleted expert) {
                add(byId, expert.personnelId(), new CareerEntry(gameTurn, CareerEntry.Kind.EXPERT, expert.skillName(),
                    "", "", 0, 0));
            } else if (event instanceof RocketPlaneWingsEarned wings) {
                add(byId, wings.personnelId(), new CareerEntry(gameTurn, CareerEntry.Kind.WINGS,
                    wings.missionConfigurationId(), "", "", 0, 0));
            } else if (event instanceof TrainingCompleted training) {
                add(byId, training.personnelId(), new CareerEntry(gameTurn, CareerEntry.Kind.TRAINING,
                    training.skillName(), training.trainingKind().name(), "", training.skillGain(), 0));
            } else if (event instanceof GoalSettled goal && goal.firstInGame() && !goal.cancelled()) {
                Flight flight = flightById.get(goal.missionInstanceId());
                if (flight != null) {
                    Stream.concat(flight.crewIds().stream(), flight.groundIds().stream()).distinct().forEach(id ->
                        add(byId, id, new CareerEntry(gameTurn, CareerEntry.Kind.FIRST, goal.goalId(),
                            flight.configurationId(), "", 0, 0)));
                }
            }
        }
    }

    private static void add(Map<String, Personnel> byId, String id, CareerEntry entry) {
        Personnel person = byId.get(id);
        if (person != null) {
            person.getCareer().add(entry);
        }
    }

    /** Opens, extends and closes each engineer's R&amp;D stint, from this season's teams. */
    private static void recordStints(PlayerSnapshot snapshot, Season season, int gameTurn) {
        for (SetSpecialist engineer : snapshot.getSetSpecialists()) {
            String now = engineer.getAssignmentState() == AssignmentState.ASSIGNED
                ? engineer.getRdTargetComponentId() : null;
            String running = engineer.getStintComponentId();
            if (running != null && !running.equals(now)) {
                closeStint(engineer, gameTurn);
            }
            if (now != null) {
                if (!now.equals(engineer.getStintComponentId())) {
                    engineer.setStintComponentId(now);
                    engineer.setStintSeasons(0);
                    engineer.setStintGain(0);
                }
                engineer.setStintSeasons(engineer.getStintSeasons() + 1);
                engineer.setStintGain(engineer.getStintGain() + season.researchGain.getOrDefault(engineer.getId(), 0.0));
            }
        }
    }

    private static void closeStint(SetSpecialist engineer, int gameTurn) {
        if (engineer.getStintSeasons() > 0) {
            engineer.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.RD_STINT, engineer.getStintComponentId(),
                "", "", Math.round(engineer.getStintGain() * 10) / 10.0, engineer.getStintSeasons()));
        }
        engineer.setStintComponentId(null);
        engineer.setStintSeasons(0);
        engineer.setStintGain(0);
    }

    /** Records each mission that ended this season on its crew's and ground staff's records. */
    private static void recordFlights(PlayerSnapshot snapshot, Map<String, Personnel> byId, List<Flight> flights,
            List<ResolutionEvent> events, int gameTurn) {
        for (Flight flight : flights) {
            if (snapshot.getInProgressMissions().containsKey(flight.instanceId())) {
                continue;
            }
            String failedAt = events.stream().filter(MissionStepResolved.class::isInstance)
                .map(MissionStepResolved.class::cast).filter(step -> step.missionInstanceId().equals(flight.instanceId()))
                .filter(Careers::failed).map(MissionStepResolved::stepId).findFirst().orElse("");
            for (String id : flight.crewIds()) {
                CareerEntry line = new CareerEntry(gameTurn, CareerEntry.Kind.FLIGHT, flight.configurationId(), "CREW",
                    failedAt, 0, 0);
                if (byId.containsKey(id)) {
                    byId.get(id).getCareer().add(line);
                } else {
                    memorialLine(snapshot, id, gameTurn, line);
                }
            }
            for (String id : flight.groundIds()) {
                Personnel person = byId.get(id);
                if (person != null) {
                    person.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.FLIGHT, flight.configurationId(),
                        person instanceof Astronaut ? "CAPCOM" : "CONTROLLER", failedAt, 0, 0));
                }
            }
        }
    }

    private static void memorialLine(PlayerSnapshot snapshot, String id, int gameTurn, CareerEntry line) {
        List<MemorialEntry> memorial = snapshot.getMemorial();
        for (int i = 0; i < memorial.size(); i++) {
            MemorialEntry entry = memorial.get(i);
            if (entry.turn() == gameTurn && id.equals(entry.personnelId())) {
                memorial.set(i, entry.withCareer(line));
            }
        }
    }

    // ---- Ambitions

    /**
     * One person's stated ambition that a planned mission would settle if it flies well.
     *
     * @param person    the person
     * @param kind      the ambition
     * @param met       {@code true} when they're on the team and it would come true; {@code false}
     *                  when someone else of their group fills the role, which passes them over
     * @param ifFirst   whether it only counts if the flight reaches a goal first in the game
     */
    public record AmbitionAtStake(Personnel person, Ambition.Kind kind, boolean met, boolean ifFirst) {
    }

    /**
     * Returns whose stated ambitions a planned mission would meet or pass over if it flies well
     * (the user's request, 2026-10-10), the way the season pass settles them: crew ambitions by who
     * is in the crew, console ambitions by who is on the ground team (CAPCOM counts as aboard for
     * {@link Ambition.Kind#BE_FIRST}). Everyone of the group not on it counts as passed over, as the season
     * pass counts them, busy elsewhere or not (2026-10-22: the preview had left out anyone in training, on
     * another mission or sidelined). The two "first" ambitions only matter while the
     * mission has a goal the agency hasn't reached yet, and then only if nobody gets there first.
     *
     * @param snapshot      the player
     * @param configuration the mission
     * @param crewIds       the planned crew, by id
     * @param groundIds     the planned controllers and CAPCOM astronauts, by id
     * @return the ambitions at stake, people in roster order
     * @throws NullPointerException if an argument is {@code null}
     */
    public static List<AmbitionAtStake> ambitionsAtStake(PlayerSnapshot snapshot, MissionConfiguration configuration,
            java.util.Collection<String> crewIds, java.util.Collection<String> groundIds) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        boolean crewed = crewed(configuration);
        boolean orbital = crewed && orbital(configuration);
        boolean mayBeFirst = false;
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                mayBeFirst |= step.goalIdsAchieved().stream().anyMatch(goal -> !snapshot.hasAchievedOrCancelledGoal(goal));
            }
        }
        Set<String> astronauts = new HashSet<>();
        snapshot.getAstronauts().forEach(astronaut -> astronauts.add(astronaut.getId()));
        Set<String> controllers = new HashSet<>();
        snapshot.getMissionControlOfficers().forEach(officer -> controllers.add(officer.getId()));
        Set<String> aboard = new HashSet<>(crewIds);
        aboard.addAll(groundIds);
        List<AmbitionAtStake> stakes = new ArrayList<>();
        List<Personnel> people = new ArrayList<>(snapshot.getAstronauts());
        people.addAll(snapshot.getMissionControlOfficers());
        for (Personnel person : people) {
            Ambition ambition = person.getAmbition();
            if (ambition == null) {
                continue;
            }
            String id = person.getId();
            boolean applies;
            boolean mine;
            Set<String> role;
            Set<String> group;
            switch (ambition.kind()) {
                case FLY_IN_SPACE -> {
                    applies = crewed;
                    role = new HashSet<>(crewIds);
                    group = astronauts;
                }
                case FLY_TO_ORBIT -> {
                    applies = orbital;
                    role = new HashSet<>(crewIds);
                    group = astronauts;
                }
                case DAYS_IN_ORBIT -> {
                    applies = orbital && configuration.durationInSeasons() >= 2;
                    role = new HashSet<>(crewIds);
                    group = astronauts;
                }
                case JOINT_FLIGHT -> {
                    applies = crewed && configuration.launches().size() >= 2;
                    role = new HashSet<>(crewIds);
                    group = astronauts;
                }
                case BE_FIRST -> {
                    applies = mayBeFirst;
                    role = aboard;
                    group = astronauts;
                }
                case CONSOLES_ORBITAL -> {
                    applies = orbital;
                    role = new HashSet<>(groundIds);
                    group = controllers;
                }
                case CONSOLES_FIRST -> {
                    applies = mayBeFirst;
                    role = new HashSet<>(groundIds);
                    group = controllers;
                }
                default -> {
                    applies = false;
                    role = Set.of();
                    group = Set.of();
                }
            }
            if (!applies) {
                continue;
            }
            mine = role.contains(id);
            boolean ifFirst = ambition.kind() == Ambition.Kind.BE_FIRST || ambition.kind() == Ambition.Kind.CONSOLES_FIRST;
            if (mine) {
                stakes.add(new AmbitionAtStake(person, ambition.kind(), true, ifFirst));
            } else if (others(role, id, group)) {
                stakes.add(new AmbitionAtStake(person, ambition.kind(), false, ifFirst));
            }
        }
        return stakes;
    }

    private static void ambition(PlayerSnapshot snapshot, ContentCatalog catalog, GoalTally tally, Season season,
            Personnel person, int gameTurn, List<ResolutionEvent> out) {
        if (person.getDisappointedSeasons() > 0) {
            person.setDisappointedSeasons(person.getDisappointedSeasons() - 1);
            if (person.getDisappointedSeasons() == 0) {
                rollNext(snapshot, person, gameTurn);
            }
            return;
        }
        Ambition ambition = person.getAmbition();
        if (ambition == null) {
            if (person.isAmbitionPending() && person.getTenureSeasons() >= SAY_AFTER_SEASONS
                    && person.getTrainingKind() != de.mkoehler.launchwindow.model.TrainingKind.BASIC) {
                say(snapshot, catalog, tally, person, gameTurn, out);
            }
            return;
        }
        if (ambition.saidTurn() == gameTurn) {
            return;
        }
        if (fulfilled(snapshot, season, person, ambition)) {
            boolean ambitious = Personality.has(person, Trait.AMBITIOUS);
            settle(person, ambition, AmbitionSettled.Outcome.FULFILLED, FULFILLED_MORALE,
                ambitious ? FULFILLED_LOYALTY_AMBITIOUS : FULFILLED_LOYALTY, gameTurn, out);
            person.setAmbition(null);
            rollNext(snapshot, person, gameTurn);
        } else if (passedOver(season, person, ambition)) {
            Ambition shorter = ambition.shortened(2);
            settle(person, shorter, AmbitionSettled.Outcome.PASSED_OVER, PASSED_OVER_MORALE, PASSED_OVER_LOYALTY,
                gameTurn, out);
            if (shorter.seasonsLeft() <= 0) {
                runOut(person, shorter, gameTurn, out);
            } else {
                person.setAmbition(shorter);
            }
        } else if (!reachable(snapshot, catalog, tally, person, ambition.kind(), ambition.ref())) {
            settle(person, ambition, AmbitionSettled.Outcome.IMPOSSIBLE, IMPOSSIBLE_MORALE, IMPOSSIBLE_LOYALTY,
                gameTurn, out);
            person.setAmbition(null);
            rollNext(snapshot, person, gameTurn);
        } else {
            Ambition shorter = ambition.shortened(1);
            if (shorter.seasonsLeft() <= 0) {
                runOut(person, shorter, gameTurn, out);
            } else {
                person.setAmbition(shorter);
            }
        }
    }

    private static void runOut(Personnel person, Ambition ambition, int gameTurn, List<ResolutionEvent> out) {
        settle(person, ambition, AmbitionSettled.Outcome.RAN_OUT, RAN_OUT_MORALE, RAN_OUT_LOYALTY, gameTurn, out);
        person.setAmbition(null);
        person.setDisappointedSeasons(DISAPPOINTED_SEASONS);
    }

    private static void settle(Personnel person, Ambition ambition, AmbitionSettled.Outcome outcome, int morale,
            int loyalty, int gameTurn, List<ResolutionEvent> out) {
        person.setMorale(Math.clamp(person.getMorale() + morale, 0, 100));
        person.setLoyalty(Math.clamp(person.getLoyalty() + loyalty, 0, 100));
        person.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.AMBITION, ambition.kind().name(),
            ambition.ref(), outcome.name(), 0, ambition.seasonsLeft()));
        out.add(new AmbitionSettled(groupOf(person), person.getId(), person.getName(), ambition.kind(), ambition.ref(),
            outcome, ambition.seasonsLeft(), morale, loyalty));
    }

    private static void rollNext(PlayerSnapshot snapshot, Personnel person, int gameTurn) {
        int percent = Personality.has(person, Trait.AMBITIOUS) ? NEXT_PERCENT_AMBITIOUS : NEXT_PERCENT_OTHERS;
        SplittableRandom random = new SplittableRandom(Intelligence.seed(snapshot.getAgencyName(),
            person.getId() + "/next/" + gameTurn) ^ NEXT_SALT);
        person.setAmbitionPending(random.nextInt(100) < percent);
    }

    private static void say(PlayerSnapshot snapshot, ContentCatalog catalog, GoalTally tally, Personnel person,
            int gameTurn, List<ResolutionEvent> out) {
        SplittableRandom random = new SplittableRandom(Intelligence.seed(snapshot.getAgencyName(),
            person.getId() + "/say/" + gameTurn) ^ DRAW_SALT);
        List<Ambition.Kind> options = new ArrayList<>();
        Map<Ambition.Kind, String> refs = new HashMap<>();
        Map<Ambition.Kind, Integer> windows = new HashMap<>();
        for (Ambition.Kind kind : Ambition.Kind.forGroup(groupOf(person).name())) {
            String ref = componentFor(snapshot, catalog, person, kind, random);
            if (!reachable(snapshot, catalog, tally, person, kind, ref)) {
                continue;
            }
            int window = patience(person, kind);
            if (kind == Ambition.Kind.COMPONENT_CAP) {
                // Only a cap the agency's own best team can reach in time (user, 2026-10-23): the window
                // stretches to fit it, and a cap too far off isn't drawn.
                OptionalInt needed = seasonsToCap(snapshot, catalog, person, ref);
                if (needed.isEmpty() || needed.getAsInt() + CAP_SLACK > CAP_WINDOW_MAX) {
                    continue;
                }
                window = Math.max(window, needed.getAsInt() + CAP_SLACK);
            }
            options.add(kind);
            refs.put(kind, ref);
            windows.put(kind, window);
        }
        if (options.isEmpty()) {
            return;
        }
        Ambition.Kind kind = options.get(random.nextInt(options.size()));
        String ref = refs.get(kind);
        int window = windows.get(kind);
        Ambition ambition = new Ambition(kind, ref, window, window, gameTurn);
        person.setAmbition(ambition);
        person.setAmbitionPending(false);
        person.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.AMBITION, kind.name(), ref,
            AmbitionSettled.Outcome.SAID.name(), 0, window));
        out.add(new AmbitionSettled(groupOf(person), person.getId(), person.getName(), kind, ref,
            AmbitionSettled.Outcome.SAID, window, 0, 0));
    }

    /** The table's window for an ambition, shortened for Ambitious and lengthened for Steady. */
    private static int patience(Personnel person, Ambition.Kind kind) {
        int window = kind.window();
        if (Personality.has(person, Trait.AMBITIOUS)) {
            window = Math.max(WINDOW_MIN, window + WINDOW_AMBITIOUS);
        }
        if (Personality.has(person, Trait.STEADY)) {
            window += WINDOW_STEADY;
        }
        return window;
    }

    /**
     * Returns how many seasons, on average, the agency's best team would take to bring a component to
     * its R&amp;D cap: the engineer and the agency's three most skilled others in the component's skill,
     * busy or not, the best in the richest slot ({@link ResearchPlanning#project}).
     *
     * @return the seasons, empty if the team doesn't get there within the projection's horizon
     */
    static OptionalInt seasonsToCap(PlayerSnapshot snapshot, ContentCatalog catalog, Personnel person,
            String componentId) {
        if (!(person instanceof SetSpecialist engineer)) {
            return OptionalInt.empty();
        }
        MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
        Comparator<SetSpecialist> bestFirst = Comparator.comparingDouble(
            (SetSpecialist specialist) -> specialist.getSkill(component.associatedSkill())).reversed();
        List<SetSpecialist> team = new ArrayList<>(List.of(engineer));
        snapshot.getSetSpecialists().stream().filter(other -> !other.getId().equals(engineer.getId()))
            .sorted(bestFirst).limit(MissionComponent.RD_SLOT_COUNT - 1).forEach(team::add);
        team.sort(bestFirst);
        ResearchPlanning.Projection projection = ResearchPlanning.project(snapshot, component,
            snapshot.getMissionComponents().get(componentId).getCurrentReliability(),
            team.toArray(new SetSpecialist[MissionComponent.RD_SLOT_COUNT]));
        return projection.seasonsTo(projection.cap());
    }

    /**
     * Returns the component a "bring a component to its cap" ambition is about: the one the engineer
     * works on if it's below its cap and in reach, else a random owned one that is; {@code ""} for other
     * ambitions or none. In reach: the agency's best team gets there within {@link #CAP_WINDOW_MAX}
     * seasons, slack included.
     */
    private static String componentFor(PlayerSnapshot snapshot, ContentCatalog catalog, Personnel person,
            Ambition.Kind kind, SplittableRandom random) {
        if (kind != Ambition.Kind.COMPONENT_CAP) {
            return "";
        }
        if (person instanceof SetSpecialist engineer && engineer.getRdTargetComponentId() != null
                && belowCap(snapshot, catalog, engineer.getRdTargetComponentId())
                && inReach(snapshot, catalog, person, engineer.getRdTargetComponentId())) {
            return engineer.getRdTargetComponentId();
        }
        List<String> below = snapshot.getMissionComponents().keySet().stream()
            .filter(id -> belowCap(snapshot, catalog, id) && inReach(snapshot, catalog, person, id)).toList();
        return below.isEmpty() ? "" : below.get(random.nextInt(below.size()));
    }

    private static boolean inReach(PlayerSnapshot snapshot, ContentCatalog catalog, Personnel person,
            String componentId) {
        OptionalInt needed = seasonsToCap(snapshot, catalog, person, componentId);
        return needed.isPresent() && needed.getAsInt() + CAP_SLACK <= CAP_WINDOW_MAX;
    }

    private static boolean belowCap(PlayerSnapshot snapshot, ContentCatalog catalog, String componentId) {
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        MissionComponent component = catalog.missionComponent(componentId).orElse(null);
        return owned != null && owned.isActive() && component != null
            && owned.getCurrentReliability() < TraitEffects.effectiveCap(snapshot, component, List.of(), false);
    }

    private static boolean fulfilled(PlayerSnapshot snapshot, Season season, Personnel person, Ambition ambition) {
        String id = person.getId();
        return switch (ambition.kind()) {
            case FLY_IN_SPACE -> season.crewFlew.contains(id);
            case FLY_TO_ORBIT -> season.crewFlewOrbital.contains(id);
            case DAYS_IN_ORBIT -> season.crewDaysInOrbit.contains(id);
            case BE_FIRST -> season.aboardFirst.contains(id);
            case JOINT_FLIGHT -> season.crewJoint.contains(id);
            case CONSOLES_ORBITAL -> season.consolesOrbital.contains(id);
            case CONSOLES_FIRST -> season.consolesFirst.contains(id);
            case BECOME_EXPERT -> person instanceof MissionControlOfficer controller && season.experts.containsKey(id)
                && season.experts.get(id).equals(bestSkill(controller).name());
            case COMPONENT_CAP -> season.capped.getOrDefault(ambition.ref(), Set.of()).contains(id);
            case FIRST_HARDWARE -> worked(person).stream().anyMatch(season.firstComponents::contains);
            case TOP_ENGINEER -> person instanceof SetSpecialist engineer && top(snapshot, engineer);
        };
    }

    private static boolean passedOver(Season season, Personnel person, Ambition ambition) {
        String id = person.getId();
        return switch (ambition.kind()) {
            // A flight passes others over once: in its first season (its second, for days in orbit).
            case FLY_IN_SPACE -> others(season.crewLaunched, id, season.astronautIds);
            case FLY_TO_ORBIT -> others(season.crewLaunchedOrbital, id, season.astronautIds);
            case DAYS_IN_ORBIT -> others(season.crewSecondSeason, id, season.astronautIds);
            case BE_FIRST -> others(season.aboardFirst, id, season.astronautIds);
            case JOINT_FLIGHT -> others(season.crewLaunchedJoint, id, season.astronautIds);
            case CONSOLES_ORBITAL -> others(season.consolesLaunchedOrbital, id, season.controllerIds);
            case CONSOLES_FIRST -> others(season.consolesFirst, id, season.controllerIds);
            case COMPONENT_CAP -> season.capped.containsKey(ambition.ref());
            case BECOME_EXPERT, FIRST_HARDWARE, TOP_ENGINEER -> false;
        };
    }

    /** Whether someone of the same group, not this person, did it. */
    private static boolean others(Set<String> did, String id, Set<String> group) {
        for (String other : did) {
            if (!other.equals(id) && group.contains(other)) {
                return true;
            }
        }
        return false;
    }

    private static boolean reachable(PlayerSnapshot snapshot, ContentCatalog catalog, GoalTally tally,
            Personnel person, Ambition.Kind kind, String ref) {
        List<MissionConfiguration> open = catalog.missionConfigurations().stream()
            .filter(configuration -> snapshot.getOpenedProgramIds().contains(configuration.programId())).toList();
        return switch (kind) {
            case FLY_IN_SPACE -> open.stream().anyMatch(Careers::crewed);
            case FLY_TO_ORBIT, CONSOLES_ORBITAL -> open.stream().anyMatch(configuration -> crewed(configuration)
                && orbital(configuration));
            case DAYS_IN_ORBIT -> open.stream().anyMatch(configuration -> crewed(configuration) && orbital(configuration)
                && configuration.durationInSeasons() >= 2);
            case JOINT_FLIGHT -> open.stream().anyMatch(configuration -> crewed(configuration)
                && configuration.launches().size() >= 2);
            case BE_FIRST, CONSOLES_FIRST, FIRST_HARDWARE -> open.stream().anyMatch(configuration ->
                unreachedGoal(snapshot, tally, configuration));
            case BECOME_EXPERT -> person instanceof MissionControlOfficer controller
                && !controller.getExpertSkills().contains(bestSkill(controller));
            case COMPONENT_CAP -> !ref.isEmpty() && belowCap(snapshot, catalog, ref);
            case TOP_ENGINEER -> person instanceof SetSpecialist engineer && snapshot.getSetSpecialists().size() >= 3
                && !top(snapshot, engineer);
        };
    }

    private static boolean unreachedGoal(PlayerSnapshot snapshot, GoalTally tally, MissionConfiguration configuration) {
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (String goal : step.goalIdsAchieved()) {
                    if (tally.countOf(goal) == 0 && !snapshot.hasAchievedOrCancelledGoal(goal)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean crewed(MissionConfiguration configuration) {
        return configuration.launches().stream().anyMatch(launch -> launch.launchType().humanRated());
    }

    private static boolean orbital(MissionConfiguration configuration) {
        return configuration.launches().stream().anyMatch(launch -> launch.launchType() == LaunchType.LEO_HR
            || launch.launchType() == LaunchType.TLI_HR || launch.launchType() == LaunchType.TPI_HR);
    }

    private static MissionControlSkill bestSkill(MissionControlOfficer controller) {
        return controller.getSkills().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
            .orElse(MissionControlSkill.values()[0]);
    }

    private static SetSkill bestSkill(SetSpecialist engineer) {
        return engineer.getSkills().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
            .orElse(SetSkill.values()[0]);
    }

    private static boolean top(PlayerSnapshot snapshot, SetSpecialist engineer) {
        SetSkill skill = bestSkill(engineer);
        double own = engineer.getSkill(skill);
        return snapshot.getSetSpecialists().stream().filter(other -> other != engineer)
            .allMatch(other -> other.getSkill(skill) < own);
    }

    /** The components a person has worked on: past stints and the one running now. */
    private static Set<String> worked(Personnel person) {
        Set<String> components = new HashSet<>();
        person.getCareer().stream().filter(line -> line.kind() == CareerEntry.Kind.RD_STINT)
            .forEach(line -> components.add(line.ref()));
        if (person.getStintComponentId() != null) {
            components.add(person.getStintComponentId());
        }
        return components;
    }

    private static void trim(List<CareerEntry> career) {
        // The hiring line and every move between agencies stay: the page names where each line was.
        while (career.size() > CAREER_KEPT) {
            int oldest = 0;
            while (oldest < career.size() && (career.get(oldest).kind() == CareerEntry.Kind.HIRED
                    || career.get(oldest).kind() == CareerEntry.Kind.JOINED)) {
                oldest++;
            }
            career.remove(oldest < career.size() ? oldest : 0);
        }
    }

    private static List<Personnel> employees(PlayerSnapshot snapshot) {
        List<Personnel> everyone = new ArrayList<>();
        everyone.addAll(snapshot.getAstronauts());
        everyone.addAll(snapshot.getSetSpecialists());
        everyone.addAll(snapshot.getMissionControlOfficers());
        return everyone;
    }

    private static PersonnelGroup groupOf(Personnel person) {
        return person instanceof Astronaut ? PersonnelGroup.ASTRONAUT
            : person instanceof SetSpecialist ? PersonnelGroup.SET : PersonnelGroup.MISSION_CONTROL;
    }
}
