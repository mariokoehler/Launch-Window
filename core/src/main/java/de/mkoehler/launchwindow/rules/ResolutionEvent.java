package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * <p>Marker interface for anything a rules-engine resolver reports having
 * done to a {@link de.mkoehler.launchwindow.model.PlayerSnapshot} during a
 * season-advance pass. Every resolver in {@code de.mkoehler.launchwindow.rules}
 * returns a {@code List<ResolutionEvent>} alongside mutating the snapshot in
 * place: the shared vocabulary the turn report, the mission replay, and later
 * the Museum read ({@code design.md} &sect;2.2(c)/&sect;3.13).</p>
 *
 * <p>Persisted in each player's turn reports ({@link TurnReport}, gap G2), so the
 * type ids below are part of the save format: never rename or reuse one, and only
 * add fields (with values old reports can do without), never remove them.</p>
 *
 * @author Mario Koehler
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BudgetReviewed.class, name = "budgetReviewed"),
    @JsonSubTypes.Type(value = BuildingUpgraded.class, name = "buildingUpgraded"),
    @JsonSubTypes.Type(value = ExpertRescueApplied.class, name = "expertRescueApplied"),
    @JsonSubTypes.Type(value = ExpertTrainingCompleted.class, name = "expertTrainingCompleted"),
    @JsonSubTypes.Type(value = GoalSettled.class, name = "goalSettled"),
    @JsonSubTypes.Type(value = MissionPrestigeChanged.class, name = "missionPrestigeChanged"),
    @JsonSubTypes.Type(value = MissionReliabilityChanged.class, name = "missionReliabilityChanged"),
    @JsonSubTypes.Type(value = MissionSkillGrowthApplied.class, name = "missionSkillGrowthApplied"),
    @JsonSubTypes.Type(value = MissionStepResolved.class, name = "missionStepResolved"),
    @JsonSubTypes.Type(value = PassengerLost.class, name = "passengerLost"),
    @JsonSubTypes.Type(value = PersonnelDeparture.class, name = "personnelDeparture"),
    @JsonSubTypes.Type(value = PrDutyPerformed.class, name = "prDutyPerformed"),
    @JsonSubTypes.Type(value = RandomEventOccurred.class, name = "randomEventOccurred"),
    @JsonSubTypes.Type(value = ResearchCompleted.class, name = "researchCompleted"),
    @JsonSubTypes.Type(value = ResearchComponentMaxed.class, name = "researchComponentMaxed"),
    @JsonSubTypes.Type(value = SeasonFundsSettled.class, name = "seasonFundsSettled"),
    @JsonSubTypes.Type(value = TrainingCompleted.class, name = "trainingCompleted"),
    @JsonSubTypes.Type(value = TraitRevealed.class, name = "traitRevealed"),
    @JsonSubTypes.Type(value = RelationshipShown.class, name = "relationshipShown"),
    @JsonSubTypes.Type(value = IntelligenceFound.class, name = "intelligenceFound"),
    @JsonSubTypes.Type(value = IntelligenceLevelChanged.class, name = "intelligenceLevelChanged"),
    @JsonSubTypes.Type(value = AmbitionSettled.class, name = "ambitionSettled"),
    @JsonSubTypes.Type(value = OfferSettled.class, name = "offerSettled")
})
public interface ResolutionEvent {
}
