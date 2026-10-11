package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.ApplyContractAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.FullWidthPicture;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.ContractsView;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.content.Commercial;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.ActiveContract;
import de.mkoehler.launchwindow.model.ContractListing;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.Contracts;

import java.util.List;
import java.util.Locale;

/**
 * Headquarters' Contracts screen ({@code ui-spec.md} &sect;4.17, the canvas
 * {@code Launch Window — Contracts}): the agency's active contract, its record per line, and the
 * board of listings, where it applies for one a turn ({@code design.md} &sect;2.11).
 *
 * @author Mario Koehler
 */
public final class ContractsScreen extends ChromeScreen {

    /** Board cards a row. */
    private static final int PER_ROW = 4;
    /** A listing card's art height when its line has no picture. */
    private static final float NO_ART_HEIGHT = 140f;
    /** The active contract's picture's width; its height follows the picture. */
    private static final float CONTRACT_ART_WIDTH = 220f;

    /** "Only what I can fly", remembered while the client runs. */
    private static boolean onlyFlyable;

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public ContractsScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.HEADQUARTERS, new Crumb("Contracts", null)), "Headquarters",
            "Contracts");
        titleActions.add(ui.label("Clients pay you to fly again what you've already flown. Contracts pay money, "
            + "not prestige.", Theme.TextStyle.BODY, Theme.ON_PAGE_STRONG));
        rebuild();
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.CONTRACTS;
    }

    @Override
    protected void rebuild() {
        PlayerSnapshot state = session.state();
        ContentCatalog catalog = session.catalog();
        content.clearChildren();
        content.top();
        content.defaults().growX();

        Table list = new Table();
        list.top();
        list.defaults().growX();
        // The mockup's 1.15 : 0.85 split.
        Table top = new Table();
        top.add(yourContract(state, catalog)).width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.565f, top))
            .fillY().padRight(Theme.SPACE_4);
        top.add(yourRecord(state, catalog)).width(0f).expandX().fillX().fillY();
        list.add(top).padBottom(Theme.SPACE_5).row();

        List<ContractsView.Offer> offers = ContractsView.board(state, catalog);
        long flyable = offers.stream().filter(ContractsView.Offer::flyable).count();
        Table head = new Table();
        head.add(ui.label(("On offer · " + offers.size() + " · " + flyable + " you could fly")
            .toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).expandX().left();
        head.add(ui.label("Never shows how many applied: applications are sealed", Theme.TextStyle.CAPTION,
            Theme.ON_PAGE)).padRight(Theme.SPACE_5);
        head.add(toggle());
        list.add(head).padBottom(Theme.SPACE_3).row();

        List<ContractsView.Offer> shown = offers.stream().filter(offer -> !onlyFlyable || offer.flyable()).toList();
        if (shown.isEmpty()) {
            list.add(emptyBoard(state, catalog, offers.isEmpty())).row();
        } else {
            Table board = new Table();
            board.top().left();
            for (int i = 0; i < shown.size(); i++) {
                board.add(listingCard(shown.get(i), state, catalog)).width(0f).expandX().fillX().fillY().top()
                    .uniformX().padRight(i % PER_ROW == PER_ROW - 1 ? 0f : Theme.SPACE_4).padBottom(Theme.SPACE_4);
                if (i % PER_ROW == PER_ROW - 1) {
                    board.row();
                }
            }
            for (int i = shown.size() % PER_ROW; i != 0 && i < PER_ROW; i++) {
                board.add().width(0f).expandX().uniformX().padRight(i == PER_ROW - 1 ? 0f : Theme.SPACE_4);
            }
            list.add(board).row();
        }
        content.add(keptScrollPane(list, "contracts")).expand().fill();
    }

    // ---- Your contract ----

    private Table yourContract(PlayerSnapshot state, ContentCatalog catalog) {
        ActiveContract contract = state.getActiveContract();
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(Theme.SPACE_4 + Theme.BORDER_EMPHASIS);
        card.top().left();
        if (contract == null) {
            card.defaults().left().growX();
            card.add(ui.label("Your contract", Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_2).row();
            card.add(ui.paragraph("No active contract. Apply for one below; it's awarded when the turn resolves.",
                Theme.TextStyle.BODY, Theme.INK)).padBottom(Theme.SPACE_2).row();
            for (String rule : List.of(
                "One application a turn, and one contract at a time.",
                "The best " + Contracts.TIERS + " scores win tiers 1 to " + Contracts.TIERS + ": "
                    + Contracts.MARGIN_PERCENTS.stream().map(margin -> "+" + margin + " %").toList()
                    .toString().replace("[", "").replace("]", "") + " over the launch cost.",
                "On failure the insurer refunds the launch cost; the failure costs its prestige as usual.")) {
                card.add(bullet(rule)).padBottom(Theme.SPACE_1).row();
            }
            card.add(actionButton("Help: Contracts", Theme.ButtonKind.SECONDARY, this::openHelp)).left()
                .fill(false, false).padTop(Theme.SPACE_3);
            return card;
        }
        // The whole picture, never cropped (the user's report, 2026-10-11).
        var art = ui.picture("categories/" + contract.lineId() + "_" + state.getArchetype().name() + ".jpg");
        card.add(art == null ? new Image(ui.solid(Theme.NAVY)) : new FullWidthPicture(art)).width(CONTRACT_ART_WIDTH)
            .top()
            .padRight(Theme.SPACE_5);
        Table middle = new Table();
        middle.top().left();
        middle.defaults().left();
        Table titleRow = new Table();
        titleRow.add(ui.label("Your contract", Theme.TextStyle.HEADING_SM, Theme.INK)).padRight(Theme.SPACE_3);
        titleRow.add(ui.badge(Theme.BadgeKind.ACTIVE, "Tier " + contract.tier() + " of " + Contracts.TIERS));
        middle.add(titleRow).padBottom(Theme.SPACE_3).row();
        middle.add(ui.label((ContractsView.client(contract.clientId()) + " · "
            + ContentNames.of(contract.lineId())).toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.INK_MUTED))
            .padBottom(2f).row();
        middle.add(ui.label(ContractsView.title(contract, state), Theme.TextStyle.HEADING_SM, Theme.INK))
            .padBottom(Theme.SPACE_3).row();
        long openTurn = session.openTurn();
        boolean lastSeason = contract.missionId() == null && contract.launchByTurn() <= openTurn;
        Table figures = new Table();
        figures.left();
        figure(figures, "Fee on success", ContractsView.money(contract.fee()), Theme.INK);
        figure(figures, "Launch cost now", ContractsView.money(Contracts.launchCost(state, contract.configurationId(),
            catalog)), Theme.INK);
        Table window = new Table();
        window.add(ui.label("Launch by " + GameDate.ofTurn(contract.launchByTurn()).label(), Theme.TextStyle.HEADING_SM,
            lastSeason ? Theme.DANGER : Theme.INK)).padRight(Theme.SPACE_2);
        window.add(ui.label(contract.missionId() == null ? ContractsView.seasonsLeft(contract, openTurn) : "launched",
            Theme.TextStyle.CAPTION, lastSeason ? Theme.DANGER : Theme.INK_MUTED)).bottom();
        figure(figures, "Window", window);
        middle.add(figures).padBottom(Theme.SPACE_4).row();
        middle.add(stepper(contract, state));
        card.add(middle).width(0f).expandX().fillX().top().padRight(Theme.SPACE_4);

        Table right = new Table();
        right.top().right();
        Table flies = new Table();
        Image flight = new Image(ui.icon("contract_flight"), Scaling.fit);
        flight.setColor(Theme.GOLD);
        flies.add(flight).size(14f).padRight(6f);
        flies.add(ui.label("Flies: " + ContentNames.of(contract.configurationId()), Theme.TextStyle.BODY_SM, Theme.INK));
        right.add(flies).right().row();
        right.add().expandY().row();
        com.badlogic.gdx.scenes.scene2d.ui.Label insurer = ui.paragraph("On failure the insurer refunds the launch "
            + "cost.", Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        insurer.setAlignment(com.badlogic.gdx.utils.Align.right);
        right.add(insurer).width(170f).right().padBottom(Theme.SPACE_2).row();
        int step = ContractsView.step(contract, state);
        String action = step == 1 ? "Plan the flight" : step == 2 ? "Open the mission plan" : "Follow it in flight";
        right.add(actionButton(action, Theme.ButtonKind.PRIMARY, () -> {
            if (step == 3) {
                game.setScreen(new MissionsScreen(game, false));
            } else if (step == 2) {
                game.setScreen(new MissionsScreen(game, true));
            } else {
                game.setScreen(new MissionPlanScreen(game, contract.configurationId(), MissionPlanScreen.Tab.ASSESSMENT));
            }
        })).right();
        card.add(right).fillY().top();
        return card;
    }

    private void figure(Table figures, String label, String value, Color color) {
        figure(figures, label, ui.label(value, Theme.TextStyle.HEADING_SM, color));
    }

    private void figure(Table figures, String label, com.badlogic.gdx.scenes.scene2d.Actor value) {
        Table figure = new Table();
        figure.left();
        figure.defaults().left();
        figure.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f)
            .row();
        figure.add(value);
        figures.add(figure).left().top().padRight(Theme.SPACE_6);
    }

    /** The contract's stepper: awarded, scheduled, flying, paid. */
    private Table stepper(ActiveContract contract, PlayerSnapshot state) {
        int now = ContractsView.step(contract, state);
        String[] steps = {"Awarded · " + GameDate.ofTurn(contract.awardedTurn()).label(),
            now >= 2 ? "Scheduled" : "Not scheduled yet", "Flying", "Paid"};
        Table track = new Table();
        track.left();
        for (int i = 0; i < steps.length; i++) {
            boolean done = i < now;
            boolean current = i == now;
            if (i > 0) {
                track.add(new Image(ui.solid(i <= now ? Theme.NAVY : Theme.NAVY_BORDER))).size(26f, 1.5f)
                    .padLeft(Theme.SPACE_2).padRight(Theme.SPACE_2);
            }
            Drawable dot = done ? ui.circle(Theme.NAVY, null, 0f, 14) : current ? ui.circle(Theme.GOLD, null, 0f, 14)
                : ui.circle(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_EMPHASIS, 14);
            track.add(new Image(dot)).size(14f).padRight(6f);
            track.add(ui.label(steps[i], current ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.CAPTION,
                done || current ? Theme.INK : Theme.INK_MUTED));
        }
        return track;
    }

    // ---- Your record ----

    private Table yourRecord(PlayerSnapshot state, ContentCatalog catalog) {
        Table card = ui.card();
        card.top();
        card.defaults().growX();
        Table head = new Table();
        head.add(ui.label("Your record", Theme.TextStyle.HEADING_SM, Theme.INK)).expandX().left();
        head.add(ui.label("One row per line you've unlocked", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        card.add(head).padBottom(Theme.SPACE_3).row();
        List<ContractsView.RecordLine> lines = ContractsView.record(state, catalog);
        if (lines.isEmpty()) {
            card.add(ui.paragraph("No contract lines yet. A program's flights with the contract icon unlock its "
                + "line once every one of them has succeeded.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
            return card;
        }
        Table table = new Table();
        table.defaults().pad(0f, Theme.SPACE_2, 0f, Theme.SPACE_2);
        String[] headers = {"Line", "Flown", "Failed", "Lapsed", "Record", "Score now"};
        for (int i = 0; i < headers.length; i++) {
            table.add(ui.label(headers[i].toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED))
                .align(i == 0 ? Align.left : Align.right).expand(i == 0, false).padBottom(Theme.SPACE_1);
        }
        table.row();
        table.add(new Image(ui.solid(Theme.NAVY))).colspan(headers.length).height(Theme.BORDER_THIN).fillX()
            .pad(0f).padBottom(Theme.SPACE_2).row();
        for (ContractsView.RecordLine line : lines) {
            table.add(ui.label(ContentNames.of(line.lineId()), Theme.TextStyle.BODY_STRONG, Theme.INK)).left()
                .padBottom(Theme.SPACE_2);
            boolean none = line.record().flown() + line.record().failed() + line.record().lapsed() == 0;
            if (none) {
                table.add(ui.label("No contracts flown yet", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).colspan(4)
                    .left().padBottom(Theme.SPACE_2);
            } else {
                table.add(number(line.record().flown())).right().padBottom(Theme.SPACE_2);
                table.add(number(line.record().failed())).right().padBottom(Theme.SPACE_2);
                table.add(number(line.record().lapsed())).right().padBottom(Theme.SPACE_2);
                table.add(ui.label(ContractsView.signed(line.points()), Theme.TextStyle.BODY_STRONG,
                    line.points() > 0 ? Theme.SUCCESS : line.points() < 0 ? Theme.DANGER : Theme.INK)).right()
                    .padBottom(Theme.SPACE_2);
            }
            if (line.score() == null) {
                table.add(ui.label(line.openHint(), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).right()
                    .padBottom(Theme.SPACE_2);
            } else {
                Label score = ui.label(String.valueOf(line.score()), Theme.TextStyle.HEADING_SM, Theme.NAVY);
                Table scored = new Table();
                scored.add(score).row();
                scored.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).growX();
                ui.tooltip(scored, "Your score in " + ContentNames.of(line.lineId()), "Weakest: " + line.weakest()
                    + ", " + line.weakestReliability() + " %\n" + ContractsView.recordWords(line.record()) + ": "
                    + ContractsView.signed(line.points()) + "\nScore: " + line.score()
                    + "\nCrew and controllers don't count: clients judge the hardware.");
                table.add(scored).right().padBottom(Theme.SPACE_2);
            }
            table.row();
        }
        card.add(table).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph("Score = your weakest component's reliability plus your record: "
            + ContractsView.signed(Contracts.RECORD_FLOWN_POINTS) + " a contract flown, "
            + ContractsView.signed(Contracts.RECORD_FAILED_POINTS) + " a failure or lapse, within ±"
            + Contracts.RECORD_CAP + ". The three best scores win tiers 1 to " + Contracts.TIERS + ".",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        return card;
    }

    private Label number(int value) {
        return ui.label(String.valueOf(value), Theme.TextStyle.BODY_SM, Theme.INK);
    }

    // ---- The board ----

    private Table toggle() {
        Table toggle = new Table();
        Table track = new Table();
        track.setBackground(onlyFlyable ? ui.solid(Theme.GOLD) : ui.rect(new Color(1f, 1f, 1f, 0.25f), Theme.ON_PAGE,
            Theme.BORDER_EMPHASIS));
        Image knob = new Image(ui.circle(onlyFlyable ? Theme.NAVY : Theme.ON_PAGE_STRONG, null, 0f, 10));
        track.add(knob).size(10f).expandX().align(onlyFlyable ? Align.right : Align.left).pad(3f);
        toggle.add(track).size(30f, 16f).padRight(Theme.SPACE_2);
        toggle.add(ui.label("Only what I can fly", Theme.TextStyle.BODY_SM, Theme.ON_PAGE_STRONG));
        toggle.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        toggle.addListener(clicked(() -> {
            onlyFlyable = !onlyFlyable;
            rebuild();
        }));
        return toggle;
    }

    private Table emptyBoard(PlayerSnapshot state, ContentCatalog catalog, boolean nothingListed) {
        Table card = ui.card();
        card.defaults().left().growX();
        boolean unlocked = !Contracts.unlockedLines(state, catalog).isEmpty();
        String text = !nothingListed ? "Nothing you can fly is on offer. Turn the filter off to see the rest."
            : unlocked ? "No contracts in your lines this turn. New ones are posted when a turn resolves."
            : "No contracts on offer. Clients come once an agency has flown every marked flight of a program: "
                + "look for the contract icon on its flights.";
        Table row = new Table();
        Image icon = new Image(ui.icon("contract_unlock"), Scaling.fit);
        icon.setColor(Theme.NAVY);
        row.add(icon).size(20f).top().padRight(Theme.SPACE_3);
        row.add(ui.paragraph(text, Theme.TextStyle.BODY, Theme.INK)).expandX().fillX();
        card.add(row);
        return card;
    }

    private Table listingCard(ContractsView.Offer offer, PlayerSnapshot state, ContentCatalog catalog) {
        ContractListing listing = offer.listing();
        boolean dim = !offer.flyable();
        Table card = new Table();
        card.top();
        card.setBackground(offer.applied() ? ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS)
            : ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        float edge = offer.applied() ? Theme.BORDER_EMPHASIS : Theme.BORDER_THIN;
        card.pad(edge);
        card.defaults().growX();
        if (offer.applied()) {
            card.add(new Image(ui.solid(Theme.GOLD))).height(Theme.BORDER_ACCENT).row();
        }

        Stack art = new Stack();
        // The whole picture at the card's width, never cropped or stretched (the user's report, 2026-10-11).
        var picture = ui.picture("categories/" + listing.lineId() + "_" + state.getArchetype().name() + ".jpg");
        Image image = picture == null ? new Image(ui.solid(Theme.NAVY)) : new FullWidthPicture(picture);
        if (dim) {
            image.setColor(0.55f, 0.55f, 0.55f, 0.45f);
        }
        Table artBack = new Table();
        artBack.setBackground(ui.solid(Theme.NAVY));
        artBack.add(image).grow();
        art.add(artBack);
        Table overlay = new Table();
        overlay.top().pad(10f);
        Table line = new Table();
        line.setBackground(ui.solid(Theme.NAVY));
        line.pad(3f, 7f, 3f, 7f);
        line.add(ui.label(ContentNames.of(listing.lineId()), Theme.TextStyle.LABEL, Theme.ON_FILL));
        overlay.add(line).expandX().left().top();
        if (offer.applied()) {
            overlay.add(ui.badge(Theme.BadgeKind.ACTIVE, "Applied")).right().top();
        }
        art.add(overlay);
        var artCell = card.add(art);
        if (picture == null) {
            artCell.height(NO_ART_HEIGHT);
        }
        artCell.row();

        Table body = new Table();
        body.top().left();
        body.pad(14f, 16f, 16f, 16f);
        body.defaults().left().growX();
        Color ink = dim ? Theme.INK_MUTED : Theme.INK;
        body.add(ui.label(offer.client().toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.INK_MUTED))
            .padBottom(3f).row();
        body.add(ui.paragraph(offer.title(), Theme.TextStyle.HEADING_SM, ink)).padBottom(Theme.SPACE_2).row();
        if (!offer.story().isEmpty()) {
            body.add(ui.paragraph(offer.story(), Theme.TextStyle.BODY_SM, ink)).padBottom(Theme.SPACE_3).row();
        }
        if (offer.commercial() != null) {
            Table tiers = new Table();
            for (int tier = 1; tier <= Contracts.TIERS; tier++) {
                Table box = new Table();
                box.setBackground(dim ? ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN)
                    : ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.04f), Theme.NAVY_BORDER,
                    Theme.BORDER_THIN));
                box.pad(6f, 8f, 6f, 8f);
                box.defaults().left();
                box.add(ui.label(ContractsView.tierLabel(tier).toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
                    Theme.INK_MUTED)).row();
                box.add(ui.label(ContractsView.money(offer.fee(tier)), Theme.TextStyle.HEADING_SM, ink));
                tiers.add(box).width(0f).expandX().fillX().uniformX().padRight(tier < Contracts.TIERS ? 6f : 0f);
            }
            body.add(tiers).padBottom(5f).row();
            body.add(ui.label("Your fee at each tier · launch cost now " + ContractsView.money(offer.launchCost()),
                Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
            body.add(marker(offer.commercial(), state, catalog, ContractsView.flies(offer))).padBottom(4f).row();
        }
        body.add(meta("clock", Theme.INK_MUTED, ContractsView.posted(listing, session.openTurn())))
            .padBottom(Theme.SPACE_3).row();
        body.add().expandY().row();
        body.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).padBottom(Theme.SPACE_2).row();
        body.add(foot(offer, state, catalog)).minHeight(32f);
        card.add(body).expand().fill();
        return card;
    }

    private Table foot(ContractsView.Offer offer, PlayerSnapshot state, ContentCatalog catalog) {
        Table foot = new Table();
        if (offer.applied()) {
            Table words = new Table();
            words.defaults().left();
            words.add(ui.label("Your application", Theme.TextStyle.BODY_STRONG, Theme.NAVY)).row();
            words.add(ui.label("decided when the turn resolves", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            foot.add(words).expandX().left();
            foot.add(actionButton("Withdraw", Theme.ButtonKind.DESTRUCTIVE, () -> submit(
                new ApplyContractAction(null, null), result -> toast("Application withdrawn.",
                    Theme.NoticeKind.SUCCESS)))).right();
            return foot;
        }
        String[] reason = ContractsView.reason(offer, state, catalog);
        if (reason != null) {
            foot.add(meta(reason[0], Theme.INK_MUTED, reason[1])).expandX().fillX().left();
            return foot;
        }
        foot.add(ui.label("Applications are sealed", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        foot.add(actionButton("Apply", Theme.ButtonKind.PRIMARY, () -> apply(offer, state, catalog))).right();
        return foot;
    }

    /** An icon and a line of text, as the cards' meta rows. */
    private Table meta(String icon, Color iconColor, String text) {
        Table meta = new Table();
        meta.left();
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(iconColor);
        meta.add(glyph).size(14f).top().padRight(6f).padTop(1f);
        meta.add(ui.paragraph(text, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX();
        return meta;
    }

    /** The flight a listing flies, with component 27's gold marker and its tooltip. */
    private Table marker(Commercial commercial, PlayerSnapshot state, ContentCatalog catalog, String text) {
        Table meta = meta("contract_flight", Theme.GOLD, text);
        ui.tooltip(meta, "Contract flight", "Contracts in " + ContentNames.of(Contracts.lineOf(commercial.programId(),
            catalog)) + " fly this configuration. " + ContractsView.unlockTip(commercial, state, catalog));
        return meta;
    }

    private Table bullet(String text) {
        Table row = new Table();
        row.left();
        row.add(new Image(ui.solid(Theme.NAVY))).size(5f).top().padTop(6f).padRight(Theme.SPACE_2);
        row.add(ui.paragraph(text, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX();
        return row;
    }

    // ---- Applying ----

    /**
     * Starts applying for a listing, as its Apply button does: the move confirmation, the program
     * picker or the terms. For the offscreen screenshot tool.
     *
     * @param listingId the listing
     */
    public void openApply(String listingId) {
        PlayerSnapshot state = session.state();
        ContentCatalog catalog = session.catalog();
        apply(ContractsView.offer(state, Contracts.listing(state, listingId).orElseThrow(), catalog), state, catalog);
    }

    private void apply(ContractsView.Offer offer, PlayerSnapshot state, ContentCatalog catalog) {
        List<Commercial> programs = Contracts.qualifyingPrograms(state, offer.listing().lineId(), catalog);
        if (state.getContractApplication() != null) {
            String from = Contracts.listing(state, state.getContractApplication().listingId())
                .map(listing -> ContractsView.offer(state, listing, catalog).title()).orElse("your other listing");
            Table card = dialogCard("Confirm", "Move your application from " + from + " to " + offer.title() + "?");
            card.add(ui.paragraph("You can apply for one listing a turn. Your application for " + from
                + " is withdrawn.", Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
            Table buttons = new Table();
            buttons.add(actionButton("Keep " + from, Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
            buttons.add(actionButton("Move it", Theme.ButtonKind.PRIMARY, () -> {
                closeDialog();
                pickProgram(offer, programs, state, catalog);
            }));
            card.add(buttons).right().fill(false, false);
            showDialog(card);
            return;
        }
        pickProgram(offer, programs, state, catalog);
    }

    private void pickProgram(ContractsView.Offer offer, List<Commercial> programs, PlayerSnapshot state,
            ContentCatalog catalog) {
        if (programs.size() < 2) {
            confirm(offer, programs.isEmpty() ? offer.commercial() : programs.getFirst(), state, catalog);
            return;
        }
        Table card = dialogCard("Apply · " + offer.title(), "Which program flies it?");
        card.add(ui.paragraph("You qualify through " + programs.size() + " programs of the line. Each flies its own "
            + "contract flight, at its own launch cost.", Theme.TextStyle.BODY, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_3).row();
        for (Commercial commercial : programs) {
            long cost = Contracts.launchCost(state, commercial.contractConfigurationId(), catalog);
            Table choice = new Table();
            choice.setBackground(ui.optionCard(false));
            choice.pad(10f, 12f, 10f, 12f);
            Table words = new Table();
            words.defaults().left();
            words.add(ui.label(ContentNames.of(commercial.contractConfigurationId()), Theme.TextStyle.BODY_STRONG,
                Theme.INK)).row();
            words.add(ui.label("Launch cost " + ContractsView.money(cost) + " · fees "
                + ContractsView.money(Contracts.fee(cost, 1)) + " / " + ContractsView.money(Contracts.fee(cost, 2))
                + " / " + ContractsView.money(Contracts.fee(cost, 3)) + " · score "
                + Contracts.score(state, commercial.programId(), catalog), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            choice.add(words).expandX().left();
            choice.add(actionButton("Next: the terms", Theme.ButtonKind.SECONDARY, () -> {
                closeDialog();
                confirm(offer, commercial, state, catalog);
            })).right();
            card.add(choice).padBottom(Theme.SPACE_2).row();
        }
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog));
        card.add(buttons).right().fill(false, false).padTop(Theme.SPACE_2);
        showDialog(card);
    }

    private void confirm(ContractsView.Offer offer, Commercial commercial, PlayerSnapshot state,
            ContentCatalog catalog) {
        long cost = Contracts.launchCost(state, commercial.contractConfigurationId(), catalog);
        Table card = dialogCard("Apply · " + offer.client(), "Apply for " + offer.title() + "?");
        Table terms = new Table();
        terms.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.04f), Theme.NAVY_BORDER,
            Theme.BORDER_THIN));
        terms.pad(10f, 12f, 10f, 12f);
        terms.defaults().growX().left().padBottom(6f);
        terms.add(bullet("Fee by tier: " + ContractsView.money(Contracts.fee(cost, 1)) + " / "
            + ContractsView.money(Contracts.fee(cost, 2)) + " / " + ContractsView.money(Contracts.fee(cost, 3))
            + ", over a launch cost now of " + ContractsView.money(cost) + ".")).row();
        terms.add(bullet("Tiers go to the best record; a fourth applicant gets nothing. Your score now: "
            + Contracts.score(state, commercial.programId(), catalog) + ".")).row();
        terms.add(bullet("Flies: " + ContentNames.of(commercial.contractConfigurationId()) + ". Pays money only: no "
            + "prestige, not a repeat.")).row();
        terms.add(bullet("If the flight fails, the insurer refunds the launch cost and you lose the prestige that "
            + "failure costs.")).row();
        terms.add(bullet("Launch within " + Contracts.LAUNCH_WINDOW_TURNS + " seasons or it lapses: −"
            + Contracts.LAPSE_PRESTIGE + " prestige, and it counts as a failure.")).padBottom(0f).row();
        card.add(terms).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        buttons.add(actionButton("Apply", Theme.ButtonKind.PRIMARY, () -> submit(
            new ApplyContractAction(offer.listing().id(), commercial.programId()), result -> {
                closeDialog();
                toast("Applied for " + offer.title() + ". It's decided when the turn resolves.",
                    Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card, 560f);
    }
}
