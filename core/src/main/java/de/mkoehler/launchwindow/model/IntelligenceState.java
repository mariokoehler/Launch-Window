package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An agency's intelligence service ({@code design.md} &sect;2.9, Intelligence): its level and the
 * target it moves toward one step a season, the season's actions, the operations queued, and a
 * folder per rival.
 *
 * @author Mario Koehler
 */
public class IntelligenceState {

    private int level;
    private int targetLevel;
    private int actionsUsed;
    private long nextOperationId;
    private List<IntelOperation> queued = new ArrayList<>();
    private Map<String, RivalFolder> folders = new LinkedHashMap<>();

    /**
     * No-argument constructor for Jackson and Kryo; level 0, no service.
     */
    public IntelligenceState() {
    }

    /**
     * Returns a new operation id.
     *
     * @return the id
     */
    public String allocateOperationId() {
        return "op" + nextOperationId++;
    }

    /**
     * Returns a rival's folder, creating it if there is none yet.
     *
     * @param playerId   the rival's player id
     * @param agencyName the rival's agency name, kept current
     * @return the folder
     */
    public RivalFolder folder(String playerId, String agencyName) {
        RivalFolder folder = folders.computeIfAbsent(playerId, id -> new RivalFolder(agencyName));
        if (agencyName != null) {
            folder.setAgencyName(agencyName);
        }
        return folder;
    }

    /**
     * Returns the level in effect this season, 0 to 3.
     *
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the level.
     *
     * @param level the level
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the level the service moves toward, one step a season.
     *
     * @return the target
     */
    public int getTargetLevel() {
        return targetLevel;
    }

    /**
     * Sets the target level.
     *
     * @param targetLevel the target
     */
    public void setTargetLevel(int targetLevel) {
        this.targetLevel = targetLevel;
    }

    /**
     * Returns the actions used this season: background checks and queued operations.
     *
     * @return the count
     */
    public int getActionsUsed() {
        return actionsUsed;
    }

    /**
     * Sets the actions used this season.
     *
     * @param actionsUsed the count
     */
    public void setActionsUsed(int actionsUsed) {
        this.actionsUsed = actionsUsed;
    }

    /**
     * Returns the next operation id's number.
     *
     * @return the number
     */
    public long getNextOperationId() {
        return nextOperationId;
    }

    /**
     * Sets the next operation id's number.
     *
     * @param nextOperationId the number
     */
    public void setNextOperationId(long nextOperationId) {
        this.nextOperationId = nextOperationId;
    }

    /**
     * Returns the operations queued this season.
     *
     * @return the operations, live
     */
    public List<IntelOperation> getQueued() {
        return queued;
    }

    /**
     * Sets the queued operations.
     *
     * @param queued the operations
     */
    public void setQueued(List<IntelOperation> queued) {
        this.queued = queued == null ? new ArrayList<>() : new ArrayList<>(queued);
    }

    /**
     * Returns the folders, by rival player id.
     *
     * @return the folders, live
     */
    public Map<String, RivalFolder> getFolders() {
        return folders;
    }

    /**
     * Sets the folders.
     *
     * @param folders the folders
     */
    public void setFolders(Map<String, RivalFolder> folders) {
        this.folders = folders == null ? new LinkedHashMap<>() : new LinkedHashMap<>(folders);
    }
}
