package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.SetRetentionAction;
import de.mkoehler.launchwindow.actions.SetRetentionDefaultAction;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.PoachTexts;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Retention;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.Poaching;

import java.util.List;
import java.util.Locale;

/**
 * The standing orders against poaching on screen ({@code ui-spec.md} &sect;4.16, Poaching canvas,
 * 2026-10-08): the department's Retention strip, the person page's Retention row, and the roster
 * mark for someone set otherwise.
 *
 * @author Mario Koehler
 */
final class RetentionCards {

    private RetentionCards() {
    }

    /**
     * Builds a department's Retention strip: the agency default as four segments, what a match does,
     * and who is set otherwise.
     *
     * @param host  the screen, for its actions
     * @param group the department's group
     * @return the strip
     */
    static Table strip(ChromeScreen host, PersonnelGroup group) {
        UiKit ui = host.ui;
        PlayerSnapshot state = host.session.state();
        int current = Poaching.matchPercent(state, group);
        Table strip = ui.card();
        strip.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        Table label = new Table();
        label.left();
        label.defaults().left();
        label.add(ui.label(("Retention · " + (group == PersonnelGroup.SET ? "engineers" : "flight controllers"))
            .toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        label.add(ui.label("Match rival offers up to", Theme.TextStyle.BODY_STRONG, Theme.INK));
        strip.add(label).width(220f).left().padRight(Theme.SPACE_4);
        Table segments = new Table();
        for (int percent : Poaching.MATCH_PERCENTS) {
            boolean on = percent == current;
            Table segment = new Table();
            segment.setBackground(ui.rect(on ? Theme.NAVY : Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
            segment.pad(Theme.SPACE_1, Theme.SPACE_3, Theme.SPACE_1, Theme.SPACE_3);
            segment.add(ui.label(PoachTexts.matchLabel(percent).toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
                on ? Theme.ON_FILL : Theme.NAVY));
            if (!on) {
                segment.setTouchable(Touchable.enabled);
                segment.addListener(ChromeScreen.clicked(() -> host.submit(new SetRetentionDefaultAction(group, percent),
                    result -> host.toast("Rival offers up to " + PoachTexts.matchLabel(percent).toLowerCase(Locale.ROOT)
                        + " are matched now.", Theme.NoticeKind.SUCCESS))));
            }
            segments.add(segment).width(84f);
        }
        strip.add(segments).padRight(Theme.SPACE_4);
        Table text = new Table();
        text.left();
        text.defaults().left();
        text.add(ui.paragraph("A match keeps them: their salary rises to the offer for good, and loyalty rises by "
            + Poaching.MATCH_LOYALTY + ". A bigger offer is theirs to decide.", Theme.TextStyle.CAPTION, Theme.INK))
            .growX().row();
        List<? extends Personnel> people = group == PersonnelGroup.SET ? state.getSetSpecialists()
            : state.getMissionControlOfficers();
        String otherwise = PoachTexts.setOtherwise(people, group);
        if (!otherwise.isEmpty()) {
            text.add(ui.paragraph(otherwise, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX().padTop(2f);
        }
        strip.add(text).expandX().fillX();
        return strip;
    }

    /**
     * Builds the person page's Retention row: three segments, and what they mean for this person.
     *
     * @param host   the screen, for its actions
     * @param person the person, an engineer or controller
     * @param group  their group
     * @param width  the row's width
     * @return the row
     */
    static Table personRow(ChromeScreen host, Personnel person, PersonnelGroup group, float width) {
        UiKit ui = host.ui;
        int percent = Poaching.matchPercent(host.session.state(), group);
        Table row = new Table();
        row.left();
        row.defaults().left();
        Table head = new Table();
        head.add(ui.label("RETENTION", Theme.TextStyle.LABEL, Theme.INK_MUTED)).expandX().left();
        head.add(ui.label("If a rival makes an offer", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(head).width(width).padBottom(Theme.SPACE_2).row();
        Table segments = new Table();
        float third = width / 3f;
        segments.add(segment(host, person, group, Retention.DEFAULT, null, "Agency default",
            PoachTexts.defaultDoes(percent))).width(third);
        segments.add(segment(host, person, group, Retention.KEEP, "retain_keep", "Keep at any price",
            "matches any offer")).width(third);
        segments.add(segment(host, person, group, Retention.LET_GO, "retain_go", "Let go", "never matches"))
            .width(third);
        row.add(segments).width(width).padBottom(Theme.SPACE_2).row();
        row.add(ui.paragraph(PoachTexts.retentionCaption(person.getRetention(), percent, person.getName()),
            Theme.TextStyle.CAPTION, Theme.INK)).width(width);
        return row;
    }

    private static Table segment(ChromeScreen host, Personnel person, PersonnelGroup group, Retention retention,
            String icon, String title, String detail) {
        UiKit ui = host.ui;
        boolean on = person.getRetention() == retention;
        Color ink = on ? Theme.ON_FILL : Theme.NAVY;
        Table segment = new Table();
        segment.setBackground(ui.rect(on ? Theme.NAVY : Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        segment.pad(Theme.SPACE_1, Theme.SPACE_2, Theme.SPACE_1, Theme.SPACE_2);
        Table top = new Table();
        if (icon != null) {
            Image glyph = new Image(ui.icon(icon), Scaling.fit);
            glyph.setColor(ink);
            top.add(glyph).size(11f).padRight(4f);
        }
        top.add(ui.label(title.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, ink));
        segment.add(top).row();
        segment.add(ui.label(detail, Theme.TextStyle.CAPTION, on ? Theme.ON_FILL : Theme.INK_MUTED));
        if (!on) {
            segment.setTouchable(Touchable.enabled);
            segment.addListener(ChromeScreen.clicked(() -> host.submit(new SetRetentionAction(group, person.getId(),
                retention), result -> host.toast("Retention set: " + title.toLowerCase(Locale.ROOT) + ".",
                Theme.NoticeKind.SUCCESS))));
        }
        return segment;
    }

    /**
     * Returns the roster mark for someone set otherwise, or {@code null} on the agency default.
     *
     * @param ui     the kit
     * @param person the person
     * @return the mark, with its tooltip
     */
    static Image mark(UiKit ui, Personnel person) {
        if (person.getRetention() == Retention.DEFAULT) {
            return null;
        }
        boolean keep = person.getRetention() == Retention.KEEP;
        Image mark = new Image(ui.icon(keep ? "retain_keep" : "retain_go"), Scaling.fit);
        mark.setColor(keep ? Theme.NAVY : Theme.INK_MUTED);
        ui.tooltip(mark, keep ? "Keep at any price" : "Let go", keep ? "Every rival offer is matched, whatever it is."
            : "No offer is ever matched; the person decides.");
        return mark;
    }
}
