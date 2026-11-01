package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.view.TraitTexts;
import de.mkoehler.launchwindow.model.Trait;

import java.util.ArrayList;
import java.util.List;

/**
 * A trait chip ({@code ui-spec.md} component 20, People canvas, 2026-10-07): one of a person's
 * revealed traits, such as "Workaholic". A quiet tag, 22 px high, filled with the skill-track tint,
 * square, no border; a team trait carries a small three-dot mark before its label. Not a button: no
 * hover look and no click, only a tooltip saying what the trait does. One fill for every trait,
 * since most cut both ways.
 *
 * @author Mario Koehler
 */
public final class TraitChip extends Table {

    /** The chip's height. */
    public static final float HEIGHT = 22f;
    /** The gap between chips, both ways. */
    public static final float GAP = 6f;

    private static final float TEAM_MARK = 10f;

    /**
     * Builds a chip.
     *
     * @param ui    the kit
     * @param trait the trait
     */
    public TraitChip(UiKit ui, Trait trait) {
        setBackground(ui.solid(Theme.SKILL_TRACK));
        pad(0f, 8f, 0f, 8f);
        if (trait.teamTrait()) {
            Image mark = new Image(ui.icon("trait_team"), Scaling.fit);
            mark.setColor(Theme.INK_MUTED);
            add(mark).size(TEAM_MARK).padRight(5f);
        }
        add(ui.label(TraitTexts.label(trait), Theme.TextStyle.BODY_SM, Theme.INK));
        ui.tooltip(this, TraitTexts.label(trait), (trait.teamTrait() ? "Team trait, acts on the whole team. "
            : "Personal trait. ") + TraitTexts.effect(trait));
    }

    @Override
    public float getPrefHeight() {
        return HEIGHT;
    }

    /**
     * Lays chips out in a row that wraps, or returns an empty row for none.
     *
     * @param ui     the kit
     * @param traits the traits
     * @param width  the row's width, so the chips know where to wrap
     * @return the row
     */
    public static Table row(UiKit ui, List<Trait> traits, float width) {
        List<TraitChip> chips = new ArrayList<>();
        for (Trait trait : traits) {
            chips.add(new TraitChip(ui, trait));
        }
        return flow(chips, width);
    }

    /**
     * Lays chips, and whatever sits among them, out in a row that wraps.
     *
     * @param items the chips and marks
     * @param width the row's width, so they know where to wrap
     * @return the row
     */
    public static Table flow(List<? extends Table> items, float width) {
        Table rows = new Table();
        rows.left().top();
        Table line = new Table();
        float used = 0f;
        for (Table item : items) {
            float itemWidth = item.getPrefWidth();
            if (used > 0f && used + GAP + itemWidth > width) {
                rows.add(line).left().padBottom(GAP).row();
                line = new Table();
                used = 0f;
            }
            line.add(item).height(HEIGHT).padRight(GAP);
            used += itemWidth + GAP;
        }
        rows.add(line).left();
        return rows;
    }
}
