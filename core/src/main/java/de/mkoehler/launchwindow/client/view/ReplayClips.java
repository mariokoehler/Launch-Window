package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Which clip the replay's monitor plays for a step ({@code ui-spec.md} &sect;4.7.3). A step id loses
 * its number ({@code 03-launch} is {@code launch}) and any vehicle in front ({@code atv-launch}: the
 * vehicles are listed in {@code assets/content/replay-clips.json}), then the first rule there whose
 * step is the rest, or the rest less a craft's letter ({@code reentry-a}), names the clip. Nothing
 * else borrows a clip: {@code lunar-ascent} isn't {@code ascent}. The step's outcome picks the
 * variant, and each variant's sidecar ({@code assets/ui/replay/<clip>-<variant>.json}) gives its length
 * and the second its result may show. A step with no clip, or a variant missing, gets the no-signal
 * clip.
 *
 * @author Mario Koehler
 */
public final class ReplayClips {

    /** The rules' file, under {@code assets/content/}, outside the content pack like the stories. */
    public static final String FILE = "replay-clips.json";
    /** Where the clips and their sidecars are, under {@code assets/}. */
    public static final String FOLDER = "ui/replay/";
    /** The no-signal clip's name. */
    public static final String NO_SIGNAL = "no-signal";
    /** How long a step with no clip waits before its result shows, in seconds (unconfirmed). */
    public static final float NO_SIGNAL_REVEAL = 1.5f;
    /** How long a step with no clip lasts while playing, in seconds (unconfirmed). */
    public static final float NO_SIGNAL_LENGTH = 3f;

    private static final ObjectMapper JSON = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static ReplayClips standard;

    /**
     * One rule: the steps it matches and the clip they play.
     *
     * @param step the step, without its number
     * @param clip the clip's name
     */
    public record Rule(String step, String clip) {
    }

    /**
     * The rules' file.
     *
     * @param rules    the rules, first match wins
     * @param vehicles the prefixes that name a vehicle ({@code gemini-a}, {@code atv}): a step behind
     *                 one plays the step's clip
     */
    public record Rules(List<Rule> rules, List<String> vehicles) {
    }

    /**
     * A sound the clip plays at a moment ({@code ui-spec.md} &sect;4.7.3, "Sound"): sound isn't baked
     * into the video, so the replay's music and room tone run on across steps.
     *
     * @param at     the second it starts, into the clip
     * @param sound  the sound's id in {@link ReplaySounds}
     * @param volume its volume, 0 to 1, or {@code null} for the sound's own
     * @param loop   whether it loops until {@code until} (or the clip's end)
     * @param until  the second a loop stops, or {@code null} for the clip's end
     * @param fade   how long a loop fades out, in seconds, or {@code null} for none
     * @param carry  whether a loop of the same sound still running from the clip before goes on
     *               instead of starting again (a cue at 0 only)
     */
    public record Cue(double at, String sound, Double volume, Boolean loop, Double until, Double fade,
                      Boolean carry) {

        /**
         * Returns whether it loops.
         *
         * @return whether it does
         */
        public boolean loops() {
            return Boolean.TRUE.equals(loop);
        }

        /**
         * Returns whether it carries a running loop on.
         *
         * @return whether it does
         */
        public boolean carries() {
            return Boolean.TRUE.equals(carry);
        }
    }

    /**
     * A clip's sidecar.
     *
     * @param duration its length in seconds
     * @param revealAt the second from which its variant can't turn into another, or {@code null}
     * @param loop     whether it loops
     * @param cues     the sounds it plays, or {@code null} for none
     */
    public record Sidecar(double duration, Double revealAt, Boolean loop, List<Cue> cues) {
    }

    /**
     * What the monitor plays for a step.
     *
     * @param video    the clip, under {@code assets/}
     * @param still    a picture for when no video can play, or {@code null}
     * @param duration how long the step lasts while playing, in seconds
     * @param revealAt when its result shows, in seconds
     * @param loop     whether the clip loops
     * @param signal   whether it's the step's own clip ({@code false}: no signal)
     * @param cues     the sounds it plays, in order of their start
     */
    public record Clip(String video, String still, float duration, float revealAt, boolean loop, boolean signal,
                       List<Cue> cues) {
    }

    private final List<Rule> rules;
    private final List<String> vehicles;
    private final Function<String, Optional<Sidecar>> sidecars;
    private final Map<String, Optional<Sidecar>> cache = new HashMap<>();

    private ReplayClips(Rules file, Function<String, Optional<Sidecar>> sidecars) {
        this.rules = file.rules() == null ? List.of() : List.copyOf(file.rules());
        this.vehicles = file.vehicles() == null ? List.of() : List.copyOf(file.vehicles());
        this.sidecars = sidecars;
    }

    /**
     * The game's clips, read from the classpath once; with no rules file, every step gets the
     * no-signal clip.
     *
     * @return the clips
     */
    public static synchronized ReplayClips standard() {
        if (standard == null) {
            ClassLoader loader = ReplayClips.class.getClassLoader();
            Rules file = new Rules(List.of(), List.of());
            try (InputStream in = loader.getResourceAsStream("content/" + FILE)) {
                if (in != null) {
                    file = JSON.readValue(in, Rules.class);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            standard = new ReplayClips(file, name -> {
                try (InputStream in = loader.getResourceAsStream(FOLDER + name + ".json")) {
                    return in == null ? Optional.empty() : Optional.of(JSON.readValue(in, Sidecar.class));
                } catch (IOException e) {
                    return Optional.empty();
                }
            });
        }
        return standard;
    }

    /**
     * Replaces the clips {@link #standard()} returns: the dev tools' replayer reads them fresh from
     * the repository's folders before every replay, so a clip rendered a minute ago plays.
     *
     * @param clips the clips
     */
    public static synchronized void setStandard(ReplayClips clips) {
        standard = java.util.Objects.requireNonNull(clips, "clips must not be null");
    }

    /**
     * Reads the clips from the repository's folders (tests and tools).
     *
     * @param assets the {@code assets} folder
     * @return the clips
     */
    public static ReplayClips load(Path assets) {
        try {
            Rules file = JSON.readValue(Files.readString(assets.resolve("content").resolve(FILE)), Rules.class);
            return new ReplayClips(file, name -> {
                Path sidecar = assets.resolve(FOLDER + name + ".json");
                try {
                    return Files.exists(sidecar) ? Optional.of(JSON.readValue(Files.readString(sidecar), Sidecar.class))
                        : Optional.empty();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the rules, in order.
     *
     * @return the rules
     */
    public List<Rule> rules() {
        return rules;
    }

    /**
     * Returns the prefixes that name a vehicle.
     *
     * @return the vehicles
     */
    public List<String> vehicles() {
        return vehicles;
    }

    /**
     * Returns a step's kind: its id without the number ({@code 09-gemini-b-launch} is
     * {@code gemini-b-launch}).
     *
     * @param stepId the step's id
     * @return its kind
     */
    public static String kind(String stepId) {
        return stepId.replaceFirst("^\\d+-", "");
    }

    /**
     * Returns the clip a step plays, if a rule names one.
     *
     * @param stepId the step's id
     * @return the clip's name
     */
    public Optional<String> clipName(String stepId) {
        String kind = kind(stepId);
        // The longest vehicle in front goes (soyuz-7k-ok before a shorter one that also fits).
        String vehicle = "";
        for (String v : vehicles) {
            if (kind.startsWith(v + "-") && v.length() > vehicle.length()) {
                vehicle = v;
            }
        }
        String step = vehicle.isEmpty() ? kind : kind.substring(vehicle.length() + 1);
        return rules.stream()
            .filter(rule -> step.equals(rule.step()) || step.equals(rule.step() + "-a") || step.equals(rule.step() + "-b"))
            .map(Rule::clip).findFirst();
    }

    /**
     * Returns the variant an outcome plays: a glitch, solved or not, goes on.
     *
     * @param outcome the step's outcome
     * @return the variant's name
     */
    public static String variant(MissionStepOutcome outcome) {
        return switch (outcome) {
            case SUCCESSFUL -> "success";
            case MINOR_GLITCH_SOLVED, MINOR_GLITCH -> "glitch";
            case FAILURE -> "failure";
            case CATASTROPHIC -> "catastrophic";
        };
    }

    /**
     * Returns what the monitor plays for a step and its outcome: its own clip, or no signal. A
     * catastrophe plays the failure until the step has a catastrophic clip of its own.
     *
     * @param stepId  the step's id
     * @param outcome its outcome
     * @return the clip
     */
    public Clip clipFor(String stepId, MissionStepOutcome outcome) {
        Optional<String> name = clipName(stepId);
        if (name.isEmpty()) {
            return noSignal();
        }
        String variant = variant(outcome);
        Optional<Sidecar> sidecar = sidecar(name.get() + "-" + variant);
        if (sidecar.isEmpty() && outcome == MissionStepOutcome.CATASTROPHIC) {
            variant = "failure";
            sidecar = sidecar(name.get() + "-" + variant);
        }
        if (sidecar.isEmpty()) {
            return noSignal();
        }
        Sidecar s = sidecar.get();
        float duration = (float) s.duration();
        float reveal = s.revealAt() == null ? duration : (float) Math.min(s.revealAt(), s.duration());
        List<Cue> cues = s.cues() == null ? List.of() : s.cues().stream()
            .sorted(java.util.Comparator.comparingDouble(Cue::at)).toList();
        return new Clip(FOLDER + name.get() + "-" + variant + ".webm", null, duration, reveal,
            Boolean.TRUE.equals(s.loop()), true, cues);
    }

    private Optional<Sidecar> sidecar(String name) {
        return cache.computeIfAbsent(name, sidecars);
    }

    /**
     * Returns the no-signal clip: a loop, the result after a pause, the move on a little later.
     *
     * @return the clip
     */
    public static Clip noSignal() {
        return new Clip(FOLDER + NO_SIGNAL + ".webm", FOLDER + NO_SIGNAL + ".png", NO_SIGNAL_LENGTH,
            NO_SIGNAL_REVEAL, true, false, List.of());
    }
}
