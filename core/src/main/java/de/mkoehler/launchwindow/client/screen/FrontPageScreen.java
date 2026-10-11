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
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.FrontPageView;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.model.FrontPage;

import java.util.ArrayList;
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

    private final List<FrontPage> pages;
    private final Runnable done;
    private final boolean reopened;
    private int index;

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
            if (!game.frontPageSeen(page.id()) && FrontPages.standard().first(page.goalId()).isPresent()) {
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
        front.add(page(page)).size(PAGE_WIDTH, PAGE_HEIGHT).padTop(MARGIN).row();
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
        Optional<FrontPageView.Page> words = FrontPageView.of(page, allPages(), FrontPages.standard(),
            goal -> session.catalog().goal(goal).isPresent(), ContentNames::of);
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
        band.add(ear(ui.label("EXTRA", Theme.TextStyle.NEWS_EXTRA, Theme.INK),
            ui.label("Special edition", Theme.TextStyle.NEWS_CAPTION, Theme.INK))).size(230f, 84f);
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
        sheet.add(lower(view, lower)).width(INNER).height(lower).padTop(LOWER_TOP).row();
        return sheet;
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
        float raceHeight = RACE_CHROME;
        for (FrontPageView.RaceRow row : view.race()) {
            raceHeight += oneLine(row) ? Math.max(small.getLineHeight(), italic.getLineHeight()) + RACE_GAP
                : small.getLineHeight() + lines(italic, row.who(), RACE_TEXT, Align.right) * italic.getLineHeight()
                    + RACE_GAP;
        }
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
        b.add(race(view)).width(COLUMN).height(raceHeight).bottom();
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

    private Table race(FrontPageView.Page view) {
        Table race = new Table();
        race.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.INK, 1f));
        race.top().pad(8f, 12f, 8f, 12f);
        race.add(ui.label("THE RACE SO FAR", Theme.TextStyle.NEWS_LABEL, Theme.INK)).padBottom(6f).row();
        for (FrontPageView.RaceRow row : view.race()) {
            Label who = ui.label(row.who(), Theme.TextStyle.NEWS_CAPTION, row.done() ? Theme.INK : Theme.INK_MUTED);
            who.setAlignment(Align.right);
            Table line = new Table();
            line.add(ui.label(row.first(), Theme.TextStyle.NEWS_SMALL, Theme.INK)).left().expandX();
            if (oneLine(row)) {
                line.add(who).right();
            } else {
                who.setWrap(true);
                line.row();
                line.add(who).width(RACE_TEXT).right();
            }
            race.add(line).width(RACE_TEXT).padBottom(RACE_GAP).row();
        }
        return race;
    }

    /** Whether a race row's First and who-and-when fit side by side. */
    private boolean oneLine(FrontPageView.RaceRow row) {
        GlyphLayout first = new GlyphLayout(ui.fonts().get(Theme.TextStyle.NEWS_SMALL), row.first());
        GlyphLayout who = new GlyphLayout(ui.fonts().get(Theme.TextStyle.NEWS_CAPTION), row.who());
        return first.width + who.width + 16f <= RACE_TEXT;
    }

    private Table strip(FrontPage page) {
        Table strip = new Table();
        String line = FrontPageView.gameLine(page, session.state().getAgencyName(), session.catalog(), ContentNames::of);
        strip.add(ui.label(line, Theme.TextStyle.BODY, Theme.ON_PAGE)).left().expandX();
        if (pages.size() > 1) {
            strip.add(ui.label((index + 1) + " of " + pages.size(), Theme.TextStyle.BODY_SM, Theme.ON_PAGE_MUTED))
                .padRight(Theme.SPACE_4);
        }
        String next = reopened ? "Close" : index + 1 < pages.size() ? "Next front page" : "Continue";
        TextButton button = ui.pageButton(next);
        button.addListener(clicked(this::next));
        strip.add(button).right();
        return strip;
    }

    private void next() {
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
