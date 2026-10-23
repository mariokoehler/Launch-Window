package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Dossier;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelFact;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PersonnelFile;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Tidbit;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * Settles a turn's intelligence for every agency at once ({@code design.md} &sect;2.9): every queued
 * operation, the catches it causes on the other side, and the passive tidbits. It reads each agency's
 * state at the start of the turn with the turn's actions applied, before anyone's season pass, and
 * rolls everything from its own seeds (the game's, the turn's, the agency's and the operation's), so
 * the same inputs always give the same findings, whatever order agencies resolve in. A rival's real
 * values never leave this class: findings carry only ranges.
 *
 * @author Mario Koehler
 */
public final class IntelligenceBoard {

    private static final long TIDBIT_SALT = 0x71DB_17C0_FFEE_1962L;
    private static final long OFFER_SALT = 0x0FFE_5EED_1945_0A55L;

    private IntelligenceBoard() {
    }

    /**
     * Settles the turn.
     *
     * @param agencies every agency's state, by player id, the turn's actions applied
     * @param catalog  the content catalog
     * @param gameSeed the game's seed
     * @param gameTurn the turn being resolved
     * @return each agency's findings, by player id; agencies with none are left out
     */
    public static Map<String, List<IntelFinding>> settle(Map<String, PlayerSnapshot> agencies, ContentCatalog catalog,
            long gameSeed, long gameTurn) {
        Objects.requireNonNull(agencies, "agencies must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Map<String, PlayerSnapshot> sorted = new TreeMap<>(agencies);
        Map<String, List<IntelFinding>> findings = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerSnapshot> spy : sorted.entrySet()) {
            PlayerSnapshot agency = spy.getValue();
            // Whom this agency's personnel files reached this turn, as "rival/person": never twice in a turn.
            Set<String> filedThisTurn = new HashSet<>();
            for (IntelOperation operation : agency.getIntelligence().getQueued()) {
                if (operation.kind() == IntelOperation.Kind.OFFER) {
                    continue;
                }
                SplittableRandom random = new SplittableRandom(seed(gameSeed, gameTurn, spy.getKey(), operation.id()));
                settleOperation(spy.getKey(), agency, operation, sorted, catalog, (int) gameTurn, random, findings,
                    filedThisTurn);
            }
        }
        settleOffers(sorted, catalog, gameSeed, gameTurn, findings);
        for (Map.Entry<String, PlayerSnapshot> spy : sorted.entrySet()) {
            int level = spy.getValue().getIntelligence().getLevel();
            if (Intelligence.TIDBIT_PERCENT[level] == 0) {
                continue;
            }
            SplittableRandom random = new SplittableRandom(seed(gameSeed, gameTurn, spy.getKey(), "tidbit")
                ^ TIDBIT_SALT);
            if (random.nextInt(100) >= Intelligence.TIDBIT_PERCENT[level]) {
                continue;
            }
            List<String> rivals = sorted.keySet().stream().filter(id -> !id.equals(spy.getKey())).toList();
            if (rivals.isEmpty()) {
                continue;
            }
            String rivalId = rivals.get(random.nextInt(rivals.size()));
            PlayerSnapshot rival = sorted.get(rivalId);
            int rivalLevel = rival.getIntelligence().getLevel();
            if (random.nextInt(100) >= Intelligence.successPercent(rivalLevel)) {
                continue;
            }
            List<IntelArea> areas = level >= Intelligence.PERSONNEL_FILE_LEVEL
                ? List.of(IntelArea.RND, IntelArea.PLANS, IntelArea.PERSONNEL) : List.of(IntelArea.RND, IntelArea.PLANS);
            IntelArea area = areas.get(random.nextInt(areas.size()));
            List<IntelFact> facts = facts(rival, area, catalog, Intelligence.halfWidth(level, rivalLevel), random);
            if (facts.isEmpty()) {
                continue;
            }
            IntelFact fact = facts.get(random.nextInt(facts.size()));
            findings.computeIfAbsent(spy.getKey(), id -> new ArrayList<>()).add(new IntelFinding(
                IntelFinding.Kind.TIDBIT, null, null, rivalId, rival.getAgencyName(), area, null, null,
                new Tidbit((int) gameTurn, fact), -1));
        }
        return findings;
    }

    /**
     * Settles every poaching offer ({@code design.md} &sect;2.2(d)), highest raise first, so two
     * offers to one person settle the same way whatever order agencies resolve in: the first decides,
     * and later ones lapse. The target's standing order answers first; unmatched, the person rolls.
     */
    private static void settleOffers(Map<String, PlayerSnapshot> agencies, ContentCatalog catalog, long gameSeed,
            long gameTurn, Map<String, List<IntelFinding>> findings) {
        Set<String> settled = new HashSet<>();
        Map<String, Integer> arriving = new java.util.HashMap<>();
        for (Map.Entry<String, IntelOperation> entry : Poaching.allOffers(agencies)) {
            String poacherId = entry.getKey();
            IntelOperation offer = entry.getValue();
            PlayerSnapshot poacher = agencies.get(poacherId);
            PlayerSnapshot target = offer.targetId().equals(poacherId) ? null : agencies.get(offer.targetId());
            PersonnelGroup group = Poaching.canBeApproached(offer.group()) ? PersonnelGroup.valueOf(offer.group())
                : null;
            Personnel person = target == null || group == null ? null : Poaching.roster(target, group).stream()
                .filter(p -> p.getId().equals(offer.personId())).findFirst().orElse(null);
            List<IntelFinding> mine = findings.computeIfAbsent(poacherId, id -> new ArrayList<>());
            String targetAgency = target == null ? null : target.getAgencyName();
            if (person == null) {
                mine.add(offerFinding(IntelFinding.Kind.OFFER_MADE, offer.id(), offer.targetId(), targetAgency,
                    lapsed(offer, OfferResult.Outcome.LAPSED_GONE)));
                continue;
            }
            String key = offer.targetId() + "/" + person.getId();
            OfferResult.Outcome blocked = settled.contains(key) ? OfferResult.Outcome.LAPSED_NOT_ELIGIBLE
                : Poaching.ineligible(person, group, (int) gameTurn, poacherId);
            String arrivingKey = poacherId + "/" + group.name();
            if (blocked == null && !Poaching.hasRoom(poacher, group, arriving.getOrDefault(arrivingKey, 0), catalog)) {
                blocked = OfferResult.Outcome.LAPSED_NO_ROOM;
            }
            long newSalary = Poaching.offeredSalary(person.getSalary(), offer.raisePercent());
            if (blocked != null) {
                mine.add(offerFinding(IntelFinding.Kind.OFFER_MADE, offer.id(), offer.targetId(), targetAgency,
                    new OfferResult(person.getId(), person.getName(), group.name(), offer.raisePercent(), blocked,
                        person.getSalary(), newSalary, 0, null, null)));
                continue;
            }
            settled.add(key);
            OfferResult.Outcome outcome;
            if (Poaching.matches(target, person, group, offer.raisePercent())) {
                outcome = OfferResult.Outcome.MATCHED;
            } else {
                SplittableRandom random = new SplittableRandom(seed(gameSeed, gameTurn, poacherId, offer.id())
                    ^ OFFER_SALT);
                int chance = Poaching.acceptPercent(offer.raisePercent(), person.getLoyalty(), person.getMorale(),
                    person.getDisappointedSeasons() > 0, Poaching.traits(person));
                outcome = random.nextInt(100) < chance ? OfferResult.Outcome.JOINED : OfferResult.Outcome.TURNED_DOWN;
            }
            boolean joined = outcome == OfferResult.Outcome.JOINED;
            if (outcome == OfferResult.Outcome.MATCHED) {
                newSalary = Poaching.matchedSalary(person, newSalary);
            }
            if (joined) {
                arriving.merge(arrivingKey, 1, Integer::sum);
            }
            OfferResult result = new OfferResult(person.getId(), person.getName(), group.name(), offer.raisePercent(),
                outcome, person.getSalary(), newSalary, joined ? Poaching.signingBonus(newSalary) : 0,
                joined && person instanceof SetSpecialist engineer ? Poaching.copy(engineer) : null,
                joined && person instanceof de.mkoehler.launchwindow.model.MissionControlOfficer controller
                    ? Poaching.copy(controller) : null);
            mine.add(offerFinding(IntelFinding.Kind.OFFER_MADE, offer.id(), offer.targetId(), targetAgency, result));
            findings.computeIfAbsent(offer.targetId(), id -> new ArrayList<>()).add(offerFinding(
                IntelFinding.Kind.OFFER_RECEIVED, null, poacherId, poacher.getAgencyName(), result.plain()));
        }
    }

    private static OfferResult lapsed(IntelOperation offer, OfferResult.Outcome outcome) {
        return new OfferResult(offer.personId(), "", offer.group() == null ? "" : offer.group(), offer.raisePercent(),
            outcome, 0, 0, 0, null, null);
    }

    private static IntelFinding offerFinding(IntelFinding.Kind kind, String operationId, String rivalId,
            String rivalAgency, OfferResult offer) {
        return new IntelFinding(kind, operationId, IntelOperation.Kind.OFFER, rivalId, rivalAgency, null, null, null,
            null, -1, offer);
    }

    private static void settleOperation(String spyId, PlayerSnapshot spy, IntelOperation operation,
            Map<String, PlayerSnapshot> agencies, ContentCatalog catalog, int gameTurn, SplittableRandom random,
            Map<String, List<IntelFinding>> findings, Set<String> filedThisTurn) {
        List<IntelFinding> mine = findings.computeIfAbsent(spyId, id -> new ArrayList<>());
        PlayerSnapshot target = operation.targetId().equals(spyId) ? null : agencies.get(operation.targetId());
        if (target == null) {
            mine.add(IntelFinding.about(IntelFinding.Kind.NO_TARGET, operation, null, -1));
            return;
        }
        Personnel person = null;
        if (operation.kind() == IntelOperation.Kind.PERSONNEL_FILE) {
            RivalFolder folder = spy.getIntelligence().getFolders().get(operation.targetId());
            person = filePick(target, PersonnelGroup.valueOf(operation.group()), operation.personId(), folder,
                filedThisTurn, operation.targetId());
            if (person == null) {
                mine.add(IntelFinding.about(IntelFinding.Kind.NO_TARGET, operation, target.getAgencyName(), -1));
                return;
            }
        }
        int spyLevel = spy.getIntelligence().getLevel();
        int targetLevel = target.getIntelligence().getLevel();
        if (random.nextInt(100) >= Intelligence.successPercent(targetLevel)) {
            boolean caught = random.nextInt(100) < Intelligence.catchPercent(targetLevel);
            mine.add(IntelFinding.about(caught ? IntelFinding.Kind.CAUGHT : IntelFinding.Kind.FAILED, operation,
                target.getAgencyName(), caught ? targetLevel : -1));
            if (caught) {
                findings.computeIfAbsent(operation.targetId(), id -> new ArrayList<>()).add(new IntelFinding(
                    IntelFinding.Kind.CAUGHT_THEIRS, null, operation.kind(), spyId, spy.getAgencyName(),
                    operation.area(), null, null, null, spyLevel));
            }
            return;
        }
        int halfWidth = Intelligence.halfWidth(spyLevel, targetLevel);
        if (operation.kind() == IntelOperation.Kind.ASSESSMENT) {
            Dossier dossier = new Dossier(operation.area(), gameTurn,
                facts(target, operation.area(), catalog, halfWidth, random));
            mine.add(new IntelFinding(IntelFinding.Kind.DOSSIER, operation.id(), operation.kind(), operation.targetId(),
                target.getAgencyName(), operation.area(), dossier, null, null,
                operation.area() == IntelArea.PLANS ? targetLevel : -1));
            return;
        }
        List<IntelFact> skills = new ArrayList<>();
        for (Map.Entry<String, Double> skill : skills(person).entrySet()) {
            int[] range = range(skill.getValue(), halfWidth, random);
            skills.add(new IntelFact(IntelFact.Kind.SKILL, skill.getKey(), range[0], range[1], 0));
        }
        List<TraitState> traits = person.getTraits();
        Trait trait = traits.isEmpty() ? null : traits.get(random.nextInt(traits.size())).trait();
        String band = Careers.loyaltyBand(person.getLoyalty());
        filedThisTurn.add(operation.targetId() + "/" + person.getId());
        PersonnelFile file = new PersonnelFile(person.getId(), person.getName(), person.getPortraitId(),
            operation.group(), person.getAge(), gameTurn, skills, trait, band, person.getAmbition(),
            person.getDisappointedSeasons() > 0, person.getSalary(), person.getTenureSeasons(),
            person.getLastApproachedTurn(), person.getApproachedBy().contains(spyId));
        mine.add(new IntelFinding(IntelFinding.Kind.PERSONNEL_FILE, operation.id(), operation.kind(),
            operation.targetId(), target.getAgencyName(), null, null, file, null, -1));
    }

    /**
     * Picks a personnel file's subject: the named person if they're still there, else the group's
     * most skilled without a file yet, else the group's most skilled. Nobody filed by another of the
     * spy's operations this turn is picked again (user, 2026-10-23: three files on one person); with
     * everyone filed, there's nobody.
     */
    private static Personnel filePick(PlayerSnapshot target, PersonnelGroup group, String personId,
            RivalFolder folder, Set<String> filedThisTurn, String targetId) {
        List<Personnel> roster = new ArrayList<>(roster(target, group));
        roster.removeIf(person -> filedThisTurn.contains(targetId + "/" + person.getId()));
        if (personId != null) {
            for (Personnel person : roster) {
                if (person.getId().equals(personId)) {
                    return person;
                }
            }
        }
        Set<String> filed = new HashSet<>();
        if (folder != null) {
            folder.getPersonnelFiles().forEach(file -> filed.add(file.personId()));
        }
        Comparator<Personnel> bySkill = Comparator.comparingDouble(IntelligenceBoard::averageSkill);
        return roster.stream().filter(person -> !filed.contains(person.getId())).max(bySkill)
            .orElse(roster.stream().max(bySkill).orElse(null));
    }

    /**
     * Returns what one area of an agency shows, numbers as ranges of the given half-width.
     *
     * @param agency    the agency looked at
     * @param area      the area
     * @param catalog   the content catalog
     * @param halfWidth how wide a range is either side
     * @param random    the operation's random source
     * @return the facts, in a stable order
     */
    static List<IntelFact> facts(PlayerSnapshot agency, IntelArea area, ContentCatalog catalog, int halfWidth,
            SplittableRandom random) {
        List<IntelFact> facts = new ArrayList<>();
        switch (area) {
            case RND -> {
                for (Map.Entry<String, OwnedMissionComponent> owned : agency.getMissionComponents().entrySet()) {
                    if (owned.getValue().isActive()) {
                        int[] range = range(owned.getValue().getCurrentReliability(), halfWidth, random);
                        facts.add(new IntelFact(IntelFact.Kind.COMPONENT_RELIABILITY, owned.getKey(), range[0],
                            range[1], 0));
                    }
                }
            }
            case PLANS -> {
                agency.getOpenedProgramIds().forEach(id -> facts.add(new IntelFact(IntelFact.Kind.OPEN_PROGRAM, id,
                    0, 0, 0)));
                for (InProgressMission mission : agency.getInProgressMissions().values()) {
                    facts.add(new IntelFact(IntelFact.Kind.IN_FLIGHT, mission.getMissionConfigurationId(), 0, 0, 0));
                }
                if (agency.getActiveContract() != null) {
                    facts.add(new IntelFact(IntelFact.Kind.CONTRACT, agency.getActiveContract().lineId(), 0, 0,
                        agency.getActiveContract().tier()));
                }
                agency.getContractRecords().forEach((line, record) -> facts.add(new IntelFact(
                    IntelFact.Kind.CONTRACT_RECORD, line, record.flown(), record.failed() + record.lapsed(),
                    Contracts.recordPoints(record))));
                Map<String, Integer> focus = new LinkedHashMap<>();
                for (SetSpecialist engineer : agency.getSetSpecialists()) {
                    if (engineer.getRdTargetComponentId() != null) {
                        focus.merge(engineer.getRdTargetComponentId(), 1, Integer::sum);
                    }
                }
                focus.forEach((id, count) -> facts.add(new IntelFact(IntelFact.Kind.RD_FOCUS, id, 0, 0, count)));
                Set<String> flown = new HashSet<>();
                for (Map.Entry<String, Integer> count : agency.getSuccessfulFlightCounts().entrySet()) {
                    if (count.getValue() > 0) {
                        catalog.missionConfiguration(count.getKey()).ifPresent(configuration -> {
                            for (MissionConfiguration.Launch launch : configuration.launches()) {
                                flown.addAll(launch.payloadComponentIds());
                            }
                        });
                    }
                }
                for (Map.Entry<String, OwnedMissionComponent> owned : agency.getMissionComponents().entrySet()) {
                    boolean payload = catalog.missionConfigurations().stream().anyMatch(configuration -> configuration
                        .launches().stream().anyMatch(launch -> launch.payloadComponentIds().contains(owned.getKey())));
                    if (owned.getValue().isActive() && payload && !flown.contains(owned.getKey())) {
                        facts.add(new IntelFact(IntelFact.Kind.SHELF, owned.getKey(), 0, 0, 0));
                    }
                }
                facts.add(new IntelFact(IntelFact.Kind.INTEL_LEVEL, "", 0, 0, agency.getIntelligence().getLevel()));
            }
            case PERSONNEL -> {
                for (PersonnelGroup group : PersonnelGroup.values()) {
                    List<? extends Personnel> roster = roster(agency, group);
                    facts.add(new IntelFact(IntelFact.Kind.HEADCOUNT, group.name(), 0, 0, roster.size()));
                    if (!roster.isEmpty()) {
                        double average = roster.stream().mapToDouble(IntelligenceBoard::averageSkill).average()
                            .orElse(0);
                        int[] range = range(average, halfWidth, random);
                        facts.add(new IntelFact(IntelFact.Kind.AVERAGE_SKILL, group.name(), range[0], range[1], 0));
                    }
                }
            }
        }
        return facts;
    }

    /**
     * Returns a range of {@code 2 × halfWidth} that holds the value, placed at random around it, within
     * 0 to 100.
     */
    static int[] range(double value, int halfWidth, SplittableRandom random) {
        int width = 2 * halfWidth;
        int low = (int) Math.round(value) - random.nextInt(width + 1);
        low = Math.clamp(low, 0, 100 - width);
        return new int[] {low, low + width};
    }

    private static List<? extends Personnel> roster(PlayerSnapshot agency, PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> agency.getAstronauts();
            case SET -> agency.getSetSpecialists();
            case MISSION_CONTROL -> agency.getMissionControlOfficers();
        };
    }

    private static Map<String, Double> skills(Personnel person) {
        Map<String, Double> skills = new LinkedHashMap<>();
        if (person instanceof de.mkoehler.launchwindow.model.Astronaut astronaut) {
            astronaut.getSkills().forEach((skill, value) -> skills.put(skill.name(), value));
        } else if (person instanceof SetSpecialist engineer) {
            engineer.getSkills().forEach((skill, value) -> skills.put(skill.name(), value));
        } else if (person instanceof de.mkoehler.launchwindow.model.MissionControlOfficer controller) {
            controller.getSkills().forEach((skill, value) -> skills.put(skill.name(), value));
        }
        return skills;
    }

    private static double averageSkill(Personnel person) {
        return skills(person).values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static long seed(long gameSeed, long gameTurn, String playerId, String key) {
        SplittableRandom byTurn = new SplittableRandom(gameSeed + gameTurn * 0x9E3779B97F4A7C15L);
        return byTurn.nextLong() ^ Intelligence.seed(playerId, key);
    }
}
