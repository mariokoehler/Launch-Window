package de.mkoehler.launchwindow.devtools;

import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.HelpValues;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Writes the in-game help ({@code ui-spec.md} component 23) as pages for the GitHub wiki: one page a
 * topic, its numbers filled from the rules as the help fills them, the screens its text talks about
 * ({@link #SCREENS}), and a sidebar listing the topics by group in the help's order. The landing page
 * ({@code Home.md}) is written by hand in the wiki itself and left alone.
 *
 * <p>Run: {@code mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.HelpWiki
 * -Dexec.args="<wiki clone> [<shots>]"}, then commit and push the clone. {@code <shots>} is a folder
 * {@code ScreenSnapshot} wrote; with it, the pictures in the clone's {@code images/} are made again from
 * those shots ({@code images/help/} is emptied first: it holds nothing else). Without it the pages link the
 * pictures already there.</p>
 *
 * @author Mario Koehler
 */
public final class HelpWiki {

    /** The server's default pack (the server's {@code GameMeta.DEFAULT_CONTENT_PACK}), whose buildings the numbers read. */
    private static final String PACK = "standard";

    private static final Pattern TOKEN = Pattern.compile("\\{[a-zA-Z0-9.]+}");

    /**
     * The wiki folder the pictures go in, this class's alone: it's emptied before they're made again, so
     * nothing else belongs there (the landing page's banner is in {@code images/}, beside it).
     */
    private static final String IMAGES = "images/help";

    /** A shot bigger than this (a photo-like hub or newspaper) goes in as JPEG; a flat screen stays PNG. */
    private static final long PNG_LIMIT = 1_000_000;

    private static final float JPEG_QUALITY = 0.9f;

    /**
     * A picture on a topic's page.
     *
     * @param shot    the {@code ScreenSnapshot} shot's name, without {@code .png}
     * @param caption the line under it
     * @param under   the subheading it sits under, or null for after the page's first paragraph
     */
    private record Screen(String shot, String caption, String under) {
    }

    /** The screens each topic's text talks about, picked by hand from the shots. */
    private static final Map<String, List<Screen>> SCREENS = new LinkedHashMap<>();

    static {
        screens("turns",
            new Screen("hub-nasa-summer-upgrading",
                "The Space Complex. The header shows the season and the countdown to the next turn.", null));
        screens("buildings",
            new Screen("hub-soviet-hover-hq", "The Space Complex: point at a building to see its name.", null),
            new Screen("hq-overview", "Inside Headquarters: the building's level, and its Upgrade button.",
                "Levels"));
        screens("turn-report",
            new Screen("report", "A turn report: the summary across the top, then missions, R&D, people and the budget.",
                null));
        screens("winning",
            new Screen("result-won", "The race is decided: the result over the Space Complex.", null));
        screens("museum",
            new Screen("museum-achievements", "The Museum's Achievements tab: every goal, reached or open.", null),
            new Screen("museum-encyclopedia", "The Encyclopedia tab: explainers on how the real thing works.", null));
        screens("budget",
            new Screen("pao", "The Public Affairs Office: funds, running costs, prestige and the funding reviews.", null));
        screens("prestige",
            new Screen("plan-configs", "A program's mission configurations, each with the prestige it pays.",
                "From missions"),
            new Screen("museum-achievements", "The Museum's Achievements: what each goal paid, or would pay.", "Goals"));
        screens("random-events",
            new Screen("report-news", "Random events in the turn report's news, under the season's real story.", null));
        screens("contracts",
            new Screen("contracts-hq-active", "Headquarters, with the Contracts board's card on the right.", null),
            new Screen("contracts-active", "The board: your contract, your record, and the contracts on offer.",
                "The board"));
        screens("programs",
            new Screen("hq-details-vostok", "A program's details: what it costs to open and the components it grants.",
                "Opening a program"),
            new Screen("hq-open-programs", "Open programs, each with its components' R&D and its next goal.",
                "Closing a program"));
        screens("roads-to-the-moon",
            new Screen("roads-nasa", "The roads to the Moon side by side.", null));
        screens("rnd",
            new Screen("payload-rnd", "Payload R&D: each component's reliability, its ceilings and the engineers on it.",
                "Engineers on R&D"));
        screens("decisions",
            new Screen("decision-tradeoff", "An R&D decision on a component's card.", "Deciding"),
            new Screen("handson-planning", "A hands-on puzzle, planned round by round.", "Hands-on"));
        screens("rockets",
            new Screen("standard-apollo-rockets", "Rocket assignment: a rocket too small for the payload is greyed out.",
                "Lift"),
            new Screen("vab-open-tooltip-reliability", "Opening a rocket program in the VAB, and where its reliability "
                + "starts.", "Reliability"));
        screens("planning-missions",
            new Screen("plan-configs", "A program's mission configurations in Headquarters.", null),
            new Screen("plan-assemble", "Assembling a mission: the crew, CAPCOM and the flight controllers' consoles.",
                "The steps of planning"));
        screens("mission-steps",
            new Screen("plan-assessment", "The mission plan's odds for every step.", "The first roll"),
            new Screen("replay-step", "The replay of a step that went wrong and was saved by the controllers.",
                "The saving throw"));
        screens("experts",
            new Screen("ac-train-expert", "Training an astronaut toward Expert in a skill.", null));
        screens("mission-control",
            new Screen("mc-overview", "Mission Control: the flight controllers and the missions they're on.", null),
            new Screen("plan-assemble", "The consoles of a mission, each with the skill it reads.", null));
        screens("hiring",
            new Screen("hire", "Recruiting engineers in the SET Centre.", null));
        screens("training",
            new Screen("set-training-advanced", "Starting advanced training in a skill.",
                "Advanced and permanent training"));
        screens("morale",
            new Screen("mc-overview-tooltip-morale", "Each person's morale in their department's roster.", null));
        screens("salaries",
            new Screen("set-manage-department", "Manage department: one adjustment to every salary in it.", null));
        screens("traits",
            new Screen("people-trait-tooltip", "A person's page, with their traits and relationships.", null));
        screens("chemistry",
            new Screen("people-assemble", "A crew's and the controllers' chemistry, and why, while assembling a mission.",
                null));
        screens("ambitions",
            new Screen("career-person", "An engineer's ambition, loyalty and career record.", null));
        screens("careers",
            new Screen("career-memorial-page", "An astronaut's page on the memorial.", null),
            new Screen("museum-memorial", "The Museum's Memorial tab.", null));
        screens("astronauts",
            new Screen("ac-overview", "The Astronaut Complex.", null),
            new Screen("progression-wings-person", "An astronaut with rocket-plane wings.", "Rocket-plane wings"));
        screens("pr-duty",
            new Screen("ac-pr-dialog", "Sending an astronaut on PR duty.", null));
        screens("poaching",
            new Screen("poach-offer", "Making an offer to a rival's controller.", "Making an offer"),
            new Screen("poach-set-retention", "A department's standing order on matching rivals' offers.",
                "Defending your people"));
        screens("the-race",
            new Screen("museum-race", "The race in the Museum: every agency's progress on the game's goals.", null));
        screens("the-paper",
            new Screen("frontpage-satellite", "The Daily Zenith on a First.", "The great Firsts"),
            new Screen("reactions-dialog-fresh", "Giving a statement on a rival's First.", "The world reacts"),
            new Screen("theories-page-mars", "A science page, settling a theory of the day.", "Theories of the day"),
            new Screen("memorial-page", "A memorial page.", "In memory"));
        screens("findings",
            new Screen("findings-report", "A finding in the turn report, with its Publish button.", null),
            new Screen("findings-dialog", "Publishing a finding: what it would pay as things stand.", "Publishing"));
        screens("intelligence",
            new Screen("intel-overview", "The Intelligence Office: its level, actions and what each level brings.",
                "What each level brings"),
            new Screen("intel-operations", "Choosing an operation, with its odds of success and of being caught.",
                "Odds, and getting caught"));
    }

    private HelpWiki() {
    }

    private static void screens(String topicId, Screen... screens) {
        SCREENS.put(topicId, List.of(screens));
    }

    /**
     * Writes the pages.
     *
     * @param args the wiki clone's folder, and optionally a {@code ScreenSnapshot} folder to take the pictures from
     * @throws IOException if a page or picture can't be written
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1 && args.length != 2) {
            throw new IllegalArgumentException("Usage: HelpWiki <wiki clone> [<ScreenSnapshot folder>]");
        }
        Path out = Path.of(args[0]);
        Help help = Help.parse(Files.readString(Path.of("assets", "content", Help.FILE)));
        ContentCatalog catalog = ContentPackLoader.load(Path.of("assets", "content", PACK));
        HelpValues values = HelpValues.of(BudgetSchedule.baspmClose(), catalog.buildings());

        Map<String, Help.Topic> byId = new LinkedHashMap<>();
        for (Help.Topic topic : help.topics()) {
            byId.put(topic.id(), topic);
        }
        for (String id : SCREENS.keySet()) {
            if (!byId.containsKey(id)) {
                throw new IllegalStateException("Screens for an unknown topic: " + id);
            }
        }
        Map<String, String> images = args.length == 2 ? writeImages(Path.of(args[1]), out.resolve(IMAGES))
            : existingImages(out.resolve(IMAGES));
        for (Help.Topic topic : help.topics()) {
            write(out.resolve(page(topic.title()) + ".md"), topicPage(topic, byId, values, images));
        }
        write(out.resolve("_Sidebar.md"), sidebar(help));
        write(out.resolve("_Footer.md"), "These pages are the game's in-game help (F1), with the numbers of the "
            + "current build. The game itself is the reference if they ever disagree.\n");
        System.out.println(help.topics().size() + " topics and " + images.size() + " pictures written to "
            + out.toAbsolutePath());
    }

    private static String topicPage(Help.Topic topic, Map<String, Help.Topic> byId, HelpValues values,
                                    Map<String, String> images) {
        List<Screen> screens = SCREENS.getOrDefault(topic.id(), List.of());
        Set<String> headings = new HashSet<>();
        StringBuilder md = new StringBuilder();
        boolean inList = false;
        boolean firstParagraph = true;
        // A first paragraph that introduces a list ("Its tabs:") keeps its pictures until after the list.
        boolean picturesPending = false;
        for (String raw : topic.body()) {
            Help.Line line = Help.Line.of(raw);
            String text = values.fill(line.text());
            if (TOKEN.matcher(text).find()) {
                throw new IllegalStateException("Unfilled token in " + topic.id() + ": " + text);
            }
            if (picturesPending && line.kind() != Help.Line.Kind.BULLET) {
                pictures(md, screens, null, images);
                picturesPending = false;
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
            if (line.kind() == Help.Line.Kind.HEADING) {
                md.append("## ").append(text).append('\n');
                headings.add(text);
                pictures(md, screens, text, images);
                continue;
            }
            md.append(text).append('\n');
            if (firstParagraph) {
                if (text.endsWith(":")) {
                    picturesPending = true;
                } else {
                    pictures(md, screens, null, images);
                }
                firstParagraph = false;
            }
        }
        if (picturesPending) {
            pictures(md, screens, null, images);
        }
        for (Screen screen : screens) {
            if (screen.under() != null && !headings.contains(screen.under())) {
                throw new IllegalStateException("No subheading \"" + screen.under() + "\" in " + topic.id());
            }
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

    /** Appends the pictures that go under {@code heading} (null: after the first paragraph), each with its caption. */
    private static void pictures(StringBuilder md, List<Screen> screens, String heading, Map<String, String> images) {
        for (Screen screen : screens) {
            if (Objects.equals(screen.under(), heading)) {
                String file = IMAGES + "/" + images.get(screen.shot());
                md.append("\n[![").append(screen.caption()).append("](").append(file).append(")](").append(file)
                    .append(")\n*").append(screen.caption()).append("*\n");
            }
        }
    }

    /**
     * Makes the wiki's pictures again from the shots: the folder is emptied first, so a picture no longer
     * picked doesn't linger.
     *
     * @return each shot's file name in the folder
     */
    private static Map<String, String> writeImages(Path shots, Path folder) throws IOException {
        if (Files.isDirectory(folder)) {
            try (Stream<Path> old = Files.list(folder)) {
                for (Path file : old.toList()) {
                    Files.delete(file);
                }
            }
        }
        Files.createDirectories(folder);
        Map<String, String> files = new HashMap<>();
        for (List<Screen> screens : SCREENS.values()) {
            for (Screen screen : screens) {
                if (files.containsKey(screen.shot())) {
                    continue;
                }
                Path png = shots.resolve(screen.shot() + ".png");
                if (!Files.isRegularFile(png)) {
                    throw new IllegalStateException("No shot " + png);
                }
                if (Files.size(png) <= PNG_LIMIT) {
                    Files.copy(png, folder.resolve(screen.shot() + ".png"));
                    files.put(screen.shot(), screen.shot() + ".png");
                } else {
                    writeJpeg(png, folder.resolve(screen.shot() + ".jpg"));
                    files.put(screen.shot(), screen.shot() + ".jpg");
                }
            }
        }
        return files;
    }

    /** The pictures already in the wiki, for writing the pages without shots. */
    private static Map<String, String> existingImages(Path folder) {
        Map<String, String> files = new HashMap<>();
        for (List<Screen> screens : SCREENS.values()) {
            for (Screen screen : screens) {
                String name = Stream.of(".png", ".jpg").map(ext -> screen.shot() + ext)
                    .filter(file -> Files.isRegularFile(folder.resolve(file))).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No picture for " + screen.shot() + " in " + folder
                        + ": pass a ScreenSnapshot folder"));
                files.put(screen.shot(), name);
            }
        }
        return files;
    }

    private static void writeJpeg(Path png, Path jpg) throws IOException {
        BufferedImage source = ImageIO.read(png.toFile());
        // The shots have an alpha channel, which the JPEG writer can't take.
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(jpg.toFile())) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
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
