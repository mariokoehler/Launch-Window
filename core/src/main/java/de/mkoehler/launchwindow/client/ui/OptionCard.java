package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

/**
 * The design system's {@code OptionCard}: a large, clickable choice with a 16:9 picture, a
 * title and a one-line description. Selected, it gets a gold outline, a gold-tinted body and a
 * "Selected" badge in the top-right corner.
 *
 * @author Mario Koehler
 */
public final class OptionCard extends Stack {

    private final UiKit ui;
    private final Table frame = new Table();
    private final Container<?> badge;
    private boolean selected;

    /**
     * Builds the card.
     *
     * @param ui          the widget kit
     * @param picture     the picture, shown 16:9 and cropped to fill
     * @param title       the title
     * @param description the one-line description
     * @param width       the card's full width, outline included
     * @param onClick     runs when the card is clicked
     */
    public OptionCard(UiKit ui, Drawable picture, String title, String description, float width, Runnable onClick) {
        this.ui = ui;
        float inner = width - 2 * Theme.BORDER_EMPHASIS;
        Image image = new Image(picture, Scaling.fill);
        Table body = new Table();
        body.pad(Theme.SPACE_4);
        body.defaults().left().growX();
        body.add(ui.label(title, Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_1).row();
        body.add(ui.paragraph(description, Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
        frame.add(image).width(inner).height(inner * 9f / 16f).row();
        frame.add(body).width(inner);
        add(frame);

        badge = new Container<>(ui.activeBadge("Selected"));
        badge.align(Align.topRight).pad(Theme.SPACE_2);
        badge.setTouchable(Touchable.disabled);
        add(badge);

        setTouchable(Touchable.enabled);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        });
        setSelected(false);
    }

    /**
     * Returns whether the card is selected.
     *
     * @return whether selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Selects or deselects the card.
     *
     * @param selected whether selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        frame.setBackground(ui.optionCard(selected));
        frame.pad(selected ? Theme.BORDER_EMPHASIS : Theme.BORDER_THIN);
        badge.setVisible(selected);
        invalidateHierarchy();
    }
}
