# The archive's first pages: images, audio and rights

The candidates for the archive (`design.md` §2.12, `ui-spec.md` §4.19), researched 2026-10-15. Each page
is a real mission's story and what it sent back, unlocked in a game by the goal it names. Nothing here is
downloaded or committed yet; every file goes into `assets/CREDITS.md` with its source when it is.

**Where the files are:**
- **NASA's library:** `https://images.nasa.gov/details/<id>`, original at
  `https://images-assets.nasa.gov/image/<id>/<id>~orig.jpg`.
- **JPL's Photojournal (PIA numbers):** now at `science.nasa.gov/photojournal/...`; every PIA below is also in
  NASA's library.
- **NSSDC:** `https://nssdc.gsfc.nasa.gov/imgcat/html/object_page/<name>.html`, TIFF under `imgcat/higher_res/`.

**The rules:**
- **NASA's own images** are not copyrighted in the US (https://www.nasa.gov/nasa-brand-center/images-and-media/).
  Credit NASA as the source, and imply no endorsement. The NASA insignia ("meatball"), logotype ("worm") and
  seal are not free: keep them out of the game's own art and UI. People in photos may have publicity rights,
  so never use them in promotion.
- **JPL's** (https://www.jpl.nasa.gov/jpl-image-use-policy): "may be used for any purpose without prior
  permission" unless the caption says otherwise. Credit "NASA/JPL-Caltech", or as the caption does.
- **Only originals:** a PIA, ARC (Ames), LRC (Langley) or NSSDC original with a NASA, JPL or Ames credit is
  safe. Anything credited to a person or a society outside NASA is not: the Planetary Society's, Ted Stryk's,
  LOIRP's, Don P. Mitchell's and amateurs' reprocessings are their own work. The exceptions are JPL staff's
  (PIA14033, PIA23645).
- **Soviet images are not public domain by default.** NSSDC marks them "Russian Space Agency, not
  necessarily in the public domain". Germany's law decides for us: §72(3) UrhG protects a photo without
  creative input (a *Lichtbild*) for 50 years from publication, and Germany never protects a foreign work
  longer than its home country. So the 1959 to 1975 images are free here. Russia's own term (70 years
  from publication for a legal entity's work, under the reading Wikimedia calls `PD-Russia`) runs longer,
  which matters only if the repo goes public. Not legal advice: recheck these before then.

## The pages

### Luna 3: the far side of the Moon (USSR, October 1959)
- **Unlocked by:** `lunar-photos` (Pictures of the Moon).
- **Images:**
  - The far-side frame `lu3_1` (NSSDC, TIFF `higher_res/luna_3/lu3_1.tiff`), or Commons
    `File:Luna_3_moon.jpg`, which Wikimedia reviewed and kept.
  - Credit: "Luna 3 (USSR, 1959). Image via NASA/NSSDC."
  - Risk: low (out of German protection; Russia's term ends 2029).

### Ranger 7: the Moon up close (NASA/JPL, July 1964)
- **Unlocked by:** `lunar-close-up-photos` (Close-ups of the Moon).
- **Images:**
  - PIA02975, the first picture of the Moon by a US spacecraft, about 17 minutes before impact.
  - PIA02974, crater Guericke, 8.5 minutes before impact.
  - The last frame, cut off at impact (NSSDC `ra7_p200`). Its sources disagree on the width shown, so
    don't quote one.
  - Credit: NASA/JPL.

### The first spacewalks: Leonov and White (USSR March 1965, NASA June 1965)
- **Unlocked by:** `spacewalk` (Spacewalk).
- **Images:**
  - S65-30431, Ed White over the Pacific, the EVA's first photo, taken by McDivitt (credit NASA).
  - S65-30433, White with his gold visor (credit NASA).
- **Leonov:** his film frames are still protected in Russia (to about 2035), so his part is told in words.
  A 1965 Soviet stamp shown whole is an option (Wikimedia's `PD-RU-exempt`; cropping a stamp can infringe).

### Lunar Orbiter 1: the Earth from the Moon (NASA, August 1966)
- **Unlocked by:** `lunar-mapping` (Moon mapped).
- **Images:**
  - The first picture of the Earth from the Moon's vicinity, 23 August 1966, frame 102 H1 to H3
    (NSSDC `lo1_h102_123`, or Langley's scan LRC-1966-B701_P-07825; credit NASA/Langley).
  - Lunar Orbiter 2's oblique of Copernicus, frame 162H, "the picture of the century" (NSSDC `lo2_h162_123`,
    Langley LRC-1967-B701_P-01194).
  - Not LOIRP's sharper restorations: their terms are unchecked.

### Apollo 8: Earthrise (NASA, December 1968)
- **Unlocked by:** `crewed-lunar-orbit` (People in lunar orbit).
- **Images (credit NASA):**
  - AS08-14-2383, the colour Earthrise.
  - AS08-13-2329, the first, black-and-white Earthrise. Sources disagree on who took it.
  - AS08-14-2453, the far side just after lunar orbit insertion.
  - AS08-17-2670, a near-vertical view of the far side.
- **Audio:** the Genesis reading, from NSSDC's broadcast video `as8_genesis1a.mov`
  (https://nssdc.gsfc.nasa.gov/planetary/lunar/apollo8_xmas.html; credit NASA).

### Apollo 11: Tranquility Base (NASA, July 1969)
- **Unlocked by:** `crewed-lunar-landing` (People on the Moon).
- **Images (credit NASA):**
  - AS11-40-5903, Aldrin by the lunar module.
  - AS11-40-5878, a bootprint.
  - AS11-40-5875, Aldrin and the flag.
  - AS11-44-6581, Eagle seen from Columbia before the descent.
  - AS11-44-6642, Eagle's ascent stage coming back, the Earth behind.
- **Audio:** "Tranquility Base here. The Eagle has landed."
  (https://nssdc.gsfc.nasa.gov/planetary/lunar/a11landing.aiff; credit NASA).

### Mariner 4: the first pictures of Mars (NASA/JPL, July 1965)
- **Unlocked by:** `mars-photos` (Pictures of Mars).
- **Images:**
  - PIA14033, the first picture coloured by hand from the printed numbers, paint by numbers (credit
    NASA/JPL-Caltech/Dan Goods, a JPL staff member).
  - PIA14032, the first TV image.
  - PIA02979, Mariner crater, frame 11E (credit NASA/JPL).

### Mariner 9: the first orbiter of another planet (NASA/JPL, 1971 to 1972)
- **Unlocked by:** `mars-mapping` (Mars mapped).
- **Images (credit NASA/JPL):**
  - PIA02995, Nix Olympica identified on the approach.
  - PIA02999, a volcano's summit over the dust storm. Caption it Ascraeus Mons (corrected in 2017),
    not Olympus.
  - PIA02998, the canyon system emerging from the storm.
  - Phobos, MTVS 4109-9 (NSSDC `m09_mtvs4109_09`), not the Planetary Society's reprocessing.

### Viking 1: on the surface of Mars (NASA/JPL, July 1976)
- **Unlocked by:** `mars-orbiter-and-lander` (Mars orbiter and lander).
- **Images (credit NASA/JPL):**
  - PIA00381, the first photograph taken on the surface, with footpad 2.
  - PIA00382 or PIA00383, the first panorama.
  - PIA00563, the first colour image, colour-corrected in 1997.
  - Optional: the "Face on Mars", Viking Orbiter 1 frame 35A72 (PIA01141), beside Mars Global Surveyor's
    1998 view showing a mesa (PIA01440).

### Pioneer 10: Jupiter (NASA Ames, December 1973)
- **Unlocked by:** `jupiter-photos` (Pictures of Jupiter).
- **Images (credit NASA Ames Research Center):**
  - ARC-1973-AC73-9253, Jupiter on 2 December from 2.58 million km.
  - ARC-1973-AC73-9278, a crescent Jupiter near closest approach.
  - ARC-1973-AC73-9279, Ganymede.
  - The plaque, ARC-1972-AC72-1338: designed by Carl Sagan and Frank Drake, drawn by Linda Salzman
    Sagan. NASA distributes it with no copyright mark; a small residual risk.
  - Not the ARC-1978-A78-0077 series, which is courtesy of private owners.

### Venus's surface: Venera 9 and Venera 13 (USSR, 1975 and 1982)
- **Unlocked by:** `venus-surface-study` (Venus's surface studied).
- **Images:**
  - The Venera 13 colour composite `v13_vg261_262` (NSSDC; TIFF `higher_res/venera/v13_vg261_262.tiff`).
    It carries no Mitchell note, but its processor is unconfirmed. Risk: medium (German protection to
    2032).
  - Venera 9's first picture: every NSSDC version is "copyright 2003 Don P. Mitchell". Use it only with
    his permission (http://mentallandscape.com/Copyright.htm: "I require permission… which I generally
    grant without any fee"), credited "Processing © Don P. Mitchell, used with permission". The user's
    call whether to ask.
- **Credit:** "Venera 13 (USSR, 1982). Image via NASA/NSSDC."

### Voyager 1: the Pale Blue Dot (NASA/JPL, 14 February 1990)
- **Unlocked by:** `saturn-flyby` (Saturn flyby), the furthest flight in the game. Voyager isn't in either
  pack (`design.md` §2.12).
- **Images (credit NASA/JPL-Caltech):**
  - PIA00452, the Pale Blue Dot (1990).
  - PIA23645, Pale Blue Dot Revisited (2020, JPL's own reprocessing).
  - PIA00451, the Family Portrait mosaic.
  - Only these versions: amateur ones circulate with their own licences.
- **Audio:** Carl Sagan explaining the picture, Library of Congress `cosmos000110` (the user's decision,
  2026-10-13, `design.md` §1.1: credited as © Druyan-Sagan Associates, claiming no licence; taken out at
  once if the estate objects).

## Later candidates
- **Luna 9** (February 1966), the first picture from the Moon's surface, for `lunar-surface-photos`.
  Commons `File:First_Photo_from_the_Surface_of_the_Moon.jpg` (its NASA source link has gone; low to medium
  risk).
- **Surveyor 1, Mariner 10** (Mercury), **Pioneer 11** (Saturn), **Venera 15/16** radar maps, **Apollo 15 to
  17** with their rovers.
