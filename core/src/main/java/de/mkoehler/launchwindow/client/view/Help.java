package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The help ({@code ui-spec.md} component 23): the game's rules and mechanics as topics, where the
 * Museum's explainers tell how the real thing works. The text lives in
 * {@code assets/content/help.json}, outside the frozen content pack like {@code strings.json}, so
 * fixes reach running games. Numbers in it are tokens such as {@code {poaching.minTenure}}, filled
 * from the rules' own constants by {@link HelpValues}, so the text never drifts from the rules.
 *
 * <p>A topic's body is a list of lines: {@code "## "} starts a subheading, {@code "- "} a bullet,
 * anything else is a paragraph.</p>
 *
 * @param topics every topic, in the list's order (grouped by {@link Topic#group()})
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record Help(List<Topic> topics) {

    /** The file under {@code assets/content/}. */
    public static final String FILE = "help.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile Help standard;

    /**
     * Copies the list.
     */
    public Help {
        topics = topics == null ? List.of() : List.copyOf(topics);
    }

    /**
     * One topic.
     *
     * @param id         its id, which screens and links name it by; never renamed
     * @param group      the list's group it sits in, such as "People"
     * @param title      its title
     * @param body       its lines, tokens unfilled
     * @param related    other topics' ids, shown as "See also" links
     * @param explainers explainer ids, shown as concept chips that open the explainer
     */
    @JsonIgnoreProperties({"_comment"})
    public record Topic(String id, String group, String title, List<String> body, List<String> related,
                        List<String> explainers) {

        /**
         * Copies the lists.
         */
        public Topic {
            body = body == null ? List.of() : List.copyOf(body);
            related = related == null ? List.of() : List.copyOf(related);
            explainers = explainers == null ? List.of() : List.copyOf(explainers);
        }

        /**
         * Returns the topic's text as the search sees it: the title and every line, tokens filled
         * and line markers dropped.
         *
         * @param values the token values
         * @return the text
         */
        public String searchText(HelpValues values) {
            StringBuilder text = new StringBuilder(title);
            for (String line : body) {
                text.append('\n').append(values.fill(Line.of(line).text()));
            }
            return text.toString();
        }
    }

    /**
     * One body line, its marker taken off.
     *
     * @param kind what it is
     * @param text its text, tokens unfilled
     */
    public record Line(Kind kind, String text) {

        /** What a line is. */
        public enum Kind {
            /** A paragraph. */
            PARAGRAPH,
            /** A subheading. */
            HEADING,
            /** A bullet. */
            BULLET
        }

        /**
         * Reads a line.
         *
         * @param line the line as written
         * @return the line
         */
        public static Line of(String line) {
            if (line.startsWith("## ")) {
                return new Line(Kind.HEADING, line.substring(3));
            }
            if (line.startsWith("- ")) {
                return new Line(Kind.BULLET, line.substring(2));
            }
            return new Line(Kind.PARAGRAPH, line);
        }
    }

    /**
     * Where help was opened from, and the topics that matter there: help opens with its list
     * filtered to them. A screen not listed opens help on every topic.
     */
    public enum Context {
        /** The Public Affairs Office. */
        PUBLIC_AFFAIRS("the Public Affairs Office", "budget", "turns"),
        /** Headquarters and its program lists. */
        HEADQUARTERS("Headquarters", "programs", "budget"),
        /** A program's details and its mission configurations. */
        PROGRAM("this program", "programs", "rnd"),
        /** The SET Centre. */
        SET_CENTRE("the SET Centre", "rnd", "chemistry", "poaching"),
        /** A Payload or Rocket R&amp;D board. */
        RND("R&D", "rnd", "chemistry", "programs"),
        /** The VAB and its rocket programs. */
        VAB("the VAB", "rnd", "programs"),
        /** Mission Control. */
        MISSION_CONTROL("Mission Control", "chemistry", "poaching"),
        /** Assembling a mission's crew and consoles. */
        ASSEMBLE_MISSION("assembling a mission", "chemistry"),
        /** A person's page. */
        PERSON("this person", "chemistry", "poaching"),
        /** The Intelligence Office. */
        INTELLIGENCE("the Intelligence Office", "intelligence", "poaching");

        private final String place;
        private final List<String> topicIds;

        Context(String place, String... topicIds) {
            this.place = place;
            this.topicIds = List.of(topicIds);
        }

        /**
         * Returns where this is, for the filter chip ("3 topics for the SET Centre").
         *
         * @return the place
         */
        public String place() {
            return place;
        }

        /**
         * Returns the topics that matter here, most important first.
         *
         * @return the topic ids
         */
        public List<String> topicIds() {
            return topicIds;
        }
    }

    /**
     * Returns the help bundled with the game, read once from the classpath.
     *
     * @return the help, or none if the file isn't there
     */
    public static Help standard() {
        Help loaded = standard;
        if (loaded == null) {
            try (InputStream in = Help.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? new Help(List.of()) : MAPPER.readValue(in, Help.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read the help", e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Reads help from JSON text.
     *
     * @param json the text
     * @return the help
     */
    public static Help parse(String json) {
        try {
            return MAPPER.readValue(json, Help.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the help", e);
        }
    }

    /**
     * Returns a topic by id.
     *
     * @param id the id
     * @return the topic, if there is one
     */
    public Optional<Topic> byId(String id) {
        return topics.stream().filter(topic -> topic.id().equals(id)).findFirst();
    }

    /**
     * Returns the topics the list shows. Typed text searches every topic, title and text, ignoring
     * case; with none, a context shows its own topics (in the list's order) and no context shows
     * them all.
     *
     * @param query   the search text, or blank for none
     * @param context where help was opened from, or {@code null} for nowhere in particular
     * @param values  the token values, so the search finds the numbers the player reads
     * @return the topics, in the list's order
     */
    public List<Topic> shown(String query, Context context, HelpValues values) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Topic> shown = new ArrayList<>();
        for (Topic topic : topics) {
            boolean keep = needle.isEmpty()
                ? context == null || context.topicIds().contains(topic.id())
                : topic.searchText(values).toLowerCase(Locale.ROOT).contains(needle);
            if (keep) {
                shown.add(topic);
            }
        }
        return shown;
    }

    /**
     * Returns the groups of some topics, in the order they first appear.
     *
     * @param topics the topics
     * @return the groups
     */
    public static List<String> groups(List<Topic> topics) {
        Set<String> groups = new LinkedHashSet<>();
        topics.forEach(topic -> groups.add(topic.group()));
        return List.copyOf(groups);
    }
}
