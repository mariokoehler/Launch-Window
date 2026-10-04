package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.CelestialBody;

import java.util.List;

/**
 * Where each celestial body sits on the solar-system art ({@code assets/ui/solar_system.jpg},
 * 1920 by 832), as percentages of the art, taken from the Headquarters mockup: the body's hit
 * ellipse (left, top, width, height) and where its label chip is centered.
 *
 * @author Mario Koehler
 */
public final class BodyMap {

    /** The art's width divided by its height. */
    public static final float ASPECT = 1920f / 832f;

    /**
     * One body's place on the art, in percent of the art's size.
     *
     * @param body   the body
     * @param left   the hit ellipse's left edge
     * @param top    the hit ellipse's top edge (from the top)
     * @param width  the hit ellipse's width
     * @param height the hit ellipse's height
     * @param labelX the label chip's center, horizontally
     * @param labelY the label chip's top edge (from the top)
     */
    public record Target(CelestialBody body, float left, float top, float width, float height, float labelX,
                         float labelY) {
    }

    /** Every body, as placed on the art. */
    public static final List<Target> TARGETS = List.of(
        new Target(CelestialBody.SUN, 0f, 12f, 9.5f, 76f, 4.6f, 89f),
        new Target(CelestialBody.MERCURY, 18.0f, 45.2f, 4.17f, 9.6f, 20.1f, 57f),
        new Target(CelestialBody.VENUS, 25.3f, 43.8f, 5.2f, 12.0f, 27.9f, 58f),
        new Target(CelestialBody.EARTH, 33.75f, 44.5f, 4.79f, 11.06f, 36.15f, 58f),
        new Target(CelestialBody.MOON, 37.6f, 42.07f, 2.08f, 4.81f, 38.65f, 31f),
        new Target(CelestialBody.MARS, 44.69f, 44.23f, 5.0f, 11.54f, 47.19f, 58f),
        new Target(CelestialBody.JUPITER, 57.71f, 33.41f, 14.17f, 32.69f, 64.79f, 68f),
        new Target(CelestialBody.SATURN, 77.08f, 38.46f, 16.35f, 24.04f, 85.26f, 64f));

    /**
     * Not instantiable; this class only holds the map.
     */
    private BodyMap() {
    }

    /**
     * Returns a body's name as shown.
     *
     * @param body the body
     * @return for example "Earth"
     */
    public static String name(CelestialBody body) {
        String lower = body.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
