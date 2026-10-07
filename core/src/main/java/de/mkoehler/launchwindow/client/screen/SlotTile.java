package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;

import java.util.Locale;

/**
 * A crew seat or console tile ({@code ui-spec.md} &sect;4.6.8): its title, employee type, the skill
 * it reads, and its holder with the value the roll reads, or why it's empty. Shared by Assemble
 * mission and Mission Control's re-staffing.
 *
 * @author Mario Koehler
 */
final class SlotTile {

    /**
     * What the tile shows.
     */
    enum State {
        /** No step uses the slot. */
        UNUSED,
        /** Someone holds it. */
        FILLED,
        /** Someone holds it after a change today (re-staffing). */
        CHANGED,
        /** Empty, and nobody of its type is free. */
        NOBODY,
        /** Empty, someone could fill it. */
        EMPTY,
        /** Its last season has flown; its holder went idle then. */
        RELEASED
    }

    private SlotTile() {
    }

    /**
     * Builds a tile.
     *
     * @param ui        the kit
     * @param title     the slot's title ("BOOSTER")
     * @param type      the employee type ("Flight controller")
     * @param astronaut whether it takes an astronaut
     * @param icon      the skill's icon, or {@code null}
     * @param skill     the skill's name, or "Seat fit"
     * @param person    the holder's name, or {@code null}
     * @param value     the holder's value, or {@code null}
     * @param state     what to show
     * @return the tile
     */
    static Table build(UiKit ui, String title, String type, boolean astronaut, String icon, String skill,
                       String person, String value, State state) {
        return build(ui, title, type, astronaut, icon, skill, person, value, state, null);
    }

    /**
     * Builds a tile with something about its holder under the skill line: their revealed traits and
     * relationship marks ({@code ui-spec.md} &sect;4.6.8, People canvas, 2026-10-07).
     *
     * @param ui        the kit
     * @param title     the slot's title ("BOOSTER")
     * @param type      the employee type ("Flight controller")
     * @param astronaut whether it takes an astronaut
     * @param icon      the skill's icon, or {@code null}
     * @param skill     the skill's name, or "Seat fit"
     * @param person    the holder's name, or {@code null}
     * @param value     the holder's value, or {@code null}
     * @param state     what to show
     * @param extras    what to show about the holder, or {@code null}
     * @return the tile
     */
    static Table build(UiKit ui, String title, String type, boolean astronaut, String icon, String skill,
                       String person, String value, State state, Actor extras) {
        Table tile = new Table();
        tile.top().left();
        tile.pad(10f, 12f, 10f, 12f);
        boolean used = state != State.UNUSED && state != State.RELEASED;
        switch (state) {
            case UNUSED, RELEASED -> tile.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.SKILL_TRACK,
                0.5f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
            case FILLED -> tile.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
            case CHANGED -> tile.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.1f), Theme.GOLD,
                Theme.BORDER_EMPHASIS));
            case NOBODY -> tile.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.GOLD_TEXT, Theme.BORDER_EMPHASIS));
            case EMPTY -> tile.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        }
        tile.defaults().growX().left();
        Table head = new Table();
        head.add(ui.label(title.toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, used ? Theme.INK : Theme.INK_MUTED))
            .expandX().left();
        Table pill = new Table();
        pill.setBackground(ui.rect(Theme.SURFACE_CARD, astronaut ? Theme.NAVY : Theme.NAVY_BORDER, Theme.BORDER_THIN));
        pill.pad(1f, 5f, 1f, 5f);
        pill.add(ui.label(type, Theme.TextStyle.LABEL, astronaut ? Theme.NAVY : Theme.INK_MUTED));
        head.add(pill);
        tile.add(head).padBottom(6f).row();
        Table skillRow = new Table();
        skillRow.left();
        if (icon != null) {
            Image glyph = new Image(ui.icon(icon), Scaling.fit);
            glyph.setColor(Theme.INK_MUTED);
            skillRow.add(glyph).size(13f).padRight(6f);
        }
        skillRow.add(ui.label(skill, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        tile.add(skillRow).padBottom(extras == null ? Theme.SPACE_3 : Theme.SPACE_2).row();
        if (extras != null) {
            tile.add(extras).padBottom(Theme.SPACE_2).row();
        }
        switch (state) {
            case UNUSED -> tile.add(ui.label("Not required", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            case RELEASED -> tile.add(ui.label("Released", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            case FILLED, CHANGED -> {
                Table holder = new Table();
                holder.add(ui.label(person, Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left();
                holder.add(ui.label(value, Theme.TextStyle.HEADING_SM, Theme.NAVY));
                tile.add(holder);
            }
            case NOBODY -> tile.add(ui.label("Nobody free for this slot", Theme.TextStyle.CAPTION, Theme.GOLD_TEXT));
            case EMPTY -> tile.add(ui.label("+ Assign", Theme.TextStyle.LABEL, Theme.NAVY));
        }
        return tile;
    }
}
