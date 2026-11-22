# Launch Window trailer: shot list (draft 2, 2026-11-22)

The film as text, for review before anything is rendered. Once agreed it becomes `shots.json`
(PLAN.md §3), each fact in `FACTS.md`. About 2:30, 37 shots; beats 1 and 2 are cut (67.5 s). Shots
marked *(optional)* can go if the film runs long or the overnight run gets too heavy.

Conventions: **C** a caption (place and date, lower left, the edit), **S** an English subtitle for
Russian text on screen, **Sound** what the clip or the edit carries under the score. No real face is
shown (visors, backs, silhouettes, hands). Craft come from the user's mission configuration art as
references (PLAN.md §7).

The score carries the arc: a low pulse under the cold open, building through the satellite beat,
the first big swell at Gagarin, a drive through the middle years, silence for 1967, a swell for
Earthrise, a hard stop for the N1, and the long final build to the boot.

What beats 1 and 2 taught us, applied to 3–7 (details in PLAN.md §7):
- **Timing.** A shot needs about 4 s to play out (a 5 s render, trimmed); 2–2.5 s only in a montage.
  The narrator speaks about 2.4 words a second, so a line has at most about 2 words per second of its
  shots; a line may run across a cut ("... raise a glass / to the first satellite"), and a pause before
  a cut lets the picture land (13 got half a second more).
- **Narration** is calm and plain, past tense, a little dry ("The papers called it 'Flopnik'."). Avoid
  sounds the voice model blurs ("catch up" came out as "cached up").
- **Time jumps** use the tear-off calendar (first and last frame); 16 is its third and last use.
- **Craft** come from the user's mission configuration art as reference-to-video references; launchers
  have no art, so their prompts give scale (gantry, tower, people), as Vanguard's.
- **People:** few and large, each with their own hand or body action, never a mouth action; the rest
  blurred shapes; backs, visors and silhouettes where a face would be small.
- **Light:** whatever is lit in the first frame stays lit, so ignitions start from an unlit first frame.
- **Prompts** say what is wanted, never what isn't; the video prompt is rewritten for the keyframe
  actually picked.
- **Space is silent** in H3's sound: the score and the edit carry those shots.

## 1. Cold open: the announcements (summer 1955), about 32 s

| # | Shot | Len | Notes | Narration |
|---|---|---|---|---|
| 01 | **Establishing shot: Washington** An aerial view of the White House with a slow pan | 4.0 | C: *Washington*. | "The year is 1955" |
| 02 | **Washington, the White House press room.** Flashbulbs pop; a hand holds a typed statement; close on the words "small earth-circling satellites". Reporters' pencils move fast. | 7.0 | C: *29 July 1955*. US look. | "The United States announces its intention to launch an Earth satellite." |
| 03 | **Establishing shot: Moscow** An aerial view of the Kremlin with a slow pan | 3.0 | C: *Moscow*. | "In Moscow, ..." |
| 04 | **Moscow, a news-agency room.** A teleprinter hammers out a line in Russian saying "U.S. to launch an artificial Earth satellite" in Cyrillic; a hand tears the sheet off. | 5.0 | S: *U.S. to launch an artificial Earth satellite*. (The tested shot, in Russian.) | "... the news lands on the desks of men who have been dreaming of the same thing." |
| 05 | **Moscow, a crowded press conference.** Seen from behind a Soviet scientist at a bank of microphones: about fifty journalists, pencils stopping, then a rush for the door. | 5.0 | C: *2 August 1955*. | "Only days later they respond in kind" |
| 06 | **An American engineering office at night.** An engineer pins a newspaper clipping 'Soviets Plan Satellite Too' to a corkboard over a chalk sketch of an orbit. | 4.0 | American props make the setting clear | "And so the 'Space Race' is born ..." |
| 07 | **A Soviet design bureau at night.** The same gesture: a clipping in Cyrillic pinned over a rocket sketch with Cyrillic labels. The desk lamp clicks on. | 4.0 | S: *U.S. to launch an artificial Earth satellite*. Mirror of 06, different office but same gesture with soviet props. The race is on: first music hit. | "...and what a race it was!" |

## 2. The first satellite (1957), about 35 s

| # | Shot | Len | Notes | Narration |
|---|---|---|---|---|
| 08 | **Establishing shot, Calendar.** A tear-off calendar with pages rapidly torn off faster and faster and finally settling on 4 Oct. 1957 | 4.0 | | "For two years, both sides quietly got to work." |
| 09 | **Washington, the Soviet embassy reception.** A reporter whispers into an American scientist's ear; his smile fades. | 4.5 | C: *Washington, Soviet embassy reception*. | "Then, in October 1957, came a rude awakening." |
| 10 | **The toast, over the speaker's shoulder.** Glasses raised reluctantly, tight smiles, sparse applause. | 4.0 | (Tested.) | "All the Americans could do was raise a glass ..." |
| 11 | **Sputnik in orbit.** It glides over the Earth. | 4.0 | Sound: our synthesized beep, from here into 12. (Tested.) | "... to the first satellite in orbit: Sputnik." |
| 12 | **An American rooftop at dusk.** Neighbours look up; a man holds a radio up to the sky, the beep coming out of it; a small point of light crosses the sky. | 6.0 | What people saw was the rocket's core stage, brighter than the satellite: the point of light is right either way. | "That night, the whole world looked up, and listened." |
| 13 | **Establishing shot, Calendar.** A tear-off calendar with pages rapidly torn off faster and faster and finally settling on 6 Dec. 1957 | 4.5 | C: *6 December 1957*. | "America had to answer, and fast. Two months later ..." |
| 14 | **Cape Canaveral, the Vanguard pad.** The slim rocket lifts a few feet, stalls, sinks back and bursts into a fireball. | 4.5 | C: *Cape Canaveral, Vanguard launch attempt*.  | "... its own satellite stood ready, on live television." |
| 15 | **The scrub beside the pad.** Smoke drifting; in the bushes the little satellite lies on its side, still beeping. | 4.0 | True: it was thrown clear and kept transmitting. Sound: the beep, alone. | "The papers called it 'Flopnik'." |

## 3. The first humans (1961–62), about 38 s

| # | Shot | Len | Notes | Narration |
|---|---|---|---|---|
| 16 | **Establishing shot, a Soviet calendar.** The same tear-off device on a Soviet office wall, Cyrillic print, pages tearing off faster and faster until it settles on 12 April 1961. | 5.1 | C: *Wednesday, 12 April 1961* (the page reads "АПРЕЛЬ 12 1961 СРЕДА"; a caption, since white text vanishes on the white page). Its last frame is held a second (`"hold"`) so the date rests before the cut. First and last frame, as 08 and 13; the first page is in 1957. The third and last calendar: the motif now marks a Soviet First. | "In the spring of 1961, Moscow did it again." |
| 17 | **Inside Vostok 1.** A white helmet with "СССР" in red, seen from the side; the visor reflects the Earth through the porthole. | 4.0 | C: *Vostok 1*. The visor's reflection hides the face. First big swell. | "Yuri Gagarin flew into space." |
| 18 | **Vostok in orbit.** The capsule glides over the Earth, the camera following it. | 4.0 | Reference to video from `vostok_orbital.png`, as Sputnik. Space comes out silent: the score carries it. | "Once around the Earth ... in one hundred and eight minutes." |
| 19 | **An American hangar.** A half-assembled Mercury capsule on a stand; in front of it an engineer holds up a newspaper, its headline "SOVIET MAN ORBITS EARTH". A second engineer looks over his shoulder. | 4.0 | The paper stays still in his hands (still text holds, moving text garbles); headline written by us. Each man his own hand action. Check the capsule's shape against `mercury_suborbital.png`. | "Meanwhile, America was still on the ground." |
| 20 | **Cape Canaveral, Freedom 7.** A slim Redstone with its Mercury capsule stands by its gantry; it ignites and climbs off the pad on a short, straight flame. | 4.5 | C: *Cape Canaveral, Freedom 7*. As Vanguard: the first frame before ignition (a lit engine in the first frame stays lit the same way through the clip), the gantry and a few figures for scale. | "Three weeks later, Alan Shepard flew. Fifteen minutes, up and down." |
| 21 | **Friendship 7 over the Pacific.** The Mercury capsule in orbit, sunlight flaring off its shingles, the ocean far below. | 4.0 | C: *Friendship 7, 20 February 1962*. Reference to video from `mercury_orbital.png`. | "And in 1962, John Glenn circled the Earth three times." |
| 22a | **Houston, Rice Stadium.** From behind the lectern: the President's back and shoulders, papers on the lectern, the packed stands beyond him in a hot afternoon sun. | 6.5 | C: *Houston, 12 September 1962*. No face, as everywhere. Kennedy had set the Moon goal before Congress in May 1961; here he said why. Kennedy's sentence (8.6 s, the game's own `assets/audio/moon-speech.mp3`, NASA) starts after the narrator, near the end of this shot, and runs on over 22b. | "That September, the President told the world where the race would end." *(Kennedy: "We choose to go to the Moon …")* |
| 22b | **The stands.** A few people in the heat, each with their own action: a man shading his eyes with his programme, a woman fanning herself, a student in shirtsleeves leaning forward. | 6.0 | Few, large people; the rest blurred shapes. Kennedy's sentence ends over it, and the beat with it. | *(Kennedy: "… in this decade and do the other things, not because they are easy, but because they are hard.")* |

## 4. The middle years (1963–67), about 24 s

A fast montage under one running line, then a stop.

| # | Shot | Len | Notes | Narration |
|---|---|---|---|---|
| 23a | **The steppe, after landing.** A woman in an orange spacesuit seen from behind, her white helmet hanging from one hand, her short hair stirring in the wind; her parachute spread on the grass beside her. Ahead, a few villagers hurry across the field toward her; one waves his cap. | 3.5 | C: *Vostok 6, 16 June 1963*. Tereshkova, the first woman in space. Seen from behind (no real face): her hair and the helmet in her hand tell it. The landing details are creative licence (user, 2026-11-22). If short hair from behind doesn't read as a woman (user): fallback, she still wears the helmet in the first frame and lifts it off, and her shoulder-length hair falls out of it. | "The successes kept coming: the first woman in space ..." |
| 23 | **Outside Voskhod 2.** A cosmonaut drifts out of the airlock on a tether, the Earth huge below; his suit stiff and ballooned. | 4.0 | C: *Voskhod 2, 18 March 1965*. Reference to video from `voskhod_spacewalk.png`. The visor shows only the Earth. He had to let air out of his suit to get back in. | "... the first spacewalk ..." |
| 24 | **Gemini docking.** The nose of a Gemini capsule slides into the docking collar of an Agena target. | 3.5 | C: *Gemini 8, 16 March 1966*. Reference to video from `gemini_docking.png`; first and last frame if the reference won't make contact. | "... the first docking." |
| 25 | **A test stand at night.** A huge engine fires into a flame trench, the ground shaking, white light. | 2.5 | Pure energy for the montage. Here the flame should stay lit: the first frame shows it firing. | "Each new rocket more powerful than the last." |
| 26 | **Mission control, at ease.** Two controllers in a calm room: one leans back in his chair, hands behind his head; the other lights a cigarette; a clock ticks on the wall. Then the green lamp on the console turns red, and the man leaning back sits bolt upright. | 4.5 | The foreshadowing (user): the calm before 1967. A green lamp, lit, is placed in the keyframe (so its place and size are fixed); the prompt turns it red near the end, and the cut lands right after. First frame only (user): a last frame too might make the people move unnaturally. The lamp is symbolic, not a depiction of a particular moment. The other people blurred shapes. | "For a while, it seemed nothing could go wrong." |
| 27 | **Two launch pads at dusk,** an American one, then a Soviet one: empty, a single wreath on each. The music stops. | 5.0 | C: *1967*. Two shots of 2.5 s. Both sides lost crews that year (Apollo 1 in January, Soyuz 1 in April). No reenactment, only the absence. | "Until, in 1967, ... both sides lost a crew." |

## 5. Around the Moon (1968), about 13 s

| # | Shot | Len | Notes | Narration |
|---|---|---|---|---|
| 28 | **A Saturn V clears the tower.** Ice falling from its flanks, the flame blinding. | 4.5 | C: *Apollo 8, 21 December 1968*. The music comes back. The tower and its swing arms give the scale. | "In December 1968, three Americans set off for the Moon." |
| 29 | **Through the capsule window.** A gloved hand raises a camera as the Earth rises over the grey lunar horizon. | 5.0 | Earthrise, 24 December. A held breath. "A single small blue Earth above the horizon": say what is wanted, never "no second Earth". | "On Christmas Eve, they saw the Earth rise over the Moon." |
| 30 | **The Earth alone,** small and blue above the Moon's edge. *(optional)* | 3.0 | No narration: the score swells. | |

## 6. The last turn (July 1969), about 6 s

| # | Shot | Len | Notes | Narration |
|---|---|---|---|---|
| 31 | **Baikonur at night.** The N1 on its floodlit pad; its thirty engines light, it rises a few hundred metres, tilts, falls back; the explosion lights up the whole steppe. | 6.0 | C: *Baikonur, 3 July 1969*. The music cuts out on the flash. No reference art for the N1: try keyframes first, its tapered cone is the check; the first frame before ignition, the tower and floodlights for scale. | "In secret, the Soviet Moon rocket fell back onto its pad." |

## 7. The landing (July 1969), about 22 s, then the logo

The narrator's last line is in 32; from there the real voices of Apollo 11 tell it.

| # | Shot | Len | Notes | Narration |
|---|---|---|---|---|
| 32 | **A beach near the Cape.** Three people from behind in the foreground (a man lowering his binoculars, a woman shading her eyes, a boy pointing); far off, a Saturn V lifts on a pillar of flame. | 4.0 | C: *Apollo 11, 16 July 1969*. Thirteen days after the N1. | "Thirteen days later, Apollo 11 lifted off." |
| 33 | **Mission control, from behind.** Two controllers frozen at their consoles, one with a hand on his headset; a fuel gauge needle near empty. | 3.0 | Sound: "60 seconds" from the real flight loop (NASA, public domain, to confirm). | *(NASA: "60 seconds")* |
| 34 | **The lunar module's descent.** Dust streaming out flat from under the engine bell, the LM's shadow on the ground, the probes touching. | 4.0 | Reference to video from `apollo_lunar_landing_type_g_h.png`. Sound: "The Eagle has landed" (`assets/audio/archive/apollo-11-eagle-has-landed.ogg`, NASA). | *(NASA: "The Eagle has landed")* |
| 35 | **The ladder, from below.** A white-suited figure backs down the LM's ladder, the black sky behind. | 3.0 | The visor gold and closed. | |
| 36 | **The boot.** Close on a lunar overshoe on the LM's footpad; it lifts and moves forward over the grey dust. A frame before it touches: cut to black. | 4.5 | C: *Sea of Tranquility, 20 July 1969*. First and last frame (as the calendars): the last frame is the boot just above the dust, so the clip ends where we cut. Sound: `assets/audio/small-step.mp3` (NASA), which ends on "for man": the cut to black lands there. He stepped off the footpad, not the last rung. | *(NASA: "That's one small step for [a] man …")* |
| 37 | **Black, then the logo.** "Launch Window", then the tagline: *Stay one small step ahead.* | 4.0 | The end card; the splash clip may serve. | |

Total: about 165 s (about 160 s without the optional shots); beats 1–2 are cut at 67.5 s.

## Decided (user, 2026-11-22)

- **Length:** about 2:30 is fine.
- **Real voices:** NASA's Apollo 11 recordings carry the landing (33, 34, 36), and Kennedy's Rice speech gets a scene of its own
  (22a, 22b), in place of Glenn's parade.
- **1967 stays:** the losses are marked (27).
- **Tereshkova stays,** with a shot of her own (23a), seen from behind after landing.
- Renders: about 21 shots at two takes each is about 7 hours, queued in rounds so a good first take saves
  its second.
