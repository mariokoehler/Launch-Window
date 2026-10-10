package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.DecisionEffect;
import de.mkoehler.launchwindow.model.Trait;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The R&amp;D decisions the game can put to a player ({@code ui-spec.md} component 26, the user's
 * idea, 2026-10-10): each a story about one component, two to four options, what each shows in
 * advance, a default, and outcomes the rules draw from when the player decides. Read from
 * {@code content/decisions.json} on the classpath; outside {@link ContentPackLoader#FILES} (not hashed,
 * not frozen), like the stories. Ids are stored in saved state and turn reports: never renamed or
 * reused.
 *
 * <p>Sizes are relative so a decision weighs the same on any component: a reliability effect is in
 * seasons of the component's R&amp;D gain, a cost or a funds effect a share of a season's funding.</p>
 *
 * @param decisions every decision
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record Decisions(List<Decision> decisions) {

    /** The decisions' file, under {@code content/}. */
    public static final String FILE = "decisions.json";

    /** No decisions. */
    public static final Decisions EMPTY = new Decisions(List.of());

    private static volatile Decisions standard;

    /** The ten kinds of decision; how the options' outcomes are drawn differs by type. */
    public enum Type {
        /** Spend something for a stated chance. */
        RESOURCE,
        /** One right answer; "I don't know" is the default. */
        KNOWLEDGE,
        /** Outcomes the options' words don't give away; traits shape them. */
        AMBIGUOUS,
        /** An idle engineer oversees it; their skill sets the odds. */
        OVERSEE,
        /** An effect over several seasons. */
        TRADEOFF,
        /** Clues, then the cause; one is right. */
        DIAGNOSIS,
        /** An offer for this turn only, with a catch. */
        OPPORTUNITY,
        /** Two engineers work together; their rapport sets the odds. */
        PAIRING,
        /** A rival's approach from the Intelligence Office; right more often at a higher level. */
        INTEL
    }

    /** Which components a decision fits. */
    public enum Fits {
        /** Rockets only. */
        ROCKET,
        /** Payloads only. */
        PAYLOAD,
        /** Either. */
        ANY
    }

    /** Who an option takes, picked by the player. */
    public enum Takes {
        /** Nobody. */
        NOBODY,
        /** One idle engineer who oversees it; their skill sets the odds. */
        OVERSEER,
        /** One engineer from the component's team, away for a while. */
        SECONDED,
        /** Two engineers, from the team or idle; their rapport sets the odds. */
        PAIR
    }

    /** How an option's good outcome's chance is set. */
    public enum Odds {
        /** The weights as written. */
        FIXED,
        /** From the overseer's skill. */
        SKILL,
        /** From the pair's rapport and traits. */
        RAPPORT,
        /** From the Intelligence Office's level. */
        INTEL
    }

    /** How an outcome reads. */
    public enum Tone {
        /** It went well. */
        GOOD,
        /** It went badly. */
        BAD,
        /** Neither. */
        NEUTRAL
    }

    /**
     * One clue, for a diagnosis.
     *
     * @param kind a short label ("Telemetry", "Technician's note", "Inspection")
     * @param icon the clue's icon: {@code clue_trace}, {@code clue_note} or {@code clue_inspect}
     * @param text the clue
     */
    public record Clue(String kind, String icon, String text) {
    }

    /**
     * One effect as written.
     *
     * @param kind    what it changes
     * @param amount  seasons of R&amp;D for reliability, a share of a season's funding for funds, a factor,
     *                or points or seasons
     * @param seasons how long a timed effect lasts
     */
    public record Effect(DecisionEffect.Kind kind, double amount, int seasons) {

        /**
         * Checks the kind.
         *
         * @throws NullPointerException if {@code kind} is {@code null}
         */
        public Effect {
            Objects.requireNonNull(kind, "kind must not be null");
        }
    }

    /**
     * One outcome of an option.
     *
     * @param id         its id within the decision
     * @param weight     its weight among the option's outcomes (for {@link Odds#FIXED})
     * @param tone       how it reads
     * @param headline   the outcome's headline
     * @param text       a sentence or two; placeholders {@code {name}} and {@code {second}} for the people
     * @param effects    what it does
     * @param favouredBy a trait that doubles this outcome's weight when anyone on the component's team
     *                   has it, revealed or not; {@code null} for none
     * @param continues  whether it opens the decision's follow-up
     */
    public record Outcome(String id, double weight, Tone tone, String headline, String text, List<Effect> effects,
                          Trait favouredBy, boolean continues) {

        /**
         * Checks the fields and copies the effects.
         *
         * @throws NullPointerException if a required field is {@code null}
         */
        public Outcome {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(tone, "tone must not be null");
            Objects.requireNonNull(headline, "headline must not be null");
            Objects.requireNonNull(text, "text must not be null");
            effects = effects == null ? List.of() : List.copyOf(effects);
        }
    }

    /**
     * One option.
     *
     * @param id       its id within the decision
     * @param label    what the player picks
     * @param shows    what it shows in advance, or {@code ""}
     * @param cost     what choosing it costs, as a share of a season's funding; 0 for nothing
     * @param takes    who it takes
     * @param odds     how its good outcome's chance is set
     * @param outcomes its outcomes
     */
    public record Option(String id, String label, String shows, double cost, Takes takes, Odds odds,
                         List<Outcome> outcomes) {

        /**
         * Fills defaults and copies the outcomes.
         *
         * @throws NullPointerException if a required field is {@code null}
         */
        public Option {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(label, "label must not be null");
            shows = shows == null ? "" : shows;
            takes = takes == null ? Takes.NOBODY : takes;
            odds = odds == null ? Odds.FIXED : odds;
            outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
        }
    }

    /**
     * One decision.
     *
     * @param id            its id, never reused
     * @param type          its type
     * @param fits          which components it fits
     * @param headline      the headline
     * @param text          the story, one to three sentences
     * @param clues         a diagnosis's clues, else empty
     * @param stakes        a line every option shares ("A right answer is worth..."), or {@code ""}
     * @param options       two to four options
     * @param defaultOption the option that applies if the player doesn't decide
     * @param explainers    explainer ids for concept chips
     * @param followUp      the decision a continuing outcome opens later, or {@code null}
     * @param step          its step in a story arc: 1 for a decision that starts one or stands alone
     * @param recall        for a follow-up, the line recalling the step before, else {@code ""}
     */
    public record Decision(String id, Type type, Fits fits, String headline, String text, List<Clue> clues,
                           String stakes, List<Option> options, String defaultOption, List<String> explainers,
                           String followUp, int step, String recall) {

        /**
         * Fills defaults and copies the lists.
         *
         * @throws NullPointerException if a required field is {@code null}
         */
        public Decision {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(headline, "headline must not be null");
            Objects.requireNonNull(text, "text must not be null");
            Objects.requireNonNull(defaultOption, "defaultOption must not be null");
            fits = fits == null ? Fits.ANY : fits;
            clues = clues == null ? List.of() : List.copyOf(clues);
            stakes = stakes == null ? "" : stakes;
            options = options == null ? List.of() : List.copyOf(options);
            explainers = explainers == null ? List.of() : List.copyOf(explainers);
            step = Math.max(1, step);
            recall = recall == null ? "" : recall;
        }

        /**
         * Returns an option by id.
         *
         * @param optionId the id
         * @return the option
         */
        public Optional<Option> option(String optionId) {
            return options.stream().filter(option -> option.id().equals(optionId)).findFirst();
        }

        /**
         * Returns the default option.
         *
         * @return the option
         * @throws IllegalStateException if the file names a default it doesn't have
         */
        public Option defaultChoice() {
            return option(defaultOption).orElseThrow(() -> new IllegalStateException("No default " + defaultOption
                + " in " + id));
        }

        /**
         * Returns an outcome by id, across the options.
         *
         * @param outcomeId the id
         * @return the outcome
         */
        public Optional<Outcome> outcome(String outcomeId) {
            return options.stream().flatMap(option -> option.outcomes().stream())
                .filter(outcome -> outcome.id().equals(outcomeId)).findFirst();
        }
    }

    /**
     * Reads the decisions.
     *
     * @param decisions the decisions
     */
    @JsonCreator
    public Decisions(@JsonProperty("decisions") List<Decision> decisions) {
        this.decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions must not be null"));
    }

    /**
     * Returns the decisions shipped on the classpath, read once.
     *
     * @return the decisions, {@link #EMPTY} if the file is missing
     * @throws UncheckedIOException if the file can't be read
     */
    public static Decisions standard() {
        Decisions loaded = standard;
        if (loaded == null) {
            try (InputStream in = Decisions.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, Decisions.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Replaces the decisions {@link #standard()} returns, for tests and tools that run without the
     * assets on the classpath (core's tests, the playout bots).
     *
     * @param decisions the decisions
     */
    public static void setStandard(Decisions decisions) {
        standard = Objects.requireNonNull(decisions, "decisions must not be null");
    }

    /**
     * Reads the decisions from a file.
     *
     * @param file the file
     * @return the decisions
     * @throws UncheckedIOException if the file can't be read
     */
    public static Decisions load(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return new ObjectMapper().readValue(in, Decisions.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns a decision by id.
     *
     * @param id the id, or {@code null}
     * @return the decision
     */
    public Optional<Decision> byId(String id) {
        return id == null ? Optional.empty() : decisions.stream().filter(decision -> decision.id().equals(id))
            .findFirst();
    }
}
