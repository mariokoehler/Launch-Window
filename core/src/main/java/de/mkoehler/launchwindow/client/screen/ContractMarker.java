package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.ContractsView;
import de.mkoehler.launchwindow.content.Commercial;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.Contracts;

import java.util.ArrayList;
import java.util.List;

/**
 * The contract marker ({@code ui-spec.md} component 27, the canvas {@code Launch Window — Contracts}):
 * the outline icon on a flight that unlocks a contract line, with a tick once flown, and the gold
 * filled icon on the flight a line's contracts fly; and a program's contract line.
 *
 * @author Mario Koehler
 */
final class ContractMarker {

    /** The marker's size beside a configuration's name. */
    static final float SIZE = 20f;

    private ContractMarker() {
    }

    /**
     * Returns a configuration's marker: outline while its program's unlock set isn't complete, filled
     * gold on the contract flight once it is, and nothing otherwise.
     *
     * @param host            the screen
     * @param configurationId the configuration
     * @return the marker, or {@code null}
     */
    static Table of(SessionScreen host, String configurationId) {
        PlayerSnapshot state = host.session.state();
        ContentCatalog catalog = host.session.catalog();
        Commercial commercial = Contracts.unlockSetOf(configurationId, catalog).orElse(null);
        if (commercial == null) {
            return null;
        }
        UiKit ui = host.ui;
        String line = ContentNames.of(Contracts.lineOf(commercial.programId(), catalog));
        Table marker = new Table();
        if (Contracts.unlocked(state, commercial)) {
            if (!commercial.contractConfigurationId().equals(configurationId)) {
                return null;
            }
            Image icon = new Image(ui.icon("contract_flight"), Scaling.fit);
            icon.setColor(Theme.GOLD);
            marker.add(icon).size(SIZE);
            ui.tooltip(marker, "Contract flight", "Contracts in " + line + " fly this configuration. Open contracts "
                + "are on the board in Headquarters.");
            return marker;
        }
        Stack stack = new Stack();
        Image icon = new Image(ui.icon("contract_unlock"), Scaling.fit);
        icon.setColor(Theme.NAVY);
        stack.add(icon);
        if (host.session.state().getSuccessfulFlightCounts().getOrDefault(configurationId, 0) > 0) {
            Table tick = new Table();
            tick.bottom().right();
            Table badge = new Table();
            badge.setBackground(ui.circle(Theme.SUCCESS, Theme.SURFACE_CARD, Theme.BORDER_EMPHASIS, 11));
            Image check = new Image(ui.icon("check"), Scaling.fit);
            check.setColor(Theme.SURFACE_CARD);
            badge.add(check).size(7f);
            tick.add(badge).size(11f).padRight(-5f).padBottom(-4f);
            stack.add(tick);
        }
        marker.add(stack).size(SIZE);
        List<String> rows = new ArrayList<>();
        for (String id : commercial.unlockConfigurationIds()) {
            boolean flown = state.getSuccessfulFlightCounts().getOrDefault(id, 0) > 0;
            rows.add((flown ? "Flown · " : "To fly · ")
                + java.util.Objects.requireNonNullElse(ContentNames.configuration(id), ContentNames.of(id)));
        }
        ui.tooltip(marker, "Unlock flight · contract line", ContractsView.unlockTip(commercial, state, catalog)
            + "\n" + String.join("\n", rows));
        return marker;
    }

    /**
     * Returns a program's contract line for its card or details: the outline icon and "Commercial line:
     * Earth-orbit satellites · 1 of 2 marked flights flown", or the gold one and "· unlocked".
     *
     * @param host      the screen
     * @param programId the program
     * @return the line, or {@code null} for a program without contracts
     */
    static Table programLine(SessionScreen host, String programId) {
        Commercial commercial = host.session.catalog().commercial(programId).orElse(null);
        if (commercial == null) {
            return null;
        }
        PlayerSnapshot state = host.session.state();
        boolean unlocked = Contracts.unlocked(state, commercial);
        UiKit ui = host.ui;
        Table row = new Table();
        row.left();
        Image icon = new Image(ui.icon(unlocked ? "contract_flight" : "contract_unlock"), Scaling.fit);
        icon.setColor(unlocked ? Theme.GOLD : Theme.NAVY);
        row.add(icon).size(14f).padRight(6f);
        String text = ContractsView.programLine(commercial, state, host.session.catalog());
        int cut = text.lastIndexOf(" · ");
        row.add(ui.label(text.substring(0, cut + 3), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(ui.label(text.substring(cut + 3), Theme.TextStyle.CAPTION, unlocked ? Theme.SUCCESS : Theme.INK_MUTED))
            .expandX().align(Align.left);
        return row;
    }
}
