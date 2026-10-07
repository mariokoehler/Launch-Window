package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Align;
import de.mkoehler.launchwindow.client.view.Explainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An explainer's drawing ({@code ui-spec.md} component 18): the picture without its text, and the
 * text set over it as real labels, so it stays as sharp as the rest of the screen at any window
 * size. Each label sits on the baseline and anchor the drawing's source gave it.
 *
 * @author Mario Koehler
 */
public final class DrawingView extends WidgetGroup {

    /** The sizes the drawing's bold Archivo text comes in, for a source that sets its own size. */
    private static final Map<Integer, Theme.TextStyle> BOLD_SIZES = Map.of(
        13, Theme.TextStyle.DRAWING_LABEL_SM, 14, Theme.TextStyle.DRAWING_LABEL_MD, 15, Theme.TextStyle.DRAWING_LABEL,
        21, Theme.TextStyle.POINT_TITLE, 26, Theme.TextStyle.DRAWING_BIG);

    private final Explainers.Drawing drawing;
    private final Image picture;
    private final List<Label> labels = new ArrayList<>();

    /**
     * Builds a drawing.
     *
     * @param ui      the kit, for the picture and the fonts
     * @param name    the drawing's name under {@link Explainers#DRAWINGS}
     * @param drawing its text
     */
    public DrawingView(UiKit ui, String name, Explainers.Drawing drawing) {
        this.drawing = drawing;
        picture = new Image(ui.picture("explainers/" + name + ".png"));
        addActor(picture);
        for (Explainers.DrawingLabel each : drawing.labels()) {
            Theme.TextStyle style = style(each);
            boolean upper = "d-sm".equals(each.kind()) || "d-head".equals(each.kind());
            Label label = ui.label(upper ? each.text().toUpperCase(Locale.ROOT) : each.text(), style,
                Color.valueOf(each.color()));
            label.setAlignment(Align.bottomLeft);
            labels.add(label);
            addActor(label);
        }
    }

    /**
     * Returns the type style a piece of the drawing's text is set in.
     *
     * @param label the text
     * @return the style
     */
    static Theme.TextStyle style(Explainers.DrawingLabel label) {
        return switch (label.kind()) {
            case "d-txt" -> Theme.TextStyle.DRAWING_TEXT;
            case "d-sm" -> Theme.TextStyle.DRAWING_SMALL;
            case "d-body" -> Theme.TextStyle.CAPTION_LG;
            default -> {
                Theme.TextStyle own = switch (label.kind()) {
                    case "d-head" -> Theme.TextStyle.DRAWING_HEAD;
                    case "d-big" -> Theme.TextStyle.DRAWING_BIG;
                    default -> Theme.TextStyle.DRAWING_LABEL;
                };
                yield label.size() > 0 ? closestBold(label.size()) : own;
            }
        };
    }

    private static Theme.TextStyle closestBold(float size) {
        int best = BOLD_SIZES.keySet().stream()
            .min((a, b) -> Float.compare(Math.abs(a - size), Math.abs(b - size))).orElseThrow();
        return BOLD_SIZES.get(best);
    }

    @Override
    public float getPrefWidth() {
        return drawing.width();
    }

    @Override
    public float getPrefHeight() {
        return drawing.height();
    }

    @Override
    public void layout() {
        float scale = getWidth() / drawing.width();
        picture.setBounds(0, 0, getWidth(), getHeight());
        for (int i = 0; i < labels.size(); i++) {
            Label label = labels.get(i);
            Explainers.DrawingLabel source = drawing.labels().get(i);
            label.setSize(label.getPrefWidth(), label.getPrefHeight());
            float x = source.x() * scale;
            switch (source.anchor()) {
                case "middle" -> x -= label.getWidth() / 2f;
                case "end" -> x -= label.getWidth();
                default -> {
                }
            }
            // Bottom-aligned, a label's text sits its font's descent above the label's bottom.
            float baseline = getHeight() - source.y() * scale;
            label.setPosition(x, baseline + label.getStyle().font.getDescent());
        }
    }
}
