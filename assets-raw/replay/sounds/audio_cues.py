"""The audio cue plan for the replay's clips: python assets-raw/replay/sounds/audio_cues.py

Writes AUDIO_CUES.md beside it: the sounds the clips need (the library), and every clip's cues per
variant, from the plan below (2026-10-14, a first pass over every published clip's timeline).

Each clip's cues are written as c(variants, at, sound, ...): variants a string of s, g, f (success,
glitch, failure); at in seconds into the clip; loop=True with until= and fade= for a loop (until the
clip's end if no until); every= repeats a one-shot every so many seconds up to until (the radio
pings); vol= a volume relative to the sound's own; note= what's happening. The times come from the
clips' own timelines (assets-raw/replay/steps/*.jsx, launch-solo/, launch-storyboard/); "≈" marks a
time read off a sheet rather than a constant, to check against the frame.

The spoiler check is ReplayClipsTest's rule: before the earliest of a clip's reveals every variant
must sound the same (a loop stopping at or after that reveal counts as playing on). A cue that breaks
it is flagged: as planned it can't go into cues.json until it's moved to the reveal or later, or made
the same in every variant.
"""
import collections
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
CLIPS = os.path.join(ROOT, "assets", "ui", "replay")
CONTENT = os.path.join(ROOT, "assets", "content")

# --- The library -----------------------------------------------------------------------------------
# id: (kind, status, length, takes, volume, description, where to look)
# kind: loop or one-shot. status: have (in replay-sounds.json with a take), new.
LIB = {
    # Engines and thrusters
    "rocket-roar": ("loop", "have", "16 s loop", 1, 0.5,
                    "A big rocket engine at full thrust, steady. The launch, the ascent's stages, the retro "
                    "motors, the rocket plane's engine (quieter).", "In the pool (JCI21, Pixabay)."),
    "ignition": ("one-shot", "new", "0.6-1.2 s", 2, 0.7,
                 "The moment an engine lights: a sharp whump and a ripping rise into the roar, so the "
                 "loop can start under its tail.", "Pixabay: 'rocket ignition', 'engine start whoosh'."),
    "engine-burn": ("loop", "new", "6-10 s loop", 1, 0.35,
                    "A spacecraft's engine burning in space, smaller and further off than a launch: a "
                    "steady, slightly muffled hiss-rumble with a low throb, heard through the structure. "
                    "Course burns, orbit insertions, the lander's engines.",
                    "Pixabay: 'thruster loop', 'rocket engine distant'; or rocket-roar low-passed."),
    "engine-sputter": ("one-shot", "new", "0.8-1.2 s", 3, 0.6,
                       "An engine coughing: irregular pops and choked bursts, catching and failing. The "
                       "stutters before a relight, a failure's dying engine.",
                       "Pixabay: 'engine sputter', 'misfire'; Stable Audio: 'rocket engine sputtering, "
                       "irregular bursts, failing to ignite'."),
    "engine-cutoff": ("one-shot", "new", "0.4-0.8 s", 1, 0.5,
                      "An engine stopping hard: a dull chunk and a short falling tail, for a cutout the "
                      "loop's fade alone would make too soft.", "Pixabay: 'engine shut off thud'."),
    "rcs-puff": ("one-shot", "new", "0.15-0.3 s", 4, 0.45,
                 "An attitude thruster's puff: a short, sharp pneumatic hiss (pssht), dry. Turning a "
                 "capsule, braking, backing away, course corrections, the hand-held gun (quieter).",
                 "Pixabay: 'air burst', 'pneumatic hiss short'."),
    "rcs-stuck": ("loop", "new", "2-4 s loop", 1, 0.5,
                  "A thruster stuck open: a continuous, rough, high-pressure hiss with a flutter. Gemini 8's "
                  "stuck jet.", "Pixabay: 'gas leak hiss loop', 'air hose'."),
    # Separation and mechanisms
    "pyro": ("one-shot", "new", "0.2-0.4 s", 3, 0.6,
             "Explosive bolts firing: a sharp, metallic crack-bang with a short ring, no boom. Staging, "
             "separations, straps letting go, an adapter opening.",
             "Pixabay: 'metal bang short', 'cap gun', 'pyrotechnic'."),
    "spring-push": ("one-shot", "new", "0.3-0.6 s", 2, 0.5,
                    "Springs letting a payload go: a metallic twang and a soft clunk.",
                    "Pixabay: 'spring release', 'boing metal'."),
    "latch-capture": ("one-shot", "new", "0.4-0.8 s", 2, 0.6,
                      "Docking latches catching: a heavy metallic clunk and a rattle of hooks, solid "
                      "and final. Also a hook catching a capsule.",
                      "Pixabay: 'heavy latch', 'metal door lock clunk'."),
    "latch-release": ("one-shot", "new", "0.3-0.5 s", 2, 0.5,
                      "Latches letting go: a lighter clack and a release of tension.",
                      "Pixabay: 'latch open click', 'metal release'."),
    "docking-bump": ("one-shot", "new", "0.3-0.6 s", 2, 0.6,
                     "A docking probe hitting the collar and bouncing off: a hollow metallic bonk, no catch.",
                     "Pixabay: 'metal thud hollow', 'bump metal'."),
    "metal-strain": ("one-shot", "new", "0.6-1.5 s", 3, 0.5,
                     "Structure under strain: a low creak and groan of metal, a jolt. Something holding that "
                     "should let go, a tumbling pair, a helicopter straining at its load.",
                     "Pixabay: 'metal creak', 'ship hull groan'."),
    "cable-snap": ("one-shot", "new", "0.2-0.4 s", 1, 0.6,
                   "A cable parting: a sharp snap and a whip.", "Pixabay: 'cable snap', 'rope snap'."),
    "hatch-open": ("one-shot", "new", "0.6-1.0 s", 1, 0.5,
                   "A hatch unlatching and swinging open: a lever clunk, a seal releasing, a creak.",
                   "Pixabay: 'hatch open', 'heavy door unlatch'."),
    "hatch-close": ("one-shot", "new", "0.6-1.0 s", 1, 0.5,
                    "A hatch swinging shut and locking: a thud and the lever's clunk.",
                    "Pixabay: 'hatch close', 'vault door close'."),
    "servo": ("loop", "new", "1-2 s loop", 1, 0.35,
              "A small electric motor driving an arm or a mast: a steady whir with a gear whine.",
              "Pixabay: 'servo motor loop', 'robot arm'."),
    "servo-stall": ("loop", "new", "1-2 s loop", 1, 0.4,
                    "The same motor straining against a jam: a higher, labouring whine with a rhythmic knock.",
                    "Pixabay: 'motor strain', 'stalled motor'."),
    "tool-clink": ("one-shot", "new", "0.1-0.2 s", 3, 0.35,
                   "Small metal contact heard through a suit: a glove on a handrail or a ladder rung, a "
                   "package taken off its mount. Muted, close.", "Pixabay: 'metal clink small'."),
    # Air, chutes, landing
    "wind": ("loop", "new", "6-10 s loop", 1, 0.4,
             "Air rushing past at speed: a broad, steady roar with gusts, for a fall under the chutes, "
             "a plane in flight, a probe in a thick atmosphere.", "Pixabay: 'wind rush loop', 'freefall wind'."),
    "wind-gust": ("one-shot", "new", "1-2 s", 2, 0.5,
                  "A gust hitting: a swelling whoosh that buffets and passes.", "Pixabay: 'wind gust'."),
    "chute-deploy": ("one-shot", "new", "0.5-1.0 s", 2, 0.6,
                     "A parachute's mortar firing and the canopy paying out: a pop and a fast fabric rustle.",
                     "Pixabay: 'parachute deploy', 'fabric whoosh'."),
    "chute-fill": ("one-shot", "new", "0.4-0.8 s", 2, 0.6,
                   "The canopy filling: a deep fabric whump that slows the fall.", "Pixabay: 'parachute open'."),
    "chute-flutter": ("loop", "new", "2-3 s loop", 1, 0.5,
                      "A chute streaming without filling: fast, loud fabric flapping.",
                      "Pixabay: 'flag flapping strong wind loop'."),
    "chute-rip": ("one-shot", "new", "0.4-0.8 s", 1, 0.6,
                  "A main chute tearing loose: a ripping of fabric and a snap of lines.",
                  "Pixabay: 'fabric rip', 'cloth tear'."),
    "splash": ("one-shot", "new", "1.0-2.0 s", 2, 0.7,
               "A capsule hitting the sea: a heavy splash and spray falling back (louder for a hard one).",
               "Pixabay: 'big splash', 'object into water'."),
    "sea": ("loop", "new", "8-12 s loop", 1, 0.35,
            "Open sea: swell slapping at a floating hull, gentle and steady.", "Pixabay: 'ocean waves hull loop'."),
    "sinking": ("one-shot", "new", "2-3 s", 1, 0.5,
                "Going under: water rushing in, gurgling, bubbles.", "Pixabay: 'sinking gurgle', 'bubbles'."),
    "ground-thud": ("one-shot", "new", "0.5-1.0 s", 3, 0.7,
                    "A craft touching down on land: a heavy thump and a scatter of dust and gravel (a lighter "
                    "take for a lander's foot, a harder one for a rough landing).",
                    "Pixabay: 'heavy thud dirt', 'impact ground'."),
    "crash": ("one-shot", "new", "1.0-2.0 s", 2, 0.8,
              "A hard impact: crumpling metal, a crunch, pieces rattling down.", "Pixabay: 'metal crash crunch'."),
    "helicopter": ("loop", "new", "4-8 s loop", 1, 0.45,
                   "A recovery helicopter hovering close: rotor thump and turbine whine.",
                   "Pixabay: 'helicopter hover loop'."),
    "winch": ("loop", "new", "1-2 s loop", 1, 0.4,
              "A hoist cable paying out or reeling in: a geared electric whine.", "Pixabay: 'winch loop'."),
    "inflate": ("one-shot", "new", "1.0-1.5 s", 1, 0.45,
                "A flotation collar inflating: a gas rush and the rubbery creak of it filling.",
                "Pixabay: 'inflate', 'air rush'."),
    "dust-hiss": ("loop", "new", "2-4 s loop", 1, 0.35,
                  "An engine's plume blasting dust off the ground: a sandy hiss over the engine.",
                  "Pixabay: 'sand blast loop', 'gravel spray'."),
    "skid-scrape": ("loop", "new", "2-3 s loop", 1, 0.5,
                    "A landing skid sliding on a dry lakebed: a grinding scrape (harsher, with sparks, for "
                    "metal on the ground).", "Pixabay: 'metal scrape loop', 'sliding gravel'."),
    "runway-rumble": ("loop", "new", "3-5 s loop", 1, 0.4,
                      "Heavy tyres rolling fast on a runway: a deep rumble and thump of joints.",
                      "Pixabay: 'airplane taxi runway loop'."),
    "brakes": ("one-shot", "new", "1.5-2.5 s", 1, 0.5,
               "Hard braking on a heavy aircraft: a squeal and the engines spooling down.",
               "Pixabay: 'airplane brakes', 'rejected takeoff'."),
    "gear-retract": ("one-shot", "new", "1.0-1.5 s", 1, 0.4,
                     "Landing gear coming up: a hydraulic whir and a closing clunk.", "Pixabay: 'landing gear'."),
    "jet-engines": ("loop", "new", "6-10 s loop", 1, 0.45,
                    "A big jet's engines: a turbine roar with its high whine (closer on the runway, steadier "
                    "in cruise).", "Pixabay: 'jet engine loop', 'airliner cabin engine'."),
    "airframe-buffet": ("loop", "new", "1-2 s loop", 1, 0.45,
                        "An airframe shaking in the air: a rattle and rumble, thuds of turbulence.",
                        "Pixabay: 'turbulence rattle'."),
    # Reentry, fire and explosions
    "reentry-plasma": ("loop", "new", "4-6 s loop", 1, 0.55,
                       "Reentry heating: a deep roar of hot air, crackling and buffeting, rising and falling "
                       "with the glow. Also a satellite burning up.",
                       "Stable Audio: 'spacecraft reentry plasma roar, crackling, buffeting'."),
    "explosion": ("one-shot", "new", "2-4 s", 2, 0.9,
                  "A rocket or a spacecraft blowing apart: a deep blast, a crack and debris raining down "
                  "(a smaller take for a burn-up's end or an impact).", "Pixabay: 'explosion debris'."),
    "fire": ("loop", "new", "4-6 s loop", 1, 0.5,
             "A propellant fire: a roaring, crackling blaze.", "Pixabay: 'fire roar loop'."),
    # On the pad
    "fueling": ("loop", "new", "4-6 s loop", 1, 0.35,
                "Propellant pumping up the tower: a rushing flow in pipes with a low pump hum.",
                "Pixabay: 'pipe flow loop', 'pump'."),
    "vent": ("loop", "new", "4-6 s loop", 1, 0.35,
             "Oxidizer boiling off: a soft, steady steam hiss with occasional puffs. Runs on from the "
             "preparations through the countdown into the launch.", "Pixabay: 'steam hiss loop'."),
    "leak": ("loop", "new", "2-3 s loop", 1, 0.5,
             "A high-pressure leak spraying: a sharp, loud jet of gas.", "Pixabay: 'pressure leak'."),
    "valve": ("one-shot", "new", "0.3-0.6 s", 1, 0.5,
              "A big valve slamming shut: a clank and a hiss cut off.", "Pixabay: 'valve close'."),
    "count-tick": ("one-shot", "new", "0.1-0.2 s", 2, 0.4,
                   "The countdown clock stepping on a second: a clean, low electronic tick (not the console's "
                   "telemetry blip).", "Pixabay: 'clock tick electronic'."),
    "count-hold": ("one-shot", "new", "0.5-0.8 s", 1, 0.5,
                   "The count stopping: a two-note falling tone.", "Stable Audio, or a synthesized placeholder."),
    # Radio and the craft's own alarms
    "radio-beep": ("one-shot", "new", "0.15-0.3 s", 3, 0.25,
                   "A craft calling home: a short radio beep in the manner of Sputnik's, with a little hiss. "
                   "One per radio ring on screen, every 0.5-0.8 s: it must sit low and stay pleasant.",
                   "NASA/public domain Sputnik recordings; Pixabay: 'radio beep'."),
    "radio-static": ("loop", "new", "2-3 s loop", 1, 0.35,
                     "A signal breaking up: crackling static with fragments of tone, louder than the console's "
                     "no-signal hiss.", "Pixabay: 'radio static loop'."),
    "heartbeat": ("one-shot", "new", "0.2-0.4 s", 2, 0.4,
                  "A heart monitor's beat as it comes down the radio: a soft electronic blip with a thump.",
                  "Pixabay: 'heart monitor beep'."),
    "power-down": ("one-shot", "new", "1.0-1.5 s", 1, 0.4,
                   "Power failing: an electric hum sliding down and cutting out, relays dropping.",
                   "Pixabay: 'power down'."),
    "power-up": ("one-shot", "new", "0.8-1.2 s", 1, 0.4,
                 "Power coming back: relays clicking in and a hum rising.", "Pixabay: 'power up'."),
    "lamp-alarm": ("loop", "new", "0.4-0.8 s loop", 1, 0.35,
                   "The craft's own alarm, in step with a blinking ALARM or FUEL lamp on screen: a short "
                   "two-tone beep, repeating (a cockpit's, not the console's master alarm).",
                   "Synthesized; Apollo's caution and warning tones as a reference."),
    "cabin-hum": ("loop", "new", "8-12 s loop", 1, 0.25,
                  "Inside a crewed craft in orbit: fans and pumps humming, the odd relay click.",
                  "Pixabay: 'spaceship interior ambience'."),
    # People
    "suit-breathing": ("loop", "new", "4-8 s loop", 1, 0.4,
                       "Breathing inside a space suit's helmet, with the hiss of its oxygen flow. Calm.",
                       "Pixabay: 'astronaut breathing'; NASA EVA recordings."),
    "suit-breathing-hard": ("loop", "new", "3-6 s loop", 1, 0.5,
                            "The same, labouring: fast, heavy breaths of a spacewalker in trouble.",
                            "Pixabay: 'heavy breathing helmet'."),
    "footfall": ("one-shot", "new", "0.2-0.4 s", 4, 0.4,
                 "A boot landing on the Moon, heard through the suit: a muffled crunch.",
                 "Foley: boots on gravel, low-passed."),
    "body-thud": ("one-shot", "new", "0.5-0.8 s", 1, 0.5,
                  "A suited figure falling flat: a muffled heavy thud and a grunt-free rustle.",
                  "Foley, low-passed."),
}

# --- The plan --------------------------------------------------------------------------------------


def c(v, at, sound, **kw):
    return dict(v=v, at=at, sound=sound, **kw)


def every(v, start, period, until, sound, **kw):
    return c(v, start, sound, every=period, until=until, **kw)


A = "sgf"
PLAN = {}
FAMILIES = collections.OrderedDict()


def clip(family, name, *cues):
    PLAN[name] = list(cues)
    FAMILIES.setdefault(family, []).append(name)


# The pad and the launch
clip("The pad and the launch", "launch-preparations",
     c("s", 0.3, "fueling", loop=True, until=4.8, fade=0.3, note="propellant runs up the tower until topped off (≈4.8)"),
     c("g", 0.3, "fueling", loop=True, until=2.6, fade=0.2, note="flow stops at the leak"),
     c("g", 3.9, "fueling", loop=True, until=5.0, fade=0.3, note="flow picks up again"),
     c("f", 0.3, "fueling", loop=True, until=2.6, fade=0.2, note="flow stops at the leak"),
     c(A, 2.0, "vent", loop=True, carry_out=True, note="oxidizer starts to vent; runs on into the countdown"),
     c("gf", 2.4, "leak", loop=True, until=3.4, note="the leak at the lower arm sprays"),
     c("g", 3.4, "valve", note="the valve shuts"),
     c("f", 3.0, "fire", loop=True, note="the leak catches fire at the base"),
     c("f", 4.3, "explosion", note="the lower tank goes"))
clip("The pad and the launch", "countdown",
     c(A, 0, "vent", loop=True, carry=True, note="venting carries on from the preparations, on into the launch"),
     c("sf", 0.3, "count-tick", note="5"), c("sf", 1.2, "count-tick", note="4"), c("sf", 2.1, "count-tick", note="3"),
     c("sf", 3.0, "count-tick", note="2"), c("sf", 3.9, "count-tick", note="1"), c("s", 4.8, "count-tick", vol=1.4, note="0"),
     c("g", 0.3, "count-tick", note="5"), c("g", 1.2, "count-tick", note="4"), c("g", 2.1, "count-tick", note="3"),
     c("g", 2.4, "count-hold", note="the count holds at 3, the digit blinking"),
     c("g", 3.6, "count-tick", note="2"), c("g", 4.5, "count-tick", note="1"), c("g", 5.4, "count-tick", vol=1.4, note="0"),
     c("f", 4.3, "ignition", note="the engine lights early"),
     c("f", 4.3, "engine-sputter", note="sputtering"),
     c("f", 4.6, "explosion", note="the base blows out"))
clip("The pad and the launch", "launch",
     c(A, 0, "vent", loop=True, carry=True, until=0.9, fade=0.4, note="venting until ignition"),
     c(A, 0.8, "ignition", note="ignition"),
     c("sg", 0.9, "rocket-roar", loop=True, note="full thrust (in cues.json)"),
     c("f", 0.9, "rocket-roar", loop=True, until=3.25, fade=0.2, note="full thrust, cut with the engine (in cues.json)"),
     c("g", 2.6, "engine-sputter", note="the stutter, 2.6 to 3.4"),
     c("g", 3.4, "ignition", vol=0.6, note="the flame catches again"),
     c("f", 3.0, "engine-sputter", note="the flame flickers"),
     c("f", 3.6, "explosion", note="≈ the break-up after the engine quits; check the frame"))
clip("The pad and the launch", "ascent",
     c(A, 0, "rocket-roar", loop=True, carry=True, until=2.8, fade=0.1, note="first stage (in cues.json)"),
     c(A, 2.9, "pyro", note="staging: the lower stage falls away"),
     c("sg", 3.2, "rocket-roar", loop=True, vol=0.6, until=6.1, fade=0.4, note="upper stage, further off (in cues.json)"),
     c("f", 3.2, "rocket-roar", loop=True, vol=0.6, until=3.45, fade=0.1, note="lights into the lower stage (in cues.json)"),
     c("f", 3.3, "explosion", note="it breaks up"))
# Orbit
clip("In Earth orbit", "deployment",
     c(A, 1.0, "spring-push", note="springs fire (the glitch's payload stays on, a jolt)"),
     c("g", 1.0, "metal-strain", vol=0.6, note="the jolt of a payload that doesn't go"),
     c("sf", 1.6, "rcs-puff", note="the stage backs away (the failure's the wrong way)"),
     c("g", 2.0, "spring-push", note="second try frees it"),
     c("g", 2.6, "rcs-puff", note="the stage backs away"),
     c("f", 2.2, "crash", vol=0.6, note="the stage runs into the payload"),
     every("s", 2.6, 0.75, 6.0, "radio-beep", note="the payload comes alive"),
     every("g", 3.6, 0.75, 6.0, "radio-beep", note="steady again, calling home"))
clip("In Earth orbit", "earth-orbit",
     every("s", 0.5, 0.75, 6.0, "radio-beep", note="a ring every 0.75 s"),
     every("g", 0.5, 0.75, 1.99, "radio-beep", note="until the rings stop at 2.0"),
     every("g", 3.4, 0.75, 6.0, "radio-beep", note="back at 3.4"),
     every("f", 0.5, 0.75, 1.99, "radio-beep", note="the rings stop at 2.0 for good"),
     c("f", 3.5, "reentry-plasma", loop=True, until=4.4, fade=0.3, note="the decaying orbit glows into the air"),
     c("f", 4.4, "explosion", vol=0.4, note="it burns up"))
clip("In Earth orbit", "insertion",
     c("s", 0.8, "engine-burn", loop=True, until=2.0, fade=0.3, note="the last burn up onto the orbit"),
     c("gf", 0.8, "engine-burn", loop=True, until=1.4, fade=0.1, note="the burn cuts out at 1.4"),
     c("gf", 1.4, "engine-cutoff"),
     c("g", 2.2, "engine-burn", loop=True, until=2.8, fade=0.3, note="relight"),
     c("f", 2.2, "engine-sputter", note="the relight dies at 2.4"),
     c("f", 3.0, "reentry-plasma", loop=True, until=4.6, fade=0.4, note="≈ it falls back and burns up"))
clip("In Earth orbit", "advanced-experiments-earth-orbit",
     every("s", 0.5, 0.75, 6.0, "radio-beep", note="readings going home"),
     every("gf", 0.5, 0.75, 1.99, "radio-beep"),
     c("gf", 2.0, "power-down", note="the power drops, the readings stop"),
     c("g", 3.2, "power-up", note="the backup comes in"),
     every("g", 3.2, 0.75, 6.0, "radio-beep", note="readings resume"))
clip("In Earth orbit", "biometric-data-transmission",
     every("s", 0.3, 0.6, 6.0, "heartbeat", note="a beat every 0.6 s on the strip"),
     every("s", 0.5, 0.75, 6.0, "radio-beep", vol=0.7),
     every("gf", 0.3, 0.6, 2.19, "heartbeat"), every("gf", 0.5, 0.75, 2.19, "radio-beep", vol=0.7),
     c("g", 2.2, "radio-static", loop=True, until=3.4, fade=0.2, note="the signal breaks up"),
     every("g", 3.4, 0.6, 6.0, "heartbeat", note="back"), every("g", 3.5, 0.75, 6.0, "radio-beep", vol=0.7),
     c("f", 2.2, "radio-static", loop=True, until=3.4, fade=0.6, note="breaks up, and never comes back"))
for lvl in ("i", "ii"):
    clip("In Earth orbit", f"duration-level-{lvl}",
         c(A, 0, "cabin-hum", loop=True, note="inside the craft as the hours pass"),
         c("gf", 2.2, "power-down", vol=0.6, note="the power dips, its gauge blinking"),
         c("gf", 2.2, "lamp-alarm", loop=True, until=3.4, note="with the blinking gauge"),
         c("g", 3.4, "power-up", note="saved"),
         c("f", 3.0, "lamp-alarm", loop=True, vol=1.3, until=3.8, note="the oxygen runs out"),
         c("f", 3.8, "engine-burn", loop=True, until=4.6, fade=0.3, note="cut short: down out of orbit"))
clip("In Earth orbit", "suborbital-flight",
     c(A, 1.2, "rcs-puff", note="the jets start the turn"),
     c("s", 3.0, "rcs-puff", note="and stop it"),
     c("g", 3.4, "rcs-puff", note="≈ late stop, it rocks"), c("g", 4.0, "rcs-puff"), c("g", 4.6, "rcs-puff"),
     c("f", 3.0, "rcs-stuck", loop=True, note="a jet sticks open; it spins on"))
# The way home
clip("The way home", "reentry-preparations",
     c(A, 0.8, "rcs-puff", note="a puff starts the turn"),
     c("s", 2.6, "rcs-puff", note="one from the other side stops it"),
     c("g", 3.0, "rcs-puff", note="late"), c("g", 3.7, "rcs-puff"), c("g", 4.2, "rcs-puff"),
     c("f", 2.6, "rcs-stuck", loop=True, note="a jet sticks open"))
clip("The way home", "retrofire",
     c("s", 1.0, "rocket-roar", loop=True, vol=0.7, until=3.6, fade=0.3, note="the three motors (in cues.json)"),
     c("g", 1.0, "rocket-roar", loop=True, vol=0.7, until=4.2, fade=0.3, note="(in cues.json)"),
     c("f", 1.0, "rocket-roar", loop=True, vol=0.7, until=2.6, fade=0.15, note="(in cues.json)"),
     c("gf", 1.6, "engine-sputter", vol=0.6, note="the middle motor dies"),
     c("g", 2.5, "ignition", vol=0.4, note="it lights again"),
     c("f", 2.9, "engine-sputter", note="a try only sputters"))
clip("The way home", "retrofire-jettison",
     c(A, 1.0, "pyro", note="the straps let go (the glitch's and failure's one only)"),
     c("gf", 1.2, "metal-strain", note="the pack swings out on the other strap"),
     c("gf", 2.8, "pyro", note="the second try"),
     c("f", 3.2, "metal-strain", note="the pack hangs on, rocking"))
clip("The way home", "modules-separation",
     c(A, 0.8, "pyro", note="the bolts fire"),
     c("s", 1.25, "cable-snap", note="the cable parts"),
     c("gf", 1.6, "metal-strain", note="the cable holds; the pair tumbles"),
     c("g", 3.4, "cable-snap", note="it burns through"),
     c("f", 4.4, "reentry-plasma", loop=True, fade=0.4, note="joined, into the air"))
clip("The way home", "reentry",
     c("sg", 1.6, "reentry-plasma", loop=True, until=4.6, fade=0.6, note="the glow builds, peak 2.2 to 3.4"),
     c("f", 1.6, "reentry-plasma", loop=True, until=3.4, fade=0.05),
     c("g", 2.4, "airframe-buffet", loop=True, until=3.9, fade=0.3, note="it oscillates"),
     c("f", 2.4, "airframe-buffet", loop=True, until=3.4, fade=0.05),
     c("f", 3.4, "explosion", note="it breaks up"))
for name, ground in (("descent-and-splashdown", False), ("descent-and-landing", True), ("descent-chutes", None)):
    cues = [c(A, 0, "wind", loop=True, until={"s": 2.6, "g": 3.2, "f": 6.4}, fade=0.8, note="falling fast"),
            c(A, 0.8, "chute-deploy", note="the drogue"), c(A, 1.0, "chute-fill", vol=0.6),
            c(A, 1.6, "chute-deploy", note="the main"),
            c("s", 1.8, "chute-fill", note="the main fills"),
            c("gf", 1.6, "chute-flutter", loop=True, until={"g": 3.0, "f": 6.4}, fade=0.2, note="the main streams"),
            c("g", 2.0, "chute-fill", vol=0.6, note="half"), c("g", 3.0, "chute-fill", note="full")]
    if ground is False:
        cues += [c("s", 4.4, "splash"), c("s", 4.6, "sea", loop=True), c("g", 4.7, "splash", vol=1.2),
                 c("g", 4.9, "sea", loop=True), c("f", 3.6, "splash", vol=1.4, note="a big splash"),
                 c("f", 3.8, "sinking", note="it goes under")]
    elif ground is True:
        cues += [c("s", 4.4, "ground-thud"), c("g", 4.7, "ground-thud", vol=1.2, note="harder; tips onto its side"),
                 c("f", 3.6, "crash", note="it hits, crumpling")]
    clip("The way home", name, *cues)
for name, ground in (("landing-and-recovery", True), ("splashdown-and-recovery", False)):
    cues = [c(A, 0, "wind", loop=True, vol=0.5, until=2.4, fade=0.4, note="under the main, the sky clear"),
            c("gf", 1.0, "wind-gust", note="a gust swings it"),
            c("f", 1.4, "chute-rip", note="the main tears loose"),
            c("sg", 3.2, "helicopter", loop=True, fade=0.5, note="the recovery helicopter comes in and hovers")]
    if ground:
        cues += [c("s", 2.4, "ground-thud"), c("g", 2.6, "ground-thud", vol=1.2, note="hard, tips onto its side"),
                 c("f", 2.2, "crash", note="it crumples")]
    else:
        cues += [c("s", 2.4, "splash"), c("sg", 2.6, "sea", loop=True), c("g", 2.6, "splash", vol=1.2, note="harder, lists"),
                 c("f", 2.2, "splash", vol=1.4), c("f", 2.4, "sinking", note="it goes under")]
    clip("The way home", name, *cues)
clip("The way home", "recovery",
     c(A, 0, "sea", loop=True, note="floating where it came down"),
     c(A, 0, "helicopter", loop=True, note="the helicopter comes in from the right"),
     c("s", 1.6, "inflate", note="the flotation collar fills"),
     c("g", 2.6, "inflate", note="late; it rights the capsule"),
     c("s", 2.2, "winch", loop=True, until=2.8, note="the hoist comes down"),
     c("g", 3.0, "winch", loop=True, until=3.6),
     c("f", 2.6, "winch", loop=True, until=3.2),
     c("s", 2.8, "latch-capture", vol=0.7, note="hooked"), c("g", 3.6, "latch-capture", vol=0.7),
     c("f", 3.2, "latch-capture", vol=0.7),
     c("f", 3.0, "metal-strain", note="the helicopter strains at a flooded capsule"),
     c("f", 4.0, "cable-snap", note="it has to let go"), c("f", 4.0, "sinking", note="it sinks (Liberty Bell 7)"))
# Burns and coasts on the maps
clip("Burns on the orbit maps", "tpi",
     c("s", 1.0, "engine-burn", loop=True, until=2.6, fade=0.3, note="out of the Earth's grip"),
     c("gf", 1.0, "engine-burn", loop=True, until=1.6, fade=0.1, note="cuts out at 1.6"),
     c("gf", 1.6, "engine-cutoff"),
     c("g", 2.2, "engine-burn", loop=True, until=3.4, fade=0.3, note="relight"),
     c("f", 2.2, "engine-burn", loop=True, until=2.6, fade=0.05), c("f", 2.6, "engine-sputter", note="the relight dies"))
clip("Burns on the orbit maps", "rendezvous-target-approach",
     c("s", 1.6, "engine-burn", loop=True, until=2.6, fade=0.3, note="up onto the other's orbit"),
     c("gf", 1.6, "engine-burn", loop=True, until=2.0, fade=0.1), c("gf", 2.0, "engine-cutoff", note="cuts out halfway"),
     c("g", 2.6, "engine-burn", loop=True, until=3.2, fade=0.3, note="relight"))
for name in ("loi", "mars-orbit-injection"):
    clip("Burns on the orbit maps", name,
         c(A, 1.6, "engine-burn", loop=True, until={"s": 2.2, "g": 1.9, "f": 1.9}, fade=0.2,
           note="behind the planet, the capture burn" + (" (the Moon: out of contact; no rings there)" if name == "loi" else "")),
         c("gf", 1.9, "engine-sputter", note="it stutters and cuts out"),
         c("g", 2.0, "engine-burn", loop=True, until=2.15, fade=0.1, note="a short relight still captures it"))
clip("Burns on the orbit maps", "tei",
     c("s", 1.6, "engine-burn", loop=True, until=2.4, fade=0.3, note="behind the Moon, the way home"),
     c("gf", 1.6, "engine-burn", loop=True, until=1.9, fade=0.1), c("gf", 1.9, "engine-cutoff"),
     c("g", 2.2, "engine-burn", loop=True, until=2.9, fade=0.3),
     c("f", 2.2, "engine-burn", loop=True, until=2.4, fade=0.05), c("f", 2.4, "engine-sputter", note="the relight dies"))
clip("Burns on the orbit maps", "lunar-ascent",
     c(A, 0.4, "ignition", vol=0.5, note="lifting off the surface"),
     c("s", 0.4, "engine-burn", loop=True, until=2.4, fade=0.3),
     c("gf", 0.4, "engine-burn", loop=True, until=1.4, fade=0.1), c("gf", 1.4, "engine-cutoff"),
     c("g", 2.2, "engine-burn", loop=True, until=3.0, fade=0.3),
     c("f", 2.2, "engine-burn", loop=True, until=2.4, fade=0.05), c("f", 2.4, "engine-sputter"),
     c("f", 3.6, "explosion", vol=0.4, note="≈ it hits the Moon"))
for name in ("earth-mars-transit", "earth-venus-transit", "earth-jupiter-transit", "jupiter-saturn-transit",
             "venus-mercury-transit", "trans-lunar-coast", "trans-earth-coast"):
    clip("Cruises and coasts on the maps", name,
         every("sg", 0.4, 0.8, 6.4, "radio-beep", note="a ring every 0.8 s"),
         every("f", 0.4, 0.8, 2.99, "radio-beep", note="until it goes quiet at 3.0"),
         c("s", 2.0, "rcs-puff", vol=0.6, note="the course correction"),
         c("gf", 3.0, "rcs-puff", vol=0.6, note="the correction (the failure's misfires)"))
for name in ("lunar-approach", "venus-approach", "mars-approach", "jupiter-approach", "saturn-approach"):
    clip("Approaches and flybys", name,
         every("sg", 0.5, 0.75, 6.0, "radio-beep", note="a ring every 0.75 s" + (" (none from the Moon's radio shadow)" if name == "lunar-approach" else "")),
         every("f", 0.5, 0.75, 6.0, "radio-beep"),
         c("s", 2.0, "rcs-puff", vol=0.6, note="the course correction"),
         c("gf", 3.0, "rcs-puff", vol=0.6, note="the correction (the failure's misfires)"))
for name in ("lunar-flyby", "venus-flyby", "mars-flyby", "jupiter-flyby", "saturn-flyby", "mercury-flyby"):
    clip("Approaches and flybys", name,
         every("sg", 0.5, 0.75, 6.4, "radio-beep", note="a ring every 0.75 s" + (" (quiet behind the Moon, about 2.4 to 3.6)" if name == "lunar-flyby" else "")),
         every("f", 0.5, 0.75, 3.99, "radio-beep", note="until it's lost after 4.0"),
         c("gf", 4.0, "rcs-puff", vol=0.6, note="the correction (the failure's misfires)"))
# At the planets and the Sun
for name in ("lunar-orbit", "venus-orbit", "mars-orbit", "sun-measurements"):
    clip("At the planets and the Sun", name,
         every("s", 0.5, 0.75, 6.0, "radio-beep", note="mapping, a ring every 0.75 s" + (" (none from behind the Moon)" if name == "lunar-orbit" else "")),
         every("gf", 0.5, 0.75, 1.99, "radio-beep"),
         c("gf", 2.0, "power-down", vol=0.6, note="safe mode: quiet, tumbling a little"),
         c("g", 3.2, "power-up", note="back"), every("g", 3.2, 0.75, 6.0, "radio-beep"))
clip("At the planets and the Sun", "lunar-impact",
     every("s", 0.3, 0.5, 3.99, "radio-beep", note="pictures, a ring every 0.5 s"),
     c("s", 4.0, "radio-static", loop=True, until=4.6, fade=0.3, note="it strikes: the signal ends"),
     every("gf", 0.3, 0.5, 1.79, "radio-beep"),
     c("gf", 1.8, "radio-static", loop=True, until=2.8, fade=0.2, note="the signal drops"),
     every("g", 2.8, 0.5, 3.99, "radio-beep"), c("g", 4.0, "radio-static", loop=True, until=4.6, fade=0.3),
     every("f", 2.8, 0.5, 6.0, "radio-beep", note="back, but off its aim: it flies on past"))
clip("At the planets and the Sun", "earth-approach",
     every("sg", 0.5, 0.75, 3.39, "radio-beep"), every("f", 0.5, 0.75, 6.0, "radio-beep"),
     c("g", 2.6, "rcs-puff", vol=0.6, note="the correction"),
     c("f", 2.6, "rcs-puff", vol=0.6, note="the correction misfires"),
     c("sg", 3.4, "reentry-plasma", loop=True, vol=0.6, fade=0.5, note="into the corridor, glowing"))
# Rendezvous and docking
clip("Rendezvous and docking", "rendezvous",
     c(A, 0.2, "rcs-puff", note="in from the left"), c(A, 1.4, "rcs-puff", note="the brake"),
     c("g", 3.0, "rcs-puff", note="a puff brings it back"),
     c("f", 3.0, "engine-sputter", vol=0.4, note="the puff only sputters: its fuel spent"))
clip("Rendezvous and docking", "target-approach",
     c(A, 0.2, "rcs-puff"), c("s", 1.8, "rcs-puff", note="the brake"),
     c("g", 2.1, "rcs-puff", vol=1.3, note="a hard, late brake"), c("g", 2.4, "rcs-puff"), c("g", 3.0, "rcs-puff"),
     c("f", 2.1, "rcs-puff", note="a puff on its top: it slides under"))
clip("Rendezvous and docking", "docking",
     c(A, 0.4, "rcs-puff", note="in"),
     c("s", 1.6, "latch-capture", note="the latches catch"),
     c("gf", 1.6, "docking-bump", note="the nose bounces off"),
     c("gf", 2.4, "rcs-puff", note="in again"),
     c("g", 3.0, "latch-capture"), c("f", 3.0, "docking-bump", note="bounces again"),
     c("f", 3.2, "rcs-puff", note="backing away"))
clip("Rendezvous and docking", "undocking",
     c(A, 0.8, "latch-release", note="the latches let go (the glitch's and failure's hold)"),
     c(A, 0.9, "rcs-puff"),
     c("gf", 0.9, "metal-strain", vol=0.6, note="the puffs only shake the two"),
     c("gf", 2.2, "latch-release", note="second try"), c("g", 2.3, "rcs-puff"),
     c("f", 2.3, "rcs-stuck", loop=True, note="a jet sticks open: it tumbles away (Gemini 8)"))
clip("Rendezvous and docking", "transposition",
     c(A, 0.3, "pyro", note="the capsule lets go of the adapter, the panels open"),
     c(A, 0.3, "rcs-puff"), c(A, 1.2, "rcs-puff", note="it turns round"), c(A, 2.2, "rcs-puff", note="in"),
     c("s", 2.8, "latch-capture"),
     c("gf", 2.8, "docking-bump"), c("gf", 3.2, "rcs-puff"), c("gf", 3.6, "docking-bump"), c("gf", 4.0, "rcs-puff"),
     c("g", 4.4, "latch-capture", note="the third try holds"), c("f", 4.4, "docking-bump", note="it backs away"))
clip("Rendezvous and docking", "extraction",
     c(A, 0.6, "latch-release", note="the lander let go from the stage"),
     c(A, 0.7, "rcs-puff", note="backing away"),
     c("gf", 0.7, "metal-strain", note="it holds fast"),
     c("g", 1.8, "latch-release"), c("g", 1.9, "rcs-puff"),
     c("f", 1.8, "metal-strain"), c("f", 2.3, "latch-release", note="the capsule lets go of it, backs off alone"),
     c("f", 2.4, "rcs-puff"))
# Spacewalks
clip("Spacewalks", "eva",
     c(A, 0, "suit-breathing", loop=True, until={"s": 6.0, "g": 6.8, "f": 2.6}, fade=0.4, note="the whole walk"),
     c(A, 0.3, "hatch-open"),
     c("s", 2.3, "tool-clink", note="takes the package"),
     c("g", 2.6, "rcs-puff", vol=0.5, note="the gun, puff by puff, to 3.2"), c("g", 2.9, "rcs-puff", vol=0.5),
     c("g", 3.9, "tool-clink", note="takes the package, late"),
     c("f", 2.6, "suit-breathing-hard", loop=True, fade=0.4, note="the visor fogs; hauled back in"),
     c("s", 4.6, "hatch-close"), c("g", 6.0, "hatch-close"), c("f", 5.2, "hatch-close"))
clip("Spacewalks", "crew-transfer",
     c(A, 0, "suit-breathing", loop=True, note="the crossing"),
     c(A, 0.3, "hatch-open"), c(A, 0.6, "hatch-open", vol=0.7, note="the other craft's"),
     every(A, 1.8, 0.35, 2.19, "tool-clink", note="hand over hand on the rail"),
     every("s", 2.2, 0.35, 3.2, "tool-clink"),
     c("g", 2.8, "rcs-puff", vol=0.5, note="the gun brings it back"), c("g", 3.1, "rcs-puff", vol=0.5),
     every("g", 3.4, 0.35, 4.4, "tool-clink"),
     c("s", 4.6, "hatch-close"), c("g", 5.8, "hatch-close"), c("f", 5.0, "hatch-close"))
# On the Moon
clip("On the Moon", "powered-descent",
     c("sg", 0, "engine-burn", loop=True, note="the descent engine, against its way"),
     c("f", 0, "engine-burn", loop=True, until=2.8, fade=0.1),
     c("gf", 2.0, "lamp-alarm", loop=True, until={"g": 3.2, "f": 3.0}, note="the ALARM lamp (Apollo 11's alarms)"),
     c("f", 3.0, "pyro", note="abort: the ascent stage fires off"),
     c("f", 3.0, "engine-burn", loop=True, vol=0.8, note="the ascent engine climbing away"))
clip("On the Moon", "lunar-landing",
     c("s", 0, "engine-burn", loop=True, until=2.6, fade=0.1), c("g", 0, "engine-burn", loop=True, until=3.4, fade=0.1),
     c("f", 0, "engine-burn", loop=True, until=2.8, fade=0.05),
     c(A, 1.0, "dust-hiss", loop=True, until={"s": 3.2, "g": 4.0, "f": 2.9}, fade=0.6, note="the plume blowing dust"),
     c("gf", 2.0, "lamp-alarm", loop=True, until={"g": 3.4, "f": 2.8}, note="the FUEL lamp"),
     c("s", 2.6, "ground-thud", vol=0.6, note="touchdown"), c("g", 3.4, "ground-thud", vol=0.6),
     c("f", 2.8, "engine-sputter", note="the engine dies"), c("f", 3.1, "crash", note="it lands hard and tips over"))
clip("On the Moon", "moonwalk",
     c(A, 0, "suit-breathing", loop=True, until={"s": 5.4, "g": 5.8, "f": 2.6}, fade=0.4),
     every(A, 0.6, 0.27, 1.4, "tool-clink", note="down the ladder"),
     c(A, 1.6, "footfall"), c(A, 1.9, "footfall"),
     c("s", 2.2, "footfall"), c("s", 2.4, "ground-thud", vol=0.3, note="sets the package down"),
     c("gf", 2.0, "body-thud", note="trips and falls flat"),
     c("g", 3.2, "ground-thud", vol=0.3, note="sets it out after all"),
     c("f", 2.6, "suit-breathing-hard", loop=True, until=5.6, fade=0.4, note="the visor fogs"))
clip("On the Moon", "lunar-liftoff",
     c("s", 0.6, "pyro", note="the stages part"), c("s", 0.6, "ignition", vol=0.7),
     c("s", 0.6, "engine-burn", loop=True, until=3.2, fade=0.8, note="climbing away"),
     c("gf", 0.6, "engine-sputter", note="it sputters"),
     c("gf", 1.6, "pyro"), c("gf", 1.6, "ignition", vol=0.7),
     c("g", 1.6, "engine-burn", loop=True, until=4.2, fade=0.8),
     c("f", 1.6, "engine-burn", loop=True, until=2.4, fade=0.05), c("f", 2.4, "engine-cutoff", note="it dies low"),
     c("f", 3.7, "crash", note="≈ the ascent stage falls back beside the lander"))
# Landers tested in Earth orbit
clip("Landers tested in Earth orbit", "lander-engine-test",
     c("s", 0.8, "engine-burn", loop=True, until=3.4, fade=0.2),
     c("gf", 0.8, "engine-sputter", note="it sputters"), c("gf", 0.8, "lamp-alarm", loop=True, until=1.6),
     c("g", 1.6, "engine-burn", loop=True, until=3.4, fade=0.2),
     c("f", 1.6, "lamp-alarm", loop=True, vol=1.3, until=3.0, fade=0.2, note="it goes out for good"))
clip("Landers tested in Earth orbit", "lander-staging",
     c("s", 0.8, "pyro"), c("s", 0.8, "engine-burn", loop=True, until=3.0, fade=0.8),
     c("gf", 0.8, "engine-sputter"), c("gf", 0.8, "lamp-alarm", loop=True, until=1.6),
     c("g", 1.6, "pyro"), c("g", 1.6, "engine-burn", loop=True, until=3.6, fade=0.8),
     c("f", 1.6, "engine-burn", loop=True, until=2.8, fade=0.1),
     c("f", 1.8, "metal-strain", note="the stages don't part; the stack tumbles"),
     c("f", 2.8, "lamp-alarm", loop=True, vol=1.3, until=4.0, fade=0.2))
# Probe landers
clip("Probe landers", "probe-release",
     c(A, 0.6, "pyro", vol=0.7, note="the latches (the glitch's and failure's hold)"),
     c("gf", 0.7, "metal-strain", vol=0.6),
     c("g", 1.8, "pyro", vol=0.7, note="the second command lets it go"),
     c("f", 1.8, "pyro", vol=0.7), c("f", 1.9, "metal-strain", vol=0.6, note="it holds again"))
clip("Probe landers", "probe-landing",
     c(A, 0, "wind", loop=True, until=2.2, fade=0.2, note="down through a thick atmosphere under the chute"),
     c("s", 2.2, "ground-thud", note="touchdown"),
     c("gf", 2.2, "ground-thud", vol=1.3, note="hard; the glitch's tips over"),
     c("sg", 2.8, "servo", loop=True, until=3.3, note="the mast extends"),
     every("s", 3.3, 0.75, 6.0, "radio-beep", note="sending"),
     every("g", 3.3, 1.5, 6.0, "radio-beep", vol=0.5, note="sending weakly (Venera 7)"))
clip("Probe landers", "probe-surface",
     every("s", 0.3, 0.75, 6.0, "radio-beep"), every("gf", 0.3, 0.75, 2.19, "radio-beep"),
     every("g", 2.55, 0.75, 6.0, "radio-beep"),
     c("s", 0.4, "servo", loop=True, until=1.6, note="the arm swings out"),
     c("s", 1.8, "ground-thud", vol=0.25, note="the scoop bites the soil"),
     c("s", 2.0, "servo", loop=True, until=2.8, note="and back"),
     c("gf", 0.4, "servo", loop=True, until=1.4),
     c("gf", 1.4, "servo-stall", loop=True, until=2.2, note="stalls (Viking 1's pin)"),
     c("g", 2.2, "servo", loop=True, until=2.5), c("g", 2.6, "ground-thud", vol=0.25),
     c("f", 2.2, "power-down", note="falls silent (Mars 3)"))
# The air launch
clip("The air launch", "air-takeoff",
     c(A, 0, "jet-engines", loop=True, note="the carrier at full power"),
     c(A, 0, "runway-rumble", loop=True, until={"s": 2.0, "g": 3.0, "f": 4.4}, fade=0.3, note="rolling"),
     c("gf", 1.6, "engine-sputter", vol=0.5, note="an engine smokes"),
     c("s", 2.8, "gear-retract"), c("g", 3.8, "gear-retract"),
     c("f", 2.4, "brakes", note="the takeoff abandoned"))
clip("The air launch", "air-cruise",
     c(A, 0, "jet-engines", loop=True, vol=0.6), c(A, 0, "wind", loop=True, vol=0.5),
     c("gf", 1.4, "lamp-alarm", loop=True, until={"g": 2.4, "f": 3.6}, fade=0.2, note="the ALARM lamp"))
clip("The air launch", "air-drop",
     c(A, 0, "jet-engines", loop=True, vol=0.6), c(A, 0, "wind", loop=True, vol=0.5),
     c(A, 0.8, "pyro", vol=0.6, note="let go from the wing"),
     c("s", 1.2, "ignition"), c("s", 1.2, "rocket-roar", loop=True, vol=0.8, until=3.6, fade=1.0, note="off ahead and up"),
     c("g", 2.2, "ignition"), c("g", 2.2, "rocket-roar", loop=True, vol=0.8, until=4.6, fade=1.0),
     c("f", 2.2, "engine-sputter", note="a sputter that dies"))
clip("The air launch", "rocket-climb",
     c("sg", 0, "rocket-roar", loop=True, vol=0.8, carry=True), c("f", 0, "rocket-roar", loop=True, vol=0.8, carry=True, until=2.8, fade=0.1),
     c(A, 0, "wind", loop=True, vol=0.6),
     c("gf", 2.0, "engine-sputter", note="the engine runs rough until 2.8"),
     c("f", 2.8, "engine-cutoff", note="it shuts down for good"))
clip("The air launch", "x15-peak",
     c("gf", 2.0, "airframe-buffet", loop=True, until={"g": 3.2, "f": 6.0}, fade=0.4, note="its nose wanders off"),
     c("f", 3.0, "wind-gust", vol=1.3, note="it departs into a spin"))
clip("The air launch", "runway-landing",
     c(A, 0, "wind", loop=True, until=2.4, fade=0.3, note="gliding in"),
     c(A, 2.4, "ground-thud", note="the skid touches (the glitch's bounces)"),
     c("s", 2.4, "skid-scrape", loop=True, until=4.9, fade=0.6, note="rolling out on the lakebed"),
     c("g", 3.0, "ground-thud"), c("g", 3.0, "skid-scrape", loop=True, until=5.4, fade=0.6),
     c("f", 3.0, "crash", vol=0.6, note="the skid gives way"),
     c("f", 3.0, "skid-scrape", loop=True, vol=1.3, until=3.7, fade=0.3, note="the tail scraping in sparks"))

# --- Checks and the counts --------------------------------------------------------------------------


def sidecar(name, variant):
    return json.load(open(os.path.join(CLIPS, f"{name}-{variant}.json")))


def until_of(cue, variant):
    u = cue.get("until")
    return u.get(variant) if isinstance(u, dict) else u


def expand(cue, variant):
    """The one-shot times (or the loop's start) a cue sounds in a variant."""
    if "every" in cue:
        out, t = [], cue["at"]
        while t <= until_of(cue, variant) + 1e-6:
            out.append(round(t, 2))
            t += cue["every"]
        return out
    return [cue["at"]]


def heard(name, variant, reveal):
    """What a variant sounds before the reveal, as ReplayClipsTest compares it."""
    out = []
    for cue in PLAN[name]:
        if variant not in cue["v"]:
            continue
        for at in expand(cue, variant):
            if at >= reveal:
                continue
            u = until_of(cue, variant) if cue.get("loop") else None
            if u is not None and u >= reveal:
                u = None
            out.append((at, cue["sound"], cue.get("vol"), bool(cue.get("loop")), u, bool(cue.get("carry"))))
    return sorted(out, key=str)


def conflicts(name):
    reveal = min(sidecar(name, v)["revealAt"] for v in "success glitch failure".split())
    sets = {v[0]: set(heard(name, v[0], reveal)) for v in ("success", "glitch", "failure")}
    common = sets["s"] & sets["g"] & sets["f"]
    bad = {}
    for v, s in sets.items():
        for item in s - common:
            bad.setdefault((item[0], item[1]), set()).add(v)
    return reveal, bad


def uses_per_clip():
    """How many times each clip plays across the standard pack's missions (as ReplayClips picks them)."""
    clips = json.load(open(os.path.join(CONTENT, "replay-clips.json"), encoding="utf-8"))
    veh = sorted(clips["vehicles"], key=len, reverse=True)

    def step(sid):
        s = re.sub(r"^\d+-", "", sid)
        for v in veh:
            if s.startswith(v + "-"):
                return s[len(v) + 1:]
        return s

    def is_(s, r):
        return s in (r, r + "-a", r + "-b")

    count = collections.Counter()
    for conf in json.load(open(os.path.join(CONTENT, "standard", "mission-configurations.json"), encoding="utf-8")):
        for season in conf["seasons"]:
            ks = [step(s["id"]) for s in season["steps"]]
            for i, k in enumerate(ks):
                for r in clips["rules"]:
                    if is_(k, r["step"]) and (r.get("after") is None or (i and is_(ks[i - 1], r["after"]))):
                        count[r["clip"]] += 1
                        break
    return count


def fmt_t(x):
    return f"{x:g}"


def main():
    uses = uses_per_clip()
    published = {f[:-len("-success.json")] for f in os.listdir(CLIPS) if f.endswith("-success.json")}
    missing = sorted(published - set(PLAN))
    extra = sorted(set(PLAN) - published)
    unknown = sorted({cue["sound"] for cues in PLAN.values() for cue in cues} - set(LIB))
    if missing or extra or unknown:
        raise SystemExit(f"not planned: {missing}; not published: {extra}; not in the library: {unknown}")

    # Per sound: the clips, the cue count, the plays across the pack.
    by_sound = collections.defaultdict(lambda: {"clips": set(), "cues": 0, "plays": 0})
    for name, cues in PLAN.items():
        for cue in cues:
            e = by_sound[cue["sound"]]
            if name not in e["clips"]:
                e["plays"] += uses[name]
            e["clips"].add(name)
            e["cues"] += 1
    all_conflicts = {}
    for name in PLAN:
        reveal, bad = conflicts(name)
        all_conflicts[name] = (reveal, bad)

    out = ["# Audio cues for the replay's clips", "",
           "Generated by `assets-raw/replay/sounds/audio_cues.py` (don't edit by hand: change the plan there and",
           "rerun). A first pass over every published clip (2026-10-14): what each clip should sound like, per",
           "variant, and the sounds that takes. Nothing here is in `cues.json` yet except where marked; a cue goes",
           "there once its sound has a take in `replay-sounds.json` (with its description below as the brief).",
           "",
           "**How sound works in the replay** (`ui-spec.md` §4.7.3, \"Sound\"): the videos are silent; a clip's",
           "sidecar lists cues `{at, sound, volume, loop, until, fade, carry}`. A one-shot plays once at `at`; a",
           "loop plays from `at` until `until` (or the clip's end), fading out over `fade`; `carry` on a loop at 0",
           "keeps the same sound running on from the clip before. Under every clip run the room tone and the",
           "console's own sounds (telemetry blips, caution tone, master alarm on a failure's reveal): the clips'",
           "cues are only what happens on screen. Space is silent, so these are film sound: engines and",
           "thrusters as heard through the structure, the radio as the craft's beeps home. Loops must be OGG or",
           "WAV (MP3 pads its ends).",
           "",
           "**The spoiler rule** (`ReplayClipsTest`): before the earliest of a clip's three reveals every variant",
           "must sound exactly the same. Sound is stricter than the picture, which may show trouble a moment",
           "before the success's reveal. Cues that break it are marked ⚠ below and listed at the end: as planned",
           "they can't go into `cues.json`; move them to the reveal or later, give every variant the same cue, or",
           "move the reveal.",
           ""]
    n_new = sum(1 for s in LIB.values() if s[1] == "new")
    n_cues = sum(len(c) for c in PLAN.values())
    out += [f"**In all:** {len(LIB)} sounds ({len(LIB) - n_new} in the pool already, {n_new} new), "
            f"{n_cues} cues over {len(PLAN)} clips (several on a shared timeline). **Plays** is how many mission "
            "steps in the standard pack play a clip that uses the sound, in any variant (an explosion only "
            "sounds in failures, an engine's sputter in glitches and failures): the order to find them in.", ""]

    out += ["## The sounds", "",
            "| Sound | Type | Have | Length | Takes | Volume | Clips | Plays | What you hear | Where to look |",
            "|---|---|---|---|---|---|---|---|---|---|"]
    for sid in sorted(LIB, key=lambda k: (-by_sound[k]["plays"], k)):
        kind, status, length, takes, vol, desc, look = LIB[sid]
        e = by_sound[sid]
        out.append(f"| `{sid}` | {kind} | {'yes' if status == 'have' else 'no'} | {length} | {takes} | {vol} | "
                   f"{len(e['clips'])} | {e['plays']} | {desc} | {look} |")
    out += ["",
            "**Type:** a loop is cut to repeat seamlessly and is started and stopped by the cue; a one-shot plays "
            "once. **Takes:** how many versions to find, so a sound heard often doesn't repeat exactly (the pool "
            "deals them without repeats). **Volume:** a starting point for the pool's own volume; a cue's "
            "`vol` below is relative to it.", ""]

    out += ["## The cues, clip by clip", "",
            "Each clip: its length and reveal per variant (s, g, f: success, glitch, failure), how many mission "
            "steps play it, then its cues in time order. **Who** is the variants a cue is in. ⚠ marks a cue "
            "the spoiler rule forbids as planned. \"Every\" repeats a one-shot (the radio rings). ≈ is a time "
            "read off the sheet, to check against the frame. \"In cues.json\" marks the cues already playing.", ""]
    for family, names in FAMILIES.items():
        out += [f"### {family}", ""]
        for name in names:
            sc = {v[0]: sidecar(name, v) for v in ("success", "glitch", "failure")}
            reveal, bad = all_conflicts[name]
            out.append(f"**`{name}`**: s {sc['s']['duration']:g} s (reveal {sc['s']['revealAt']:g}), "
                       f"g {sc['g']['duration']:g} s ({sc['g']['revealAt']:g}), f {sc['f']['duration']:g} s "
                       f"({sc['f']['revealAt']:g}); plays {uses[name]} times. Earliest reveal {reveal:g}.")
            out += ["", "| At | Who | Sound | Type | Until / fade | Vol | What's happening |", "|---|---|---|---|---|---|---|"]
            for cue in sorted(PLAN[name], key=lambda q: (q["at"], q["v"])):
                kind = "loop" if cue.get("loop") else ("every " + fmt_t(cue["every"]) + " s" if "every" in cue else "one-shot")
                u = cue.get("until")
                if isinstance(u, dict):
                    us = ", ".join(f"{k} {fmt_t(x)}" for k, x in u.items())
                else:
                    us = fmt_t(u) if u is not None else ("end" if cue.get("loop") else "")
                if cue.get("fade"):
                    us += f" / {fmt_t(cue['fade'])}"
                if cue.get("carry"):
                    us += " (carried on)"
                if cue.get("carry_out"):
                    us += " (runs on into the next clip)"
                flag = ""
                if any(at == cue["at"] and snd == cue["sound"] for (at, snd) in bad):
                    flag = "⚠ "
                elif "every" in cue and any(snd == cue["sound"] and cue["at"] <= at <= (until_of(cue, "s") or 99)
                                              for (at, snd) in bad):
                    flag = "⚠ "
                out.append(f"| {flag}{fmt_t(cue['at'])} | {cue['v']} | `{cue['sound']}` | {kind} | {us} | "
                           f"{cue.get('vol', '')} | {cue.get('note', '')} |")
            out.append("")

    out += ["## The spoiler conflicts", "",
            "Cues that differ between variants before a clip's earliest reveal: what's heard there would tell the "
            "outcome early. Most are the trouble itself starting a moment before the success's reveal (the "
            "picture shows it there too, which the sheets allow). For each: sound it at the reveal or after, "
            "give every variant the same cue (an engine cough the success also has), or move the reveal.", "",
            "| Clip | Earliest reveal | At | Sound | Only in |", "|---|---|---|---|---|"]
    n_bad = 0
    for name in sorted(all_conflicts):
        reveal, bad = all_conflicts[name]
        for (at, snd), vs in sorted(bad.items()):
            n_bad += 1
            out.append(f"| `{name}` | {reveal:g} | {at:g} | `{snd}` | {''.join(sorted(vs, key='sgf'.index))} |")
    out += ["", f"{n_bad} conflicts in {sum(1 for r, b in all_conflicts.values() if b)} clips.", ""]

    path = os.path.join(HERE, "AUDIO_CUES.md")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out))
    print(path, f"{len(LIB)} sounds, {n_cues} cues, {n_bad} conflicts")


if __name__ == "__main__":
    main()
