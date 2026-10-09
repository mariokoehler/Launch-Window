package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.Missions;
import de.mkoehler.launchwindow.client.view.Staffing;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.rules.MissionPrestige;

import java.util.Locale;

/**
 * A mission configuration's card ({@code ui-spec.md} &sect;4.6.5): its art, how often it has
 * flown, launch tags, duration, the next flight's prestige, cost and staffing. Shared by a
 * program's configuration list and Headquarters' "Plan a mission" list.
 *
 * @author Mario Koehler
 */
final class ConfigCards {

    private static final float ART_ASPECT = 2.4f;
    private static final float ART_HEIGHT = 178f;

    private ConfigCards() {
    }

    /**
     * Builds a configuration's card.
     *
     * @param host          the screen it sits on
     * @param configuration the configuration
     * @param open          whether its program is open (the art dims when not)
     * @param onClick       what a click does
     * @return the card
     */
    static Table card(ChromeScreen host, MissionConfiguration configuration, boolean open, Runnable onClick) {
        return card(host, configuration, open, onClick, ART_HEIGHT);
    }

    /**
     * Builds a configuration's card with shorter or taller art.
     *
     * @param host          the screen it sits on
     * @param configuration the configuration
     * @param open          whether its program is open (the art dims when not)
     * @param onClick       what a click does
     * @param artHeight     the art's height
     * @return the card
     */
    static Table card(ChromeScreen host, MissionConfiguration configuration, boolean open, Runnable onClick,
            float artHeight) {
        UiKit ui = host.ui;
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        card.top();
        Drawable art = ui.croppedPicture("configurations/" + configuration.id() + ".jpg",
            ART_ASPECT * ART_HEIGHT / artHeight);
        Image image = new Image(art == null ? ui.solid(Theme.NAVY) : art);
        if (!open) {
            image.setColor(1f, 1f, 1f, 0.6f);
        }
        card.add(image).growX().height(artHeight).pad(Theme.BORDER_THIN).row();

        Table body = new Table();
        body.top().left();
        body.defaults().left().growX();
        Table head = new Table();
        head.add(ui.label(Missions.name(configuration), Theme.TextStyle.HEADING_SM, Theme.INK)).expandX().left();
        int flown = Missions.flown(configuration, host.session.state());
        head.add(ui.label(flown == 0 ? "Not flown" : "Flown " + flown + " ×", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).right();
        body.add(head).padBottom(10f).row();

        Table tags = new Table();
        tags.left();
        for (int i = 0; i < configuration.launches().size(); i++) {
            MissionConfiguration.Launch launch = configuration.launches().get(i);
            tags.add(tag(ui, Missions.launchTag(launch.launchType()), false)).padRight(4f);
            if (launch.launchType().humanRated() && launch.launchType().needsRocket()) {
                tags.add(tag(ui, "Human-rated", true)).padRight(4f);
            }
        }
        if (configuration.launches().size() > 1) {
            tags.add(tag(ui, configuration.launches().size() + " ×", false)).padRight(4f);
        }
        if (!Missions.missingGoals(configuration, host.session.state()).isEmpty()) {
            tags.add(tag(ui, "Locked", true));
        }
        body.add(tags).padBottom(10f).row();

        Table stats = new Table();
        stats.left();
        stats.defaults().left().expandX().fillX().uniformX().padBottom(8f);
        String seasons = configuration.durationInSeasons() + (configuration.durationInSeasons() == 1 ? " season"
            : " seasons");
        int steps = configuration.seasons().stream().mapToInt(season -> season.steps().size()).sum();
        stat(ui, stats, "Duration", seasons + " · " + steps + " steps");
        stat(ui, stats, "Prestige", String.format(Locale.ROOT, "%,d", Missions.nextPrestige(configuration,
            host.session.state())));
        stat(ui, stats, "Cost", ChromeScreen.money(configuration.cost()));
        stats.row();
        stat(ui, stats, "Crew", String.valueOf(Missions.crewCount(configuration)));
        stat(ui, stats, "Controllers", String.valueOf(Missions.controllerCount(configuration)));
        stat(ui, stats, "CAPCOM", String.valueOf(Missions.capcomCount(configuration)));
        body.add(stats).row();
        // Free people against the need, so staffing can be planned seasons ahead (play test 2026-10-07).
        String free = Staffing.cardLine(configuration, host.session.state());
        if (free != null) {
            boolean short_ = Staffing.isShort(configuration, host.session.state());
            Label line = ui.label(free, Theme.TextStyle.CAPTION, short_ ? Theme.DANGER : Theme.INK_MUTED);
            ui.tooltip(line, "People free now", "Free means idle and available: not on a mission, not in training, "
                + "not on R&D. Assembly needs this many free at once. The planning screen shows who's coming.");
            body.add(line).padBottom(4f).row();
        }
        body.add(ui.label(tierNote(flown), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(body).growX().pad(14f, 16f, 16f, 16f);
        card.setTouchable(Touchable.enabled);
        card.addListener(ChromeScreen.clicked(onClick));
        return card;
    }

    private static String tierNote(int flown) {
        return switch (flown) {
            case 0 -> "Full prestige on the first flight.";
            case 1 -> MissionPrestige.SECOND_TIME_PERCENT + " % on the second flight.";
            default -> MissionPrestige.LATER_TIME_PERCENT + " % on every flight from now on.";
        };
    }

    private static void stat(UiKit ui, Table stats, String label, String value) {
        Table cell = new Table();
        cell.left();
        cell.defaults().left();
        cell.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(2f).row();
        cell.add(ui.label(value, Theme.TextStyle.BODY_SM, Theme.INK));
        stats.add(cell);
    }

    /**
     * Builds a small tag: a thin outline around a label, navy for emphasis.
     */
    private static Table tag(UiKit ui, String text, boolean navy) {
        Table tag = new Table();
        tag.setBackground(ui.rect(Theme.SURFACE_CARD, navy ? Theme.NAVY : Theme.NAVY_BORDER, Theme.BORDER_THIN));
        tag.pad(2f, 6f, 2f, 6f);
        tag.add(ui.label(text, Theme.TextStyle.LABEL, navy ? Theme.NAVY : Theme.INK));
        return tag;
    }
}
