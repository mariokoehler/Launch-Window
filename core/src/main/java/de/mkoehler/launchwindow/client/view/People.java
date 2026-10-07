package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import de.mkoehler.launchwindow.rules.Chemistry;
import de.mkoehler.launchwindow.rules.Personality;
import de.mkoehler.launchwindow.rules.Teams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * What the people screens show ({@code ui-spec.md} components 20 to 22, &sect;4.14): revealed
 * traits, shown relationships, and a team's chemistry as the player knows it, with the previews the
 * pickers need. Plain logic, no libGDX. Every chemistry here is the known-only one
 * ({@code Chemistry.of(..., true)}), so nothing hidden ever reaches a screen.
 *
 * @author Mario Koehler
 */
public final class People {

    /**
     * A chemistry's level, as the indicator names it (component 21).
     */
    public enum Level {
        /** Below -0.5. */
        POOR("Poor"),
        /** -0.5 to -0.15. */
        STRAINED("Strained"),
        /** -0.15 to 0.15. */
        NEUTRAL("Neutral"),
        /** 0.15 to 0.5. */
        GOOD("Good"),
        /** Above 0.5. */
        EXCELLENT("Excellent");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        /**
         * Returns the level's word, such as "Good".
         *
         * @return the word
         */
        public String label() {
            return label;
        }

        /**
         * Returns the level of a chemistry value.
         *
         * @param value the chemistry, -1 to +1
         * @return the level
         */
        public static Level of(double value) {
            if (value < -0.5) {
                return POOR;
            }
            if (value < -0.15) {
                return STRAINED;
            }
            if (value <= 0.15) {
                return NEUTRAL;
            }
            return value <= 0.5 ? GOOD : EXCELLENT;
        }
    }

    /**
     * One line of a chemistry's breakdown.
     *
     * @param icon the line's mark: {@code rel_good}, {@code rel_clash} or {@code trait_team}
     * @param text the reason, such as "Team player: Okada"
     * @param sign +1 if it helps, -1 if it hurts
     */
    public record ReasonLine(String icon, String text, int sign) {
    }

    /**
     * A shown relationship, seen from one of the two.
     *
     * @param partner         the other person, as they are now
     * @param positive        whether they work well together (else they clash)
     * @param seasonsTogether how many seasons they've shared a team
     */
    public record Relationship(Personnel partner, boolean positive, int seasonsTogether) {
    }

    private People() {
    }

    /**
     * Returns what a chemistry does to the team's output, in whole percent.
     *
     * @param value the chemistry
     * @return the percentage, such as 4 for +4 %
     */
    public static long percent(double value) {
        return Math.round(value * Chemistry.OUTPUT_BAND * 100);
    }

    /**
     * Words a chemistry's effect on output: "+4 %", "−11 %" or "±0 %".
     *
     * @param value the chemistry
     * @return the text
     */
    public static String percentText(double value) {
        long percent = percent(value);
        return (percent > 0 ? "+" : percent < 0 ? "−" : "±") + Math.abs(percent) + " %";
    }

    /**
     * Returns someone's revealed traits, in the order they came out.
     *
     * @param person the person
     * @return the traits
     */
    public static List<Trait> traits(Personnel person) {
        return person.getTraits().stream().filter(TraitState::revealed).map(TraitState::trait).toList();
    }

    /**
     * Returns whether someone may still have traits to show: they're inside the reveal window.
     * Says nothing about whether they have any.
     *
     * @param person the person
     * @return whether the player is still getting to know them
     */
    public static boolean gettingToKnow(Personnel person) {
        return !Personality.fullyKnown(person);
    }

    /**
     * Finds anyone in the agency by id.
     *
     * @param state the player's state
     * @param id    the id
     * @return the person, or {@code null} if they've left
     */
    public static Personnel find(PlayerSnapshot state, String id) {
        return id == null ? null : Teams.find(state, id);
    }

    /**
     * Returns someone's shown relationships, warmest first.
     *
     * @param state  the player's state
     * @param person the person
     * @return the relationships with people still in the agency
     */
    public static List<Relationship> relationships(PlayerSnapshot state, Personnel person) {
        List<Relationship> list = new ArrayList<>();
        for (Rapport pair : state.getRapports()) {
            if (!pair.shown() || !pair.involves(person.getId())) {
                continue;
            }
            Personnel partner = find(state, pair.first().equals(person.getId()) ? pair.second() : pair.first());
            if (partner != null) {
                list.add(new Relationship(partner, pair.rapport() > 0, pair.seasonsTogether()));
            }
        }
        list.sort((a, b) -> Boolean.compare(b.positive(), a.positive()));
        return list;
    }

    /**
     * Returns the shown relationships a candidate has with people already in a team, for a picker's
     * marks.
     *
     * @param state     the player's state
     * @param candidate the candidate
     * @param teamIds   the team's ids; the candidate's own id is ignored
     * @return the relationships, warmest first
     */
    public static List<Relationship> marks(PlayerSnapshot state, Personnel candidate, Collection<String> teamIds) {
        return relationships(state, candidate).stream()
            .filter(relationship -> teamIds.contains(relationship.partner().getId())).toList();
    }

    /**
     * Returns the chemistry of a component's R&amp;D team as the player knows it, with someone
     * extra on it (for the assign dialog's preview).
     *
     * @param state     the player's state
     * @param component the component
     * @param team      the engineers on it now
     * @param extra     someone to count as on it too, or {@code null}
     * @return the chemistry
     */
    public static Chemistry.Result research(PlayerSnapshot state, MissionComponent component,
            List<SetSpecialist> team, SetSpecialist extra) {
        List<SetSpecialist> members = new ArrayList<>(team);
        if (extra != null && !members.contains(extra)) {
            members.add(extra);
        }
        return Chemistry.research(state, members, component.associatedSkill(), true);
    }

    /**
     * Returns the chemistry of a mission's crew and controllers as the player knows it.
     *
     * @param state          the player's state
     * @param configuration  the mission
     * @param crewSlotIds    one id per crew seat, {@code null} for an empty one
     * @param controlSlotIds one id per control slot, {@code null} for an empty one
     * @return the chemistry
     */
    public static Chemistry.Result mission(PlayerSnapshot state, MissionConfiguration configuration,
            List<String> crewSlotIds, List<String> controlSlotIds) {
        // A stand-in for the mission as it would fly, as MissionAssessment builds one.
        InProgressMission plan = new InProgressMission("preview", configuration.id(), crewSlotIds, controlSlotIds);
        return Chemistry.mission(state, plan, true);
    }

    /**
     * Returns the mission chemistry with one slot's holder swapped for someone else (for the
     * picker's preview).
     *
     * @param state          the player's state
     * @param configuration  the mission
     * @param crewSlotIds    the crew seats now
     * @param controlSlotIds the control slots now
     * @param crew           whether the slot is a crew seat (else a control slot)
     * @param slot           the slot
     * @param candidateId    who would take it
     * @return the chemistry
     */
    public static Chemistry.Result missionWith(PlayerSnapshot state, MissionConfiguration configuration,
            List<String> crewSlotIds, List<String> controlSlotIds, boolean crew, int slot, String candidateId) {
        List<String> crewIds = new ArrayList<>(crewSlotIds);
        List<String> controlIds = new ArrayList<>(controlSlotIds);
        (crew ? crewIds : controlIds).set(slot, candidateId);
        return mission(state, configuration, crewIds, controlIds);
    }

    /**
     * Returns the ids of a mission's crew and controllers, without empty slots.
     *
     * @param crewSlotIds    the crew seats
     * @param controlSlotIds the control slots
     * @return the ids
     */
    public static List<String> teamIds(List<String> crewSlotIds, List<String> controlSlotIds) {
        List<String> ids = new ArrayList<>();
        for (String id : crewSlotIds) {
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
        for (String id : controlSlotIds) {
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Words a chemistry's known reasons for its breakdown, one line each, pairs first.
     *
     * @param state  the player's state, for names
     * @param result the chemistry
     * @return the lines
     */
    public static List<ReasonLine> reasons(PlayerSnapshot state, Chemistry.Result result) {
        List<ReasonLine> lines = new ArrayList<>();
        for (Chemistry.Reason reason : result.reasons()) {
            int sign = reason.amount() >= 0 ? 1 : -1;
            if ("pair".equals(reason.kind())) {
                String names = surname(state, reason.ids().get(0)) + " and " + surname(state, reason.ids().get(1));
                lines.add(sign > 0 ? new ReasonLine("rel_good", names + " work well together", 1)
                    : new ReasonLine("rel_clash", names + " clash", -1));
                continue;
            }
            String who = surname(state, reason.ids().get(0));
            Trait trait = Trait.valueOf(reason.kind());
            String text = switch (trait) {
                case LONER -> sign > 0 ? "Loner, working alone: " + who : "Loner in a big team: " + who;
                case MUST_BE_THE_BEST -> sign > 0 ? "Must be the best, and is: " + who
                    : "Must be the best, but outdone: " + who;
                default -> TraitTexts.label(trait) + ": " + who;
            };
            lines.add(new ReasonLine("trait_team", text, sign));
        }
        return lines;
    }

    /**
     * Returns someone's surname: the last word of their name.
     *
     * @param name the full name
     * @return the surname
     */
    public static String surname(String name) {
        String trimmed = name.trim();
        int space = trimmed.lastIndexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(space + 1);
    }

    private static String surname(PlayerSnapshot state, String id) {
        Personnel person = find(state, id);
        return person == null ? "Someone" : surname(person.getName());
    }

    /**
     * Returns someone's group, as a person page or relationship line shows it: "SET", "Astronaut"
     * or "Mission Control".
     *
     * @param person the person
     * @return the group's short name
     */
    public static String group(Personnel person) {
        if (person instanceof Astronaut) {
            return "Astronaut";
        }
        return person instanceof MissionControlOfficer ? "Mission Control" : "SET";
    }

    /**
     * Returns what someone is doing now, as their roster row says it.
     *
     * @param person  the person
     * @param state   the player's state
     * @param catalog the content pack
     * @return the state, such as "Idle"
     */
    public static String stateText(Personnel person, PlayerSnapshot state, ContentCatalog catalog) {
        Objects.requireNonNull(person, "person must not be null");
        if (person instanceof Astronaut astronaut) {
            return AstronautCorps.stateText(astronaut, state, catalog);
        }
        if (person instanceof MissionControlOfficer officer) {
            return MissionControl.stateText(officer, state, catalog);
        }
        return SetDepartment.stateText((SetSpecialist) person);
    }

    /**
     * Words how long two people have shared a team: "12 seasons together".
     *
     * @param seasons the seasons
     * @return the text
     */
    public static String seasonsTogether(int seasons) {
        return seasons + (seasons == 1 ? " season" : " seasons") + " together";
    }

    /**
     * Words a hint as a former colleague put it, quoted: “driven”.
     *
     * @param hint the hint's id
     * @return the quote, or {@code null} for none
     */
    public static String hintQuote(String hint) {
        String words = TraitTexts.hint(hint);
        return words == null ? null : "“" + words.toLowerCase(Locale.ROOT) + "”";
    }
}
