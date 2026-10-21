# The archive's first pages: images, audio and rights

The candidates for the archive (`design.md` §2.12, `ui-spec.md` §4.19), researched 2026-10-15. Each page
is a real mission's story and what it sent back, unlocked in a game by the goal it names. Downloaded and
built on 2026-10-15 (`make_archive_assets.py`; the originals stay local in `originals/`), credited in
`assets/CREDITS.md`. Left out: Venera 9's pictures (Mitchell's), the Leonov stamp, the Face on Mars. Lunar
Orbiter's two frames are Langley's scans of NSSDC's `lo1_h102_123` and `lo2_h162_123`.

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

## Added 2026-10-15: eight more pages

Their pictures, sources and rights are in `make_archive_assets.py` (`PICTURES`) and `assets/CREDITS.md`; the
figures' sources in `CHECKS.md`.
- **NASA:** Surveyor 1 (its footpad and mosaic are small pictures from a 2021 NASA article, with no catalog
  number; PIA02976, S66-32144), Apollo 15 (AS15-86-11603, AS15-85-11425, AS15-88-11866, S71-42955, S71-43788,
  and the hammer and feather cut from NSSDC's video), Mariner 10 (PIA03103, whose library caption is wrong;
  PIA23791, JPL's own 2020 reprocessing; PIA02961; PIA02962), Pioneer 11 (two NASA Science pictures with no
  catalog number, credited NASA/Ames; ARC-1979-AC79-9114-57, the mission control room).
- **Soviet:** Luna 9 (NSSDC's `luna9close_big`, 1966, out of German protection), Lunokhod 1 (LRO frame
  M175502049RC from NASA's PDS archive, public domain under LROC's terms, cropped and mirrored; NSSDC's
  `lunokhod` photo, publication date unverified), Luna 16 (PIA12984, an LROC product NASA republishes: LROC's
  terms reserve such products, a low to medium risk; NSSDC's small `luna16`), Venera 15 and 16 (the PDS
  Geosciences Node's `venera-maxwell` and PIA00465: 1983 Soviet radar data, a medium risk).
- **LROC's terms** (https://www.lroc.asu.edu/about/terms): what its PDS archive delivers is public domain; its
  website's featured images and products are ASU's, free for education with the credit
  "NASA/GSFC/Arizona State University".

## The findings' pages (2026-10-21, design.md 2.13)

### Explorer 1: the Van Allen belts (NASA, January 1958)
- **Unlocked by:** `magnetic-field-study` (Space environment measured), a finding: the page opens for the agencies that know it.
- **Images:**
  - `5663627`, Pickering, Van Allen and von Braun holding up the Explorer 1 model at the National Academy of
    Sciences, 1 February 1958 (NASA library, https://images.nasa.gov/details/5663627, original
    `5663627~orig.jpg`). The library lists it under Marshall (MSFC); NASA Science credits the same photo
    "NASA/JPL", as used here. Public domain (NASA). Shows real people: never use it in promotion.
  - `6414024`, Explorer 1 being fitted to its Jupiter-C, January 1958 (NASA library,
    https://images.nasa.gov/details/6414024; credit NASA/MSFC). Public domain.
  - `S61-479`, an early NASA drawing of the inner and outer belts. Not in the NASA library; the original is the
    file NASA published with its 2018 Goddard article,
    https://www.nasa.gov/wp-content/uploads/2018/01/historic_radiation_belts_orig.jpg (1280 x 720, credited there
    "NASA’s Goddard Space Flight Center/Historic image of Van Allen Belts courtesy of NASA’s Langley Research
    Center"). The id is the plate number printed on it. Public domain (NASA); low risk. Its exact date is
    unknown, so the caption gives none.
  - Not used: PIA04601, a JPL poster carrying the JPL logotype.

### Korabl-Sputnik 2: Belka and Strelka come home (USSR, August 1960)
- **Unlocked by:** `organisms-in-microgravity` (a finding: makes the first crewed orbit and reentry safer).
- **Images:**
  - `strelka`: a television frame of a dog in orbit, as NSSDC shows it on its Sputnik 5 page
    (https://nssdc.gsfc.nasa.gov/image/spacecraft/strelka.jpg, live; only 228 × 310 px, the only size NSSDC
    has). Credit: "Korabl-Sputnik 2 (USSR, 1960). Image via NASA/NSSDC". A Soviet image of 1960, so out of
    German protection under the §72(3) UrhG rule above; Russia's term runs longer (recheck before the repo goes
    public). Risk: low. Note: NSSDC published the very same file in 2002 as `laika.jpg`, so which dog it shows
    is not certain; the caption only says NSSDC names her Strelka. The rights conclusion holds either way: if
    the frame is Laika's (1957), its 50-year term has also run, though 1957 is just outside the 1959 to 1975
    window named above.
  - Optional, not on the page (the user's call): `9248173`, Yuri Gagarin's portrait, cropped from the Huntsville Times front page of 12 April 1961 in NASA's
    library (MSFC, https://images.nasa.gov/details/9248173; crop box 520,621 to 1216,1566 px of the
    1984 × 3000 original, the photo only, no newspaper text or masthead). The photo is captioned "AP Wirephoto
    via radio from Moscow": a Soviet photo of 1961 (out of German protection under the same rule), relayed
    by AP. Risk: medium. It is not NASA's work: AP could claim the wirephoto, and NASA's library hosts the page
    as part of MSFC's history, not as a NASA image. Gagarin is a person in a photo, so never use it in
    promotion. Left off the page because it is a newspaper reproduction, not an original, and shows neither the dogs nor
    the craft; the page has one image, like `luna-3` and `luna-9`. To add it: file `archive/9248173.jpg`,
    caption "Yuri Gagarin on the front page of the Huntsville Times, 12 April 1961, the day of his one-orbit
    flight.", credit "Soviet photo via AP, in NASA/MSFC’s collection · 9248173", and its CREDITS line below:
    `ui/archive/9248173.jpg`: Korabl-Sputnik 2: Belka and Strelka come home. Yuri Gagarin on the front page of
    the Huntsville Times, 12 April 1961, the day of his one-orbit flight. Soviet photo via AP, in NASA/MSFC’s
    collection · 9248173, https://images.nasa.gov/details/9248173. Cropped to the photograph. A Soviet photo
    (1961) relayed by AP, from a newspaper page in NASA's library; not NASA's work. Used as a photograph
    without creative input whose 50-year term under German law (§ 72(3) UrhG) has run out; AP's claim
    unchecked; recheck before the repository goes public.
  - Not used: NSSDC's `sputnik2_vsm` ("courtesy of Alexander Chernov - all rights reserved"); Wikimedia
    Commons' dog and Vostok-model photos (users' CC BY-SA work); the StarChild newspaper collage
    (a Western newspaper page). No NASA photo of a Vostok capsule or of Belka and Strelka exists in NASA's
    library (searched "Vostok", "Gagarin", "Belka", "Strelka", "Soviet dog", "Sputnik").

### Pegasus: wings to catch meteoroids (NASA, February to July 1965)
- **Unlocked by:** `micrometeoroid-study` (Micrometeoroid risk measured), a finding: the page opens for the
  agencies that know it.
- **Images (NASA library, public domain):**
  - `6519204`, Fairchild technicians checking an extended Pegasus wing (https://images.nasa.gov/details/6519204;
    credit NASA/MSFC). The library's date (1 January 1965) is a placeholder; Stages to Saturn dates the photo
    March 1964 (not certainly this frame), so the caption says only "before the first launch in 1965".
  - `6518485`, Pegasus 1 inside its shroud on the SA-9 S-IV stage, 13 January 1965
    (https://images.nasa.gov/details/6518485; credit NASA/MSFC). The satellite itself is hidden in the white
    shroud.
  - `S65-42437`, the night launch of SA-8 with Pegasus 2, 25 May 1965 (https://images.nasa.gov/details/S65-42437;
    credit NASA).
  - Also available: `6519630` (SA-9 launch, 16 February 1965) and `9801797` (an MSFC artist's concept of
    Pegasus in orbit). No NASA image of the wings deployed in orbit was found.

### Pioneers 6 to 9: weather reports from the Sun (NASA Ames, 1965 to 1968)
- **Unlocked by:** `solar-wind-measurements` (Solar wind measured), a finding: the page opens for the agencies
  that know it.
- **Images (public domain, NASA):**
  - `pioneer6-9`, NSSDC's painting of the Pioneer 6 to 9 spacecraft,
    https://nssdc.gsfc.nasa.gov/image/spacecraft/pioneer6-9.jpg (761 x 750). Credit NASA (via NSSDC); the
    artist is not named. NASA History's 2017 article uses a mirrored copy of the same painting. NSSDC also has a
    black-and-white view, https://nssdc.gsfc.nasa.gov/planetary/image/pioneer_6.jpg (2089 x 1892).
  - `pioneer_solar_weather_console`, the Pioneer space weather console at Ames during Apollo 10, May 1969. Not
    found in the NASA library; the original is the file in NASA History's Pioneer 8 article,
    https://www.nasa.gov/wp-content/uploads/2017/12/pioneer_solar_weather_console.png (948 x 739, PNG: convert
    to JPG). Credit NASA Ames Research Center (per the article's caption). Low risk.
  - `ARC-1966-A-37949-1`, Ames staff in the Pioneer tape processing station's data room, 29 November 1966
    (https://images.nasa.gov/details/ARC-1966-A-37949-1; credit NASA Ames Research Center). The library
    caption names the people (Fimmel, Harvey, Vinyard, Wirth, Klauk): never use it in promotion.
  - Not used: the orbit diagram of the August 1972 flare (337 px wide, too small), from the same article.

### Lunar Orbiter: counting meteoroid hits (NASA, 1966 to 1967)
- **Unlocked by:** `lunar-micrometeoroid-study` (a finding: makes crewed lunar orbit, surface activities and spacewalks at the Moon safer).
- **Images:** NASA's library has no photo of the whole Lunar Orbiter spacecraft (searched "Lunar Orbiter" across
  all centers and Langley's LRC series; it holds the camera payload, LRC-1967-B701_P-05655, which belongs to the
  `lunar-orbiter-1` page's story, and launch photos).
  - `lunar_orbiter_render`: NSSDC's rendering of the spacecraft,
    https://nssdc.gsfc.nasa.gov/planetary/image/lunar_orbiter_render.jpg (2000 × 1814 px), shown on
    https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1966-073A with the note "Upper rendered image
    credit NASA/NSSDCA (public domain)". Credit: "NASA/NSSDCA". Risk: low. It is a modern rendering, not a 1960s
    photo; the caption says so.
  - `LRC-1967-B701_P-01853`: Lunar Orbiter 3's Atlas-Agena D lifting off, 5 February 1967 (NASA library, Langley;
    original TIFF https://images-assets.nasa.gov/image/LRC-1967-B701_P-01853/LRC-1967-B701_P-01853~orig.tif).
    Credit: "NASA/Langley". Public domain.
  - Not used: NSSDC's `lunar_orbiter.jpg` and `lunar_orbiter_nasm.jpg` (a spacecraft photo with no stated source,
    and apparently the National Air and Space Museum's exhibit), and `lunar_orbiter_moon_jf.jpg` (a 2007 Photoshop
    composite of the craft over the Moon by an unnamed maker). The puncture-rate plot in Byers' TM X-3487 (p. 143)
    is a NASA figure but a poor scan.

### Venera 4 and Mariner 5: into the air of Venus (USSR and NASA, October 1967)
- **Unlocked by:** `venus-structure-study` (a finding: opens only for the agencies that know it, `design.md` §2.13).
- **Images:**
  - `mariner-5-spacecraft`, the Mariner 5 spacecraft, from NASA History's "55 Years Ago, Mariner 5 Explores
    Venus" (https://www.nasa.gov/wp-content/uploads/2022/10/mariner_5_flyby_2_spacecraft.jpg, 985 × 799; its
    metadata names a 3777 × 3065 original, not found online). No catalog number and no credit on the page;
    credited "NASA/JPL" because JPL built and flew Mariner 5. A NASA photo: public domain, low risk (as
    Surveyor 1's uncatalogued pictures).
  - `6757466`, the Atlas-Agena with Mariner V on the pad, 14 June 1967 (NASA library, MSFC; original
    https://images-assets.nasa.gov/image/6757466/6757466~orig.jpg). Credit NASA. Public domain.
  - `venera_4_capsule`, the Venera 4 descent capsule (NSSDC, https://nssdc.gsfc.nasa.gov/planetary/image/venera_4_capsule.jpg,
    320 × 302, black and white; linked from NSSDC's Venera 4 page as "Venera 4 descent capsule"). Credit
    "Venera 4 (USSR, 1967). Image via NASA/NSSDC". A Soviet photo, which NSSDC marks "not necessarily in the
    public domain". Its publication date is unverified; a period photo of a 1967 craft, most likely published
    in 1967 or soon after, so out of German protection (§ 72(3) UrhG, 50 years) under PAGES.md's 1959 to 1975
    rule: low to medium risk until a publication date is found. Small: show it small.
  - Left out: NSSDC's `venera_4_desc_module_vsm` (a modern colour museum photo, outside the 1959 to 1975
    window), NASA History's Venera 4 model photos ("Courtesy Memorial Museum of Cosmonautics, Moscow", not
    NASA's), NASA History's encounter diagram (its "closest approach about 2,000 mi" contradicts the 4,094 km
    flown), and science.nasa.gov's `mariner5.jpg` (captioned as a Mariner Mars diagram).

### Mascons: the Moon’s uneven pull (NASA, 1966 to 1969)
- **Unlocked by:** `lunar-structure-survey` (a finding: makes lunar orbit insertion and lunar orbit safer).
- **Images:**
  - `PIA16587`: GRAIL's map of the Moon's gravity field (Mercator, far side in the center), 5 December 2012.
    NASA library https://images.nasa.gov/details/PIA16587 (original
    https://images-assets.nasa.gov/image/PIA16587/PIA16587~orig.jpg); Photojournal
    https://science.nasa.gov/photojournal/grails-gravity-field-of-the-moon/. Credit as Photojournal gives it:
    "NASA/ARC/MIT". Risk: low to medium. It is a NASA mission product published by JPL, but co-credited to MIT (the
    principal investigator's institution), like the LROC co-credits noted above. Every GRAIL map checked carries a
    co-credit: PIA16623 is NASA/JPL-Caltech/MIT/GSFC, and PIA16578 is NASA/JPL-Caltech/CSM (Colorado
    School of Mines). PIA16587 shows the near-side mascons best.
  - `AS12-51-7507`: Intrepid in lunar orbit before its descent, 19 November 1969, photographed from Yankee Clipper
    (NASA library, https://images-assets.nasa.gov/image/as12-51-7507/as12-51-7507~orig.jpg). Credit: "NASA".
    Public domain.
  - Not used: S69-57076, a Lunar Orbiter 3 frame of an Apollo 12 site. Its library caption describes the prime
    site near Surveyor 3, but the frame is labelled "Landing Site 7", which does not match.

### Zond 5: tortoises around the Moon (USSR, September 1968)
- **Unlocked by:** `lunar-bioscience` (a finding: makes crewed circumlunar flight safer).
- **Images:** NSSDC has no Zond 5 picture (no spacecraft image, and its Soviet lunar data list holds Zond 3,
  6, 7 and 8 only). Both pictures come from NASA History's article "50 Years Ago: On the Way to the Moon"
  (https://www.nasa.gov/history/50-years-ago-on-the-way-to-the-moon-0/), where each sits in a two-photo
  composite credited "RKK Energiya". Cut each out of its composite; both are small.
  - `zond5_earth`: the whole Earth from about 90,000 km, September 1968. Right half of
    https://www.nasa.gov/wp-content/uploads/2018/09/zond-5.jpg (919 × 424 px; crop x 609 to 917, full height,
    about 308 × 424 px). Credit: "Zond 5 (USSR, 1968). Image via NASA, credited RKK Energiya".
  - `zond5_splashdown`: the capsule afloat in the Indian Ocean. Left half of
    https://www.nasa.gov/wp-content/uploads/2018/09/zond-6.jpg (913 × 337 px; crop x 0 to 456, full height).
    Same credit.
  - Rights: Soviet photos of 1968, so out of German protection under the §72(3) UrhG rule above (50 years from
    publication) if they were published by 1975. First publication is unverified for both (an auction listing
    of a UPI print suggests the Earth photo circulated in the West soon after the flight; not checked), which
    is the main risk. RKK Energiya, the credited
    holder, is a legal entity, so Russia's term would be 70 years from publication (to about 2039): recheck
    before the repo goes public. Risk: medium.
  - Not used: the tortoises being examined (right half of `zond-6.jpg`): shows identifiable people and its
    publication date is unknown; the Planetary Society's Earth image (processed and captioned by others);
    the 1969 Soviet stamp and Commons' drawings (users' work).

### Mariners 6 and 7: the thin air of Mars (NASA/JPL, July to August 1969)
- **Unlocked by:** `mars-surface-and-atmosphere-study` (Mars studied), a finding: opens for the agencies that know it.
- **Images:**
  - PIA02981, two Mariner 7 approach views of Mars with the bright south polar cap (NASA Image Library
    https://images.nasa.gov/details/PIA02981, original `https://images-assets.nasa.gov/image/PIA02981/PIA02981~orig.jpg`;
    Photojournal https://science.nasa.gov/photojournal/mars-full-disk-approach-view-from-mariner-7/). Credit NASA/JPL.
    Public domain, low risk. Only 640 x 360 px. Its caption's "arrived on August 4" is the US date (flyby 5 August UT).
  - PIA02989, the south polar region from Mariner 9 (top, 1971, through the dust storm) and Mariner 7 (bottom, August
    1969) (library https://images.nasa.gov/details/PIA02989, 4288 x 6265 px original). Credit NASA/JPL. Public domain,
    low risk. The caption must say which half is which.
  - 69PC-23, Mariner 6's Atlas-Centaur lifting off from Launch Complex 36B, 8:29 p.m. EST 24 February 1969 (library
    https://images.nasa.gov/details/69PC-23, KSC, 2400 x 3000 px). Credit NASA. Public domain, low risk; no people
    identifiable.
  - Not used: PIA02365 ("Giant's Footprint"), credited NASA/JPL/MSSS (Malin Space Science Systems, a company) and its
    caption gives Mariner 7's closest approach as about 4,200 km, against NSSDC's and NASA Science's 3,430 km. No
    1969 near-encounter close-up with a NASA caption was found in the library or Photojournal; PDS's raw Mariner 69
    frames (https://pds-imaging.jpl.nasa.gov/volumes/mariner69.html) would need processing.

### Orbiting Frog Otolith: the inner ear in orbit (NASA, November 1970)
- **Unlocked by:** `amphibians-in-microgravity` (a finding).
- **Images:** NASA's image library has nothing on OFO (searched "Orbiting Frog Otolith", "otolith", "OFO",
  "frog", "bullfrog", "Scout" 1970, "Wallops" 1970; its frog pictures are the 1992 Spacelab-J embryology
  experiment). NSSDC has no OFO picture either. Both images are drawings from NASA's own press kit, News
  Release 70-132 of 12 August 1970, scanned in NTRS (https://ntrs.nasa.gov/citations/19700024873, PDF
  https://ntrs.nasa.gov/api/citations/19700024873/downloads/19700024873.pdf). Each page is a 2552 × 3304 px
  grayscale scan, printed sideways: extract the page image, rotate it to landscape and crop to the drawing,
  leaving out the page number, "-more-" and the figure number.
  - `ntrs-19700024873-fig4`: figure 4, "Orbiting Frog Otolith Spacecraft", a labeled cutaway (release
    page 9a, PDF page 14).
  - `ntrs-19700024873-fig5`: figure 5, "FOEP Cutaway and Interior Views", the frogs' water-filled centrifuge
    and life support (release page 10a, PDF page 16).
  - Credit: NASA. Rights: a NASA press release, public domain in the US. The spacecraft was built by Aerojet's
    Space General unit (press kit p. 9) and the drawings may be the contractor's, but NASA issued them in its
    release with no copyright mark; a small residual risk, like the Pioneer plaque's.
  - Not used: the press kit's figure 2 (the macula), a textbook-style drawing that may be copied from a
    published source; figures in the 1972 University of Milan contractor report (CR-62084), whose
    contractor-made figures are not clearly NASA's; Wikimedia Commons' OFO files (no source given).

### Surveyor 3: digging into the Moon (NASA, 1967 to 1969)
- **Unlocked by:** `lunar-soil-study` (a finding: makes powered descent and landing on the Moon safer).
- **Images:**
  - `AS12-48-7133`: Conrad at Surveyor 3 with Intrepid behind, 20 November 1969, taken by Bean (NASA library,
    https://images-assets.nasa.gov/image/AS12-48-7133/AS12-48-7133~orig.jpg). Credit: "NASA". Public domain;
    it shows an astronaut, so it must not be used in promotion.
  - `surveyor3_sp146_fig4-10`: Surveyor 3's TV view of trench 3, widened, with the scoop ready for bearing test 4,
    28 April 1967. Cut from NASA SP-146, *Surveyor III: A Preliminary Report* (1967), p. 74 (PDF page 81),
    https://ntrs.nasa.gov/api/citations/19670023253/downloads/19670023253.pdf: the page is a 2362 × 3163 px
    embedded scan, cropped to x 346 to 2192, y 307 to 2267. A cut is ready at
    `C:\Users\mario\AppData\Local\Temp\claude\C--Temp-LW\3cc51302-abb6-441d-b717-4fbfaebd2ce5\scratchpad\img_s3\surveyor3_sp146_fig4-10.jpg`.
    Credit: "NASA/JPL". Public domain (a NASA publication; NTRS marks it public use permitted). Quality: a
    halftone scan of a 1967 TV picture with reseau dots; fine at archive size. NASA's library has no Surveyor 3 TV
    frame.
  - `as12-48-7110`: a Surveyor 3 footpad and its scoop, with the imprint from the bounce, photographed by Apollo 12
    on 20 November 1969 (NASA library, https://images-assets.nasa.gov/image/as12-48-7110/as12-48-7110~orig.jpg).
    Credit: "NASA". Public domain.
  - Not used: Apollo 12's own photos of the trenches, AS12-48-7106 to 7109 and 7126 to 7129. They are not in NASA's
    library, and the Apollo Lunar Surface Journal's copies are JSC scans post-processed by Kipp Teague, so they
    fall under the rule against reprocessing by people outside NASA. The National Gallery of Art's print of
    NASA's 67-H-481 trench mosaic is another institution's scan.

### Vikings 1 and 2: the soil of Mars (NASA/JPL, July to September 1976)
- **Unlocked by:** `mars-soil-study` (Martian soil studied), a finding: opens for the agencies that know it.
- **Images (credit NASA/JPL, public domain, low risk):**
  - PIA00389, Viking 1's first trench, dug on sol 8 (28 July 1976) (library https://images.nasa.gov/details/PIA00389,
    Photojournal https://science.nasa.gov/photojournal/trench-excavated-by-viking-1-surface-sampler/). Small: 251 x 512 px.
  - PIA00390, Viking 1's buried footpad 3, 1 August 1976 (library https://images.nasa.gov/details/PIA00390,
    Photojournal https://science.nasa.gov/photojournal/viking-landers-buried-footpad-3/). 520 x 512 px.
  - PIA00145, Viking 2's arm trenching the "Bonneville Salt Flats" crust, 25 September 1976, four frames (library
    https://images.nasa.gov/details/PIA00145; full caption on the PDS Rings mirror
    https://pds-rings.seti.org/press_releases/pages/PIA00xxx/PIA00145.html). 1089 x 512 px.
  - Optional: PIA00395, Viking 1's collector head full of soil for the GCMS, 9 August 1976, out of focus
    (library https://images.nasa.gov/details/PIA00395). Small: 259 x 402 px.

### Pioneer Venus 2: four probes at once (NASA, December 1978)
- **Unlocked by:** `venus-atmosphere-study` (a finding: opens only for the agencies that know it, `design.md` §2.13).
- **Images (NASA library, Ames; originals at `https://images-assets.nasa.gov/image/<id>/<id>~orig.jpg`):**
  - ARC-1977-AC77-0376-8, project manager Charlie Hall with the Multiprobe at Hughes Aircraft, December 1976
    (stamped "NASA 77-HC-95"). The library's photographer field says "TRW" and Siddiqi credits it "NASA/TRW":
    a contractor photo NASA released, public domain as NASA distributes it, low risk. Hall is a real person:
    never use it in promotion. Crop the scanned print's border and handwritten number.
  - ARC-1978-AC78-9245, Paul Hudson's painting of the Multiprobe after releasing its four probes (credited
    "NASA/ Paul Hudson" on NASA Science's Pioneer Venus 2 page, "Illustration by Paul Hudson" by NASA History).
    A named artist's work that NASA distributes as its own with no copyright mark: a small residual risk, as
    the Pioneer 10 plaque. Credit "NASA Ames Research Center/Paul Hudson". Crop the scan's grey border.
  - Left out: ARC-1977-AC77-0475-9 and -10 (Don Davis's art, an independent artist), ARC-1978-AC78-0238
    (Rick Guidice's painting, which the library notes is upside down), ARC-1978-AC78-9037 (the Orbiter's
    crescent Venus, not the probes).

## Later candidates
- **Venera 9's first picture,** with Don P. Mitchell's permission (the user's call; left out for now).
- **Apollo 16 and 17,** Luna 17's sister rover Lunokhod 2, Mars 3's one picture.
