package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.SetSkill;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * <p>The words of incident investigations ({@code design.md} &sect;2.14(c)): what a report finds and what was
 * changed, keyed by the culprit component's group and the failing step's phase; the contributing factors an
 * independent board adds; and the board's front-page stories. Read from
 * {@code content/investigation-causes.json} on the classpath. Deliberately outside
 * {@link ContentPackLoader#FILES} (not hashed, not frozen), like {@code event-stories.json}: the rules store the
 * picked ids, so an id is never renamed or reused.</p>
 *
 * <p>Tokens: {@code {component}} in a cause; {@code {mission}}, {@code {program}} (the program's name, for headlines), {@code {component}}, {@code {cause}} (the cause's
 * {@code short}), {@code {season}}, {@code {agency}} and {@code {crew}} in a board story.</p>
 *
 * @param phases       the step phases, matched in order against a step's kind
 * @param defaultPhase the phase of a kind no pattern matches
 * @param causes       the causes
 * @param factors      a board's contributing factors
 * @param boardStories a board's front-page stories
 * @author Mario Koehler
 */
public record InvestigationCauses(List<PhaseRule> phases, String defaultPhase, List<Cause> causes,
                                  List<Factor> factors, List<BoardStory> boardStories) {

    /** The file's name under {@code content/}. */
    public static final String FILE = "investigation-causes.json";

    private static final InvestigationCauses EMPTY = new InvestigationCauses(List.of(), "space", List.of(), List.of(),
        List.of());

    private static volatile InvestigationCauses standard;

    /**
     * The group of hardware a cause is about, folded from {@link SetSkill}.
     */
    public enum Group {
        /** Launch vehicles, crew-rated or not. */
        ROCKET,
        /** Uncrewed probes and satellites. */
        PROBE,
        /** Crewed spacecraft. */
        SPACECRAFT,
        /** Spacesuits. */
        SUIT;

        /**
         * Returns the group of a research skill.
         *
         * @param skill the component's skill
         * @return the group
         */
        public static Group of(SetSkill skill) {
            return switch (skill) {
                case ROCKETS, HUMAN_RATED_ROCKETS -> ROCKET;
                case SPACE_PROBES -> PROBE;
                case CREWED_SPACECRAFT -> SPACECRAFT;
                case EVA_SUITS -> SUIT;
            };
        }
    }

    /**
     * One phase and the step kinds it takes. A kind alone can't always tell: a landing is on the Moon, on Venus or
     * on a runway, so a rule may also name the bodies or the programs it holds for.
     *
     * @param id       the phase
     * @param pattern  a regular expression found anywhere in a step's kind
     * @param bodies   the bodies (a program category's, {@code CelestialBody} names) it holds for; empty for any
     * @param programs the programs it holds for; empty for any
     */
    public record PhaseRule(String id, String pattern, List<String> bodies, List<String> programs) {

        /**
         * Validates the rule and copies the lists.
         *
         * @throws NullPointerException if the id or the pattern is {@code null}
         */
        public PhaseRule {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(pattern, "pattern must not be null");
            bodies = bodies == null ? List.of() : List.copyOf(bodies);
            programs = programs == null ? List.of() : List.copyOf(programs);
        }

        /**
         * Returns whether the rule holds for a step.
         *
         * @param kind      the step's kind
         * @param body      the flight's body, or {@code null}
         * @param programId the flight's program, or {@code null}
         * @return whether it holds
         */
        public boolean holds(String kind, String body, String programId) {
            return (bodies.isEmpty() || body != null && bodies.contains(body))
                && (programs.isEmpty() || programId != null && programs.contains(programId))
                && Pattern.compile(pattern).matcher(kind).find();
        }
    }

    /**
     * One cause and its fix.
     *
     * @param id      the id, stored in saved state and reports
     * @param group   the hardware it's about
     * @param phases  the phases it fits; empty for any
     * @param fatal   whether it may explain a lost crew ({@code false} for a cause that harms nobody)
     * @param shortly the cause in a few words, for headlines ({@code short} in the file)
     * @param finding what the investigation found
     * @param fix     what was changed
     */
    public record Cause(String id, Group group, List<String> phases, boolean fatal, String shortly, String finding,
                        String fix) {

        /**
         * Reads a cause; {@code fatal} defaults to {@code true}.
         *
         * @param id      the id
         * @param group   the group
         * @param phases  the phases, or {@code null} for any
         * @param fatal   whether it may explain a lost crew, or {@code null} for yes
         * @param shortly the short form
         * @param finding the finding
         * @param fix     the fix
         * @return the cause
         */
        @JsonCreator
        public static Cause of(@JsonProperty("id") String id, @JsonProperty("group") Group group,
                               @JsonProperty("phases") List<String> phases, @JsonProperty("fatal") Boolean fatal,
                               @JsonProperty("short") String shortly, @JsonProperty("finding") String finding,
                               @JsonProperty("fix") String fix) {
            return new Cause(id, group, phases == null ? List.of() : List.copyOf(phases), fatal == null || fatal,
                shortly, finding, fix);
        }

        /**
         * Returns whether it fits a phase.
         *
         * @param phase the phase
         * @return {@code true} when it lists the phase or lists none
         */
        public boolean fits(String phase) {
            return phases.isEmpty() || phases.contains(phase);
        }
    }

    /**
     * A contributing factor, which a board's report adds to its cause.
     *
     * @param id   the id, stored on printed pages
     * @param text the sentence
     */
    public record Factor(String id, String text) {
    }

    /**
     * A board's front-page story.
     *
     * @param id       the id, stored on printed pages
     * @param headline the headline
     * @param deck     the deck under it
     * @param lead     the first paragraph
     * @param close    the last paragraph
     */
    public record BoardStory(String id, String headline, String deck, String lead, String close) {
    }

    /**
     * Copies the lists.
     */
    public InvestigationCauses {
        phases = phases == null ? List.of() : List.copyOf(phases);
        defaultPhase = defaultPhase == null ? "space" : defaultPhase;
        causes = causes == null ? List.of() : List.copyOf(causes);
        factors = factors == null ? List.of() : List.copyOf(factors);
        boardStories = boardStories == null ? List.of() : List.copyOf(boardStories);
    }

    /**
     * Returns the words on the classpath, read once; empty when the file is missing.
     *
     * @return the words
     * @throws UncheckedIOException if the file can't be read
     */
    public static InvestigationCauses standard() {
        InvestigationCauses loaded = standard;
        if (loaded == null) {
            try (InputStream in = InvestigationCauses.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, InvestigationCauses.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Reads the words from a file, for tests.
     *
     * @param file the file
     * @return the words
     * @throws UncheckedIOException if the file can't be read
     */
    public static InvestigationCauses load(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return new ObjectMapper().readValue(in, InvestigationCauses.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns the phase of a step kind, for no body or program in particular.
     *
     * @param kind the step kind
     * @return the phase's id
     */
    public String phaseOf(String kind) {
        return phaseOf(kind, null, null);
    }

    /**
     * Returns the phase of a step (its kind, {@code rules.Findings.kind}) on a flight: the first rule that holds.
     *
     * @param kind      the step kind
     * @param body      the flight's body ({@code CelestialBody} name), or {@code null}
     * @param programId the flight's program, or {@code null}
     * @return the phase's id
     */
    public String phaseOf(String kind, String body, String programId) {
        for (PhaseRule rule : phases) {
            if (rule.holds(kind, body, programId)) {
                return rule.id();
            }
        }
        return defaultPhase;
    }

    /**
     * Returns the causes a report on this group and phase may give.
     *
     * @param group     the culprit's group
     * @param phase     the failing step's phase
     * @param fatalOnly whether a crew was lost
     * @return the causes, in the file's order
     */
    public List<Cause> eligible(Group group, String phase, boolean fatalOnly) {
        return causes.stream().filter(c -> c.group() == group && c.fits(phase) && (!fatalOnly || c.fatal())).toList();
    }

    /**
     * Picks a cause the agency hasn't been told yet, and records it as told. Once every eligible cause has been
     * told, they all count as untold again.
     *
     * @param group     the culprit's group
     * @param phase     the failing step's phase
     * @param fatalOnly whether a crew was lost
     * @param random    the random source
     * @param used      the causes told so far; mutated
     * @return the cause's id, or {@code null} when none is eligible
     */
    public String pick(Group group, String phase, boolean fatalOnly, RandomGenerator random, List<String> used) {
        List<Cause> eligible = eligible(group, phase, fatalOnly);
        if (eligible.isEmpty()) {
            return null;
        }
        List<Cause> fresh = new ArrayList<>(eligible.stream().filter(c -> !used.contains(c.id())).toList());
        if (fresh.isEmpty()) {
            eligible.forEach(c -> used.remove(c.id()));
            fresh.addAll(eligible);
        }
        String id = fresh.get(random.nextInt(fresh.size())).id();
        used.add(id);
        return id;
    }

    /**
     * Returns a cause by id.
     *
     * @param id the id, maybe {@code null}
     * @return the cause, if known
     */
    public Optional<Cause> cause(String id) {
        return id == null ? Optional.empty() : causes.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    /**
     * Returns a factor by id.
     *
     * @param id the id, maybe {@code null}
     * @return the factor, if known
     */
    public Optional<Factor> factor(String id) {
        return id == null ? Optional.empty() : factors.stream().filter(f -> f.id().equals(id)).findFirst();
    }

    /**
     * Returns a board story by id.
     *
     * @param id the id, maybe {@code null}
     * @return the story, if known
     */
    public Optional<BoardStory> boardStory(String id) {
        return id == null ? Optional.empty() : boardStories.stream().filter(s -> s.id().equals(id)).findFirst();
    }
}
