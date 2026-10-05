package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.PersonnelNames.Identity;
import de.mkoehler.launchwindow.rules.PersonnelNames.NameLists;
import de.mkoehler.launchwindow.rules.PortraitCatalog.Portrait;
import de.mkoehler.launchwindow.rules.PortraitCatalog.Sex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link PersonnelNames} and {@link PortraitCatalog} (gap G5).
 *
 * @author Mario Koehler
 */
class PersonnelNamesTest {

    /**
     * Every agency has portraits for every personnel group.
     */
    @Test
    void everyAgencyAndGroupHasPortraits() {
        for (Archetype archetype : Archetype.values()) {
            for (PersonnelGroup group : PersonnelGroup.values()) {
                assertFalse(PortraitCatalog.of(archetype, group).isEmpty(), archetype + " " + group);
            }
        }
    }

    /**
     * Every portrait's ethnicity has name lists, and every gendered surname has exactly two forms.
     */
    @Test
    void everyPortraitHasNames() {
        for (Portrait portrait : PortraitCatalog.all()) {
            assertNotNull(PersonnelNames.all().get(portrait.ethnicity()),
                portrait.id() + " has no names for " + portrait.ethnicity());
        }
        for (Map.Entry<String, NameLists> entry : PersonnelNames.all().entrySet()) {
            for (String surname : entry.getValue().surnames()) {
                String[] forms = surname.split("/", -1);
                assertTrue(forms.length <= 2 && !forms[0].isBlank() && !forms[forms.length - 1].isBlank(),
                    entry.getKey() + ": " + surname);
            }
        }
    }

    /**
     * Every letter in a name is one the client's fonts are generated with, or it would draw as a box.
     */
    @Test
    void everyLetterIsInTheFonts() {
        for (Map.Entry<String, NameLists> entry : PersonnelNames.all().entrySet()) {
            List<String> names = new ArrayList<>(entry.getValue().male());
            names.addAll(entry.getValue().female());
            names.addAll(entry.getValue().surnames());
            for (String name : names) {
                for (char c : name.replace("/", "").replace(" ", "").toCharArray()) {
                    assertTrue(Theme.CHARACTERS.indexOf(c) >= 0, entry.getKey() + ": '" + c + "' in " + name);
                }
            }
        }
    }

    /**
     * A candidate's identity is stable for the same id and agency, and matches its portrait: the agency,
     * the group, the age range, and a first name from the lists for the portrait's ethnicity and sex.
     */
    @Test
    void identitiesAreStableAndMatchTheirPortrait() {
        assertEquals(identity("p12", Archetype.SOVIET, "OKB-1"), identity("p12", Archetype.SOVIET, "OKB-1"));
        Set<String> names = new HashSet<>();
        for (Archetype archetype : Archetype.values()) {
            for (int i = 1; i <= 200; i++) {
                Identity who = identity("p" + i, archetype, "Orion");
                names.add(who.name());
                Portrait portrait = PortraitCatalog.byId(who.portraitId());
                assertEquals(archetype, portrait.archetype());
                assertEquals(PersonnelGroup.SET, portrait.group());
                assertTrue(who.age() >= portrait.minAge() && who.age() <= portrait.maxAge());
                NameLists lists = PersonnelNames.namesFor(portrait.ethnicity());
                String first = who.name().substring(0, who.name().lastIndexOf(' '));
                assertTrue((portrait.sex() == Sex.FEMALE ? lists.female() : lists.male()).contains(first),
                    who.name() + " for " + portrait);
            }
        }
        assertTrue(names.size() > 300, "400 candidates get mostly different names: " + names.size());
    }

    /**
     * First names and surnames are drawn independently, so a famous person's full name can turn up even
     * when neither half is famous alone. None of these can be generated; add to the list when a new
     * combination is found.
     */
    @Test
    void noFamousFullName() {
        List<String> famous = List.of("Booker Washington", "Tigran Petrosyan", "Tigran Sargsyan",
            "Karen Karapetyan", "Alisher Usmanov", "Vincent Lombardi", "Grigory Orlov", "Oleg Orlov",
            "Mikhail Vorontsov", "Dmitri Polyansky", "Yuri Gagarin", "Valentina Tereshkova", "Sergei Korolev",
            "John Glenn", "Alan Shepard", "Ellen Ochoa");
        Set<String> possible = new HashSet<>();
        for (NameLists lists : PersonnelNames.all().values()) {
            for (String surname : lists.surnames()) {
                for (String first : lists.male()) {
                    possible.add(first + " " + PersonnelNames.surname(surname, Sex.MALE));
                }
                for (String first : lists.female()) {
                    possible.add(first + " " + PersonnelNames.surname(surname, Sex.FEMALE));
                }
            }
        }
        for (String name : famous) {
            assertFalse(possible.contains(name), name);
        }
    }

    /**
     * Surnames take the form for the person's sex.
     */
    @Test
    void surnamesTakeTheFormForTheSex() {
        assertEquals("Ozerova", PersonnelNames.surname("Ozerov/Ozerova", Sex.FEMALE));
        assertEquals("Ozerov", PersonnelNames.surname("Ozerov/Ozerova", Sex.MALE));
        assertEquals("Kazlauskienė", PersonnelNames.surname("Kazlauskas/Kazlauskienė", Sex.FEMALE));
        assertEquals("Beridze", PersonnelNames.surname("Beridze", Sex.FEMALE));
    }

    /**
     * Taken portraits are avoided while any other is free, recent ones while enough others are free, and
     * once every portrait is taken one is reused rather than failing.
     */
    @Test
    void takenAndRecentPortraitsAreAvoided() {
        List<Portrait> all = PortraitCatalog.of(Archetype.NASA, PersonnelGroup.ASTRONAUT);
        Set<String> allButOne = new HashSet<>();
        for (int i = 1; i < all.size(); i++) {
            allButOne.add(all.get(i).id());
        }
        for (int i = 0; i < 20; i++) {
            assertEquals(all.get(0).id(), PersonnelNames.forCandidate("p" + i, Archetype.NASA, "Orion",
                PersonnelGroup.ASTRONAUT, allButOne, Set.of()).portraitId());
            assertNotEquals(all.get(0).id(), PersonnelNames.forCandidate("p" + i, Archetype.NASA, "Orion",
                PersonnelGroup.ASTRONAUT, Set.of(), Set.of(all.get(0).id())).portraitId());
            assertEquals(all.get(0).id(), PersonnelNames.forCandidate("p" + i, Archetype.NASA, "Orion",
                PersonnelGroup.ASTRONAUT, allButOne, Set.of(all.get(0).id())).portraitId());
        }
        Set<String> everything = new HashSet<>(allButOne);
        everything.add(all.get(0).id());
        assertNotNull(PersonnelNames.forCandidate("p1", Archetype.NASA, "Orion", PersonnelGroup.ASTRONAUT,
            everything, Set.of()).portraitId());
    }

    /**
     * The hiring pool gives its candidates portraits, names and ages that match. Two agencies with the
     * same pool seed get the same skills but different identities: an identity depends on the agency, the
     * skills only on the pool's stream.
     */
    @Test
    void thePoolGivesCandidatesMatchingIdentitiesWithoutTouchingItsStream() {
        PlayerSnapshot first = new PlayerSnapshot("Orion", Archetype.NASA, 0, 0, 0, 1);
        PlayerSnapshot second = new PlayerSnapshot("Vega", Archetype.SOVIET, 0, 0, 0, 1);
        HiringPool.refreshCandidates(first, new SplittableRandom(7));
        HiringPool.refreshCandidates(second, new SplittableRandom(7));
        assertEquals(first.getSetSpecialistCandidates().get(0).getSkills(),
            second.getSetSpecialistCandidates().get(0).getSkills());
        assertNotEquals(first.getSetSpecialistCandidates().get(0).getName(),
            second.getSetSpecialistCandidates().get(0).getName());
        for (Personnel candidate : first.getAstronautCandidates()) {
            Portrait portrait = PortraitCatalog.byId(candidate.getPortraitId());
            assertEquals(PersonnelGroup.ASTRONAUT, portrait.group());
            assertTrue(candidate.getAge() >= portrait.minAge() && candidate.getAge() <= portrait.maxAge());
        }
    }

    /**
     * A batch never repeats a portrait, nor gives out one on the roster or the memorial, nor (while there
     * are enough others) one from the pool it replaces.
     */
    @Test
    void aBatchAvoidsRosterMemorialAndThePreviousPool() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Orion", Archetype.NASA, 0, 0, 0, 1);
        List<Portrait> portraits = PortraitCatalog.of(Archetype.NASA, PersonnelGroup.ASTRONAUT);
        Astronaut veteran = new Astronaut("p900", "Veteran", 35, 60, 50);
        veteran.setPortraitId(portraits.get(0).id());
        snapshot.setAstronauts(new ArrayList<>(List.of(veteran)));
        snapshot.setMemorial(new ArrayList<>(List.of(new MemorialEntry("Lost", 30, 3, MemorialEntry.Cause.ACCIDENT,
            null, null, portraits.get(1).id()))));
        SplittableRandom random = new SplittableRandom(3);
        for (int round = 0; round < 10; round++) {
            Set<String> previous = new HashSet<>();
            snapshot.getAstronautCandidates().forEach(c -> previous.add(c.getPortraitId()));
            HiringPool.refreshCandidates(snapshot, random);
            Set<String> batch = new HashSet<>();
            for (Personnel candidate : snapshot.getAstronautCandidates()) {
                assertTrue(batch.add(candidate.getPortraitId()), "repeated in a batch");
                assertNotEquals(portraits.get(0).id(), candidate.getPortraitId());
                assertNotEquals(portraits.get(1).id(), candidate.getPortraitId());
                assertFalse(previous.contains(candidate.getPortraitId()), "repeated from the previous pool");
            }
        }
    }

    private static Identity identity(String id, Archetype archetype, String agency) {
        return PersonnelNames.forCandidate(id, archetype, agency, PersonnelGroup.SET, Set.of(), Set.of());
    }
}
