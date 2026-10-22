package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.FrontPageView;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.content.FrontPageReactions;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.Theories;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.rules.ReactionPrinter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A front page for one of the great Firsts ({@code ui-spec.md} &sect;4.18): the top half of a
 * broadsheet over the dimmed hub, with the game line and the way on in a strip under it. Shows the
 * unseen pages one after another on login (before the turn report) and when a turn resolves while
 * the player is in the game; reopened alone from the Museum and the turn report.
 *
 * <p>The page's layout follows the Photocraft mockup ({@code assets-raw/frontpage/mockup/}) at
 * 1920 x 1080: a 1440 x 900 page with a 40 px margin, a three-column lower half (the photo, then
 * two text columns of 336 px).</p>
 *
 * <p>A follow-up, "The world reacts" (&sect;4.18.1), has four text columns of 322 px instead, the photo
 * over the first, the race box at the foot of the last (the canvas "Launch Window - The World Reacts",
 * 2026-10-22). Its article runs on from column to column, a paragraph split where a column ends; what
 * still doesn't fit once the closing paragraphs are dropped runs on to a continuation sheet ("Continued
 * on page 2"), so nothing is ever cut. On a rival's First the strip offers a statement (component 28).</p>
 *
 * @author Mario Koehler
 */
public class FrontPageScreen extends SessionScreen {

    /** The page's size and margin. */
    private static final float PAGE_WIDTH = 1440f;
    private static final float PAGE_HEIGHT = 900f;
    private static final float MARGIN = 40f;
    private static final float INNER = PAGE_WIDTH - 2 * MARGIN;
    /** The lower half's columns: the photo, then two text columns, a rule and gutters between. */
    private static final float PHOTO_WIDTH = 640f;
    private static final float PHOTO_HEIGHT = 440f;
    private static final float COLUMN = 336f;
    private static final float GUTTER = (INNER - PHOTO_WIDTH - 2 * COLUMN - 2f) / 4f;
    /** The top band's height and its distance from the page's top edge. */
    private static final float BAND_HEIGHT = 108f;
    private static final float BAND_TOP = 46f;
    private static final float FOLIO_HEIGHT = 20f;
    /** The space above the lower half. */
    private static final float LOWER_TOP = 15f;
    /** Everything above the lower half but the headline and the deck: the band, rules and folio. */
    private static final float TOP_FIXED = BAND_TOP + BAND_HEIGHT + 14f + 4f + 3f + 1f + 6f + FOLIO_HEIGHT + 4f + 1f
        + 6f + 2f + 10f + 1f + LOWER_TOP;
    /** What the column heads and foot boxes take from the two text columns. */
    private static final float BYLINE_HEIGHT = 24f;
    private static final float BRIEF_HEIGHT = 84f;
    private static final float RACE_CHROME = 52f;
    private static final float RACE_GAP = 4f;
    private static final float RACE_TEXT = COLUMN - 24f;
    private static final float PARAGRAPH_GAP = 4f;
    /** A paragraph's first-line indent, as spaces (the fonts have no em space). */
    private static final String INDENT_TEXT = "    ";
    /** A follow-up's four text columns and the photo over the first. */
    private static final float R_COLUMN = 322f;
    private static final float R_GUTTER = (INNER - 4 * R_COLUMN - 3f) / 6f;
    private static final float R_PHOTO_HEIGHT = 221f;
    /** The short rule above a filler item, and the space either side of it. */
    private static final float FILLER_RULE = 64f;
    private static final float FILLER_GAP = 8f;
    /** The continuation sheet's running head. */
    private static final float RUNNING_HEAD = 46f;

    private final List<FrontPage> pages;
    private final Runnable done;
    private final boolean reopened;
    private int index;
    /** 0 for the page itself, 1 for its continuation sheet. */
    private int sheet;
    /** What a follow-up's page couldn't hold, for its continuation sheet. */
    private List<Piece> carried = List.of();
    private int fontGeneration;

    /**
     * Shows pages one after another.
     *
     * @param game     the game, in a session
     * @param pages    the pages, oldest first; at least one
     * @param done     what happens after the last one (switches the screen)
     * @param reopened whether the player asked for the page (the button says "Close")
     */
    public FrontPageScreen(LaunchWindowGame game, List<FrontPage> pages, Runnable done, boolean reopened) {
        super(game);
        this.pages = List.copyOf(pages);
        this.done = done;
        this.reopened = reopened;
        root.setBackground(ui.solid(Theme.NAVY));
        rebuild();
    }

    /**
     * Never refreshes on its own: the screen that showed the pages refreshes once they're done, so a
     * turn resolving meanwhile can't open more pages over these.
     */
    @Override
    protected void refresh() {
    }

    @Override
    public void render(float delta) {
        if (fontGeneration != ui.fonts().generation()) {
            // The columns and the headline's size were measured with the old metrics: lay out again.
            rebuild();
        }
        super.render(delta);
    }

    /**
     * Returns the pages this player hasn't seen on this device, oldest first: printed since they
     * joined, not yet moved on from.
     *
     * @param game the game, in a session
     * @return the pages
     */
    public static List<FrontPage> unseen(LaunchWindowGame game) {
        List<FrontPage> all = game.session().frontPages();
        if (all == null) {
            return List.of();
        }
        long joined = game.session().joinedTurn();
        List<FrontPage> unseen = new ArrayList<>();
        for (FrontPage page : all) {
            if (page.gameTurn() < joined) {
                continue;
            }
            // A science page shows only for a theory the public followed (ui-spec.md 4.18.2).
            boolean words = page.science() ? Theories.standard().theory(page.storyId())
                .map(t -> t.reach() == Theories.Reach.PUBLIC).orElse(false)
                : FrontPages.standard().first(page.goalId()).isPresent();
            if (!game.frontPageSeen(page.id()) && words) {
                unseen.add(page);
            }
        }
        return unseen;
    }

    /**
     * Returns the page that tells a First, its own or folded in.
     *
     * @param game   the game, in a session
     * @param goalId the First's goal
     * @return the page, if one was printed and fetched
     */
    public static Optional<FrontPage> pageFor(LaunchWindowGame game, String goalId) {
        List<FrontPage> all = game.session().frontPages();
        return all == null ? Optional.empty() : all.stream().filter(p -> p.tells(goalId)).findFirst();
    }

    @Override
    protected void rebuild() {
        fontGeneration = ui.fonts().generation();
        root.clearChildren();
        FrontPage page = pages.get(index);
        Image art = new Image();
        art.setScaling(Scaling.fill);
        Drawable backdrop = ui.backdrop(Hub.artPath(session.state().getArchetype(),
            session.gameInfo().response().date().season(), game.hubClock().current()));
        art.setDrawable(backdrop == null ? ui.blueprintGrid() : backdrop);
        Image scrim = new Image(ui.solid(Theme.SCRIM_DEEP));
        scrim.setScaling(Scaling.stretch);

        Table front = new Table();
        front.top();
        front.add(sheet == 0 ? page(page) : continuation(page)).size(PAGE_WIDTH, PAGE_HEIGHT).padTop(MARGIN).row();
        front.add(strip(page)).width(PAGE_WIDTH).padTop(28f);

        Stack stack = new Stack();
        stack.add(art);
        stack.add(scrim);
        stack.add(front);
        root.add(stack).grow();
    }

    private Table page(FrontPage page) {
        Table sheet = new Table();
        Drawable paper = ui.picture("frontpage/paper.jpg");
        sheet.setBackground(paper == null ? ui.solid(Color.valueOf("#f5f0dd")) : paper);
        sheet.top().pad(0f, MARGIN, MARGIN, MARGIN);
        carried = List.of();
        java.util.function.Predicate<String> inPack = goal -> session.catalog().goal(goal).isPresent();
        Optional<FrontPageView.Page> words = page.followUp()
            ? FrontPageView.ofReactions(page, allPages(), FrontPageReactions.standard(), FrontPages.standard(),
                inPack, ContentNames::of)
            : page.science() ? FrontPageView.ofScience(page, allPages(), Theories.standard(), FrontPages.standard(),
                inPack, ContentNames::of)
            : FrontPageView.of(page, allPages(), FrontPages.standard(), inPack, ContentNames::of);
        if (words.isEmpty()) {
            return sheet;
        }
        FrontPageView.Page view = words.get();

        // The top band: two ears and the masthead between them.
        Table band = new Table();
        band.add(ear(ui.label("LATE CITY EDITION", Theme.TextStyle.NEWS_FOLIO, Theme.INK),
            ui.label(view.weather(), Theme.TextStyle.NEWS_CAPTION, Theme.INK))).size(230f, 84f);
        Image masthead = new Image(ui.picture("frontpage/masthead.png"));
        masthead.setScaling(Scaling.fit);
        band.add(masthead).size(761f, BAND_HEIGHT).expandX();
        if (page.followUp() || page.science()) {
            band.add(ear(ui.label(page.science() ? "SCIENCE" : "THE WORLD", Theme.TextStyle.NEWS_EAR_TWO, Theme.INK),
                ui.label(page.science() ? "REPORT" : "REACTS", Theme.TextStyle.NEWS_EAR_TWO, Theme.INK)))
                .size(230f, 84f);
        } else {
            band.add(ear(ui.label("EXTRA", Theme.TextStyle.NEWS_EXTRA, Theme.INK),
                ui.label("Special edition", Theme.TextStyle.NEWS_CAPTION, Theme.INK))).size(230f, 84f);
        }
        sheet.add(band).width(INNER).height(BAND_HEIGHT).padTop(BAND_TOP).row();

        // The folio line between its rules.
        sheet.add(rule()).width(INNER).height(4f).padTop(14f).row();
        sheet.add(rule()).width(INNER).height(1f).padTop(3f).row();
        Table folio = new Table();
        folio.add(ui.label(view.folio(), Theme.TextStyle.NEWS_FOLIO, Theme.INK)).left().width(300f);
        folio.add(ui.label(view.season(), Theme.TextStyle.NEWS_FOLIO, Theme.INK)).expandX();
        Label price = ui.label("FIVE CENTS", Theme.TextStyle.NEWS_FOLIO, Theme.INK);
        price.setAlignment(Align.right);
        folio.add(price).right().width(300f);
        sheet.add(folio).width(INNER).height(FOLIO_HEIGHT).padTop(6f).row();
        sheet.add(rule()).width(INNER).height(1f).padTop(4f).row();

        Label headline = headline(view);
        float headlineHeight = lines(headline, headline.getText().toString(), INNER)
            * headline.getStyle().font.getLineHeight();
        sheet.add(headline).width(INNER).height(headlineHeight).padTop(6f).row();
        Label deck = ui.label(view.deck(), Theme.TextStyle.NEWS_DECK, Theme.INK);
        deck.setWrap(true);
        deck.setAlignment(Align.center);
        float deckHeight = lines(deck, view.deck(), INNER) * deck.getStyle().font.getLineHeight();
        sheet.add(deck).width(INNER).height(deckHeight).padTop(2f).row();
        sheet.add(rule()).width(INNER).height(1f).padTop(10f).row();

        float lower = PAGE_HEIGHT - MARGIN - TOP_FIXED - headlineHeight - deckHeight;
        sheet.add(page.firstPage() ? lower(view, lower) : lowerReactions(page, view, lower)).width(INNER).height(lower)
            .padTop(LOWER_TOP).row();
        return sheet;
    }

    /**
     * A follow-up's lower half: four text columns, the photo and its caption over the first, the race box at
     * the foot of the last. The closing paragraphs go first when the article is long; what still doesn't fit
     * is carried to the continuation sheet, with "Continued on page 2" at the foot of the last column. A short
     * article leaves room: short local items fill it, as a paper fills its columns.
     */
    private Table lowerReactions(FrontPage page, FrontPageView.Page view, float height) {
        BitmapFont body = ui.fonts().get(Theme.TextStyle.NEWS_BODY);
        BitmapFont captionFont = ui.fonts().get(Theme.TextStyle.NEWS_CAPTION);
        // A science page has no photo: its first column starts with the byline.
        boolean photo = view.photo() != null;
        float captionHeight = photo ? lines(captionFont, view.caption(), R_COLUMN, Align.left)
            * captionFont.getLineHeight() : 0f;
        float raceHeight = raceHeight(view, R_COLUMN - 24f);
        float jump = captionFont.getLineHeight() + 6f;
        float[] capacities = {height - (photo ? R_PHOTO_HEIGHT + 6f + captionHeight + 8f : 0f) - BYLINE_HEIGHT, height,
            height, height - raceHeight - 12f};
        List<String> paragraphs = new ArrayList<>(view.paragraphs());
        int droppable = view.droppable();
        Flow flow = flow(pieces(paragraphs), capacities, body);
        while (!flow.rest().isEmpty() && droppable > 0) {
            paragraphs.remove(paragraphs.size() - 1);
            droppable--;
            flow = flow(pieces(paragraphs), capacities, body);
        }
        if (!flow.rest().isEmpty()) {
            capacities[3] -= jump;
            flow = flow(pieces(paragraphs), capacities, body);
            carried = flow.rest();
        }
        List<List<FrontPageReactions.Filler>> fillers = fillers(page, flow, capacities, body);

        Table lower = new Table();
        lower.top().left();
        for (int c = 0; c < 4; c++) {
            if (c > 0) {
                lower.add(rule()).width(1f).height(height).padLeft(R_GUTTER).padRight(R_GUTTER);
            }
            Table column = new Table();
            column.top().left();
            if (c == 0) {
                if (photo) {
                    column.add(new Image(cropped(view.photo(), R_COLUMN / R_PHOTO_HEIGHT)))
                        .size(R_COLUMN, R_PHOTO_HEIGHT).row();
                    column.add(ui.paragraph(view.caption(), Theme.TextStyle.NEWS_CAPTION, Theme.INK)).width(R_COLUMN)
                        .padTop(6f).padBottom(8f).left().row();
                }
                column.add(ui.label(page.science() ? "FROM OUR SCIENCE CORRESPONDENT" : "FROM OUR SPACE CORRESPONDENT",
                    Theme.TextStyle.NEWS_BYLINE, Theme.INK)).left().height(BYLINE_HEIGHT - 6f).padBottom(6f).row();
            }
            for (Piece piece : flow.columns().get(c)) {
                column.add(pieceLabel(piece)).width(R_COLUMN).padBottom(PARAGRAPH_GAP).left().row();
            }
            boolean above = !flow.columns().get(c).isEmpty() || c == 0;
            for (FrontPageReactions.Filler filler : fillers.get(c)) {
                if (above) {
                    column.add(rule()).width(FILLER_RULE).height(1f).padTop(FILLER_GAP).padBottom(FILLER_GAP).row();
                }
                column.add(filler(filler)).width(R_COLUMN).row();
                above = true;
            }
            column.add().expandY().row();
            if (c == 3) {
                if (!carried.isEmpty()) {
                    Label more = ui.label("Continued on page 2", Theme.TextStyle.NEWS_CAPTION, Theme.INK);
                    more.setAlignment(Align.right);
                    column.add(more).width(R_COLUMN).padBottom(6f).row();
                }
                column.add(race(view, R_COLUMN)).width(R_COLUMN).height(raceHeight).bottom();
            }
            lower.add(column).width(R_COLUMN).height(height).top();
        }
        return lower;
    }

    /**
     * The continuation sheet of a follow-up whose article didn't fit its page: a running head, then the
     * rest of the article in the same four columns.
     */
    private Table continuation(FrontPage page) {
        // Lays the page out again, so what's carried matches the current fonts.
        page(page);
        Table sheet = new Table();
        Drawable paper = ui.picture("frontpage/paper.jpg");
        sheet.setBackground(paper == null ? ui.solid(Color.valueOf("#f5f0dd")) : paper);
        sheet.top().pad(MARGIN);
        sheet.add(rule()).width(INNER).height(4f).row();
        Table head = new Table();
        head.add(ui.label("THE WORLD REACTS", Theme.TextStyle.NEWS_LABEL, Theme.INK)).left().width(300f);
        head.add(ui.label("CONTINUED FROM PAGE 1", Theme.TextStyle.NEWS_FOLIO, Theme.INK)).expandX();
        Label season = ui.label(seasonOf(page), Theme.TextStyle.NEWS_FOLIO, Theme.INK);
        season.setAlignment(Align.right);
        head.add(season).right().width(300f);
        sheet.add(head).width(INNER).height(RUNNING_HEAD - 10f).padTop(3f).row();
        sheet.add(rule()).width(INNER).height(1f).padBottom(14f).row();
        float height = PAGE_HEIGHT - 2 * MARGIN - RUNNING_HEAD - 4f - 1f - 14f;
        Flow flow = flow(new ArrayDeque<>(carried), new float[] {height, height, height, height},
            ui.fonts().get(Theme.TextStyle.NEWS_BODY));
        Table columns = new Table();
        columns.top().left();
        for (int c = 0; c < 4; c++) {
            if (c > 0) {
                columns.add(rule()).width(1f).height(height).padLeft(R_GUTTER).padRight(R_GUTTER);
            }
            Table column = new Table();
            column.top().left();
            for (Piece piece : flow.columns().get(c)) {
                column.add(pieceLabel(piece)).width(R_COLUMN).padBottom(PARAGRAPH_GAP).left().row();
            }
            column.add().expandY();
            columns.add(column).width(R_COLUMN).height(height).top();
        }
        sheet.add(columns).width(INNER).height(height).row();
        return sheet;
    }

    /**
     * Picks the short items for a follow-up's spare room: in an order drawn from the page's id, so every player
     * reads the same page, each item at most once, into the column the article ends in and every one after it.
     */
    private List<List<FrontPageReactions.Filler>> fillers(FrontPage page, Flow flow, float[] capacities,
                                                          BitmapFont body) {
        List<List<FrontPageReactions.Filler>> fills = new ArrayList<>();
        for (int c = 0; c < capacities.length; c++) {
            fills.add(new ArrayList<>());
        }
        if (!carried.isEmpty()) {
            return fills;
        }
        List<FrontPageReactions.Filler> order = new ArrayList<>(FrontPageReactions.standard().fillers());
        java.util.Collections.shuffle(order, new java.util.Random(page.id().hashCode()));
        int last = 0;
        for (int c = 0; c < capacities.length; c++) {
            if (!flow.columns().get(c).isEmpty()) {
                last = c;
            }
        }
        for (int c = last; c < capacities.length; c++) {
            float room = capacities[c];
            for (Piece piece : flow.columns().get(c)) {
                room -= pieceHeight(piece, body);
            }
            boolean above = !flow.columns().get(c).isEmpty() || c == 0;
            for (java.util.Iterator<FrontPageReactions.Filler> it = order.iterator(); it.hasNext(); ) {
                FrontPageReactions.Filler filler = it.next();
                float needed = fillerHeight(filler, body) + (above ? 2 * FILLER_GAP + 1f : 0f);
                if (needed <= room) {
                    fills.get(c).add(filler);
                    it.remove();
                    room -= needed;
                    above = true;
                }
            }
        }
        return fills;
    }

    private Table filler(FrontPageReactions.Filler filler) {
        Table item = new Table();
        item.top().left();
        Label head = ui.label(filler.headline().toUpperCase(java.util.Locale.ROOT), Theme.TextStyle.NEWS_LABEL,
            Theme.INK);
        head.setWrap(true);
        head.setAlignment(Align.center);
        item.add(head).width(R_COLUMN).padBottom(4f).row();
        for (String paragraph : filler.paragraphs()) {
            item.add(pieceLabel(new Piece(paragraph, true))).width(R_COLUMN).padBottom(PARAGRAPH_GAP).left().row();
        }
        return item;
    }

    private float fillerHeight(FrontPageReactions.Filler filler, BitmapFont body) {
        BitmapFont label = ui.fonts().get(Theme.TextStyle.NEWS_LABEL);
        float height = lines(label, filler.headline().toUpperCase(java.util.Locale.ROOT), R_COLUMN, Align.center)
            * label.getLineHeight() + 4f;
        for (String paragraph : filler.paragraphs()) {
            height += pieceHeight(new Piece(paragraph, true), body);
        }
        return height;
    }

    private static String seasonOf(FrontPage page) {
        return de.mkoehler.launchwindow.model.GameDate.ofTurn(page.gameTurn()).label().toUpperCase(java.util.Locale.ROOT);
    }

    /** A part of a paragraph: all of it, or what a column holds of it; only a paragraph's start is indented. */
    private record Piece(String text, boolean indent) {
    }

    /** Pieces per column, and what no column held. */
    private record Flow(List<List<Piece>> columns, List<Piece> rest) {
    }

    private static Deque<Piece> pieces(List<String> paragraphs) {
        Deque<Piece> pieces = new ArrayDeque<>();
        paragraphs.forEach(p -> pieces.add(new Piece(p, true)));
        return pieces;
    }

    /**
     * Runs pieces through columns of the given heights, splitting a paragraph between words where a column
     * ends (at least two lines stay behind, so no column ends on a lone line).
     */
    private Flow flow(Deque<Piece> queue, float[] capacities, BitmapFont font) {
        float lineHeight = font.getLineHeight();
        List<List<Piece>> columns = new ArrayList<>();
        for (float capacity : capacities) {
            List<Piece> column = new ArrayList<>();
            float used = 0f;
            while (!queue.isEmpty()) {
                Piece piece = queue.peekFirst();
                float needed = pieceHeight(piece, font);
                if (used + needed <= capacity) {
                    column.add(queue.pollFirst());
                    used += needed;
                    continue;
                }
                int room = (int) Math.floor((capacity - used - PARAGRAPH_GAP) / lineHeight);
                Piece[] split = room >= 2 ? split(piece, room, font) : null;
                if (split != null) {
                    queue.pollFirst();
                    column.add(split[0]);
                    queue.addFirst(split[1]);
                }
                break;
            }
            columns.add(column);
        }
        return new Flow(columns, new ArrayList<>(queue));
    }

    /** Splits a piece after as many words as fill {@code lines} lines; null if not even one line's worth fits. */
    private Piece[] split(Piece piece, int lines, BitmapFont font) {
        String[] words = piece.text().split(" ");
        int low = 0;
        int high = words.length - 1;
        while (low < high) {
            int mid = (low + high + 1) / 2;
            String head = String.join(" ", java.util.Arrays.copyOfRange(words, 0, mid));
            if (lines(font, (piece.indent() ? INDENT_TEXT : "") + head, R_COLUMN, Align.left) <= lines) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        if (low == 0) {
            return null;
        }
        return new Piece[] {new Piece(String.join(" ", java.util.Arrays.copyOfRange(words, 0, low)), piece.indent()),
            new Piece(String.join(" ", java.util.Arrays.copyOfRange(words, low, words.length)), false)};
    }

    private float pieceHeight(Piece piece, BitmapFont font) {
        return lines(font, (piece.indent() ? INDENT_TEXT : "") + piece.text(), R_COLUMN, Align.left)
            * font.getLineHeight() + PARAGRAPH_GAP;
    }

    private Label pieceLabel(Piece piece) {
        return ui.paragraph((piece.indent() ? INDENT_TEXT : "") + piece.text(), Theme.TextStyle.NEWS_BODY, Theme.INK);
    }

    private Table ear(Label top, Label bottom) {
        Table ear = new Table();
        ear.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.INK, 1f));
        ear.add(top).row();
        ear.add(bottom).padTop(4f);
        return ear;
    }

    private Image rule() {
        Image rule = new Image(ui.solid(Theme.INK));
        rule.setScaling(Scaling.stretch);
        return rule;
    }

    /**
     * The headline: one line at 80 px, else one line at 64, else two lines at 64; the short
     * headline the same way if the long one needs three.
     */
    private Label headline(FrontPageView.Page view) {
        for (String text : List.of(view.headline(), view.shortHeadline())) {
            for (Theme.TextStyle style : List.of(Theme.TextStyle.NEWS_HEADLINE, Theme.TextStyle.NEWS_HEADLINE_SM)) {
                Label label = headlineLabel(text, style);
                if (lines(label, text, INNER) == 1) {
                    return label;
                }
            }
            Label two = headlineLabel(text, Theme.TextStyle.NEWS_HEADLINE_SM);
            if (lines(two, text, INNER) <= 2) {
                return two;
            }
        }
        return headlineLabel(view.shortHeadline(), Theme.TextStyle.NEWS_HEADLINE_SM);
    }

    private Label headlineLabel(String text, Theme.TextStyle style) {
        Label label = ui.label(text, style, Theme.INK);
        label.setAlignment(Align.center);
        label.setWrap(true);
        return label;
    }

    private static int lines(Label label, String text, float width) {
        return lines(label.getStyle().font, text, width, Align.center);
    }

    private static int lines(BitmapFont font, String text, float width, int align) {
        GlyphLayout layout = new GlyphLayout(font, text, Color.WHITE, width, align, true);
        Set<Float> rows = new HashSet<>();
        for (GlyphLayout.GlyphRun run : layout.runs) {
            rows.add(run.y);
        }
        return Math.max(1, rows.size());
    }

    private Table lower(FrontPageView.Page view, float height) {
        Table lower = new Table();
        lower.top().left();

        // The photo keeps its column's width and gives up height, cropped about its middle.
        Table photoColumn = new Table();
        photoColumn.top().left();
        Label caption = ui.paragraph(view.caption(), Theme.TextStyle.NEWS_CAPTION, Theme.INK);
        BitmapFont captionFont = caption.getStyle().font;
        float captionHeight = lines(captionFont, view.caption(), PHOTO_WIDTH, Align.left) * captionFont.getLineHeight();
        float photoHeight = Math.min(PHOTO_HEIGHT, height - captionHeight - 8f);
        photoColumn.add(new Image(cropped(view.photo(), PHOTO_WIDTH / photoHeight))).size(PHOTO_WIDTH, photoHeight)
            .row();
        photoColumn.add(caption).width(PHOTO_WIDTH).padTop(8f).left();
        lower.add(photoColumn).width(PHOTO_WIDTH).height(height).top();

        // Fill the first column, then the second, paragraphs whole; drop closing paragraphs from the
        // end while they don't fit.
        BitmapFont small = ui.fonts().get(Theme.TextStyle.NEWS_SMALL);
        BitmapFont italic = ui.fonts().get(Theme.TextStyle.NEWS_CAPTION);
        float raceHeight = raceHeight(view, RACE_TEXT);
        float capacityA = height - BYLINE_HEIGHT - BRIEF_HEIGHT;
        float capacityB = height - raceHeight - 12f;
        List<String> paragraphs = new ArrayList<>(view.paragraphs());
        int droppable = view.droppable();
        List<String> columnA = new ArrayList<>();
        List<String> columnB = new ArrayList<>();
        while (true) {
            columnA.clear();
            columnB.clear();
            float usedA = 0f;
            float usedB = 0f;
            for (String paragraph : paragraphs) {
                float needed = paragraphHeight(paragraph);
                if (columnB.isEmpty() && usedA + needed <= capacityA) {
                    columnA.add(paragraph);
                    usedA += needed;
                } else {
                    columnB.add(paragraph);
                    usedB += needed;
                }
            }
            if (usedB <= capacityB || droppable == 0) {
                break;
            }
            paragraphs.remove(paragraphs.size() - 1);
            droppable--;
        }

        lower.add(rule()).width(1f).height(height).padLeft(GUTTER).padRight(GUTTER);
        Table a = new Table();
        a.top().left();
        a.add(ui.label("FROM OUR SPACE CORRESPONDENT", Theme.TextStyle.NEWS_BYLINE, Theme.INK)).left()
            .height(BYLINE_HEIGHT - 6f).padBottom(6f).row();
        for (String paragraph : columnA) {
            a.add(paragraphLabel(paragraph)).width(COLUMN).padBottom(PARAGRAPH_GAP).left().row();
        }
        a.add().expandY().row();
        a.add(brief(view)).width(COLUMN).bottom();
        lower.add(a).width(COLUMN).height(height).top();

        lower.add(rule()).width(1f).height(height).padLeft(GUTTER).padRight(GUTTER);
        Table b = new Table();
        b.top().left();
        for (String paragraph : columnB) {
            b.add(paragraphLabel(paragraph)).width(COLUMN).padBottom(PARAGRAPH_GAP).left().row();
        }
        b.add().expandY().row();
        b.add(race(view, COLUMN)).width(COLUMN).height(raceHeight).bottom();
        lower.add(b).width(COLUMN).height(height).top();
        return lower;
    }

    /** A picture cropped about its middle to an aspect (width over height). */
    private Drawable cropped(String path, float aspect) {
        TextureRegionDrawable picture = ui.picture(path);
        if (picture == null) {
            return ui.solid(Theme.INK_MUTED);
        }
        TextureRegion region = new TextureRegion(picture.getRegion());
        int width = region.getRegionWidth();
        int height = region.getRegionHeight();
        if ((float) width / height < aspect) {
            int kept = Math.round(width / aspect);
            region.setRegion(region.getRegionX(), region.getRegionY() + (height - kept) / 2, width, kept);
        } else {
            int kept = Math.round(height * aspect);
            region.setRegion(region.getRegionX() + (width - kept) / 2, region.getRegionY(), kept, height);
        }
        return new TextureRegionDrawable(region);
    }

    private Label paragraphLabel(String text) {
        return ui.paragraph(INDENT_TEXT + text, Theme.TextStyle.NEWS_BODY, Theme.INK);
    }

    private float paragraphHeight(String text) {
        BitmapFont font = ui.fonts().get(Theme.TextStyle.NEWS_BODY);
        return lines(font, INDENT_TEXT + text, COLUMN, Align.left) * font.getLineHeight() + PARAGRAPH_GAP;
    }

    private Table brief(FrontPageView.Page view) {
        Table brief = new Table();
        brief.top().left();
        brief.add(rule()).width(COLUMN).height(1f).row();
        brief.add(ui.label("IN BRIEF", Theme.TextStyle.NEWS_LABEL, Theme.INK)).padTop(6f).row();
        for (String line : view.brief()) {
            brief.add(ui.label(line, Theme.TextStyle.NEWS_CAPTION, Theme.INK)).left().padTop(3f).row();
        }
        return brief;
    }

    /** The race box's height for a text width. */
    private float raceHeight(FrontPageView.Page view, float textWidth) {
        BitmapFont small = ui.fonts().get(Theme.TextStyle.NEWS_SMALL);
        BitmapFont italic = ui.fonts().get(Theme.TextStyle.NEWS_CAPTION);
        float raceHeight = RACE_CHROME;
        for (FrontPageView.RaceRow row : view.race()) {
            raceHeight += oneLine(row, textWidth) ? Math.max(small.getLineHeight(), italic.getLineHeight()) + RACE_GAP
                : small.getLineHeight() + lines(italic, row.who(), textWidth, Align.right) * italic.getLineHeight()
                    + RACE_GAP;
        }
        return raceHeight;
    }

    private Table race(FrontPageView.Page view, float width) {
        float textWidth = width - 24f;
        Table race = new Table();
        race.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.INK, 1f));
        race.top().pad(8f, 12f, 8f, 12f);
        race.add(ui.label("THE RACE SO FAR", Theme.TextStyle.NEWS_LABEL, Theme.INK)).padBottom(6f).row();
        for (FrontPageView.RaceRow row : view.race()) {
            Label who = ui.label(row.who(), Theme.TextStyle.NEWS_CAPTION, row.done() ? Theme.INK : Theme.INK_MUTED);
            who.setAlignment(Align.right);
            Table line = new Table();
            line.add(ui.label(row.first(), Theme.TextStyle.NEWS_SMALL, Theme.INK)).left().expandX();
            if (oneLine(row, textWidth)) {
                line.add(who).right();
            } else {
                who.setWrap(true);
                line.row();
                line.add(who).width(textWidth).right();
            }
            race.add(line).width(textWidth).padBottom(RACE_GAP).row();
        }
        return race;
    }

    /** Whether a race row's First and who-and-when fit side by side. */
    private boolean oneLine(FrontPageView.RaceRow row, float textWidth) {
        GlyphLayout first = new GlyphLayout(ui.fonts().get(Theme.TextStyle.NEWS_SMALL), row.first());
        GlyphLayout who = new GlyphLayout(ui.fonts().get(Theme.TextStyle.NEWS_CAPTION), row.who());
        return first.width + who.width + 16f <= textWidth;
    }

    private Table strip(FrontPage page) {
        Table strip = new Table();
        String line = page.followUp() ? "Statements only: no effect on the race."
            : page.science() ? "Science news: no effect on the race."
            : FrontPageView.gameLine(page, session.state().getAgencyName(), session.catalog(), ContentNames::of);
        String history = page.followUp() ? FrontPageReactions.standard().historyNotes().followUp()
            : page.science() ? Theories.standard().theory(page.storyId()).map(Theories.Theory::historyNote).orElse("")
            : "";
        if (!history.isBlank()) {
            strip.add(ui.label(line, Theme.TextStyle.BODY, Theme.ON_PAGE)).left();
            HistoryNote note = new HistoryNote(ui, history);
            note.setColor(Theme.ON_PAGE);
            strip.add(note).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_2).left().expandX();
        } else {
            strip.add(ui.label(line, Theme.TextStyle.BODY, Theme.ON_PAGE)).left().expandX();
        }
        if (pages.size() > 1) {
            strip.add(ui.label((index + 1) + " of " + pages.size(), Theme.TextStyle.BODY_SM, Theme.ON_PAGE_MUTED))
                .padRight(Theme.SPACE_4);
        }
        statementButton(page).ifPresent(extra -> strip.add(extra).padRight(Theme.SPACE_4));
        String next = sheet == 0 && !carried.isEmpty() ? "Turn to page 2"
            : reopened ? "Close" : index + 1 < pages.size() ? "Next front page" : "Continue";
        TextButton button = ui.pageButton(next);
        button.addListener(clicked(this::next));
        strip.add(button).right();
        return strip;
    }

    /**
     * Shows the current page's continuation sheet, if its article ran on to one, as the strip's "Turn to
     * page 2" does ({@code ScreenSnapshot}).
     */
    public void showContinuation() {
        if (sheet == 0 && !carried.isEmpty()) {
            next();
        }
    }

    /**
     * The strip's second button on a First's page (component 28): "Give a statement" or "Your statement:
     * Polite · Change" while this player may answer it, or "Read the reactions" once its follow-up is printed.
     */
    private Optional<TextButton> statementButton(FrontPage page) {
        if (!page.firstPage() || sheet > 0) {
            return Optional.empty();
        }
        Optional<FrontPage> followUp = allPages().stream()
            .filter(p -> p.followUp() && page.id().equals(p.reactsTo())).findFirst();
        if (followUp.isPresent()) {
            if (!reopened) {
                return Optional.empty();
            }
            TextButton read = ui.pageGhostButton("Read the reactions");
            read.addListener(clicked(() -> game.visit(new FrontPageScreen(game, List.of(followUp.get()),
                () -> game.setScreen(this), true))));
            return Optional.of(read);
        }
        if (!ReactionPrinter.mayReact(page, Math.toIntExact(session.openTurn()), session.state().getAgencyName(),
                Math.toIntExact(session.joinedTurn()))) {
            return Optional.empty();
        }
        Optional<FrontPage.Statement> given = session.statement(page.id());
        TextButton button = ui.pageGhostButton(given.map(s -> "Your statement: "
            + FrontPageView.stanceName(s.stance()) + " · Change").orElse("Give a statement"));
        if (given.isPresent()) {
            Image check = new Image(ui.icon("check"), Scaling.fit);
            check.setColor(Theme.ON_PAGE_STRONG);
            Label label = button.getLabel();
            button.clearChildren();
            button.add(check).size(13f).padRight(6f);
            button.add(label);
        }
        button.addListener(clicked(() -> new StatementDialog(this, page, this::rebuild).show()));
        return Optional.of(button);
    }

    private void next() {
        if (sheet == 0 && !carried.isEmpty()) {
            sheet = 1;
            rebuild();
            return;
        }
        sheet = 0;
        if (!reopened) {
            game.markFrontPageSeen(pages.get(index).id());
        }
        if (index + 1 < pages.size()) {
            index++;
            rebuild();
            return;
        }
        done.run();
    }

    private List<FrontPage> allPages() {
        List<FrontPage> all = session.frontPages();
        return all == null ? pages : all;
    }
}
