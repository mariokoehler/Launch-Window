package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.RelationshipMarker;
import de.mkoehler.launchwindow.client.ui.SkillBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.TraitChip;
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.client.view.Tips;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The person page ({@code ui-spec.md} &sect;4.14, People canvas, 2026-10-07): everything about one
 * employee, so the player gets to know them. Who they are (portrait, group, age, state, salary,
 * morale, the row's actions) on the left; what they are (skills, revealed traits, shown
 * relationships) in the middle. Opened from a roster name, a turn report line or a relationship.
 * The ambition, loyalty and career record arrive with phase 4, and with them the right-hand column
 * and the memorial's read-only page.
 *
 * @author Mario Koehler
 */
public final class PersonScreen extends ChromeScreen {

    private static final float LEFT_WIDTH = 440f;
    private static final float MIDDLE_WIDTH = 720f;
    private static final float PORTRAIT = 128f;

    private final String personId;
    private final Department department;
    private final Supplier<Screen> back;
    private final PersonnelDialogs dialogs;

    /**
     * Opens someone's page.
     *
     * @param game   the game, in a session
     * @param person the person
     * @param back   builds the screen to return to
     */
    public PersonScreen(LaunchWindowGame game, Personnel person, Supplier<Screen> back) {
        this(game, person, departmentOf(person), back);
    }

    private PersonScreen(LaunchWindowGame game, Personnel person, Department department, Supplier<Screen> back) {
        super(game, List.of(Crumb.SPACE_COMPLEX, buildingCrumb(department), new Crumb(person.getName(), screen -> { })),
            department.place() + " · Personnel", person.getName());
        this.personId = person.getId();
        this.department = department;
        this.back = back;
        this.dialogs = new PersonnelDialogs(this, department);
        rebuild();
    }

    /**
     * Returns the department someone works in.
     *
     * @param person the person
     * @return the department
     */
    static Department departmentOf(Personnel person) {
        if (person instanceof Astronaut) {
            return Department.ASTRONAUT;
        }
        return person instanceof MissionControlOfficer ? Department.MISSION_CONTROL : Department.SET;
    }

    private static Crumb buildingCrumb(Department department) {
        return switch (department) {
            case SET -> Crumb.SET_CENTRE;
            case ASTRONAUT -> Crumb.ASTRONAUT_COMPLEX;
            case MISSION_CONTROL -> Crumb.MISSION_CONTROL;
        };
    }

    @Override
    protected void goBack() {
        game.setScreen(back.get());
    }

    private Personnel person() {
        return People.find(session.state(), personId);
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.PERSON;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top().left();
        Personnel person = person();
        if (person == null) {
            // Dismissed, retired or lost since the page opened.
            content.add(ui.notice(Theme.NoticeKind.INFO, "No longer with the agency.",
                "This person has left since the page was opened.")).width(LEFT_WIDTH).left();
            return;
        }
        content.add(whoCard(person)).width(LEFT_WIDTH).top().padRight(Theme.SPACE_4);
        Table middle = new Table();
        middle.top();
        middle.defaults().growX().padBottom(Theme.SPACE_4);
        middle.add(skillsCard(person)).row();
        middle.add(traitsCard(person)).row();
        middle.add(relationshipsCard(person)).row();
        content.add(scrollPane(middle)).width(MIDDLE_WIDTH).top().expandY().fillY().padRight(Theme.SPACE_4);
        // The story column (phase 4): the ambition and the career record.
        Table story = new Table();
        story.top();
        story.defaults().growX().padBottom(Theme.SPACE_4);
        Table ambition = CareerCards.ambition(ui, person);
        if (ambition != null) {
            story.add(ambition).row();
        }
        story.add(CareerCards.career(ui, person.getCareer(), CareerCards.running(person), session.catalog())).row();
        content.add(scrollPane(story)).expand().fill().top();
    }

    // ---- Who they are

    private Table whoCard(Personnel person) {
        Table card = ui.card();
        card.top().left();
        card.defaults().left();
        Table head = new Table();
        head.top().left();
        head.add(RosterCells.avatar(ui, person)).size(PORTRAIT).top().padRight(Theme.SPACE_3);
        Table names = new Table();
        names.top().left();
        names.defaults().left();
        names.add(ui.label(department.place() + " · " + role(), Theme.TextStyle.EYEBROW, Theme.INK)).padBottom(4f)
            .row();
        names.add(ui.label(person.getName(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(4f).row();
        names.add(ui.label(department.ageLine(person), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        de.mkoehler.launchwindow.model.CareerEntry joined = de.mkoehler.launchwindow.client.view.PoachTexts
            .joined(person);
        if (joined != null) {
            Table tag = new Table();
            tag.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.SUCCESS, Theme.BORDER_THIN));
            tag.pad(2f, 6f, 2f, 6f);
            Image arrow = new Image(ui.icon("joined"), Scaling.fit);
            arrow.setColor(Theme.SUCCESS);
            tag.add(arrow).size(11f).padRight(4f);
            tag.add(ui.label(("Joined from " + joined.ref()).toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
                Theme.SUCCESS));
            names.add(tag).padTop(Theme.SPACE_2).row();
            names.add(ui.label(de.mkoehler.launchwindow.client.view.CareerTexts.date(joined) + ", for +" + joined.count()
                + " %", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(2f);
        }
        head.add(names).expandX().fillX().top();
        card.add(head).growX().padBottom(Theme.SPACE_4).row();

        Table facts = new Table();
        facts.left();
        facts.defaults().left().padBottom(Theme.SPACE_3);
        String state = People.stateText(person, session.state(), session.catalog());
        boolean active = person.getUnavailableSeasons() == 0 && person.getAssignmentState() != AssignmentState.IDLE;
        facts.add(factLabel("State")).width(90f);
        facts.add(active ? ui.badge(Theme.BadgeKind.ACTIVE, state) : RosterCells.pill(ui, state)).row();
        facts.add(factLabel("Salary"));
        facts.add(ui.label(String.format(Locale.ROOT, "$ %.1f a season", person.getSalary()), Theme.TextStyle.BODY_SM,
            Theme.INK)).row();
        facts.add(factLabel("Morale"));
        Table morale = new Table();
        Color color = RosterCells.moraleColor(person.getMorale());
        Image bar = new Image(ui.solid(color));
        Table track = new Table();
        track.setBackground(ui.solid(Theme.SKILL_TRACK));
        track.left();
        track.add(bar).width(180f * Math.max(0, Math.min(100, person.getMorale())) / 100f).height(6f);
        track.add().expandX();
        morale.add(track).width(180f).height(6f).padRight(Theme.SPACE_3);
        morale.add(ui.label(String.valueOf(person.getMorale()), Theme.TextStyle.BODY_STRONG, color));
        Tips.Tip tip = Tips.stat("morale");
        if (tip != null) {
            ui.tooltip(morale, tip.title(), tip.text());
        }
        facts.add(morale).row();
        facts.add(factLabel("Loyalty"));
        Table loyalty = CareerCards.bar(ui, person.getLoyalty(), Theme.NAVY);
        ui.tooltip(loyalty, "Loyalty · " + de.mkoehler.launchwindow.client.view.CareerTexts.loyaltyBand(
            person.getLoyalty()), "How attached this person is to the agency. It grows with every season here and with "
            + "raises, and with ambitions that come true; it falls when one is passed over or runs out. It's what "
            + "keeps rivals from hiring engineers and flight controllers away.");
        facts.add(loyalty).row();
        card.add(facts).growX().row();
        if (department != Department.ASTRONAUT) {
            card.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(1f).growX().padBottom(Theme.SPACE_3).row();
            float inner = LEFT_WIDTH - 2 * (Theme.SPACE_4 + Theme.BORDER_EMPHASIS);
            card.add(RetentionCards.personRow(this, person, department == Department.SET
                ? de.mkoehler.launchwindow.rules.PersonnelGroup.SET
                : de.mkoehler.launchwindow.rules.PersonnelGroup.MISSION_CONTROL, inner)).width(inner)
                .padBottom(Theme.SPACE_3).row();
        }

        Table actions = new Table();
        actions.left();
        if (person.getUnavailableSeasons() == 0 && person.getAssignmentState() == AssignmentState.IDLE) {
            actions.add(actionButton("Train", Theme.ButtonKind.SECONDARY, () -> dialogs.openTraining(person)))
                .padRight(6f);
        } else if (person.getAssignmentState() == AssignmentState.IN_TRAINING
            && person.getTrainingKind() != TrainingKind.BASIC) {
            actions.add(actionButton("Pull out", Theme.ButtonKind.SECONDARY, () -> dialogs.confirmPullOut(person)))
                .padRight(6f);
        }
        actions.add(actionButton("Dismiss", Theme.ButtonKind.DESTRUCTIVE, () -> dialogs.confirmDismiss(person)));
        card.add(actions).padTop(Theme.SPACE_2);
        return card;
    }

    private String role() {
        return switch (department) {
            case SET -> "Engineer";
            case ASTRONAUT -> "Astronaut";
            case MISSION_CONTROL -> "Flight controller";
        };
    }

    private Label factLabel(String text) {
        return ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED);
    }

    // ---- What they are

    private Table skillsCard(Personnel person) {
        Table card = ui.card();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(sectionTitle("Skills")).expandX().left();
        Table potential = RosterCells.statChip(ui, "learning_potential", "Learning potential "
            + person.getLearningPotential(), Theme.INK_MUTED);
        head.add(potential);
        card.add(head).padBottom(Theme.SPACE_3).row();
        for (String skill : department.skills()) {
            card.add(skillRow(person, skill, department.label(skill), department.icon(skill),
                department.skill(person, skill), department.isExpert(person, skill))).padBottom(6f).row();
        }
        if (person instanceof MissionControlOfficer officer) {
            card.add(skillRow(person, "FLIGHT_DIRECTOR", "Flight Director", "skill_flight_director",
                officer.flightDirectorSkill(), false)).padBottom(6f).row();
        }
        return card;
    }

    private Table skillRow(Personnel person, String skill, String label, String icon, double value, boolean expert) {
        Table row = new Table();
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        // A fixed glyph column, so every bar starts at the same x whatever the icon's shape.
        Container<Image> glyphCell = new Container<>(glyph).size(14f * Math.min(1f, ui.iconAspect(icon)), 14f);
        row.add(glyphCell).width(18f).padRight(Theme.SPACE_2);
        Table name = new Table();
        name.left();
        name.add(ui.label(label, Theme.TextStyle.BODY_SM, Theme.INK));
        if (expert) {
            name.add(ui.badge(Theme.BadgeKind.ACTIVE, "Expert")).padLeft(Theme.SPACE_2);
        }
        row.add(name).width(220f).left();
        row.add(new SkillBar(ui, value)).expandX().fillX().height(6f).padRight(Theme.SPACE_4);
        row.add(ui.label(String.format(Locale.ROOT, "%.1f", value), Theme.TextStyle.BODY_SM, Theme.INK)).width(44f)
            .right();
        Tips.Tip tip = Tips.skill(label, skill);
        ui.tooltip(row, tip.title() + (expert ? " · Expert" : ""), tip.text());
        return row;
    }

    private Table traitsCard(Personnel person) {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(sectionTitle("Traits")).padBottom(Theme.SPACE_3).row();
        List<Trait> traits = People.traits(person);
        if (!traits.isEmpty()) {
            card.add(TraitChip.row(ui, traits, MIDDLE_WIDTH - 40f)).padBottom(Theme.SPACE_2).row();
        }
        boolean learning = People.gettingToKnow(person);
        card.add(ui.label(learning ? "Still getting to know each other" : "Nothing more to discover",
            Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED)).row();
        String hint = People.hintQuote(person.getHint());
        if (learning && hint != null) {
            card.add(ui.paragraph("When hired, a former colleague said: " + hint + ".", Theme.TextStyle.BODY_SM,
                Theme.INK)).padTop(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table relationshipsCard(Personnel person) {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(sectionTitle("Relationships")).padBottom(Theme.SPACE_2).row();
        List<People.Relationship> relationships = People.relationships(session.state(), person);
        if (relationships.isEmpty()) {
            card.add(ui.paragraph("None yet. A relationship shows once two people have worked side by side long "
                + "enough to really get on, or not.", Theme.TextStyle.BODY_SM, Theme.INK)).padTop(4f).row();
        }
        for (int i = 0; i < relationships.size(); i++) {
            if (i > 0) {
                card.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
            }
            card.add(relationshipRow(relationships.get(i))).padTop(10f).padBottom(10f).row();
        }
        return card;
    }

    private Table relationshipRow(People.Relationship relationship) {
        Personnel partner = relationship.partner();
        Table row = new Table();
        row.add(RosterCells.avatar(ui, partner)).size(40f).padRight(Theme.SPACE_3);
        Table who = new Table();
        who.left();
        who.defaults().left();
        who.add(RelationshipMarker.kind(ui, relationship.positive())).padBottom(4f).row();
        Label name = ui.label(partner.getName(), Theme.TextStyle.BODY_STRONG, Theme.INK);
        name.setTouchable(Touchable.enabled);
        name.addListener(clicked(() -> game.setScreen(new PersonScreen(game, partner, back))));
        who.add(name);
        row.add(who).expandX().fillX().left();
        Table where = new Table();
        where.right();
        where.defaults().right();
        where.add(ui.label(People.group(partner) + " · " + People.stateText(partner, session.state(),
            session.catalog()), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        where.add(ui.label(People.seasonsTogether(relationship.seasonsTogether()), Theme.TextStyle.CAPTION,
            Theme.INK));
        row.add(where).right();
        return row;
    }

    private Container<Label> sectionTitle(String text) {
        return new Container<>(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .left();
    }
}
