package de.mkoehler.launchwindow.net.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A turn report, or why there's none. The report travels as the same JSON it's saved as
 * ({@link de.mkoehler.launchwindow.rules.TurnReport}), read with the persistence mapper on the
 * client, so its many event types need no wire registration of their own. The JSON is gzipped:
 * it's very repetitive, and a busy turn's report would otherwise outgrow Kryo's object buffer
 * ({@link de.mkoehler.launchwindow.net.NetworkConstants#OBJECT_BUFFER_SIZE}).
 *
 * @param success     whether the request was served (a missing report is still a success)
 * @param message     why it wasn't served, or {@code ""}
 * @param found       whether there's a report for the turn asked about
 * @param turn        the report's turn, or -1 when none was found
 * @param gzippedJson the report as gzipped UTF-8 JSON, or empty when none was found
 * @author Mario Koehler
 */
public record TurnReportResponse(boolean success, String message, boolean found, long turn, byte[] gzippedJson) {

    /**
     * Keeps a non-null payload.
     */
    public TurnReportResponse {
        gzippedJson = gzippedJson == null ? new byte[0] : gzippedJson;
    }

    /**
     * A report found.
     *
     * @param turn its turn
     * @param json the report as JSON
     * @return the response
     */
    public static TurnReportResponse of(long turn, String json) {
        Objects.requireNonNull(json, "json must not be null");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new TurnReportResponse(true, "", true, turn, bytes.toByteArray());
    }

    /**
     * No report for the turn asked about: none written yet, or the player joined later.
     *
     * @return the response
     */
    public static TurnReportResponse none() {
        return new TurnReportResponse(true, "", false, -1, new byte[0]);
    }

    /**
     * The request couldn't be served.
     *
     * @param message why
     * @return the response
     */
    public static TurnReportResponse failure(String message) {
        return new TurnReportResponse(false, message, false, -1, new byte[0]);
    }

    /**
     * The report as JSON.
     *
     * @return the unzipped JSON, or {@code ""} when none was found
     * @throws IOException if the payload isn't valid gzip
     */
    public String json() throws IOException {
        if (gzippedJson.length == 0) {
            return "";
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzippedJson))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TurnReportResponse that && success == that.success && found == that.found
            && turn == that.turn && message.equals(that.message) && Arrays.equals(gzippedJson, that.gzippedJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message, found, turn, Arrays.hashCode(gzippedJson));
    }

    @Override
    public String toString() {
        return "TurnReportResponse[success=" + success + ", message=" + message + ", found=" + found + ", turn="
            + turn + ", gzippedJson=" + gzippedJson.length + " bytes]";
    }
}
