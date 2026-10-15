# Radio call-outs: ideas for the replay

The takes in this folder are the user's (2026-10-14): generated in ComfyUI with VibeVoice, brownian
noise under them for the radio, the Quindar tones at both ends. Each file's name is exactly what's said.
Several fit the same moment, so a moment's lines can be one sound's takes in `replay-sounds.json` (dealt
with no repeats, `ui-spec.md` §4.7.3).

**Status (2026-10-15):** every line below has takes except "Flight, stand by one.", which VibeVoice
won't say (eight seeds, never the "one"); Claude made them with the user's workflow
(`make_callouts.py`, `callouts.json`), screened them with `check_callouts.py`, and the user kept the
best. Where they play: `make_radio_sounds.py`'s descriptions and `assets-raw/replay/cues.json`.

Below: the lines, mostly for glitches and failures. They follow how the real flight loop
sounded (`design.md` §1.1, "The flight loop in trouble").

## How the real loop sounds when things go wrong

- **Calm, not dramatic.** Controllers and crews were trained to report in a flat, clipped voice, and the
  calmer the call, the worse it usually was. Apollo 13's "we've had a problem" was said almost casually.
  For VibeVoice: the same calm reference voice as the routine calls, maybe one notch tenser. Shouting and
  "Oh no!" sound like a film, not like Houston.
- **Who's calling whom:** "Flight, Booster", "Flight, FIDO". The caller says the person they want first,
  then their own console. Answers are short: "Go", "Copy", "Roger", "Stand by".
- **Facts, not feelings:** what the data says ("pressure dropping", "no data", "we've lost the signal"),
  then a decision ("we're go", "abort", "stand by").
- **The silence is part of it.** A pause, "Stand by", and nothing for a few seconds is the most tense
  thing the loop does.

## Lines for any outcome (they spoil nothing)

These fit every variant, so they can play *before* the reveal, where the spoiler rule (`ReplayClipsTest`)
only allows sound that's the same in all three. The outcome lines below must start at the reveal or later.

- Stand by.
- Flight, stand by one.
- Copy, standing by.
- Watching it.
- We're looking at it.
- Roger, copy.
- Data coming in now.

## Glitch: something goes wrong, then it's saved

Something's off, a call, then the fix and a "go". The arc fits the glitch variants and the saving throw.

**Launch and ascent**
- Flight, Booster. We have an engine out. Remaining engines are compensating.
- Inboard engine out. We're still go.
- We're seeing some oscillations. Within limits.
- Trajectory is a little low. Guidance is correcting.
- Flight, Booster. Second stage ignition late, but it's good thrust now.
- Hold, hold, hold. … Recycling. Counting again.

**In orbit, the craft and the probes**
- Flight, we've lost telemetry. … We have it back. Signal is good.
- Loss of signal. … Acquisition of signal. Data looks good.
- Flight, EECOM. Main bus undervolt. … Switching to backup. Voltage is coming back.
- We have a program alarm. … We're go on that alarm.
- Attitude control is drifting. … Switching to backup. Rates are damping.
- Separation indicator did not light. … Confirmed by telemetry. We have separation.
- Flight, the burn was short. We can trim it on the next pass.

**Coming home**
- We're in blackout. … Expect acquisition in about two minutes.
- Drogue is late. … Drogue deployed. Main chute is out.
- One chute is not fully inflated. Two good mains. Landing should be fine.
- Recovery reports a hard landing. Crew is okay.

**Rendezvous, docking, spacewalks, the Moon**
- Closing rate is high. Recommend you brake. … Rates look good now.
- Capture latches did not engage. Back off and try again. … Hard dock.
- He's having trouble getting back in. … Hatch is closed. He's in.
- Sixty seconds. … Thirty seconds. … Contact light. (Apollo 11's fuel calls as Armstrong searched for a
  clear spot.)

## Failure: nothing saves it

Short and flat. Many real failures were announced by a commentator, not a controller (the PAO, "Public
Affairs"), with the same calm.

**On the pad, launch and ascent**
- Hold, hold, hold. We have a cutoff.
- Engine shutdown. Vehicle is safe on the pad.
- Flight, the vehicle is off trajectory.
- We've lost thrust. Range safety is standing by.
- Range safety has initiated destruct.
- Obviously a major malfunction. (Challenger, 1986: too close to a real loss of a crew to quote outright;
  reword, e.g. "We have a major malfunction.")
- Abort. Abort. Abort.
- The launch escape system has fired. Crew capsule is clear.
- We have no downlink.

**In orbit, the craft and the probes**
- Flight, we've lost all data.
- No signal. We've lost the spacecraft.
- Loss of signal. … Still no signal. … No acquisition.
- Flight, the burn did not take place.
- Separation has failed. The payload is still attached.
- Flight, we're tumbling. We cannot stop it. (Gemini 8, 1966, was "We have serious problems here.
  We're tumbling end over end up here.")
- Pressure is dropping fast.
- The probe has stopped transmitting.

**Coming home**
- We're past the expected acquisition time. Still no signal.
- Main chute did not deploy.
- Recovery has no contact.
- Flight, the capsule is sinking. (Liberty Bell 7, 1961: the hatch blew, Grissom swam clear, the capsule
  went down.)

**Rendezvous, docking, spacewalks, the Moon**
- Abort the docking. Back away.
- Flight, we have a collision.
- Abort the landing. Abort.
- We've lost contact with the lander.

## Real calls, for flavour

As usually quoted; check before a history note uses one (`design.md` §1.1).

- "Houston, we've had a problem." Apollo 13, 1970. Apollo's most famous "glitch" call, and it ended in
  survival: a fine line for a saved step if it's reworded ("Flight, we've had a problem here.").
- "Try SCE to AUX." Apollo 12, 1969: lightning struck the Saturn V twice, the telemetry went to garbage,
  and EECOM John Aaron recognized the pattern and asked for a switch almost nobody knew. It saved the
  mission. A perfect saved-glitch line, and a real one nobody owns.
- "1202 … we're go on that alarm." Apollo 11, 1969 (already in §1.1 as a saving throw).
