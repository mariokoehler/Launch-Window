package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * A row of small labels lined up over a {@link ReliabilityBar}'s markers (2026-10-10): each sits
 * centred above the reliability it names, as wide as the bar. Where two would overlap, the cap's
 * label wins, then the higher milestone; the other is hidden, and the tooltip still lists it.
 * Labels stay ordinary {@link Label}s, so they keep the fonts' pixel snapping.
 *
 * @author Mario Koehler
 */
public final class MilestoneLabels extends WidgetGroup {

    /** The least space between two labels. */
    private static final float GAP = 6f;

    private final float max;
    private final List<Entry> entries = new ArrayList<>();

    private record Entry(Label label, float value, boolean cap) {
    }

    /**
     * Builds an empty row for a bar.
     *
     * @param max the bar's end, {@link ReliabilityBar#max()}
     */
    public MilestoneLabels(float max) {
        this.max = Math.max(max, 0.0001f);
    }

    /**
     * Adds a label above a reliability.
     *
     * @param ui    the widget kit
     * @param value the reliability
     * @param text  the label, such as "~3"
     * @param color its colour
     * @param cap   whether it names the cap, which wins any overlap
     * @return this row
     */
    public MilestoneLabels add(UiKit ui, double value, String text, Color color, boolean cap) {
        Label label = ui.label(text, Theme.TextStyle.CAPTION, color);
        entries.add(new Entry(label, (float) value, cap));
        addActor(label);
        invalidateHierarchy();
        return this;
    }

    @Override
    public float getPrefHeight() {
        float height = 0;
        for (Entry entry : entries) {
            height = Math.max(height, entry.label().getPrefHeight());
        }
        return height;
    }

    @Override
    public float getPrefWidth() {
        return 200f;
    }

    @Override
    public void layout() {
        float width = getWidth();
        // Place the cap first, then milestones from the highest down; skip any that would overlap.
        List<Entry> order = new ArrayList<>(entries);
        order.sort((a, b) -> a.cap() != b.cap() ? (a.cap() ? -1 : 1) : Float.compare(b.value(), a.value()));
        List<float[]> placed = new ArrayList<>();
        for (Entry entry : order) {
            Label label = entry.label();
            float labelWidth = label.getPrefWidth();
            float centre = width * Math.min(1f, entry.value() / max);
            float x = Math.max(0f, Math.min(width - labelWidth, centre - labelWidth / 2f));
            boolean free = true;
            for (float[] other : placed) {
                if (x < other[1] + GAP && x + labelWidth + GAP > other[0]) {
                    free = false;
                    break;
                }
            }
            label.setVisible(free);
            if (free) {
                label.setBounds(x, 0f, labelWidth, label.getPrefHeight());
                placed.add(new float[] {x, x + labelWidth});
            }
        }
    }
}
