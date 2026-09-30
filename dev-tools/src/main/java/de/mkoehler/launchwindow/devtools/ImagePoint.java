package de.mkoehler.launchwindow.devtools;

/**
 * A single polygon vertex in image-pixel space: top-left origin, Y
 * increasing downward - exactly how the source PNG's own pixels are
 * indexed. Any flip needed for a Y-up rendering convention (e.g. libGDX,
 * see {@code MenuHotspotPrototype}) happens on the loading side, not here.
 * <p>
 * A mutable bean with a no-arg constructor, matching every other class in
 * this project that Jackson reads from a JSON file directly.
 *
 * @author Mario Koehler
 */
public class ImagePoint {

    private double x;
    private double y;

    /**
     * Creates an uninitialized instance, for Jackson.
     */
    public ImagePoint() {
    }

    /**
     * Creates a new instance with the given coordinates.
     *
     * @param x the x coordinate, in image pixels
     * @param y the y coordinate, in image pixels
     */
    public ImagePoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the x coordinate, in image pixels.
     *
     * @return the x coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Sets the x coordinate, in image pixels.
     *
     * @param x the new x coordinate
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Returns the y coordinate, in image pixels.
     *
     * @return the y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the y coordinate, in image pixels.
     *
     * @param y the new y coordinate
     */
    public void setY(double y) {
        this.y = y;
    }
}
