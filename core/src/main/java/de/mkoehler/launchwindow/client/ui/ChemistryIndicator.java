package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.rules.Chemistry;

import java.util.List;

/**
 * The chemistry indicator ({@code ui-spec.md} component 21, People canvas, 2026-10-07): how well a
 * team works together, as a five-step scale with the current step filled, the level's word and the
 * effect on output ("Good +4 % output"). Here colour does code good and bad, backed by the word and
 * the step's position. Three forms: the full one on a card, with an optional breakdown that hover
 * previews and a click pins; a compact one for rows and pickers; and a preview, "Good → Strained
 * −5 %", for what a candidate would change. Whenever part of the team may still be hidden, it says
 * "may differ: unknown factors", always in the same words.
 *
 * @author Mario Koehler
 */
public final class ChemistryIndicator extends Table {

    /** The words shown while part of a team may still be hidden. */
    public static final String UNKNOWN = "may differ: unknown factors";

    private static final float BREAKDOWN_WIDTH = 360f;

    private Popover popover;

    private ChemistryIndicator() {
    }

    /**
     * Returns a level's colour for its filled step.
     *
     * @param level the level
     * @return the colour
     */
    public static Color stepColor(People.Level level) {
        return switch (level) {
            case POOR -> Theme.DANGER;
            case STRAINED -> Theme.GOLD;
            case NEUTRAL -> Theme.NAVY;
            case GOOD, EXCELLENT -> Theme.SUCCESS;
        };
    }

    /**
     * Returns a level's colour for its word.
     *
     * @param level the level
     * @return the colour
     */
    public static Color textColor(People.Level level) {
        return switch (level) {
            case POOR -> Theme.DANGER;
            case STRAINED -> Theme.GOLD_TEXT;
            case NEUTRAL -> Theme.INK;
            case GOOD, EXCELLENT -> Theme.SUCCESS;
        };
    }

    /**
     * Builds the full indicator: scale, level, "+4 % output", and "may differ" under it when part
     * of the team may be hidden.
     *
     * @param ui     the kit
     * @param result the known-only chemistry
     * @return the indicator
     */
    public static ChemistryIndicator full(UiKit ui, Chemistry.Result result) {
        ChemistryIndicator indicator = new ChemistryIndicator();
        indicator.left();
        indicator.defaults().left();
        People.Level level = People.Level.of(result.value());
        Table line = new Table();
        line.add(scale(ui, level, 12f, 8f)).padRight(8f);
        line.add(ui.label(level.label(), Theme.TextStyle.BODY_STRONG, textColor(level))).padRight(8f);
        line.add(ui.label(People.percentText(result.value()) + " output", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
        indicator.add(line).row();
        if (result.unknownFactors()) {
            indicator.add(ui.label(UNKNOWN, Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED)).padTop(2f);
        }
        return indicator;
    }

    /**
     * Builds the full indicator with its breakdown: hovering the indicator previews the reasons,
     * a click pins them open.
     *
     * @param ui     the kit
     * @param result the known-only chemistry
     * @param lines  its reasons ({@link People#reasons})
     * @return the indicator
     */
    public static ChemistryIndicator withBreakdown(UiKit ui, Chemistry.Result result, List<People.ReasonLine> lines) {
        ChemistryIndicator indicator = new ChemistryIndicator();
        indicator.left();
        indicator.defaults().left();
        People.Level level = People.Level.of(result.value());
        Table trigger = new Table();
        trigger.add(scale(ui, level, 12f, 8f)).padRight(8f);
        trigger.add(ui.label(level.label(), Theme.TextStyle.BODY_STRONG, textColor(level))).padRight(8f);
        trigger.add(ui.label(People.percentText(result.value()) + " output", Theme.TextStyle.BODY_SM,
            Theme.INK_MUTED)).padRight(6f);
        Image chevron = new Image(ui.icon("chevron"), Scaling.fit);
        chevron.setColor(Theme.INK_MUTED);
        chevron.setOrigin(5f, 5f);
        chevron.setRotation(-90f);
        trigger.add(chevron).size(10f);
        Table underline = new Table();
        underline.add(trigger).left().row();
        underline.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).growX().padTop(1f);
        indicator.add(underline).row();
        if (result.unknownFactors()) {
            indicator.add(ui.label(UNKNOWN, Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED)).padTop(2f);
        }
        Table card = breakdown(ui, result, lines, true);
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(12f, 14f, 12f, 14f);
        indicator.popover = Popover.attach(underline, card);
        return indicator;
    }

    /**
     * Builds the compact indicator for rows and pickers: small scale, level, "+7 %".
     *
     * @param ui     the kit
     * @param result the known-only chemistry
     * @return the indicator
     */
    public static ChemistryIndicator compact(UiKit ui, Chemistry.Result result) {
        ChemistryIndicator indicator = new ChemistryIndicator();
        indicator.left();
        People.Level level = People.Level.of(result.value());
        indicator.add(scale(ui, level, 6f, 6f)).padRight(8f);
        indicator.add(ui.label(level.label(), Theme.TextStyle.BODY_STRONG, textColor(level))).padRight(8f);
        indicator.add(ui.label(People.percentText(result.value()), Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
        return indicator;
    }

    /**
     * Builds a preview of what a change would do: "Good → [scale] Strained −5 %", with "may differ"
     * under it when part of the new team may be hidden.
     *
     * @param ui   the kit
     * @param now  the chemistry now
     * @param then the chemistry after the change
     * @return the preview
     */
    public static ChemistryIndicator preview(UiKit ui, Chemistry.Result now, Chemistry.Result then) {
        ChemistryIndicator indicator = new ChemistryIndicator();
        indicator.left();
        indicator.defaults().left();
        People.Level from = People.Level.of(now.value());
        Table line = new Table();
        line.add(ui.label(from.label(), Theme.TextStyle.BODY_SM, textColor(from))).padRight(6f);
        line.add(ui.label("→", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padRight(6f);
        line.add(compact(ui, then));
        indicator.add(line).row();
        if (then.unknownFactors()) {
            indicator.add(ui.label("may differ", Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED)).padTop(2f);
        }
        return indicator;
    }

    /**
     * Builds a chemistry's breakdown: its level and percentage, then one line per known reason
     * with its direction only. Pairs below the threshold and unrevealed traits never get a line.
     *
     * @param ui     the kit
     * @param result the known-only chemistry
     * @param lines  its reasons
     * @param header whether to head it "TEAM CHEMISTRY" with the level
     * @return the breakdown, unframed
     */
    public static Table breakdown(UiKit ui, Chemistry.Result result, List<People.ReasonLine> lines, boolean header) {
        Table card = new Table();
        card.top().left();
        card.defaults().left().growX();
        float inner = BREAKDOWN_WIDTH - 28f;
        if (header) {
            People.Level level = People.Level.of(result.value());
            Table head = new Table();
            head.add(ui.label("TEAM CHEMISTRY", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
            head.add(ui.label(level.label() + " · " + People.percentText(result.value()), Theme.TextStyle.BODY_STRONG,
                textColor(level)));
            card.add(head).width(inner).padBottom(Theme.SPACE_2).row();
        } else {
            // Inline, the breakdown takes its parent's width.
            inner = 0f;
        }
        if (lines.isEmpty()) {
            sized(card.add(ui.paragraph("Nothing has come out yet that moves it either way.", Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED)), inner).padBottom(4f).row();
        }
        for (People.ReasonLine reason : lines) {
            Table row = new Table();
            Image glyph = new Image(ui.icon(reason.icon()), Scaling.fit);
            glyph.setColor("trait_team".equals(reason.icon()) ? Theme.INK_MUTED
                : RelationshipMarker.color(reason.sign() > 0));
            row.add(glyph).size("trait_team".equals(reason.icon()) ? 10f : 14f).width(18f).padRight(8f);
            row.add(ui.paragraph(reason.text(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX().left();
            row.add(ui.label(reason.sign() > 0 ? "+" : "−", Theme.TextStyle.HEADING_SM,
                reason.sign() > 0 ? Theme.SUCCESS : Theme.DANGER)).padLeft(8f);
            sized(card.add(row), inner).padBottom(6f).row();
        }
        if (result.unknownFactors() && header) {
            card.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).width(inner).padTop(2f)
                .padBottom(6f).row();
            card.add(ui.paragraph("May differ: unknown factors. Some of this team are still new to each other, so the "
                + "real chemistry can be better or worse.", Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED))
                .width(inner);
        }
        return card;
    }

    /** Gives a cell a fixed width, or lets it grow with its table when the width is 0. */
    private static com.badlogic.gdx.scenes.scene2d.ui.Cell<?> sized(com.badlogic.gdx.scenes.scene2d.ui.Cell<?> cell,
            float width) {
        return width > 0f ? cell.width(width) : cell.growX();
    }

    /**
     * Builds the five-step scale with the level's step filled.
     */
    private static Table scale(UiKit ui, People.Level level, float width, float height) {
        Table scale = new Table();
        for (People.Level step : People.Level.values()) {
            Color color = step == level ? stepColor(level) : Theme.SKILL_TRACK;
            scale.add(new Image(ui.solid(color))).size(width, height).padRight(step.ordinal() < 4 ? 2f : 0f);
        }
        return scale;
    }

    /**
     * Returns the breakdown's popover, or {@code null} for an indicator without one.
     *
     * @return the popover
     */
    public Popover popover() {
        return popover;
    }

    /**
     * Closes the breakdown when the indicator leaves the stage, such as in a rebuild.
     *
     * @param stage the new stage, or null when detached
     */
    @Override
    protected void setStage(Stage stage) {
        if (stage == null && popover != null) {
            popover.close();
        }
        super.setStage(stage);
    }
}
