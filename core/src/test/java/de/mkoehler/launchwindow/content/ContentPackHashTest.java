package de.mkoehler.launchwindow.content;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests {@link ContentPackHash}.
 *
 * @author Mario Koehler
 */
class ContentPackHashTest {

    /**
     * The same text hashes the same whatever its line endings or byte-order mark, and any real
     * change gives a different hash.
     */
    @Test
    void onlyRealChangesChangeTheHash() {
        Map<String, String> lf = pack("[\n  {\"id\": \"a\"}\n]\n");
        Map<String, String> crlf = pack("[\r\n  {\"id\": \"a\"}\r\n]\r\n");
        Map<String, String> bom = pack("﻿[\n  {\"id\": \"a\"}\n]\n");
        Map<String, String> changed = pack("[\n  {\"id\": \"b\"}\n]\n");
        assertEquals(hash(lf), hash(crlf));
        assertEquals(hash(lf), hash(bom));
        assertNotEquals(hash(lf), hash(changed));
    }

    /**
     * The bundled pack hashes the same from a folder and from the classpath, so a server's frozen
     * copy and a client's bundled one agree.
     */
    @Test
    void theBundledPackHashesTheSameFromDiskAndClasspath() throws Exception {
        String fromDisk = ContentPackHash.of(EarlyEraPackTest.PACK);
        assertEquals(64, fromDisk.length());
        // core does not bundle assets/ itself, so read through a class loader rooted at assets/, as EarlyEraPackTest does.
        try (URLClassLoader assets = new URLClassLoader(new URL[] {Path.of("..", "assets").toUri().toURL()}, null)) {
            assertEquals(fromDisk, ContentPackHash.ofClasspath("content/early-era", assets));
        }
    }

    private static Map<String, String> pack(String goals) {
        Map<String, String> files = new HashMap<>();
        for (String name : ContentPackLoader.FILES) {
            files.put(name, "[]");
        }
        files.put(ContentPackLoader.GOALS_FILE, goals);
        return files;
    }

    private static String hash(Map<String, String> files) {
        return ContentPackHash.of(name -> files.get(name).getBytes(StandardCharsets.UTF_8));
    }
}
