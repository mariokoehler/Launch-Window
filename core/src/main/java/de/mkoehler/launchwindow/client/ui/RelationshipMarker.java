package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.view.People;

/**
 * The relationship marker ({@code ui-spec.md} component 22, People canvas, 2026-10-07): two linked
 * rings in green for "works well with", a spark in red for "clashes with". Unlike a trait, a
 * relationship is plainly good or bad, so it is colour coded. Two forms: the kind label of a person
 * page's list item, and the small mark a picker puts next to a candidate, with the partner's surname
 * and a tooltip naming them in full.
 *
 * @author Mario Koehler
 */
public final class RelationshipMarker {

    private static final float GLYPH = 14f;

    private RelationshipMarker() {
    }

    /**
     * Returns a relationship's colour.
     *
     * @param positive whether they work well together
     * @return green or red
     */
    public static Color color(boolean positive) {
        return positive ? Theme.SUCCESS : Theme.DANGER;
    }

    /**
     * Returns a relationship's icon name.
     *
     * @param positive whether they work well together
     * @return {@code rel_good} or {@code rel_clash}
     */
    public static String icon(boolean positive) {
        return positive ? "rel_good" : "rel_clash";
    }

    /**
     * Builds a relationship's glyph alone, in its colour.
     *
     * @param ui       the kit
     * @param positive whether they work well together
     * @return the glyph
     */
    public static Image glyph(UiKit ui, boolean positive) {
        Image glyph = new Image(ui.icon(icon(positive)), Scaling.fit);
        glyph.setColor(color(positive));
        return glyph;
    }

    /**
     * Builds the kind label of a list item: the glyph and "WORKS WELL WITH" or "CLASHES WITH".
     *
     * @param ui       the kit
     * @param positive whether they work well together
     * @return the label
     */
    public static Table kind(UiKit ui, boolean positive) {
        Table kind = new Table();
        kind.add(glyph(ui, positive)).size(GLYPH).padRight(6f);
        kind.add(ui.label(positive ? "WORKS WELL WITH" : "CLASHES WITH", Theme.TextStyle.LABEL, color(positive)));
        return kind;
    }

    /**
     * Builds a picker's mark: the glyph and the partner's surname, with a tooltip naming them.
     *
     * @param ui          the kit
     * @param positive    whether they work well together
     * @param partnerName the partner's full name
     * @param where       where the partner is, such as "Already on this team."
     * @return the mark
     */
    public static Table mark(UiKit ui, boolean positive, String partnerName, String where) {
        Table mark = new Table();
        mark.add(glyph(ui, positive)).size(GLYPH).padRight(4f);
        mark.add(ui.label(People.surname(partnerName), Theme.TextStyle.BODY_SM, Theme.INK));
        ui.tooltip(mark, (positive ? "Works well with " : "Clashes with ") + partnerName, where);
        return mark;
    }
}
