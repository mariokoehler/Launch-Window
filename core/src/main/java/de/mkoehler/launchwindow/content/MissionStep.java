package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.AstronautSkill;

import java.util.List;
import java.util.Objects;

/**
 * <p>One step of a mission season's resolution batch - what
 * {@link de.mkoehler.launchwindow.rules.MissionStepResolution} needs to resolve
 * it. People are involved through their configuration's <b>staffing slots</b>,
 * as in BASPM ({@code baspm.md} &sect;10.11, user decision 2026-10-03): a
 * controller involvement names one of the configuration's
 * {@link MissionConfiguration#controlSlots() control slots}, and the roll reads
 * the person in that slot at the slot's skill; a crew involvement names a crew
 * slot and one of that astronaut's skills. Nothing is averaged across people.</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack, never per-player state, and never sent over the network.</p>
 *
 * @param id                       a stable identifier, unique within the
 *                                 {@link MissionSeason} it belongs to
 * @param componentInvolvements    the mission components involved in this step and
 *                                 each one's involvement percentage; percentages must
 *                                 sum to exactly 100 ({@code design.md} &sect;2.5(e))
 * @param controllerInvolvements   the control slots involved (flight controllers and
 *                                 CAPCOM alike) and each one's involvement percentage;
 *                                 percentages sum to 100 or less, and may be empty - an
 *                                 empty list means the saving throw can never succeed
 * @param crewInvolvements         the crew slots' skills involved and each one's weight;
 *                                 either empty (no crew involved) or weights summing to
 *                                 exactly 100 across every crew slot, since the
 *                                 crew-blend is a weighted average
 * @param crewRisk                 whether a worst-tier failure of this step can cost crew
 *                                 lives; requires at least one crew involvement
 * @param prestigeSharePercent     this step's share of its configuration's prestige
 *                                 total, 0-100 ({@code design.md} &sect;2.6(d))
 * @param failurePrestigeDeduction the prestige this step deducts when it fails, as a
 *                                 non-negative magnitude ({@code design.md} &sect;2.6(d))
 * @param goalIdsAchieved          the goals this step achieves when it succeeds
 * @author Mario Koehler
 */
public record MissionStep(String id, List<ComponentInvolvement> componentInvolvements,
                           List<ControllerInvolvement> controllerInvolvements,
                           List<CrewInvolvement> crewInvolvements, boolean crewRisk,
                           int prestigeSharePercent, long failurePrestigeDeduction,
                           List<String> goalIdsAchieved) {

    /** How far a crew weight sum may stray from 100 and still count as 100. */
    private static final double WEIGHT_TOLERANCE = 1e-6;

    /**
     * A step with no goals.
     *
     * @param id                       see {@link MissionStep}
     * @param componentInvolvements    see {@link MissionStep}
     * @param controllerInvolvements   see {@link MissionStep}
     * @param crewInvolvements         see {@link MissionStep}
     * @param crewRisk                 see {@link MissionStep}
     * @param prestigeSharePercent     see {@link MissionStep}
     * @param failurePrestigeDeduction see {@link MissionStep}
     * @throws NullPointerException     see the canonical constructor
     * @throws IllegalArgumentException see the canonical constructor
     */
    public MissionStep(String id, List<ComponentInvolvement> componentInvolvements,
                       List<ControllerInvolvement> controllerInvolvements,
                       List<CrewInvolvement> crewInvolvements, boolean crewRisk,
                       int prestigeSharePercent, long failurePrestigeDeduction) {
        this(id, componentInvolvements, controllerInvolvements, crewInvolvements, crewRisk,
            prestigeSharePercent, failurePrestigeDeduction, List.of());
    }

    /**
     * A step with no prestige share, no failure deduction, and no goals.
     *
     * @param id                     see {@link MissionStep}
     * @param componentInvolvements  see {@link MissionStep}
     * @param controllerInvolvements see {@link MissionStep}
     * @param crewInvolvements       see {@link MissionStep}
     * @param crewRisk               see {@link MissionStep}
     * @throws NullPointerException     see the canonical constructor
     * @throws IllegalArgumentException see the canonical constructor
     */
    public MissionStep(String id, List<ComponentInvolvement> componentInvolvements,
                       List<ControllerInvolvement> controllerInvolvements,
                       List<CrewInvolvement> crewInvolvements, boolean crewRisk) {
        this(id, componentInvolvements, controllerInvolvements, crewInvolvements, crewRisk, 0, 0);
    }

    /**
     * Validates every field and copies the lists. Slot indices are checked against
     * the configuration's slots by {@link MissionConfiguration}, which knows them.
     *
     * @throws NullPointerException     if {@code id} or any list is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank, the components are empty
     *                                   or don't sum to exactly 100, the controller
     *                                   percentages sum past 100 or repeat a slot, the crew
     *                                   weights are non-empty and don't sum to 100 or repeat a
     *                                   slot-and-skill pair, {@code crewRisk} has no crew, or a
     *                                   prestige value is out of range
     */
    public MissionStep {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(componentInvolvements, "componentInvolvements must not be null");
        Objects.requireNonNull(controllerInvolvements, "controllerInvolvements must not be null");
        Objects.requireNonNull(crewInvolvements, "crewInvolvements must not be null");
        Objects.requireNonNull(goalIdsAchieved, "goalIdsAchieved must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (componentInvolvements.isEmpty()) {
            throw new IllegalArgumentException("componentInvolvements must not be empty");
        }
        componentInvolvements = List.copyOf(componentInvolvements);
        controllerInvolvements = List.copyOf(controllerInvolvements);
        crewInvolvements = List.copyOf(crewInvolvements);
        goalIdsAchieved = List.copyOf(goalIdsAchieved);

        requireNoDuplicates(componentInvolvements.stream().map(ComponentInvolvement::componentId).toList(),
            "componentInvolvements");
        requireNoDuplicates(controllerInvolvements.stream().map(ControllerInvolvement::slot).toList(),
            "controllerInvolvements");
        requireNoDuplicates(crewInvolvements.stream().map(crew -> crew.slot() + ":" + crew.skill()).toList(),
            "crewInvolvements");

        if (componentInvolvements.stream().mapToInt(ComponentInvolvement::percent).sum() != 100) {
            throw new IllegalArgumentException("componentInvolvements percentages must sum to exactly 100");
        }
        if (controllerInvolvements.stream().mapToInt(ControllerInvolvement::percent).sum() > 100) {
            throw new IllegalArgumentException("controllerInvolvements percentages must not sum past 100");
        }
        double crewSum = crewInvolvements.stream().mapToDouble(CrewInvolvement::percent).sum();
        if (!crewInvolvements.isEmpty() && Math.abs(crewSum - 100) > WEIGHT_TOLERANCE) {
            throw new IllegalArgumentException("crewInvolvements weights must sum to exactly 100: " + crewSum);
        }
        if (crewRisk && crewInvolvements.isEmpty()) {
            throw new IllegalArgumentException("crewRisk requires at least one crewInvolvements entry");
        }
        if (prestigeSharePercent < 0 || prestigeSharePercent > 100) {
            throw new IllegalArgumentException("prestigeSharePercent must be 0-100: " + prestigeSharePercent);
        }
        if (failurePrestigeDeduction < 0) {
            throw new IllegalArgumentException(
                "failurePrestigeDeduction must not be negative: " + failurePrestigeDeduction);
        }
        requireNoDuplicates(goalIdsAchieved, "goalIdsAchieved");
    }

    private static void requireNoDuplicates(List<?> keys, String fieldName) {
        if (keys.size() != keys.stream().distinct().count()) {
            throw new IllegalArgumentException(fieldName + " must not contain a duplicate entry: " + keys);
        }
    }

    /**
     * One mission component's involvement in a step.
     *
     * @param componentId the involved {@link MissionComponent#id()}, or a rocket slot
     *                    ({@link MissionConfiguration#rocketSlotId(int)})
     * @param percent     the involvement percentage, 1-100
     */
    public record ComponentInvolvement(String componentId, int percent) {

        /**
         * Validates both fields.
         *
         * @throws NullPointerException     if {@code componentId} is {@code null}
         * @throws IllegalArgumentException if {@code componentId} is blank, or
         *                                   {@code percent} is outside 1-100
         */
        public ComponentInvolvement {
            Objects.requireNonNull(componentId, "componentId must not be null");
            if (componentId.isBlank()) {
                throw new IllegalArgumentException("componentId must not be blank");
            }
            if (percent < 1 || percent > 100) {
                throw new IllegalArgumentException("percent must be within 1-100: " + percent);
            }
        }
    }

    /**
     * One control slot's involvement in a step: the person in that slot, read at
     * the slot's skill.
     *
     * @param slot    the 0-based index into the configuration's control slots
     * @param percent the involvement percentage, 1-100
     */
    public record ControllerInvolvement(int slot, int percent) {

        /**
         * Validates both fields.
         *
         * @throws IllegalArgumentException if {@code slot} is negative or
         *                                   {@code percent} is outside 1-100
         */
        public ControllerInvolvement {
            if (slot < 0) {
                throw new IllegalArgumentException("slot must not be negative: " + slot);
            }
            if (percent < 1 || percent > 100) {
                throw new IllegalArgumentException("percent must be within 1-100: " + percent);
            }
        }
    }

    /**
     * One crew slot's skill involved in a step, with its weight. One crew slot can
     * appear with several skills (BASPM's {@code AstronautInvolved} with several
     * {@code SkillUsedIndex} entries).
     *
     * @param slot    the 0-based index into the configuration's crew slots
     * @param skill   the astronaut skill read
     * @param percent the weight, above 0 and at most 100
     */
    public record CrewInvolvement(int slot, AstronautSkill skill, double percent) {

        /**
         * Validates every field.
         *
         * @throws NullPointerException     if {@code skill} is {@code null}
         * @throws IllegalArgumentException if {@code slot} is negative or
         *                                   {@code percent} is not above 0 and at most 100
         */
        public CrewInvolvement {
            Objects.requireNonNull(skill, "skill must not be null");
            if (slot < 0) {
                throw new IllegalArgumentException("slot must not be negative: " + slot);
            }
            if (percent <= 0 || percent > 100) {
                throw new IllegalArgumentException("percent must be above 0 and at most 100: " + percent);
            }
        }
    }
}
