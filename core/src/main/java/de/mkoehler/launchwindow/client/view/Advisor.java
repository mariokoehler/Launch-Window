package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.List;
import java.util.function.Predicate;

/**
 * The advisor ({@code ui-spec.md} component 24): a new agency's first things to do, then the road ahead as
 * the programs open in stages (the user's idea, 2026-10-26: the next capsule, the rocket plane meanwhile, a
 * probe to the Moon, the bigger capsules, the Moon), shown on a card on the hub. Each item ticks itself off
 * from the player's state, never from a click, so it works the same on every machine; only "open the help"
 * and "read the turn report" come from this client's own marks. The player can't hide it, only minimize it
 * (user, 2026-10-26: a new player could hide it by accident and lose the help that comes later). Once nothing
 * is left, the client retires it for good.
 *
 * @author Mario Koehler
 */
public final class Advisor {

    /** How many open items the card shows at once, so it stays a nudge rather than a chore list. */
    public static final int SHOWN = 3;
    /**
     * The reliability (percent) a program's payload and a rocket should both reach before the
     * advisor suggests the first mission (the user's number, 2026-10-08): fresh hardware starts
     * at a few percent and would most likely fail.
     */
    public static final double READY_RELIABILITY = de.mkoehler.launchwindow.rules.RdDecisions.READY_RELIABILITY;

    private Advisor() {
    }

    /** Where an item's "Go" link leads. */
    public enum Destination {
        /** Opens the help itself. */
        HELP,
        /** The SET Centre. */
        SET_CENTRE,
        /** Headquarters. */
        HEADQUARTERS,
        /** The VAB. */
        VAB,
        /** The Payload R&amp;D board. */
        RND,
        /** The Rocket R&amp;D board. */
        ROCKET_RND,
        /** Mission Control. */
        MISSION_CONTROL,
        /** The Astronaut Complex. */
        ASTRONAUT_COMPLEX,
        /** The latest turn report. */
        TURN_REPORT,
        /** The roads to the Moon compared (ui-spec.md 4.21). */
        ROADS_TO_THE_MOON
    }

    /** Where an item stands. */
    public enum Status {
        /** To do now: the card shows it. */
        OPEN,
        /** Not yet possible (the first turn report before the first turn resolves). */
        LATER,
        /** Done. */
        DONE,
        /** Not needed by this agency so far (astronauts before any crewed program). */
        NOT_NEEDED
    }

    /**
     * What the advisor needs beyond the player's state: this client's own marks.
     *
     * @param helpOpened     whether the player has opened the help
     * @param reportResolved whether a turn has resolved, so there's a report to read
     * @param reportRead     whether the player has read a turn report
     */
    public record Marks(boolean helpOpened, boolean reportResolved, boolean reportRead) {
    }

    /**
     * One thing to do.
     *
     * @param id          its id
     * @param title       its title, an order to the player
     * @param text        a line on why and how
     * @param go          the "Go" link's label
     * @param destination where the link leads
     * @param topicIds    the help topics its "?" opens on
     */
    public record Item(String id, String title, String text, String go, Destination destination,
                       List<String> topicIds) {
    }

    /** Every item, in the order a new agency should do them. */
    public static final List<Item> ITEMS = List.of(
        new Item("help", "Open the help",
            "Every rule, by topic. F1 opens it on any screen, on the topics for that screen.",
            "Open help", Destination.HELP, List.of("turns", "buildings")),
        new Item("hire-engineers", "Hire engineers",
            "Your agency starts with nobody. Engineers at the SET Centre build and improve your hardware.",
            "SET Centre", Destination.SET_CENTRE, List.of("hiring", "salaries")),
        new Item("open-program", "Open a program",
            "Headquarters opens programs. A satellite is the cheapest way to start.",
            "Headquarters", Destination.HEADQUARTERS, List.of("programs", "budget")),
        new Item("open-rocket", "Open a rocket program",
            "Every mission needs a rocket. The VAB opens one; pick one that lifts your program's payload.",
            "VAB", Destination.VAB, List.of("rockets")),
        new Item("assign-rnd", "Put engineers on R&D",
            "New hardware is unreliable. Engineers on R&D raise its reliability every season.",
            "R&D board", Destination.RND, List.of("rnd", "chemistry")),
        new Item("hire-controllers", "Hire flight controllers",
            "Every mission is flown from Mission Control's consoles, uncrewed ones too.",
            "Mission Control", Destination.MISSION_CONTROL, List.of("mission-control", "hiring")),
        new Item("hire-astronauts", "Hire astronauts",
            "Your crewed program needs a crew, and a CAPCOM on the ground.",
            "Astronaut Complex", Destination.ASTRONAUT_COMPLEX, List.of("astronauts", "training")),
        new Item("raise-reliability", "Raise reliability to " + whole(READY_RELIABILITY) + "\u00A0%",
            "Fresh hardware would most likely fail. Keep engineers on R&D until a program's payload and a "
                + "rocket both reach " + whole(READY_RELIABILITY) + "\u00A0%.",
            "R&D board", Destination.RND, List.of("rnd", "mission-steps")),
        new Item("plan-mission", "Plan your first mission",
            "Pick a mission in an open program, then its rocket, crew and consoles. It launches when the turn "
                + "resolves.",
            "Headquarters", Destination.HEADQUARTERS, List.of("planning-missions", "mission-steps")),
        new Item("read-report", "Read your first turn report",
            "What happened while you were away: launches, R&D, people and the news.",
            "Turn report", Destination.TURN_REPORT, List.of("turn-report")));

    /** The categories of the bigger capsules, which a person in space opens. */
    private static final List<String> BIGGER_CAPSULES = List.of("two-crew-capsules", "three-crew-capsules");
    /** The one-crew capsules' category. */
    private static final List<String> ONE_CREW = List.of("one-crew-capsules");
    /** The rocket planes' category. */
    private static final List<String> SPACE_PLANES = List.of(de.mkoehler.launchwindow.rules.RocketPlaneWings.CATEGORY);
    /** The lunar probes' category. */
    private static final List<String> LUNAR_PROBES = List.of("lunar-probes");
    /** The crewed Moon programs' category. */
    private static final List<String> MOON = List.of("lunar-expeditions");

    /**
     * The road ahead, after the first steps (the user's idea, 2026-10-26, {@code design.md} &sect;2.3): each
     * item opens when its programs do, in the order the progression opens them, and goes once the player
     * has acted or moved past it. {@code {capsule}} and {@code {plane}} stand for the agency's own programs
     * ({@link #title}, {@link #text}).
     */
    public static final List<Item> ROAD_AHEAD = List.of(
        new Item("next-crew", "Start on a person in space",
            "Your satellite opened {capsule}, the capsule that puts a person in space. Its R&D is long: open it "
                + "now.", "Headquarters", Destination.HEADQUARTERS, List.of("programs", "astronauts")),
        new Item("rocket-plane", "Fly the {plane} meanwhile",
            "A rocket plane is cheap and quick while the capsule is in R&D: prestige, contracts, and every pilot "
                + "who lands it earns rocket-plane wings, which make their capsule flights safer.",
            "Headquarters", Destination.HEADQUARTERS, List.of("programs", "astronauts")),
        new Item("lunar-probe", "Send a probe to the Moon",
            "The Moon programs open with a probe at the Moon and a person in orbit. Probes are cheap: fly one "
                + "while the capsule gets ready.", "Headquarters", Destination.HEADQUARTERS, List.of("programs")),
        new Item("bigger-capsules", "Choose your road to the Moon",
            "A person in space opened the bigger capsules and the Moon craft's tests in Earth orbit. Opening a "
                + "Moon craft's tests usually makes its Moon flights free to open later.",
            "Compare the roads", Destination.ROADS_TO_THE_MOON, List.of("roads-to-the-moon", "programs")),
        new Item("moon", "Go to the Moon",
            "A probe at the Moon and a person in orbit opened the Moon programs. A crew flies round the Moon "
                + "before it orbits it, and orbits it before it lands.",
            "Headquarters", Destination.HEADQUARTERS, List.of("programs", "mission-steps")));

    /** The first steps, then the road ahead. */
    private static final List<Item> ALL = java.util.stream.Stream.concat(ITEMS.stream(), ROAD_AHEAD.stream()).toList();

    /**
     * A one-off tip apart from the first steps (the user's idea, 2026-10-12): the first time someone
     * out of basic training sits idle once the agency is set up, the card says so, even after the
     * first steps are done. Idle people lose morale every season and resign at the floor, and new
     * players don't know that training is free. It never counts toward {@link #finished}.
     */
    public static final Item IDLE_TIP = new Item("idle-staff", "Keep idle people busy",
        "Anyone idle loses " + de.mkoehler.launchwindow.rules.PersonnelLifecycle.IDLE_MORALE_LOSS_PER_SEASON
            + " morale a season and resigns at " + de.mkoehler.launchwindow.rules.PersonnelLifecycle.RESIGNATION_MORALE_FLOOR
            + ". Training is free and raises skills: send whoever waits for work to training, or put engineers on R&D.",
        "Their building", Destination.SET_CENTRE, List.of("morale", "training"));

    /**
     * Returns whether the idle tip is due now: the agency is past its first steps (someone is on R&amp;D
     * or a mission has flown), someone out of basic training is idle, and nobody is in advanced or
     * permanent training yet (a player who trains people already knows; one who starts has acted on it).
     *
     * @param state the player's state
     * @return whether it's due
     */
    public static boolean idleTipDue(PlayerSnapshot state) {
        boolean setUp = flown(state) || state.getSetSpecialists().stream()
            .map(SetSpecialist::getRdTargetComponentId).anyMatch(id -> id != null);
        boolean training = java.util.stream.Stream.of(state.getAstronauts(), state.getSetSpecialists(),
                state.getMissionControlOfficers()).flatMap(List::stream)
            .anyMatch(person -> person.getTrainingKind() == TrainingKind.ADVANCED
                || person.getTrainingKind() == TrainingKind.PERMANENT);
        return setUp && !training && idleDestination(state) != null;
    }

    /** The building of the first group with someone idle, engineers first, or {@code null} if nobody is. */
    private static Destination idleDestination(PlayerSnapshot state) {
        if (state.getSetSpecialists().stream().anyMatch(Advisor::idle)) {
            return Destination.SET_CENTRE;
        }
        if (state.getAstronauts().stream().anyMatch(Advisor::idle)) {
            return Destination.ASTRONAUT_COMPLEX;
        }
        if (state.getMissionControlOfficers().stream().anyMatch(Advisor::idle)) {
            return Destination.MISSION_CONTROL;
        }
        return null;
    }

    private static boolean idle(de.mkoehler.launchwindow.model.Personnel person) {
        return person.getAssignmentState() == de.mkoehler.launchwindow.model.AssignmentState.IDLE
            && person.getUnavailableSeasons() == 0;
    }

    /**
     * Returns where an item stands.
     *
     * @param item    the item
     * @param state   the player's state
     * @param catalog the content pack
     * @param marks   this client's marks
     * @return its status
     */
    public static Status status(Item item, PlayerSnapshot state, ContentCatalog catalog, Marks marks) {
        return switch (item.id()) {
            case "help" -> done(marks.helpOpened());
            case "hire-engineers" -> done(!state.getSetSpecialists().isEmpty());
            // A program, a rocket and R&D cost money or need an engineer who can work: they wait until
            // the first engineer is out of basic training (play test, 2026-10-09).
            case "open-program" -> !Programs.open(state, catalog).isEmpty() ? Status.DONE
                : engineerAvailable(state) ? Status.OPEN : Status.LATER;
            case "open-rocket" -> !Rockets.open(state, catalog).isEmpty() ? Status.DONE
                : engineerAvailable(state) ? Status.OPEN : Status.LATER;
            case "assign-rnd" -> state.getSetSpecialists().stream().map(SetSpecialist::getRdTargetComponentId)
                .anyMatch(id -> id != null) ? Status.DONE : engineerAvailable(state) ? Status.OPEN : Status.LATER;
            case "hire-controllers" -> !state.getMissionControlOfficers().isEmpty() ? Status.DONE
                : openProgramNeeds(state, catalog, configuration -> Missions.controllerCount(configuration) > 0)
                ? Status.OPEN : Status.NOT_NEEDED;
            case "hire-astronauts" -> !state.getAstronauts().isEmpty() ? Status.DONE
                : openProgramNeeds(state, catalog, configuration -> Missions.crewCount(configuration) > 0
                    || Missions.capcomCount(configuration) > 0) ? Status.OPEN : Status.NOT_NEEDED;
            // Only once someone is on R&D is there reliability to raise.
            case "raise-reliability" -> flown(state) || ready(state, catalog) ? Status.DONE
                : state.getSetSpecialists().stream().map(SetSpecialist::getRdTargetComponentId).anyMatch(id -> id != null)
                ? Status.OPEN : Status.LATER;
            // Held back until the hardware is ready; "raise reliability" stands in for it meanwhile.
            case "plan-mission" -> flown(state) ? Status.DONE : ready(state, catalog) ? Status.OPEN
                : Status.NOT_NEEDED;
            case "read-report" -> marks.reportRead() ? Status.DONE : marks.reportResolved() ? Status.OPEN
                : Status.LATER;
            // The road ahead. The first two wait for a first flight too, as in a pack without the gates.
            case "next-crew" -> !exists(state, catalog, ONE_CREW) ? Status.NOT_NEEDED
                : opened(state, catalog, ONE_CREW) || opened(state, catalog, BIGGER_CAPSULES) ? Status.DONE
                : available(state, catalog, ONE_CREW) && flown(state) ? Status.OPEN : Status.LATER;
            case "rocket-plane" -> !exists(state, catalog, SPACE_PLANES) ? Status.NOT_NEEDED
                : opened(state, catalog, SPACE_PLANES) || opened(state, catalog, BIGGER_CAPSULES)
                    || state.getAstronauts().stream().anyMatch(Astronaut::isRocketPlaneWings) ? Status.DONE
                : available(state, catalog, SPACE_PLANES) && flown(state) ? Status.OPEN : Status.LATER;
            case "lunar-probe" -> !exists(state, catalog, LUNAR_PROBES) ? Status.NOT_NEEDED
                : opened(state, catalog, LUNAR_PROBES) || opened(state, catalog, MOON) ? Status.DONE
                : available(state, catalog, LUNAR_PROBES)
                    && (opened(state, catalog, ONE_CREW) || opened(state, catalog, BIGGER_CAPSULES))
                ? Status.OPEN : Status.LATER;
            case "bigger-capsules" -> !exists(state, catalog, BIGGER_CAPSULES) ? Status.NOT_NEEDED
                : opened(state, catalog, BIGGER_CAPSULES) ? Status.DONE
                : available(state, catalog, BIGGER_CAPSULES) ? Status.OPEN : Status.LATER;
            case "moon" -> !exists(state, catalog, MOON) ? Status.NOT_NEEDED
                : opened(state, catalog, MOON) ? Status.DONE
                : available(state, catalog, MOON) ? Status.OPEN : Status.LATER;
            default -> throw new IllegalArgumentException("Unknown advisor item: " + item.id());
        };
    }

    /**
     * Returns whether an engineer is on the books and out of basic training, so a program or rocket
     * opened now could be worked on at once.
     *
     * @param state the player's state
     * @return whether one is
     */
    static boolean engineerAvailable(PlayerSnapshot state) {
        return state.getSetSpecialists().stream().anyMatch(engineer -> engineer.getTrainingKind() != TrainingKind.BASIC);
    }

    /**
     * Returns when a waiting item can be done, for the card: "Once the turn resolves", or once the
     * first engineers finish basic training.
     *
     * @param item  the item
     * @param state the player's state
     * @return the phrase, starting with "Once"
     */
    public static String waitingFor(Item item, PlayerSnapshot state) {
        return switch (item.id()) {
            case "open-program", "open-rocket", "assign-rnd" -> "Once your first engineers finish basic training";
            case "raise-reliability" -> "Once engineers are on R&D";
            case "next-crew", "rocket-plane" -> "Once your first satellite reaches orbit";
            case "lunar-probe" -> "Once your capsule program is open";
            case "bigger-capsules" -> "Once a person has flown in space";
            case "moon" -> "Once a probe reaches the Moon and a person orbits the Earth";
            default -> "Once the turn resolves";
        };
    }

    /**
     * Returns the items to do now, in order.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @param marks   this client's marks
     * @return the open items
     */
    public static List<Item> open(PlayerSnapshot state, ContentCatalog catalog, Marks marks) {
        return ALL.stream().filter(item -> status(item, state, catalog, marks) == Status.OPEN).toList();
    }

    /**
     * Returns the items that can't be done yet, in order: the card names them once nothing else
     * is open.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @param marks   this client's marks
     * @return the waiting items
     */
    public static List<Item> later(PlayerSnapshot state, ContentCatalog catalog, Marks marks) {
        return ALL.stream().filter(item -> status(item, state, catalog, marks) == Status.LATER).toList();
    }

    /**
     * Returns whether the advisor has nothing left to say, now or later: the client then retires it
     * for good.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @param marks   this client's marks
     * @return whether it's finished
     */
    public static boolean finished(PlayerSnapshot state, ContentCatalog catalog, Marks marks) {
        return open(state, catalog, marks).isEmpty() && later(state, catalog, marks).isEmpty();
    }

    /**
     * Returns whether every first step is done (or not needed), so the card speaks of the road ahead.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @param marks   this client's marks
     * @return whether they are
     */
    public static boolean firstStepsDone(PlayerSnapshot state, ContentCatalog catalog, Marks marks) {
        return ITEMS.stream().map(item -> status(item, state, catalog, marks))
            .allMatch(status -> status == Status.DONE || status == Status.NOT_NEEDED);
    }

    /**
     * Returns an item's title, with the agency's own program for a placeholder.
     *
     * @param item    the item
     * @param state   the player's state
     * @param catalog the content pack
     * @return the title
     */
    public static String title(Item item, PlayerSnapshot state, ContentCatalog catalog) {
        return named(item.title(), state, catalog);
    }

    /**
     * Returns an item's line, with the agency's own programs for the placeholders.
     *
     * @param item    the item
     * @param state   the player's state
     * @param catalog the content pack
     * @return the line
     */
    public static String text(Item item, PlayerSnapshot state, ContentCatalog catalog) {
        return named(item.text(), state, catalog);
    }

    private static String named(String text, PlayerSnapshot state, ContentCatalog catalog) {
        return text.replace("{capsule}", firstName(state, catalog, ONE_CREW, "a one-crew capsule"))
            .replace("{plane}", firstName(state, catalog, SPACE_PLANES, "rocket plane"));
    }

    /** The name of the agency's first program in the categories, or the fallback. */
    private static String firstName(PlayerSnapshot state, ContentCatalog catalog, List<String> categories,
                                    String fallback) {
        return Programs.visible(state, catalog).stream().filter(program -> categories.contains(program.categoryId()))
            .findFirst().map(program -> ContentNames.of(program.id())).orElse(fallback);
    }

    private static boolean exists(PlayerSnapshot state, ContentCatalog catalog, List<String> categories) {
        return Programs.visible(state, catalog).stream().anyMatch(program -> categories.contains(program.categoryId()));
    }

    private static boolean opened(PlayerSnapshot state, ContentCatalog catalog, List<String> categories) {
        return Programs.open(state, catalog).stream().anyMatch(program -> categories.contains(program.categoryId()));
    }

    private static boolean available(PlayerSnapshot state, ContentCatalog catalog, List<String> categories) {
        return Programs.visible(state, catalog).stream().anyMatch(program -> categories.contains(program.categoryId())
            && Programs.state(program, state) == Programs.State.AVAILABLE);
    }

    /**
     * Returns an item's line on where things stand now, or none: for "raise reliability", the best
     * open program's payload and the best open rocket.
     *
     * @param item    the item
     * @param state   the player's state
     * @param catalog the content pack
     * @return the line, or {@code null}
     */
    public static String progress(Item item, PlayerSnapshot state, ContentCatalog catalog) {
        if (!item.id().equals("raise-reliability")) {
            return null;
        }
        double payload = bestPayload(state, catalog);
        double rocket = bestRocket(state, catalog);
        return "Now: payload " + (payload < 0 ? "none open" : whole(payload) + "\u00A0%") + ", rocket "
            + (rocket < 0 ? "none open" : whole(rocket) + "\u00A0%") + ".";
    }

    /**
     * Returns where an item's "Go" link leads now: for "raise reliability", the R&amp;D board of
     * whichever is further behind.
     *
     * @param item    the item
     * @param state   the player's state
     * @param catalog the content pack
     * @return the destination
     */
    public static Destination destination(Item item, PlayerSnapshot state, ContentCatalog catalog) {
        if (item == IDLE_TIP) {
            Destination idle = idleDestination(state);
            return idle == null ? item.destination() : idle;
        }
        if (item.id().equals("raise-reliability")) {
            double payload = bestPayload(state, catalog);
            double rocket = bestRocket(state, catalog);
            return payload >= 0 && rocket < payload ? Destination.ROCKET_RND : Destination.RND;
        }
        return item.destination();
    }

    /**
     * Returns whether a program's payload and a rocket are both reliable enough for a first
     * mission.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return whether they are
     */
    public static boolean ready(PlayerSnapshot state, ContentCatalog catalog) {
        return bestPayload(state, catalog) >= READY_RELIABILITY && bestRocket(state, catalog) >= READY_RELIABILITY;
    }

    /** A flight that ended leaves no in-progress mission, so flown ones count too. */
    private static boolean flown(PlayerSnapshot state) {
        return !state.getInProgressMissions().isEmpty() || state.getLastFlightSeasonTurn() >= 0
            || state.getLastFailureTurn() >= 0 || !state.getSuccessfulFlightCounts().isEmpty();
    }

    /** The open program whose least reliable payload component is best: that reliability, or -1. */
    private static double bestPayload(PlayerSnapshot state, ContentCatalog catalog) {
        double best = -1;
        for (Program program : Programs.open(state, catalog)) {
            double least = Double.MAX_VALUE;
            for (String id : program.requiredMissionComponentIds()) {
                OwnedMissionComponent owned = state.getMissionComponents().get(id);
                least = Math.min(least, owned == null || !owned.isActive() ? 0 : owned.getCurrentReliability());
            }
            if (least != Double.MAX_VALUE) {
                best = Math.max(best, least);
            }
        }
        return best;
    }

    /** The most reliable open rocket's reliability, or -1. */
    private static double bestRocket(PlayerSnapshot state, ContentCatalog catalog) {
        double best = -1;
        for (MissionComponent rocket : Rockets.open(state, catalog)) {
            best = Math.max(best, state.getMissionComponents().get(rocket.id()).getCurrentReliability());
        }
        return best;
    }

    private static String whole(double percent) {
        return String.valueOf((int) Math.floor(percent));
    }

    private static Status done(boolean done) {
        return done ? Status.DONE : Status.OPEN;
    }

    private static boolean openProgramNeeds(PlayerSnapshot state, ContentCatalog catalog,
                                            Predicate<MissionConfiguration> needs) {
        for (Program program : Programs.open(state, catalog)) {
            if (Missions.of(program, catalog).stream().anyMatch(needs)) {
                return true;
            }
        }
        return false;
    }
}
