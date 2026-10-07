package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.CareerTexts;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Personnel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The person page's story column ({@code ui-spec.md} &sect;4.14, People canvas; {@code design.md}
 * &sect;2.2(d), phase 4): the ambition card and the career record, shared by the living page and
 * the memorial's.
 *
 * @author Mario Koehler
 */
final class CareerCards {

    private static final float DOT = 8f;

    private CareerCards() {
    }

    /**
     * Builds the ambition card: what the person wants and how long they'll wait, or that they're
     * disappointed; {@code null} for someone with neither.
     *
     * @param ui     the kit
     * @param person the person
     * @return the card, or {@code null}
     */
    static Table ambition(UiKit ui, Personnel person) {
        Ambition ambition = person.getAmbition();
        if (ambition == null && person.getDisappointedSeasons() == 0) {
            return null;
        }
        Table card = ui.card();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(title(ui, "Ambition")).expandX().left();
        String status = CareerTexts.status(person);
        if (status != null) {
            head.add(ambition != null ? ui.activeBadge(status) : ui.badge(Theme.BadgeKind.DANGER, status));
        }
        card.add(head).padBottom(Theme.SPACE_3).row();
        if (ambition != null) {
            card.add(ui.paragraph(CareerTexts.ambition(ambition.kind(), ambition.ref()), Theme.TextStyle.HEADING_SM,
                Theme.INK)).padBottom(Theme.SPACE_2).row();
            card.add(ui.paragraph(CareerTexts.ambitionHow(ambition.kind()), Theme.TextStyle.BODY_SM, Theme.INK))
                .padBottom(Theme.SPACE_2).row();
            card.add(ui.label("Said so in " + GameDate.ofTurn(Math.max(1, ambition.saidTurn())).label() + ", with "
                + ambition.window() + " seasons of patience.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        } else {
            card.add(ui.paragraph("An ambition ran out. Disappointed for now; another may come later.",
                Theme.TextStyle.BODY_SM, Theme.INK)).row();
        }
        return card;
    }

    /**
     * Builds the career record: newest first, the R&amp;D stint still running on top as "so far".
     *
     * @param ui      the kit
     * @param career  the record, oldest first
     * @param running the stint still running, or {@code null}
     * @param catalog the content pack
     * @return the card
     */
    static Table career(UiKit ui, List<CareerEntry> career, Running running, ContentCatalog catalog) {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(title(ui, "Career record")).padBottom(Theme.SPACE_3).row();
        List<String[]> lines = new ArrayList<>();
        List<Boolean> gold = new ArrayList<>();
        if (running != null) {
            lines.add(new String[] {"Now", String.format(Locale.ROOT, "%s R&D, %d %s so far: reliability +%.1f",
                ContentNames.of(running.componentId()), running.seasons(), running.seasons() == 1 ? "season"
                    : "seasons", running.gain())});
            gold.add(false);
        }
        for (int i = career.size() - 1; i >= 0; i--) {
            CareerEntry entry = career.get(i);
            lines.add(new String[] {CareerTexts.date(entry), CareerTexts.line(entry, catalog)});
            gold.add(CareerTexts.highlight(entry));
        }
        if (lines.isEmpty()) {
            card.add(ui.paragraph("The record starts at the next turn.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED))
                .row();
        }
        for (int i = 0; i < lines.size(); i++) {
            Table row = new Table();
            row.top().left();
            row.add(ui.label(lines.get(i)[0], Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(84f).top().left()
                .padRight(Theme.SPACE_2);
            Image dot = new Image(ui.circle(gold.get(i) ? Theme.GOLD : Theme.SURFACE_CARD,
                gold.get(i) ? Theme.GOLD : Theme.NAVY_BORDER, 1.5f, (int) DOT));
            row.add(new Container<>(dot).size(DOT)).top().padTop(4f).padRight(Theme.SPACE_3);
            row.add(ui.paragraph(lines.get(i)[1], Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX();
            card.add(row).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    /**
     * An R&amp;D stint still running.
     *
     * @param componentId the component
     * @param seasons     the seasons so far
     * @param gain        the reliability added so far
     */
    record Running(String componentId, int seasons, double gain) {
    }

    /**
     * Returns a person's running stint, or {@code null}.
     *
     * @param person the person
     * @return the stint
     */
    static Running running(Personnel person) {
        return person.getStintComponentId() == null || person.getStintSeasons() == 0 ? null
            : new Running(person.getStintComponentId(), person.getStintSeasons(), person.getStintGain());
    }

    /**
     * Builds a thin stat bar with its number, as morale has on the person page.
     *
     * @param ui    the kit
     * @param value the value, 0 to 100
     * @param color the bar's colour
     * @return the bar
     */
    static Table bar(UiKit ui, int value, Color color) {
        Table bar = new Table();
        Table track = new Table();
        track.setBackground(ui.solid(Theme.SKILL_TRACK));
        track.left();
        track.add(new Image(ui.solid(color))).width(180f * Math.max(0, Math.min(100, value)) / 100f).height(6f);
        track.add().expandX();
        bar.add(track).width(180f).height(6f).padRight(Theme.SPACE_3);
        bar.add(ui.label(String.valueOf(value), Theme.TextStyle.BODY_STRONG, color));
        return bar;
    }

    private static Container<Label> title(UiKit ui, String text) {
        return new Container<>(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .left();
    }
}
