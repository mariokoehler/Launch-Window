package de.mkoehler.launchwindow.devtools;

import java.util.ArrayList;
import java.util.List;

/**
 * A single named hotspot polygon: a hand-authored building outline, as
 * {@link ImagePoint} (image-pixel-space) vertices, plus the name a menu
 * screen would use to identify which building was clicked/hovered.
 * <p>
 * A mutable bean with a no-arg constructor, matching every other class in
 * this project that Jackson reads from a JSON file directly.
 *
 * @author Mario Koehler
 */
public class NamedPolygon {

    private String name = "";
    private List<ImagePoint> points = new ArrayList<>();

    /**
     * Returns this polygon's name.
     *
     * @return the name, never {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Sets this polygon's name.
     *
     * @param name the new name; {@code null} is normalized to empty
     */
    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    /**
     * Returns this polygon's vertices, in image-pixel space, in order.
     *
     * @return the vertex list, never {@code null}
     */
    public List<ImagePoint> getPoints() {
        return points;
    }

    /**
     * Replaces this polygon's vertex list, copying the given list
     * defensively.
     *
     * @param points the new vertex list; {@code null} is treated as empty
     */
    public void setPoints(List<ImagePoint> points) {
        this.points = points == null ? new ArrayList<>() : new ArrayList<>(points);
    }

    @Override
    public String toString() {
        return name + "  (" + points.size() + " pts)";
    }
}
