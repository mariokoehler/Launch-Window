package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.rules.HiringPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.Stream;

/**
 * <p>Makes a player's slot in the game, for an account that claims one ({@link RequestDispatcher}) and for
 * a filler bot ({@code BotPlayers}) alike: the starting state, with names unique in the game.</p>
 *
 * @author Mario Koehler
 */
public final class PlayerSlots {

    private PlayerSlots() {
    }

    /**
     * Creates a player's slot: the archetype, a unique agency name and director name, and the starting
     * state (the budget schedule's starting values, every building at level 1, empty rosters, and fresh
     * candidate pools; user decision, 2026-10-03). A late joiner starts at their own season 1
     * ({@code design.md} &sect;3.8) and knows what was published before it joined (&sect;2.13).
     *
     * @param dataRoot     the data root
     * @param mapper       the persistence mapper
     * @param schedule     the budget schedule
     * @param gameSeed     the game's seed
     * @param playerId     the player's folder name: an account's id, or a bot's
     * @param archetype    the archetype
     * @param agencyName   the agency's name, stripped
     * @param directorName the director's name, stripped
     * @return the response
     * @throws IOException if a player's files can't be read or the slot can't be written
     */
    public static ClaimSlotResponse claim(Path dataRoot, ObjectMapper mapper, BudgetSchedule schedule, long gameSeed,
            String playerId, Archetype archetype, String agencyName, String directorName) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(dataRoot.resolve("players").resolve(playerId), mapper);
        if (store.load().isPresent()) {
            return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.ALREADY_CLAIMED, "This account already has a slot in this game.");
        }
        Map<String, Integer> publicFindings = new LinkedHashMap<>();
        for (PlayerSnapshot other : everyPlayer(dataRoot, mapper)) {
            // A late joiner knows what was published before it joined (design.md section 2.13).
            other.getPublicFindings().forEach((goal, turn) -> publicFindings.merge(goal, turn, Math::min));
            if (agencyName.equalsIgnoreCase(other.getAgencyName())) {
                return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.AGENCY_NAME_TAKEN, "The agency name \"" + agencyName + "\" is already taken.");
            }
            if (directorName.equalsIgnoreCase(other.getDirectorName())) {
                return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.DIRECTOR_NAME_TAKEN, "The director name \"" + directorName + "\" is already taken.");
            }
        }
        PlayerSnapshot start = new PlayerSnapshot(agencyName, archetype, schedule.initialFunds(),
            schedule.initialPrestige(), schedule.initialFundingRate(), 1);
        start.setDirectorName(directorName);
        start.setPublicFindings(publicFindings);
        HiringPool.refreshCandidates(start, new SplittableRandom(GameSeeds.forNewPlayer(gameSeed, playerId)));
        store.save(0, start, null);
        return ClaimSlotResponse.success("Welcome, Director " + directorName + " of " + agencyName + ".");
    }

    /**
     * Loads every claimed player's latest snapshot.
     *
     * @param dataRoot the data root
     * @param mapper   the persistence mapper
     * @return the snapshots
     * @throws IOException if a player's files can't be read
     */
    public static List<PlayerSnapshot> everyPlayer(Path dataRoot, ObjectMapper mapper) throws IOException {
        Path players = dataRoot.resolve("players");
        List<PlayerSnapshot> result = new ArrayList<>();
        if (!Files.exists(players)) {
            return result;
        }
        try (Stream<Path> listing = Files.list(players)) {
            for (Path playerDir : listing.filter(Files::isDirectory).toList()) {
                new PlayerSnapshotStore(playerDir, mapper).load().ifPresent(result::add);
            }
        }
        return result;
    }
}
