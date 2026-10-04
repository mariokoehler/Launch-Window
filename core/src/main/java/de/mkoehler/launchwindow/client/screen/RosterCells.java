package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;

/**
 * The pieces of a crew roster row (component 11), shared by Mission Control's controllers and the
 * Astronaut Complex's astronauts: the avatar, a skill cell with its Expert mark, the plain state
 * pill, and the salary and morale chips.
 *
 * @author Mario Koehler
 */
final class RosterCells {

    private RosterCells() {
    }

    /**
     * Builds a skill cell: the skill's icon and value, tinted gold with a star for an Expert skill.
     *
     * @param ui     the UI kit
     * @param icon   the skill's icon
     * @param value  the value
     * @param expert whether the person is an Expert in it
     * @return the cell
     */
    static Table skillCell(UiKit ui, String icon, double value, boolean expert) {
        Table cell = new Table();
        if (expert) {
            cell.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.12f), Theme.GOLD,
                Theme.BORDER_THIN));
        }
        cell.pad(2f, 4f, 2f, 4f);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        cell.add(glyph).size(14f).padRight(4f);
        cell.add(ui.label(String.valueOf(Math.round(value)), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).left();
        if (expert) {
            // The mockup's star glyph isn't in the font; the prestige star, in gold, stands in.
            Image star = new Image(ui.icon("prestige"), Scaling.fit);
            star.setColor(Theme.GOLD_TEXT);
            cell.add(star).size(10f).padLeft(3f);
        }
        return cell;
    }

    /**
     * Builds the outlined pill for a state that isn't busy (Idle, Unavailable).
     *
     * @param ui   the UI kit
     * @param text the state
     * @return the pill
     */
    static Container<Label> pill(UiKit ui, String text) {
        Container<Label> pill = new Container<>(ui.label(text, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        pill.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        pill.pad(Theme.SPACE_1, Theme.SPACE_2, Theme.SPACE_1, Theme.SPACE_2);
        return pill;
    }

    /**
     * Builds a small outlined chip with an icon, for salary and morale.
     *
     * @param ui    the UI kit
     * @param icon  the icon
     * @param text  the text
     * @param color the icon's and text's color
     * @return the chip
     */
    static Table statChip(UiKit ui, String icon, String text, Color color) {
        Table chip = new Table();
        chip.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        chip.pad(3f, Theme.SPACE_2, 3f, Theme.SPACE_2);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(color);
        chip.add(glyph).size(12f).padRight(Theme.SPACE_1);
        chip.add(ui.label(text, Theme.TextStyle.CAPTION, color)).expandX().left();
        return chip;
    }

    /**
     * Returns morale's color: green when high, gold in the middle, red when low.
     *
     * @param morale the morale
     * @return the color
     */
    static Color moraleColor(int morale) {
        if (morale >= 65) {
            return Theme.SUCCESS;
        }
        return morale >= 45 ? Theme.GOLD_TEXT : Theme.DANGER;
    }

    /**
     * Builds the navy avatar with the person's initials.
     *
     * @param ui   the UI kit
     * @param name the person's name
     * @return the avatar
     */
    static Table avatar(UiKit ui, String name) {
        Table avatar = new Table();
        avatar.setBackground(ui.solid(Theme.NAVY));
        Label initials = ui.label(SetCentreScreen.initials(name), Theme.TextStyle.HEADING_SM, Theme.ON_FILL);
        initials.setAlignment(Align.center);
        avatar.add(initials);
        return avatar;
    }
}
