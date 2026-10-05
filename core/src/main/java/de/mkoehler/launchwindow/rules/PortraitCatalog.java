package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The personnel portraits a candidate can be given, read from the classpath resource
 * {@code personnel/portraits.json}. That file is written by {@code assets-raw/portraits/make_portraits.py}
 * from the portrait manifest (never edited by hand): each portrait's agency, personnel group, sex,
 * ethnicity key (into {@code personnel/names.json}, see {@link PersonnelNames}) and the age range the
 * person in it looks.
 *
 * @author Mario Koehler
 */
public final class PortraitCatalog {

    /** The classpath resource the catalog is read from. */
    public static final String RESOURCE = "personnel/portraits.json";

    private static final List<Portrait> PORTRAITS = load();

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PortraitCatalog() {
    }

    /**
     * Returns every portrait, sorted by id.
     *
     * @return the portraits
     */
    public static List<Portrait> all() {
        return PORTRAITS;
    }

    /**
     * Returns the portraits for one agency and personnel group, sorted by id.
     *
     * @param archetype the agency's archetype
     * @param group     the personnel group
     * @return the matching portraits
     */
    public static List<Portrait> of(Archetype archetype, PersonnelGroup group) {
        List<Portrait> result = new ArrayList<>();
        for (Portrait portrait : PORTRAITS) {
            if (portrait.archetype() == archetype && portrait.group() == group) {
                result.add(portrait);
            }
        }
        return result;
    }

    /**
     * Returns one portrait by id.
     *
     * @param id the portrait id, such as {@code nasa_001}
     * @return the portrait, or {@code null} if there is none with that id
     */
    public static Portrait byId(String id) {
        for (Portrait portrait : PORTRAITS) {
            if (portrait.id().equals(id)) {
                return portrait;
            }
        }
        return null;
    }

    private static List<Portrait> load() {
        try (InputStream in = PortraitCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("no classpath resource " + RESOURCE);
            }
            List<Portrait> portraits = new ObjectMapper().readValue(in, new TypeReference<List<Portrait>>() { });
            return portraits.stream().sorted((a, b) -> a.id().compareTo(b.id())).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + RESOURCE, e);
        }
    }

    /**
     * Whether the person in a portrait is a man or a woman, which picks their first name and,
     * for gendered surnames, its form.
     */
    public enum Sex {
        MALE,
        FEMALE
    }

    /**
     * One portrait.
     *
     * @param id        the portrait id, the image's file name without extension
     * @param archetype the agency it was made for
     * @param group     the personnel group it shows (astronauts wear flight gear, for example)
     * @param sex       the person's sex
     * @param ethnicity the key of the person's name lists in {@code personnel/names.json}
     * @param minAge    the youngest age the person looks
     * @param maxAge    the oldest age the person looks
     */
    public record Portrait(String id, Archetype archetype, PersonnelGroup group, Sex sex, String ethnicity,
                           int minAge, int maxAge) {

        /**
         * Checks the fields.
         */
        public Portrait {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(archetype, "archetype must not be null");
            Objects.requireNonNull(group, "group must not be null");
            Objects.requireNonNull(sex, "sex must not be null");
            Objects.requireNonNull(ethnicity, "ethnicity must not be null");
            if (minAge > maxAge) {
                throw new IllegalArgumentException("minAge above maxAge for " + id);
            }
        }
    }
}
