package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlSkill;

import java.util.List;
import java.util.Objects;

/**
 * <p>One step of a mission season's resolution batch - what
 * {@link de.mkoehler.launchwindow.rules.MissionStepResolution} needs to
 * resolve it, not who is assigned to it. Involvement is expressed against
 * skills/components, never specific personnel: which astronaut or
 * controller actually fills a skill's involvement is left to
 * {@link de.mkoehler.launchwindow.rules.MissionLaunch}'s own assignment:
 * how several crew sharing one skill combine into that skill's single
 * contribution is the <em>mean</em> skill value across every assigned
 * person (user decision, {@code design.md} &sect;2.5(f)) -
 * {@code baspm.md} &sect;8.2's own trace
 * ("summed per-skill-per-crew-member") confirms crew contributions are
 * tallied per skill but does not itself pin down this aggregation rule.</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network.</p>
 *
 * @param id                     a stable identifier, unique within the
 *                               {@link MissionSeason} it belongs to (not
 *                               necessarily across other seasons)
 * @param componentInvolvements the mission components involved in this
 *                               step and each one's involvement
 *                               percentage; percentages must sum to
 *                               exactly 100 - see
 *                               {@code design.md} &sect;2.5(e)
 * @param controllerInvolvements the flight-controller skills involved in
 *                                this step and each one's involvement
 *                                percentage; percentages sum to 100 or
 *                                less, and may be empty - an empty list
 *                                means the saving throw can never succeed,
 *                                since {@link de.mkoehler.launchwindow.rules.MissionStepResolution#computeSavingThrowProbability}
 *                                scales it by this same sum
 * @param crewInvolvements       the astronaut skills involved in this step
 *                                and each one's involvement percentage;
 *                                either empty (no crew involved) or
 *                                percentages summing to exactly 100 - a
 *                                partial sum would read as a skill
 *                                shortfall to {@link de.mkoehler.launchwindow.rules.MissionStepResolution#computeBaseProbability}'s
 *                                weighted-average blend rather than
 *                                reduced crew presence
 * @param crewRisk                whether a worst-tier failure of this step
 *                                can cost crew lives
 *                                ({@link de.mkoehler.launchwindow.rules.MissionStepOutcome#CATASTROPHIC});
 *                                requires at least one entry in
 *                                {@code crewInvolvements}
 * @param prestigeSharePercent    this step's share of its configuration's
 *                                prestige total, 0-100; the shares across
 *                                every step of one configuration must sum to
 *                                either exactly 100 or 0 - see
 *                                {@link ContentCatalog} and
 *                                {@code design.md} &sect;2.6(d)
 * @param failurePrestigeDeduction the prestige this step deducts when it
 *                                resolves {@code FAILURE} or
 *                                {@code CATASTROPHIC}, as a non-negative
 *                                magnitude - it replaces the season's
 *                                accumulated prestige rather than adding to
 *                                it, {@code design.md} &sect;2.6(d)
 * @author Mario Koehler
 */
public record MissionStep(String id, List<ComponentInvolvement> componentInvolvements,
                           List<ControllerInvolvement> controllerInvolvements,
                           List<CrewInvolvement> crewInvolvements, boolean crewRisk,
                           int prestigeSharePercent, long failurePrestigeDeduction,
                           List<String> goalIdsAchieved) {

    /**
     * A step with no prestige share, no failure deduction, and no goals - the
     * shape every step had before prestige and goals were modeled.
     *
     * @param id                     see {@link MissionStep}
     * @param componentInvolvements  see {@link MissionStep}
     * @param controllerInvolvements see {@link MissionStep}
     * @param crewInvolvements       see {@link MissionStep}
     * @param crewRisk               see {@link MissionStep}
     * @param prestigeSharePercent   see {@link MissionStep}
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
     * A step with no prestige share and no failure deduction - the shape
     * every step had before prestige was modeled, kept so existing content
     * and tests don't all need touching.
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
     * Validates every field, defensively copies all three involvement
     * lists into unmodifiable views, and checks their percentage-sum and
     * crew-risk constraints.
     *
     * @throws NullPointerException     if {@code id} or any involvement
     *                                   list is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank,
     *                                   {@code componentInvolvements} is
     *                                   empty or its percentages do not sum
     *                                   to exactly 100,
     *                                   {@code controllerInvolvements}'s own
     *                                   percentages sum past 100,
     *                                   {@code crewInvolvements} is
     *                                   non-empty and its percentages do
     *                                   not sum to exactly 100, a
     *                                   component id or skill repeats
     *                                   within one list, or
     *                                   {@code crewRisk} is {@code true}
     *                                   with an empty {@code crewInvolvements}
     */
    public MissionStep {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(componentInvolvements, "componentInvolvements must not be null");
        Objects.requireNonNull(controllerInvolvements, "controllerInvolvements must not be null");
        Objects.requireNonNull(crewInvolvements, "crewInvolvements must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (componentInvolvements.isEmpty()) {
            throw new IllegalArgumentException("componentInvolvements must not be empty");
        }

        componentInvolvements = List.copyOf(componentInvolvements);
        controllerInvolvements = List.copyOf(controllerInvolvements);
        crewInvolvements = List.copyOf(crewInvolvements);

        requireNoDuplicates(componentInvolvements.stream().map(ComponentInvolvement::componentId).toList(),
            "componentInvolvements");
        requireNoDuplicates(controllerInvolvements.stream().map(ControllerInvolvement::skill).toList(),
            "controllerInvolvements");
        requireNoDuplicates(crewInvolvements.stream().map(CrewInvolvement::skill).toList(), "crewInvolvements");

        requireExactly100(componentInvolvements.stream().mapToInt(ComponentInvolvement::percent).sum(),
            "componentInvolvements");
        requireAtMost100(controllerInvolvements.stream().mapToInt(ControllerInvolvement::percent).sum(),
            "controllerInvolvements");
        if (!crewInvolvements.isEmpty()) {
            requireExactly100(crewInvolvements.stream().mapToInt(CrewInvolvement::percent).sum(), "crewInvolvements");
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
        Objects.requireNonNull(goalIdsAchieved, "goalIdsAchieved must not be null");
        goalIdsAchieved = List.copyOf(goalIdsAchieved);
        if (goalIdsAchieved.size() != goalIdsAchieved.stream().distinct().count()) {
            throw new IllegalArgumentException("goalIdsAchieved must not contain a duplicate entry");
        }
    }

    /**
     * Validates that a list of keys contains no duplicate.
     *
     * @param keys      the keys to check
     * @param fieldName the field name to report in the exception message
     * @throws IllegalArgumentException if any key repeats
     */
    private static void requireNoDuplicates(List<?> keys, String fieldName) {
        if (keys.size() != keys.stream().distinct().count()) {
            throw new IllegalArgumentException(fieldName + " must not contain a duplicate entry: " + keys);
        }
    }

    /**
     * Validates that a percentage sum equals exactly 100.
     *
     * @param sum       the sum to check
     * @param fieldName the field name to report in the exception message
     * @throws IllegalArgumentException if {@code sum} is not exactly 100
     */
    private static void requireExactly100(int sum, String fieldName) {
        if (sum != 100) {
            throw new IllegalArgumentException(fieldName + " percentages must sum to exactly 100: " + sum);
        }
    }

    /**
     * Validates that a percentage sum does not exceed 100.
     *
     * @param sum       the sum to check
     * @param fieldName the field name to report in the exception message
     * @throws IllegalArgumentException if {@code sum} exceeds 100
     */
    private static void requireAtMost100(int sum, String fieldName) {
        if (sum > 100) {
            throw new IllegalArgumentException(fieldName + " percentages must not sum past 100: " + sum);
        }
    }

    /**
     * One mission component's involvement in a step.
     *
     * @param componentId the involved {@link MissionComponent#id()}
     * @param percent     the involvement percentage, 1-100
     */
    public record ComponentInvolvement(String componentId, int percent) {

        /**
         * Validates both fields.
         *
         * @throws NullPointerException     if {@code componentId} is {@code null}
         * @throws IllegalArgumentException if {@code componentId} is blank,
         *                                   or {@code percent} is outside 1-100
         */
        public ComponentInvolvement {
            Objects.requireNonNull(componentId, "componentId must not be null");
            if (componentId.isBlank()) {
                throw new IllegalArgumentException("componentId must not be blank");
            }
            requirePercentage(percent);
        }
    }

    /**
     * One flight-controller skill's involvement in a step.
     *
     * @param skill   the involved skill
     * @param percent the involvement percentage, 1-100
     */
    public record ControllerInvolvement(MissionControlSkill skill, int percent) {

        /**
         * Validates both fields.
         *
         * @throws NullPointerException     if {@code skill} is {@code null}
         * @throws IllegalArgumentException if {@code percent} is outside 1-100
         */
        public ControllerInvolvement {
            Objects.requireNonNull(skill, "skill must not be null");
            requirePercentage(percent);
        }
    }

    /**
     * One astronaut skill's involvement in a step.
     *
     * @param skill   the involved skill
     * @param percent the involvement percentage, 1-100
     */
    public record CrewInvolvement(AstronautSkill skill, int percent) {

        /**
         * Validates both fields.
         *
         * @throws NullPointerException     if {@code skill} is {@code null}
         * @throws IllegalArgumentException if {@code percent} is outside 1-100
         */
        public CrewInvolvement {
            Objects.requireNonNull(skill, "skill must not be null");
            requirePercentage(percent);
        }
    }

    /**
     * Validates that a single involvement's percentage falls within 1-100 -
     * a zero-percent entry is noise, not a meaningful involvement; omit the
     * entry entirely instead.
     *
     * @param percent the value to check
     * @throws IllegalArgumentException if {@code percent} is outside 1-100
     */
    private static void requirePercentage(int percent) {
        if (percent < 1 || percent > 100) {
            throw new IllegalArgumentException("percent must be within 1-100: " + percent);
        }
    }
}
