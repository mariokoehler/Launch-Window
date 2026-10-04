package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import de.mkoehler.launchwindow.model.Archetype;

import java.util.EnumMap;
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

    /**
     * Loads the fonts and images and builds the shared styles.
     *
     * @param assets    the assets root, holding {@code fonts/}, {@code branding/} and {@code ui/}
     * @param fontScale the ratio of the real window height to {@value Theme#VIEW_HEIGHT}, so text stays sharp
     */
    public UiKit(FileHandle assets, float fontScale) {
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
