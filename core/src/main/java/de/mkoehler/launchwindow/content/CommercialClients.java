package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * <p>The contract board's clients ({@code design.md} &sect;2.11): who orders a flight in each line,
 * per side, with a title and a short story. The market draws its listings from them, so they are one
 * of each pack's rules files in {@link ContentPackLoader#FILES}: hashed and frozen with each game's pack (review.md 2.2), read
 * through the game's {@link ContentCatalog#rules()}. Listings store client ids, so an
 * id is never renamed or reused.</p>
 *
 * <p>The Soviet side has no commercial market; its clients are ministries and institutes ordering a
 * service, under the same rules.</p>
 *
 * @param clients every client
 * @author Mario Koehler
 */
public record CommercialClients(List<Client> clients) {

    /** The file's name, in each pack's folder. */
    public static final String FILE = "commercial-clients.json";

    /** No clients: what a missing file reads as. */
    public static final CommercialClients EMPTY = new CommercialClients(List.of());

    private static volatile CommercialClients standard;

    /**
     * One client's order.
     *
     * @param id     a stable id, never reused, since listings store it
     * @param lineId the contract line, a program category
     * @param side   the side that sees it
     * @param name   the client ("Transatlantic Telephone Co.", "Ministry of Communications")
     * @param title  the order's title ("Transatlantic relay")
     * @param story  two or three sentences
     */
    public record Client(String id, String lineId, Archetype side, String name, String title, String story) {

        /**
         * Checks the fields.
         *
         * @throws NullPointerException if a field is {@code null}
         */
        public Client {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(lineId, "lineId must not be null");
            Objects.requireNonNull(side, "side must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(story, "story must not be null");
        }
    }

    /**
     * Copies the list.
     *
     * @param clients the clients
     */
    public CommercialClients(@JsonProperty("clients") List<Client> clients) {
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    /**
     * Returns the client's clients, for its screens: the game's pack's, once a session installed them
     * ({@link #setStandard}), else the standard pack's on the classpath, read once. The rules never read
     * these: they go by the game's {@link ContentCatalog#rules()}.
     *
     * @return the clients, or {@link #EMPTY} if the file isn't on the classpath
     */
    public static CommercialClients standard() {
        CommercialClients loaded = standard;
        if (loaded == null) {
            try (InputStream in = CommercialClients.class.getClassLoader().getResourceAsStream(
                    "content/" + ContentPackLoader.CLIENT_DEFAULT_PACK + "/" + FILE)) {
                loaded = in == null ? EMPTY : read(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Replaces the clients {@link #standard()} returns: the game's pack's, installed by the client's
     * session, or a tool's own.
     *
     * @param clients the clients
     */
    public static void setStandard(CommercialClients clients) {
        standard = Objects.requireNonNull(clients, "clients must not be null");
    }

    /**
     * Reads the clients from a file.
     *
     * @param file the file
     * @return the clients
     * @throws UncheckedIOException if the file can't be read
     */
    public static CommercialClients load(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return read(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Reads the clients from a stream, as a pack's file.
     *
     * @param in the stream, left open
     * @return the clients
     * @throws IOException if it can't be read or isn't valid
     */
    public static CommercialClients read(InputStream in) throws IOException {
        return new ObjectMapper().readValue(in, CommercialClients.class);
    }

    /**
     * Returns a client by its id.
     *
     * @param id the id, or {@code null}
     * @return the client, or empty
     */
    public Optional<Client> byId(String id) {
        return clients.stream().filter(client -> client.id().equals(id)).findFirst();
    }

    /**
     * Picks a client for a new listing in a line, one the board hasn't used yet, and records it as
     * used. Once every client of the line and side has run, they start over.
     *
     * @param lineId the line
     * @param side   the side
     * @param random the random source
     * @param used   the ids already used; mutated in place
     * @return the client's id, or {@code null} if the line has none for that side
     */
    public String pick(String lineId, Archetype side, RandomGenerator random, List<String> used) {
        Objects.requireNonNull(used, "used must not be null");
        List<Client> ofLine = clients.stream()
            .filter(client -> client.lineId().equals(lineId) && client.side() == side).toList();
        if (ofLine.isEmpty()) {
            return null;
        }
        List<Client> fresh = ofLine.stream().filter(client -> !used.contains(client.id())).toList();
        if (fresh.isEmpty()) {
            ofLine.forEach(client -> used.remove(client.id()));
            fresh = ofLine;
        }
        String id = fresh.get(random.nextInt(fresh.size())).id();
        used.add(id);
        return id;
    }
}
