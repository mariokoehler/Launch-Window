package de.mkoehler.launchwindow.bot;

import de.mkoehler.launchwindow.actions.AssignResearchAction;
import de.mkoehler.launchwindow.actions.CloseProgramAction;
import de.mkoehler.launchwindow.actions.CloseRocketProgramAction;
import de.mkoehler.launchwindow.actions.ConvertPrestigeAction;
import de.mkoehler.launchwindow.actions.DecideAction;
import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.actions.LaunchMissionAction;
import de.mkoehler.launchwindow.actions.OpenProgramAction;
import de.mkoehler.launchwindow.actions.OpenRocketProgramAction;
import de.mkoehler.launchwindow.actions.QueueIntelOperationAction;
import de.mkoehler.launchwindow.actions.SetIntelligenceTargetAction;
import de.mkoehler.launchwindow.actions.StartTrainingAction;
import de.mkoehler.launchwindow.actions.UpgradeBuildingAction;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.BuildingUpgrades;
import de.mkoehler.launchwindow.rules.Intelligence;
import de.mkoehler.launchwindow.rules.MissionStaffing;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PrestigeConversion;
import de.mkoehler.launchwindow.rules.RdDecisions;
import de.mkoehler.launchwindow.rules.RocketPrograms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * <p>A filler player for play tests ({@code design.md} &sect;5, "Filler bots"): an agency the server plays
 * itself, which takes part rather than competes. It follows its plan ({@link BotPlans}) the simple way:
 * opens the next flight's program and the cheapest rocket that lifts it, hires and upgrades until it can
 * staff it, puts its engineers on the flight's hardware, sends idle people to training, and launches once
 * the hardware is about 70 % reliable. Now and then it runs an intelligence assessment on a rival, it
 * answers R&amp;D decisions at random (a hands-on puzzle is left to the team), and about one turn in ten
 * it doesn't log in at all.</p>
 *
 * <p>It never touches the state: it proposes one {@link PlayerAction} at a time, which the caller submits
 * through the same validation a player's goes through, then asks for the next one against the new
 * preview. A proposal is made once a turn, so one the rules refuse isn't tried again.</p>
 *
 * <p>Every number here is unconfirmed (proposed 2026-10-25).</p>
 *
 * @author Mario Koehler
 */
public final class FillerBot {

    /** The most actions it takes in one turn, a guard against a loop. */
    public static final int MAX_ACTIONS_PER_TURN = 60;
    /** The share of turns it skips, as a player who didn't log in, in percent. */
    static final int IDLE_TURN_PERCENT = 10;
    /** It launches once everything it flies is this reliable (or at its R&amp;D ceiling), give or take {@link #LAUNCH_JITTER}. */
    static final double LAUNCH_RELIABILITY = 70;
    /** How far a turn's launch bar wanders either way. */
    static final double LAUNCH_JITTER = 5;
    /** Its people stay home from training once the hardware is this close to the launch bar. */
    static final double CLOSE_TO_READY = 15;
    /** The engineers it keeps, as far as the SET Center holds them. */
    static final int SPECIALISTS = 6;
    /** The chance an idle astronaut or controller starts a course in a turn, in percent. */
    static final int TRAIN_PERCENT = 25;
    /** It runs an Analysis section above this much money, and closes it below {@link #INTEL_OFF_FUNDS}. */
    static final long INTEL_ON_FUNDS = 8_000;
    /** See {@link #INTEL_ON_FUNDS}. */
    static final long INTEL_OFF_FUNDS = 3_000;
    /** The chance it runs an assessment in a turn it can, in percent. */
    static final int INTEL_OPERATION_PERCENT = 35;

    private final ContentCatalog catalog;
    private final List<String> plan;

    /**
     * Creates a bot for one agency.
     *
     * @param catalog the game's pack
     * @param plan    the flights it works through, in order ({@link BotPlans#forPack})
     */
    public FillerBot(ContentCatalog catalog, List<String> plan) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.plan = List.copyOf(plan);
    }

    /**
     * Starts a turn.
     *
     * @param seed   the turn's seed, so a turn played again plays the same
     * @param rivals the other agencies' keys, for intelligence
     * @return the turn, to draw its actions from
     */
    public Turn turn(long seed, List<String> rivals) {
        return new Turn(seed, rivals);
    }

    /**
     * One turn's play: {@link #next} proposes the next action against the current preview.
     */
    public final class Turn {

        private final long seed;
        private final List<String> rivals;
        private final boolean idle;
        private final double launchAt;
        private final Set<String> tried = new HashSet<>();

        private Turn(long seed, List<String> rivals) {
            this.seed = seed;
            this.rivals = List.copyOf(rivals);
            SplittableRandom random = new SplittableRandom(seed);
            this.idle = random.nextInt(100) < IDLE_TURN_PERCENT;
            this.launchAt = LAUNCH_RELIABILITY - LAUNCH_JITTER + random.nextDouble() * 2 * LAUNCH_JITTER;
        }

        /**
         * Returns whether this is a turn it sits out (it still pays off a debt and answers a decision).
         *
         * @return whether it's idle
         */
        public boolean idle() {
            return idle;
        }

        /**
         * Proposes the next action, never one proposed before this turn.
         *
         * @param snapshot the agency's preview, with this turn's actions so far
         * @return the action, or {@code null} when it's done for the turn
         */
        public PlayerAction next(PlayerSnapshot snapshot) {
            for (Map.Entry<String, PlayerAction> proposal : proposals(snapshot).entrySet()) {
                if (tried.add(proposal.getKey())) {
                    PlayerAction action = proposal.getValue();
                    action.setSeed(new SplittableRandom(seed ^ proposal.getKey().hashCode()).nextLong());
                    return action;
                }
            }
            return null;
        }

        /** Everything it would do now, by a key that names it, most pressing first. */
        private Map<String, PlayerAction> proposals(PlayerSnapshot snapshot) {
            Map<String, PlayerAction> out = new LinkedHashMap<>();
            if (snapshot.getFunds() < 0) {
                long amount = PrestigeConversion.maxConversion(snapshot, catalog);
                if (amount > 0) {
                    out.put("convert", new ConvertPrestigeAction(amount));
                }
            }
            decide(snapshot, out);
            if (idle) {
                return out;
            }
            intelligenceLevel(snapshot, out);
            MissionConfiguration target = target(snapshot);
            if (target != null) {
                program(snapshot, target, out);
                MissionComponent rocket = cheapestRocketFor(snapshot, target);
                if (rocket != null && !isActive(snapshot, rocket.id())) {
                    rocket(snapshot, rocket, out);
                }
                staff(snapshot, target, out);
                research(snapshot, target, rocket, out);
                boolean ready = hardwareReady(snapshot, target, rocket, launchAt);
                if (ready && snapshot.getInProgressMissions().values().stream()
                        .noneMatch(mission -> mission.getMissionConfigurationId().equals(target.id()))) {
                    launch(snapshot, target, rocket, out);
                }
                if (!hardwareReady(snapshot, target, rocket, launchAt - CLOSE_TO_READY)) {
                    train(snapshot, out);
                }
            }
            intelligenceOperation(snapshot, out);
            return out;
        }

        /** An R&amp;D decision waiting: an option at random, never the hands-on puzzle; the default if that's refused. */
        private void decide(PlayerSnapshot snapshot, Map<String, PlayerAction> out) {
            PendingDecision pending = snapshot.getPendingDecision();
            if (pending == null || pending.decided()) {
                return;
            }
            Decisions.Decision decision = catalog.rules().decisions().byId(pending.decisionId()).orElse(null);
            MissionComponent component = catalog.missionComponent(pending.componentId()).orElse(null);
            if (decision == null || component == null) {
                return;
            }
            String play = decision.playOption().map(Decisions.Option::id).orElse(null);
            List<Decisions.Option> options = decision.options().stream()
                .filter(option -> !option.id().equals(play)).toList();
            Decisions.Option option = options.get(new SplittableRandom(seed).nextInt(options.size()));
            String person = null;
            String second = null;
            switch (option.takes()) {
                case OVERSEER -> person = idOf(RdDecisions.overseers(snapshot, component), 0);
                case SECONDED -> person = idOf(RdDecisions.team(snapshot, component.id()), 0);
                case PAIR -> {
                    List<SetSpecialist> people = RdDecisions.pairable(snapshot, component);
                    person = idOf(people, 0);
                    second = idOf(people, 1);
                }
                default -> { }
            }
            String key = "decide:" + pending.decisionId() + ":";
            out.put(key + option.id(), new DecideAction(option.id(), person, second));
            out.put(key + decision.defaultOption(), new DecideAction(decision.defaultOption(), null, null));
        }

        /** An Analysis section while the money lasts. */
        private void intelligenceLevel(PlayerSnapshot snapshot, Map<String, PlayerAction> out) {
            int level = snapshot.getIntelligence().getTargetLevel();
            if (rivals.isEmpty()) {
                return;
            }
            if (level < Intelligence.ASSESSMENT_LEVEL && snapshot.getFunds() > INTEL_ON_FUNDS) {
                out.put("intel-level:" + Intelligence.ASSESSMENT_LEVEL,
                    new SetIntelligenceTargetAction(Intelligence.ASSESSMENT_LEVEL));
            } else if (level > 0 && snapshot.getFunds() < INTEL_OFF_FUNDS) {
                out.put("intel-level:0", new SetIntelligenceTargetAction(0));
            }
        }

        /** Now and then, an assessment of a rival at random. */
        private void intelligenceOperation(PlayerSnapshot snapshot, Map<String, PlayerAction> out) {
            if (rivals.isEmpty() || snapshot.getIntelligence().getLevel() < Intelligence.ASSESSMENT_LEVEL
                    || Intelligence.actionsLeft(snapshot) == 0 || !chance("intel-op", INTEL_OPERATION_PERCENT)) {
                return;
            }
            SplittableRandom random = new SplittableRandom(seed ^ 0x1D7EL);
            String rival = rivals.get(random.nextInt(rivals.size()));
            IntelArea area = IntelArea.values()[random.nextInt(IntelArea.values().length)];
            out.put("intel-op", QueueIntelOperationAction.assessment(rival, area));
        }

        /** The target's program, making room in Headquarters first. */
        private void program(PlayerSnapshot snapshot, MissionConfiguration target, Map<String, PlayerAction> out) {
            if (snapshot.getOpenedProgramIds().contains(target.programId())) {
                return;
            }
            if (snapshot.getOpenedProgramIds().size()
                    >= BuildingUpgrades.capacity(snapshot, Building.HEADQUARTERS, catalog.buildings())) {
                Set<String> stillFlown = new HashSet<>();
                for (String id : plan.subList(plan.indexOf(target.id()), plan.size())) {
                    catalog.missionConfiguration(id).ifPresent(later -> stillFlown.add(later.programId()));
                }
                for (String programId : snapshot.getOpenedProgramIds()) {
                    if (!stillFlown.contains(programId)) {
                        out.put("close-program:" + programId, new CloseProgramAction(programId));
                    }
                }
                upgrade(snapshot, Building.HEADQUARTERS, out);
            }
            out.put("open-program:" + target.programId(), new OpenProgramAction(target.programId()));
        }

        /** The target's rocket, with a VAB that can build it and room in it. */
        private void rocket(PlayerSnapshot snapshot, MissionComponent rocket, Map<String, PlayerAction> out) {
            if (!RocketPrograms.classAllowed(rocket.rocket().rocketClass(),
                    snapshot.buildingLevel(Building.VEHICLE_ASSEMBLY))) {
                upgrade(snapshot, Building.VEHICLE_ASSEMBLY, out);
                return;
            }
            if (RocketPrograms.openCount(snapshot, catalog)
                    >= BuildingUpgrades.capacity(snapshot, Building.VEHICLE_ASSEMBLY, catalog.buildings())) {
                for (MissionComponent other : rockets()) {
                    if (!other.id().equals(rocket.id()) && isActive(snapshot, other.id())
                            && !RocketPrograms.inFlight(snapshot, other.id())) {
                        out.put("close-rocket:" + other.id(), new CloseRocketProgramAction(other.id()));
                    }
                }
            }
            out.put("open-rocket:" + rocket.id(), new OpenRocketProgramAction(rocket.id()));
        }

        /**
         * The people the target needs, one spare astronaut, and {@link #SPECIALISTS} engineers; and one more for
         * every seat "auto-assign" can't fill today beyond those away training or sick, who'll be back.
         */
        private void staff(PlayerSnapshot snapshot, MissionConfiguration target, Map<String, PlayerAction> out) {
            MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(snapshot, target);
            int astronautsShort = (int) target.usedCrewSlots().stream()
                .filter(slot -> staffing.crewSlotIds().get(slot) == null).count();
            int controllersShort = 0;
            for (int slot : target.usedControlSlots()) {
                if (staffing.controlSlotIds().get(slot) == null) {
                    if (target.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT) {
                        astronautsShort++;
                    } else {
                        controllersShort++;
                    }
                }
            }
            controllersShort = Math.max(0, controllersShort - away(snapshot.getMissionControlOfficers()));
            astronautsShort = Math.max(0, astronautsShort - away(snapshot.getAstronauts()));
            hire(snapshot, PersonnelGroup.MISSION_CONTROL, Building.MISSION_CONTROL,
                Math.max(target.requiredMissionControlOfficerCount(),
                    snapshot.getMissionControlOfficers().size() + controllersShort), out);
            hire(snapshot, PersonnelGroup.ASTRONAUT, Building.ASTRONAUT_CENTER,
                Math.max(target.requiredAstronautCount() + target.requiredCapcomCount() + 1,
                    snapshot.getAstronauts().size() + astronautsShort), out);
            hire(snapshot, PersonnelGroup.SET, null,
                Math.min(SPECIALISTS, BuildingUpgrades.capacity(snapshot, Building.SET_CENTER, catalog.buildings())),
                out);
        }

        private void hire(PlayerSnapshot snapshot, PersonnelGroup group, Building building, int wanted,
                Map<String, PlayerAction> out) {
            List<? extends Personnel> roster = switch (group) {
                case ASTRONAUT -> snapshot.getAstronauts();
                case SET -> snapshot.getSetSpecialists();
                case MISSION_CONTROL -> snapshot.getMissionControlOfficers();
            };
            if (roster.size() >= wanted) {
                return;
            }
            if (building != null && wanted > BuildingUpgrades.capacity(snapshot, building, catalog.buildings())) {
                upgrade(snapshot, building, out);
            }
            List<? extends Personnel> pool = switch (group) {
                case ASTRONAUT -> snapshot.getAstronautCandidates();
                case SET -> snapshot.getSetSpecialistCandidates();
                case MISSION_CONTROL -> snapshot.getMissionControlCandidates();
            };
            pool.stream().sorted(Comparator.comparingDouble(FillerBot::averageSkill).reversed())
                .limit(wanted - roster.size())
                .forEach(best -> out.put("hire:" + best.getId(), new HireAction(group, best.getId())));
        }

        private void upgrade(PlayerSnapshot snapshot, Building building, Map<String, PlayerAction> out) {
            if (!snapshot.getBuildingUpgradeSeasonsRemaining().containsKey(building)) {
                out.put("upgrade:" + building, new UpgradeBuildingAction(building));
            }
        }

        /** Every idle engineer onto the target's hardware still short of its ceiling, best skill first. */
        private void research(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent rocket,
                Map<String, PlayerAction> out) {
            List<String> components = flownComponents(target);
            if (rocket != null && isActive(snapshot, rocket.id())) {
                components.add(rocket.id());
            }
            for (String componentId : components) {
                OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
                MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
                if (owned == null || !owned.isActive() || owned.getCurrentReliability() >= component.maxRdReliability()
                        || !hasFreeSlot(snapshot, componentId)) {
                    continue;
                }
                snapshot.getSetSpecialists().stream().filter(FillerBot::available)
                    .max(Comparator.comparingDouble(specialist -> specialist.getSkill(component.associatedSkill())))
                    .ifPresent(best -> out.put("research:" + best.getId() + ":" + componentId,
                        new AssignResearchAction(best.getId(), componentId)));
            }
        }

        /** The target, staffed the way "auto-assign" would, if everyone's there and the money is. */
        private void launch(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent rocket,
                Map<String, PlayerAction> out) {
            MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(snapshot, target);
            boolean seated = target.usedCrewSlots().stream().allMatch(slot -> staffing.crewSlotIds().get(slot) != null)
                && target.usedControlSlots().stream().allMatch(slot -> staffing.controlSlotIds().get(slot) != null);
            if (!seated) {
                return;
            }
            // An air drop flies without a rocket: its slot holds "".
            List<String> rockets = target.launches().stream()
                .map(launch -> launch.launchType().needsRocket() ? rocket.id() : "").toList();
            out.put("launch:" + target.id(), new LaunchMissionAction(target.id(), staffing.crewSlotIds(),
                staffing.controlSlotIds(), rockets));
        }

        /** Some idle astronauts and controllers into a course on their weakest skill. */
        private void train(PlayerSnapshot snapshot, Map<String, PlayerAction> out) {
            for (Astronaut astronaut : snapshot.getAstronauts()) {
                if (available(astronaut) && chance("train:" + astronaut.getId(), TRAIN_PERCENT)) {
                    AstronautSkill weakest = Arrays.stream(AstronautSkill.values())
                        .min(Comparator.comparingDouble(astronaut::getSkill)).orElseThrow();
                    out.put("train:" + astronaut.getId(), new StartTrainingAction(PersonnelGroup.ASTRONAUT,
                        astronaut.getId(), TrainingKind.ADVANCED, weakest.name(), 0));
                }
            }
            for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
                if (available(officer) && chance("train:" + officer.getId(), TRAIN_PERCENT)) {
                    MissionControlSkill weakest = Arrays.stream(MissionControlSkill.values())
                        .min(Comparator.comparingDouble(officer::getSkill)).orElseThrow();
                    out.put("train:" + officer.getId(), new StartTrainingAction(PersonnelGroup.MISSION_CONTROL,
                        officer.getId(), TrainingKind.ADVANCED, weakest.name(), 0));
                }
            }
        }

        /** A roll that comes out the same however often it's asked this turn. */
        private boolean chance(String key, int percent) {
            return new SplittableRandom(seed ^ ((long) key.hashCode() << 17)).nextInt(100) < percent;
        }
    }

    /**
     * Returns the flight it works on: the first in its plan with a goal of its own still open.
     *
     * @param snapshot the agency
     * @return the flight, or {@code null} once the plan is flown
     */
    public MissionConfiguration target(PlayerSnapshot snapshot) {
        for (String id : plan) {
            MissionConfiguration configuration = catalog.missionConfiguration(id).orElse(null);
            if (configuration != null && configuration.seasons().stream().flatMap(season -> season.steps().stream())
                    .flatMap(step -> step.goalIdsAchieved().stream())
                    .anyMatch(goal -> !snapshot.hasAchievedOrCancelledGoal(goal))) {
                return configuration;
            }
        }
        return null;
    }

    /** The cheapest rocket open to the agency that lifts every launch of the flight, or {@code null}. */
    private MissionComponent cheapestRocketFor(PlayerSnapshot snapshot, MissionConfiguration configuration) {
        MissionComponent best = null;
        for (MissionComponent rocket : rockets()) {
            if (!rocket.availableTo(snapshot.getArchetype())) {
                continue;
            }
            boolean liftsAll = configuration.launches().stream().filter(launch -> launch.launchType().needsRocket())
                .allMatch(launch -> {
                    double capacity = rocket.payloadCapacity(launch.launchType());
                    return capacity > 0 && capacity >= requiredLift(launch);
                });
            if (liftsAll && (best == null || rocket.unitCost() < best.unitCost())) {
                best = rocket;
            }
        }
        return best;
    }

    /** The payloads' mass less their own lift. */
    private double requiredLift(MissionConfiguration.Launch launch) {
        double mass = 0;
        for (String id : launch.payloadComponentIds()) {
            MissionComponent payload = catalog.missionComponent(id).orElseThrow();
            mass += payload.mass() - (payload.ownLift() == null ? 0 : switch (launch.launchType()) {
                case SUBORBITAL, SUBORBITAL_HR -> payload.ownLift().payloadToSuborbital();
                case LEO, LEO_HR -> payload.ownLift().payloadToLeo();
                case TLI, TLI_HR -> payload.ownLift().payloadToTli();
                case TPI, TPI_HR -> payload.ownLift().payloadToTpi();
                case AIR_DROP -> 0;
            });
        }
        return Math.max(0, mass);
    }

    /** Whether everything the flight flies, its rocket too if it needs one, is at the bar or its ceiling. */
    private boolean hardwareReady(PlayerSnapshot snapshot, MissionConfiguration configuration,
            MissionComponent rocket, double bar) {
        List<String> flown = flownComponents(configuration);
        if (configuration.launches().stream().anyMatch(launch -> launch.launchType().needsRocket())) {
            if (rocket == null || !isActive(snapshot, rocket.id())) {
                return false;
            }
            flown.add(rocket.id());
        }
        for (String componentId : flown) {
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
            MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
            if (owned == null || owned.getCurrentReliability() < Math.min(bar, component.maxRdReliability() - 0.5)) {
                return false;
            }
        }
        return true;
    }

    /** The components a flight flies, from its steps, without its rockets. */
    private static List<String> flownComponents(MissionConfiguration configuration) {
        List<String> flown = new ArrayList<>();
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                    String id = involvement.componentId();
                    if (!id.startsWith(MissionConfiguration.ROCKET_SLOT_PREFIX) && !flown.contains(id)) {
                        flown.add(id);
                    }
                }
            }
        }
        return flown;
    }

    private List<MissionComponent> rockets() {
        return catalog.missionComponents().stream().filter(MissionComponent::hasRocketSpec).toList();
    }

    private static boolean isActive(PlayerSnapshot snapshot, String componentId) {
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        return owned != null && owned.isActive();
    }

    private static boolean hasFreeSlot(PlayerSnapshot snapshot, String componentId) {
        long taken = snapshot.getSetSpecialists().stream()
            .filter(specialist -> componentId.equals(specialist.getRdTargetComponentId())).count();
        return taken < MissionComponent.RD_SLOT_COUNT;
    }

    /** The people of a roster away for now: in training, on PR duty, or sick, but not on a flight. */
    private static int away(List<? extends Personnel> roster) {
        return (int) roster.stream().filter(person -> person.getAssignmentState() == AssignmentState.IN_TRAINING
            || person.getAssignmentState() == AssignmentState.PR_DUTY
            || person.getAssignmentState() == AssignmentState.IDLE && person.getUnavailableSeasons() > 0).count();
    }

    private static boolean available(Personnel person) {
        return person.getAssignmentState() == AssignmentState.IDLE && person.getUnavailableSeasons() == 0;
    }

    private static String idOf(List<? extends Personnel> people, int index) {
        return people.size() > index ? people.get(index).getId() : null;
    }

    private static double averageSkill(Personnel person) {
        return switch (person) {
            case Astronaut a -> Arrays.stream(AstronautSkill.values()).mapToDouble(a::getSkill).average().orElse(0);
            case MissionControlOfficer m -> Arrays.stream(MissionControlSkill.values()).mapToDouble(m::getSkill)
                .average().orElse(0);
            case SetSpecialist s -> Arrays.stream(SetSkill.values()).mapToDouble(s::getSkill).average().orElse(0);
            default -> 0;
        };
    }
}
