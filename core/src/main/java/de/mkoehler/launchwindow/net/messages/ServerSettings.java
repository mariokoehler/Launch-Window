package de.mkoehler.launchwindow.net.messages;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <p>The server's settings as the admin sees and edits them ({@code ui-spec.md} &sect;4.22, G21): every field of
 * {@code game-meta.json} but the admin password, plus what the dialog needs to show them (the open turn, the next
 * resolution, which fields are locked and why, the packs the server bundles). Sent as gzipped JSON inside
 * {@link ServerSettingsResponse} and {@link SaveServerSettingsRequest}, so none of its types need registering with
 * Kryo.</p>
 *
 * <p>A save sends the same record back with the admin's changes; the server reads only the fields that aren't
 * locked and ignores the rest (the status, the locks, the packs).</p>
 *
 * @param version          the file's hash as read; a save carries it back, and is refused if the file changed since
 * @param openTurn         the game turn that's open
 * @param agencies         how many agencies are in the game, bots included
 * @param nextResolution   the next resolution, epoch millis, or -1 if none is scheduled
 * @param activeDays       the weekdays a turn resolves on, as {@code java.time.DayOfWeek} names
 * @param resolutionTime   the time of day, HH:MM
 * @param timezone         the zone id
 * @param breaks           the blackout periods
 * @param contentPack      the pack's folder name
 * @param declaredGoalIds  the goals that win the race
 * @param gameSeed         the seed, or {@code null} once it's hidden (locked)
 * @param devMode          dev mode
 * @param devArchiveOpen   every archive page open, in dev mode
 * @param bots             the filler bots
 * @param packLock         why the pack and seed can't change any more, or {@code null} while they can
 * @param goalsLock        why the declared goals can't change any more, or {@code null} while they can
 * @param packs            the packs the server bundles
 * @author Mario Koehler
 */
public record ServerSettings(String version, long openTurn, int agencies, long nextResolution,
                             List<String> activeDays, String resolutionTime, String timezone, List<Break> breaks,
                             String contentPack, List<String> declaredGoalIds, Long gameSeed, boolean devMode,
                             boolean devArchiveOpen, List<Bot> bots, String packLock, String goalsLock,
                             List<Pack> packs) {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Copies the lists, so a record never shares one.
     */
    public ServerSettings {
        activeDays = activeDays == null ? List.of() : List.copyOf(activeDays);
        breaks = breaks == null ? List.of() : List.copyOf(breaks);
        declaredGoalIds = declaredGoalIds == null ? List.of() : List.copyOf(declaredGoalIds);
        bots = bots == null ? List.of() : List.copyOf(bots);
        packs = packs == null ? List.of() : List.copyOf(packs);
    }

    /**
     * One blackout period, inclusive.
     *
     * @param start the first day, YYYY-MM-DD
     * @param end   the last day, YYYY-MM-DD
     */
    public record Break(String start, String end) {
    }

    /**
     * One filler bot.
     *
     * @param id           its id, {@code bot-} and a name
     * @param archetype    NASA or SOVIET
     * @param agencyName   its agency's name, or empty to draw one
     * @param directorName its director's name, or empty to draw one
     * @param sinceTurn    the turn its agency was founded, or 0 if it has none yet (then it's still editable)
     */
    public record Bot(String id, String archetype, String agencyName, String directorName, long sinceTurn) {
    }

    /**
     * A pack the server bundles.
     *
     * @param id           its folder name
     * @param goalIds      its goals, in the pack's order
     * @param defaultGoals the race a game on it declares by default
     */
    public record Pack(String id, List<String> goalIds, List<String> defaultGoals) {

        /**
         * Copies the lists.
         */
        public Pack {
            goalIds = goalIds == null ? List.of() : List.copyOf(goalIds);
            defaultGoals = defaultGoals == null ? List.of() : List.copyOf(defaultGoals);
        }
    }

    /**
     * Returns the pack with an id.
     *
     * @param id the pack's id
     * @return the pack, or {@code null} if the server doesn't bundle it
     */
    public Pack pack(String id) {
        return packs.stream().filter(pack -> pack.id().equals(id)).findFirst().orElse(null);
    }

    /**
     * Returns whether the pack and seed are locked.
     *
     * @return whether they are
     */
    public boolean packLocked() {
        return packLock != null;
    }

    /**
     * Returns whether the declared goals are locked.
     *
     * @return whether they are
     */
    public boolean goalsLocked() {
        return goalsLock != null;
    }

    /**
     * Writes a value as gzipped JSON, for the wire.
     *
     * @param value the value
     * @return the bytes
     */
    public static byte[] gzip(Object value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            MAPPER.writeValue(gzip, value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /**
     * Reads a value written by {@link #gzip(Object)}.
     *
     * @param bytes the bytes
     * @param type  the value's type
     * @param <T>   the type
     * @return the value, or {@code null} for no bytes
     * @throws UncheckedIOException if they don't read as one
     */
    public static <T> T gunzip(byte[] bytes, Class<T> type) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return MAPPER.readValue(gzip, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The fields a save refused, keyed as the dialog keys its fields ({@code days}, {@code time}, {@code timezone},
     * {@code breaks.N}, {@code pack}, {@code goals}, {@code bots.N.agency}, {@code bots.N.director},
     * {@code password}), each with the server's words.
     *
     * @param fields the refused fields
     */
    public record FieldErrors(Map<String, String> fields) {

        /**
         * Copies the map.
         */
        public FieldErrors {
            fields = fields == null ? Map.of() : Map.copyOf(fields);
        }
    }
}
