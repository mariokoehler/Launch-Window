package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.mkoehler.launchwindow.model.Archetype;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <p>One filler bot in {@code game-meta.json} ({@code design.md} &sect;5, "Filler bots"): an agency the
 * server plays itself. Its id is its folder under {@code players/}, which no account can log into
 * (account ids are UUIDs). The names are optional: a bot without them gets an agency name from
 * {@code agency-names.json} and a director's name from the personnel names when its slot is made.</p>
 *
 * <pre>"bots": [ { "id": "bot-1", "archetype": "SOVIET" } ]</pre>
 *
 * @param id           the bot's id, {@code bot-} and lower-case letters, digits or dashes
 * @param archetype    its archetype
 * @param agencyName   its agency's name, or {@code null} to draw one
 * @param directorName its director's name, or {@code null} to draw one
 * @author Mario Koehler
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BotPlayer(@JsonProperty("id") String id, @JsonProperty("archetype") Archetype archetype,
                        @JsonProperty("agencyName") String agencyName,
                        @JsonProperty("directorName") String directorName) {

    /** What a bot's id looks like: safe as a folder name, and never an account's. */
    private static final Pattern ID = Pattern.compile("bot-[a-z0-9-]{1,32}");

    /**
     * Checks the id and archetype.
     */
    public BotPlayer {
        Objects.requireNonNull(id, "a bot needs an id");
        Objects.requireNonNull(archetype, "bot " + id + " needs an archetype");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("a bot's id is bot- and up to 32 lower-case letters, digits or "
                + "dashes, not " + id);
        }
    }
}
