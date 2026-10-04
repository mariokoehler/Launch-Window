package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The look of Launch Window in code: colors, spacing, radii, borders and type
 * styles, taken directly from the project's design system (the "restrained
 * engineering blueprint" direction, {@code design.md} &sect;3.12, and the
 * published design-system artifact it references).
 * <p>
 * Unlike a raised-paper UI, this system has no shadows and (almost) no
 * rounded corners: every surface is either sharp-cornered ({@link
 * #RADIUS_NONE}, the default for buttons/badges/cards/tables) or a genuine
 * circle ({@link #RADIUS_FULL}, progress-step dots and nothing else). That
 * made the shape-rendering half of this class much simpler than a
 * drop-shadow, rounded-box system would need: {@link Shapes#rect} is two
 * nested axis-aligned rectangles, no signed-distance-field math required;
 * only {@link Shapes#circle} (small, and the one place curves actually
 * appear) still renders with anti-aliasing.
 * <p>
 * All pixel values are in the design's virtual resolution of
 * {@value #VIEW_WIDTH} by {@value #VIEW_HEIGHT}. Lay screens out in a
 * {@code FitViewport} of that size and every number here can be used as is.
 * <b>This resolution is an unconfirmed default</b>, proposed the same session
 * as this class (no real screen has been laid out yet to confirm it against)
 * &mdash; see {@code design.md} &sect;4.
 * <p>
 * The {@link Color} constants are shared instances. Never call a mutating
 * method such as {@code set}, {@code mul} or {@code lerp} on them directly;
 * copy first with {@code new Color(Theme.NAVY)}.
 * <p>
 * Text is drawn with two typefaces, chosen per {@link TextStyle}: Archivo
 * for headers, labels, buttons and table headers, and Work Sans for
 * anything meant to be read at length &mdash; mission briefs, the
 * educational asides {@code design.md} &sect;1.1 commits to. {@link
 * Fonts#load} generates one {@link BitmapFont}
 * per {@link TextStyle} with gdx-freetype; {@link Shapes} builds the flat
 * fills, thin-bordered rectangles and progress dots.
 * <p>
 * <b>Color is semantic, not decorative</b> &mdash; this is the single rule
 * most worth holding the line on as new screens get built (see each color
 * constant's own doc): {@link #GOLD} means active/in-progress/caution, never
 * danger; {@link #SUCCESS} means a genuinely good outcome; {@link #DANGER} is
 * reserved for destructive actions and real failures. Conflating "in
 * progress" with "danger" was an actual mistake made and corrected during
 * this system's own design &mdash; don't reintroduce it.
 *
 * @author Mario Koehler
 */
public final class Theme {

    // ------------------------------------------------------------------ view

    /**
     * Width, in pixels, of the virtual screen the design is drawn for.
     * Unconfirmed default (see the class doc) &mdash; revisit once a real
     * screen layout exists to check it against.
     */
    public static final int VIEW_WIDTH = 1920;

    /**
     * Height, in pixels, of the virtual screen the design is drawn for.
     * Unconfirmed default, see {@link #VIEW_WIDTH}.
     */
    public static final int VIEW_HEIGHT = 1080;

    // ---------------------------------------------------------------- colors

    /**
     * The page frame only &mdash; header, nav and footer margins. Never
     * behind a card, table or paragraph of body text: that specific
     * separation (texture stays on the frame, content sits on {@link
     * #SURFACE_CARD}) is what made this direction actually readable.
     */
    public static final Color SURFACE_PAGE = Color.valueOf("#3d6a9a");

    /**
     * Any surface meant to be read: the mission-detail card, tables,
     * paragraph blocks. Always opaque.
     */
    public static final Color SURFACE_CARD = Color.valueOf("#ffffff");

    /**
     * The fine (20 px-pitch) drafting-grid hairline drawn on {@link
     * #SURFACE_PAGE}. White at 9% alpha.
     */
    public static final Color GRID_LINE = Color.valueOf("#ffffff17");

    /**
     * The bolder (100 px-pitch) drafting-grid line drawn on {@link
     * #SURFACE_PAGE}, on top of {@link #GRID_LINE}. White at 18% alpha.
     */
    public static final Color GRID_LINE_STRONG = Color.valueOf("#ffffff2e");

    /**
     * Body text on {@link #SURFACE_CARD}.
     */
    public static final Color INK = Color.valueOf("#1c1c1a");

    /**
     * Secondary/de-emphasized text on {@link #SURFACE_CARD} (a role column,
     * a progress-step label).
     */
    public static final Color INK_MUTED = Color.valueOf("#5b5548");

    /**
     * Default text/icon color directly on {@link #SURFACE_PAGE} (inactive
     * nav labels).
     */
    public static final Color ON_PAGE = Color.valueOf("#eaf1fb");

    /**
     * Headings and emphasized text directly on {@link #SURFACE_PAGE} (the
     * wordmark placeholder, "AGENCY: NASA").
     */
    public static final Color ON_PAGE_STRONG = Color.valueOf("#ffffff");

    /**
     * Secondary text directly on {@link #SURFACE_PAGE} (timestamps, "NEXT
     * REVIEW").
     */
    public static final Color ON_PAGE_MUTED = Color.valueOf("#e4edf9");

    /**
     * The gold/amber family used as small text directly on {@link
     * #SURFACE_PAGE} (the tagline, a reference caption) &mdash; a paler step
     * than {@link #GOLD} so it clears 4.5:1 contrast against the page
     * background. Never set {@link #GOLD} itself as text on {@link
     * #SURFACE_PAGE}.
     */
    public static final Color GOLD_ON_PAGE = Color.valueOf("#ffe9b3");

    /**
     * The green/success family used as small text directly on {@link
     * #SURFACE_PAGE} (the footer's funds-delta readout, e.g. "+2,420.0")
     * &mdash; a paler mint step than {@link #SUCCESS} so it clears 4.5:1
     * against the page background (measured ~4.7:1, matching {@link
     * #GOLD_ON_PAGE}'s own margin). <b>Never set {@link #SUCCESS} itself as
     * text on {@link #SURFACE_PAGE}</b> &mdash; it measures ~1.15:1 there,
     * essentially invisible; this was a real bug caught in the design system
     * before this constant existed (`design.md` &sect;3.12). Added
     * 2026-10-04 once the header/footer chrome mockups made this a real,
     * not speculative, need.
     */
    public static final Color SUCCESS_ON_PAGE = Color.valueOf("#c4f5e0");

    /**
     * Primary/neutral UI: headers, primary-button fill, table header-row
     * fill, borders on {@link #SURFACE_CARD}. The neutral color of the
     * interface itself, not a signal.
     */
    public static final Color NAVY = Color.valueOf("#16305c");

    /**
     * Table cell dividers and other secondary borders on {@link
     * #SURFACE_CARD}. Navy at 30% alpha.
     */
    public static final Color NAVY_BORDER = Color.valueOf("#16305c4d");

    /**
     * Fill for an active / in-progress / caution state (a status badge, the
     * current step in a sequence, a "steady" rating). <b>Never danger</b>
     * &mdash; the one status a mission actually has while it's running gets
     * this color, not {@link #DANGER}.
     */
    public static final Color GOLD = Color.valueOf("#e0a72e");

    /**
     * Text and icons on a {@link #GOLD} fill. An alias of {@link #NAVY} (the
     * design system's {@code on-gold} token is literally {@code {navy}}):
     * do not give it its own value if it ever needs tuning, retune
     * {@link #NAVY}.
     */
    public static final Color ON_GOLD = NAVY;

    /**
     * The gold/amber family used as text directly on {@link #SURFACE_CARD}
     * (an "ASCENT"-style current-step label, a "MISSION BRIEF" eyebrow)
     * &mdash; darkened for contrast. Never set {@link #GOLD} itself as text
     * on white.
     */
    public static final Color GOLD_TEXT = Color.valueOf("#8a5d12");

    /**
     * Fill for a genuinely good/high/positive outcome (a high morale rating
     * today; a successful mission result later). Deliberately distinct from
     * {@link #GOLD} &mdash; added once it became clear "active" and "good"
     * needed to be visibly different things.
     */
    public static final Color SUCCESS = Color.valueOf("#1f6b47");

    /**
     * Reserved for destructive actions (Scrub) and real failure states.
     * Never used to mean "something is currently happening" &mdash; that is
     * what {@link #GOLD} is for.
     */
    public static final Color DANGER = Color.valueOf("#c6402b");

    /**
     * Reserved for exactly one action per screen: advancing the game clock
     * (BASPM's "End Season"). A muted violet, deliberately outside the rest
     * of the palette's hue range so it reads as its own thing rather than a
     * variant of an existing status color. <b>Currently unused by any real
     * screen</b> &mdash; {@code ui-spec.md} dropped the one button that
     * would have used it, since Launch Window's async model resolves turns
     * on the server's own schedule and has no player-facing "end my turn"
     * action. Kept defined (matching {@link BadgeKind#DANGER}'s own
     * not-yet-used precedent) because the design system still reserves the
     * token and {@link ButtonKind#COMMIT} for whatever eventually needs it.
     */
    public static final Color COMMIT = Color.valueOf("#4b3a72");

    /**
     * Text and icons on a dark solid fill &mdash; {@link #NAVY}, {@link
     * #SUCCESS}, {@link #DANGER} or {@link #COMMIT}. One shared light color
     * rather than near-identical creams per fill.
     */
    public static final Color ON_FILL = Color.valueOf("#f5efe1");

    /**
     * The empty background track behind a skill/progress bar on {@link
     * #SURFACE_CARD}. Navy at 14% alpha.
     */
    public static final Color SKILL_TRACK = Color.valueOf("#16305c24");

    // --------------------------------------------------------------- spacing

    /** Tight gaps: icon-to-label, a badge's own padding. */
    public static final float SPACE_1 = 4f;

    /** Default gap between related inline elements; a button's/badge's horizontal breathing room. */
    public static final float SPACE_2 = 8f;

    /** Gap between a section's eyebrow label and its card; button padding. */
    public static final float SPACE_3 = 12f;

    /** A card's own padding; gap between stacked sections on a screen. */
    public static final float SPACE_4 = 16f;

    /** Gap between major sections of a screen. */
    public static final float SPACE_5 = 20f;

    /** Page-level outer padding/margin. */
    public static final float SPACE_6 = 30f;

    // ---------------------------------------------------------------- radii

    /**
     * The default for every surface, button and badge in this system
     * &mdash; a sharp, drafted look is the point.
     */
    public static final float RADIUS_NONE = 0f;

    /**
     * Reserved for genuinely circular indicators only: a progress-step dot,
     * the orbit-ring glyph. {@link Shapes#circle} treats any value here as
     * "half the shape's own diameter" &mdash; never used on a button, badge
     * or card.
     */
    public static final float RADIUS_FULL = 9999f;

    // --------------------------------------------------------------- borders

    /**
     * The default border weight almost everywhere &mdash; nav tabs, table
     * dividers, secondary/destructive buttons.
     */
    public static final float BORDER_THIN = 1f;

    /**
     * The one place a slightly heavier line is used: the primary card's own
     * outline, a progress-track dash, and the current-step progress dot's
     * ring. A thicker border than this was the one thing user feedback
     * rejected outright in an earlier direction &mdash; don't reintroduce
     * one.
     */
    public static final float BORDER_EMPHASIS = 1.5f;

    /**
     * The width of a Card's {@code accent-left} stripe. Distinct from
     * {@link #BORDER_EMPHASIS} on purpose: it's a color block, not a line.
     */
    public static final float BORDER_ACCENT = 3f;

    /**
     * Not meant to be instantiated: this class only holds constants and
     * factories.
     */
    private Theme() {
    }

    // ------------------------------------------------------------------ text

    /**
     * The font files, relative to the directory passed to {@link
     * Fonts#load}. The static TrueType files (Archivo and Work Sans, both
     * open-source Google Fonts under the SIL Open Font License &mdash; no
     * BASPM-content concern) live in {@code assets/fonts/}, with their
     * license texts beside them (added 2026-10-04).
     */
    public enum FontFile {
        /** Archivo SemiBold, weight 600. */
        ARCHIVO_SEMIBOLD("Archivo-SemiBold.ttf"),
        /** Archivo Bold, weight 700. */
        ARCHIVO_BOLD("Archivo-Bold.ttf"),
        /** Work Sans Regular, weight 400. */
        WORK_SANS_REGULAR("WorkSans-Regular.ttf"),
        /** Work Sans Medium, weight 500. */
        WORK_SANS_MEDIUM("WorkSans-Medium.ttf"),
        /** Work Sans SemiBold, weight 600. */
        WORK_SANS_SEMIBOLD("WorkSans-SemiBold.ttf"),
        /** Work Sans SemiBold Italic, weight 600 &mdash; the one italic style this system uses. */
        WORK_SANS_SEMIBOLD_ITALIC("WorkSans-SemiBoldItalic.ttf");

        private final String fileName;

        FontFile(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Returns the file name inside the fonts directory.
         *
         * @return the file name, such as {@code Archivo-Bold.ttf}
         */
        public String fileName() {
            return fileName;
        }
    }

    /**
     * The type scale, taken directly from the design system's {@code
     * tokens.json} (its "Display" group is {@link #HEADING_LG}, {@link
     * #HEADING_SM}, {@link #EYEBROW} and {@link #LABEL}, all set in Archivo;
     * its "Body" group is the other four, in Work Sans). The wordmark is
     * deliberately <b>not</b> part of this scale: {@code design.md} &sect;3.12
     * explains why reaching for a bigger display style was the wrong fix,
     * and a real logotype is tracked separately.
     */
    public enum TextStyle {
        /** A screen's own title: Archivo Bold 24, line-height 29. */
        HEADING_LG(FontFile.ARCHIVO_BOLD, 24, 29, 0f),
        /** A card's title (e.g. a mission name): Archivo Bold 18, line-height 23. */
        HEADING_SM(FontFile.ARCHIVO_BOLD, 18, 23, 0f),
        /**
         * A small section label above a Card (e.g. "ACTIVE MISSION"):
         * Archivo Bold 11, line-height 13, letter-spaced ~1px. Render the
         * text upper case yourself; the style carries no casing.
         */
        EYEBROW(FontFile.ARCHIVO_BOLD, 11, 13, 1f / 11f),
        /** Buttons, nav tabs, table headers, badges: Archivo SemiBold 10, line-height 12, letter-spaced 0.5px. */
        LABEL(FontFile.ARCHIVO_SEMIBOLD, 10, 12, 0.5f / 10f),
        /**
         * Paragraph copy &mdash; mission briefs, the educational asides
         * {@code design.md} &sect;1.1 commits to: Work Sans Regular 13,
         * line-height 20. This is the style the whole type system exists to
         * serve; never substitute a UI style for a real paragraph.
         */
        BODY(FontFile.WORK_SANS_REGULAR, 13, 20, 0f),
        /**
         * A bold lead-in inside body copy (the mockups' {@code <strong>}, e.g. "Versions don't
         * match." at the start of a notice): Work Sans SemiBold 13, line-height 20. Added
         * 2026-10-04 for the Connect/Join Game screens; a Scene2D {@code Label} can't switch
         * fonts mid-text, so it's set as its own label above the rest of the paragraph.
         */
        BODY_STRONG(FontFile.WORK_SANS_SEMIBOLD, 13, 20, 0f),
        /** Table cell text: Work Sans Medium 12, line-height 17. */
        BODY_SM(FontFile.WORK_SANS_MEDIUM, 12, 17, 0f),
        /** Small meta text &mdash; footer stats, a progress-step label: Work Sans Medium 11, line-height 14. */
        CAPTION(FontFile.WORK_SANS_MEDIUM, 11, 14, 0f),
        /**
         * The tagline accent beneath the wordmark, and similar small italic
         * accents: Work Sans SemiBold Italic 12, line-height 15. Pair with
         * {@link #GOLD_ON_PAGE} when set directly on {@link #SURFACE_PAGE}.
         */
        TAGLINE(FontFile.WORK_SANS_SEMIBOLD_ITALIC, 12, 15, 0f);

        private final FontFile file;
        private final int size;
        private final int lineHeight;
        private final float letterSpacingEm;

        TextStyle(FontFile file, int size, int lineHeight, float letterSpacingEm) {
            this.file = file;
            this.size = size;
            this.lineHeight = lineHeight;
            this.letterSpacingEm = letterSpacingEm;
        }

        /**
         * Returns the font file this style is generated from.
         *
         * @return the font file
         */
        public FontFile file() {
            return file;
        }

        /**
         * Returns the size in pixels at the virtual resolution.
         *
         * @return the font size
         */
        public int size() {
            return size;
        }

        /**
         * Returns the design's intended line height in pixels. Not applied
         * automatically by {@link Fonts#load} (a {@code BitmapFont}'s own
         * metrics drive real line breaking); use it when setting a
         * {@code Label}'s wrap leading by hand.
         *
         * @return the line height in pixels
         */
        public int lineHeight() {
            return lineHeight;
        }

        /**
         * Returns the extra letter spacing as a fraction of the size.
         *
         * @return the letter spacing in em
         */
        public float letterSpacingEm() {
            return letterSpacingEm;
        }
    }

    /**
     * The characters every font is generated with: the basic set plus Latin
     * Extended letters, so names such as Koehler, Lodz and Istanbul render,
     * and the punctuation the screens use. Shared verbatim with the sibling
     * projects' own {@code Theme} classes &mdash; generic Unicode range data,
     * not project-specific content.
     */
    public static final String CHARACTERS = FreeTypeFontGenerator.DEFAULT_CHARS
        + "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏ"
        + "ÐÑÒÓÔÕÖØÙÚÛÜÝÞß"
        + "àáâãäåæçèéêëìíîï"
        + "ðñòóôõöøùúûüýþÿ"
        + "ĀāĂăĄąĆćĈĉĊċČčĎď"
        + "ĐđĒēĔĕĖėĘęĚěĜĝĞğ"
        + "İıŁłŒœŚśŠšŤťŪūŮů"
        + "ŰűŲųŸŹźŻżŽž"
        + "€×–—…‘’“”·−•";

    /**
     * One {@link BitmapFont} per {@link TextStyle}, owned by this object.
     * Dispose it when the game closes.
     */
    public static final class Fonts implements Disposable {

        private final Map<TextStyle, BitmapFont> fonts = new EnumMap<>(TextStyle.class);

        private Fonts() {
        }

        /**
         * Generates every text style from the TrueType files in a directory.
         * Each style becomes a bitmap font sized for the given scale, so a
         * window larger than {@value Theme#VIEW_HEIGHT} pixels tall can pass
         * a scale above 1 to keep text sharp; the fonts are scaled back down
         * by the same factor, so their size in the {@value Theme#VIEW_WIDTH}
         * by {@value Theme#VIEW_HEIGHT} layout stays the one the style
         * declares.
         *
         * @param directory the folder holding the font files, for example {@code Gdx.files.internal("fonts")}
         * @param scale     the ratio of the real window height to {@value Theme#VIEW_HEIGHT}; 1 for the design size
         * @return the generated fonts
         * @throws IllegalArgumentException if the scale is not positive
         */
        public static Fonts load(FileHandle directory, float scale) {
            if (scale <= 0f) {
                throw new IllegalArgumentException("Scale must be positive: " + scale);
            }
            Fonts result = new Fonts();
            Map<FontFile, FreeTypeFontGenerator> generators = new EnumMap<>(FontFile.class);
            try {
                for (TextStyle style : TextStyle.values()) {
                    FreeTypeFontGenerator generator = generators.computeIfAbsent(style.file(),
                        file -> new FreeTypeFontGenerator(directory.child(file.fileName())));
                    BitmapFont font = generator.generateFont(parameterFor(style, scale));
                    font.getData().setScale(1f / scale);
                    result.fonts.put(style, font);
                }
            } finally {
                generators.values().forEach(FreeTypeFontGenerator::dispose);
            }
            return result;
        }

        private static FreeTypeFontParameter parameterFor(TextStyle style, float scale) {
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = Math.max(1, Math.round(style.size() * scale));
            parameter.spaceX = Math.round(parameter.size * style.letterSpacingEm());
            parameter.color = Color.WHITE;
            parameter.characters = CHARACTERS;
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            return parameter;
        }

        /**
         * Returns the font of a style. The glyphs are white, so tint them
         * with the font color of a label or button style.
         *
         * @param style the text style
         * @return the font; owned by this object
         */
        public BitmapFont get(TextStyle style) {
            return fonts.get(style);
        }

        /**
         * Releases every font and its glyph pages.
         */
        @Override
        public void dispose() {
            fonts.values().forEach(BitmapFont::dispose);
            fonts.clear();
        }
    }

    // ---------------------------------------------------------------- shapes

    /**
     * The kinds of button the design has, matching the design system's
     * {@code Button} component exactly: one solid primary action per
     * screen, a plain neutral outline for the rest, and a red outline
     * reserved for destructive actions.
     */
    public enum ButtonKind {
        /** {@link #NAVY} fill, {@link #ON_FILL} label. The one primary action in a group. */
        PRIMARY,
        /** {@link #NAVY}-outlined, transparent. A neutral action beside a primary one. */
        SECONDARY,
        /** {@link #DANGER}-outlined, transparent. Actions that actually end or cancel something. */
        DESTRUCTIVE,
        /** {@link #COMMIT} fill, {@link #ON_FILL} label. See {@link #COMMIT}'s own doc &mdash; not used by any real screen yet. */
        COMMIT
    }

    /**
     * The kinds of badge the design has, matching the design system's
     * {@code Badge} component exactly.
     */
    public enum BadgeKind {
        /** {@link #GOLD} fill. In progress, caution, or a steady/neutral state. */
        ACTIVE,
        /** {@link #SUCCESS} fill. A genuinely good/high outcome. */
        SUCCESS,
        /** {@link #DANGER} fill. Failure &mdash; not yet used by a real screen, see {@code Badge}'s own README in the design system. */
        DANGER
    }

    /**
     * The kinds of notice box: a full-width message on {@link #SURFACE_CARD}'s family of
     * tints, bordered in its own color. Taken from the Connect and Join Game mockups'
     * local {@code .banner}/{@code .hint} CSS (2026-10-04), promoted into the design system's
     * own {@code Notice} component the same day; {@link #SUCCESS} added 2026-10-04 once the
     * SET Centre mockup's Action Result Toast (ui-spec.md &sect;2 component 6) needed a
     * fourth, non-navy-neutral case for "it worked".
     */
    public enum NoticeKind {
        /** A problem the player can fix (versions don't match): {@link #GOLD} border, {@link #GOLD_TEXT} text. */
        WARNING,
        /** A failure (the server can't be reached): {@link #DANGER} border and text. */
        DANGER,
        /** Neutral guidance (the Join Game hint): {@link #NAVY} border, {@link #INK} text. */
        INFO,
        /** An action actually went through: {@link #SUCCESS} border and text. */
        SUCCESS;

        /**
         * Returns the text color to set inside a notice of this kind.
         *
         * @return the text color
         */
        public Color textColor() {
            return switch (this) {
                case WARNING -> GOLD_TEXT;
                case DANGER -> Theme.DANGER;
                case INFO -> INK;
                case SUCCESS -> Theme.SUCCESS;
            };
        }
    }

    /**
     * The state of one step in a {@code ProgressTrack}-style sequence.
     */
    public enum ProgressState {
        /** Already happened &mdash; a neutral fact, not a value judgment on the outcome. Solid {@link #NAVY} dot. */
        DONE,
        /** The step running right now. A {@link #GOLD}-ringed dot &mdash; the "active" meaning showing up outside a badge. */
        CURRENT,
        /** Hasn't happened yet. A faint outlined dot. */
        PENDING
    }

    /**
     * Builds and owns the stretchable/fixed-size textures for fills, borders
     * and progress dots. They need an OpenGL context, so create this in the
     * game's {@code create()} and dispose it when the game closes.
     */
    public static final class Shapes implements Disposable {

        private final List<Texture> textures = new ArrayList<>();
        private final NinePatchDrawable whitePixel;

        /**
         * Creates the shape factory, building the single shared 1&times;1
         * white texture every {@link #solid} fill is tinted from.
         */
        public Shapes() {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            textures.add(texture);
            whitePixel = new NinePatchDrawable(new NinePatch(new TextureRegion(texture)));
        }

        /**
         * A flat, opaque or semi-transparent color fill, no border. Reuses
         * the one shared white texture (tinted), so this is cheap to call
         * repeatedly.
         *
         * @param color the fill color
         * @return the drawable
         */
        public NinePatchDrawable solid(Color color) {
            return whitePixel.tint(color);
        }

        /**
         * A stretchable, sharp-cornered rectangle with a uniform border on
         * all four sides &mdash; the general-purpose primitive behind
         * {@link #button}, {@link #navTab} and every other bordered shape in
         * this system. No built-in padding: leave room for content with
         * {@code SPACE_*} in your layout.
         *
         * @param fill        the fill color; use an alpha-0 color for a hollow, outline-only look
         * @param border      the border color
         * @param borderWidth the border width in pixels, 0 for none
         * @return the drawable; its texture is owned by this object
         */
        public NinePatchDrawable rect(Color fill, Color border, float borderWidth) {
            int bw = Math.max(0, Math.round(borderWidth));
            int size = 2 * bw + 1;
            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            if (bw > 0) {
                pixmap.setColor(border);
                pixmap.fillRectangle(0, 0, size, size);
            }
            pixmap.setColor(fill);
            pixmap.fillRectangle(bw, bw, size - 2 * bw, size - 2 * bw);
            Texture texture = new Texture(pixmap);
            // Nearest, not Linear: the patch is only a few pixels, and linear filtering blends
            // the border into the stretched centre, turning a flat fill into a gradient (seen on
            // the first Connect screen snapshot, 2026-10-04).
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pixmap.dispose();
            textures.add(texture);
            NinePatchDrawable drawable = new NinePatchDrawable(
                new NinePatch(new TextureRegion(texture), bw, bw, bw, bw));
            drawable.setLeftWidth(0f);
            drawable.setRightWidth(0f);
            drawable.setTopHeight(0f);
            drawable.setBottomHeight(0f);
            return drawable;
        }

        /**
         * A small, fixed-size, anti-aliased circle &mdash; used only for
         * progress-step dots and similar indicators (see {@link
         * #RADIUS_FULL}'s doc). Unlike {@link #rect}, this one needs
         * anti-aliasing: a plain axis-aligned rectangle's edges already sit
         * on the pixel grid, but a small circle's don't.
         *
         * @param fill        the fill color
         * @param border      the ring color, or {@code null} for a plain filled disc
         * @param borderWidth the ring width in pixels, ignored if {@code border} is {@code null}
         * @param diameter    the circle's diameter in pixels
         * @return the drawable; its texture is owned by this object
         */
        public Drawable circle(Color fill, Color border, float borderWidth, int diameter) {
            int size = Math.max(1, diameter);
            int bw = Math.max(0, Math.round(borderWidth));
            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            double r = size / 2.0;
            int fillRgba = Color.rgba8888(fill);
            int borderRgba = border == null ? 0 : Color.rgba8888(border);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    double px = x + 0.5;
                    double py = y + 0.5;
                    double dist = Math.hypot(px - r, py - r) - r;
                    double outer = coverage(dist);
                    int out = 0;
                    if (border != null && bw > 0) {
                        double inner = coverage(Math.hypot(px - r, py - r) - (r - bw));
                        out = over(out, borderRgba, Math.max(0.0, outer - inner));
                        out = over(out, fillRgba, inner);
                    } else {
                        out = over(out, fillRgba, outer);
                    }
                    pixmap.drawPixel(x, y, out);
                }
            }
            Texture texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            pixmap.dispose();
            textures.add(texture);
            return new TextureRegionDrawable(new TextureRegion(texture));
        }

        /**
         * A short repeating dash unit (8 px on, 4 px off) tiled to fill
         * whatever width it's stretched to &mdash; the connector between two
         * {@link #progressDot}s.
         *
         * @param color the dash color
         * @return the drawable; its texture is owned by this object
         */
        public Drawable dash(Color color) {
            int dashLength = 8;
            int gapLength = 4;
            int thickness = Math.max(1, Math.round(BORDER_EMPHASIS));
            Pixmap pixmap = new Pixmap(dashLength + gapLength, thickness, Pixmap.Format.RGBA8888);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            pixmap.setColor(color);
            pixmap.fillRectangle(0, 0, dashLength, thickness);
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            textures.add(texture);
            return new TiledDrawable(new TextureRegion(texture));
        }

        /**
         * Builds a button style: the up, pressed and disabled looks and the
         * label font color. There are no shadows or a pressed-offset sink in
         * this system (see the class doc) &mdash; the pressed look is a
         * subtle darken instead. The style does not own the font.
         *
         * @param kind the kind of button
         * @param font the font for the label, normally {@code fonts.get(TextStyle.LABEL)}
         * @return the style, ready for a {@code TextButton}
         */
        public TextButtonStyle button(ButtonKind kind, BitmapFont font) {
            TextButtonStyle style = new TextButtonStyle();
            style.font = font;
            style.disabledFontColor = INK_MUTED;
            Color pressedOverlay = new Color(0f, 0f, 0f, 0.06f);
            switch (kind) {
                case PRIMARY -> {
                    style.up = solid(NAVY);
                    style.down = solid(new Color(NAVY).lerp(Color.BLACK, 0.12f));
                    style.disabled = solid(new Color(NAVY.r, NAVY.g, NAVY.b, 0.4f));
                    style.fontColor = ON_FILL;
                }
                case SECONDARY -> {
                    style.up = rect(new Color(0f, 0f, 0f, 0f), NAVY, BORDER_THIN);
                    style.down = rect(pressedOverlay, NAVY, BORDER_THIN);
                    style.disabled = rect(new Color(0f, 0f, 0f, 0f), INK_MUTED, BORDER_THIN);
                    style.fontColor = NAVY;
                }
                case DESTRUCTIVE -> {
                    style.up = rect(new Color(0f, 0f, 0f, 0f), DANGER, BORDER_THIN);
                    style.down = rect(pressedOverlay, DANGER, BORDER_THIN);
                    style.disabled = rect(new Color(0f, 0f, 0f, 0f), INK_MUTED, BORDER_THIN);
                    style.fontColor = DANGER;
                }
                default -> {
                    style.up = solid(COMMIT);
                    style.down = solid(new Color(COMMIT).lerp(Color.BLACK, 0.12f));
                    style.disabled = solid(new Color(COMMIT.r, COMMIT.g, COMMIT.b, 0.4f));
                    style.fontColor = ON_FILL;
                }
            }
            return style;
        }

        /**
         * Builds a badge style: a solid fill and the matching text color. A
         * badge has no built-in padding; wrap the {@code Label} in a
         * {@code Container} or give its {@code Table} cell {@code SPACE_1}/
         * {@code SPACE_2} padding.
         *
         * @param kind the kind of badge
         * @param font the font for the label, normally {@code fonts.get(TextStyle.LABEL)}
         * @return the style, ready for a {@code Label}
         */
        public LabelStyle badge(BadgeKind kind, BitmapFont font) {
            LabelStyle style = new LabelStyle();
            style.font = font;
            switch (kind) {
                case ACTIVE -> {
                    style.background = solid(GOLD);
                    style.fontColor = ON_GOLD;
                }
                case SUCCESS -> {
                    style.background = solid(SUCCESS);
                    style.fontColor = ON_FILL;
                }
                default -> {
                    style.background = solid(DANGER);
                    style.fontColor = ON_FILL;
                }
            }
            return style;
        }

        /**
         * Builds a {@code Field} component's input-box style: a thin navy
         * border on {@link #SURFACE_CARD}, switching to {@link #DANGER} and
         * a {@link #BORDER_EMPHASIS} focus ring when {@code error} is set
         * &mdash; matching the design system's {@code .lw-field__control}
         * exactly. The label above the box is not part of this style: it's
         * a plain {@link TextStyle#LABEL} {@code Label} in {@link
         * #INK_MUTED}, upper-cased by the caller, same as every other
         * section-eyebrow label in this system. Scene2D has no single style
         * object that can toggle border color at runtime, so build two
         * instances (one per {@code error} state) rather than mutating one.
         *
         * @param font  the font for the typed text, normally {@code fonts.get(TextStyle.BODY)}
         * @param error whether to render the validation-failure look
         * @return the style, ready for a {@code TextField}
         */
        public TextFieldStyle textField(BitmapFont font, boolean error) {
            TextFieldStyle style = new TextFieldStyle();
            style.font = font;
            style.fontColor = INK;
            style.messageFont = font;
            style.messageFontColor = INK_MUTED;
            Color borderColor = error ? DANGER : NAVY;
            style.background = rect(SURFACE_CARD, borderColor, BORDER_THIN);
            style.focusedBackground = rect(SURFACE_CARD, borderColor, BORDER_EMPHASIS);
            style.disabledBackground = rect(SURFACE_CARD, INK_MUTED, BORDER_THIN);
            style.cursor = solid(NAVY);
            style.selection = solid(new Color(NAVY.r, NAVY.g, NAVY.b, 0.25f));
            return style;
        }

        /**
         * Builds an {@code OptionCard} background: a plain {@link
         * #NAVY_BORDER}-thin outline on {@link #SURFACE_CARD} normally, or a
         * {@link #BORDER_EMPHASIS} {@link #GOLD} outline over a gold-tinted
         * fill when {@code selected} &mdash; matching
         * {@code .lw-optioncard--selected} exactly (a 10% blend of gold into
         * white, not a translucent gold overlay, so it stays opaque on top
         * of the card's own image). Its "Selected" badge is not part of this
         * drawable: it's a plain {@link #badge}({@link BadgeKind#ACTIVE}, ...).
         *
         * @param selected whether to render the selected look
         * @return the drawable; its texture is owned by this object
         */
        public NinePatchDrawable optionCard(boolean selected) {
            return selected
                ? rect(new Color(SURFACE_CARD).lerp(GOLD, 0.1f), GOLD, BORDER_EMPHASIS)
                : rect(SURFACE_CARD, NAVY_BORDER, BORDER_THIN);
        }

        /**
         * Builds a {@code HistoryNote} popover's background: a thin navy
         * border on {@link #SURFACE_CARD}, the "archive index card" look,
         * flat like every other surface in this system (see the class doc).
         * <b>Resolved (user, 2026-10-04):</b> the design system's own CSS
         * briefly gave this popover a drop shadow &mdash; the only one this
         * system would have had &mdash; and it read as out of place; the
         * design system dropped it the same day to match. The open-book
         * icon itself is not a {@code Shapes} method at all: unlike a flat
         * rect or circle, it's an actual drawn glyph, so it's a real asset
         * ({@code assets-raw/svg/icon_history_note.svg}, rasterized to
         * {@code assets/ui/icon_history_note.png}); see {@code HistoryNote}.
         *
         * @return the drawable; its texture is owned by this object
         */
        public NinePatchDrawable historyNoteCard() {
            return rect(SURFACE_CARD, NAVY, BORDER_THIN);
        }

        /**
         * The primary Card variant: a {@link #BORDER_EMPHASIS}-wide
         * {@link #NAVY} outline around {@link #SURFACE_CARD}. Use for a
         * self-contained block that reads as its own object (an
         * active-mission detail card).
         *
         * @return the drawable
         */
        public NinePatchDrawable cardOutlined() {
            return rect(SURFACE_CARD, NAVY, BORDER_EMPHASIS);
        }

        /**
         * The quieter Card variant: {@link #SURFACE_CARD} with a
         * {@link #BORDER_ACCENT}-wide left stripe only, in {@code
         * accentColor} (the design system's default is {@link #GOLD_TEXT}).
         * Use for a smaller callout attached to a topic (a "Mission Brief"
         * paragraph); pass a different color only when the callout's own
         * content is a warning, not as a cosmetic choice.
         *
         * @param accentColor the stripe color
         * @return the drawable; its texture is owned by this object
         */
        public NinePatchDrawable cardAccent(Color accentColor) {
            int bw = Math.max(1, Math.round(BORDER_ACCENT));
            int size = bw + 1;
            Pixmap pixmap = new Pixmap(size, 1, Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(accentColor);
            pixmap.fillRectangle(0, 0, bw, 1);
            pixmap.setColor(SURFACE_CARD);
            pixmap.fillRectangle(bw, 0, size - bw, 1);
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            textures.add(texture);
            NinePatchDrawable drawable = new NinePatchDrawable(
                new NinePatch(new TextureRegion(texture), bw, 0, 0, 0));
            drawable.setLeftWidth(0f);
            drawable.setRightWidth(0f);
            drawable.setTopHeight(0f);
            drawable.setBottomHeight(0f);
            return drawable;
        }

        /**
         * Builds a notice box background: a thin border in the kind's color around a pale,
         * opaque tint of it (10% gold, 8% danger, 6% navy or 8% success blended into white,
         * matching the design system's {@code color-mix} values). No built-in padding; pad
         * the content by {@link #SPACE_3}.
         *
         * @param kind the kind of notice
         * @return the drawable; its texture is owned by this object
         */
        public NinePatchDrawable notice(NoticeKind kind) {
            return switch (kind) {
                case WARNING -> rect(new Color(SURFACE_CARD).lerp(GOLD, 0.10f), GOLD, BORDER_THIN);
                case DANGER -> rect(new Color(SURFACE_CARD).lerp(DANGER, 0.08f), DANGER, BORDER_THIN);
                case INFO -> rect(new Color(SURFACE_CARD).lerp(NAVY, 0.06f), NAVY, BORDER_THIN);
                case SUCCESS -> rect(new Color(SURFACE_CARD).lerp(SUCCESS, 0.08f), SUCCESS, BORDER_THIN);
            };
        }

        /**
         * Builds the page frame's blueprint grid, exactly as the UI Asset Sheet specifies it: a
         * 100 by 100 tile of {@link #SURFACE_PAGE}, fine {@link #GRID_LINE} hairlines at 20, 40,
         * 60 and 80 on both axes, and one {@link #GRID_LINE_STRONG} line at 0 on each axis only
         * (the tile's far edge is the next tile's 0, so drawing it there too would double the
         * line once tiled). Paint it behind a whole screen; it repeats with no seam.
         *
         * @return the drawable; its texture is owned by this object
         */
        public TiledDrawable blueprintGrid() {
            int tile = 100;
            int pitch = 20;
            Pixmap pixmap = new Pixmap(tile, tile, Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(SURFACE_PAGE);
            pixmap.fill();
            pixmap.setBlending(Pixmap.Blending.SourceOver);
            pixmap.setColor(GRID_LINE);
            for (int at = pitch; at < tile; at += pitch) {
                pixmap.drawLine(at, 0, at, tile - 1);
                pixmap.drawLine(0, at, tile - 1, at);
            }
            pixmap.setColor(GRID_LINE_STRONG);
            pixmap.drawLine(0, 0, 0, tile - 1);
            pixmap.drawLine(0, 0, tile - 1, 0);
            Texture texture = new Texture(pixmap);
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            pixmap.dispose();
            textures.add(texture);
            return new TiledDrawable(new TextureRegion(texture));
        }

        /**
         * A nav-tab background. Renders directly on {@link #SURFACE_PAGE}
         * &mdash; never place one on a {@link #SURFACE_CARD}, its inactive
         * look assumes the blueprint background underneath.
         *
         * @param active whether this is the current tab
         * @return the drawable
         */
        public Drawable navTab(boolean active) {
            return active
                ? solid(SURFACE_CARD)
                : rect(new Color(0f, 0f, 0f, 0f), ON_PAGE, BORDER_THIN);
        }

        /**
         * One dot of a {@code ProgressTrack}-style sequence, sized by the
         * caller (the design system's own mockup used ~11 px at a much
         * smaller virtual resolution than {@link Theme#VIEW_WIDTH}; no fixed
         * size is prescribed here, pick one that reads well in your layout).
         *
         * @param state    the step's state
         * @param diameter the dot's diameter in pixels
         * @return the drawable
         */
        public Drawable progressDot(ProgressState state, int diameter) {
            return switch (state) {
                case DONE -> circle(NAVY, null, 0, diameter);
                case CURRENT -> circle(SURFACE_CARD, GOLD, BORDER_EMPHASIS, diameter);
                case PENDING -> circle(SURFACE_CARD, NAVY_BORDER, BORDER_EMPHASIS, diameter);
            };
        }

        /**
         * The dash connecting two {@link #progressDot}s. {@link #GOLD} for
         * the segment leading into the current step, {@link #NAVY_BORDER}
         * otherwise &mdash; matching {@link #progressDot}'s own coloring
         * exactly is what makes the chain read as one sequence.
         *
         * @param leadsIntoCurrent whether this segment leads into the current step
         * @return the drawable
         */
        public Drawable progressDash(boolean leadsIntoCurrent) {
            return dash(leadsIntoCurrent ? GOLD : NAVY_BORDER);
        }

        /**
         * Releases every texture built so far.
         */
        @Override
        public void dispose() {
            textures.forEach(Texture::dispose);
            textures.clear();
        }

        /**
         * Turns a signed distance into how much of a pixel is covered.
         *
         * @param distance the distance from the pixel center to the edge
         * @return a value from 0 (outside) to 1 (inside)
         */
        private static double coverage(double distance) {
            return Math.max(0.0, Math.min(1.0, 0.5 - distance));
        }

        /**
         * Composites a color over a pixel with the "source over" rule, on
         * straight (not premultiplied) alpha.
         *
         * @param dst      the pixel below, as RGBA8888
         * @param src      the color on top, as RGBA8888
         * @param coverage how much of the pixel the color covers, from 0 to 1
         * @return the combined pixel, as RGBA8888
         */
        private static int over(int dst, int src, double coverage) {
            double sa = (src & 0xff) / 255.0 * coverage;
            double da = (dst & 0xff) / 255.0;
            double oa = sa + da * (1.0 - sa);
            if (oa <= 0.0) {
                return 0;
            }
            int red = blend((src >>> 24) & 0xff, (dst >>> 24) & 0xff, sa, da, oa);
            int green = blend((src >>> 16) & 0xff, (dst >>> 16) & 0xff, sa, da, oa);
            int blue = blend((src >>> 8) & 0xff, (dst >>> 8) & 0xff, sa, da, oa);
            return (red << 24) | (green << 16) | (blue << 8) | (int) Math.round(oa * 255.0);
        }

        /**
         * Blends one color channel for {@link #over}.
         *
         * @param s  the top channel value, 0 to 255
         * @param d  the bottom channel value, 0 to 255
         * @param sa the top alpha, 0 to 1
         * @param da the bottom alpha, 0 to 1
         * @param oa the resulting alpha, above 0
         * @return the resulting channel value, 0 to 255
         */
        private static int blend(int s, int d, double sa, double da, double oa) {
            return (int) Math.round((s * sa + d * da * (1.0 - sa)) / oa);
        }
    }
}
