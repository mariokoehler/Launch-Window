package de.mkoehler.launchwindow.bot;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Archetype;

import java.util.List;
import java.util.Map;

/**
 * <p>The bots' plans: each agency's flights in order, the first with a goal still open flown next.
 * Shared by the playout harness ({@code dev-tools}' {@code ContentPackPlayouts}) and the filler bots
 * that play in a real game ({@link FillerBot}).</p>
 *
 * @author Mario Koehler
 */
public final class BotPlans {

    /** The early race, the early-era pack's. */
    public static final Map<Archetype, List<String>> EARLY_ERA = Map.of(
        Archetype.SOVIET, List.of("sputnik-1-orbit", "vostok-uncrewed-suborbital", "vostok-suborbital",
            "vostok-uncrewed-orbital", "vostok-orbital"),
        Archetype.NASA, List.of("explorer-1-orbit", "mercury-uncrewed-suborbital", "mercury-suborbital",
            "mercury-uncrewed-orbital", "mercury-orbital"));

    /**
     * The plans to the Moon: the early race, one lunar probe (it gates the lunar programs), the lunar
     * craft's tests in Earth orbit, then out to the Moon step by step. Each flight clears reliability
     * penalties of the ones after it, as a careful player would fly them.
     */
    public static final Map<Archetype, List<String>> MOON = Map.of(
        Archetype.SOVIET, List.of("sputnik-1-orbit", "vostok-uncrewed-suborbital", "vostok-suborbital",
            "vostok-uncrewed-orbital", "vostok-orbital", "luna-2-impact",
            "soyuz-7k-l3-uncrewed-suborbital", "soyuz-7k-l3-uncrewed-orbital", "soyuz-7k-l3-orbital-duration-1",
            "soyuz-7k-l3-spacewalk", "soyuz-7k-l3-uncrewed-lander-test", "soyuz-7k-l3-lander-test",
            "soyuz-7k-l3-uncrewed-flyby", "soyuz-7k-l3-flyby", "soyuz-7k-l3-uncrewed-lunar-orbit",
            "soyuz-7k-l3-lunar-orbit", "soyuz-7k-l3-lander-test-lunar-orbit", "soyuz-7k-l3-landing"),
        Archetype.NASA, List.of("explorer-1-orbit", "mercury-uncrewed-suborbital", "mercury-suborbital",
            "mercury-uncrewed-orbital", "mercury-orbital", "ranger-3-impact",
            "apollo-uncrewed-suborbital", "apollo-uncrewed-orbital", "apollo-orbital-duration-1",
            "apollo-uncrewed-lunar-module-test", "apollo-lunar-module-test-low-orbit",
            "apollo-uncrewed-flyby", "apollo-flyby", "apollo-uncrewed-lunar-orbit", "apollo-lunar-orbit",
            "apollo-lander-test", "apollo-landing"));

    private BotPlans() {
    }

    /**
     * Returns the plan an agency flies in a pack: the Moon's where the pack has all its flights, else the
     * early race's.
     *
     * @param catalog   the pack
     * @param archetype the agency's archetype
     * @return the plan, or an empty list if the pack has neither
     */
    public static List<String> forPack(ContentCatalog catalog, Archetype archetype) {
        for (Map<Archetype, List<String>> plans : List.of(MOON, EARLY_ERA)) {
            List<String> plan = plans.get(archetype);
            if (plan.stream().allMatch(id -> catalog.missionConfiguration(id).isPresent())) {
                return plan;
            }
        }
        return List.of();
    }
}
