# Launch Window: the trailer (plan)

A teaser of about 90 seconds (a ballpark, not a target) to open the game's kick-off session before
the first live game in January 2027. It is live action rendered by AI: no game screens. It tells the
space race as it happened, from the two announcements of 1955 to the first step on the Moon, and cuts
to the logo a moment before the boot touches the ground.

Decisions so far (user, 2026-11-21):
- **History as it happened.** No invented events and no improved arc: the real race is the story.
  Every depicted fact is checked and sourced in `FACTS.md` before its shot is final, as for the
  game's history notes.
- **No narrator.** Pictures, a music score and captions (place and date). The sound MiniMax H3
  generates with each clip stays where it fits; we decide per shot once we have heard it.
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
| Upscale to 1080p | to be chosen in the test (§5, phase 1) | H3 runs at about 1 MP locally; 2K is only the hosted service. |
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
  Armstrong or anyone real.
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
| `make_clips.py` | H3 clips from the picked keyframes and the shot's prompt. `--profile draft` or `final`, `--variants N`, `--only <shot>`, `--beat <beat>`. Writes `clips/<profile>/<shot>_<n>.mp4` and `manifest.json` (prompt, seed, profile, render time). The pick per shot goes in `picks.json`. Contact sheets (a few frames per variant) in `review/`. |
| `make_edit.py` | Writes a FilmCraft JSONL from `shots.json`, `audio.json` and the picks: a 1920×1080 24 fps sequence, each picked clip trimmed and placed, its own sound on a track muted unless `keepAudio`, captions as text graphics, score and NASA audio, the end card. Runs it headless, saves `build/trailer.fcproj` and exports `build/trailer-<profile>.mp4`. The same script for draft and final: only the clip folder changes. |
| `make_score.py` | Only if the score is generated (Music 3): the prompt and length from the rough cut. |

ComfyUI workflows are the user's, built and tested in the GUI, exported in API format and flattened
here like `blueprints_workflow_api.json` (Qwen keyframe; H3 image-to-video; H3 reference-to-video;
H3 multiframe; the final refine and upscale).

**Kept in git:** the scripts, the JSON files, `FACTS.md`, `CREDITS.md` and the picked keyframes
(`git add -f`, as for the patches). **Not kept in git:** clips (large, and regenerable: that is the
point of the overnight run), review sheets and `build/`. ComfyUI keeps its own copy of everything
(`lw_trailer_*` in its output folder; never delete those). The final export lives outside the repo.

### Draft and final: the open question

A seed does not give the same clip at another resolution or step count. So a draft we like at low
resolution would *not* come back the same from a fresh high-resolution render. Two ways to handle it,
to be settled in the test shot:

1. **Refine the approved draft** (preferred if it works): the final pass takes the picked draft
   clip, upscales it and re-noises it partly (a video "hires fix"), keeping its motion and timing and
   adding detail. Then `final` means "the same takes, sharper". The refine would re-roll the sound, so
   a shot's kept sound comes from its draft take (`make_edit.py` reads the audio from the draft clip
   and the picture from the final).
2. **Fresh finals, picked in the morning:** the overnight run renders 3–4 final variants per shot from
   the approved keyframes and prompt; we pick again. The keyframes hold composition and look either
   way, since they are full resolution from the start.

Draft profile (to tune): 672×384, the Lightning LoRA (8 steps), 2–3 variants. Final: 1344×768,
25 steps, no turbo, then the upscale to 1920×1080. The test shot measures the times, which size the
overnight run (about 25 shots).

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
6. **The overnight run.** `make_clips.py --profile final`, then `make_edit.py --profile final`.
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

## 7. Open points

- The edit was only tested with dummy clips: phase 1's test shot also goes through `make_edit.py`
  (transitions, the sound mix and fades still untried). The fallback is EffectCraft, proven headless
  in this repo.
- The draft-to-final path (§4): settled in the test shot.
- The upscaler for 1080p: chosen in the test shot.
- Score: Music 3 or a curated track (credited either way).
- Whether a beat for the Soviet N1 or the lunar race's end (the Soviets standing down) belongs in the
  film, or the landing alone closes it.
