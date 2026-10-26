package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The words of the roads to the Moon ({@code ui-spec.md} &sect;4.21, {@code design.md} &sect;2.3(e)): for each
 * Moon program that lands, its Earth-orbit tests, its mode, how it works, its trade-offs and its history; for
 * the ones that only fly round or orbit the Moon, a line. Every number the screen shows comes from the rules
 * ({@link RoadsToMoon}); only words live in {@code assets/content/moon-roads.json}, outside the frozen pack.
 * The screen never ranks the roads: they show in this file's order.
 *
 * @param firsts         the goals shown as "Firsts on the way", in chip order
 * @param roads          the roads that land, in column order
 * @param withoutLanding the roads that reach the Moon without landing, in strip order
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record MoonRoads(List<String> firsts, List<Road> roads, List<WithoutLanding> withoutLanding) {

    /** The roads file, under the assets' {@code content/} folder. */
    public static final String FILE = "moon-roads.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile MoonRoads standard;

    /**
     * Copies the lists; missing ones read as empty.
     */
    public MoonRoads {
        firsts = firsts == null ? List.of() : List.copyOf(firsts);
        roads = roads == null ? List.of() : List.copyOf(roads);
        withoutLanding = withoutLanding == null ? List.of() : List.copyOf(withoutLanding);
    }

    /** How a road gets its crew down and home (the explainer "Three ways to the Moon"). */
    public enum Mode {
        /** One ship lands whole and comes back. */
        DIRECT_ASCENT("Direct ascent", "mode_direct"),
        /** Two launches meet in Earth orbit. */
        EARTH_ORBIT_RENDEZVOUS("Earth-orbit rendezvous", "mode_eor"),
        /** A lander leaves the mothership in lunar orbit. */
        LUNAR_ORBIT_RENDEZVOUS("Lunar-orbit rendezvous", "mode_lor");

        private final String label;
        private final String icon;

        Mode(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        /**
         * Returns the mode's name, as its badge shows it.
         *
         * @return the name
         */
        public String label() {
            return label;
        }

        /**
         * Returns the mode's glyph, a UI icon's name.
         *
         * @return the icon
         */
        public String icon() {
            return icon;
        }
    }

    /** How far a road that doesn't land goes. */
    public enum Reach {
        /** Round the Moon and home. */
        FLYBY("Flyby only"),
        /** Into lunar orbit and home. */
        LUNAR_ORBIT("Lunar orbit only");

        private final String label;

        Reach(String label) {
            this.label = label;
        }

        /**
         * Returns how the strip names it.
         *
         * @return the name
         */
        public String label() {
            return label;
        }
    }

    /**
     * A road that lands.
     *
     * @param program     the Moon program
     * @param earthOrbit  the Earth-orbit tests that make its Moon flights free to open
     * @param mode        how it gets down and home
     * @param historical  whether it's the agency's historical choice, which the screen tags quietly
     * @param howItWorks  one sentence
     * @param pros        what speaks for it, the "For" list
     * @param cons        what it asks of you, the "Against" list
     * @param historyNote its real story, for the history note
     */
    public record Road(String program, String earthOrbit, Mode mode, boolean historical, String howItWorks,
                       @JsonProperty("for") List<String> pros, @JsonProperty("against") List<String> cons,
                       String historyNote) {

        /**
         * Checks the ids and copies the lists.
         */
        public Road {
            Objects.requireNonNull(program, "program must not be null");
            Objects.requireNonNull(earthOrbit, "earthOrbit must not be null");
            Objects.requireNonNull(mode, "mode must not be null");
            pros = pros == null ? List.of() : List.copyOf(pros);
            cons = cons == null ? List.of() : List.copyOf(cons);
        }
    }

    /**
     * A road that reaches the Moon without landing.
     *
     * @param program    the Moon program
     * @param earthOrbit its Earth-orbit tests
     * @param reach      how far it goes
     * @param line       one line about it, never framed as a mistake
     */
    public record WithoutLanding(String program, String earthOrbit, Reach reach, String line) {

        /**
         * Checks the ids.
         */
        public WithoutLanding {
            Objects.requireNonNull(program, "program must not be null");
            Objects.requireNonNull(earthOrbit, "earthOrbit must not be null");
            Objects.requireNonNull(reach, "reach must not be null");
        }
    }

    /**
     * Returns the roads bundled with the game, read once from the classpath.
     *
     * @return the roads, or none if the file isn't there
     */
    public static MoonRoads standard() {
        MoonRoads loaded = standard;
        if (loaded == null) {
            try (InputStream in = MoonRoads.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? new MoonRoads(List.of(), List.of(), List.of()) : MAPPER.readValue(in,
                    MoonRoads.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read the roads to the Moon", e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Reads roads from JSON text.
     *
     * @param json the text
     * @return the roads
     */
    public static MoonRoads parse(String json) {
        try {
            return MAPPER.readValue(json, MoonRoads.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the roads to the Moon", e);
        }
    }

    /**
     * Returns the road a program belongs to, as its Moon program or its Earth-orbit tests, if any.
     *
     * @param programId the program
     * @return the road
     */
    public Optional<Road> roadOf(String programId) {
        return roads.stream().filter(road -> road.program().equals(programId) || road.earthOrbit().equals(programId))
            .findFirst();
    }

    /**
     * Returns whether a program is part of the comparison: a road, a road that doesn't land, or either's tests.
     *
     * @param programId the program
     * @return whether it is
     */
    public boolean covers(String programId) {
        return Stream.concat(roads.stream().flatMap(road -> Stream.of(road.program(), road.earthOrbit())),
                withoutLanding.stream().flatMap(road -> Stream.of(road.program(), road.earthOrbit())))
            .anyMatch(programId::equals);
    }
}
