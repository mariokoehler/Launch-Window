package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.TraitChip;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.Outlook;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.client.view.Tips;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

import java.util.List;

/**
 * The pieces of a crew roster row (component 11), shared by Mission Control's controllers and the
 * Astronaut Complex's astronauts, and the SET Centre's engineers: the avatar, the name that opens the
 * person page, a skill cell with its Expert mark, the plain state pill, the traits, and the salary
 * and morale chips.
 *
 * @author Mario Koehler
 */
final class RosterCells {

    private RosterCells() {
    }

    /** A roster row's skill icon size: 20 % up from the mockup's 14 px, so the glyphs read (play test). */
    static final float SKILL_ICON = 17f;

    /**
     * Builds a skill cell: the skill's icon and value, tinted gold with a star for an Expert skill,
     * with a tooltip naming the skill and what it's for.
     *
     * @param ui     the UI kit
     * @param icon   the skill's icon
     * @param value  the value
     * @param expert whether the person is an Expert in it
     * @param tip    the skill's tooltip
     * @return the cell
     */
    static Table skillCell(UiKit ui, String icon, double value, boolean expert, Tips.Tip tip) {
        Table cell = new Table();
        if (expert) {
            cell.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.12f), Theme.GOLD,
                Theme.BORDER_THIN));
        }
        cell.pad(2f, 4f, 2f, 4f);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        cell.add(glyph).height(SKILL_ICON).width(SKILL_ICON * Math.min(1f, ui.iconAspect(icon))).padRight(4f);
        cell.add(ui.label(String.valueOf(Math.round(value)), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).left();
        if (expert) {
            // The mockup's star glyph isn't in the font; the prestige star, in gold, stands in.
            Image star = new Image(ui.icon("prestige"), Scaling.fit);
            star.setColor(Theme.GOLD_TEXT);
            cell.add(star).size(10f).padLeft(3f);
        }
        ui.tooltip(cell, tip.title() + (expert ? " · Expert" : ""), tip.text());
        return cell;
    }

    /** A roster row's traits column ({@code ui-spec.md} component 11, People canvas, 2026-10-07). */
    static final float TRAITS_WIDTH = 230f;

    /**
     * Builds a person's name and age line; clicking the name opens their person page
     * ({@code ui-spec.md} &sect;4.14).
     *
     * @param ui      the UI kit
     * @param person  the person
     * @param ageLine the caption under the name
     * @param open    opens the person page
     * @return the cell
     */
    static Table who(UiKit ui, Personnel person, String ageLine, Runnable open) {
        Table who = new Table();
        who.left();
        who.defaults().left();
        Label name = ui.label(person.getName(), Theme.TextStyle.HEADING_SM, Theme.INK);
        name.setTouchable(Touchable.enabled);
        name.addListener(ChromeScreen.clicked(open));
        ui.tooltip(name, person.getName(), "Opens the person page: skills, traits and relationships.");
        com.badlogic.gdx.scenes.scene2d.ui.Image mark = RetentionCards.mark(ui, person);
        if (mark == null) {
            who.add(name).padBottom(2f).row();
        } else {
            Table line = new Table();
            line.add(name).padRight(6f);
            line.add(mark).size(13f);
            who.add(line).padBottom(2f).row();
        }
        who.add(ui.label(ageLine, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        return who;
    }

    /**
     * Builds a row's traits cell: the revealed trait chips, and "Still getting to know each other"
     * while the person is inside the reveal window. Never empty slots or a count.
     *
     * @param ui     the UI kit
     * @param person the person
     * @return the cell
     */
    static Table traits(UiKit ui, Personnel person) {
        Table cell = new Table();
        cell.left();
        cell.defaults().left();
        List<Trait> traits = People.traits(person);
        if (!traits.isEmpty()) {
            cell.add(TraitChip.row(ui, traits, TRAITS_WIDTH)).row();
        }
        if (People.gettingToKnow(person)) {
            cell.add(ui.label("Still getting to know each other", Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED))
                .padTop(traits.isEmpty() ? 0f : 4f).row();
        }
        // An ambition running, or disappointment after one (phase 4, design.md 2.2(d)).
        String status = de.mkoehler.launchwindow.client.view.CareerTexts.status(person);
        if (status != null) {
            String text = person.getAmbition() != null ? "Ambition · " + status : status;
            Label line = ui.label(text, Theme.TextStyle.CAPTION, person.getAmbition() != null ? Theme.GOLD_TEXT
                : Theme.DANGER);
            if (person.getAmbition() != null) {
                ui.tooltip(line, "Ambition", de.mkoehler.launchwindow.client.view.CareerTexts.ambition(
                    person.getAmbition().kind(), person.getAmbition().ref()) + ". "
                    + de.mkoehler.launchwindow.client.view.CareerTexts.ambitionHow(person.getAmbition().kind()));
            }
            cell.add(line).padTop(4f);
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
    /**
     * Adds a roster row's look-ahead note under its state badge (2026-10-10): when training ends and
     * a retirement within a year. Nothing when neither applies.
     *
     * @param ui       the widget kit
     * @param holder   the state cell
     * @param person   the employee
     * @param group    their group
     * @param openTurn the open turn
     */
    static void outlookNote(UiKit ui, Table holder, Personnel person, PersonnelGroup group, long openTurn) {
        String note = Outlook.rosterNote(person, group, openTurn);
        if (note != null) {
            holder.add(ui.label(note, Theme.TextStyle.CAPTION, note.contains("etires") ? Theme.GOLD_TEXT
                : Theme.INK_MUTED)).left().padTop(4f).row();
        }
    }

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
        Tips.Tip tip = Tips.stat(icon);
        if (tip != null) {
            ui.tooltip(chip, tip.title(), tip.text());
        }
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
     * Builds a person's avatar: their portrait ({@code assets/ui/portraits/<id>.jpg}, made by
     * {@code assets-raw/portraits/make_portrait_assets.py}), or a navy square with their initials if
     * they have none or the file is missing.
     *
     * @param ui     the UI kit
     * @param person the person
     * @return the avatar
     */
    static Table avatar(UiKit ui, Personnel person) {
        Table avatar = new Table();
        TextureRegionDrawable portrait = person.getPortraitId() == null ? null
            : ui.picture("portraits/" + person.getPortraitId() + ".jpg");
        if (portrait != null) {
            avatar.add(new Image(portrait, Scaling.fill)).grow();
            return avatar;
        }
        avatar.setBackground(ui.solid(Theme.NAVY));
        Label initials = ui.label(SetCentreScreen.initials(person.getName()), Theme.TextStyle.HEADING_SM,
            Theme.ON_FILL);
        initials.setAlignment(Align.center);
        avatar.add(initials);
        return avatar;
    }
}
