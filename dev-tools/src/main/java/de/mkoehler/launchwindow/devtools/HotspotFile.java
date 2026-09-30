package de.mkoehler.launchwindow.devtools;

import java.util.ArrayList;
import java.util.List;

/**
 * The on-disk shape of a {@code <image>.hotspots.json} sidecar file
 * produced by {@link HotspotEditor}: the named building polygons authored
 * for one specific menu image, plus that image's own pixel dimensions so a
 * loader (or the editor itself, reopening the file later) can detect a
 * mismatched/replaced source image rather than silently misplacing every
 * hotspot.
 * <p>
 * A mutable bean with a no-arg constructor, matching every other class in
 * this project that Jackson reads from a JSON file directly.
 *
 * @author Mario Koehler
 */
public class HotspotFile {

    private int imageWidth;
    private int imageHeight;
    private List<NamedPolygon> polygons = new ArrayList<>();

    /**
     * Returns the width, in pixels, of the image this file was authored
     * against.
     *
     * @return the image width
     */
    public int getImageWidth() {
        return imageWidth;
    }

    /**
     * Sets the width, in pixels, of the image this file was authored
     * against.
     *
     * @param imageWidth the new image width
     */
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    /**
     * Returns the height, in pixels, of the image this file was authored
     * against.
     *
     * @return the image height
     */
    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * Sets the height, in pixels, of the image this file was authored
     * against.
     *
     * @param imageHeight the new image height
     */
    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    /**
     * Returns the named polygons in this file.
     *
     * @return the polygon list, never {@code null}
     */
    public List<NamedPolygon> getPolygons() {
        return polygons;
    }

    /**
     * Replaces the polygon list, copying the given list defensively.
     *
     * @param polygons the new polygon list; {@code null} is treated as empty
     */
    public void setPolygons(List<NamedPolygon> polygons) {
        this.polygons = polygons == null ? new ArrayList<>() : new ArrayList<>(polygons);
    }
}
