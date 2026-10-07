package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything an agency's intelligence service knows about one rival ({@code design.md} &sect;2.9,
 * {@code ui-spec.md} &sect;4.15's folders): the newest dossier per area, personnel files, tidbits,
 * catches both ways, and the rival's Intelligence level as last learned.
 *
 * @author Mario Koehler
 */
public class RivalFolder {

    /** The rival's level was learned from a programs-and-plans dossier. */
    public static final String FROM_DOSSIER = "DOSSIER";
    /** The rival's level was learned when they caught one of our agents. */
    public static final String FROM_BEING_CAUGHT = "CAUGHT";
    /** The rival's level was learned when we caught one of theirs. */
    public static final String FROM_A_CATCH = "CATCH";

    private String agencyName;
    private Map<IntelArea, Dossier> dossiers = new LinkedHashMap<>();
    private int knownLevel = -1;
    private int knownLevelTurn = -1;
    private String knownLevelSource;
    private List<PersonnelFile> personnelFiles = new ArrayList<>();
    private List<Tidbit> tidbits = new ArrayList<>();
    private List<IntelCatch> catches = new ArrayList<>();

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public RivalFolder() {
    }

    /**
     * Creates an empty folder.
     *
     * @param agencyName the rival's agency name
     */
    public RivalFolder(String agencyName) {
        this.agencyName = agencyName;
    }

    /**
     * Records the rival's level, unless a newer one is already known.
     *
     * @param level  the level
     * @param turn   the game turn it held
     * @param source where it came from, one of the {@code FROM_} constants
     */
    public void learnLevel(int level, int turn, String source) {
        if (turn >= knownLevelTurn) {
            knownLevel = level;
            knownLevelTurn = turn;
            knownLevelSource = source;
        }
    }

    /**
     * Returns the rival's agency name, as it was when last seen.
     *
     * @return the name
     */
    public String getAgencyName() {
        return agencyName;
    }

    /**
     * Sets the rival's agency name.
     *
     * @param agencyName the name
     */
    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    /**
     * Returns the newest dossier per area.
     *
     * @return the dossiers, live
     */
    public Map<IntelArea, Dossier> getDossiers() {
        return dossiers;
    }

    /**
     * Sets the dossiers.
     *
     * @param dossiers the dossiers
     */
    public void setDossiers(Map<IntelArea, Dossier> dossiers) {
        this.dossiers = dossiers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dossiers);
    }

    /**
     * Returns the rival's Intelligence level as last learned, or -1 if never.
     *
     * @return the level
     */
    public int getKnownLevel() {
        return knownLevel;
    }

    /**
     * Sets the known level.
     *
     * @param knownLevel the level, or -1
     */
    public void setKnownLevel(int knownLevel) {
        this.knownLevel = knownLevel;
    }

    /**
     * Returns the game turn the known level held, or -1.
     *
     * @return the turn
     */
    public int getKnownLevelTurn() {
        return knownLevelTurn;
    }

    /**
     * Sets the game turn the known level held.
     *
     * @param knownLevelTurn the turn
     */
    public void setKnownLevelTurn(int knownLevelTurn) {
        this.knownLevelTurn = knownLevelTurn;
    }

    /**
     * Returns where the known level came from, one of the {@code FROM_} constants, or {@code null}.
     *
     * @return the source
     */
    public String getKnownLevelSource() {
        return knownLevelSource;
    }

    /**
     * Sets where the known level came from.
     *
     * @param knownLevelSource the source
     */
    public void setKnownLevelSource(String knownLevelSource) {
        this.knownLevelSource = knownLevelSource;
    }

    /**
     * Returns the personnel files, one per person, newest last.
     *
     * @return the files, live
     */
    public List<PersonnelFile> getPersonnelFiles() {
        return personnelFiles;
    }

    /**
     * Sets the personnel files.
     *
     * @param personnelFiles the files
     */
    public void setPersonnelFiles(List<PersonnelFile> personnelFiles) {
        this.personnelFiles = personnelFiles == null ? new ArrayList<>() : new ArrayList<>(personnelFiles);
    }

    /**
     * Returns the tidbits, oldest first.
     *
     * @return the tidbits, live
     */
    public List<Tidbit> getTidbits() {
        return tidbits;
    }

    /**
     * Sets the tidbits.
     *
     * @param tidbits the tidbits
     */
    public void setTidbits(List<Tidbit> tidbits) {
        this.tidbits = tidbits == null ? new ArrayList<>() : new ArrayList<>(tidbits);
    }

    /**
     * Returns the catches both ways, oldest first.
     *
     * @return the catches, live
     */
    public List<IntelCatch> getCatches() {
        return catches;
    }

    /**
     * Sets the catches.
     *
     * @param catches the catches
     */
    public void setCatches(List<IntelCatch> catches) {
        this.catches = catches == null ? new ArrayList<>() : new ArrayList<>(catches);
    }
}
