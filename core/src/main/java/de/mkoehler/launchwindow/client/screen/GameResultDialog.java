package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Museum;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.rules.WinCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The game result ({@code ui-spec.md} &sect;4.13), a dialog over the hub at the first visit after
 * the result, and reopened from the Museum: who won and when, the race table, and for a same-turn
 * finish the medal table. Who won is the server's result; the tables come from the rival
 * summaries (gap G3) through {@code core}'s {@link WinCondition}.
 *
 * @author Mario Koehler
 */
final class GameResultDialog {

    /** The dialog's width, from the mockup. */
    static final float WIDTH = 980f;

    private static final String[] ORDINALS = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th"};

    private final SessionScreen host;
    private final LaunchWindowGame game;
    private final UiKit ui;

    /**
     * Prepares the dialog for a screen.
     *
     * @param host the screen it opens over
     */
    GameResultDialog(SessionScreen host) {
        this.host = host;
        this.game = host.game;
        this.ui = host.ui;
    }

    /**
     * Opens the dialog, fetching the rival summaries first if this session has none. Without them
     * it still names the winner, leaving the tables out.
     */
    void open() {
        if (host.session.rivals() != null) {
            host.showDialog(build(host.session.rivals()), WIDTH);
            return;
        }
        game.gateway().fetchRivals(host.session.credentials(), result -> {
            if (!host.isShowing()) {
                return;
            }
            if (result.ok()) {
                host.session.keepRivals(result.rivals());
            }
            host.showDialog(build(result.ok() ? result.rivals() : List.of()), WIDTH);
        });
    }

    private Table build(List<RivalSummary> rivals) {
        GameInfoResponse info = host.session.gameInfo().response();
        List<String> declared = info.declaredGoalIds();
        List<String> winners = info.winnerAgencies();
        boolean tied = "TIED".equals(info.resultStatus());
        String names = String.join(" and ", winners);
        String mine = host.session.state().getAgencyName();
        boolean youWon = !tied && winners.size() == 1 && winners.get(0).equals(mine);
        Optional<WinCondition.MedalTable> medals = rivals.isEmpty() ? Optional.empty()
            : Museum.medalTable(rivals, declared);

        String title = tied ? "A tie: " + names : names + " wins the race";
        Table card = host.dialogCard("The race is decided", title);

        Table lead = new Table();
        Table trophy = new Table();
        trophy.setBackground(ui.trophy());
        Image star = new Image(ui.icon("prestige"), Scaling.fit);
        star.setColor(Theme.NAVY);
        trophy.add(star).size(26f);
        lead.add(trophy).size(56f).padRight(Theme.SPACE_4);
        Table words = new Table();
        words.defaults().left().growX();
        String goals = declared.size() == 1 ? "the goal" : "all " + words(declared.size()) + " goals";
        String text;
        if (tied) {
            text = names + " finished at the same turn and are level on every place of the medal table, so they "
                + "share the win.";
        } else if (youWon) {
            text = "Congratulations, Director " + surname(host.session.state().getDirectorName()) + ". Your agency "
                + "was first to finish " + goals + " this game is won by.";
        } else {
            String director = info.winnerDirectors().isEmpty() ? "" : ", led by " + info.winnerDirectors().get(0);
            text = names + director + ", finished " + goals + " first" + (medals.isPresent()
                ? ", on the medal table." : ".");
        }
        words.add(ui.paragraph(text, Theme.TextStyle.BODY, Theme.INK)).padBottom(2f).row();
        words.add(ui.paragraph("Decided in " + Museum.date((int) info.resultTurn()) + " (turn "
            + info.resultTurn() + ").", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
        lead.add(words).expandX().fillX();
        card.add(lead).padBottom(Theme.SPACE_4).row();

        List<String> goalNames = declared.stream().map(ContentNames::of).toList();
        if (!rivals.isEmpty()) {
            card.add(RaceTable.build(ui, Museum.race(rivals, declared), goalNames, false)).padBottom(Theme.SPACE_4)
                .row();
        }
        medals.ifPresent(table -> card.add(medalTable(table, rivals, goalNames)).padBottom(Theme.SPACE_4).row());

        Table footer = new Table();
        footer.add(ui.label("Play goes on. You can open this again from the Museum.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).expandX().left();
        if (!(host instanceof MuseumScreen)) {
            footer.add(ChromeScreen.onClick(ui.button("Open the Museum", Theme.ButtonKind.SECONDARY), () -> {
                game.markResultSeen();
                host.closeDialog();
                game.setScreen(new MuseumScreen(game, MuseumScreen.Tab.RACE));
            })).padRight(Theme.SPACE_2);
        }
        footer.add(ChromeScreen.onClick(ui.button("Keep playing", Theme.ButtonKind.PRIMARY), this::keepPlaying));
        card.add(footer).row();
        return card;
    }

    private void keepPlaying() {
        game.markResultSeen();
        host.closeDialog();
        if (!(host instanceof MuseumScreen)) {
            host.toast("You can see the result again in the Museum, on The race.", Theme.NoticeKind.INFO);
        }
    }

    private Table medalTable(WinCondition.MedalTable table, List<RivalSummary> rivals, List<String> goalNames) {
        Table block = new Table();
        block.defaults().growX().left();
        block.add(ui.label(("The medal table · " + table.players().size() + " agencies finished at the same turn")
            .toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        List<String> winners = host.session.gameInfo().response().winnerAgencies();
        List<String> order = new ArrayList<>(table.players());
        order.sort(java.util.Comparator.comparingInt(player -> winners.contains(
            rivals.get(Integer.parseInt(player)).agencyName()) ? 0 : 1));
        List<List<String>> rows = new ArrayList<>();
        for (String player : order) {
            List<String> cells = new ArrayList<>();
            cells.add(rivals.get(Integer.parseInt(player)).agencyName());
            for (int place : table.places().get(player)) {
                cells.add(ORDINALS[Math.min(place, ORDINALS.length) - 1]);
            }
            int[] counts = table.counts(player);
            List<String> summary = new ArrayList<>();
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > 0) {
                    summary.add(counts[i] + " × " + ORDINALS[Math.min(i, ORDINALS.length - 1)]);
                }
            }
            cells.add(String.join(", ", summary));
            rows.add(cells);
        }
        Table grid = new Table();
        grid.setBackground(ui.solid(new com.badlogic.gdx.graphics.Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.18f)));
        grid.defaults().fill().padBottom(Theme.BORDER_THIN);
        List<String> heads = new ArrayList<>();
        heads.add("Agency");
        heads.addAll(goalNames);
        heads.add("Places");
        for (String head : heads) {
            Table cell = new Table();
            cell.setBackground(ui.solid(Theme.NAVY));
            cell.pad(10f, 12f, 10f, 12f);
            cell.add(ui.label(head.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.ON_FILL)).expandX().left();
            grid.add(cell).expandX().uniformX();
        }
        grid.row();
        for (List<String> cells : rows) {
            for (int i = 0; i < cells.size(); i++) {
                Table cell = new Table();
                cell.setBackground(ui.solid(Theme.SURFACE_CARD));
                cell.pad(10f, 12f, 10f, 12f);
                boolean strong = i == 0 || i == cells.size() - 1;
                cell.add(ui.label(cells.get(i), strong ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.BODY_SM,
                    Theme.INK)).expandX().left();
                grid.add(cell);
            }
            grid.row();
        }
        block.add(grid).padBottom(Theme.SPACE_2).row();
        block.add(ui.paragraph("Each goal ranks the tied agencies by the turn they did it; a shared turn is a shared "
            + "place. The most first places wins, then the most second places.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED));
        return block;
    }

    private static String words(int count) {
        String[] small = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
        return count < small.length ? small[count] : String.valueOf(count);
    }

    private static String surname(String director) {
        String[] parts = director.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}
