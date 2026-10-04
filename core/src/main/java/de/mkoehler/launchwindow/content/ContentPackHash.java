package de.mkoehler.launchwindow.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The fingerprint of a content pack: a SHA-256 over every file in {@link ContentPackLoader#FILES},
 * in that order, each as its name followed by its text. The server sends it with the game info
 * (gap G1) so a client can tell whether its own copy of the pack is exactly the game's frozen one
 * ({@code design.md} &sect;5 M5, "Content packs").
 *
 * <p>Line endings and a leading byte-order mark don't count: text is hashed with CRLF turned into
 * LF and any BOM removed. A Git checkout on Windows can give the same file either ending, and two
 * copies that load identically must hash identically.</p>
 *
 * @author Mario Koehler
 */
public final class ContentPackHash {

    private static final byte[] SEPARATOR = {0};

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private ContentPackHash() {
    }

    /**
     * Hashes a pack in a folder on disk.
     *
     * @param packDirectory the folder holding the pack's files
     * @return the hash, as 64 lower-case hex digits
     * @throws UncheckedIOException if a file is missing or unreadable
     */
    public static String of(Path packDirectory) {
        Objects.requireNonNull(packDirectory, "packDirectory must not be null");
        return of(fileName -> Files.readAllBytes(packDirectory.resolve(fileName)));
    }

    /**
     * Hashes a pack on the classpath.
     *
     * @param resourcePrefix the pack's folder on the classpath, such as {@code content/early-era}
     * @param classLoader    the class loader to read through
     * @return the hash, as 64 lower-case hex digits
     * @throws UncheckedIOException if a file is missing or unreadable
     */
    public static String ofClasspath(String resourcePrefix, ClassLoader classLoader) {
        Objects.requireNonNull(resourcePrefix, "resourcePrefix must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        return of(fileName -> {
            try (InputStream in = classLoader.getResourceAsStream(resourcePrefix + "/" + fileName)) {
                if (in == null) {
                    throw new IOException("no classpath resource " + resourcePrefix + "/" + fileName);
                }
                return in.readAllBytes();
            }
        });
    }

    /**
     * Hashes a pack whose files a reader supplies.
     *
     * @param reader reads one file of the pack by name
     * @return the hash, as 64 lower-case hex digits
     * @throws UncheckedIOException if a file can't be read
     */
    public static String of(FileReader reader) {
        MessageDigest digest = sha256();
        for (String fileName : ContentPackLoader.FILES) {
            byte[] content;
            try {
                content = reader.read(fileName);
            } catch (IOException e) {
                throw new UncheckedIOException("cannot read pack file " + fileName, e);
            }
            digest.update(fileName.getBytes(StandardCharsets.UTF_8));
            digest.update(SEPARATOR);
            digest.update(normalized(content).getBytes(StandardCharsets.UTF_8));
            digest.update(SEPARATOR);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String normalized(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        return text.replace("\r\n", "\n");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime has SHA-256", e);
        }
    }

    /**
     * Reads one file of a pack.
     */
    @FunctionalInterface
    public interface FileReader {
        /**
         * Reads a file.
         *
         * @param fileName the file's name within the pack
         * @return its bytes
         * @throws IOException if it can't be read
         */
        byte[] read(String fileName) throws IOException;
    }
}
