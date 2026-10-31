package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.rules.PortraitCatalog;

import java.util.Locale;
import java.util.Map;

/**
 * Which press photo a front page prints ({@code ui-spec.md} &sect;4.18). One goal's photo has a face, the
 * first human in orbit, so it comes in a version per sex and look and shows the agency's own pilot; a tie
 * shows the first agency's. Only the pilot's portrait is kept on the page, so this table can change without
 * reprinting anything. A page with no portrait to go by gets the plain photo.
 *
 * @author Mario Koehler
 */
public final class PilotPhoto {

    /** The goal whose photo shows its pilot. */
    public static final String GOAL = "human-in-orbit";

    /** Each ethnicity in {@code personnel/portraits.json} by the look of its photo, broad enough for newsprint. */
    static final Map<String, String> LOOKS = Map.ofEntries(
        Map.entry("white_american", "european"), Map.entry("irish_american", "european"),
        Map.entry("italian_american", "european"), Map.entry("german_american", "european"),
        Map.entry("polish_american", "european"), Map.entry("scandinavian_american", "european"),
        Map.entry("jewish_american", "european"), Map.entry("russian", "european"),
        Map.entry("ukrainian", "european"), Map.entry("belarusian", "european"),
        Map.entry("estonian", "european"), Map.entry("latvian", "european"), Map.entry("lithuanian", "european"),
        Map.entry("chuvash", "european"), Map.entry("soviet_jewish", "european"),
        Map.entry("black_american", "black"),
        Map.entry("japanese_american", "east-asian"), Map.entry("chinese_american", "east-asian"),
        Map.entry("korean_american", "east-asian"), Map.entry("buryat", "east-asian"),
        Map.entry("kazakh", "central-asian"), Map.entry("uzbek", "central-asian"),
        Map.entry("volga_tatar", "central-asian"),
        Map.entry("mexican_american", "latin"), Map.entry("puerto_rican_american", "latin"),
        Map.entry("filipino_american", "latin"), Map.entry("navajo", "latin"),
        Map.entry("armenian", "caucasus"), Map.entry("georgian", "caucasus"),
        Map.entry("azerbaijani", "caucasus"), Map.entry("greek_american", "caucasus"));

    private PilotPhoto() {
    }

    /**
     * Returns a page's photo.
     *
     * @param page the page
     * @return the path under {@code assets/ui/}
     */
    public static String of(FrontPage page) {
        String plain = "frontpage/" + page.goalId() + ".jpg";
        if (!GOAL.equals(page.goalId())) {
            return plain;
        }
        for (FrontPage.Achiever achiever : page.achievers()) {
            if (achiever.commanderPortraitId() != null) {
                String version = version(PortraitCatalog.byId(achiever.commanderPortraitId()));
                return version == null ? plain : version;
            }
        }
        return plain;
    }

    /** A portrait's version of the photo, or {@code null} for an unknown portrait or ethnicity. */
    static String version(PortraitCatalog.Portrait portrait) {
        String look = portrait == null ? null : LOOKS.get(portrait.ethnicity());
        return look == null ? null
            : "frontpage/" + GOAL + "-" + portrait.sex().name().toLowerCase(Locale.ROOT) + "-" + look + ".jpg";
    }
}
