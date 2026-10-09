package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.SkillBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.TraitChip;
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Museum;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.MemorialEntry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The memorial's person page ({@code ui-spec.md} &sect;4.14, People canvas; {@code design.md}
 * &sect;2.2(d), phase 4): the living page's layout, read-only, for an astronaut the agency lost.
 * Who they were and how it ended, skills at the end, the traits the player knew, and the career.
 *
 * @author Mario Koehler
 */
public final class MemorialPersonScreen extends ChromeScreen {

    private static final float LEFT_WIDTH = 400f;
    private static final float MIDDLE_WIDTH = 720f;
    private static final float PORTRAIT = 128f;

    private final int index;
    private final Supplier<Screen> back;

    /**
     * Builds the page for one memorial entry.
     *
     * @param game  the game, in a session
     * @param index the entry's place in the memorial
     * @param back  builds the screen to return to
     */
    public MemorialPersonScreen(LaunchWindowGame game, int index, Supplier<Screen> back) {
        super(game, List.of(Crumb.SPACE_COMPLEX, new Crumb("Museum", ChromeScreen::goMuseum),
            new Crumb(game.session().state().getMemorial().get(index).name(), null)), "Museum · In memory",
            game.session().state().getMemorial().get(index).name());
        this.index = index;
        this.back = back;
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(back.get());
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.MEMORIAL;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top().left();
        MemorialEntry entry = session.state().getMemorial().get(index);
        Table left = new Table();
        left.top();
        left.defaults().growX().padBottom(Theme.SPACE_4);
        left.add(whoCard(entry)).row();
        left.add(endCard(entry)).row();
        content.add(left).width(LEFT_WIDTH).top().padRight(Theme.SPACE_4);
        Table middle = new Table();
        middle.top();
        middle.defaults().growX().padBottom(Theme.SPACE_4);
        middle.add(skillsCard(entry)).row();
        middle.add(traitsCard(entry)).row();
        content.add(scrollPane(middle)).width(MIDDLE_WIDTH).top().expandY().fillY().padRight(Theme.SPACE_4);
        Table story = new Table();
        story.top();
        story.defaults().growX().padBottom(Theme.SPACE_4);
        story.add(CareerCards.career(ui, entry.career(), null, session.catalog())).row();
        content.add(scrollPane(story)).expand().fill().top();
    }

    private Table whoCard(MemorialEntry entry) {
        Table card = ui.card();
        card.top().left();
        card.defaults().left();
        Drawable portrait = entry.portraitId() == null ? null : ui.picture("portraits/" + entry.portraitId() + ".jpg");
        Table head = new Table();
        head.top().left();
        Image image = new Image(portrait == null ? ui.solid(Theme.NAVY) : portrait, Scaling.fill);
        // Dimmed toward grey: remembered, not on the roster.
        image.setColor(0.72f, 0.72f, 0.72f, 1f);
        head.add(image).size(PORTRAIT).top().padRight(Theme.SPACE_3);
        Table names = new Table();
        names.top().left();
        names.defaults().left();
        names.add(ui.label("Astronaut", Theme.TextStyle.EYEBROW, Theme.INK)).padBottom(4f).row();
        names.add(ui.label(entry.name(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(4f).row();
        String from = entry.joinedTurn() >= 1 ? String.valueOf(GameDate.ofTurn(entry.joinedTurn()).year()) : "?";
        names.add(ui.label(("Served " + from + " to " + GameDate.ofTurn(Math.max(1, entry.turn())).year())
            .toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        head.add(names).expandX().fillX().top();
        card.add(head).growX();
        return card;
    }

    private Table endCard(MemorialEntry entry) {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(title("How it ended")).padBottom(Theme.SPACE_2).row();
        Museum.Plaque plaque = Museum.memorial(session.state(), session.catalog()).get(index);
        card.add(ui.paragraph(plaque.line(), Theme.TextStyle.BODY_STRONG, Theme.INK)).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph(plaque.detail(), Theme.TextStyle.BODY_SM, Theme.INK)).row();
        return card;
    }

    private Table skillsCard(MemorialEntry entry) {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(title("Skills at the end")).padBottom(Theme.SPACE_3).row();
        Department department = Department.ASTRONAUT;
        for (Map.Entry<String, Double> skill : entry.skills().entrySet()) {
            Table row = new Table();
            row.add(ui.label(department.label(skill.getKey()), Theme.TextStyle.BODY_SM, Theme.INK)).width(220f).left();
            row.add(new SkillBar(ui, skill.getValue())).expandX().fillX().height(6f).padRight(Theme.SPACE_4);
            row.add(ui.label(String.format(Locale.ROOT, "%.1f", skill.getValue()), Theme.TextStyle.BODY_SM, Theme.INK))
                .width(44f).right();
            card.add(row).padBottom(6f).row();
        }
        return card;
    }

    private Table traitsCard(MemorialEntry entry) {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(title("Traits")).padBottom(Theme.SPACE_3).row();
        if (entry.traits().isEmpty()) {
            card.add(ui.label("None had come out.", Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED)).row();
        } else {
            card.add(TraitChip.row(ui, entry.traits(), MIDDLE_WIDTH - 40f)).row();
        }
        return card;
    }

    private Container<Label> title(String text) {
        return new Container<>(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .left();
    }
}
