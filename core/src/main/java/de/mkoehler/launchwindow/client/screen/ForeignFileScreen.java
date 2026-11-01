package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.DashedBox;
import de.mkoehler.launchwindow.client.ui.FullWidthPicture;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.ForeignHardware;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Museum;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Dossier;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelFact;
import de.mkoehler.launchwindow.model.RivalFolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The file on a piece of the other side's hardware ({@code ui-spec.md} &sect;4.15): read-only, with no
 * prices and no open button, under a hatched strip that says whose it is.
 *
 * @author Mario Koehler
 */
public final class ForeignFileScreen extends ChromeScreen {

    private static final float ART_WIDTH = 300f;
    private static final float SIDE_WIDTH = 440f;

    private final String itemId;

    /**
     * Builds the screen from the session.
     *
     * @param game   the game, in a session
     * @param itemId the program or rocket
     */
    public ForeignFileScreen(LaunchWindowGame game, String itemId) {
        super(game, List.of(Crumb.SPACE_COMPLEX,
            new Crumb("Intelligence Office", screen -> screen.game.setScreen(new IntelligenceOfficeScreen(screen.game,
                IntelligenceOfficeScreen.Tab.OVERVIEW))),
            new Crumb("Foreign hardware", screen -> screen.game.setScreen(new IntelligenceOfficeScreen(screen.game,
                IntelligenceOfficeScreen.Tab.FOREIGN_HARDWARE))),
            new Crumb(ContentNames.of(itemId), null)), "Intelligence Office · Foreign hardware",
            ContentNames.of(itemId));
        this.itemId = itemId;
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(new IntelligenceOfficeScreen(game, IntelligenceOfficeScreen.Tab.FOREIGN_HARDWARE));
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.INTELLIGENCE;
    }

    private boolean rocket() {
        return session.catalog().program(itemId).isEmpty();
    }

    private List<String> parts() {
        return session.catalog().program(itemId).map(Program::requiredMissionComponentIds).orElse(List.of(itemId));
    }

    @Override
    protected void rebuild() {
        titleActions.clearChildren();
        titleActions.add(onClick(ui.pageButton("All foreign hardware"), this::goBack));
        content.clearChildren();
        content.top();
        Table side = new Table();
        side.top();
        side.defaults().growX();
        String note = ContentNames.historyNote(itemId);
        if (note != null) {
            side.add(realThing(note)).padBottom(Theme.SPACE_4).row();
        }
        side.add(notYours()).padBottom(Theme.SPACE_4).row();
        Table dossiers = dossiers();
        if (dossiers != null) {
            side.add(dossiers).row();
        }
        Table columns = new Table();
        columns.top();
        columns.add(file()).expandX().fillX().top().padRight(Theme.SPACE_4);
        columns.add(side).width(SIDE_WIDTH).top();
        content.add(scrollPane(columns)).expand().fill();
    }

    private Table file() {
        Table file = new Table();
        file.setBackground(ui.card().getBackground());
        file.top();
        file.add(strip()).growX().row();
        Table body = new Table();
        body.top();
        body.pad(Theme.SPACE_5);
        body.defaults().growX().left();
        Table head = new Table();
        head.top();
        String art = rocket() ? "rockets/" + itemId + ".jpg" : "programs/" + itemId + ".jpg";
        Drawable picture = ui.croppedPicture(art, 1.55f);
        head.add(new Image(picture == null ? ui.solid(Theme.NAVY) : picture, Scaling.fill))
            .size(ART_WIDTH, ART_WIDTH / 1.55f).top().padRight(Theme.SPACE_4);
        Table words = new Table();
        words.top().left();
        words.defaults().left();
        Table tags = new Table();
        tags.add(IntelligenceOfficeScreen.kindTag(ui, rocket())).padRight(Theme.SPACE_2);
        tags.add(tag("Their hardware"));
        words.add(tags).padBottom(Theme.SPACE_2).row();
        words.add(ui.label(ContentNames.of(itemId), Theme.TextStyle.HEADING_LG, Theme.INK)).padBottom(Theme.SPACE_2)
            .row();
        ForeignHardware.Row row = ForeignHardware.standard().row(itemId, session.state(), session.catalog(),
            session.gameInfo().response().decided());
        if (row.role() != null) {
            words.add(ui.label(row.role(), Theme.TextStyle.BODY_STRONG, Theme.INK)).padBottom(Theme.SPACE_1).row();
        }
        if (row.linkLead() != null) {
            words.add(counterpart(row)).row();
        }
        head.add(words).expandX().fillX().top();
        body.add(head).padBottom(Theme.SPACE_4).row();
        body.add(eyebrow("About")).padBottom(Theme.SPACE_2).row();
        String about = ContentNames.longDescription(itemId);
        body.add(ui.paragraph(about == null ? ContentNames.description(itemId) : about, Theme.TextStyle.BODY, Theme.INK))
            .padBottom(Theme.SPACE_4).row();
        body.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).padBottom(Theme.SPACE_3).row();
        body.add(eyebrow(rocket() ? "Blueprint" : "Its hardware")).padBottom(Theme.SPACE_2).row();
        Table grid = new Table();
        grid.top();
        int column = 0;
        for (String part : parts()) {
            grid.add(part(part)).expandX().fillX().uniformX().top().pad(0f, 0f, Theme.SPACE_3, column == 0
                ? Theme.SPACE_4 : 0f);
            if (++column == 2) {
                column = 0;
                grid.row();
            }
        }
        if (column == 1) {
            grid.add().expandX().fillX().uniformX();
        }
        body.add(grid).row();
        file.add(body).growX();
        return file;
    }

    private Table strip() {
        Archetype side = ForeignHardware.side(itemId, session.catalog());
        Table strip = new Table();
        strip.setBackground(new DashedBox(ui.solid(Theme.NAVY), null,
            ui.solid(new Color(Theme.NAVY).lerp(Theme.ON_FILL, 0.08f))));
        strip.pad(Theme.SPACE_2, Theme.SPACE_4, Theme.SPACE_2, Theme.SPACE_4);
        Table stamp = new Table();
        stamp.setBackground(ui.rect(Theme.NAVY, Theme.ON_FILL, Theme.BORDER_EMPHASIS));
        stamp.pad(4f, Theme.SPACE_3, 4f, Theme.SPACE_3);
        Image glyph = new Image(ui.icon("document"), Scaling.fit);
        glyph.setColor(Theme.ON_FILL);
        stamp.add(glyph).size(12f).padRight(6f);
        stamp.add(ui.label("FOREIGN FILE · READ ONLY", Theme.TextStyle.LABEL, Theme.ON_FILL));
        strip.add(stamp).left().expandX();
        Integer learned = session.state().getIntelligence().getForeignHardware().get(itemId);
        String who = (side == null ? "Foreign" : Museum.archetype(side)) + " hardware";
        strip.add(ui.label(learned == null ? who : who + " · analysed " + Museum.date(learned), Theme.TextStyle.CAPTION,
            Theme.ON_FILL)).right();
        return strip;
    }

    private Table tag(String text) {
        Table tag = new Table();
        tag.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        tag.pad(2f, 6f, 2f, 6f);
        tag.add(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED));
        return tag;
    }

    private Label eyebrow(String text) {
        return ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
    }

    private Table counterpart(ForeignHardware.Row row) {
        Table line = new Table();
        line.add(ui.label(Character.toUpperCase(row.linkLead().charAt(0)) + row.linkLead().substring(1),
            Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
        String own = ForeignHardware.standard().items().get(itemId).counterpart();
        // A counterpart that's a program has a page of its own; a rocket's lives in the VAB's list.
        boolean page = own != null && session.catalog().program(own).isPresent();
        Label name = ui.label(row.linkName(), Theme.TextStyle.BODY_SM, page ? Theme.NAVY : Theme.INK);
        if (page) {
            name.setTouchable(Touchable.enabled);
            name.addListener(clicked(() -> game.setScreen(new ProgramDetailsScreen(game, own, null))));
            Table underlined = new Table();
            underlined.add(name).row();
            underlined.add(new Image(ui.solid(Theme.NAVY))).height(1f).growX();
            line.add(underlined);
        } else {
            line.add(name);
        }
        return line;
    }

    private Table part(String id) {
        Table part = new Table();
        part.top();
        part.defaults().growX().left();
        part.add(ui.label(ContentNames.of(id), Theme.TextStyle.BODY_STRONG, Theme.INK)).padBottom(Theme.SPACE_1).row();
        // A rocket's About is already this description.
        String description = rocket() ? null : ContentNames.description(id);
        if (description != null) {
            part.add(ui.paragraph(description, Theme.TextStyle.BODY_SM, Theme.INK)).padBottom(Theme.SPACE_2).row();
        }
        TextureRegionDrawable blueprint = ui.picture("blueprints/" + id + ".jpg");
        if (blueprint != null) {
            part.add(new FullWidthPicture(blueprint)).row();
        }
        return part;
    }

    private Table realThing(String note) {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        Image glyph = new Image(ui.historyNoteIcon(), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        head.add(glyph).size(16f).padRight(Theme.SPACE_2);
        head.add(eyebrow("The real thing")).expandX().left();
        card.add(head).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph(note, Theme.TextStyle.BODY_SM, Theme.INK));
        return card;
    }

    private Table notYours() {
        Table box = new Table();
        box.setBackground(new DashedBox(ui.solid(Theme.SURFACE_CARD), ui.solid(Theme.NAVY_BORDER),
            ui.solid(new Color(Theme.NAVY).lerp(Theme.SURFACE_CARD, 0.96f))));
        box.pad(Theme.SPACE_4);
        box.top();
        box.defaults().growX().left();
        box.add(ui.label("Theirs, not yours", Theme.TextStyle.BODY_STRONG, Theme.INK)).padBottom(Theme.SPACE_1).row();
        box.add(ui.paragraph("You can't open or build the other side's hardware, so there are no prices here. How "
            + "reliable theirs is lives in each rival's R&D dossier.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
        return box;
    }

    /** The rivals' R&amp;D dossiers that list this item's parts, or {@code null} with none. */
    private Table dossiers() {
        List<String> parts = parts();
        List<String[]> rows = new ArrayList<>();
        for (RivalFolder folder : session.state().getIntelligence().getFolders().values()) {
            Dossier dossier = folder.getDossiers().get(IntelArea.RND);
            if (dossier == null) {
                continue;
            }
            for (IntelFact fact : dossier.facts()) {
                if (parts.contains(fact.ref())) {
                    rows.add(new String[] {ContentNames.of(fact.ref()) + " · " + folder.getAgencyName(),
                        Museum.date(dossier.turn())});
                }
            }
        }
        if (rows.isEmpty()) {
            return null;
        }
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        card.add(eyebrow("In your dossiers")).padBottom(Theme.SPACE_2).row();
        for (String[] row : rows) {
            Table line = new Table();
            line.add(ui.paragraph(row[0], Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX().left()
                .padRight(Theme.SPACE_2);
            line.add(ui.label(row[1], Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
            card.add(line).padBottom(Theme.SPACE_1).row();
        }
        card.add(ui.paragraph("Where their R&D dossiers list its parts, with reliability as a range.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(Theme.SPACE_1);
        return card;
    }
}
