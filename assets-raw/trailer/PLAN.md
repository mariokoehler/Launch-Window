# Launch Window: the trailer (plan)

A teaser of about 90 seconds (a ballpark, not a target) to open the game's kick-off session before
the first live game in January 2027. It is live action rendered by AI: no game screens. It tells the
space race as it happened, from the two announcements of 1955 to the first step on the Moon, and cuts
to the logo a moment before the boot touches the ground.

Decisions so far (user, 2026-11-21):
- **History as it happened.** No invented events and no improved arc: the real race is the story.
  Every depicted fact is checked and sourced in `FACTS.md` before its shot is final, as for the
  game's history notes. Small liberties that make the story easier to follow are fine (user,
  2026-11-21): Sedov's answer of 2 August 1955 is set in Moscow, not Copenhagen, so the cold open
  doesn't jump between three cities.
- **A narrator, at least for the context** (user, 2026-11-21, reversing "no narrator"): short lines
  in the shot list's Narration column, a generated voice in our own pipeline (as the replay's radio
  call-outs, `assets-raw/audio/chatter/`), never an imitation of a real person. Plus a music score and
  captions. The sound MiniMax H3 generates with each clip stays where it fits; we decide per shot
  once we have heard it.
- **Captions in the edit** (§1), **no real faces** (§2), and the satellite beat built on the embassy
  reception of 4 October 1957 rather than an interrupted American countdown (§6: the reception is
  what happened; no American launch was imminent that night). Confirmed by the user, 2026-11-21.
- **The whole film is scripted.** One script file describes every shot; tools in this folder generate
  keyframes and clips from it, cut the film in FilmCraft and export it. Draft quality while iterating,
  final quality in one overnight run.

## 1. Tools and models

| Job | Tool | Notes |
|---|---|---|
| Keyframes (stills) | Qwen Image 2.1 in ComfyUI (installed) | Always at exactly 1344×768, H3's native size: an off-aspect first frame is stretched, a last frame centre-cropped. 1344×768 is 1.75:1, a little taller than 16:9: the edit fills the 1920×1080 frame (`clip.fillFrame`), cropping about 1% top and bottom, so keyframes keep a small safe margin there. |
| Clips | MiniMax H3 in ComfyUI | `fl2va` installed. Reference-to-video and Multiframe need the `ref2va` weights from `Comfy-Org/MiniMax-H3` (to download). |
| Upscale to 1080p | FilmCraft's scaling for now | H3 runs at about 1 MP locally; 2K is only the hosted service. A separate upscale pass only if the projector shows the need. |
| Score | MiniMax Music 3 in ComfyUI (installed), or a curated track | Decided after the rough cut. |
| Edit, captions, end card, export | FilmCraft (`C:\Program Files\FilmCraft\filmcraft-cli.exe`) | Headless, tested 2026-11-21 with dummy clips: `run <script.jsonl>` with `--save-as`/`--project … --save`; `file.newSequence` (1920×1080, 24 fps), `file.import` (returns item ids), `timeline.place` (item, track, time, `sourceIn`, `duration`; returns clip ids), `clip.fillFrame`, `timeline.setTrack` (mute), `graphics.newText` (a caption as its own clip), `file.exportMedia` (h264). `render --seconds S --out f.png` checks a frame. |

Fallback model: **LTX-2.5** (4K HDR, several shots in one generation, first and last frame; gated
weights, another stack). Only if H3 disappoints in the test shot.

### Which H3 mode for which shot

| Shot | Mode |
|---|---|
| Most shots: a mood, a hand, a face turned away | Image-to-video, **first frame only**. Pinning a last frame on a 2–4 s shot often bends the motion. |
| The end state is the point (rocket on the pad, then off it) | Image-to-video, **first and last frame**. |
| Exact timing (the boot, a cut on a beat) | **Multiframe**: stills pinned at frame indexes (`round(seconds × 24)`). |
| A person seen in several shots | **Reference-to-video** with a character sheet (up to 9 reference images, `<Picture N>` in the prompt). |

Prompting (from Comfy's H3 prompt guide): English; the scene first, then `[Shot 1] …`, later shots
`[Shot 2] At 00:02.500, the camera cuts to …` (one time, increasing); `overall_soundscape:` for
ambience and effects, `non_diegetic_music:` for score (we set it to `N/A`: the score comes in the edit);
**no negative prompts** (naming a thing brings it in: say what should be there instead). Length in
frames at 24 fps, on the 17k+5 grid (124 frames ≈ 5 s). We generate about 5 s per shot and trim.

### Captions: in the edit

Captions ("Copenhagen, 1955", "4 October 1957") are FilmCraft text graphics, not generated text:
- They stay crisp and steady; generated lettering wobbles from frame to frame.
- A changed date or a typo costs a re-export, not a re-render.
- One typeface and placement for the whole film, the game's own (from the client's fonts).

Text that belongs to the world (a newspaper headline, the teletype's paper, a countdown clock) is
generated in the keyframe by Qwen, quoted verbatim in the prompt, and checked by eye as with the front
pages. If H3 garbles it in motion, the shot stays short or nearly still, or the text is kept out of
focus.

The end card is the game's logo; the splash clip (`assets-raw/splash/`) may serve as its animation.

## 2. Things that stay out of the generator

- **Real voices.** "That's one small step for [a] man…" is NASA's own Apollo 11 recording (public
  domain as a work of the US government: confirm and credit in `CREDITS.md` here). Never an AI voice of
  Armstrong or anyone real. (Sputnik's beep, by contrast, is our own synthesized sound: §7.)
- **Real faces.** Gagarin, Korolev, Armstrong and the rest appear behind visors, in silhouette, from
  behind or as hands. That is also the more cinematic choice.
- **BASPM.** Nothing from it, as everywhere in the repo (`design.md` §1.2). The trailer needs none.

## 3. The script: `shots.json`

The film's single source. The generators read it and so does the edit. Sketch of one entry:

```json
{
  "id": "s02-embassy-toast",
  "beat": "satellite",
  "side": "us",
  "look": "us-1957",
  "duration": 3.0,
  "trim": [0.8, 3.8],
  "mode": "i2v",
  "keyframes": {
    "first": "A crowded embassy reception in Washington at night, 1957 ...",
    "last": null
  },
  "references": [],
  "video": "[Shot 1] A man in a dark suit leans to whisper into a scientist's ear; the scientist lowers his champagne glass ...",
  "soundscape": "Murmur of a crowded reception, glasses clinking, then a hush.",
  "keepAudio": "decide",
  "caption": {"text": "Washington, 4 October 1957", "at": 0.3, "for": 2.5},
  "facts": ["embassy-reception-1957"]
}
```

- `look` names a shared style block in `looks.json` (film stock, grain, colour, lens): for example the
  American side warm Kodachrome, the Soviet side cooler newsreel grain, the Moon clean and stark.
  The look is appended to every keyframe prompt, as the blueprints' style block is.
- `characters.json` holds the recurring people (the American flight director, the Soviet chief
  designer, the two pad engineers): a description each, and a character sheet generated once and
  picked, fed to reference-to-video.
- `duration` and `trim` drive the edit; the order of entries is the order of the film.
- `audio.json` lists the score, the NASA recording and any effect, with their in points on the
  timeline (and beat markers once the score exists, so cuts can land on them).

## 4. The tools (in this folder, after the portraits' and blueprints' pattern)

| Script | Does |
|---|---|
| `make_keyframes.py` | Qwen keyframes for every shot (first and last frames, character sheets): fixed seeds, 3 variants, review sheets in `review/`, the pick per frame in `picks.json`. `--only <shot>`. |
| `make_clips.py` | H3 clips from the picked keyframes and the shot's prompt. `--profile draft` or `final`, `--variants N`, `--only <shot>`, `--beat <beat>`. `--queue` queues the takes in ComfyUI and exits; `--collect` fetches what is finished (`clips/pending.json` lists what is out). Takes are queued in rounds (every shot's first take, then every second), so a shot whose first take is good can drop its second with `--only <shot> --skip` before it starts (user); `--takes N` caps the new takes per shot. Writes `clips/<profile>/<shot>_<n>.mp4` and `manifest.json` (prompt, seed, profile, render time). The pick per shot goes in `picks.json`. Contact sheets (a few frames per variant) in `review/`. |
| `make_edit.py` | Writes a FilmCraft JSONL from `shots.json`, `audio.json` and the picks: a 1920×1080 24 fps sequence, each picked clip trimmed and placed, its own sound on a track muted unless `keepAudio`, captions as text graphics, score and NASA audio, the end card. Runs it headless, saves `build/trailer.fcproj` and exports `build/trailer-<profile>.mp4`. The same script for draft and final: only the clip folder changes. |
| `make_score.py` | Only if the score is generated (Music 3): the prompt and length from the rough cut. |

ComfyUI workflows are the user's, built and tested in the GUI, exported in API format and flattened
here like `blueprints_workflow_api.json` (Qwen keyframe; H3 image-to-video; H3 reference-to-video;
H3 multiframe).

**Kept in git:** the scripts, the JSON files, `FACTS.md`, `CREDITS.md` and the picked keyframes
(`git add -f`, as for the patches). **Not kept in git:** clips (large, and regenerable: that is the
point of the overnight run), review sheets and `build/`. ComfyUI keeps its own copy of everything
(`lw_trailer_*` in its output folder; never delete those). The final export lives outside the repo.

### Draft and final: fresh finals, picked in the morning

A seed does not give the same clip at another resolution or step count, so a draft we like does not
come back the same from a final render. Refining the approved draft instead (H3 has no video-to-video
node here, so the draft was anchored as a guide through the multiframe workflow) was tried on
2026-11-21 and gave terrible quality (user): dropped.

So drafts settle each shot's keyframe, prompt, framing and timing; the overnight run renders 2–3 final
takes per shot from them, and we pick again in the morning. The keyframes hold composition and look,
since they are full resolution from the start. A shot whose draft sound was the point can take its
sound from the draft take in the edit.

Draft profile: 672×384, the Lightning LoRA (8 steps; reference-to-video runs at full steps), 2 takes.
Final: 1344×768, 25 steps, no turbo; FilmCraft scales it to 1920×1080 (a separate upscale pass only if
the projector shows the need). About 10 minutes a 5 s take: 25 shots × 2 takes fit in one night.

## 5. Phases

0. **Set up.** Download `ref2va`. The user builds and exports the workflows; we flatten them.
1. **The test shot.** The satellite beat's turn: the embassy reception, then Sputnik in orbit.
   It proves the looks, the prompt shape, the H3 sound, the draft-to-final path (§4) and the render
   times. Also one shot with world text (a teletype) to see how H3 holds it.
2. **The script.** `shots.json` for every beat, written out shot by shot; `FACTS.md` checked.
3. **The storyboard.** A Claude Design canvas (`Launch Window — Trailer`): one picked keyframe per
   shot in order, with its duration, mode and caption. Iterate there until the sequence reads.
4. **Drafts and the rough cut.** Draft clips beat by beat; `make_edit.py --profile draft` after
   each, so the film is always watchable end to end.
5. **The score.** Against the rough cut; then cuts tuned to its beats.
6. **The overnight run.** `make_clips.py --profile final --queue` (queues every take and exits), in the
   morning `make_clips.py --collect`, then `make_edit.py --profile final`.
7. **Polish.** Captions, the end card, the sound mix, a check on the meeting room's projector and
   speakers.

## 6. The film (first pass; detailed shot by shot in phase 2)

| Beat | About | What happens (history to be confirmed in `FACTS.md`) |
|---|---|---|
| Cold open: the announcements | 15 s | Summer 1955. Washington: a press room, flashbulbs, the words "earth-circling satellite". A teletype in Moscow clatters the news; a man tears it off. Copenhagen, days later: a Soviet scientist (Sedov) tells reporters the USSR will launch one too. A teletype in an American office; the clipping pinned over a chalk orbit. The Soviet engineer pins the American clipping on his own board, the same gesture. Two workshops' lights come on at night. |
| Satellite | 15 s | The Vanguard team at work. 4 October 1957, a reception at the Soviet embassy in Washington: a whisper, a toast that goes awkward. Hard cut: Sputnik in orbit, beeping. Rooftop listeners with a radio held to the sky. December: Vanguard explodes on its pad on live television. |
| Human in space and in orbit | 12 s | 12 April 1961: Gagarin takes both at once, one orbit in Vostok 1; a pilot's visor reflects the Earth. The American capsule is still on the ground. |
| The US catches up | 10 s | May 1961, Shepard's suborbital hop: fifteen minutes, up and down. February 1962, Glenn's capsule over the Pacific: the first American in orbit, ten months behind. |
| The middle years | 10 s | Faster cuts: the first spacewalk (Leonov, 1965), Gemini's rendezvous and docking, a test stand fire, smoky control rooms. A loss here, with care (the game has In memory). |
| Lunar flyby | 10 s | December 1968, Apollo 8: the Earth rising over the Moon's horizon. A quiet beat. |
| Landing | 15 s | July 1969: the lander's descent in dust; a slow push in on the ladder and the boot. "That's one small step for [a] man…" The boot moves forward; a moment before it touches, cut to black and the logo, the tagline "Stay one small step ahead." |

The film has about 20–25 shots. With 3–4 variants per shot that is 80–100 drafts.

## 7. Findings from the test shot (2026-11-21)

- **Speed** (RTX 4090): a keyframe about 8 s; a 5 s draft clip (672×384, Lightning) about 40 s once the
  model is loaded (90–135 s while models swap, 9 min cold); a 5 s final (1344×768, 25 steps) about
  10 min. About 25 shots × 2 final takes fit in one night.
- **The look works** (user): warm 1957 Kodachrome for the American side, cooler grain for Moscow.
- **People and rooms** come out well from a Qwen keyframe and first-frame image-to-video (the whisper).
  H3 overplays crowd emotion: an "awkward toast" became a cheering party facing the camera until the
  prompt kept everyone's back to the camera and said what the mood was.
- **Spacecraft:** image models only half know what Sputnik and its successors looked like (wrong
  antenna counts and places). The user's mission configuration art (`assets-raw/Mission
  Configuration/`) gets them right: as **the first frame** (kept exactly, little motion) or as a
  **reference-to-video reference** (`<Picture 1>` with a retention line "no more and no fewer"
  antennas; new framing and real camera motion). The user's pick was the reference take. Reference-to-
  video runs at full steps even in draft (`"turbo": false`): the 4-step turbo holds references weakly.
- **Groups need individual actions** (user, 2026-11-21): a group given one shared action ("the reporters
  scribble and lean in") mostly stands and stares into the middle distance. Give at least two or three people
  their own action, each placed in the frame ("the reporter in the grey fedora on the left taps his pencil
  on his notebook, then writes"). Keep the actions to hands and bodies: a reporter told to lick his pencil
  did strange things with his tongue (user). Fewer, larger people also keep faces from turning uncanny: a face 20–30 px wide is a patch or
  two in the model's compressed grid and shimmers from frame to frame (the c02a-press-room-small trial): its faces came out
  clearly better (user), though the background stayed in focus despite "shallow depth of field" and its faces
  still turn odd at times. Next try: a longer lens (85mm, wide open) and the background people described as
  blurred shapes.
- **The video prompt describes the picked keyframe, not the keyframe prompt:** the model rarely draws exactly
  what was asked (two Soviets became one; an empty-handed man got a glass). After picking a first frame, rewrite
  the video prompt for who is actually in it and what they hold (user).
- **A light lit in the first frame stays lit:** a flashbulb firing in the keyframe burned for 3 s in the clip.
  Keyframes show flashes dark; the video prompt makes each "a single blink of white light that is gone in a
  split second" (measured: 2–4 frames).
- **Naming what you don't want brings it in** (as Comfy's guide warns): describe the wanted state
  ("a single Earth fills the lower part of the frame") instead.
- **Sound:** usable for rooms and machines, under a score (user). Space scenes come out near silent
  (peaks −30 to −45 dB) whatever the prompt says, and "the silence of space" makes it a hum. The
  Sputnik beep goes in at the edit (synthesized, below).
- **World text** holds while the paper is still; newly printed lines become garble. Keep it short and
  cut before new text appears.
- **Soviet text is in Russian** (user, 2026-11-21): the Moscow teleprinter prints Cyrillic (for
  example "США ЗАПУСТЯТ ИСКУССТВЕННЫЙ СПУТНИК ЗЕМЛИ"), which nobody in the audience reads, so garble
  doesn't matter; an English subtitle in the edit translates it ("U.S. to launch an artificial Earth
  satellite"). The same goes for any Soviet sign, paper or panel. `make_edit.py` needs a subtitle
  style (centred, lower third) beside the caption.
- **The Sputnik beep is synthesized** (user, 2026-11-21): our own plausible beep, no recording needed.
  The placeholder in `make_edit.py` becomes the real one once it sounds right (a radio-filtered tone,
  short pulses, static under it).

- **Memory** (2026-11-21): one final H3 take needs about 40 GB of weights (the 21 GB `fl2va` model,
  the 15.7 GB Qwen3-VL 32B text encoder, 3.4 GB of VAEs) on a 24 GB card with 32 GB of RAM. ComfyUI
  keeps what doesn't fit in VRAM in RAM, and keeps models cached after use (Qwen Image after
  keyframes, `ref2va` after `fl2va`). Its process held 48 GB committed, and Windows' commit limit was
  67 of 70.5 GB used. Resolution and steps barely change this: weights dominate. ComfyUI never failed;
  Claude Code's low-memory reaper killed the script waiting on it, three times. So the renders are
  queued and the script exits (`--queue`, then `--collect`), and the scripts ask ComfyUI to unload its
  models (`/free`) before they queue. Also suggested to the user: a larger page file, ComfyUI's
  `--cache-ram 8` (keep 8 GB free instead of about 3).
- **A smaller H3 model** (2026-11-21): the community mixed INT4/INT8 `fl2va`
  (`Abiray/Minimax-H3-nvfp4-INT4-INT8-Convrot` on Hugging Face, 15.9 GB against 21 GB) rendered c03 and c06 at
  final settings with final's seeds (`--profile final-mixed`): the same detail and legible text, slightly
  different motion, about the same time (9–11 min). The NVFP4 builds need an RTX 50 card; GGUF saves little.

## 8. Open points

- The edit works (2026-11-21): `make_edit.py` cut the four test shots into a 19 s rough cut with
  captions faded in and out, fades from and to black, the clips' own sound and a placeholder beep
  at −6 dB, exported in 13 s. Still untried: cuts between shots other than hard cuts, and the score.
- Whether finals need an upscale pass beyond FilmCraft's scaling (check on the projector).
- Score: Music 3 or a curated track (credited either way).
- Whether a beat for the Soviet N1 or the lunar race's end (the Soviets standing down) belongs in the
  film, or the landing alone closes it.
