package de.mkoehler.launchwindow.client.view;

import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.utils.ShortArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.Season;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The Space Complex hub's view logic ({@code ui-spec.md} &sect;4.3, {@code design.md} &sect;3.13):
 * its seven places, their hotspot polygons, which art to show, and the time-of-day clock.
 *
 * @author Mario Koehler
 */
public final class Hub {

    /** The art's size, which is also the virtual screen's. */
    public static final int ART_WIDTH = 1920;
    /** The art's height after the bottom crop; the hotspot files still say 1088. */
    public static final int ART_HEIGHT = 1080;
    /** How often re-entering the hub moves the time of day on: every 4 real minutes. */
    public static final long TIME_OF_DAY_STEP_MILLIS = 4 * 60 * 1000L;

    private static final ObjectMapper JSON = new ObjectMapper();

    private Hub() {
    }

    /**
     * The hub's buildings, named as in the hotspot files.
     */
    public enum Place {
        VAB("VAB", "The VAB", Building.VEHICLE_ASSEMBLY),
        MISSION_CONTROL("Mission Control", "Mission Control", Building.MISSION_CONTROL),
        ASTRONAUT_COMPLEX("Astronaut Complex", "The Astronaut Complex", Building.ASTRONAUT_CENTER),
        SET_CENTRE("SET Centre", "The SET Centre", Building.SET_CENTER),
        PUBLIC_AFFAIRS_OFFICE("Public Affairs Office", "The Public Affairs Office", null),
        HEADQUARTERS("Headquarters", "Headquarters", Building.HEADQUARTERS),
        MUSEUM("Museum", "The Museum", null);

        private final String label;
        private final String inSentence;
        private final Building building;

        Place(String label, String inSentence, Building building) {
            this.label = label;
            this.inSentence = inSentence;
            this.building = building;
        }

        /**
         * Returns the name shown on hover.
         *
         * @return the label
         */
        public String label() {
            return label;
        }

        /**
         * Returns the name as it starts a sentence ("The VAB isn't built yet.").
         *
         * @return the name
         */
        public String inSentence() {
            return inSentence;
        }

        /**
         * Returns the capped building this place is, if any.
         *
         * @return the building, or {@code null} for the Public Affairs Office and the Museum
         */
        public Building building() {
            return building;
        }
    }

    /**
     * The time of day the art shows, in the order the clock moves through them.
     */
    public enum TimeOfDay {
        DAWN, NOON, DUSK, NIGHT;

        /**
         * Returns the next time of day, night wrapping to dawn.
         *
         * @return the next one
         */
        public TimeOfDay next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /**
     * One place's hotspot, in screen coordinates (origin bottom-left, y up, as Scene2D draws).
     *
     * @param place     the place
     * @param vertices  the polygon, x and y alternating
     * @param triangles the polygon's triangulation, indices into its points (it may be concave)
     * @param labelX    where the hover label is centered, the polygon's centroid
     * @param labelY    as {@code labelX}
     */
    public record Hotspot(Place place, float[] vertices, short[] triangles, float labelX, float labelY) {

        /**
         * Returns whether a point lies inside the polygon.
         *
         * @param x the x, in screen coordinates
         * @param y the y, in screen coordinates
         * @return whether inside
         */
        public boolean contains(float x, float y) {
            return Intersector.isPointInPolygon(vertices, 0, vertices.length, x, y);
        }
    }

    /**
     * Reads an agency's hotspot file and turns it into screen coordinates. The file is in image
     * pixels with a top-left origin, y down; y flips against the cropped art's height.
     *
     * @param in the file's contents
     * @return one hotspot per place, in the file's order
     * @throws IOException              if the file can't be read
     * @throws IllegalArgumentException if a polygon names no known place
     */
    public static List<Hotspot> readHotspots(InputStream in) throws IOException {
        HotspotFile file = JSON.readValue(in, HotspotFile.class);
        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        List<Hotspot> hotspots = new ArrayList<>();
        for (HotspotFile.Polygon polygon : file.polygons()) {
            Place place = Place.valueOf(polygon.name());
            float[] vertices = new float[polygon.points().size() * 2];
            for (int i = 0; i < polygon.points().size(); i++) {
                vertices[2 * i] = (float) polygon.points().get(i).x();
                vertices[2 * i + 1] = ART_HEIGHT - (float) polygon.points().get(i).y();
            }
            ShortArray indices = triangulator.computeTriangles(vertices);
            float[] centroid = centroid(vertices);
            hotspots.add(new Hotspot(place, vertices, indices.toArray(), centroid[0], centroid[1]));
        }
        return hotspots;
    }

    /**
     * Returns the area centroid of a simple polygon.
     */
    private static float[] centroid(float[] v) {
        double area = 0;
        double cx = 0;
        double cy = 0;
        int n = v.length / 2;
        for (int i = 0; i < n; i++) {
            double x0 = v[2 * i];
            double y0 = v[2 * i + 1];
            double x1 = v[2 * ((i + 1) % n)];
            double y1 = v[2 * ((i + 1) % n) + 1];
            double cross = x0 * y1 - x1 * y0;
            area += cross;
            cx += (x0 + x1) * cross;
            cy += (y0 + y1) * cross;
        }
        area /= 2;
        return new float[] {(float) (cx / (6 * area)), (float) (cy / (6 * area))};
    }

    /**
     * Returns the hotspot file for an agency, under {@code assets/}.
     *
     * @param archetype the agency's archetype
     * @return the path
     */
    public static String hotspotPath(Archetype archetype) {
        return "ui/hub/" + archetype.name() + ".hotspots.json";
    }

    /**
     * Returns the art for an agency, a season and a time of day, under {@code assets/}.
     *
     * @param archetype the agency's archetype
     * @param season    the game's season
     * @param timeOfDay the time of day
     * @return the path
     */
    public static String artPath(Archetype archetype, Season season, TimeOfDay timeOfDay) {
        return "ui/hub/" + archetype.name() + "_" + season.name().toLowerCase(Locale.ROOT) + "_"
            + timeOfDay.name().toLowerCase(Locale.ROOT) + ".jpg";
    }

    /**
     * The time-of-day clock: purely client-side ambience, no server involved. Each entry to the hub
     * moves the art one step on if at least {@link #TIME_OF_DAY_STEP_MILLIS} have passed since the
     * last step (not since the last entry, so hopping between screens still sees it move), and
     * only on entry, never while the player sits on the hub. Lives as long as the client.
     */
    public static final class Clock {
        /** The time of day of the first entry. Unconfirmed default: the mockup's noon. */
        public static final TimeOfDay START = TimeOfDay.NOON;

        private TimeOfDay current;
        private long lastStepMillis;

        /**
         * Enters the hub at a moment and returns the time of day to show.
         *
         * @param nowMillis the wall-clock time
         * @return the time of day
         */
        public TimeOfDay enter(long nowMillis) {
            if (current == null) {
                current = START;
                lastStepMillis = nowMillis;
            } else if (nowMillis - lastStepMillis >= TIME_OF_DAY_STEP_MILLIS) {
                current = current.next();
                lastStepMillis = nowMillis;
            }
            return current;
        }

        /**
         * Returns the time of day last shown, without entering.
         *
         * @return the time of day, or {@link #START} before the first entry
         */
        public TimeOfDay current() {
            return Objects.requireNonNullElse(current, START);
        }
    }

    /** The hotspot file's shape, as the {@code HotspotEditor} dev tool writes it. */
    record HotspotFile(int imageWidth, int imageHeight, List<Polygon> polygons) {
        record Polygon(String name, List<Point> points) {
        }

        record Point(double x, double y) {
        }
    }
}
