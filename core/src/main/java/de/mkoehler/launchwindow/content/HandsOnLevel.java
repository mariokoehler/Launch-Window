package de.mkoehler.launchwindow.content;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>A hands-on decision's puzzle ({@code ui-spec.md} component 26, type 11; {@code design.md} &sect;5,
 * Backlog, the user's idea, 2026-10-11): the resources, the commands that turn some into others, how
 * many rounds of how many commands, where the player starts, and the objective and the bonus. Read
 * from {@code decisions.json} with its decision. The rules are {@code rules.HandsOn}.</p>
 *
 * <p>The first resource is the one that pays the basic commands and calls in an expert on a
 * failure (shifts in a test campaign).</p>
 *
 * @param resources the resources, the paying one first
 * @param commands  the commands, in the table's order
 * @param rounds    the rounds
 * @param slots     the commands a round
 * @param start     what the player holds at the start, by resource
 * @param objective the amounts held at the end that make a success
 * @param bonus     the higher amounts that make a bonus success, and end the puzzle when met
 * @param resist    what calling in an expert on a failure costs, in the first resource
 * @author Mario Koehler
 */
public record HandsOnLevel(List<Resource> resources, List<Command> commands, int rounds, int slots,
                           Map<String, Integer> start, Map<String, Integer> objective, Map<String, Integer> bonus,
                           int resist) {

    /**
     * One resource.
     *
     * @param id    its id within the level ({@code shift})
     * @param name  its name, plural ({@code Shifts})
     * @param one   its name for one ({@code shift})
     * @param icon  its icon ({@code res_shift})
     * @param color its colour token: {@code gold}, {@code danger}, {@code navy} or {@code success}
     */
    public record Resource(String id, String name, String one, String icon, String color) {

        /**
         * Checks the fields.
         *
         * @throws NullPointerException if a field is {@code null}
         */
        public Resource {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(one, "one must not be null");
            Objects.requireNonNull(icon, "icon must not be null");
            Objects.requireNonNull(color, "color must not be null");
        }
    }

    /**
     * One command.
     *
     * @param id      its id within the level
     * @param name    its name
     * @param group   the resource whose group it sits in, the one it makes
     * @param input   what it costs, by resource; paid when queued
     * @param output  what it makes, by resource, the main output first; credited at execution
     * @param primary the main output, the one a roll moves by one
     * @param rolls   whether it rolls; the recharge command doesn't
     */
    public record Command(String id, String name, String group, Map<String, Integer> input,
                          Map<String, Integer> output, String primary, boolean rolls) {

        /**
         * Checks the fields and keeps the amounts in file order.
         *
         * @throws NullPointerException if a field is {@code null}
         */
        public Command {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(group, "group must not be null");
            input = input == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(input));
            output = output == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(output));
            Objects.requireNonNull(primary, "primary must not be null");
        }
    }

    /**
     * Checks the level and keeps the amounts in file order.
     *
     * @throws NullPointerException     if a field is {@code null}
     * @throws IllegalArgumentException if a command names a resource the level doesn't have, or no
     *                                  command is free to queue
     */
    public HandsOnLevel {
        resources = List.copyOf(Objects.requireNonNull(resources, "resources must not be null"));
        commands = List.copyOf(Objects.requireNonNull(commands, "commands must not be null"));
        start = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(start, "start")));
        objective = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(objective,
            "objective")));
        bonus = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(bonus, "bonus")));
        resist = resist <= 0 ? 1 : resist;
        if (resources.isEmpty() || rounds <= 0 || slots <= 0) {
            throw new IllegalArgumentException("a hands-on level needs resources, rounds and slots");
        }
        List<String> ids = resources.stream().map(Resource::id).toList();
        for (Command command : commands) {
            for (String id : command.input().keySet()) {
                require(ids, id, command.id());
            }
            for (String id : command.output().keySet()) {
                require(ids, id, command.id());
            }
            require(ids, command.group(), command.id());
            if (!command.output().containsKey(command.primary())) {
                throw new IllegalArgumentException(command.id() + "'s main output isn't one of its outputs");
            }
        }
        if (commands.stream().noneMatch(command -> command.input().isEmpty())) {
            throw new IllegalArgumentException("a hands-on level needs a command with no input");
        }
    }

    private static void require(List<String> ids, String id, String command) {
        if (!ids.contains(id)) {
            throw new IllegalArgumentException(command + " names unknown resource " + id);
        }
    }

    /**
     * Returns the resource that pays the basic commands and the experts.
     *
     * @return the first resource
     */
    public Resource payer() {
        return resources.getFirst();
    }

    /**
     * Returns a resource by id.
     *
     * @param id the id
     * @return the resource, or empty
     */
    public Optional<Resource> resource(String id) {
        return resources.stream().filter(resource -> resource.id().equals(id)).findFirst();
    }

    /**
     * Returns a command by id.
     *
     * @param id the id
     * @return the command, or empty
     */
    public Optional<Command> command(String id) {
        return commands.stream().filter(command -> command.id().equals(id)).findFirst();
    }
}
