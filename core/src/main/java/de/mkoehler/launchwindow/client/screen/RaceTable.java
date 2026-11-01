package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Museum;
import de.mkoehler.launchwindow.content.FrontPages;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The race table ({@code ui-spec.md} &sect;4.11 and &sect;4.13): one row per agency, one column
 * per declared goal with the date it was done and a First tag. Shared by the Museum's race tab
 * and the game result dialog.
 *
 * @author Mario Koehler
 */
final class RaceTable {

    /** The smallest size a goal's mission patch stays legible at; the Museum never draws one smaller. */
    static final float PATCH_SIZE = 64f;

    /** A row's height: room for a patch in the Museum, the old compact height in the result dialog. */
    private static final float MUSEUM_CELL_HEIGHT = PATCH_SIZE + 16f;

    private static final float CELL_HEIGHT = 60f;

    /** An open goal's greyed patch is also faded, so it reads as not yet earned. */
    private static final float OPEN_ALPHA = 0.6f;

    private RaceTable() {
    }

    /**
     * Builds the table.
     *
     * @param ui        the UI kit
     * @param rows      the rows, in order
     * @param goalIds   the declared goals' ids, in order
     * @param museum    {@code true} for the Museum's version (place, patches, a Done column, the
     *                  player's row tinted); {@code false} for the result dialog's (winners tinted)
     * @return the table
     */
    static Table build(UiKit ui, List<Museum.RaceRow> rows, List<String> goalIds, boolean museum) {
        return build(ui, rows, goalIds, museum, null);
    }

    /**
     * Builds the table, with each First tag of a First that has a front page opening it
     * ({@code ui-spec.md} &sect;4.18).
     *
     * @param ui        the UI kit
     * @param rows      the rows, in order
     * @param goalIds   the declared goals' ids, in order
     * @param museum    {@code true} for the Museum's version
     * @param openPage  opens a First's front page by its goal, or {@code null} for plain tags
     * @return the table
     */
    static Table build(UiKit ui, List<Museum.RaceRow> rows, List<String> goalIds, boolean museum,
                       Consumer<String> openPage) {
        // Rows sit a hairline apart on a light navy ground: the mockup's row dividers.
        Table table = new Table();
        table.setBackground(ui.solid(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.18f)));
        table.defaults().fill().padBottom(Theme.BORDER_THIN);
        if (museum) {
            table.add(head(ui, "Place")).width(64f);
        }
        // Not uniform with the goals: a long agency name would widen every column (large text, 2026-10-26).
        table.add(head(ui, "Agency")).expandX();
        for (String goalId : goalIds) {
            table.add(head(ui, ContentNames.of(goalId))).expandX().uniformX();
        }
        if (museum) {
            table.add(head(ui, "Done")).width(90f);
        }
        table.add(head(ui, "Finished")).width(150f);
        table.row();
        Color mine = new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.12f);
        float height = museum ? MUSEUM_CELL_HEIGHT : CELL_HEIGHT;
        for (Museum.RaceRow row : rows) {
            boolean tinted = museum ? row.you() : row.winner();
            Color fill = tinted ? mine : Theme.SURFACE_CARD;
            if (museum) {
                table.add(cell(ui, fill).add(ui.label(String.valueOf(row.place()), Theme.TextStyle.HEADING_SM,
                    Theme.NAVY)).getTable()).height(height);
            }
            Table agency = cell(ui, fill);
            Table who = new Table();
            who.defaults().left();
            who.add(ui.label(row.agency(), Theme.TextStyle.BODY_STRONG, Theme.INK)).padBottom(2f).row();
            Table line = new Table();
            line.add(ui.label("Dir. " + row.director(), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padRight(6f);
            Table archetype = new Table();
            archetype.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            archetype.pad(1f, 6f, 1f, 6f);
            archetype.add(ui.label(row.archetype(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            line.add(archetype);
            who.add(line);
            agency.add(who).expandX().left();
            table.add(agency).height(height);
            for (int i = 0; i < row.cells().size(); i++) {
                Museum.Cell goal = row.cells().get(i);
                Table cell = cell(ui, fill);
                if (museum) {
                    cell.add(patch(ui, goalIds.get(i), goal.done(), PATCH_SIZE)).size(PATCH_SIZE).padRight(Theme.SPACE_2);
                }
                Table text = new Table();
                text.defaults().left();
                text.add(ui.label(goal.when(), Theme.TextStyle.BODY_SM, goal.done() ? Theme.INK : Theme.INK_MUTED))
                    .row();
                if (goal.first()) {
                    text.add(pageLink(ui, firstTag(ui, "First"), goalIds.get(i), openPage)).padTop(2f);
                }
                cell.add(text).expandX().left();
                table.add(cell).height(height);
            }
            if (museum) {
                table.add(cell(ui, fill).add(ui.label(row.count() + " of " + row.cells().size(),
                    Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left().getTable()).height(height);
            }
            table.add(cell(ui, fill).add(ui.label(row.finished(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX()
                .left().getTable()).height(height);
            table.row();
        }
        return table;
    }

    /**
     * Builds a goal badge's patch: the goal's embroidered mission patch, in colour once done and
     * greyed out while open ({@code assets/ui/patches/}, made by {@code make_patch_assets.py}).
     * A goal without patch art gets the drawn placeholder: a check once done, a star while open.
     *
     * @param ui     the UI kit
     * @param goalId the goal
     * @param done   whether the goal is done
     * @param size   the patch's diameter
     * @return the patch
     */
    static Table patch(UiKit ui, String goalId, boolean done, float size) {
        Table patch = new Table();
        Drawable art = ui.picture("patches/" + goalId + (done ? "" : "_open") + ".png");
        if (art != null) {
            Image image = new Image(art, Scaling.fit);
            if (!done) {
                image.setColor(1f, 1f, 1f, OPEN_ALPHA);
            }
            patch.add(image).size(size);
            return patch;
        }
        patch.setBackground(ui.goalPatch(done));
        Image glyph = new Image(ui.icon(done ? "check" : "prestige"), Scaling.fit);
        glyph.setColor(done ? Theme.SUCCESS : Theme.NAVY);
        patch.add(glyph).size(size * 0.45f);
        return patch;
    }

    /**
     * Makes a First tag open the First's front page, when the First has one ({@code ui-spec.md}
     * &sect;4.18).
     *
     * @param ui       the UI kit
     * @param tag      the tag
     * @param goalId   the goal it marks
     * @param openPage opens a front page by its goal, or {@code null} to leave the tag plain
     * @return the tag
     */
    static Table pageLink(UiKit ui, Table tag, String goalId, Consumer<String> openPage) {
        if (openPage == null || FrontPages.standard().first(goalId).isEmpty()) {
            return tag;
        }
        tag.setTouchable(Touchable.enabled);
        tag.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openPage.accept(goalId);
            }
        });
        ui.tooltip(tag, "Front page", "Read the newspaper's front page for this First.");
        return tag;
    }

    /**
     * Builds the gold First tag.
     *
     * @param ui   the UI kit
     * @param text its text
     * @return the tag
     */
    static Table firstTag(UiKit ui, String text) {
        Table tag = new Table();
        tag.setBackground(ui.solid(Theme.GOLD));
        tag.pad(2f, 6f, 2f, 6f);
        tag.add(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.NAVY));
        return tag;
    }

    private static Table head(UiKit ui, String text) {
        Table head = new Table();
        head.setBackground(ui.solid(Theme.NAVY));
        head.pad(10f, 12f, 10f, 12f);
        head.add(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.ON_FILL)).expandX().left();
        return head;
    }

    private static Table cell(UiKit ui, Color fill) {
        Table cell = new Table();
        cell.setBackground(ui.solid(fill));
        cell.pad(0f, 12f, 0f, 12f);
        cell.left();
        return cell;
    }
}
