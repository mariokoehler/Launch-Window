package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.LaunchType;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionPrestige;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Headquarters' mission planning view logic ({@code ui-spec.md} &sect;4.6.5 to &sect;4.6.8): a
 * program's configurations, their names and counts, the prestige of the next flight, and the
 * rockets that can fly each launch. The verdicts are {@code core}'s ({@link MissionLaunch},
 * {@link MissionPrestige}); this class only words them.
 *
 * @author Mario Koehler
 */
public final class Missions {

    private Missions() {
    }

    /**
     * Returns a program's configurations, in the content pack's order.
     *
     * @param program the program
     * @param catalog the content pack
     * @return the configurations
     */
    public static List<MissionConfiguration> of(Program program, ContentCatalog catalog) {
        return catalog.missionConfigurations().stream()
            .filter(configuration -> configuration.programId().equals(program.id()))
            .toList();
    }

    /**
     * Names a configuration within its program ("Orbital flight" for {@code vostok-orbital}), from the
     * pack's text, or from its id if the pack has none.
     *
     * @param configuration the configuration
     * @return the name
     */
    public static String name(MissionConfiguration configuration) {
        String own = ContentNames.configuration(configuration.id());
        if (own != null) {
            return own;
        }
        String id = configuration.id();
        String prefix = configuration.programId() + "-";
        String rest = id.startsWith(prefix) && id.length() > prefix.length() ? id.substring(prefix.length()) : id;
        String words = ContentNames.of(rest);
        return words.isEmpty() ? words : words.charAt(0) + words.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns how many crew seats the steps use.
     *
     * @param configuration the configuration
     * @return the count
     */
    public static int crewCount(MissionConfiguration configuration) {
        return configuration.usedCrewSlots().size();
    }

    /**
     * Returns how many flight controller slots the steps use (the layout may have more).
     *
     * @param configuration the configuration
     * @return the count
     */
    public static int controllerCount(MissionConfiguration configuration) {
        return (int) configuration.usedControlSlots().stream()
            .filter(slot -> configuration.controlSlots().get(slot).kind() == StaffingSlot.Kind.MISSION_CONTROL)
            .count();
    }

    /**
     * Returns how many astronaut control slots (CAPCOM) the steps use.
     *
     * @param configuration the configuration
     * @return the count
     */
    public static int capcomCount(MissionConfiguration configuration) {
        return (int) configuration.usedControlSlots().stream()
            .filter(slot -> configuration.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT)
            .count();
    }

    /**
     * Returns how often the player has flown a configuration to completion.
     *
     * @param configuration the configuration
     * @param state         the player's state
     * @return the count
     */
    public static int flown(MissionConfiguration configuration, PlayerSnapshot state) {
        return state.getSuccessfulFlightCounts().getOrDefault(configuration.id(), 0);
    }

    /**
     * Returns the prestige the next flight pays in full: the first time, 50 % the second, 30 %
     * after that ({@link MissionPrestige}).
     *
     * @param configuration the configuration
     * @param state         the player's state
     * @return the prestige
     */
    public static long nextPrestige(MissionConfiguration configuration, PlayerSnapshot state) {
        return MissionPrestige.tierTotal(configuration, MissionPrestige.tierFor(flown(configuration, state)));
    }

    /**
     * Returns the goals a configuration still waits for before it can fly
     * ({@link MissionConfiguration#requiredGoalIds()}), in the pack's order.
     *
     * @param configuration the configuration
     * @param state         the player's state
     * @return the missing goals, empty when it can fly
     */
    public static List<String> missingGoals(MissionConfiguration configuration, PlayerSnapshot state) {
        return configuration.requiredGoalIds().stream().filter(goal -> !state.hasAchievedOrCancelledGoal(goal))
            .toList();
    }

    /**
     * Words a launch type ("Low Earth orbit, human-rated").
     *
     * @param type the launch type
     * @return the words
     */
    public static String launchLabel(LaunchType type) {
        String place = switch (type) {
            case SUBORBITAL, SUBORBITAL_HR -> "Suborbital";
            case LEO, LEO_HR -> "Low Earth orbit";
            case TLI, TLI_HR -> "Trans-lunar injection";
            case TPI, TPI_HR -> "Trans-planetary injection";
            // No rocket, so nothing to rate: a carrier aircraft drops the craft.
            case AIR_DROP -> "Air drop from a carrier aircraft";
        };
        return type.humanRated() && type.needsRocket() ? place + ", human-rated" : place;
    }

    /**
     * Words a launch type briefly, for a tag ("LEO").
     *
     * @param type the launch type
     * @return the words
     */
    public static String launchTag(LaunchType type) {
        return switch (type) {
            case SUBORBITAL, SUBORBITAL_HR -> "Suborbital";
            case LEO, LEO_HR -> "LEO";
            case TLI, TLI_HR -> "TLI";
            case TPI, TPI_HR -> "TPI";
            case AIR_DROP -> "Air drop";
        };
    }

    /**
     * One rocket the player could pick for a launch.
     *
     * @param rocket   the rocket
     * @param owned    its reliability
     * @param lift     what it lifts to the launch's destination, 0 if it can't fly the type
     * @param spare    lift left after the payload
     * @param eligible whether it can fly the launch ({@link MissionLaunch#canLift})
     * @param reasons  why not, in words, empty when eligible
     */
    public record RocketChoice(MissionComponent rocket, OwnedMissionComponent owned, double lift, double spare,
                               boolean eligible, List<String> reasons) {
    }

    /**
     * Returns the rockets for one launch: every open rocket the player has, eligible or not, in
     * the pack's order. None for an air drop, which flies without one.
     *
     * @param launch  the launch
     * @param state   the player's state
     * @param catalog the content pack
     * @return the choices
     */
    public static List<RocketChoice> rocketChoices(MissionConfiguration.Launch launch, PlayerSnapshot state,
                                                   ContentCatalog catalog) {
        double mass = MissionLaunch.requiredLift(launch, catalog);
        List<RocketChoice> choices = new ArrayList<>();
        if (!launch.launchType().needsRocket()) {
            return choices;
        }
        for (MissionComponent rocket : Rockets.open(state, catalog)) {
            double lift = rocket.payloadCapacity(launch.launchType());
            List<String> reasons = new ArrayList<>();
            if (launch.launchType().humanRated() && rocket.associatedSkill() != SetSkill.HUMAN_RATED_ROCKETS) {
                reasons.add("Not human-rated.");
            }
            if (rocket.rocket().capacityFor(launch.launchType()) <= 0) {
                reasons.add("Can't reach " + launchLabel(launch.launchType()).toLowerCase(Locale.ROOT)
                    .replace(", human-rated", "") + ".");
            } else if (rocket.rocket().capacityFor(launch.launchType()) < mass) {
                double payload = MissionLaunch.payloadMass(launch, catalog);
                reasons.add(mass == payload ? String.format(Locale.ROOT, "Too small for the %,.0f kg payload.", mass)
                    : String.format(Locale.ROOT, "Too small: of the %,.0f kg payload, %,.0f kg needs lifting.", payload,
                        mass));
            }
            boolean eligible = MissionLaunch.canLift(rocket, launch, catalog);
            if (!eligible && reasons.isEmpty()) {
                reasons.add("Can't fly this launch.");
            }
            choices.add(new RocketChoice(rocket, state.getMissionComponents().get(rocket.id()), lift,
                Math.max(0, lift - mass), eligible, eligible ? List.of() : reasons));
        }
        return choices;
    }

    /**
     * Words a launch's payload ("Vostok Capsule, 4,700 kg").
     *
     * @param launch  the launch
     * @param catalog the content pack
     * @return the words
     */
    public static String payload(MissionConfiguration.Launch launch, ContentCatalog catalog) {
        List<String> names = launch.payloadComponentIds().stream().map(ContentNames::of).toList();
        if (!launch.launchType().needsRocket()) {
            // An air drop lifts nothing: the mass would only read "0 kg".
            return String.join(", ", names);
        }
        return String.join(", ", names) + String.format(Locale.ROOT, ", %,.0f kg",
            MissionLaunch.payloadMass(launch, catalog));
    }
}
