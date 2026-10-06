package de.mkoehler.launchwindow.client.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code assets/CREDITS.md}, the credits for every third-party asset, into sections for the
 * Settings dialog's credits page. Only the Markdown the file uses is understood: a {@code #} title
 * (skipped), {@code ##} section headings, paragraphs whose lines wrap, and {@code -} bullets whose
 * continuation lines are indented. Backticks are dropped.
 *
 * @author Mario Koehler
 */
public final class Credits {

    /**
     * One section of the credits.
     *
     * @param title   the section heading, or {@code ""} for text before the first heading
     * @param entries its paragraphs and bullets, each joined onto one line
     */
    public record Section(String title, List<Entry> entries) {
    }

    /**
     * One paragraph or bullet.
     *
     * @param text   the text, on one line
     * @param bullet whether it was a bullet
     */
    public record Entry(String text, boolean bullet) {
    }

    private Credits() {
    }

    /**
     * Makes a credit drawable with a font that only has {@code characters}: a parenthesis holding a
     * letter the font lacks (an original-script name next to its English one, say) is left out, and
     * any other missing letter is dropped.
     *
     * @param text       the credit
     * @param characters the letters the font has
     * @return the text the font can draw
     */
    public static String drawable(String text, String characters) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '(') {
                int close = text.indexOf(')', i);
                if (close > i && !allDrawable(text.substring(i, close + 1), characters)) {
                    while (out.length() > 0 && out.charAt(out.length() - 1) == ' ') {
                        out.setLength(out.length() - 1);
                    }
                    i = close + 1;
                    continue;
                }
            }
            if (c == ' ' || characters.indexOf(c) >= 0) {
                out.append(c);
            }
            i++;
        }
        return out.toString();
    }

    private static boolean allDrawable(String text, String characters) {
        for (char c : text.toCharArray()) {
            if (c != ' ' && characters.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses the credits file.
     *
     * @param markdown the file's text; {@code null} gives no sections
     * @return the sections in file order, empty ones left out
     */
    public static List<Section> parse(String markdown) {
        List<Section> sections = new ArrayList<>();
        if (markdown == null) {
            return sections;
        }
        String title = "";
        List<Entry> entries = new ArrayList<>();
        StringBuilder current = null;
        boolean bullet = false;
        for (String raw : markdown.replace("\r\n", "\n").split("\n")) {
            String line = raw.replace("`", "");
            String trimmed = line.trim();
            boolean heading = trimmed.startsWith("#");
            boolean newBullet = trimmed.startsWith("- ");
            boolean continuation = !trimmed.isEmpty() && !heading && !newBullet && current != null
                && (!bullet || line.startsWith(" "));
            if (continuation) {
                current.append(' ').append(trimmed);
                continue;
            }
            if (current != null) {
                entries.add(new Entry(current.toString(), bullet));
                current = null;
            }
            if (trimmed.startsWith("## ")) {
                if (!entries.isEmpty()) {
                    sections.add(new Section(title, entries));
                }
                title = trimmed.substring(3).trim();
                entries = new ArrayList<>();
            } else if (heading || trimmed.isEmpty()) {
                // The file's own title, or a blank line between entries.
                continue;
            } else {
                bullet = newBullet;
                current = new StringBuilder(newBullet ? trimmed.substring(2).trim() : trimmed);
            }
        }
        if (current != null) {
            entries.add(new Entry(current.toString(), bullet));
        }
        if (!entries.isEmpty()) {
            sections.add(new Section(title, entries));
        }
        return sections;
    }
}
