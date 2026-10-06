package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import de.mkoehler.launchwindow.model.Archetype;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The client's shared widget kit: owns the fonts, the shape textures and the image assets,
 * and builds the small pieces every screen is made of, so screens never call {@link
 * Theme.Shapes} in a loop (each call there makes a new texture). Build one in the game's
 * {@code create()}, dispose it when the game closes.
 *
 * @author Mario Koehler
 */
public final class UiKit implements Disposable {

    private final Theme.Fonts fonts;
    private final Theme.Shapes shapes = new Theme.Shapes();
    private final Texture logo;
    private final Texture historyNoteIcon;
    private final Map<Archetype, Texture> archetypeArt = new EnumMap<>(Archetype.class);
    private final Map<Theme.ButtonKind, TextButton.TextButtonStyle> buttonStyles = new EnumMap<>(Theme.ButtonKind.class);
    private final Map<Theme.NoticeKind, Drawable> notices = new EnumMap<>(Theme.NoticeKind.class);
    private final TextField.TextFieldStyle fieldStyle;
    private final TextField.TextFieldStyle fieldErrorStyle;
    private final Drawable blueprintGrid;
    private final Drawable cardOutlined;
    private final Drawable optionCard;
    private final Drawable optionCardSelected;
    private final Drawable historyNoteCard;
    private final Label.LabelStyle activeBadge;
    private final Drawable badgeFill;
    private final FileHandle assets;
    private final Map<String, Texture> icons = new HashMap<>();
    private final Map<String, Texture> pictures = new HashMap<>();
    private final Map<String, Drawable> rects = new HashMap<>();
    private final Map<Theme.BadgeKind, Drawable> badgeFills = new EnumMap<>(Theme.BadgeKind.class);
    /**
     * The tooltips' timing: quicker than Scene2D's default two seconds, and no grow animation. A
     * tooltip is laid out again each time it shows, since it lives off the stage and so misses the
     * re-layout after the fonts are rebuilt for a new window size.
     */
    private final TooltipManager tooltips = new TooltipManager() {
        @Override
        protected void showAction(Tooltip tooltip) {
            relayout(tooltip.getContainer());
            super.showAction(tooltip);
        }
    };
    /** How wide a tooltip's explanation wraps. */
    private static final float TOOLTIP_TEXT_WIDTH = 280f;
    private Slider.SliderStyle sliderStyle;
    private TextButton.TextButtonStyle pageButtonStyle;
    private String backdropPath;
    private Texture backdrop;

    /**
     * Loads the fonts and images and builds the shared styles.
     *
     * @param assets    the assets root, holding {@code fonts/}, {@code branding/} and {@code ui/}
     * @param fontScale the window's pixels per layout unit ({@link Theme.Fonts#windowScale}), so text stays sharp
     */
    public UiKit(FileHandle assets, float fontScale) {
        this.assets = assets;
        this.fonts = Theme.Fonts.load(assets.child("fonts"), fontScale);
        this.logo = smoothTexture(assets.child("branding/logo.png"));
        this.historyNoteIcon = smoothTexture(assets.child("ui/icon_history_note.png"));
        archetypeArt.put(Archetype.NASA, smoothTexture(assets.child("ui/archetype_nasa.jpg")));
        archetypeArt.put(Archetype.SOVIET, smoothTexture(assets.child("ui/archetype_soviet.jpg")));
        for (Theme.ButtonKind kind : Theme.ButtonKind.values()) {
            buttonStyles.put(kind, shapes.button(kind, fonts.get(Theme.TextStyle.LABEL)));
        }
        for (Theme.NoticeKind kind : Theme.NoticeKind.values()) {
            notices.put(kind, shapes.notice(kind));
        }
        this.fieldStyle = paddedField(shapes.textField(fonts.get(Theme.TextStyle.BODY), false));
        this.fieldErrorStyle = paddedField(shapes.textField(fonts.get(Theme.TextStyle.BODY), true));
        this.blueprintGrid = shapes.blueprintGrid();
        this.cardOutlined = shapes.cardOutlined();
        this.optionCard = shapes.optionCard(false);
        this.optionCardSelected = shapes.optionCard(true);
        this.historyNoteCard = shapes.historyNoteCard();
        this.activeBadge = shapes.badge(Theme.BadgeKind.ACTIVE, fonts.get(Theme.TextStyle.LABEL));
        Drawable badgeFill = activeBadge.background;
        activeBadge.background = null;
        this.badgeFill = badgeFill;
        for (Theme.BadgeKind kind : Theme.BadgeKind.values()) {
            badgeFills.put(kind, shapes.badge(kind, fonts.get(Theme.TextStyle.LABEL)).background);
        }
        tooltips.initialTime = 0.4f;
        tooltips.subsequentTime = 0.1f;
        tooltips.resetTime = 1.5f;
        tooltips.animations = false;
        tooltips.offsetX = 12f;
        tooltips.offsetY = 12f;
    }

    /**
     * Marks an actor and everything inside it for a new layout, so text laid out with the old
     * glyphs is laid out again with the new ones ({@link Theme.Fonts#generation}).
     *
     * @param actor the actor
     */
    public static void relayout(Actor actor) {
        if (actor instanceof Layout layout) {
            layout.invalidate();
        }
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                relayout(child);
            }
        }
    }

    /**
     * Returns a UI icon from {@code assets/ui/icons/}, white so it can be tinted with
     * {@code Image.setColor}. Loaded once and kept.
     *
     * @param name the icon's file name without {@code .png}, such as {@code prestige}
     * @return the drawable
     */
    public TextureRegionDrawable icon(String name) {
        Texture texture = icons.computeIfAbsent(name, key -> smoothTexture(assets.child("ui/icons/" + key + ".png")));
        return new TextureRegionDrawable(texture);
    }

    /**
     * Returns a picture from {@code assets/ui/}, such as a program's art, loaded once and kept.
     *
     * @param path the path under {@code assets/ui/}, such as {@code programs/vostok.jpg}
     * @return the drawable, or {@code null} if there's no such picture
     */
    public TextureRegionDrawable picture(String path) {
        if (!pictures.containsKey(path)) {
            FileHandle file = assets.child("ui/" + path);
            pictures.put(path, file.exists() ? smoothTexture(file) : null);
        }
        Texture texture = pictures.get(path);
        return texture == null ? null : new TextureRegionDrawable(texture);
    }

    /**
     * Returns a text file from the assets folder, such as {@code CREDITS.md}.
     *
     * @param path the path under the assets folder
     * @return the text, or {@code null} if there's no such file
     */
    public String text(String path) {
        FileHandle file = assets.child(path);
        return file.exists() ? file.readString("UTF-8") : null;
    }

    /**
     * Returns a picture from {@code assets/ui/} cropped about its centre to an aspect ratio, so it
     * fills a box of that shape without spilling out of it.
     *
     * @param path   the path under {@code assets/ui/}
     * @param aspect the box's width over its height
     * @return the drawable, or {@code null} if there's no such picture
     */
    public TextureRegionDrawable croppedPicture(String path, float aspect) {
        TextureRegionDrawable picture = picture(path);
        if (picture == null) {
            return null;
        }
        Texture texture = picture.getRegion().getTexture();
        int width = texture.getWidth();
        int height = texture.getHeight();
        if (width / (float) height > aspect) {
            int cropped = Math.round(height * aspect);
            return new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(texture,
                (width - cropped) / 2, 0, cropped, height));
        }
        int cropped = Math.round(width / aspect);
        return new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(texture, 0,
            (height - cropped) / 2, width, cropped));
    }

    /**
     * Returns a full-screen backdrop from {@code assets/}, such as the hub's art. Only one is kept:
     * asking for another releases the previous one, so the 32 hub images never pile up, while
     * returning to the hub with the same art loads nothing.
     *
     * @param path the path under {@code assets/}
     * @return the drawable, or {@code null} if there's no such file
     */
    public TextureRegionDrawable backdrop(String path) {
        if (!path.equals(backdropPath)) {
            if (backdrop != null) {
                backdrop.dispose();
            }
            FileHandle file = assets.child(path);
            backdrop = file.exists() ? smoothTexture(file) : null;
            backdropPath = path;
        }
        return backdrop == null ? null : new TextureRegionDrawable(backdrop);
    }

    /**
     * Returns the unread mark: a small gold dot ringed in navy, as on the hub's Turn Report button.
     *
     * @return the drawable, 10 px across
     */
    public Drawable unreadDot() {
        return rects.computeIfAbsent("unreadDot", key -> shapes.circle(Theme.GOLD, Theme.NAVY, 1.5f, 10));
    }

    /**
     * Returns the game result's trophy disc: a solid gold circle.
     *
     * @return the drawable, drawn at 112 px and scaled down
     */
    public Drawable trophy() {
        return rects.computeIfAbsent("trophy", key -> shapes.circle(Theme.GOLD, Theme.GOLD, 1f, 112));
    }

    /**
     * Returns a small circle ({@link Theme.Shapes#circle}), such as an explainer's numbered disc or
     * its page dots, built once per look.
     *
     * @param fill        the fill
     * @param border      the ring, or {@code null} for none
     * @param borderWidth the ring's width
     * @param diameter    the diameter in pixels
     * @return the drawable
     */
    public Drawable circle(Color fill, Color border, float borderWidth, int diameter) {
        return rects.computeIfAbsent("circle:" + fill + "/" + border + "/" + borderWidth + "/" + diameter,
            key -> shapes.circle(fill, border, borderWidth, diameter));
    }

    /**
     * Returns a progress-track dot ({@link Theme.Shapes#progressDot}), built once per state.
     *
     * @param state the step's state
     * @return the drawable, 14 px across
     */
    public Drawable progressDot(Theme.ProgressState state) {
        return rects.computeIfAbsent("progressDot:" + state, key -> shapes.progressDot(state, 14));
    }

    /**
     * Returns the dash between two progress-track dots ({@link Theme.Shapes#progressDash}).
     *
     * @param leadsIntoCurrent whether it leads into the current step
     * @return the drawable
     */
    public Drawable progressDash(boolean leadsIntoCurrent) {
        return rects.computeIfAbsent("progressDash:" + leadsIntoCurrent, key -> shapes.progressDash(leadsIntoCurrent));
    }

    /**
     * Returns a nav tab's background ({@link Theme.Shapes#navTab}): white when active, an outline
     * on the page otherwise. Built once per look.
     *
     * @param active whether the tab is the current one
     * @return the drawable
     */
    public Drawable navTab(boolean active) {
        return rects.computeIfAbsent(active ? "navTab:active" : "navTab", key -> shapes.navTab(active));
    }

    /**
     * Returns the hover highlight for a round hotspot (a celestial body): the hub's decided look,
     * white at 18% for both fill and outline, as a circle stretched to the hotspot's ellipse.
     *
     * @return the drawable
     */
    public Drawable ellipseHighlight() {
        Color white = new Color(1f, 1f, 1f, 0.18f);
        return rects.computeIfAbsent("ellipse", key -> shapes.circle(white, white, 2f, 256));
    }

    /**
     * Returns a goal badge's patch ({@link Theme.Shapes#goalPatch}), built once per look.
     *
     * @param achieved whether the goal is achieved
     * @return the drawable
     */
    public Drawable goalPatch(boolean achieved) {
        return rects.computeIfAbsent("goal/" + achieved, key -> shapes.goalPatch(achieved, 128));
    }

    /**
     * Returns the native size of an icon's image, for keeping an icon's aspect ratio (the SET
     * skill icons all differ: size them by height only).
     *
     * @param name the icon's name
     * @return its width divided by its height
     */
    public float iconAspect(String name) {
        Texture texture = icons.computeIfAbsent(name, key -> smoothTexture(assets.child("ui/icons/" + key + ".png")));
        return (float) texture.getWidth() / texture.getHeight();
    }

    /**
     * Returns a sharp-cornered bordered rectangle ({@link Theme.Shapes#rect}), built once per
     * combination and kept, so screens that rebuild their widgets never make new textures.
     *
     * @param fill        the fill
     * @param border      the border color
     * @param borderWidth the border width, 0 for none
     * @return the drawable
     */
    public Drawable rect(Color fill, Color border, float borderWidth) {
        String key = fill + "/" + border + "/" + borderWidth;
        return rects.computeIfAbsent(key, k -> shapes.rect(fill, border, borderWidth));
    }

    /**
     * Returns the slider style (the training dialog's target level, the department salary
     * slider): a thin {@link Theme#SKILL_TRACK} track with a {@link Theme#NAVY} fill up to a
     * small navy knob. Built once.
     *
     * @return the style
     */
    public Slider.SliderStyle slider() {
        if (sliderStyle == null) {
            NinePatchDrawable track = (NinePatchDrawable) shapes.rect(Theme.SKILL_TRACK, Theme.SKILL_TRACK, 0f);
            track.setMinHeight(4f);
            NinePatchDrawable fill = (NinePatchDrawable) shapes.rect(Theme.NAVY, Theme.NAVY, 0f);
            fill.setMinHeight(4f);
            NinePatchDrawable knob = (NinePatchDrawable) shapes.rect(Theme.NAVY, Theme.NAVY, 0f);
            knob.setMinWidth(10f);
            knob.setMinHeight(18f);
            sliderStyle = new Slider.SliderStyle(track, knob);
            sliderStyle.knobBefore = fill;
        }
        return sliderStyle;
    }

    /**
     * Returns the accent-left card background ({@link Theme.Shapes#cardAccent}), built once per color.
     *
     * @param accent the stripe color
     * @return the drawable
     */
    public Drawable cardAccent(Color accent) {
        return rects.computeIfAbsent("accent/" + accent, key -> shapes.cardAccent(accent));
    }

    /**
     * Returns a flat fill, no border.
     *
     * @param color the fill color
     * @return the drawable
     */
    public Drawable solid(Color color) {
        return shapes.solid(color);
    }

    /**
     * Returns a notice box background ({@link Theme.Shapes#notice}).
     *
     * @param kind the kind
     * @return the drawable
     */
    public Drawable noticeBackground(Theme.NoticeKind kind) {
        return notices.get(kind);
    }

    /**
     * Builds a badge of any kind, padded {@link Theme#SPACE_1} by {@link Theme#SPACE_2}.
     *
     * @param kind the kind
     * @param text the text
     * @return the badge
     */
    public Container<Label> badge(Theme.BadgeKind kind, String text) {
        Color textColor = kind == Theme.BadgeKind.ACTIVE ? Theme.ON_GOLD : Theme.ON_FILL;
        Container<Label> badge = new Container<>(label(text, Theme.TextStyle.LABEL, textColor));
        badge.setBackground(badgeFills.get(kind));
        badge.pad(Theme.SPACE_1, Theme.SPACE_2, Theme.SPACE_1, Theme.SPACE_2);
        return badge;
    }

    /**
     * Builds a one-line notice (the action result toast's look): the message alone, no bold lead.
     *
     * @param kind the kind
     * @param text the message
     * @return the notice's table
     */
    public Table notice(Theme.NoticeKind kind, String text) {
        Table notice = new Table();
        notice.setBackground(notices.get(kind));
        notice.pad(Theme.SPACE_3);
        notice.add(paragraph(text, Theme.TextStyle.BODY, kind.textColor())).growX();
        return notice;
    }

    /**
     * Returns the fonts.
     *
     * @return the fonts; owned by this kit
     */
    public Theme.Fonts fonts() {
        return fonts;
    }

    /**
     * Builds a label. An {@link Theme.TextStyle#EYEBROW} is upper-cased here, as the design
     * system's CSS does; every other style keeps the text as given.
     *
     * @param text  the text
     * @param style the type style
     * @param color the text color
     * @return the label
     */
    public Label label(String text, Theme.TextStyle style, Color color) {
        String shown = style == Theme.TextStyle.EYEBROW ? text.toUpperCase(Locale.ROOT) : text;
        return new Label(shown, new Label.LabelStyle(fonts.get(style), color));
    }

    /**
     * Gives an actor a hover tooltip (ui-spec.md component 17): a small navy card with a bold title
     * and a short explanation, shown when the pointer rests on the actor. It explains what a value,
     * icon or control means and does, which is rules help; real-world stories use a history note
     * instead (component 15), never this.
     *
     * @param target the actor to explain
     * @param title  the title, such as "Morale"
     * @param text   one to three short sentences, or {@code null} for the title alone
     * @return the tooltip, already attached
     */
    public Tooltip<Table> tooltip(Actor target, String title, String text) {
        Table card = new Table();
        card.setBackground(solid(Theme.NAVY));
        card.pad(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_2, Theme.SPACE_3);
        card.defaults().left();
        card.add(label(title, Theme.TextStyle.BODY_STRONG, Theme.ON_FILL)).row();
        if (text != null) {
            card.add(paragraph(text, Theme.TextStyle.BODY_SM, Theme.ON_FILL)).width(TOOLTIP_TEXT_WIDTH).padTop(2f);
        }
        Tooltip<Table> tooltip = new Tooltip<>(card, tooltips);
        tooltip.getContainer().setTouchable(Touchable.disabled);
        if (target.getTouchable() == Touchable.childrenOnly) {
            // A table's padding should count as hovering it too.
            target.setTouchable(Touchable.enabled);
        }
        target.addListener(tooltip);
        return tooltip;
    }

    /**
     * Builds a label that wraps to its cell's width.
     *
     * @param text  the text
     * @param style the type style
     * @param color the text color
     * @return the label
     */
    public Label paragraph(String text, Theme.TextStyle style, Color color) {
        Label label = label(text, style, color);
        label.setWrap(true);
        label.setAlignment(Align.topLeft);
        return label;
    }

    /**
     * Builds a button, padded as the design system's {@code .lw-btn} is.
     *
     * @param text the label
     * @param kind the kind of button
     * @return the button
     */
    public TextButton button(String text, Theme.ButtonKind kind) {
        TextButton button = new TextButton(text, buttonStyles.get(kind));
        button.pad(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_2, Theme.SPACE_3);
        return button;
    }

    /**
     * Builds a secondary button for use directly on the blue page: the design system's secondary
     * button is transparent with navy text, which disappears on {@link Theme#SURFACE_PAGE}, so this
     * one gets a white fill (the mockups' own on-page icon buttons use the same white fill and
     * {@link Theme#ON_PAGE} border). Not yet a design-system variant.
     *
     * @param text the label
     * @return the button
     */
    public TextButton pageButton(String text) {
        if (pageButtonStyle == null) {
            pageButtonStyle = new TextButton.TextButtonStyle(buttonStyles.get(Theme.ButtonKind.SECONDARY));
            pageButtonStyle.up = rect(Theme.SURFACE_CARD, Theme.ON_PAGE, Theme.BORDER_THIN);
            pageButtonStyle.down = rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.08f), Theme.ON_PAGE,
                Theme.BORDER_THIN);
            pageButtonStyle.disabled = rect(new Color(1f, 1f, 1f, 0.5f), Theme.ON_PAGE, Theme.BORDER_THIN);
        }
        TextButton button = new TextButton(text, pageButtonStyle);
        button.pad(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_2, Theme.SPACE_3);
        return button;
    }

    /**
     * Builds a text field in its normal look.
     *
     * @param text        the initial text
     * @param placeholder the hint shown while empty
     * @return the field
     */
    public TextField textField(String text, String placeholder) {
        TextField field = new TextField(text, fieldStyle);
        field.setMessageText(placeholder);
        return field;
    }

    /**
     * Switches a text field between its normal and its error look.
     *
     * @param field the field
     * @param error whether to show the error look
     */
    public void setFieldError(TextField field, boolean error) {
        field.setStyle(error ? fieldErrorStyle : fieldStyle);
    }

    /**
     * Builds an outlined card (the design system's {@code Card variant="outlined"}): white,
     * a navy {@link Theme#BORDER_EMPHASIS} outline, {@link Theme#SPACE_4} padding.
     *
     * @return the card's table
     */
    public Table card() {
        Table card = new Table();
        card.setBackground(cardOutlined);
        float pad = Theme.SPACE_4 + Theme.BORDER_EMPHASIS;
        card.pad(pad);
        return card;
    }

    /**
     * Builds a notice box: a bold lead-in line, then the rest of the message, wrapped.
     *
     * @param kind  the kind of notice
     * @param lead  the bold lead-in, such as "Versions don't match."
     * @param text  the rest of the message
     * @return the notice's table
     */
    public Table notice(Theme.NoticeKind kind, String lead, String text) {
        Table notice = new Table();
        notice.setBackground(notices.get(kind));
        notice.pad(Theme.SPACE_3);
        notice.defaults().growX().left();
        notice.add(paragraph(lead, Theme.TextStyle.BODY_STRONG, kind.textColor())).row();
        notice.add(paragraph(text, Theme.TextStyle.BODY, kind.textColor()));
        return notice;
    }

    /**
     * Returns the page frame's tiled blueprint grid.
     *
     * @return the drawable
     */
    public Drawable blueprintGrid() {
        return blueprintGrid;
    }

    /**
     * Returns an option card's background.
     *
     * @param selected whether the card is selected
     * @return the drawable
     */
    public Drawable optionCard(boolean selected) {
        return selected ? optionCardSelected : optionCard;
    }

    /**
     * Returns the history-note popover's background.
     *
     * @return the drawable
     */
    public Drawable historyNoteCard() {
        return historyNoteCard;
    }

    /**
     * Builds the gold "active" badge (the design system's {@code Badge variant="active"}),
     * padded {@link Theme#SPACE_1} by {@link Theme#SPACE_2}.
     *
     * @param text the badge text
     * @return the badge
     */
    public Container<Label> activeBadge(String text) {
        Container<Label> badge = new Container<>(new Label(text, activeBadge));
        badge.setBackground(badgeFill);
        badge.pad(Theme.SPACE_1, Theme.SPACE_2, Theme.SPACE_1, Theme.SPACE_2);
        return badge;
    }

    /**
     * Returns the logo, drawn at 2&times; for its 150 px slot.
     *
     * @return the drawable
     */
    public Drawable logo() {
        return new TextureRegionDrawable(logo);
    }

    /**
     * Returns the history-note glyph, white so it can be tinted.
     *
     * @return the drawable
     */
    public Drawable historyNoteIcon() {
        return new TextureRegionDrawable(historyNoteIcon);
    }

    /**
     * Returns an archetype's card art.
     *
     * @param archetype the archetype
     * @return the drawable
     */
    public Drawable archetypeArt(Archetype archetype) {
        return new TextureRegionDrawable(archetypeArt.get(archetype));
    }

    /**
     * Releases the fonts, shape textures and images.
     */
    @Override
    public void dispose() {
        fonts.dispose();
        shapes.dispose();
        logo.dispose();
        historyNoteIcon.dispose();
        archetypeArt.values().forEach(Texture::dispose);
        icons.values().forEach(Texture::dispose);
        pictures.values().stream().filter(java.util.Objects::nonNull).forEach(Texture::dispose);
        if (backdrop != null) {
            backdrop.dispose();
        }
    }

    private static Texture smoothTexture(FileHandle file) {
        Texture texture = new Texture(file, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        return texture;
    }

    /**
     * Gives a field's boxes the inner padding of {@code .lw-field__control}.
     *
     * @param style the style from {@link Theme.Shapes#textField}
     * @return the same style
     */
    private static TextField.TextFieldStyle paddedField(TextField.TextFieldStyle style) {
        for (Drawable drawable : new Drawable[] {style.background, style.focusedBackground, style.disabledBackground}) {
            NinePatchDrawable box = (NinePatchDrawable) drawable;
            box.setLeftWidth(Theme.SPACE_3);
            box.setRightWidth(Theme.SPACE_3);
            box.setTopHeight(Theme.SPACE_2);
            box.setBottomHeight(Theme.SPACE_2);
        }
        return style;
    }
}
