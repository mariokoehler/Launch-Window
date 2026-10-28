package de.mkoehler.launchwindow.devtools;

import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.HelpValues;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Writes the in-game help ({@code ui-spec.md} component 23) as pages for the GitHub wiki: one page a
 * topic, its numbers filled from the rules as the help fills them, and a sidebar listing the topics by
 * group in the help's order. The landing page ({@code Home.md}) is written by hand in the wiki itself
 * and left alone.
 *
 * <p>Run: {@code mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.HelpWiki
 * -Dexec.args=<wiki clone>}, then commit and push the clone.</p>
 *
 * @author Mario Koehler
 */
public final class HelpWiki {

    /** The server's default pack (the server's {@code GameMeta.DEFAULT_CONTENT_PACK}), whose buildings the numbers read. */
    private static final String PACK = "standard";

    private static final Pattern TOKEN = Pattern.compile("\\{[a-zA-Z0-9.]+}");

    private HelpWiki() {
    }

    /**
     * Writes the pages.
     *
     * @param args the wiki clone's folder
     * @throws IOException if a page can't be written
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: HelpWiki <wiki clone>");
        }
        Path out = Path.of(args[0]);
        Help help = Help.parse(Files.readString(Path.of("assets", "content", Help.FILE)));
        ContentCatalog catalog = ContentPackLoader.load(Path.of("assets", "content", PACK));
        HelpValues values = HelpValues.of(BudgetSchedule.baspmClose(), catalog.buildings());

        Map<String, Help.Topic> byId = new LinkedHashMap<>();
        for (Help.Topic topic : help.topics()) {
            byId.put(topic.id(), topic);
        }
        for (Help.Topic topic : help.topics()) {
            write(out.resolve(page(topic.title()) + ".md"), topicPage(topic, byId, values));
        }
        write(out.resolve("_Sidebar.md"), sidebar(help));
        write(out.resolve("_Footer.md"), "These pages are the game's in-game help (F1), with the numbers of the "
            + "current build. The game itself is the reference if they ever disagree.\n");
        System.out.println(help.topics().size() + " topics written to " + out.toAbsolutePath());
    }

    private static String topicPage(Help.Topic topic, Map<String, Help.Topic> byId, HelpValues values) {
        StringBuilder md = new StringBuilder();
        boolean inList = false;
        for (String raw : topic.body()) {
            Help.Line line = Help.Line.of(raw);
            String text = values.fill(line.text());
            if (TOKEN.matcher(text).find()) {
                throw new IllegalStateException("Unfilled token in " + topic.id() + ": " + text);
            }
            if (line.kind() == Help.Line.Kind.BULLET) {
                if (!inList && !md.isEmpty()) {
                    md.append('\n');
                }
                md.append("- ").append(text).append('\n');
                inList = true;
                continue;
            }
            if (!md.isEmpty()) {
                md.append('\n');
            }
            inList = false;
            md.append(line.kind() == Help.Line.Kind.HEADING ? "## " + text : text).append('\n');
        }
        List<String> related = new ArrayList<>();
        for (String id : topic.related()) {
            Help.Topic other = byId.get(id);
            if (other != null) {
                related.add(link(other.title()));
            }
        }
        if (!related.isEmpty()) {
            md.append("\n**See also:** ").append(String.join(" · ", related)).append('\n');
        }
        return md.toString();
    }

    private static String sidebar(Help help) {
        StringBuilder md = new StringBuilder("**[Launch Window](Home)**\n");
        String group = null;
        for (Help.Topic topic : help.topics()) {
            if (!topic.group().equals(group)) {
                group = topic.group();
                md.append("\n**").append(group).append("**\n\n");
            }
            md.append("- ").append(link(topic.title())).append('\n');
        }
        return md.toString();
    }

    /** A topic's page name, as the wiki names its file: spaces become hyphens. */
    private static String page(String title) {
        return title.replace(' ', '-');
    }

    private static String link(String title) {
        return "[" + title + "](" + URLEncoder.encode(page(title), StandardCharsets.UTF_8) + ")";
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
