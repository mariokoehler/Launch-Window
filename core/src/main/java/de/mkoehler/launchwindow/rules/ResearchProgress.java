package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * <p>The R&amp;D season-advance pass: every {@code SET} employee currently
 * {@link AssignmentState#ASSIGNED} to a mission component, occupying one of
 * that component's four R&amp;D slots (see {@link SetSpecialist#getRdTargetComponentId()}/
 * {@link SetSpecialist#getRdSlot()}), grows two separate things this season -
 * their own skill in the component's associated group
 * ({@link SkillGrowth#flatRdGain}, flat, no distance-to-cap scaling,
 * re-deriving {@code baspm.md} &sect;9.3), and the targeted component's own
 * reliability ({@link SkillGrowth#rdReliabilityGain}, diminishing-returns-
 * shaped against that component's own R&amp;D ceiling, scaled by the
 * employee's skill and by which slot they occupy). Headless, per
 * {@code design.md} &sect;5 M2 - no persistence/network/libGDX dependency.</p>
 *
 * <p><b>Code-confirmed, re-deriving {@code baspm.md} &sect;10.8's exact
 * multi-slot formula</b> (user decision, 2026-10-01, see {@code design.md}
 * &sect;3.4) with Launch Window's own numbers, not BASPM's.</p>
 *
 * <p><b>"Doing R&amp;D" requires a valid slot, not just an
 * {@link AssignmentState#ASSIGNED} state with a target set</b>: a specialist
 * whose {@link SetSpecialist#getRdSlot()} is {@code null}, outside
 * {@code [0, }{@link MissionComponent#RD_SLOT_COUNT}{@code )}, or collides
 * with another specialist already claiming the same (component, slot) pair
 * this season (first-encountered-in-roster-order wins; the loser is treated
 * as not researching at all this season, for <em>both</em> their own skill
 * gain and the component's reliability gain) does nothing this pass -
 * ordinary states in an async multiplayer game with no real R&amp;D-assignment
 * player-action path yet (that enforcement belongs at the assignment layer
 * once one exists, not here), so this silently skips rather than throwing.</p>
 *
 * <p><b>Two passes, not one, and the random-draw order changed from an
 * earlier version of this class to make per-component aggregation possible -
 * both passes iterate in the same grouped order, component by component (in
 * the order each component's first active researcher appears in roster
 * order), slot 0 before 1 before 2 before 3 within a component, not literal
 * roster order:</b> first, every actively-researching specialist's own flat
 * skill gain is drawn and applied in that grouped order - this is what "the
 * assignee's skill as it stood at the start of their own turn this season"
 * below is captured from, <em>before</em> this same season's own flat skill
 * gain is applied, keeping the two draws' inputs independent of each other's
 * output. Only then does the second pass draw each occupied slot's raw
 * reliability-gain amount, in the same grouped order - BASPM itself
 * computes a component's headroom factor <em>once</em> per season, before
 * any of its slots' contributions, not re-fetched after each slot applies -
 * so this pass mirrors that: every occupied slot's raw gain is computed
 * against the component's reliability as it stood before this season's
 * R&amp;D (not updated slot-by-slot), summed, the stagnation guard applied
 * once to the sum, then the whole total is applied and clamped to
 * {@link MissionComponent#maxRdReliability()} in one step. <b>Supersedes an
 * earlier version's "two employees researching the same component stack,
 * applied in roster order, each computed against whatever the component's
 * reliability already is" design</b> - that sequential model doesn't match
 * BASPM's own shared-headroom, slot-banded mechanic once it was actually
 * traced (&sect;10.8).</p>
 *
 * <p><b>Each specialist's reported {@link ResearchCompleted#reliabilityGain()}
 * is their proportional share of the component's actually-applied total</b>
 * (their own nominal, pre-clamp slot contribution divided by the sum of all
 * occupied slots' nominal contributions, times the total that was actually
 * applied after the stagnation guard/ceiling clamp) - BASPM itself has no
 * concept of per-employee attribution once slots are summed, so this
 * proportional split is Launch Window's own choice, made so that summing
 * every event's {@code reliabilityGain} for one season always equals that
 * component's actual reliability delta (the bot harness, {@code design.md}
 * &sect;3.10, relies on this for its own reporting).</p>
 *
 * <p><b>Never touches {@link OwnedMissionComponent#getFloorReliability()}</b>
 * - only {@link OwnedMissionComponent#getCurrentReliability()} grows from
 * R&amp;D; the floor stays exactly what {@link ProgramOpening} set it
 * to.</p>
 *
 * <p><b>A component reaching its R&amp;D ceiling this pass - or already
 * sitting at it - releases every occupied slot's specialist back to
 * {@link AssignmentState#IDLE}</b> (clearing both
 * {@link SetSpecialist#getRdTargetComponentId()} and
 * {@link SetSpecialist#getRdSlot()}), reported as one
 * {@link ResearchComponentMaxed} event per maxed component - see that
 * class's own Javadoc for the full {@code baspm.md} &sect;10.8 derivation
 * and its unconfirmed timing nuance. A released specialist still keeps
 * this same season's {@link ResearchCompleted} event and skill gain; only
 * their assignment is cleared.</p>
 *
 * <p><b>Composition order: {@link PersonnelLifecycle}, then
 * {@link MissionProgress}, then {@link TrainingProgress}, then this
 * class, then {@code HiringPool.refreshCandidates} last</b> - the exact order
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
     * Below this remaining headroom to a component's own R&amp;D ceiling,
     * any positive nominal gain snaps the component straight to
     * {@link MissionComponent#maxRdReliability()} instead of letting the
     * per-season gain asymptotically crawl toward it forever - re-deriving
     * {@code baspm.md} &sect;10.8's own stagnation guard (BASPM's own
     * threshold is {@code 0.1}; this is Launch Window's own value, per
     * {@code design.md} &sect;1.2, not a copy).
     */
    static final double HEADROOM_STAGNATION_THRESHOLD = 0.2;

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
     * @return the {@link ResearchCompleted}/{@link ResearchComponentMaxed}
     *     events this pass caused, grouped by target component (in the
     *     order each component's first active researcher appears in roster
     *     order) and slot 0 before 1 before 2 before 3 within a component -
     *     not literal roster order; a component's own
     *     {@code ResearchComponentMaxed}, if any, comes right after its
     *     {@code ResearchCompleted} events; empty if nobody did R&amp;D
     *     this season
     * @throws NullPointerException if any argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog,
            RandomGenerator random) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(random, "random must not be null");

        Map<String, SetSpecialist[]> slotsByComponentId = groupActiveResearchersBySlot(snapshot, catalog);

        // Pass 1: flat skill gain, independent per specialist, grouped order (see class
        // Javadoc) - also captures each researching specialist's skill as it stood before
        // this gain, for pass 2.
        Map<SetSpecialist, Double> skillBeforeGain = new LinkedHashMap<>();
        for (SetSpecialist[] slots : slotsByComponentId.values()) {
            for (SetSpecialist specialist : slots) {
                if (specialist == null) {
                    continue;
                }
                MissionComponent component = catalog.missionComponent(specialist.getRdTargetComponentId())
                    .orElseThrow();
                SetSkill group = component.associatedSkill();
                double skillBefore = specialist.getSkill(group);
                skillBeforeGain.put(specialist, skillBefore);
                double skillFactor = SkillGrowth.drawRdGainFactor(random);
                double skillGain = SkillGrowth.flatRdGain(skillBefore, skillFactor) * snapshot.getRdResultsMultiplier();
                specialist.setSkill(group, Math.min(SkillGrowth.SKILL_CAP, skillBefore + skillGain));
            }
        }

        // Pass 2: reliability gain, grouped per component, shared start-of-season headroom.
        List<ResolutionEvent> events = new ArrayList<>();
        for (Map.Entry<String, SetSpecialist[]> entry : slotsByComponentId.entrySet()) {
            advanceComponent(entry.getKey(), entry.getValue(), snapshot, catalog, skillBeforeGain, random, events);
        }
        return events;
    }

    /**
     * Groups every specialist genuinely doing R&amp;D by their target
     * component id, indexed within each component by slot (see this
     * class's own Javadoc for what counts as "genuinely doing R&amp;D" and
     * the slot-collision tie-break). Iteration order follows the target
     * component's first active researcher's position in roster order -
     * what makes the random-draw order reproducible from a seed.
     *
     * @param snapshot the snapshot whose roster to scan, in roster order
     * @param catalog  the content catalog targeted components are looked up in
     * @return target component id to a fixed-size {@value MissionComponent#RD_SLOT_COUNT}-element
     *     array, indexed by slot, {@code null} for an unoccupied slot
     */
    private static Map<String, SetSpecialist[]> groupActiveResearchersBySlot(PlayerSnapshot snapshot,
            ContentCatalog catalog) {
        Map<String, SetSpecialist[]> result = new LinkedHashMap<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            if (specialist.getAssignmentState() != AssignmentState.ASSIGNED) {
                continue;
            }
            String targetId = specialist.getRdTargetComponentId();
            if (targetId == null) {
                continue;
            }
            Integer slot = specialist.getRdSlot();
            if (slot == null || slot < 0 || slot >= MissionComponent.RD_SLOT_COUNT) {
                continue;
            }
            if (snapshot.getMissionComponents().get(targetId) == null) {
                continue;
            }
            if (catalog.missionComponent(targetId).isEmpty()) {
                continue;
            }
            SetSpecialist[] slots = result.computeIfAbsent(targetId,
                id -> new SetSpecialist[MissionComponent.RD_SLOT_COUNT]);
            if (slots[slot] != null) {
                // Slot collision: first-encountered-in-roster-order wins, see this class's own Javadoc.
                continue;
            }
            slots[slot] = specialist;
        }
        return result;
    }

    /**
     * Advances one mission component's R&amp;D for this season: draws every
     * occupied slot's raw amount, computes each slot's nominal reliability
     * gain against the component's shared start-of-season reliability, sums
     * them, applies the stagnation guard and the ceiling clamp once, then
     * reports each contributing specialist's proportional share - see this
     * class's own Javadoc for the full contract.
     *
     * @param componentId    the target component's id
     * @param slots          the component's four R&amp;D slots, {@code null}
     *                       for an unoccupied one
     * @param snapshot       the snapshot the targeted component is owned in
     * @param catalog        the content catalog the targeted component's own
     *                       definition is looked up in
     * @param skillBeforeGain each researching specialist's skill as it stood
     *                       before this season's own flat skill gain
     * @param random         the random source this season's gains are drawn from
     * @param events         the list to append this component's
     *                       {@link ResearchCompleted} events to, in slot order
     */
    private static void advanceComponent(String componentId, SetSpecialist[] slots, PlayerSnapshot snapshot,
            ContentCatalog catalog, Map<SetSpecialist, Double> skillBeforeGain, RandomGenerator random,
            List<ResolutionEvent> events) {
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
        double startReliability = owned.getCurrentReliability();

        double[] nominalGains = new double[MissionComponent.RD_SLOT_COUNT];
        double nominalTotal = 0.0;
        for (int slot = 0; slot < MissionComponent.RD_SLOT_COUNT; slot++) {
            SetSpecialist specialist = slots[slot];
            if (specialist == null) {
                continue;
            }
            double rawAmount = SkillGrowth.drawRdSlotRawAmount(component.rdSlotBandMaxAmounts(), slot, random);
            double skillBefore = skillBeforeGain.get(specialist);
            double nominalGain = SkillGrowth.rdReliabilityGain(rawAmount, startReliability,
                component.maxRdReliability(), skillBefore) * snapshot.getRdResultsMultiplier();
            nominalGains[slot] = nominalGain;
            nominalTotal += nominalGain;
        }

        double actualAppliedTotal = 0.0;
        if (nominalTotal > 0.0) {
            double headroom = Math.max(0.0, component.maxRdReliability() - startReliability);
            double appliedTotal = headroom < HEADROOM_STAGNATION_THRESHOLD ? headroom : nominalTotal;
            double newReliability = Math.min(component.maxRdReliability(), startReliability + appliedTotal);
            actualAppliedTotal = newReliability - startReliability;
            owned.setCurrentReliability(newReliability);
        }

        // Always one event per occupied slot, even when nominalTotal is 0 (e.g. the component
        // is already at its R&D ceiling, or every occupied slot's assignee has 0 skill) - the
        // flat skill gain from pass 1 already happened for these specialists and must still be
        // reported, just with a 0 reliabilityGain share rather than a division by zero.
        for (int slot = 0; slot < MissionComponent.RD_SLOT_COUNT; slot++) {
            SetSpecialist specialist = slots[slot];
            if (specialist == null) {
                continue;
            }
            double share = nominalTotal > 0.0 ? nominalGains[slot] / nominalTotal * actualAppliedTotal : 0.0;
            events.add(new ResearchCompleted(specialist.getId(), specialist.getName(), componentId,
                specialist.getSkill(component.associatedSkill()) - skillBeforeGain.get(specialist), share, slot));
        }

        // Re-deriving baspm.md §10.8's own release-at-the-cap mechanic (see this class's own
        // Javadoc and ResearchComponentMaxed): checked against the reliability as it stands at
        // the end of this season, whether or not it actually changed this season - a component
        // already sitting at its cap when a specialist is first assigned to it releases them the
        // very next season, same as one that just reached the cap this season.
        ResearchComponentMaxed maxedEvent =
            releaseResearchersIfMaxed(snapshot, componentId, owned.getCurrentReliability(),
                component.maxRdReliability());
        if (maxedEvent != null) {
            events.add(maxedEvent);
        }
    }

    /**
     * Releases every {@code SET} specialist currently
     * {@link AssignmentState#ASSIGNED} to {@code componentId}'s R&amp;D back
     * to {@link AssignmentState#IDLE} if {@code currentReliability} has
     * reached {@code maxRdReliability} - the shared release-at-the-cap
     * mechanic both this class's own R&amp;D pass and
     * {@link MissionProgress}'s post-mission-season reliability change use
     * ({@code baspm.md} &sect;10.4/&sect;10.8 both call the identical
     * {@code UnassignAllSETPersonnelFromMissionComponent}, one from a
     * flight outcome, the other from an R&amp;D season - see this class's
     * own Javadoc). Scans the whole roster by target id rather than a
     * slots array, so it works regardless of which resolver is calling it.
     *
     * @param snapshot            the player snapshot whose SET roster to scan
     * @param componentId         the component to check and release researchers from
     * @param currentReliability  the component's reliability as it stands
     *                            right now (after whichever resolver's own
     *                            change this season, if any)
     * @param maxRdReliability    the component's own R&amp;D reliability ceiling
     * @return a {@link ResearchComponentMaxed} event if anyone was
     *     released, or {@code null} if {@code currentReliability} is still
     *     below {@code maxRdReliability}, or nobody was assigned to this
     *     component anyway
     */
    static ResearchComponentMaxed releaseResearchersIfMaxed(PlayerSnapshot snapshot, String componentId,
            double currentReliability, double maxRdReliability) {
        if (currentReliability < maxRdReliability) {
            return null;
        }
        List<String> releasedPersonnelIds = releaseResearchers(snapshot, componentId);
        if (releasedPersonnelIds.isEmpty()) {
            return null;
        }
        List<String> names = releasedPersonnelIds.stream()
            .map(id -> snapshot.getSetSpecialists().stream().filter(specialist -> specialist.getId().equals(id))
                .map(SetSpecialist::getName).findFirst().orElse(id))
            .toList();
        return new ResearchComponentMaxed(componentId, releasedPersonnelIds, names);
    }

    /**
     * Releases every SET specialist researching a component back to idle - used when
     * the component reaches its R&amp;D ceiling, and when it goes inactive because the
     * last program using it closed (BASPM's {@code UnassignAllSETPersonnelFromMissionComponent},
     * {@code baspm.md} &sect;10.8/&sect;10.9).
     *
     * @param snapshot    the player; mutated in place
     * @param componentId the component
     * @return the released specialists' ids, possibly empty
     */
    static List<String> releaseResearchers(PlayerSnapshot snapshot, String componentId) {
        List<String> releasedPersonnelIds = new ArrayList<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            if (specialist.getAssignmentState() == AssignmentState.ASSIGNED
                    && componentId.equals(specialist.getRdTargetComponentId())) {
                specialist.setAssignmentState(AssignmentState.IDLE);
                specialist.setRdTargetComponentId(null);
                specialist.setRdSlot(null);
                releasedPersonnelIds.add(specialist.getId());
            }
        }
        return releasedPersonnelIds;
    }
}
