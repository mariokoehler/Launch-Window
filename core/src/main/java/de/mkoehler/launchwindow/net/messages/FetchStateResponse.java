package de.mkoehler.launchwindow.net.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <p>The server's reply to a {@link FetchStateRequest}.</p>
 *
 * <p>The state travels as gzipped JSON, as the turn report does (2026-10-07): sent as an object
 * graph it grew with every roster, rapport and intelligence folder toward Kryo's 64 KB object
 * buffer (a full agency measured 34 KB, before phase 4's career records), and an oversized message
 * drops the connection. Zipped it is a fraction of that.</p>
 *
 * @param success       whether the state was retrieved
 * @param message       a human-readable message - empty on success, a rejection
 *                      reason on failure
 * @param gzippedState  the player's current state as gzipped JSON, empty on failure
 * @author Mario Koehler
 */
public record FetchStateResponse(boolean success, String message, byte[] gzippedState) {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Keeps a non-null payload.
     */
    public FetchStateResponse {
        gzippedState = gzippedState == null ? new byte[0] : gzippedState;
    }

    /**
     * Creates a successful response.
     *
     * @param state the player's current state
     * @return the successful response
     */
    public static FetchStateResponse success(PlayerSnapshot state) {
        Objects.requireNonNull(state, "state must not be null");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(JSON.writeValueAsBytes(state));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not pack the player's state", e);
        }
        return new FetchStateResponse(true, "", bytes.toByteArray());
    }

    /**
     * Creates a rejected response.
     *
     * @param message a human-readable rejection reason
     * @return the failed response
     */
    public static FetchStateResponse failure(String message) {
        return new FetchStateResponse(false, message, new byte[0]);
    }

    /**
     * Unpacks the player's state. Each call unpacks a fresh copy.
     *
     * @return the state, or {@code null} on failure
     * @throws UncheckedIOException if the payload isn't valid gzipped JSON
     */
    public PlayerSnapshot state() {
        if (gzippedState.length == 0) {
            return null;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzippedState))) {
            return JSON.readValue(gzip.readAllBytes(), PlayerSnapshot.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not unpack the player's state", e);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FetchStateResponse that && success == that.success
            && Objects.equals(message, that.message) && Arrays.equals(gzippedState, that.gzippedState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message, Arrays.hashCode(gzippedState));
    }

    @Override
    public String toString() {
        return "FetchStateResponse[success=" + success + ", message=" + message + ", gzippedState="
            + gzippedState.length + " bytes]";
    }
}
