package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.InvestigationCauses;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.Incident;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * <p>Incident investigations ({@code design.md} &sect;2.14). A flight that ends in a failure is recorded as an
 * {@link Incident} the moment its failing step resolves ({@link #record}, called by {@link MissionProgress}).
 * Without a lost crew, the agency may investigate it itself: an SET specialist leads for a season, for a small
 * fee, and on success a share of each component's loss comes back. With a lost crew, an independent board
 * investigates, and reports on its own clock, the turn crewed flights may fly again: flavour only.</p>
 *
 * <p>The odds and the share are stamped when an investigation starts ({@link #start}), so the dialog's
 * prediction is what's rolled; the roll uses the start action's server seed, which the client never sees. The
 * culprit and the causes are drawn from the agency's salted seed, never the season's random stream, so
 * playouts stay comparable with investigations on or off.</p>
 *
 * <p>Every number is unconfirmed (&sect;4).</p>
 *
 * @author Mario Koehler
 */
public final class Investigations {

    /** The turns an agency investigation is offered for, counting the one after the failure. */
    public static final int OFFER_TURNS = 2;

    /** The fee, flat and small (user, 2026-11-14): the price that matters is the lead's season. */
    public static final long FEE = 100;

    /** The chance at a match of 0. */
    public static final double BASE_CHANCE = 0.40;

    /** The chance added per point of match. */
    public static final double CHANCE_PER_POINT = 0.005;

    /** The highest chance. */
    public static final double MAX_CHANCE = 0.90;

    /** The share of a loss given back at a perfect match (user, 2026-11-14: the cap, not the guarantee). */
    public static final double MAX_SHARE = 2.0 / 3.0;

    /** The lead's morale on success. */
    public static final int SUCCESS_MORALE = 5;

    /** The outcome a career line records for a found cause. */
    public static final String FOUND = "FOUND";

    /** The outcome a career line records for an undetermined one. */
    public static final String UNDETERMINED = "UNDETERMINED";

    private static final long CULPRIT_SALT = 0x1C1D_E07C_1967_0127L;
    private static final long ROLL_SALT = 0x7E57_B0A2_D196_1104L;
    private static final long BOARD_SALT = 0x0B0A_2D00_1986_0128L;
    private static final long PULL_SALT = 0x7A11_0E70_1965_0318L;

    private static volatile boolean enabled = !"off".equals(System.getProperty("rules.investigations"));

    /**
     * What starting or withdrawing an investigation did.
     */
    public enum Outcome {
        /** Started, with the lead and the fee. */
        STARTED,
        /** Withdrawn: the fee is back, the lead back at work. */
        WITHDRAWN,
        /** No such incident waits for an investigation (lapsed, settled, or the board's). */
        NO_INCIDENT,
        /** It's started already. */
        ALREADY_STARTED,
        /** It isn't started, so there's nothing to withdraw. */
        NOT_STARTED,
        /** No SET specialist with that id. */
        UNKNOWN_LEAD,
        /** The specialist isn't free: neither idle, on R&amp;D nor in training past basic, or unavailable. */
        LEAD_NOT_FREE,
        /** The funds don't cover the fee. */
        INSUFFICIENT_FUNDS,
        /** Investigations are switched off (playouts only). */
        DISABLED
    }

    /**
     * A lead's prospects, as the choose-a-lead dialog shows them.
     *
     * @param match    the match, 0 to 100
     * @param chance   the chance the cause is found, 0 to 1
     * @param share    the share of each loss given back on success, 0 to 1
     * @param recovery the reliability given back on success, all components together
     * @param forgone  the R&amp;D the lead's own component would expect to lose this season, 0 when not researching
     * @param kept     the share of the training round in progress a pull-out keeps, 0 to 1; -1 when not training
     */
    public record Prospect(double match, double chance, double share, double recovery, double forgone,
                           double kept) {

        /**
         * Returns the reliability to expect, the chance times the recovery.
         *
         * @return the expected points
         */
        public double expected() {
            return chance * recovery;
        }
    }

    private Investigations() {
    }

    /**
     * Switches investigations on or off, for playouts ({@code -Drules.investigations=off} does the same).
     *
     * @param on whether failures are investigated
     */
    public static void setEnabled(boolean on) {
        enabled = on;
    }

    /**
     * Returns whether investigations are on.
     *
     * @return {@code true} unless a playout switched them off
     */
    public static boolean enabled() {
        return enabled;
    }

    // ---- Recording

    /**
     * Records a flight that just failed ({@code MissionProgress}), after its season's reliability changes.
     * A lost crew opens the board's investigation; otherwise the agency is offered its own, unless the
     * failure cost nothing (every component at its floor).
     *
     * @param snapshot      the player
     * @param catalog       the content pack
     * @param configuration the configuration that failed
     * @param mission       the flight
     * @param step          the step that failed
     * @param losses        what each of the failing step's components lost, rocket slots resolved
     * @param crewIds       the crew lost, empty when none was
     * @param crewNames     their names
     * @param gameTurn      the turn being resolved
     * @param events        the list to add the offer or the board's opening to
     */
    static void record(PlayerSnapshot snapshot, ContentCatalog catalog, MissionConfiguration configuration,
                       InProgressMission mission, MissionStep step, Map<String, Double> losses, List<String> crewIds,
                       List<String> crewNames, int gameTurn, List<ResolutionEvent> events) {
        if (!enabled) {
            return;
        }
        List<Incident.Part> parts = new ArrayList<>();
        for (var involvement : step.componentInvolvements()) {
            String componentId = MissionLaunch.resolveComponentId(involvement.componentId(), mission.getRocketIds());
            if (parts.stream().noneMatch(p -> p.componentId().equals(componentId))) {
                parts.add(new Incident.Part(componentId, involvement.percent(),
                    Math.max(0, losses.getOrDefault(componentId, 0.0))));
            }
        }
        if (parts.isEmpty()) {
            return;
        }
        boolean crewLost = !crewIds.isEmpty();
        String culprit = culprit(snapshot, mission, parts);
        Incident incident = new Incident(mission.getId(), crewLost ? Incident.Kind.OFFICIAL : Incident.Kind.AGENCY,
            gameTurn, configuration.id(), step.id(), parts, culprit, crewIds, crewNames, null, null, null, 0, 0, 0, 0);
        if (crewLost) {
            snapshot.getIncidents().add(incident);
            events.add(new OfficialInvestigationOpened(incident.id(), configuration.id(), step.id(),
                gameTurn + CrewedGrounding.GROUNDING_SEASONS));
        } else if (incident.totalLoss() > 0) {
            snapshot.getIncidents().add(incident);
            events.add(new InvestigationOffered(incident.id(), configuration.id(), step.id(), incident.totalLoss(),
                gameTurn + OFFER_TURNS));
        }
    }

    /**
     * Draws the component a report blames: by involvement times its unreliability as flown, so the weak link is
     * blamed most often.
     */
    private static String culprit(PlayerSnapshot snapshot, InProgressMission mission, List<Incident.Part> parts) {
        double[] weights = new double[parts.size()];
        double total = 0;
        for (int i = 0; i < parts.size(); i++) {
            double flown = MissionProgress.flyingReliability(snapshot, mission, parts.get(i).componentId());
            weights[i] = parts.get(i).involvement() * Math.max(0, 100 - flown);
            total += weights[i];
        }
        if (total <= 0) {
            return parts.stream().max(Comparator.comparingInt(Incident.Part::involvement)).orElseThrow().componentId();
        }
        double roll = new SplittableRandom(Intelligence.seed(snapshot.getAgencyName(), "incident/" + mission.getId())
            ^ CULPRIT_SALT).nextDouble() * total;
        for (int i = 0; i < parts.size(); i++) {
            roll -= weights[i];
            if (roll < 0) {
                return parts.get(i).componentId();
            }
        }
        return parts.getLast().componentId();
    }

    // ---- The odds

    /**
     * Returns the incident waiting for an agency investigation, or under way, by id.
     *
     * @param snapshot   the player
     * @param incidentId the incident
     * @return the incident, if it's the agency's and still open
     */
    public static Optional<Incident> agencyIncident(PlayerSnapshot snapshot, String incidentId) {
        return snapshot.getIncidents().stream()
            .filter(i -> i.kind() == Incident.Kind.AGENCY && i.id().equals(incidentId)).findFirst();
    }

    /**
     * Returns the agency's incidents still offered or under way, oldest first.
     *
     * @param snapshot the player
     * @return the incidents
     */
    public static List<Incident> agencyIncidents(PlayerSnapshot snapshot) {
        return snapshot.getIncidents().stream().filter(i -> i.kind() == Incident.Kind.AGENCY).toList();
    }

    /**
     * Returns the board investigations still running, oldest first.
     *
     * @param snapshot the player
     * @return the incidents
     */
    public static List<Incident> officialIncidents(PlayerSnapshot snapshot) {
        return snapshot.getIncidents().stream().filter(i -> i.kind() == Incident.Kind.OFFICIAL).toList();
    }

    /**
     * Returns the last turn an offered investigation can be started in.
     *
     * @param incident the incident
     * @return the turn
     */
    public static int lastTurn(Incident incident) {
        return incident.turn() + OFFER_TURNS;
    }

    /**
     * Returns the turn whose resolution issues a board's report.
     *
     * @param incident the incident
     * @return the turn
     */
    public static int dueTurn(Incident incident) {
        return incident.turn() + CrewedGrounding.GROUNDING_SEASONS;
    }

    /**
     * Returns how well a specialist's skills match the failing step: their skill in each component's research
     * group, as R&amp;D reads it (times {@link PayExpectations#skillFactor}), weighted by the component's
     * involvement in the step.
     *
     * @param lead     the specialist
     * @param catalog  the content pack
     * @param incident the incident
     * @return the match, 0 to 100
     */
    public static double match(SetSpecialist lead, ContentCatalog catalog, Incident incident) {
        double weighted = 0;
        int involvement = 0;
        for (Incident.Part part : incident.components()) {
            MissionComponent component = catalog.missionComponent(part.componentId()).orElse(null);
            if (component == null) {
                continue;
            }
            weighted += part.involvement() * lead.getSkill(component.associatedSkill());
            involvement += part.involvement();
        }
        if (involvement == 0) {
            return 0;
        }
        return Math.clamp(weighted / involvement * PayExpectations.skillFactor(lead), 0.0, 100.0);
    }

    /**
     * Returns the chance an investigation finds the cause.
     *
     * @param match the lead's match, 0 to 100
     * @return the chance, 0 to 1
     */
    public static double chance(double match) {
        return Math.min(MAX_CHANCE, BASE_CHANCE + CHANCE_PER_POINT * Math.clamp(match, 0.0, 100.0));
    }

    /**
     * Returns the share of each loss a found cause gives back.
     *
     * @param match the lead's match, 0 to 100
     * @return the share, 0 to {@link #MAX_SHARE}
     */
    public static double share(double match) {
        return MAX_SHARE * Math.clamp(match, 0.0, 100.0) / 100.0;
    }

    /**
     * Returns what a found cause would give back today, all components together, clamped at each component's
     * maximum.
     *
     * @param snapshot the player
     * @param catalog  the content pack
     * @param incident the incident
     * @param share    the share
     * @return the points
     */
    public static double recovery(PlayerSnapshot snapshot, ContentCatalog catalog, Incident incident, double share) {
        double total = 0;
        for (Incident.Part part : incident.components()) {
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(part.componentId());
            MissionComponent component = catalog.missionComponent(part.componentId()).orElse(null);
            if (owned == null || component == null) {
                continue;
            }
            double current = owned.getCurrentReliability();
            total += Math.max(0, Math.min(component.maxReliability(), current + share * part.loss()) - current);
        }
        return total;
    }

    /**
     * Returns the specialists who could lead: idle, on R&amp;D or in training past basic (user, 2026-11-14: everyone
     * in one place, the cost of pulling someone out shown beside them), and available, best match first.
     *
     * @param snapshot the player
     * @param catalog  the content pack
     * @param incident the incident
     * @return the specialists
     */
    public static List<SetSpecialist> candidates(PlayerSnapshot snapshot, ContentCatalog catalog, Incident incident) {
        return snapshot.getSetSpecialists().stream().filter(Investigations::free)
            .sorted(Comparator.comparingDouble((SetSpecialist s) -> match(s, catalog, incident)).reversed())
            .toList();
    }

    /**
     * Returns a specialist's prospects as a lead.
     *
     * @param snapshot the player
     * @param catalog  the content pack
     * @param incident the incident
     * @param lead     the specialist
     * @return the prospects
     */
    public static Prospect prospect(PlayerSnapshot snapshot, ContentCatalog catalog, Incident incident,
                                    SetSpecialist lead) {
        double match = match(lead, catalog, incident);
        double share = share(match);
        return new Prospect(match, chance(match), share, recovery(snapshot, catalog, incident, share),
            forgone(snapshot, catalog, lead), kept(lead));
    }

    /**
     * Returns the share of the training round in progress a specialist pulled out to lead would keep
     * ({@link SkillGrowth#partialCreditShare}, as pulling out of training does).
     *
     * @param lead the specialist
     * @return the share, 0 to 1; -1 when not in training
     */
    public static double kept(SetSpecialist lead) {
        if (lead.getAssignmentState() != AssignmentState.IN_TRAINING) {
            return -1;
        }
        return lead.getTrainingSeasonsTotal() <= 0 ? 0
            : SkillGrowth.partialCreditShare(lead.getTrainingSeasonsCompleted(), lead.getTrainingSeasonsTotal());
    }

    /**
     * Returns the R&amp;D a specialist's component would expect to lose this season if they left it: the
     * team's expected gain with them, less without.
     *
     * @param snapshot the player
     * @param catalog  the content pack
     * @param lead     the specialist
     * @return the points, 0 when they aren't researching
     */
    public static double forgone(PlayerSnapshot snapshot, ContentCatalog catalog, SetSpecialist lead) {
        String componentId = lead.getRdTargetComponentId();
        if (lead.getAssignmentState() != AssignmentState.ASSIGNED || componentId == null) {
            return 0;
        }
        double with = ResearchPlanning.expectedSeasonGain(snapshot, catalog, componentId);
        lead.setAssignmentState(AssignmentState.IDLE);
        try {
            return Math.max(0, with - ResearchPlanning.expectedSeasonGain(snapshot, catalog, componentId));
        } finally {
            lead.setAssignmentState(AssignmentState.ASSIGNED);
        }
    }

    /**
     * Returns the lead a bot picks: the most reliability expected back, less the R&amp;D left behind; someone in
     * training only when nobody else is free.
     *
     * @param snapshot the player
     * @param catalog  the content pack
     * @param incident the incident
     * @return the lead, or empty when nobody is free
     */
    public static Optional<SetSpecialist> bestLead(PlayerSnapshot snapshot, ContentCatalog catalog,
                                                   Incident incident) {
        List<SetSpecialist> free = candidates(snapshot, catalog, incident);
        List<SetSpecialist> working = free.stream()
            .filter(lead -> lead.getAssignmentState() != AssignmentState.IN_TRAINING).toList();
        return (working.isEmpty() ? free : working).stream().max(Comparator.comparingDouble(lead -> {
            Prospect prospect = prospect(snapshot, catalog, incident, lead);
            return prospect.expected() - prospect.forgone();
        }));
    }

    private static boolean free(SetSpecialist specialist) {
        if (specialist.getUnavailableSeasons() > 0) {
            return false;
        }
        return switch (specialist.getAssignmentState()) {
            case IDLE, ASSIGNED -> true;
            // Basic training can't be cut short (TrainingProgress.pullOutOfTraining).
            case IN_TRAINING -> specialist.getTrainingKind() != de.mkoehler.launchwindow.model.TrainingKind.BASIC;
            default -> false;
        };
    }

    // ---- Starting and withdrawing

    /**
     * Starts the agency's investigation of an incident: the lead leaves their R&amp;D for the season, or is pulled
     * out of training as {@code PullOutOfTrainingAction} would (keeping a share of the round), the fee is paid, and
     * the odds are stamped.
     *
     * @param snapshot   the player
     * @param catalog    the content pack
     * @param incidentId the incident
     * @param leadId     the SET specialist to lead it
     * @param seed       the start action's server seed, which settles it
     * @return what happened
     */
    public static Outcome start(PlayerSnapshot snapshot, ContentCatalog catalog, String incidentId, String leadId,
                                long seed) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        if (!enabled) {
            return Outcome.DISABLED;
        }
        Incident incident = agencyIncident(snapshot, incidentId).orElse(null);
        if (incident == null) {
            return Outcome.NO_INCIDENT;
        }
        if (incident.started()) {
            return Outcome.ALREADY_STARTED;
        }
        SetSpecialist lead = specialist(snapshot, leadId);
        if (lead == null) {
            return Outcome.UNKNOWN_LEAD;
        }
        if (!free(lead)) {
            return Outcome.LEAD_NOT_FREE;
        }
        if (snapshot.getFunds() < FEE) {
            return Outcome.INSUFFICIENT_FUNDS;
        }
        Prospect prospect = prospect(snapshot, catalog, incident, lead);
        String returnTo = null;
        if (lead.getAssignmentState() == AssignmentState.ASSIGNED) {
            returnTo = lead.getRdTargetComponentId();
            PersonnelActions.releaseResearch(snapshot, leadId);
        } else if (lead.getAssignmentState() == AssignmentState.IN_TRAINING
                && TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.SET,
                    leadId, new SplittableRandom(seed ^ PULL_SALT)) != PullOutOfTrainingOutcome.PULLED_OUT) {
            return Outcome.LEAD_NOT_FREE;
        }
        lead.setAssignmentState(AssignmentState.INVESTIGATING);
        snapshot.setFunds(snapshot.getFunds() - FEE);
        replace(snapshot, incident.startedBy(lead.getId(), lead.getName(), returnTo, FEE, prospect.chance(),
            prospect.share(), seed));
        return Outcome.STARTED;
    }

    /**
     * Withdraws an investigation before the turn resolves: the fee comes back, and the lead goes back to the
     * R&amp;D they left, if there's still room, or idle. Someone pulled out of training stays out: the round is
     * over, as any pull-out ends it.
     *
     * @param snapshot   the player
     * @param catalog    the content pack
     * @param incidentId the incident
     * @return what happened
     */
    public static Outcome withdraw(PlayerSnapshot snapshot, ContentCatalog catalog, String incidentId) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Incident incident = agencyIncident(snapshot, incidentId).orElse(null);
        if (incident == null) {
            return Outcome.NO_INCIDENT;
        }
        if (!incident.started()) {
            return Outcome.NOT_STARTED;
        }
        snapshot.setFunds(snapshot.getFunds() + incident.fee());
        releaseLead(snapshot, catalog, incident);
        replace(snapshot, incident.withdrawn());
        return Outcome.WITHDRAWN;
    }

    // ---- The season

    /**
     * Settles the season's investigations, after its R&amp;D: every agency investigation that ran, every offer
     * past its last turn, and every board report due.
     *
     * @param snapshot the player
     * @param catalog  the content pack
     * @param gameTurn the turn being resolved
     * @return the events
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog, int gameTurn) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        List<ResolutionEvent> events = new ArrayList<>();
        InvestigationCauses causes = InvestigationCauses.standard();
        for (Incident incident : List.copyOf(snapshot.getIncidents())) {
            if (incident.kind() == Incident.Kind.OFFICIAL) {
                if (gameTurn >= dueTurn(incident)) {
                    events.add(issueReport(snapshot, catalog, causes, incident));
                    snapshot.getIncidents().remove(incident);
                }
            } else if (incident.started()) {
                events.addAll(conclude(snapshot, catalog, causes, incident, gameTurn));
                snapshot.getIncidents().remove(incident);
            } else if (gameTurn >= lastTurn(incident)) {
                events.add(new InvestigationLapsed(incident.id(), incident.missionConfigurationId()));
                snapshot.getIncidents().remove(incident);
            }
        }
        return events;
    }

    private static List<ResolutionEvent> conclude(PlayerSnapshot snapshot, ContentCatalog catalog,
                                                  InvestigationCauses causes, Incident incident, int gameTurn) {
        List<ResolutionEvent> events = new ArrayList<>();
        SplittableRandom random = new SplittableRandom(incident.seed() ^ ROLL_SALT);
        boolean found = random.nextDouble() < incident.chance();
        List<InvestigationConcluded.Recovery> recoveries = new ArrayList<>();
        String causeId = null;
        if (found) {
            for (Incident.Part part : incident.components()) {
                OwnedMissionComponent owned = snapshot.getMissionComponents().get(part.componentId());
                MissionComponent component = catalog.missionComponent(part.componentId()).orElse(null);
                if (owned == null || component == null || part.loss() <= 0) {
                    continue;
                }
                double before = owned.getCurrentReliability();
                double after = Math.max(before, Math.min(component.maxReliability(),
                    before + incident.share() * part.loss()));
                if (after <= before) {
                    continue;
                }
                owned.setCurrentReliability(after);
                recoveries.add(new InvestigationConcluded.Recovery(part.componentId(), after - before));
                ResearchComponentMaxed maxed = ResearchProgress.releaseResearchersIfMaxed(snapshot,
                    part.componentId(), after, TraitEffects.effectiveCap(snapshot, component, List.of(), false));
                if (maxed != null) {
                    events.add(maxed);
                }
            }
            causeId = causes.pick(group(catalog, incident), phase(catalog, causes, incident), false, random,
                snapshot.getUsedInvestigationCauseIds());
        }
        SetSpecialist lead = specialist(snapshot, incident.leadId());
        int morale = 0;
        if (lead != null) {
            if (found) {
                int before = lead.getMorale();
                lead.setMorale(Math.clamp(before + SUCCESS_MORALE, 0, 100));
                morale = lead.getMorale() - before;
            }
            lead.getCareer().add(new CareerEntry(gameTurn, CareerEntry.Kind.INVESTIGATION,
                incident.missionConfigurationId(), incident.stepId(), found ? FOUND : UNDETERMINED,
                recoveries.stream().mapToDouble(InvestigationConcluded.Recovery::points).sum(), 0));
        }
        releaseLead(snapshot, catalog, incident);
        events.addFirst(new InvestigationConcluded(incident.id(), incident.missionConfigurationId(),
            incident.stepId(), incident.leadId(), incident.leadName(), found, causeId, incident.culpritComponentId(),
            recoveries, morale));
        return events;
    }

    private static OfficialReportIssued issueReport(PlayerSnapshot snapshot, ContentCatalog catalog,
                                                    InvestigationCauses causes, Incident incident) {
        SplittableRandom random = new SplittableRandom(Intelligence.seed(snapshot.getAgencyName(),
            "board/" + incident.id()) ^ BOARD_SALT);
        String causeId = causes.pick(group(catalog, incident), phase(catalog, causes, incident), true, random,
            snapshot.getUsedInvestigationCauseIds());
        String factorId = causes.factors().isEmpty() ? null
            : causes.factors().get(random.nextInt(causes.factors().size())).id();
        return new OfficialReportIssued(incident.id(), incident.missionConfigurationId(), incident.stepId(),
            incident.turn(), causeId, factorId, incident.culpritComponentId(), incident.crewIds(),
            incident.crewNames());
    }

    /**
     * Returns the culprit's group, for the causes.
     *
     * @param catalog  the content pack
     * @param incident the incident
     * @return the group; a spacecraft's when the culprit isn't in the pack any more
     */
    public static InvestigationCauses.Group group(ContentCatalog catalog, Incident incident) {
        return catalog.missionComponent(incident.culpritComponentId())
            .map(c -> InvestigationCauses.Group.of(c.associatedSkill())).orElse(InvestigationCauses.Group.SPACECRAFT);
    }

    /**
     * Returns the failing step's phase, for the causes: by its kind, and by the flight's body and program where the
     * kind alone can't tell.
     *
     * @param catalog  the content pack
     * @param causes   the words
     * @param incident the incident
     * @return the phase
     */
    public static String phase(ContentCatalog catalog, InvestigationCauses causes, Incident incident) {
        MissionConfiguration configuration = catalog.missionConfiguration(incident.missionConfigurationId())
            .orElse(null);
        return configuration == null ? causes.phaseOf(Findings.kind(incident.stepId()))
            : causes.phaseOf(Findings.kind(incident.stepId()), Findings.body(catalog, configuration).name(),
                configuration.programId());
    }

    // ---- Helpers

    private static void releaseLead(PlayerSnapshot snapshot, ContentCatalog catalog, Incident incident) {
        SetSpecialist lead = specialist(snapshot, incident.leadId());
        if (lead == null || lead.getAssignmentState() != AssignmentState.INVESTIGATING) {
            return;
        }
        lead.setAssignmentState(AssignmentState.IDLE);
        if (incident.returnComponentId() != null) {
            PersonnelActions.assignResearch(snapshot, lead.getId(), incident.returnComponentId(), catalog);
        }
    }

    private static void replace(PlayerSnapshot snapshot, Incident incident) {
        List<Incident> incidents = snapshot.getIncidents();
        for (int i = 0; i < incidents.size(); i++) {
            if (incidents.get(i).id().equals(incident.id())) {
                incidents.set(i, incident);
                return;
            }
        }
    }

    private static SetSpecialist specialist(PlayerSnapshot snapshot, String id) {
        if (id == null) {
            return null;
        }
        return snapshot.getSetSpecialists().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Returns the names of a crew about to be lost, in the mission's order, for the board's report.
     *
     * @param snapshot the player
     * @param mission  the flight
     * @return the names
     */
    static List<String> crewNames(PlayerSnapshot snapshot, InProgressMission mission) {
        List<String> names = new ArrayList<>();
        for (String id : mission.crewIds()) {
            snapshot.getAstronauts().stream().filter(a -> a.getId().equals(id)).map(Astronaut::getName).findFirst()
                .ifPresent(names::add);
        }
        return names;
    }
}
