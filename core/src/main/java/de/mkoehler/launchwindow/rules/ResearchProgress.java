package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * <p>The R&amp;D season-advance pass: every {@code SET} employee currently
 * {@link AssignmentState#ASSIGNED} to a mission component (see
 * {@link SetSpecialist#getRdTargetComponentId()}) grows two separate
 * things this season - their own skill in the component's associated
 * group ({@link SkillGrowth#flatRdGain}, flat, no distance-to-cap scaling,
 * re-deriving {@code baspm.md} &sect;9.3), and the targeted component's own
 * reliability ({@link SkillGrowth#reliabilityGain}, diminishing-returns-
 * shaped against that component's own ceiling, scaled by the employee's
 * skill). Headless, per {@code design.md} &sect;3.10 - no persistence/
 * network/libGDX dependency.</p>
 *
 * <p><b>(Unconfirmed default - both the reliability-gain formula's shape
 * and its factor range, see {@code design.md} &sect;3.4):</b> Launch
 * Window's own formula, not ported from BASPM - {@code baspm.md} has no
 * decompiled trace for this half of the mechanic, only a manual-level
 * summary that it scales with skill at all, not yet verified against the
 * decompiled source.</p>
 *
 * <p><b>Skipped, not an error:</b> a {@code SET} employee not
 * {@link AssignmentState#ASSIGNED}, with no R&amp;D target set, or whose
 * target isn't actually an owned component (a stale/cleared reference) -
 * all ordinary states in an async multiplayer game, so this pass silently
 * does nothing for that employee rather than throwing.</p>
 *
 * <p><b>Both of a season's effects use the employee's skill as it stood at
 * the start of their own turn this season</b>, before this same season's
 * own flat skill gain is applied - the two draws and their resulting gains
 * stay independent of each other's output, not entangled. The skill-gain
 * factor is always drawn before the reliability-gain factor, so a seed
 * reproduces a run exactly.</p>
 *
 * <p><b>Two employees researching the same component stack, applied in
 * roster order</b>: each one's reliability gain is computed against
 * whatever the component's reliability already is when their own turn
 * comes up - genuine diminishing returns across simultaneous researchers
 * within one season, the same way repeated seasons already diminish.</p>
 *
 * <p><b>Never touches {@link OwnedMissionComponent#getFloorReliability()}</b>
 * - only {@link OwnedMissionComponent#getCurrentReliability()} grows from
 * R&amp;D; the floor stays exactly what {@link ProgramOpening} set it
 * to.</p>
 *
 * <p><b>Composition order: {@link PersonnelLifecycle}, then
 * {@link TrainingProgress}, then this class, then
 * {@code HiringPool.refreshCandidates} last</b> - the exact order
 * {@link HiringPool}'s own Javadoc documents. A {@code SET} employee who
 * departed this same season does no further R&amp;D, and running after
 * {@code TrainingProgress} (not before) is also what makes the two
 * resolvers' random draws reproducible from a seed, since both draw from
 * the same shared source.</p>
 *
 * @author Mario Koehler
 */
public final class ResearchProgress {

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private ResearchProgress() {
    }

    /**
     * Advances one season of R&amp;D progress for every {@code SET}
     * employee currently assigned to a mission component, mutating the
     * snapshot in place.
     *
     * @param snapshot the player snapshot to advance; mutated in place
     * @param catalog  the content catalog the targeted components' own
     *                 definitions are looked up in
     * @param random   the random source this season's gains are drawn from
     * @return the {@link ResearchCompleted} events this pass caused, in
     *     roster order; empty if nobody did R&amp;D this season
     * @throws NullPointerException if any argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog,
            RandomGenerator random) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(random, "random must not be null");

        List<ResolutionEvent> events = new ArrayList<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            advanceOne(specialist, snapshot, catalog, random, events);
        }
        return events;
    }

    /**
     * Advances one {@code SET} employee's R&amp;D for this season, if
     * they're genuinely doing any - see this class's own Javadoc for the
     * skip cases.
     *
     * @param specialist the employee to advance
     * @param snapshot   the snapshot their targeted component is owned in
     * @param catalog    the content catalog the targeted component's own
     *                   definition is looked up in
     * @param random     the random source this season's gains are drawn from
     * @param events     the list to append a {@link ResearchCompleted} to,
     *                   if this employee actually did R&amp;D this season
     */
    private static void advanceOne(SetSpecialist specialist, PlayerSnapshot snapshot, ContentCatalog catalog,
            RandomGenerator random, List<ResolutionEvent> events) {
        if (specialist.getAssignmentState() != AssignmentState.ASSIGNED) {
            return;
        }
        String targetId = specialist.getRdTargetComponentId();
        if (targetId == null) {
            return;
        }
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(targetId);
        if (owned == null) {
            return;
        }
        MissionComponent component = catalog.missionComponent(targetId).orElse(null);
        if (component == null) {
            return;
        }

        SetSkill group = component.associatedSkill();
        double skillBefore = specialist.getSkill(group);
        double skillFactor = SkillGrowth.drawRdGainFactor(random);
        double reliabilityFactor = SkillGrowth.drawRdReliabilityFactor(random);
        double skillGain = SkillGrowth.flatRdGain(skillBefore, skillFactor);
        double reliabilityGain = SkillGrowth.reliabilityGain(owned.getCurrentReliability(),
            component.maxReliability(), skillBefore, reliabilityFactor);

        specialist.setSkill(group, skillBefore + skillGain);
        owned.setCurrentReliability(owned.getCurrentReliability() + reliabilityGain);

        events.add(new ResearchCompleted(specialist.getId(), specialist.getName(), targetId, skillGain,
            reliabilityGain));
    }
}
