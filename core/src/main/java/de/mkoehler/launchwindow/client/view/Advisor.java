package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.List;
import java.util.function.Predicate;

/**
 * The advisor ({@code ui-spec.md} component 24): a new agency's first things to do, shown on a card
 * on the hub until they're done. Each item ticks itself off from the player's state, never from a
 * click, so it works the same on every machine; only "open the help" and "read the turn report"
 * come from this client's own marks. Once nothing is left, the client retires the advisor for good.
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
        TURN_REPORT
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
        return ITEMS.stream().filter(item -> status(item, state, catalog, marks) == Status.OPEN).toList();
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
        return ITEMS.stream().filter(item -> status(item, state, catalog, marks) == Status.LATER).toList();
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
