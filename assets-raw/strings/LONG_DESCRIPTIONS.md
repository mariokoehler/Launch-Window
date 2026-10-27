# Long descriptions: sources

Sources for every figure in the programs' and components' `longDescription` (`assets/content/*/strings.json`), written 2026-10-27 in four batches. Fixes made at review are noted at the end.

---

# Batch 1 checks

NSSDCA's catalog (nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action) returned error pages for most ids during this
session (it worked only for 1965-009A and 1970-094A). NASA Science mission pages (science.nasa.gov, which cite NSSDCA
and Siddiqi's *Beyond Earth*) were used where they exist; Wikipedia otherwise, as a pointer to its sources.

## Programs

### sputnik-1
- Launched 4 October 1957 on an R-7; 83.6 kg; 20.005 / 40.002 MHz: https://www.nasa.gov/history/sputnik/14.html, https://en.wikipedia.org/wiki/Sputnik_1
- Orbit about 215 x 939 km, 96.2 min; reentry 4 January 1958 after 1,440 orbits: https://en.wikipedia.org/wiki/Sputnik_1 (infobox)
- "Nearly 8 km/s" is orbital speed in low orbit (physics, about 7.8 km/s at 200 km); not a sourced Sputnik figure.
- "No science package, only a radio and sensors for its own temperature and pressure": Wikipedia (temperature/pressure encoded in beeps, barometric switch).

### explorer-1
- Juno I, a modified Jupiter-C; launch 1 February 1958 03:48 GMT = night of 31 January local: https://en.wikipedia.org/wiki/Explorer_1
- Upper stages of clustered small solid motors spun in a tub (well documented, Wikipedia Juno I / Explorer 1). Text says the guided first stage aimed the stack and spin steadied the unguided upper stages (Wikipedia Juno I / Explorer 1).
- Orbit 358 x 2,550 km; counter saturated, Explorer 3 (tape recorder) confirmed: https://en.wikipedia.org/wiki/Explorer_1

### pegasus
- Three, Saturn I, 1965; retired 29 August 1968; Pegasus 1 decayed 17 September 1978, Pegasus 2 3 November 1979: https://en.wikipedia.org/wiki/Pegasus_(satellite)
- Particle mass range 1e-7 to 1e-4 g: NSSDCA 1965-009A https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1965-009A
- About 200 square meters, thicknesses 0.04 / 0.2 / 0.4 mm aluminum: DTIC paper (194.5 m2, 38/203/406 micrometers) https://apps.dtic.mil/sti/pdfs/ADA455598.pdf ; NASA 2017 slides (about 200 m2) https://ntrs.nasa.gov/api/citations/20170008156/downloads/20170008156.pdf ; Wikipedia gives "more than 210 m2". Rounded to "about 200".

### biosatellite
- Ames Research Center, General Electric, Delta rockets, three flights 1966-1969, C/E/F cancelled (so six planned): https://space.skyrocket.de/doc_sdat/biosat.htm , https://en.wikipedia.org/wiki/Biosatellite_program
- Aim: effects of weightlessness and radiation; ground controls: https://en.wikipedia.org/wiki/Biosatellite_program ; NTRS search summaries.
- Sr-85 source on Biosatellite II, spring drive, shielded before reentry: NTRS (search result https://ntrs.nasa.gov/api/citations/19680004637/downloads/19680004637.pdf area; summary only, the PDF text was not readable here).

### orbiting-frog-otolith
- Experiment by Torquato Gualtierotti (University of Milan, at Ames); first planned for an Apollo mission (1966); responses changed early and returned toward normal in the last 10-20 hours: https://en.wikipedia.org/wiki/Orbiting_Frog_Otolith
- Otolith physiology (crystals on hair cells, sense gravity/linear acceleration) and the link to space motion sickness are textbook; no single page cited. "Suspected cause" kept deliberately loose.

### korabl-sputnik-2
- 19 August 1960, Belka, Strelka, 40 mice, 2 rats, plants, TV camera, about 25 hours, landed 20 August; 28 July 1960 failure killing Bars and Lisichka: https://en.wikipedia.org/wiki/Korabl-Sputnik_2
- Dogs returned in an ejected pressurized animal cabin on the ejection seat: https://mail.russianspaceweb.com/vostok-1k-2.html (search summary)
- Gagarin, Vostok 1, 12 April 1961: https://en.wikipedia.org/wiki/Vostok_(spacecraft)

### sputnik-2
- Launch 3 November 1957; telemetry of respiration, pulse, blood pressure; two spectrophotometers (UV, X-ray); cosmic-ray counter; apogee about 1,660 km; 162 days, reentry 14 April 1958: https://en.wikipedia.org/wiki/Sputnik_2

### pioneer-4
- Pioneer 3: 6 December 1958, max about 102,322 km, booster shut down about 4 s early, about 1,030 km/h short of escape: https://science.nasa.gov/mission/pioneer-3/
- Pioneer 4: Juno II, outer belt intensity changed (probably a solar flare), contact lost at about 655,000 km, record: https://science.nasa.gov/mission/pioneer-4
- "More than 11 km/s": Earth escape speed, physics. Juno II = Jupiter first stage + clustered small solid motors (Wikipedia Juno II; the NASA page names the Sergeant motors).

### zond-5
- 7K-L1 for two cosmonauts, circumlunar; Proton with Blok D; crewed flight cancelled after Apollo 8: https://en.wikipedia.org/wiki/Soyuz_7K-L1
- Closest approach about 1,950 km over the far side: https://www.nasa.gov/feature/50-years-ago-on-the-way-to-the-moon-0 (and Wikipedia Zond 5)
- Lunar return speed about 11 km/s: Luna 16 capsule reentered at about 11 km/s (https://en.wikipedia.org/wiki/Luna_16); entry-angle corridor is general reentry physics.
- "Earlier attempts had failed": Wikipedia 7K-L1 (12 launched, several lost before Zond 5).

### ranger-3
- 330 kg, Atlas-Agena B, 26 January 1962, TV camera, capsule with seismometer, gamma-ray spectrometer with first interplanetary gamma-ray flux measurements: https://science.nasa.gov/mission/ranger-3/
- Rangers 3, 4, 5 failed; later (Block III) Rangers cameras only: https://en.wikipedia.org/wiki/Ranger_4 , https://en.wikipedia.org/wiki/Ranger_5 , https://space.skyrocket.de/doc_sdat/ranger_bl2.htm

### luna-2
- Luna 1 missed by about 5,995 km in January 1959: https://en.wikipedia.org/wiki/Luna_1
- Launch 12 September 1959, impact 13 September 21:02 UT (about 38 hours), about 3.3 km/s, sodium cloud, no magnetic field, ion traps / solar wind, Jodrell Bank tracking: https://en.wikipedia.org/wiki/Luna_2
- Doubt: the sodium cloud's size differs between sources (400 km vs 650 km), so it is left out.

### pioneer-2
- Able 3, 8 November 1958, Thor-Able, instruments (ion chamber, magnetometer, temperature sensors, micrometeoroid sensor, proportional counter telescope, TV): https://science.nasa.gov/mission/pioneer-2
- Able probes built by Space Technology Laboratories; Pioneer 1 = Able 2, first NASA launch: https://science.nasa.gov/mission/pioneer-1-able-2
- 2.6-day trip then TX-8-6 solid motor for lunar orbit: Wikipedia Pioneer 0 (Able 1), same design family https://en.wikipedia.org/wiki/Pioneer_0
- No rocket had reached escape speed before Luna 1 (2 January 1959): Wikipedia Luna 1.

### lunar-orbiter
- Five, 1966-1967, all successful; first three low-inclination orbits, 20 candidate sites; last two high polar orbits, about 99 % coverage: https://en.wikipedia.org/wiki/Lunar_Orbiter_program
- Mascons discovered 1968 by Muller and Sjogren from Lunar Orbiter tracking: https://en.wikipedia.org/wiki/Mass_concentration_(astronomy)

### surveyor
- Seven launched, five landed, 1966-1968, Atlas-Centaur; Surveyor 1 on 2 June 1966 first US soft landing; Luna 9 about four months earlier; scoops and alpha-scattering instruments; landing sites suitable for Apollo: https://en.wikipedia.org/wiki/Surveyor_program , https://science.nasa.gov/mission/surveyor-1/

### luna-15
- Launched 13 July 1969 (three days before Apollo 11), lunar orbit 17 July, two days of checks and maneuvers, 52 orbits: https://en.wikipedia.org/wiki/Luna_15
- Luna 16 (1970), 20 (1972), 24 (1976) returned samples: Wikipedia Luna 16 / 20 / 24.

## Components

### sputnik-1-satellite
- 58 cm (58.5), 83.6 kg, two 2 mm hemispheres with seal and 36 bolts, 1 mm polished outer layer, nitrogen, 51 kg silver-zinc batteries, 1 W transmitter, fan above 36 C, beep length encodes temperature/pressure, two pairs of antennas 2.4 and 2.9 m: https://en.wikipedia.org/wiki/Sputnik_1

### explorer-1-satellite
- 203 cm x 15.2 cm, 13.97 kg, 8.3 kg instruments, Geiger tube, five temperature sensors, acoustic and wire-grid micrometeorite detectors, mercury batteries about 40 % of payload, 60 mW / 31 days, 10 mW / 105 days: https://en.wikipedia.org/wiki/Explorer_1 (its infobox says "60 W", an error; the text says 60 mW).

### pegasus-satellite
- About 1,450 kg, wings 4.3 m wide (the 29 m span is left to the program's history note), solar cells, TV camera recorded deployment, stayed on upper stage: https://en.wikipedia.org/wiki/Pegasus_(satellite) ; NSSDCA gives 29.3 x 4.3 m.
- Capacitor panels: aluminum target sheet, 12-micrometer Mylar dielectric, vapor-deposited copper: DTIC https://apps.dtic.mil/sti/pdfs/ADA455598.pdf . Discharge-and-recharge counting is how such capacitor detectors work (same source family).

### biosatellite-satellite
- Adapter stays in orbit, reentry vehicle with capsule, retrorocket, parachute, radioed position over the Pacific, aerial recovery by the US Air Force: 1967 NASA pre-launch release (via NTRS search https://ntrs.nasa.gov/api/citations/19670025294/downloads/19670025294.pdf ) and NASM record https://www.airandspace.si.edu/es/collection-objects/satellite-biosatellite-2/nasm_A19731629000
- Bowl-shaped fiberglass heat shield: government document on a Biosatellite-type reentry vehicle (search summary of https://corpora.tika.apache.org/base/docs/govdocs1/761/761795.pdf ; could not open it).
- Three sections (adapter, reentry vehicle, experiment capsule), as the release describes.

### ofo-satellite
- About 0.76 m x 1.2 m body, 132.9 kg, yo-yo despin, four booms raising moment of inertia, 41 kg package, water-filled centrifuge, 0.6 g for 14 s, artificial lung, oxygen bottle, Baralyme, water at about 16 C: https://en.wikipedia.org/wiki/Orbiting_Frog_Otolith ; mass also NSSDCA https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1970-094A

### vostok-capsule
- Sphere 2.3 m, 2,460 kg (LePage: about 2,400); equipment module 2.27 t; TDU-1 about 16 kN for 45 s; cosmonaut ejected at about 7 km and landed separately: https://en.wikipedia.org/wiki/Vostok_(spacecraft) , https://thespacereview.com/article/1821/1
- Heat protection on all sides: Wikipedia Vostok.

### sputnik-2-satellite
- Cone 4 m high, 2 m base, about 500 kg, not designed to separate from Blok A; spectrophotometers in the nose; padded cabin, harness, gelatin food, electrodes, TV 10 frames/s; air regeneration absorbing CO2 / producing oxygen: https://en.wikipedia.org/wiki/Sputnik_2

### pioneer-4-probe
- 51 cm tall, 23 cm base (Wikipedia Pioneer 4 / skyrocket); two Geiger-Mueller tubes, photoelectric sensor trigger; spin slowed from 480 to 11 rpm about 11 hours after launch: https://science.nasa.gov/mission/pioneer-4
- Avoided the history note's mass, gold wash, stripes and batteries.

### zond-5-probe
- 5,375 kg launch mass; no orbital module, support cone with high-gain dish, reserve parachute removed, gyro platform and star sensors added; skip reentry flown by three flights: https://en.wikipedia.org/wiki/Soyuz_7K-L1 , https://en.wikipedia.org/wiki/Zond_5

### ranger-3-probe
- Hexagonal base 1.5 m, dish antenna, solar panels 5.2 m span, balsa impact limiter, retrorocket, radar altimeter triggering separation, batteries for 30 days: https://en.wikipedia.org/wiki/Ranger_3 , https://en.wikipedia.org/wiki/Ranger_4
- Capsule 42.6 kg, separation at about 21.4 km, balsa casing, designed to bounce: https://science.nasa.gov/mission/ranger-3/

### luna-2-probe
- About 390 kg; magnetometers, Geiger and scintillation counters, ion traps; pennants: https://en.wikipedia.org/wiki/Luna_2 (pentagon and burst details left to the history notes)
- "No engine of its own": the Ye-1A had no propulsion (Wikipedia Luna 2 describes a direct path, no corrections). Moderate confidence.
- Doubt: Wikipedia says the pennants were "detonated before impact"; the history notes say they scattered on impact. The long text no longer touches this.

### pioneer-2-probe
- About 39 kg (NASA 39.6, Wikipedia 39.2), instruments: https://science.nasa.gov/mission/pioneer-2
- 74 cm x 76 cm and TX-8-6 motor are Able 1's figures (Pioneer 2 was "nearly identical"): https://en.wikipedia.org/wiki/Pioneer_0 , https://science.nasa.gov/mission/pioneer-1-able-2

### lunar-orbiter-probe
- 386.9 kg, three-axis stabilized, four solar panels, engine for lunar orbit insertion, 68 kg Kodak system adapted from the NRO's Samos E-1 camera, Canopus tracker: https://science.nasa.gov/mission/lunar-orbiter-1/
- 610 mm and 80 mm lenses on one roll of 70 mm film, Bimat processing, scanner: https://en.wikipedia.org/wiki/Lunar_Orbiter_program , https://www.nationalgeographic.com/science/2019/02/moon-photographs-from-1960s-were-developed-in-space-heres-how (film width 65 vs 70 mm differs between sources; left out).

### surveyor-lander
- Triangular aluminum frame, about 3 m tall, three legs with shock absorbers, 995.2 kg launch, 294.3 kg landed, solid retro over 60 % of the mass: https://science.nasa.gov/mission/surveyor-1/
- Retro fired at about 75 km, jettisoned at about 11 km, three verniers with Doppler radar, cutoff at about 3.4 m, about 3 m/s touchdown: https://en.wikipedia.org/wiki/Surveyor_program

### luna-15-probe
- Descent stage with four legs, tanks, landing radar, TV camera, drill arm; drill 35 cm; spherical capsule on the ascent stage: https://en.wikipedia.org/wiki/Luna_16 (the same Ye-8-5 design)
- Ascent stage about 520 kg with its own engine, reaching about 2,700 m/s (the no-guidance direct return is left to the history note): https://www.drewexmachina.com/2019/07/19/luna-15-the-soviet-unions-last-lunar-gamble/ (secondary source; not confirmed against Siddiqi).

## Problems found in the existing text

- **pioneer-2 history note:** "climbed to about 1,550 km". NASA Science gives 1,530 km (951 miles): https://science.nasa.gov/mission/pioneer-2 . Suggest "about 1,530 km" or "about 1,500 km".
- **korabl-sputnik-2 history note:** says Pushinka was given to Caroline Kennedy. Wikipedia's Korabl-Sputnik 2 article says Jacqueline Kennedy. Most accounts (JFK Library) describe the puppy as a gift for Caroline, sent after Jacqueline asked Khrushchev about it, so the note is defensible; flagging only.
- **luna-2 history note vs luna-2-probe history note:** the two notes are nearly identical (both: 13 September 1959, first object to reach another world, pentagon balls, copy to Eisenhower). Worth giving the component a different note.
- **explorer-1 history note:** 31 January 1958 is the US local date (1 February UTC); fine as written.
- **zond-5-probe history note:** "fell ballistically, at much higher g-forces than planned": consistent with Wikipedia's "steep descent at about 20 g".

---

# Batch 2 checks

Note: on 2026-10-27 the NSSDCA catalog pages for Luna 3 (1959-008A), Luna 17 (1970-095A), Venera 7
(1970-060A), Venera 9 (1975-050A) and Venera 15 (1983-053A) returned an error page, so those entries
rest on Wikipedia (as a pointer to its sources) plus the other sources listed. Only URLs that
returned data are cited.

## luna-3 (program)
- No course-correction engine; passed near the Moon's south pole at about 6,200 km on 6 October 1959;
  went behind the Moon south to north; gravity bent the orbit so it returned over the northern
  hemisphere, where Soviet stations were; apogee about 500,000 km: https://en.wikipedia.org/wiki/Luna_3
- "Hundreds of thousands of kilometers": apogee about 500,000 km; frames transmitted as it came back
  toward the Earth (same source).

## luna-3-probe
- Cylinder with hemispherical ends, 130 cm long; 278.5 kg; sealed, about 0.22 atm ("about a fifth");
  solar cells charging internal batteries; thermal shutters opened above 25 °C; dual-lens camera,
  35 mm film; developed, fixed and dried on board, flying-spot scanner, sent as analog video like a
  facsimile: https://en.wikipedia.org/wiki/Luna_3

## luna-17 (program)
- Launch 10 November 1970, landing in Mare Imbrium: https://en.wikipedia.org/wiki/Luna_17 ,
  https://en.wikipedia.org/wiki/Lunokhod_1
- Five-person team, a delay of about 5 seconds; two operators, one driving and one keeping the
  steerable antenna pointed at the Earth; drives planned from terrain images:
  https://en.wikipedia.org/wiki/Luna_17 , https://www.orbitalfocus.uk/Diaries/Luna/Luna17.php
- Polonium-210 heater through the lunar night: https://en.wikipedia.org/wiki/Lunokhod_1
- Soil tests (25 RIFMA analyses, penetrometer at 500 places): Lunokhod 1 article.

## luna-17-lander
- Descent stage like Luna 16's; extra orbit-insertion tanks jettisoned in lunar orbit before
  descent; about 3.3 m across including legs; 85 km circular orbit, then 19 x 85 km; engine fired
  03:41, landing 03:47 UTC (about six minutes); about 1,836 kg on the surface:
  https://www.orbitalfocus.uk/Diaries/Luna/Luna17.php
- Doubt: "1,800 kg on the surface, rover included" assumes the Orbital Focus figure includes the
  rover (it is given for Luna 17 as landed). Not confirmed elsewhere.
- **History note doubt:** "four legs": no source I reached gives a leg count (Wikipedia and Orbital
  Focus mention legs without a number). The note's "folding ramps on two opposite sides" agrees with
  Orbital Focus (two pairs of ramps on opposite sides); Wikipedia says only "two ramps". Not
  confirmed: which side Lunokhod used (east).

## lunokhod-1-rover
- Tub-like pressurized body, about 1.35 m high; eight wheels each with its own motor; convex lid with
  solar array; polonium-210 heater; four TV cameras; X-ray spectrometer, penetrometer:
  https://en.wikipedia.org/wiki/Lunokhod_1
- Lid closed at night: https://en.wikipedia.org/wiki/Lunokhod_programme ("During the lunar nights,
  the lid was closed...")
- Ninth wheel as odometer; one failed motor would not stop it:
  https://www.orbitalfocus.uk/Diaries/Luna/Luna17.php
- Left out: mass, because sources disagree (756 kg in the Luna 17 article, 840 kg in Lunokhod 1).

## pioneer-sun-orbiter (program)
- Measured solar wind, magnetic field, cosmic rays from widely separated points:
  https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1965-105A
- Orbit 0.814 to 0.985 AU, period 311.3 days; by end of 1970 it had passed the Sun's far side;
  21 to 24 November 1968 solar occultation, first time a spacecraft was tracked passing behind the
  Sun: https://science.nasa.gov/mission/pioneer-6/
- "Probing the Sun's outer atmosphere": the two-frequency beacon/radio propagation experiment and the
  occultation results (JPL and UCLA, 1969), same page. Mild inference: the page says the occultation
  results were reported; that the radio passed through the corona is the purpose of an occultation.

## pioneer-6-probe
- 0.94 m diameter, 0.81 m tall, 1.8 m magnetometer boom, 79 W from body-mounted cells:
  https://space.jpl.nasa.gov/msl/QuickLooks/pioneer6QL.html
- About 60 rpm, spin axis perpendicular to the ecliptic; high-gain directional antenna; bit rates
  up to 512 bps: https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1965-105A
- Left out: mass (NSSDCA 146 kg vs NASA science page 62.14 kg vs JPL 63 kg).
- **Short description doubt:** "spinning slowly": NSSDCA gives about 60 rpm, once a second, which
  is not slow for a spacecraft (the Pioneer Venus Orbiter spun at 5 rpm). Suggest "spinning to keep
  stable".
- History note: the four-probe ring and warnings to the FAA, airlines, power companies agree with
  https://science.nasa.gov/mission/pioneer-6/ ; the Apollo planners part is not on that page
  (unverified, not contradicted).

## mariner-10 (program)
- Gravity assist saved a larger, costlier rocket and fuel: https://www.nasa.gov/history/45-years-ago-mariner-10-off-to-venus-and-mercury/
- Atlas-Centaur, 3 November 1973: same page and
  https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1973-085A
- Colombo (University of Padua), 176 days, exactly twice Mercury's orbital period; same sunlit
  hemisphere each pass; about 40 to 45 % imaged; weak magnetic field, Moon-like:
  https://www.nasa.gov/history/45-years-ago-mariner-10-first-to-explore-mercury/ ,
  https://science.nasa.gov/mission/mariner-10/ ("nearly half")
- The opening physics (a probe falling sunward gains speed that must be shed) is general orbital
  mechanics, stated without a figure.

## mariner-10-probe
- Octagonal magnesium frame 1.39 m diagonal; two panels 2.69 x 0.97 m; sunshade; 1.37 m dish,
  117.6 kbit/s max; 5.8 m magnetometer boom: https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1973-085A
- "About half the Earth's distance": Mercury's distance is 0.31 to 0.47 AU and the probe met it at
  aphelion (NASA 45-years page: first encounter timed for Mercury farthest from the Sun).
- Left out: launch mass (502.9 vs 473.9 kg on the same NSSDCA page).

## mariner-5 (program)
- Launch 14 June 1967; occultation behind Venus, about 26 minutes out of contact:
  https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1967-060A
- No camera; hydrogen corona by UV photometer; no magnetic field; surface about 527 °C and 75 to
  100 atm: https://science.nasa.gov/mission/mariner-5/
- Mariner 2 flew past Venus in December 1962 ("almost five years"): general record
  (https://science.nasa.gov/mission/mariner-5/ mentions a closer flyby than Mariner 2's).
- Density and temperature profile from the refracted signal: NSSDCA says the instruments measured
  the radio refractivity of the atmosphere; deriving density/temperature from refractivity is the
  method's purpose.

## mariner-5-probe
- Octagonal magnesium frame 127 cm diagonal; 244.9 kg; four panels spanning 550 cm, 555 W at Venus;
  silver-zinc battery; high-gain dish on top; mast with low-gain antenna and magnetometer; UV
  photometer, trapped radiation detector, Faraday cup, magnetometer, S-band occultation:
  https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1967-060A
- **History note:** spare of Mariner 4, no camera: confirmed (science.nasa.gov/mission/mariner-5).
  The panels "turned to face the other way and made smaller" is not confirmed by what I reached
  (unverified, not contradicted).

## pioneer-venus-orbiter (program)
- Launch 20 May 1978; orbit insertion 4 December 1978 by an 18,000 N solid motor; period about a
  day; periapsis held near 142 to 253 km early on: https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1978-051A
- Smoother than Earth; little or no magnetic field; sulfuric acid clouds; nearly continuous lightning
  detected; Maxwell Montes about 10.8 km above the mean surface: https://science.nasa.gov/mission/pioneer-venus-1/
- **History note doubt:** the note says the radar band ran from "74 degrees north"; NASA's page says
  73°N to 63°S. Minor.

## pioneer-venus-orbiter-probe
- Drum 2.5 m by 1.2 m; about 5 rpm; 312 W at Venus; 1.09 m dish mechanically despun to point at the
  Earth; solid insertion motor: NSSDCA 1978-051A (above).
- Radar mapper, about 75 km resolution: https://science.nasa.gov/mission/pioneer-venus-1/
- "Looked down on the low part of each orbit": inference from the altimeter design (a radar
  altimeter works near periapsis); not stated outright in the sources I reached.

## venera-15 (program)
- 8 cm side-looking radar, look angle about 10°; altimeter: https://astro.if.ufrgs.br/solar/venera16.htm ,
  https://solarviews.com/eng/venera16.htm , https://en.wikipedia.org/wiki/Venera_15
- Orbit about 24 hours, periapsis about 1,000 km near 62°N: https://en.wikipedia.org/wiki/Venera_15
  (its infobox gives other values; the text values are used).
- "Timing and frequency of the echoes": general principle of synthetic-aperture radar; no figure.

## venera-15-probe
- Cylinder with the radar antenna at one end; 1 m altimeter dish aimed at the planet's center during
  imaging; onboard computers stored each image until complete;
  5,250 kg at launch, 4,000 kg dry: https://en.wikipedia.org/wiki/Venera_15
- 8 cm wavelength: sources above.
- **History note doubt:** the antenna's "about 6 meters" is contested: NASA-derived pages give
  6 m x 1.4 m, Wikipedia gives a 0.6 m by 1.4 m dish. I left the size out of the long text.
  The note's 1 to 2 km resolution over eight months agrees with the solarviews/NASA-derived pages.

## pioneer-venus-multiprobe (program)
- Launch 8 August 1978; large probe released 16 November, small probes 20 November; bus entry
  9 December 1978: https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1978-078A
- North, Day, Night probes and their sites (60°N; day side; night side); large probe used a
  parachute, small probes none; temperatures below 50 km very similar despite sites thousands of
  km apart: https://science.nasa.gov/mission/pioneer-venus-2/ ,
  https://en.wikipedia.org/wiki/Pioneer_Venus_Multiprobe
- **History note doubt:** "None was built to survive hitting the ground, but the Day probe did":
  NASA's page says two of the three small probes survived impact; the Day probe was the one that
  kept transmitting (67 min 37 s). The note's wording implies only the Day probe survived. Suggest
  "and the Day probe kept sending..." instead.

## pioneer-venus-multiprobe-craft
- Bus 2.5 m cylinder; large probe 315 kg (NASA: 316.5 kg), about 1.5 m across, pressure vessel inside
  a nose cone and aft cover, parachute; small probes about 0.8 m (NASA: 76 cm) and 90 kg, no
  parachutes, aeroshells kept; doors opened at about 70 km; bus had no heat shield and no parachute:
  https://en.wikipedia.org/wiki/Pioneer_Venus_Multiprobe , https://science.nasa.gov/mission/pioneer-venus-2/ ,
  NSSDCA 1978-078A
- Cone-shaped probes: science.nasa.gov/mission/pioneer-venus-2.
- **History note doubt:** the bus "burned up near 110 km": NSSDCA says signals until 110 km; NASA's
  science page says it burned up at about 120 km. Consistent enough; flagged only.

## venera-7 (program)
- Launch 17 August 1970; about 475 °C; pressure 9.0 MPa (about 90 bar) calculated from temperature
  and models, not measured; stopped within about 0.2 s on impact, so solid ground with little dust:
  https://en.wikipedia.org/wiki/Venera_7
- Three earlier probes (Venera 4, 5, 6) failed in the atmosphere: consistent with the component
  history note and Wikipedia's Venera program article.
- **History note doubt (program):** "about 60 km/h": Wikipedia gives 16.5 m/s, which is 59 km/h. OK.
  "some 23 minutes" of data: Wikipedia says about 20 minutes from the surface, with about 23 minutes
  of weak signal found on tape later. Fine, but the two figures are easily confused.

## venera-7-probe
- Lander 500 kg; parachute opened about 60 km, reefed to 1.8 m² and opened to 2.5 m² after the
  reefing line melted; built for about 18 MPa and 580 °C, a large margin over what was expected
  ("built far stronger than ... expected to need"); pressure inferred:
  https://en.wikipedia.org/wiki/Venera_7
- **History note doubt:** "built for 180 atmospheres and 540 degrees Celsius": Wikipedia gives about
  18 MPa (about 180 atm, fine) and 580 °C. The 540 °C figure needs a source check. The titanium hull
  was not confirmed in what I reached.

## venera-lander (program)
- Venera 9 launched 8 June 1975 on Proton-K/D, launch mass 4,936 kg (Venera 7 was 1,180 kg, so much
  heavier); carrier first spacecraft to orbit Venus and relayed the lander; about 485 °C and 90 atm:
  https://en.wikipedia.org/wiki/Venera_9 , https://en.wikipedia.org/wiki/Venera_7
- Later carriers flew past and relayed during the flyby (Venera 13 and 14; Venera 11 and 12 also
  flight platforms): https://en.wikipedia.org/wiki/Venera_13 , https://en.wikipedia.org/wiki/Venera_program
- "A lander lying on hot ground can hardly aim a radio at the Earth": reasoning (Venera 7's
  misaligned antenna after tipping over, Wikipedia Venera 7), not a sourced quote.

## venera-9-probe
- 2.4 m spherical entry shell, entry at 10.7 km/s, shed before the parachute; pressure sphere about
  80 cm; fluid loop and pre-cooling; crushable ring: https://ntrs.nasa.gov/api/citations/20170002533/downloads/20170002533.pdf ,
  https://en.wikipedia.org/wiki/Venera_9
- Carrier with its own engine braking into orbit: Venera 9 orbiter entered orbit 20 October 1975
  (Wikipedia). Solar panels: Venera 9 bus design (Wikipedia Venera program).
- History note (parachutes dropped about 50 km, disc brake, about 7 m/s) agrees with Wikipedia.

## mariner-6 (program)
- Twin Mariner 7; first Mariners on an Atlas-Centaur, heavier instruments; launch 25 February 1969;
  closest approach south of the equator; about 98 % CO2; polar cap consistent with solid CO2:
  https://science.nasa.gov/mission/mariner-6/
- Pressure 6 to 7 mb; instruments (two TV cameras, IR and UV spectrometers, IR radiometer, scan
  platform): https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1969-014A
- Mariner 4 flew in 1965 ("four years").
- **History note doubt (program):** "about 3,430 km up": NSSDCA 3,431 km, NASA science page 3,429 km. OK.
  The note's "camera team called Mars more like the Moon" is not contradicted, but NSSDCA's summary
  says the surface "was very different from the Moon's" (mixed findings); not wrong, flagged only.

## mariner-6-probe
- Octagonal magnesium frame 138.4 cm diagonal; four panels, 5.79 m span, 449 W at Mars; 1 m dish;
  two-axis scan platform; instruments; analog tape recorder for TV, played back after the flyby:
  https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1969-014A ,
  https://science.nasa.gov/mission/mariner-6/
- History note's 16,200 bps channel agrees with NSSDCA.

## mariner-9 (program)
- Launch 30 May 1971; burn 915.6 s on 14 November 1971 ("just over 15 minutes", "five and a half
  months after launch"; NSSDCA says 15 min 23 s); orbit
  12 h 34 min, trimmed to just under 12 h for synchronous Goldstone transmissions; 7,329 pictures;
  85 % mapped at 1 to 2 km; Valles Marineris; about 20 volcanoes by February 1972; last contact
  27 October 1972: https://science.nasa.gov/mission/mariner-9/ ,
  https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1971-051A
- Channels like dried riverbeds: secondary sources (Carr, Surface of Mars:
  https://resolve.cambridge.org/core/books/surface-of-mars/channels-valleys-and-gullies/5A55C3E6658F765483ABA91E0D77ACC4 ;
  https://www.open.edu/openlearn/science-maths-technology/the-search-water-on-mars/content-section-5.1 ).
  NASA's page says only that past water was speculated; the text says "looked as if", not that it was.

## mariner-9-probe
- Octagonal magnesium frame; launch mass 997.9 kg including 439.1 kg expendables ("almost half");
  1,340 N engine, up to 5 restarts; panels 6.89 m tip to tip; wide/narrow cameras, IR radiometer,
  UV spectrometer, IR interferometer spectrometer; turned off when attitude gas ran out:
  https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1971-051A

## viking (program)
- Launches 20 August and 9 September 1975 on Titan IIIE-Centaur; Viking 1 lander until November 1982:
  https://science.nasa.gov/mission/viking/
- Original 4 July landing put off because orbiter images showed the site too rough (boulders); new
  site about 800 km west-northwest, still in Chryse Planitia: https://en.wikipedia.org/wiki/Viking_1 ,
  https://solarviews.com/history/SP-4212/ch10-6.html
- Orbiter and lander flew joined; orbiter relayed: NSSDCA 1975-075A.

## viking-orbiter
- Based on Mariner 9; octagonal ring about 2.5 m; four wings, 620 W at Mars; 2,328 kg launch mass,
  1,445 kg propellant and gas; 1,323 N engine; scan platform with imaging, IR thermal mapping and
  water vapor instruments; relay radio: https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1975-075A

## viking-lander
- Six-sided aluminum base, three legs; two Pu-238 RTGs with wind screens; aeroshell with ablative
  heat shield; parachute 16 m diameter deployed at about 6 km; three throttleable hydrazine engines,
  18 nozzles each to disperse exhaust; touchdown about 2.4 m/s; sampler arm:
  https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1975-075C
- NSSDCA says "two 16 m diameter parachutes"; I wrote "a parachute about 16 m across" (one canopy
  per lander is the usual account), so no count is given.
- "To disturb the ground less": NSSDCA says to disperse the exhaust and minimize effects on the
  ground: https://en.wikipedia.org/wiki/Viking_program
- **History note:** "about 112 degrees Celsius for some 40 hours": Wikipedia/NASA tech paper give
  111 to 112 °C for 40 h; the National Academies report gives a minimum of 111.7 °C held for 30 h
  (https://www.nap.edu/read/11381/chapter/16). Acceptable as written.

---

# Batch 3 checks

Sources abbreviated below:
- WP-Mars2: https://en.wikipedia.org/wiki/Mars_2
- WP-Mars3: https://en.wikipedia.org/wiki/Mars_3
- WP-521: https://en.wikipedia.org/wiki/Mars_1969A ; WP-522: https://en.wikipedia.org/wiki/Mars_1969B
- WP-M69: https://en.wikipedia.org/wiki/M-69_(spacecraft)
- RSW-M69: https://russianspaceweb.com/mars69.html ; Gunter-M69: https://space.skyrocket.de/doc_sdat/mars-69.htm
- NSSDCA-P10: https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1972-012A
- NSSDCA-P11: https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1973-019A
- NASA-P10: https://science.nasa.gov/mission/pioneer-10/ ; NASA-P11: https://science.nasa.gov/mission/pioneer-11/
- NASA-X15: https://www.nasa.gov/reference/x-15/
- WP-XLR99: https://en.wikipedia.org/wiki/Reaction_Motors_XLR99 ; USAF museum XLR99: https://www.nationalmuseum.af.mil/Visit/Museum-Exhibits/Fact-Sheets/Display/Article/197692/reaction-motors-xlr99-rocket/
- WP-X15list: https://en.wikipedia.org/wiki/List_of_X-15_flights
- WP-XLR11: https://en.wikipedia.org/wiki/Reaction_Motors_XLR11
- Guinness altitude: https://www.guinnessworldrecords.com/world-records/highest-altitude-by-an-aircraft-(official)
- WP-MiG105: https://en.wikipedia.org/wiki/Mikoyan-Gurevich_MiG-105
- WP-Mercury: https://en.wikipedia.org/wiki/Project_Mercury ; WP-MercSC: https://en.wikipedia.org/wiki/Mercury_spacecraft
- WP-Vostok: https://en.wikipedia.org/wiki/Vostok_programme ; WP-VostokSC: https://en.wikipedia.org/wiki/Vostok_(spacecraft) ; WP-Vostok1: https://en.wikipedia.org/wiki/Vostok_1
- WP-Gemini: https://en.wikipedia.org/wiki/Project_Gemini ; WP-GemSuit: https://en.wikipedia.org/wiki/Gemini_spacesuit
- WP-Agena: https://en.wikipedia.org/wiki/Agena_target_vehicle ; WP-RM81: https://en.wikipedia.org/wiki/RM-81_Agena
- WP-AdvGem: https://en.wikipedia.org/wiki/Advanced_Gemini ; SFH-LunarGemini: https://www.spaceflighthistories.com/post/lunar-gemini
- WP-Transtage: https://en.wikipedia.org/wiki/Transtage
- WP-Voskhod: https://en.wikipedia.org/wiki/Voskhod_(spacecraft) ; WP-VoskhodProg: https://en.wikipedia.org/wiki/Voskhod_programme ; WP-Voskhod2: https://en.wikipedia.org/wiki/Voskhod_2
- WP-Berkut: https://en.wikipedia.org/wiki/Berkut_spacesuit
- WP-L1: https://en.wikipedia.org/wiki/Soyuz_7K-L1 ; WP-K146: https://en.wikipedia.org/wiki/Kosmos_146 ; WP-K154: https://en.wikipedia.org/wiki/Kosmos_154
- WP-Zond6: https://en.wikipedia.org/wiki/Zond_6 ; RSW-Zond7: https://russianspaceweb.com/zond7.html

## mars-2 (program)
- Launched 19 May 1971 on a Proton: WP-Mars2. Mars 3 launched 28 May 1971 (nine days later): WP-Mars3.
- Orbiter carried the lander and relayed its signals: WP-Mars2 ("relaying lander signals").
- About 60 pictures from Mars 2 and 3 together; surface temperatures -110 to 13 °C: WP-Mars2.
- "Air too thin for parachutes alone", radio delay rules out steering from Earth: physics; WP-Mars2 notes radio delays made remote driving of the rover impractical, and the descent used aeroshell + parachutes + retrorockets.
- Doubt: WP-Mars2's apoapsis figure is inconsistent (24,940 vs 2,494 km), so no orbit figures were used.

## mars-2-probe
- 4,650 kg total, orbiter 3,440 kg, descent module 1,210 kg: WP-Mars2.
- Aeroshell cone 2.9 m, landing capsule sphere 1.2 m, parachutes, landing engine, foam shock absorption, four petals righting the craft, two cameras, rover 4.5 kg placed by an arm: WP-Mars2, WP-Mars3.
- The history note's "first human-made object to reach the surface" (27 November 1971) is consistent with WP-Mars2.

## mars-m69 (program)
- Proton chosen to outdo Mariner (Mariner 6 and 7 in the 1969 window): RSW-M69, WP-M69.
- Planned orbit about 1,000 x 70,000 km: RSW-M69, Gunter-M69.
- Instruments: RSW-M69 lists 13 orbiter instruments including three tele-photometers, so "cameras and about ten other instruments". WP-521 lists nine instruments (fewer); I used RSW's count. Mild doubt.
- Begun from the Luna E-8 landing stage, unsuitable for the Mars trip: WP-521, WP-M69, Gunter-M69. Six-month cruise: RSW-M69.
- Redesigned after the failures into the M-71 (Mars 2/3, first 4MV): WP-M69.
- History note (dates 27 March and 2 April 1969, lander dropped) agrees with WP-521/522.

## mars-m69-probe
- Two fixed solar arrays: Gunter-M69. About 7 square meters: RSW-M69. Dish 2.8 m: RSW-M69.
- Film imaging, about 160 frames: Gunter-M69, RSW-M69. Sent as television signals: WP-521/522 ("encoding images as television signals"). "Scanned on board" is the implication of film plus TV transmission (standard for Soviet film-readout systems), not stated word for word.
- Small tanks of bubble-free propellant fired briefly to settle the propellant before the main engine starts: RSW-M69 (6 to 8 second firing).
- History note: 4.85 t matches WP/Gunter (RSW gives 3,834 kg for "version 2", a discrepancy; I avoided stating the mass). Its second failure "about 30 seconds after liftoff": WP-522 says the rocket fell and exploded 41 s after ignition, after the other engines compensated for about 30 s; Gunter says "after about 30 seconds". Acceptable but loose.

## pioneer-10 (program)
- 51,682 km/h, fastest at the time: NASA-P10 ("about 51,700 km/h").
- Entered asteroid belt 15 July 1972, emerged February 1973, fewer hits than expected: NASA-P10.
- Jupiter closest approach about 130,354 km from the planet (cloud tops), about 500 pictures, radiation triggered false commands, Io image lost: NASA-P10.
- Crossed Neptune's orbit 13 June 1983, first human-made object beyond Neptune: NASA-P10.
- Doubt on history note: it gives the Jupiter flyby as 3 December 1973. NSSDCA-P10 and NASA-P10 give 4 December 1973 (02:26 UT); 3 December is the US local date. Also launch: NASA-P10 gives 2 March 1972 01:49 UT; NSSDCA-P10 says 3 March. The note's 2 March matches NASA. Consider "4 December 1973" for UT consistency.

## pioneer-10-probe
- 258 kg, 2.74 m dish, hexagonal compartment: NSSDCA-P10.
- Spin-stabilized, dish on the spin axis pointed at Earth, about 4.8 to 5 rpm, CONSCAN movable feed, 8 W transmitter: search results citing Wikipedia Pioneer 10 (https://en.wikipedia.org/wiki/Pioneer_10) and the Pioneer telemetry almanac (https://arxiv.org/pdf/2606.23755). Rounded to "about five times a minute".
- RTGs with plutonium: NSSDCA-P10 (SNAP-19). History note's 155 W and 6.6 m magnetometer boom agree with NSSDCA-P10.

## pioneer-11 (program)
- Launch 6 April 1973; Jupiter 3 December 1974 about 42,500 km above cloud tops, over 171,000 km/h; less time in radiation belts thanks to speed; first polar images: NASA-P11 (NSSDCA-P11 says "within 43,000 km").
- Path back across the Solar System, corrections 26 May 1976 and 13 July 1978: NASA-P11. "Almost five years" Dec 1974 to Sep 1979.
- Saturn: about 440 images; bow shock at about 1.5 million km, first conclusive evidence of Saturn's magnetic field: NASA-P11.

## pioneer-11-probe
- 259 kg: NSSDCA-P11. Same design, added fluxgate magnetometer: NSSDCA-P11 (it is the "low-sensitivity" fluxgate, i.e. for strong fields).
- 144 W at Jupiter, 100 W at Saturn; operations ended September 1995 for lack of power: NSSDCA-P11.
- History note: F ring and the near miss of a small moon are consistent with NASA-P11 (new satellite about 200 km).

## x-15 (program)
- Dropped from a B-52 at about 45,000 ft (13,700 m): NASA-X15.
- Engine burn 80 to 120 seconds; dry lakebed landings: NASA-X15.
- 12 pilots: NASA-X15. Eight pilots above 50 miles (80 km), 13 flights, US astronaut wings: WP-X15list, Space.com (https://www.space.com/1465-nasa-15-pilots-awarded-astronaut-wings.html).
- Data fed Mercury, Gemini, Apollo and Shuttle; reaction controls: NASA-X15.

## x-15-aircraft
- Length about 50 ft (15 m), span 22 ft (6.7 m), launch 31,275 lb (14.2 t), burnout 12,295 lb (5.6 t): NASA-X15.
- XLR99: 57,000 lbf (about 254 kN), throttleable, restartable, liquid oxygen and anhydrous ammonia: NASA-X15, WP-XLR99, USAF museum.
- Hydrogen peroxide thrusters in nose (pitch, yaw) and wings (roll): NASA-X15.
- Lower part of the vertical tail jettisoned before landing; skids and nose wheel: NASA-X15.

## mig-e-154 (program)
- Written as an invention, per the brief and the history note. No flights invented.
- Ye-266M (E-266M), modified MiG-25, 37,650 m on 31 August 1977, official FAI absolute altitude record: Guinness altitude. Guinness describes it as the record for an aircraft (zoom climb), so "still the official altitude record for an aircraft" (air-launched rocket planes like the X-15 do not count under FAI rules for this record). Mild doubt: wording "for an aircraft" follows Guinness's title.
- Doubt on history note (component mig-e-154-spaceplane and program): "a subsonic test version of the orbiter flew eight times after being dropped from a Tu-95 bomber". WP-MiG105 and migflug say the MiG-105's first flight (11 October 1976) took off under its own power from an airstrip; air drops from a Tu-95K came from 1977. Eight flights 1976 to 1978 is right, but not all were drops. Suggest "flew eight times, some after being dropped from a Tu-95 bomber".

## mig-e-154-spaceplane
- Invented design (as the short description). XLR11: four chambers, no throttle, each chamber switched on and off separately: WP-XLR11, Centennial of Flight (https://centennialofflight.net/essay/SPACEFLIGHT/early_rocketry/SP7G4.htm).
- Control surfaces useless in thin air, hence thrusters: physics, and NASA-X15 on reaction controls.

## mercury (program)
- Approved 7 October 1958; three objectives: WP-Mercury.
- Ham, MR-2, 31 January 1961; Enos, MA-5, 29 November 1961, two orbits: WP-Mercury.
- Six crewed flights 1961 to 1963 on Redstone then Atlas; Faith 7 34 h 19 min, 22 orbits: WP-Mercury.
- Unknowns about swallowing and vision in weightlessness: widely documented concern of the period (e.g. Glenn ate applesauce on MA-6 to test it); general phrasing kept.

## mercury-capsule
- Length 3.3 m, pure oxygen at 5.5 psi (0.38 bar), heaviest flight version about 1,400 kg, hydrogen peroxide thrusters, three retrorockets, landing bag deployed by lowering the heat shield: WP-MercSC.
- Base diameter: WP-MercSC says 6.0 ft (1.8 m); Smithsonian lists 73 in (1.85 m) (https://si.edu/object/capsule-mercury-17:nasm_A19680570000). Rounded to "about 1.9 m"; could read "about 1.85 m" if preferred.
- Ablative fiberglass heat shield for orbital flights: WP-MercSC, NASA NTRS heat shield papers (https://ntrs.nasa.gov/citations/19660006061).
- Contoured couch "shaped to his body": well documented (individually molded couches); not separately sourced.

## vostok (program)
- OKB-1, Korolev: WP-Vostok.
- Manual controls locked, code in envelope: WP-Vostok1. (Sources disagree on the code's length; omitted.)
- Orbit decays within about ten days, supplies for ten days: WP-VostokSC.
- Dogs first (Korabl-Sputnik flights), six crewed flights 1961 to 1963, Vostok 3 and 4 in August 1962, Vostok 5 nearly five days (4 d 23 h), Tereshkova first woman: WP-Vostok.
- Note: the short description mentions "short hops"; Vostok never flew suborbital hops in reality. I didn't repeat it.

## gemini (program)
- Goals announced January 1962 (long flights, rendezvous/docking, EVA, controlled reentry and landing): WP-Gemini.
- Ten crewed flights 1965 to 1966; Gemini 7 13 d 18 h: WP-Gemini. "Longer than a trip to the Moon and back" (Apollo missions ran about 6 to 12 days).
- Rendezvous counter-intuitive (thrusting toward a target raises the orbit): orbital mechanics; Gemini 4's failed station-keeping attempt is the well-known example. Not tied to one flight in the text.

## gemini-capsule
- Reentry module plus adapter module (retrorockets, power, propulsion, oxygen, water), adapter discarded before reentry; 16 OAMS thrusters; ejection seats instead of escape tower: WP-Gemini.
- Rendezvous radar in nose: commonly documented; WP-Gemini mentions only "in-flight radar". Mild doubt about location wording (it was in the nose, per standard Gemini diagrams).
- Offset center of gravity (about 1.75 in), lift by rolling: NASA NTRS (https://ntrs.nasa.gov/api/citations/19720063745/downloads/19720063745.pdf) per search result.

## gemini-eva-suit
- Rubberized nylon bladder, link-net layer, outer cover; G4C added Mylar insulation layers; 3.7 psi (about 0.26 bar); ELSS chest pack on later flights (Gemini 8 to 12): WP-GemSuit.
- G4C had extra thermal and micrometeoroid layers: Smithsonian (https://airandspace.si.edu/collection-objects/pressure-suit-g4-c-lovell-gemini-12-flown/nasm_A19680449000) and Gemini 4 debriefing.
- Ed White, Gemini 4, June 1965: WP-Gemini.

## agena-target-vehicle
- Lockheed Agena D, McDonnell docking adapter, about 7.92 m by 1.52 m, Atlas launch, Bell engine qualified for up to 15 restarts: WP-Agena.
- 16,000 lbf (about 71 kN), UDMH and IRFNA: WP-RM81 (one fact sheet says N2O4; WP is more specific).
- Gemini 10 apogee 763 km, Gemini 11 record 1,369 km: WP-Agena.

## circumlunar-gemini-earth-orbit
- Lunar return about 11 km/s vs about 7.8 km/s: from the lunar-orbital component's history note, standard figures. Energy per kilogram ratio (11/7.8)^2 = about 2: arithmetic.
- 1964 study (Hammersmith, 3 April 1964): thicker heat shield and insulation made it too heavy for the Titan II: WP-AdvGem, SFH-LunarGemini.
- Two-launch scheme with a Transtage launched separately and Gemini docking to it: WP-AdvGem, SFH-LunarGemini.
- Gemini 10 and 11 boosted by docked Agenas: WP-Agena.
- Written as tests that never flew.

## gemini-transtage
- 4.57 m tall, 3.05 m diameter, gross 12,247 kg, two AJ10-138 engines of 8,000 lbf (36 kN), gimbaled, pressure-fed, Aerozine 50 and N2O4, burn time 440 s, restarts: WP-Transtage.
- History note: 16 June 1966 Titan IIIC with seven IDCSP satellites plus a gravity-gradient test satellite is consistent with what I know; not rechecked in detail.

## lunar-orbital-gemini-earth-orbit
- Apollo lunar orbit insertion about 889 m/s, trans-Earth injection about 1,000 m/s (Apollo 11): Apollo Flight Journal (https://www.nasa.gov/history/afj/ap11fj/video/21day6-tei.html) and search results. Hence "nearly 1 km/s" and "about as much again".
- NASA "Advanced Gemini Missions Conceptual Study", 30 July 1964: Centaur sends Gemini and Agena toward the Moon, Agena does lunar orbit insertion and trans-Earth injection: WP-AdvGem.

## gemini-lunar-propulsion-module
- Described honestly as the game's module, standing in for the Agena of the 1964 study: WP-AdvGem.
- Burn sizes as above. Hypergolic advantage: general engineering.

## gemini-direct-ascent-earth-orbit
- Lunar orbit rendezvous chosen in 1962: SFH-LunarGemini (and well known: July 1962).
- Gemini lunar direct ascent on one Saturn V, described as the last effort of direct-ascent supporters: WP-AdvGem, SFH-LunarGemini.
- The test list is framed as what tests would have had to prove; no flights invented.
- Note: the game says this program needs Nova; the real study used a Saturn V. The text says Saturn V for the real study and doesn't mention Nova.

## gemini-direct-ascent-craft
- Four modules: retrograde module with an RL10 for trans-lunar coast and initial descent, jettisoned before landing; terminal landing module; about a day on the surface; service module for ascent and trans-Earth injection; reentry module modified for lunar-return speed: WP-AdvGem, SFH-LunarGemini.
- The jettison altitude is given inconsistently (SFH: "5,900 miles (1,800 meters)"), so it was left out.
- History note: RL10 first flight 27 November 1963 (Atlas-Centaur AC-2) is correct as far as I know.

## voskhod (program)
- Original goal to beat Gemini to a multi-person crew, reused Vostok parts: WP-VoskhodProg.
- No ejection seats, so no escape: WP-Voskhod (crew returned inside the capsule; the Vostok seat was also the launch escape, WP-VostokSC).
- Voskhod 1, 12 October 1964, about a day, three crew: WP-VoskhodProg.
- Voskhod 2: automatic retrofire failed, manual reentry on a later orbit (18th instead of 16th), landed in dense forest in Perm region: WP-Voskhod2, Pavel Belyayev (https://en.wikipedia.org/wiki/Pavel_Belyayev). Distance off course is disputed (2,000 km on WP vs about 386 km elsewhere), so omitted.
- Voskhod 3 cancelled; superseded by Soyuz: WP-VoskhodProg, WP-Voskhod.
- History note: Leonov 12 minutes, 5.35 m tether matches common figures. Note that WP-Berkut gives the harness as 7 m long; 5.35 m is usually quoted as the tether length. Fine.

## voskhod-capsule
- Sphere 2.3 m; ejection seat removed; backup solid-fuel retrorocket on top; airlock 70 cm stowed, 2.5 m long extended, 1 m inner diameter, jettisoned after use: WP-Voskhod.

## berkut-eva-suit
- Modified SK-1, NPP Zvezda, three-layer pressure shell (load-bearing outer layer, main and backup sealed layers), vacuum insulation cover, oxygen vented through helmet and suit to space, two pressure settings 0.27 and 0.40 atm, suit 20 kg plus backpack 21 kg = 41 kg: WP-Berkut.
- History note: 0.4 atm and white suit consistent; WP-Berkut gives 45 minutes of primary life support (note says 30 to 45).

## soyuz-7k-l1-earth-orbit
- Two-crew circumlunar Soyuz derivative, Proton with Blok D: WP-L1.
- Star sensors and gyro platform for deep-space navigation: WP-L1.
- Kosmos 146, 10 March 1967, first Proton-K flight, judged successful: WP-K146.
- Kosmos 154, 8 April 1967: control fault jettisoned the ullage motors early, Blok D main engine did not ignite, stranded in low orbit: WP-K154, RSW (https://russianspaceweb.com/l1-3p-kosmos154.html).
- Doubt: WP-K146 is muddled (calls a 177 x 296 km orbit "highly elliptical" and mentions a translunar trajectory). I only said it went well.

## soyuz-7k-l1-spacecraft
- Orbital module replaced by a support cone, high-gain dish added, reserve parachute removed, gyro platform and star sensors added: WP-L1.
- Skip reentry to land in Kazakhstan instead of the Indian Ocean (Zond 5 had come down ballistically in the Indian Ocean), lower g-loads: WP-L1, WP-Zond6, RSW-Zond7 (RSW gives 10 to 15 g down to 4 to 7 g; not quoted).
- History note: Zond 6's depressurization and parachute released too early agree with WP-L1 ("crashed after parachute failure").

## Revisions after review (supersede notes above where they differ)
- gemini-capsule: now "the first crewed spacecraft that could change its own orbit" (Gemini 3, 23 March 1965, changed orbit with its OAMS thrusters; WP-Gemini, https://en.wikipedia.org/wiki/Gemini_3). Uncrewed stages such as the Agena had restarted in orbit earlier, so "American craft" was dropped.
- mig-e-154: the E-266M record is now "the record for an aircraft that takes off under its own power" (FAI absolute altitude record, Guinness), so it does not clash with the X-15's 108 km in this batch.
- mig-e-154-spaceplane: "four rocket engines" (as the short description), compared with the four-chamber XLR11.
- mars-2 / mars-2-probe: the rover on skis and its cable are now mentioned (WP-Mars2); foam is inside the capsule (WP-Mars2: "foam shock absorption inside the module").
- vostok: ejection before landing now mentioned (WP-VostokSC). Vostok 3/4 and Vostok 5 details were cut for length.
- voskhod: the inflatable airlock is named (WP-Voskhod).
- mercury-capsule: escape tower added (WP-MercSC: three small solid rockets on the tower).
- x-15-aircraft: "lower part of its ventral fin" (NASA-X15: the lower part of the vertical tail, jettisoned and recovered by parachute).
- gemini-lunar-propulsion-module: rewritten so as not to repeat the program's burn figures and 1964 study; pressure-fed hypergolic design and casting off before reentry are written as what the game's module "would" do (an imagined part), not as facts about a real proposal.
- gemini-direct-ascent texts: "a single heavy rocket" instead of "Saturn V", because the game launches this program on Nova. The real 1960s study used a Saturn V (WP-AdvGem); the caller may prefer to name it.

---

# Batch 4 checks

Sources abbreviated below:
- [EA-LK] http://www.astronautix.com/l/lk.html
- [EA-LOK] http://www.astronautix.com/s/soyuz7k-lok.html
- [EA-L1] http://www.astronautix.com/s/soyuz7k-l1.html
- [EA-LK700] http://www.astronautix.com/l/lk-700.html
- [EA-GLOR] http://www.astronautix.com/g/geminilor.html
- [W-LK] https://en.wikipedia.org/wiki/LK_(spacecraft)
- [W-LOK] https://en.wikipedia.org/wiki/Soyuz_7K-LOK
- [W-AG] https://en.wikipedia.org/wiki/Advanced_Gemini
- [SFH] https://www.spaceflighthistories.com/post/lunar-gemini
- [NSSDC-CSM] https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1969-059A
- [NSSDC-LM] https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1969-059C
- [NSSDC-LRV] https://nssdc.gsfc.nasa.gov/planetary/lunar/apollo_lrv.html

## Programs

### soyuz-7k-l3-earth-orbit
- LOK waits in lunar orbit, LK lands; no tunnel, spacewalk transfer in a lunar suit: [W-LOK], [EA-LOK].
- Blok D brakes into lunar orbit and lowers the LK: [W-LK].
- Kosmos 382, 2 December 1970, Blok D fired three times in Earth orbit simulating lunar orbit insertion, circularization and descent: https://en.wikipedia.org/wiki/Kosmos_382
- Avoided the history note's Kosmos 379/398/434 LK tests.
- Doubt (game data, not text): the real LOK and LK docked with the **Kontakt** system (LOK probe, LK hexagonal grid; [EA-LOK], [W-LK]), not Igla. The game grants `igla-docking-system` for this program and for `soyuz-7k-l3`. My texts avoid naming the docking system for the L3 complex so they don't contradict the game, but the Igla component here is historically wrong; a Kontakt component would be accurate.

### lk-700-earth-orbit
- Never built, so written as a plan. ~150 t in LEO (154 t gross, UR-700 payload 150 t), 17,124 kg landed, return capsule 3,130 kg, crew 2, typical mission 8.3 days, re-entry at 11 km/s: [EA-LK700].
- Engine restart need is an inference from the stage sequence in [EA-LK700] (braking, landing, return stages), not a quoted requirement.

### gemini-eor-earth-orbit
- Gemini 6A and 7 rendezvous, December 1965; Gemini 8 first docking with an Agena, 16 March 1966: https://en.wikipedia.org/wiki/Gemini_8 , https://www.space.com/32290-gemini-space-docking-50-years.html
- The rest (service module, tug restart, lander near home) describes what the game's test program would prove; no flight invented.

### circumlunar-gemini
- Gemini on Titan II meeting a Transtage launched separately, docking in LEO: [W-AG], [SFH].
- Transtage = Titan III upper stage: https://en.wikipedia.org/wiki/Transtage
- Heat shield worry and proposed thicker shield: [W-AG], [SFH] (stated for Gemini-Centaur).
- Lunar return ~11 km/s: Apollo 15 entry 36,096 ft/s (11.0 km/s), https://www.nasa.gov/history/afj/ap15fj/25b-day13_entry+splashdown.html ; orbital speed under 8 km/s is standard (~7.8 km/s). (11/7.8)^2 = 1.99, so "roughly twice the energy per kilogram" is my arithmetic.
- "Studied from 1961 to 1965": Chamberlin 1961, Hammersmith study 3 April 1964, Gemini Large Earth Orbit 1965 ([W-AG], [SFH]).
- Free return with no further firing: general trajectory fact (circumlunar free-return), consistent with Zond/Apollo 8 profiles.

### lunar-orbital-gemini
- Lunar orbit insertion about 900 m/s: Apollo 8 LOI cut 2,994 ft/s (913 m/s), burn behind the Moon after loss of signal: https://www.nasa.gov/history/afj/ap08fj/11day3_black_approach.html , https://www.nasa.gov/history/apollo-8-mission-details/
- Trans-Earth burn about 1,000 m/s or more: LOK's TEI 1,100 m/s [EA-LOK]. Apollo 8's TEI also fired on the far side, out of contact, with Houston waiting for acquisition of signal: https://theconversation.com/50-years-ago-australia-and-the-apollo-8-mission-that-sent-a-christmas-message-from-the-moon-104391 , https://patriotspoint.org/news/astronaut-jim-lovell-says-there-santa-claus
- Study with Centaur sending the stack and Agena doing LOI and TEI: [W-AG], [SFH].

### gemini-direct-ascent
- Lunar direct ascent Gemini: retrograde module with RL10, jettisoned at 1,800 m; terminal landing module; service module for ascent and TEI; Gemini-based re-entry module modified for lunar return; Saturn V launch: [W-AG]. ([SFH] garbles the altitude as "5,900 miles (1,800 meters)"; 1,800 m = 5,900 ft per [W-AG].)
- RL10 burns liquid hydrogen and oxygen: https://en.wikipedia.org/wiki/RL10
- "Every kilogram costs several more" is the rocket-equation multiplier described at https://en.wikipedia.org/wiki/Lunar_orbit_rendezvous (no figure given).
- Wrote "a giant rocket" rather than naming Saturn V or Nova, since the game requires Nova and the study used Saturn V.

### apollo
- LOR saves landing and relaunching the return propellant: https://en.wikipedia.org/wiki/Lunar_orbit_rendezvous
- LOR chosen July 1962 (announced 11 July 1962), alternatives direct ascent (Nova) and EOR: same page.
- Earth-Moon distance about 384,000 km (mean): standard.
- Two astronauts land while the third waits in orbit (short description): standard Apollo profile, NASA https://www.nasa.gov/mission/apollo-11/
- Crewed lunar flights December 1968 (Apollo 8) to December 1972 (Apollo 17): https://www.nasa.gov/history/apollo-8-mission-details/ ; Apollo 17 December 1972 standard.

### gemini-eor
- EOR used several launches joined in Earth orbit: https://en.wikipedia.org/wiki/Lunar_orbit_rendezvous
- Hydrogen boil-off in orbit is a general cryogenic fact (no figure given).
- Doubt: the history note's Chamberlin plan was actually a lunar orbit rendezvous plan with the Langley lander ([EA-GLOR], [SFH]: "Chamberlin favored LOR"); its Earth-orbit part was docking the capsule/lander with a stage. The game's "EOR" label is BASPM's framing. My text describes the game's program (assembly in Earth orbit, the lander returning to the stack in lunar orbit) without attributing it to Chamberlin.

### soyuz-7k-l1
- No orbital module, two crew: [W-L1] https://en.wikipedia.org/wiki/Soyuz_7K-L1 ; habitable volume 4 m³: [EA-L1]. No motive for leaving it off is given in the text (EA says it went with the reserve parachute to make room for a side hatch; weight saving is likely but unsourced).
- Proton + Blok D: [W-L1], [EA-L1].
- Skip re-entry to reach Soviet territory: Zond 7 page https://en.wikipedia.org/wiki/Zond_7 , Zond 6 limited to 4 to 7 g https://en.wikipedia.org/wiki/Zond_6
- Only Zond 7 (August 1969) would have been safe for a crew: [W-L1], Zond 7 page. Worded "safe for a crew", not "survived" (Zond 5's 20 g is survivable).
- Trip about a week: Zond 7 7 to 14 August 1969 [W-L1].

### soyuz-7k-l3
- Text says "one cosmonaut" (which crewman landed is not stated in my sources). One cosmonaut transfers by spacewalk (no tunnel), lands alone after Blok D braking, lifts off, docks and spacewalks back with samples: [W-LK], [W-LOK], [EA-LOK].
- Rode one N1; program cancelled May 1974: [W-LOK].

### lk-700
- 150 t in LEO; three clustered Block I stages for TLI, Block II braking, Block III landing, Block IV return with Block V capsule 3,130 kg; crew 2; surface stay 12 to 24 h, two moonwalks; mission 8.3 days: [EA-LK700].
- Doubt about the history note: [EA-LK700] dates the draft project approval to 20 October 1965 (UR-700 work from 1962) and lists 31 December 1970 as the definitive cancellation of UR-700/LK-700, while also saying work went on until 1974. The note's "proposed in 1964" and "cancelled in 1974" are defensible but not the only reading.

### apollo-earth-orbit
- Apollo 5, 22 January 1968, uncrewed LM-1, "fire in the hole" ascent ignition while mated: https://en.wikipedia.org/wiki/Apollo_5
- Apollo 7, October 1968, 10 days 20 hours, SPS fired eight times: https://en.wikipedia.org/wiki/Apollo_7
- The short description's spacewalks: Apollo 9 (the crewed LM flight) included Schweickart's EVA: https://en.wikipedia.org/wiki/Apollo_9 (standard; alluded to only, as the history note covers Apollo 9).
- Doubt (game data): the program's shared components include `agena-target-vehicle`; no Apollo flight used an Agena. Not mentioned in the text.

### soyuz-7k-ok
- Soyuz 1, April 1967, Komarov killed by parachute failure; Soyuz 3, October 1968, rendezvous with uncrewed Soyuz 2 but no docking: https://en.wikipedia.org/wiki/Soyuz_3 , https://www.nasa.gov/history/50-years-ago-soviets-return-cosmonauts-to-space/
- Soyuz 6/7/8, October 1969, seven cosmonauts at once, Vulkan welding on Soyuz 6: https://en.wikipedia.org/wiki/Soyuz_6
- Soyuz 9, June 1970, 17 d 16 h, crew barely able to walk, preparation for stations: https://en.wikipedia.org/wiki/Soyuz_9

## Components

### soyuz-7k-lok-spacecraft
- 9,850 kg; 10.0 to 10.06 m long: [W-LOK], [EA-LOK].
- Block I main engine 3,388 kgf (33 kN) for the 1,100 m/s TEI; smaller 417 kgf engine for lunar-orbit maneuvers: [EA-LOK].
- Fuel cells replaced solar panels: [W-LOK]; water used by crew: [EA-LOK].
- Docking probe (Kontakt) and a cupola for manual docking by sight: [EA-LOK].

### igla-docking-system
- Three antenna sets on the active craft (search, gyro-stabilized tracking, docking); orientation accurate to about one degree; slow approach; pin-cone units, active/passive: https://en.scientificrussia.ru/articles/the-worlds-first-igla-automatic-docking-system
- Range, closing speed and direction from antenna signals: NASA/MSFC paper via search, https://ntrs.nasa.gov/api/citations/19930012254/downloads/19930012254.pdf (variation in strength between antennas used for relative position); range and approach speed: https://russianspaceweb.com/soyuz8-launch.html
- Avoided the history note's Kurs and target-orientation material. No range figure used (only one source gave 25 km).

### krechet-eva-suit
- First semi-rigid suit, aluminum alloy hard torso, soft limbs, ball-bearing shoulder and wrist joints, waist and hip joints, fold-out chest panel, 10 h primary life support, back ring for getting up: https://en.wikipedia.org/wiki/Krechet-94
- Mass: the article gives 106 kg (spec list) and 90 kg (text). Used 106 kg; 106/6 = 17.7 kg, "under 18 kg".

### lk-lander
- 5,560 kg; 5.2 m high: [W-LK], [EA-LK]. Apollo 11 LM 15,103 kg [NSSDC-LM]: ratio 0.37, "little more than a third".
- Legs (LPU) left on the surface as launch stand: [W-LK] (Blok E lifts off from it), [EA-LK].
- Finger controller because of limited suit mobility: Krechet-94 page.
- Hover 15 to 20 s in later development (original plan 50 s): [EA-LK]. Written "perhaps 20 seconds".
- Hexagonal grid 1.8 m across with 108 receptacles for the LOK's probe: [EA-LK].
- "Flew it standing at a window": [EA-LK] describes the standing cosmonaut; consistent with history-free sources, not separately confirmed by a NASA source.

### lk-700-spacecraft
- Reconciled with the short description ("in one piece, with no rendezvous"): one craft for the whole trip, though its landing stage stays behind, as in [EA-LK700].
- Block III landing stage 3,500 kg, six legs; slopes up to 15 degrees; Block IV 11,670 kg lifts off and makes TEI; capsule 3,130 kg, Apollo-shaped, L/D 0.35: [EA-LK700]. EA is internally inconsistent about which block lifts off; followed its text.

### gemini-eor-service-module
- Gemini adapter: retrograde section with four solid TE-M-385 retrorockets fired one at a time; equipment section with OAMS (16 thrusters), propellant, power, oxygen and water: https://en.wikipedia.org/wiki/Gemini_spacecraft
- Lunar requirements (bigger tanks, more consumables) are design reasoning for a game construct; no such module existed. Avoided the history note's fuel-cell and discarded-before-reentry material.

### langley-lunar-lander
- Open, single-crew, no cabin; 4,372 kg with storable propellant, lighter with cryogenic: [EA-GLOR], [W-AG].
- Doubt: the short description says "a seat"; the program history note says the astronaut stands. Text says "a place for one astronaut" to avoid taking sides.

### light-lunar-eva-suit
- Gemini 9A, June 1966 (EVA 5 June), Cernan overheated and his visor fogged: https://en.wikipedia.org/wiki/Gemini_9A , https://www.thespacecollective.com/blog/gemini-9a
- The rest is a generic requirements list (no figures). Avoided the history note's sublimator.

### s-ivb-space-tug
- 17.8 m long, 6.6 m diameter, one J-2 of 1,033 kN (500 series), LH2/LOX, restartable on the 500 series: https://en.wikipedia.org/wiki/S-IVB
- TLI burn "nearly six minutes": Apollo 11 S-IVB TLI burn 5 min 41 s (thisdayinaviation, citing NASA), and about 5 min 47 s in other accounts: https://www.thisdayinaviation.com/tag/trans-lunar-injection/
- No real Space Tug use of the S-IVB; written as the game's tug role.

### apollo-lunar-eva-suit
- 3.7 psi (25.5 kPa) oxygen; liquid cooling garment with water tubes; PLSS 6 h (Apollo 7 to 14), 7 h (15 to 17); A7LB waist joint for the rover; total EVA mass about 91 kg (Apollo 11 to 14) and 81.6 kg/13.6 kg lunar quoted elsewhere in the same article: https://en.wikipedia.org/wiki/Apollo/Skylab_A7L . Used 91 kg and 91/6 = 15 kg. The article has citation warnings; figures are consistent with NASA EMU descriptions but I did not find a NASA page giving the 91 kg total.
- Wrote "multilayer outer cover" rather than 13 layers, to avoid seeming to contradict the history note's "about 21 layers" (whole suit).

### apollo-csm
- CM 3.9 m base, 3.65 m high, 6.17 m³; SM 3.9 m by 7.6 m; SPS 91,000 N; fuel cells; CSM 28,801 kg; heat shield 1.8 to 6.9 cm; three main parachutes: [NSSDC-CSM].
- Offset centre of mass, L/D about 0.37, rolled to steer: https://en.wikipedia.org/wiki/Apollo_command_and_service_module
- Entry about 11 km/s: Apollo 15 AFJ (above).

### apollo-lunar-module
- 15,103 kg total; descent propellant 8,248 kg + ascent 2,376 kg = 10,624 kg (more than half); descent stage octagonal 4.2 m; footpad span 9.4 m; descent engine about 45,000 N, deep-throttling; ascent engine fixed, about 15,000 N: [NSSDC-LM]. Wikipedia gives the ascent engine as 16,000 N (3,500 lbf): https://en.wikipedia.org/wiki/Apollo_Lunar_Module ; used "about 16 kN" (NSSDCA says about 15,000 N, so "about 15 to 16 kN"; either rounding is honest).
- Throttle 10 to 60 percent and full; crushable aluminum honeycomb in legs: Wikipedia LM page.

### lunar-roving-vehicle
- 210 kg, 3.1 m, 490 kg payload, 0.25 hp motor per wheel, two 36 V silver-zinc batteries, woven zinc-coated steel tires with titanium chevrons, stowed folded in LM quad 1, gyro and odometer navigation: [NSSDC-LRV].
- Aluminum alloy frame, lowered with tapes, walkback limit (relaxed on Apollo 17): https://en.wikipedia.org/wiki/Lunar_Roving_Vehicle

### soyuz-7k-ok-spacecraft
- About 6.5 t (6,560 kg; Soyuz 9 launch mass 6,460 kg), 7.95 m long, span 9.80 m: https://en.wikipedia.org/wiki/Soyuz_(spacecraft) (table column inferred), https://en.wikipedia.org/wiki/Soyuz_9
- Probe on active, drogue on passive; no internal transfer on 7K-OK, so spacewalks: https://en.wikipedia.org/wiki/Soyuz_7K-OK
- Avoided the history note's lifting re-entry.
- Program text carries the short description's Igla-guided automatic docking (Kosmos 186/188 in the history note, Igla sources above).

### yastreb-eva-suit
- Designed 1966 at Zvezda with Leonov's input, to fix the Berkut's ballooning; pulley-and-cable joints; suit 20 kg, life-support 21 kg; about 2.5 h; flew once (Soyuz 4/5): https://en.wikipedia.org/wiki/Yastreb
- Leonov's suit ballooned and he could not re-enter the airlock until he bled off pressure: https://en.wikipedia.org/wiki/Voskhod_2 , https://www.russianspaceweb.com/voskhod2-eva.html

## Other notes on the entries
- `soyuz-7k-lok-spacecraft` history note says the N1 exploded at 107 seconds on 23 November 1972; not checked (not my text), but sources commonly give about 107 s.
- `apollo-earth-orbit` and `soyuz-7k-l3` use the Igla/Agena components noted above.

---

## Review fixes (2026-10-27)

- mig-e-154, mig-e-154-spaceplane, gemini-lunar-propulsion-module: told in the game's own voice; the history note says the E-154 is our invention.
- lk-700: the last sentence no longer repeats the 150 t.
- venera-7: it sent a faint signal from the surface for about 20 minutes (23 by most accounts), not stopped at once.
- Existing text: pioneer-venus-multiprobe's note (two probes survived the impact), pioneer-6-probe's short description (it spun about once a second), mig-e-154-spaceplane's note (the MiG-105's first flight took off on its own), pioneer-10's note (closest approach 4 December 1973 UT).

## Follow-up decisions (user, 2026-10-27)

- **Langley lander: standing, no seat.** Air & Space's "Lunar Landers That Never Were" describes the Gemini plan's
  Langley-style lander as a platform "on which an intrepid astronaut would stand"
  (https://www.airspacemag.com/space/lunar-landers-that-never-were-24004927); NASA SP-4205, *Chariots for Apollo*, has
  Langley's 1961 "shoestring" craft as "a solo astronaut perched atop an open rocket platform with landing legs"
  (https://apolloexplorer.co.uk/books/SP-4205/ch3-5.html). No source gives it a seat. Short and long descriptions fixed.
- **Luna 2: two history notes, two stories.** The component's note now tells of the rocket's last stage striking the
  Moon about half an hour after the probe (https://en.wikipedia.org/wiki/Luna_2,
  https://en.wikipedia.org/wiki/Luna_(rocket)) and of Luna 1, the first craft to orbit the Sun.
- **Disputed figures: one value each, used consistently.** Venera 7 built for 540 °C (the history note and the archive
  page agree), Pioneer 2's peak 1,550 km, Venera 15's radar dish about 6 m long.
- **Vostok's "short hops"** stay in its short description (the game's suborbital flights mirror NASA's); no history note
  claims a suborbital Vostok.
- **Igla on the lunar Soyuz** stays (the real craft used Kontakt): one docking component serves the 7K-OK too.
- **Explorer 1: no spoiler** (user, 2026-10-27). The long description and the `explorer-1-orbit` history note told of the
  Van Allen belts, which the first satellite's archive page reveals once it has flown (and the theory "Is space above
  the air empty?" asks). The description now ends on the counter going where only sounding rockets had measured
  (his V-2, Aerobee and rockoon flights reached about 100 to 200 km for minutes: https://en.wikipedia.org/wiki/James_Van_Allen, https://en.wikipedia.org/wiki/Rockoon), with nobody knowing what it would find.
  The history note now tells of the missing tape recorder (`assets-raw/archive/CHECKS.md`), the radio to 23 May 1958
  (`design.md` §1.1, E-16) and the reentry on 31 March 1970 (JPL Mission and Spacecraft Library, https://space.jpl.nasa.gov/msl/QuickLooks/explorer1QL.html; Wikipedia, decay
  date; NSSDCA 1958-001A).
