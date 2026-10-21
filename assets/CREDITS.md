# Credits for third-party assets

Assets in this folder that weren't made for Launch Window, with the credit their licence asks for.

## Fonts

Archivo and Work Sans, under the SIL Open Font License; see `fonts/OFL-Archivo.txt` and
`fonts/OFL-WorkSans.txt`.

Old Standard TT (Bold) and Libre Caslon Text (Regular and Italic, static instances at weight 400
cut from the variable fonts), for the Firsts' front pages, under the SIL Open Font License; see
`fonts/OFL-OldStandard.txt` and `fonts/OFL-LibreCaslonText.txt`.

## Audio

- `audio/small-step.mp3`: Neil Armstrong on the Moon, 20 July 1969, "That's one small step for
  (a) man, one giant leap for mankind". NASA, https://www.nasa.gov/historical-sounds/. Public
  domain (NASA). Plays with the splash.
- `audio/moon-speech.mp3`: President John F. Kennedy at Rice University, Houston, 12 September
  1962, "We choose to go to the Moon". NASA, https://www.nasa.gov/historical-sounds/. Public domain.
  Plays when the Join screen opens.
- `audio/replay/`: the replay's sounds (`content/replay-sounds.json`). Sounds made for Launch Window
  need no credit here; any that weren't are credited below, one bullet per file, and marked
  `"credit": true` in the catalog. Generated music names its model and the model's terms here.
- `audio/replay/mission-control-room.ogg`: mission control's room tone, a 4-minute loop mixed for
  Launch Window by its author from several NASA recordings of mission radio chatter. NASA,
  public domain.
- `audio/replay/telemetry-blip-1.ogg`, `audio/replay/telemetry-blip-2.ogg`,
  `audio/replay/telemetry-blip-3.ogg`: made for Launch Window by its author (2026-10-18;
  `assets-raw/audio/telemetry_beep*.ogg`).
- `audio/replay/caution-tone.ogg`: "Warning Sound" by freesound_community, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-warning-sound-6686/. Pixabay Content
  License, https://pixabay.com/service/license-summary/. The first of its two sweeps, level raised
  11 dB.
- `audio/replay/master-alarm.ogg`: "High Pitch Alarm Buzzer" by flutie8211, Pixabay,
  https://pixabay.com/sound-effects/high-pitch-alarm-buzzer-464349/. Pixabay Content License,
  https://pixabay.com/service/license-summary/. Level raised 10 dB.
- `audio/replay/no-signal-static.ogg`: "TV Static" by freesound_community, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-tv-static-7019/. Pixabay Content License,
  https://pixabay.com/service/license-summary/. Played forwards then backwards as one loop, level
  raised 12 dB.
- `audio/replay/hold-click-1.ogg`, `audio/replay/hold-click-2.ogg`: "Button Click Vintage Sound FX"
  by thepixelguymaker, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-button-click-vintage-sound-fx-541135/. Pixabay
  Content License, https://pixabay.com/service/license-summary/. Split into its press and its
  release, each level raised to a peak of about -1 dBFS.
- `audio/replay/rocket-roar.ogg`: "Rocket Launch SFX" by JCI21, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-rocket-launch-sfx-253937/. Pixabay Content
  License, https://pixabay.com/service/license-summary/. Level raised 10 dB.
- `audio/replay/rcs-puff.ogg`: "Inhaler Puff" by freesound_community, Pixabay,
  https://pixabay.com/sound-effects/household-inhaler-puff-37313/. Pixabay Content License,
  https://pixabay.com/service/license-summary/. Level raised 2 dB.
- `audio/replay/hatch-open.ogg`: "Inhaler Puff 170427_1464" by freesound_community, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-inhaler-puff-170427-1464-101773/. Pixabay
  Content License, https://pixabay.com/service/license-summary/. Level raised 5.5 dB.
- `audio/replay/fueling.ogg`: "Sample Pump Sound" by samplefile_download, Pixabay,
  https://pixabay.com/sound-effects/technology-sample-pump-sound-251173/. Pixabay Content License,
  https://pixabay.com/service/license-summary/. Level raised 7 dB.
- `audio/replay/vent.ogg`: "Air Hiss Tubes Loop" by freesound_community, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-air-hiss-tubes-loop-96446/. Pixabay Content
  License, https://pixabay.com/service/license-summary/. Level raised 10 dB.
- `audio/replay/reentry-plasma.ogg`: "Air Rush Longer" by freesound_community, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-air-rush-longer-106990/. Pixabay Content
  License, https://pixabay.com/service/license-summary/. Level raised 2 dB.
- `audio/replay/splash.ogg`: "Water Splash" by Universfield, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-water-splash-199583/. Pixabay Content License,
  https://pixabay.com/service/license-summary/. Level raised 7 dB.
- `audio/replay/helicopter.ogg`: "Helicopter Hovering (Distant)" by freesound_community, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-helicopter-hovering-distant-23791/. Pixabay
  Content License, https://pixabay.com/service/license-summary/. Level raised 4 dB.
- `audio/replay/explosion.ogg`: "My First Recorded Explosion" by freesound_community, Pixabay,
  https://pixabay.com/sound-effects/film-special-effects-my-first-recorded-explosion-103816/. Pixabay
  Content License, https://pixabay.com/service/license-summary/.
- `audio/replay/decoupler.ogg`, `audio/replay/explosion-debris.ogg`, `audio/replay/servo.ogg`,
  `audio/replay/deploy.ogg`: made for Launch Window by its author with Stable Audio 3 (Stability AI; the
  ComfyUI repackaging, https://huggingface.co/Comfy-Org/stable-audio-3/), under the same licence as the
  fanfares below. Levels raised (9, 1.5, 0 and 6 dB).
- `audio/replay/fanfare-success.mp3`, `audio/replay/fanfare-failure.mp3`: made for Launch Window by its
  author with Stable Audio 3 (Stability AI; the ComfyUI repackaging,
  https://huggingface.co/Comfy-Org/stable-audio-3/). The model is under the Stability AI Community
  License (https://huggingface.co/stabilityai/stable-audio-open-1.0/blob/main/LICENSE.md), which
  leaves the outputs to whoever generates them and allows non-commercial use free of charge; its
  attribution terms cover distributing the model, not its outputs.
- `audio/replay/radio-abort-docking-1.mp3`, `audio/replay/radio-abort-docking-2.mp3`,
  `audio/replay/radio-abort-docking-3.mp3`, `audio/replay/radio-abort-docking-4.mp3`,
  `audio/replay/radio-abort-docking-5.mp3`, `audio/replay/radio-abort-docking-6.mp3`,
  `audio/replay/radio-abort-docking-7.mp3`, `audio/replay/radio-abort-landing-1.mp3`,
  `audio/replay/radio-abort-landing-2.mp3`, `audio/replay/radio-abort-landing-3.mp3`,
  `audio/replay/radio-abort-landing-4.mp3`, `audio/replay/radio-abort-landing-5.mp3`,
  `audio/replay/radio-abort-landing-6.mp3`, `audio/replay/radio-abort-landing-7.mp3`,
  `audio/replay/radio-abort-landing-8.mp3`, `audio/replay/radio-approach-good-1.mp3`,
  `audio/replay/radio-approach-good-2.mp3`, `audio/replay/radio-approach-good-3.mp3`,
  `audio/replay/radio-approach-good-4.mp3`, `audio/replay/radio-approach-good-5.mp3`,
  `audio/replay/radio-approach-good-6.mp3`, `audio/replay/radio-attitude-drift-1.mp3`,
  `audio/replay/radio-attitude-drift-2.mp3`, `audio/replay/radio-attitude-drift-3.mp3`,
  `audio/replay/radio-attitude-drift-4.mp3`, `audio/replay/radio-attitude-drift-5.mp3`,
  `audio/replay/radio-attitude-drift-6.mp3`, `audio/replay/radio-attitude-drift-7.mp3`,
  `audio/replay/radio-attitude-drift-8.mp3`, `audio/replay/radio-blackout-1.mp3`,
  `audio/replay/radio-blackout-2.mp3`, `audio/replay/radio-blackout-3.mp3`,
  `audio/replay/radio-blackout-4.mp3`, `audio/replay/radio-blackout-5.mp3`,
  `audio/replay/radio-blackout-6.mp3`, `audio/replay/radio-blackout-7.mp3`,
  `audio/replay/radio-breakup-1.mp3`, `audio/replay/radio-breakup-2.mp3`,
  `audio/replay/radio-breakup-3.mp3`, `audio/replay/radio-breakup-4.mp3`,
  `audio/replay/radio-breakup-5.mp3`, `audio/replay/radio-breakup-6.mp3`,
  `audio/replay/radio-burn-failed-1.mp3`, `audio/replay/radio-burn-failed-2.mp3`,
  `audio/replay/radio-burn-failed-3.mp3`, `audio/replay/radio-burn-failed-4.mp3`,
  `audio/replay/radio-burn-failed-5.mp3`, `audio/replay/radio-burn-failed-6.mp3`,
  `audio/replay/radio-burn-failed-7.mp3`, `audio/replay/radio-burn-failed-8.mp3`,
  `audio/replay/radio-burn-good-1.mp3`, `audio/replay/radio-burn-good-2.mp3`,
  `audio/replay/radio-burn-good-3.mp3`, `audio/replay/radio-burn-good-4.mp3`,
  `audio/replay/radio-burn-good-5.mp3`, `audio/replay/radio-burn-good-6.mp3`,
  `audio/replay/radio-burn-good-7.mp3`, `audio/replay/radio-burn-good-8.mp3`,
  `audio/replay/radio-burn-short-1.mp3`, `audio/replay/radio-burn-short-2.mp3`,
  `audio/replay/radio-burn-short-3.mp3`, `audio/replay/radio-burn-short-4.mp3`,
  `audio/replay/radio-burn-short-5.mp3`, `audio/replay/radio-burn-short-6.mp3`,
  `audio/replay/radio-cant-brake-1.mp3`, `audio/replay/radio-cant-brake-2.mp3`,
  `audio/replay/radio-cant-brake-3.mp3`, `audio/replay/radio-cant-brake-4.mp3`,
  `audio/replay/radio-cant-brake-5.mp3`, `audio/replay/radio-capsule-sinking-1.mp3`,
  `audio/replay/radio-capsule-sinking-2.mp3`, `audio/replay/radio-capsule-sinking-3.mp3`,
  `audio/replay/radio-capsule-sinking-4.mp3`, `audio/replay/radio-chute-partial-1.mp3`,
  `audio/replay/radio-chute-partial-2.mp3`, `audio/replay/radio-chute-partial-3.mp3`,
  `audio/replay/radio-chute-partial-4.mp3`, `audio/replay/radio-chute-partial-5.mp3`,
  `audio/replay/radio-chute-partial-6.mp3`, `audio/replay/radio-chute-partial-7.mp3`,
  `audio/replay/radio-chute-partial-8.mp3`, `audio/replay/radio-chute-partial-9.mp3`,
  `audio/replay/radio-closing-fast-1.mp3`, `audio/replay/radio-closing-fast-10.mp3`,
  `audio/replay/radio-closing-fast-11.mp3`, `audio/replay/radio-closing-fast-12.mp3`,
  `audio/replay/radio-closing-fast-13.mp3`, `audio/replay/radio-closing-fast-2.mp3`,
  `audio/replay/radio-closing-fast-3.mp3`, `audio/replay/radio-closing-fast-4.mp3`,
  `audio/replay/radio-closing-fast-5.mp3`, `audio/replay/radio-closing-fast-6.mp3`,
  `audio/replay/radio-closing-fast-7.mp3`, `audio/replay/radio-closing-fast-8.mp3`,
  `audio/replay/radio-closing-fast-9.mp3`, `audio/replay/radio-coast-good-1.mp3`,
  `audio/replay/radio-coast-good-2.mp3`, `audio/replay/radio-coast-good-3.mp3`,
  `audio/replay/radio-coast-good-4.mp3`, `audio/replay/radio-coast-good-5.mp3`,
  `audio/replay/radio-coast-good-6.mp3`, `audio/replay/radio-coast-good-7.mp3`,
  `audio/replay/radio-coast-good-8.mp3`, `audio/replay/radio-collision-1.mp3`,
  `audio/replay/radio-collision-2.mp3`, `audio/replay/radio-collision-3.mp3`,
  `audio/replay/radio-collision-4.mp3`, `audio/replay/radio-collision-5.mp3`,
  `audio/replay/radio-collision-6.mp3`, `audio/replay/radio-contact-light-1.mp3`,
  `audio/replay/radio-contact-light-2.mp3`, `audio/replay/radio-contact-light-3.mp3`,
  `audio/replay/radio-contact-light-4.mp3`, `audio/replay/radio-contact-light-5.mp3`,
  `audio/replay/radio-contact-light-6.mp3`, `audio/replay/radio-contact-light-7.mp3`,
  `audio/replay/radio-contact-light-8.mp3`, `audio/replay/radio-contact-light-9.mp3`,
  `audio/replay/radio-corridor-glitch-1.mp3`, `audio/replay/radio-corridor-glitch-2.mp3`,
  `audio/replay/radio-corridor-glitch-3.mp3`, `audio/replay/radio-corridor-glitch-4.mp3`,
  `audio/replay/radio-corridor-glitch-5.mp3`, `audio/replay/radio-corridor-glitch-6.mp3`,
  `audio/replay/radio-countdown-hold-1.mp3`, `audio/replay/radio-countdown-hold-2.mp3`,
  `audio/replay/radio-countdown-hold-3.mp3`, `audio/replay/radio-countdown-hold-4.mp3`,
  `audio/replay/radio-course-corrected-1.mp3`, `audio/replay/radio-course-corrected-2.mp3`,
  `audio/replay/radio-course-corrected-3.mp3`, `audio/replay/radio-course-corrected-4.mp3`,
  `audio/replay/radio-course-corrected-5.mp3`, `audio/replay/radio-course-corrected-6.mp3`,
  `audio/replay/radio-crew-back-in-1.mp3`, `audio/replay/radio-crew-back-in-2.mp3`,
  `audio/replay/radio-crew-back-in-3.mp3`, `audio/replay/radio-crew-back-in-4.mp3`,
  `audio/replay/radio-crew-back-in-5.mp3`, `audio/replay/radio-crew-back-in-6.mp3`,
  `audio/replay/radio-crew-well-1.mp3`, `audio/replay/radio-crew-well-10.mp3`,
  `audio/replay/radio-crew-well-2.mp3`, `audio/replay/radio-crew-well-3.mp3`,
  `audio/replay/radio-crew-well-4.mp3`, `audio/replay/radio-crew-well-5.mp3`,
  `audio/replay/radio-crew-well-6.mp3`, `audio/replay/radio-crew-well-7.mp3`,
  `audio/replay/radio-crew-well-8.mp3`, `audio/replay/radio-crew-well-9.mp3`,
  `audio/replay/radio-cutoff-1.mp3`, `audio/replay/radio-cutoff-2.mp3`,
  `audio/replay/radio-cutoff-3.mp3`, `audio/replay/radio-cutoff-4.mp3`,
  `audio/replay/radio-cutoff-5.mp3`, `audio/replay/radio-cutoff-6.mp3`,
  `audio/replay/radio-data-lost-1.mp3`, `audio/replay/radio-data-lost-10.mp3`,
  `audio/replay/radio-data-lost-11.mp3`, `audio/replay/radio-data-lost-12.mp3`,
  `audio/replay/radio-data-lost-13.mp3`, `audio/replay/radio-data-lost-14.mp3`,
  `audio/replay/radio-data-lost-15.mp3`, `audio/replay/radio-data-lost-2.mp3`,
  `audio/replay/radio-data-lost-3.mp3`, `audio/replay/radio-data-lost-4.mp3`,
  `audio/replay/radio-data-lost-5.mp3`, `audio/replay/radio-data-lost-6.mp3`,
  `audio/replay/radio-data-lost-7.mp3`, `audio/replay/radio-data-lost-8.mp3`,
  `audio/replay/radio-data-lost-9.mp3`, `audio/replay/radio-descent-good-1.mp3`,
  `audio/replay/radio-descent-good-2.mp3`, `audio/replay/radio-descent-good-3.mp3`,
  `audio/replay/radio-descent-good-4.mp3`, `audio/replay/radio-descent-good-5.mp3`,
  `audio/replay/radio-descent-good-6.mp3`, `audio/replay/radio-descent-good-7.mp3`,
  `audio/replay/radio-descent-good-8.mp3`, `audio/replay/radio-docked-1.mp3`,
  `audio/replay/radio-docked-10.mp3`, `audio/replay/radio-docked-2.mp3`,
  `audio/replay/radio-docked-3.mp3`, `audio/replay/radio-docked-4.mp3`,
  `audio/replay/radio-docked-5.mp3`, `audio/replay/radio-docked-6.mp3`,
  `audio/replay/radio-docked-7.mp3`, `audio/replay/radio-docked-8.mp3`,
  `audio/replay/radio-docked-9.mp3`, `audio/replay/radio-drogue-late-1.mp3`,
  `audio/replay/radio-drogue-late-2.mp3`, `audio/replay/radio-drogue-late-3.mp3`,
  `audio/replay/radio-egress-good-1.mp3`, `audio/replay/radio-egress-good-2.mp3`,
  `audio/replay/radio-egress-good-3.mp3`, `audio/replay/radio-egress-good-4.mp3`,
  `audio/replay/radio-egress-good-5.mp3`, `audio/replay/radio-egress-good-6.mp3`,
  `audio/replay/radio-egress-good-7.mp3`, `audio/replay/radio-egress-good-8.mp3`,
  `audio/replay/radio-engine-out-1.mp3`, `audio/replay/radio-engine-out-2.mp3`,
  `audio/replay/radio-engine-out-3.mp3`, `audio/replay/radio-engine-rough-1.mp3`,
  `audio/replay/radio-engine-rough-2.mp3`, `audio/replay/radio-engine-rough-3.mp3`,
  `audio/replay/radio-engine-rough-4.mp3`, `audio/replay/radio-engine-rough-5.mp3`,
  `audio/replay/radio-engine-rough-6.mp3`, `audio/replay/radio-engine-rough-7.mp3`,
  `audio/replay/radio-engine-rough-8.mp3`, `audio/replay/radio-engine-test-failed-1.mp3`,
  `audio/replay/radio-engine-test-failed-2.mp3`, `audio/replay/radio-engine-test-failed-3.mp3`,
  `audio/replay/radio-engine-test-failed-4.mp3`, `audio/replay/radio-engine-test-failed-5.mp3`,
  `audio/replay/radio-engine-test-good-1.mp3`, `audio/replay/radio-engine-test-good-2.mp3`,
  `audio/replay/radio-engine-test-good-3.mp3`, `audio/replay/radio-engine-test-good-4.mp3`,
  `audio/replay/radio-engine-test-good-5.mp3`, `audio/replay/radio-engine-test-good-6.mp3`,
  `audio/replay/radio-entry-corridor-good-1.mp3`, `audio/replay/radio-entry-corridor-good-2.mp3`,
  `audio/replay/radio-entry-corridor-good-3.mp3`, `audio/replay/radio-entry-corridor-good-4.mp3`,
  `audio/replay/radio-entry-good-1.mp3`, `audio/replay/radio-entry-good-2.mp3`,
  `audio/replay/radio-entry-good-3.mp3`, `audio/replay/radio-entry-good-4.mp3`,
  `audio/replay/radio-entry-good-5.mp3`, `audio/replay/radio-entry-good-6.mp3`,
  `audio/replay/radio-escape-fired-1.mp3`, `audio/replay/radio-escape-fired-2.mp3`,
  `audio/replay/radio-escape-fired-3.mp3`, `audio/replay/radio-eva-good-1.mp3`,
  `audio/replay/radio-eva-good-10.mp3`, `audio/replay/radio-eva-good-11.mp3`,
  `audio/replay/radio-eva-good-12.mp3`, `audio/replay/radio-eva-good-2.mp3`,
  `audio/replay/radio-eva-good-3.mp3`, `audio/replay/radio-eva-good-4.mp3`,
  `audio/replay/radio-eva-good-5.mp3`, `audio/replay/radio-eva-good-6.mp3`,
  `audio/replay/radio-eva-good-7.mp3`, `audio/replay/radio-eva-good-8.mp3`,
  `audio/replay/radio-eva-good-9.mp3`, `audio/replay/radio-eva-task-failed-1.mp3`,
  `audio/replay/radio-eva-task-failed-2.mp3`, `audio/replay/radio-eva-task-failed-3.mp3`,
  `audio/replay/radio-eva-task-failed-4.mp3`, `audio/replay/radio-eva-task-failed-5.mp3`,
  `audio/replay/radio-eva-trouble-1.mp3`, `audio/replay/radio-eva-trouble-2.mp3`,
  `audio/replay/radio-eva-trouble-3.mp3`, `audio/replay/radio-eva-trouble-4.mp3`,
  `audio/replay/radio-eva-tumble-1.mp3`, `audio/replay/radio-eva-tumble-2.mp3`,
  `audio/replay/radio-eva-tumble-3.mp3`, `audio/replay/radio-eva-tumble-4.mp3`,
  `audio/replay/radio-eva-tumble-5.mp3`, `audio/replay/radio-eva-tumble-6.mp3`,
  `audio/replay/radio-flyby-good-1.mp3`, `audio/replay/radio-flyby-good-2.mp3`,
  `audio/replay/radio-flyby-good-3.mp3`, `audio/replay/radio-flyby-good-4.mp3`,
  `audio/replay/radio-flyby-good-5.mp3`, `audio/replay/radio-flyby-good-6.mp3`,
  `audio/replay/radio-flyby-good-7.mp3`, `audio/replay/radio-go-for-launch-1.mp3`,
  `audio/replay/radio-go-for-launch-2.mp3`, `audio/replay/radio-guidance-internal-1.mp3`,
  `audio/replay/radio-guidance-internal-2.mp3`, `audio/replay/radio-guidance-internal-3.mp3`,
  `audio/replay/radio-guidance-internal-4.mp3`, `audio/replay/radio-guidance-internal-5.mp3`,
  `audio/replay/radio-guidance-internal-6.mp3`, `audio/replay/radio-guidance-internal-7.mp3`,
  `audio/replay/radio-guidance-internal-8.mp3`, `audio/replay/radio-guidance-internal-9.mp3`,
  `audio/replay/radio-hard-landing-1.mp3`, `audio/replay/radio-hard-landing-2.mp3`,
  `audio/replay/radio-hard-landing-3.mp3`, `audio/replay/radio-hard-landing-4.mp3`,
  `audio/replay/radio-hard-landing-5.mp3`, `audio/replay/radio-hard-landing-6.mp3`,
  `audio/replay/radio-hard-landing-7.mp3`, `audio/replay/radio-hard-landing-8.mp3`,
  `audio/replay/radio-impact-good-1.mp3`, `audio/replay/radio-impact-good-2.mp3`,
  `audio/replay/radio-impact-good-3.mp3`, `audio/replay/radio-impact-good-4.mp3`,
  `audio/replay/radio-impact-good-5.mp3`, `audio/replay/radio-impact-good-6.mp3`,
  `audio/replay/radio-ingress-good-1.mp3`, `audio/replay/radio-ingress-good-2.mp3`,
  `audio/replay/radio-ingress-good-3.mp3`, `audio/replay/radio-ingress-good-4.mp3`,
  `audio/replay/radio-ingress-good-5.mp3`, `audio/replay/radio-ingress-good-6.mp3`,
  `audio/replay/radio-ingress-good-7.mp3`, `audio/replay/radio-ingress-good-8.mp3`,
  `audio/replay/radio-landed-good-1.mp3`, `audio/replay/radio-landed-good-2.mp3`,
  `audio/replay/radio-landed-good-3.mp3`, `audio/replay/radio-landed-good-4.mp3`,
  `audio/replay/radio-landed-good-5.mp3`, `audio/replay/radio-lander-away-1.mp3`,
  `audio/replay/radio-lander-away-2.mp3`, `audio/replay/radio-lander-away-3.mp3`,
  `audio/replay/radio-lander-away-4.mp3`, `audio/replay/radio-lander-away-5.mp3`,
  `audio/replay/radio-lander-away-6.mp3`, `audio/replay/radio-lander-down-1.mp3`,
  `audio/replay/radio-lander-down-10.mp3`, `audio/replay/radio-lander-down-2.mp3`,
  `audio/replay/radio-lander-down-3.mp3`, `audio/replay/radio-lander-down-4.mp3`,
  `audio/replay/radio-lander-down-5.mp3`, `audio/replay/radio-lander-down-6.mp3`,
  `audio/replay/radio-lander-down-7.mp3`, `audio/replay/radio-lander-down-8.mp3`,
  `audio/replay/radio-lander-down-9.mp3`, `audio/replay/radio-lander-lost-1.mp3`,
  `audio/replay/radio-lander-lost-10.mp3`, `audio/replay/radio-lander-lost-2.mp3`,
  `audio/replay/radio-lander-lost-3.mp3`, `audio/replay/radio-lander-lost-4.mp3`,
  `audio/replay/radio-lander-lost-5.mp3`, `audio/replay/radio-lander-lost-6.mp3`,
  `audio/replay/radio-lander-lost-7.mp3`, `audio/replay/radio-lander-lost-8.mp3`,
  `audio/replay/radio-lander-lost-9.mp3`, `audio/replay/radio-latches-1.mp3`,
  `audio/replay/radio-latches-2.mp3`, `audio/replay/radio-latches-3.mp3`,
  `audio/replay/radio-latches-4.mp3`, `audio/replay/radio-latches-5.mp3`,
  `audio/replay/radio-latches-6.mp3`, `audio/replay/radio-latches-7.mp3`,
  `audio/replay/radio-latches-8.mp3`, `audio/replay/radio-launch-failure-1.mp3`,
  `audio/replay/radio-launch-failure-2.mp3`, `audio/replay/radio-launch-failure-3.mp3`,
  `audio/replay/radio-launch-failure-4.mp3`, `audio/replay/radio-liftoff-failed-1.mp3`,
  `audio/replay/radio-liftoff-failed-2.mp3`, `audio/replay/radio-liftoff-failed-3.mp3`,
  `audio/replay/radio-liftoff-failed-4.mp3`, `audio/replay/radio-liftoff-failed-5.mp3`,
  `audio/replay/radio-liftoff-good-1.mp3`, `audio/replay/radio-liftoff-good-2.mp3`,
  `audio/replay/radio-liftoff-good-3.mp3`, `audio/replay/radio-liftoff-good-4.mp3`,
  `audio/replay/radio-liftoff-good-5.mp3`, `audio/replay/radio-liftoff-late-1.mp3`,
  `audio/replay/radio-liftoff-late-2.mp3`, `audio/replay/radio-liftoff-late-3.mp3`,
  `audio/replay/radio-liftoff-late-4.mp3`, `audio/replay/radio-liftoff-late-5.mp3`,
  `audio/replay/radio-liftoff-late-6.mp3`, `audio/replay/radio-loi-good-1.mp3`,
  `audio/replay/radio-loi-good-2.mp3`, `audio/replay/radio-loi-good-3.mp3`,
  `audio/replay/radio-loi-good-4.mp3`, `audio/replay/radio-loi-good-5.mp3`,
  `audio/replay/radio-loi-good-6.mp3`, `audio/replay/radio-loi-good-7.mp3`,
  `audio/replay/radio-loi-good-8.mp3`, `audio/replay/radio-loi-good-9.mp3`,
  `audio/replay/radio-main-chute-failed-1.mp3`, `audio/replay/radio-main-chute-failed-2.mp3`,
  `audio/replay/radio-main-chute-failed-3.mp3`, `audio/replay/radio-main-chute-failed-4.mp3`,
  `audio/replay/radio-main-chute-failed-5.mp3`, `audio/replay/radio-main-chute-failed-6.mp3`,
  `audio/replay/radio-main-chute-failed-7.mp3`, `audio/replay/radio-main-chute-failed-8.mp3`,
  `audio/replay/radio-main-good-1.mp3`, `audio/replay/radio-main-good-10.mp3`,
  `audio/replay/radio-main-good-2.mp3`, `audio/replay/radio-main-good-3.mp3`,
  `audio/replay/radio-main-good-4.mp3`, `audio/replay/radio-main-good-5.mp3`,
  `audio/replay/radio-main-good-6.mp3`, `audio/replay/radio-main-good-7.mp3`,
  `audio/replay/radio-main-good-8.mp3`, `audio/replay/radio-main-good-9.mp3`,
  `audio/replay/radio-max-q-1.mp3`, `audio/replay/radio-max-q-2.mp3`,
  `audio/replay/radio-max-q-3.mp3`, `audio/replay/radio-max-q-4.mp3`,
  `audio/replay/radio-max-q-5.mp3`, `audio/replay/radio-max-q-6.mp3`,
  `audio/replay/radio-max-q-7.mp3`, `audio/replay/radio-moon-touchdown-1.mp3`,
  `audio/replay/radio-moon-touchdown-10.mp3`, `audio/replay/radio-moon-touchdown-11.mp3`,
  `audio/replay/radio-moon-touchdown-2.mp3`, `audio/replay/radio-moon-touchdown-3.mp3`,
  `audio/replay/radio-moon-touchdown-4.mp3`, `audio/replay/radio-moon-touchdown-5.mp3`,
  `audio/replay/radio-moon-touchdown-6.mp3`, `audio/replay/radio-moon-touchdown-7.mp3`,
  `audio/replay/radio-moon-touchdown-8.mp3`, `audio/replay/radio-moon-touchdown-9.mp3`,
  `audio/replay/radio-moonwalk-fall-1.mp3`, `audio/replay/radio-moonwalk-fall-2.mp3`,
  `audio/replay/radio-moonwalk-fall-3.mp3`, `audio/replay/radio-moonwalk-fall-4.mp3`,
  `audio/replay/radio-moonwalk-fall-5.mp3`, `audio/replay/radio-moonwalk-fall-6.mp3`,
  `audio/replay/radio-moonwalk-good-1.mp3`, `audio/replay/radio-moonwalk-good-10.mp3`,
  `audio/replay/radio-moonwalk-good-11.mp3`, `audio/replay/radio-moonwalk-good-12.mp3`,
  `audio/replay/radio-moonwalk-good-13.mp3`, `audio/replay/radio-moonwalk-good-2.mp3`,
  `audio/replay/radio-moonwalk-good-3.mp3`, `audio/replay/radio-moonwalk-good-4.mp3`,
  `audio/replay/radio-moonwalk-good-5.mp3`, `audio/replay/radio-moonwalk-good-6.mp3`,
  `audio/replay/radio-moonwalk-good-7.mp3`, `audio/replay/radio-moonwalk-good-8.mp3`,
  `audio/replay/radio-moonwalk-good-9.mp3`, `audio/replay/radio-no-acquisition-1.mp3`,
  `audio/replay/radio-no-acquisition-2.mp3`, `audio/replay/radio-no-acquisition-3.mp3`,
  `audio/replay/radio-no-acquisition-4.mp3`, `audio/replay/radio-no-acquisition-5.mp3`,
  `audio/replay/radio-no-acquisition-6.mp3`, `audio/replay/radio-no-acquisition-7.mp3`,
  `audio/replay/radio-no-acquisition-8.mp3`, `audio/replay/radio-no-acquisition-9.mp3`,
  `audio/replay/radio-off-trajectory-1.mp3`, `audio/replay/radio-off-trajectory-2.mp3`,
  `audio/replay/radio-off-trajectory-3.mp3`, `audio/replay/radio-off-trajectory-4.mp3`,
  `audio/replay/radio-off-trajectory-5.mp3`, `audio/replay/radio-off-trajectory-6.mp3`,
  `audio/replay/radio-orbit-insertion-1.mp3`, `audio/replay/radio-orbit-insertion-2.mp3`,
  `audio/replay/radio-orbit-insertion-3.mp3`, `audio/replay/radio-orbit-insertion-4.mp3`,
  `audio/replay/radio-orbit-science-1.mp3`, `audio/replay/radio-orbit-science-2.mp3`,
  `audio/replay/radio-orbit-science-3.mp3`, `audio/replay/radio-orbit-science-4.mp3`,
  `audio/replay/radio-orbit-science-5.mp3`, `audio/replay/radio-orbit-science-6.mp3`,
  `audio/replay/radio-oscillations-1.mp3`, `audio/replay/radio-oscillations-2.mp3`,
  `audio/replay/radio-oscillations-3.mp3`, `audio/replay/radio-oscillations-4.mp3`,
  `audio/replay/radio-oscillations-5.mp3`, `audio/replay/radio-over-the-top-1.mp3`,
  `audio/replay/radio-over-the-top-2.mp3`, `audio/replay/radio-over-the-top-3.mp3`,
  `audio/replay/radio-over-the-top-4.mp3`, `audio/replay/radio-pad-fire-1.mp3`,
  `audio/replay/radio-pad-fire-10.mp3`, `audio/replay/radio-pad-fire-2.mp3`,
  `audio/replay/radio-pad-fire-3.mp3`, `audio/replay/radio-pad-fire-4.mp3`,
  `audio/replay/radio-pad-fire-5.mp3`, `audio/replay/radio-pad-fire-6.mp3`,
  `audio/replay/radio-pad-fire-7.mp3`, `audio/replay/radio-pad-fire-8.mp3`,
  `audio/replay/radio-pad-fire-9.mp3`, `audio/replay/radio-pad-glitch-1.mp3`,
  `audio/replay/radio-pad-glitch-2.mp3`, `audio/replay/radio-pad-glitch-3.mp3`,
  `audio/replay/radio-pad-glitch-4.mp3`, `audio/replay/radio-pad-glitch-5.mp3`,
  `audio/replay/radio-pad-glitch-6.mp3`, `audio/replay/radio-pad-glitch-7.mp3`,
  `audio/replay/radio-pad-shutdown-1.mp3`, `audio/replay/radio-pad-shutdown-2.mp3`,
  `audio/replay/radio-pad-shutdown-3.mp3`, `audio/replay/radio-payload-separation-1.mp3`,
  `audio/replay/radio-payload-separation-10.mp3`, `audio/replay/radio-payload-separation-2.mp3`,
  `audio/replay/radio-payload-separation-3.mp3`, `audio/replay/radio-payload-separation-4.mp3`,
  `audio/replay/radio-payload-separation-5.mp3`, `audio/replay/radio-payload-separation-6.mp3`,
  `audio/replay/radio-payload-separation-7.mp3`, `audio/replay/radio-payload-separation-8.mp3`,
  `audio/replay/radio-payload-separation-9.mp3`, `audio/replay/radio-pitching-downrange-1.mp3`,
  `audio/replay/radio-pitching-downrange-2.mp3`, `audio/replay/radio-plane-climb-1.mp3`,
  `audio/replay/radio-plane-climb-2.mp3`, `audio/replay/radio-plane-climb-3.mp3`,
  `audio/replay/radio-plane-climb-4.mp3`, `audio/replay/radio-plane-climb-5.mp3`,
  `audio/replay/radio-plane-climb-failed-1.mp3`, `audio/replay/radio-plane-climb-failed-2.mp3`,
  `audio/replay/radio-plane-climb-failed-3.mp3`, `audio/replay/radio-plane-climb-failed-4.mp3`,
  `audio/replay/radio-plane-climb-failed-5.mp3`, `audio/replay/radio-plane-cruise-1.mp3`,
  `audio/replay/radio-plane-cruise-2.mp3`, `audio/replay/radio-plane-cruise-3.mp3`,
  `audio/replay/radio-plane-cruise-4.mp3`, `audio/replay/radio-plane-cruise-5.mp3`,
  `audio/replay/radio-plane-cruise-6.mp3`, `audio/replay/radio-plane-cruise-7.mp3`,
  `audio/replay/radio-plane-cruise-abort-1.mp3`, `audio/replay/radio-plane-cruise-abort-2.mp3`,
  `audio/replay/radio-plane-cruise-abort-3.mp3`, `audio/replay/radio-plane-cruise-abort-4.mp3`,
  `audio/replay/radio-plane-cruise-abort-5.mp3`, `audio/replay/radio-plane-cruise-abort-6.mp3`,
  `audio/replay/radio-plane-cruise-glitch-1.mp3`, `audio/replay/radio-plane-cruise-glitch-2.mp3`,
  `audio/replay/radio-plane-cruise-glitch-3.mp3`, `audio/replay/radio-plane-cruise-glitch-4.mp3`,
  `audio/replay/radio-plane-drop-1.mp3`, `audio/replay/radio-plane-drop-2.mp3`,
  `audio/replay/radio-plane-drop-3.mp3`, `audio/replay/radio-plane-drop-4.mp3`,
  `audio/replay/radio-plane-drop-5.mp3`, `audio/replay/radio-plane-drop-failed-1.mp3`,
  `audio/replay/radio-plane-drop-failed-2.mp3`, `audio/replay/radio-plane-drop-failed-3.mp3`,
  `audio/replay/radio-plane-drop-failed-4.mp3`, `audio/replay/radio-plane-drop-glitch-1.mp3`,
  `audio/replay/radio-plane-drop-glitch-2.mp3`, `audio/replay/radio-plane-drop-glitch-3.mp3`,
  `audio/replay/radio-plane-drop-glitch-4.mp3`, `audio/replay/radio-plane-drop-glitch-5.mp3`,
  `audio/replay/radio-plane-drop-glitch-6.mp3`, `audio/replay/radio-plane-fast-good-1.mp3`,
  `audio/replay/radio-plane-fast-good-2.mp3`, `audio/replay/radio-plane-fast-good-3.mp3`,
  `audio/replay/radio-plane-fast-good-4.mp3`, `audio/replay/radio-plane-fast-good-5.mp3`,
  `audio/replay/radio-plane-hot-spot-1.mp3`, `audio/replay/radio-plane-hot-spot-2.mp3`,
  `audio/replay/radio-plane-hot-spot-3.mp3`, `audio/replay/radio-plane-hot-spot-4.mp3`,
  `audio/replay/radio-plane-hot-spot-5.mp3`, `audio/replay/radio-plane-hot-spot-6.mp3`,
  `audio/replay/radio-plane-hot-spot-7.mp3`, `audio/replay/radio-plane-takeoff-1.mp3`,
  `audio/replay/radio-plane-takeoff-2.mp3`, `audio/replay/radio-plane-takeoff-3.mp3`,
  `audio/replay/radio-plane-takeoff-4.mp3`, `audio/replay/radio-plane-takeoff-5.mp3`,
  `audio/replay/radio-plane-takeoff-6.mp3`, `audio/replay/radio-plane-takeoff-7.mp3`,
  `audio/replay/radio-plane-takeoff-abort-1.mp3`, `audio/replay/radio-plane-takeoff-abort-2.mp3`,
  `audio/replay/radio-plane-takeoff-abort-3.mp3`, `audio/replay/radio-plane-takeoff-abort-4.mp3`,
  `audio/replay/radio-plane-takeoff-glitch-1.mp3`, `audio/replay/radio-plane-takeoff-glitch-2.mp3`,
  `audio/replay/radio-plane-takeoff-glitch-3.mp3`, `audio/replay/radio-plane-takeoff-glitch-4.mp3`,
  `audio/replay/radio-plane-takeoff-glitch-5.mp3`, `audio/replay/radio-plane-takeoff-glitch-6.mp3`,
  `audio/replay/radio-pressure-1.mp3`, `audio/replay/radio-pressure-10.mp3`,
  `audio/replay/radio-pressure-11.mp3`, `audio/replay/radio-pressure-12.mp3`,
  `audio/replay/radio-pressure-2.mp3`, `audio/replay/radio-pressure-3.mp3`,
  `audio/replay/radio-pressure-4.mp3`, `audio/replay/radio-pressure-5.mp3`,
  `audio/replay/radio-pressure-6.mp3`, `audio/replay/radio-pressure-7.mp3`,
  `audio/replay/radio-pressure-8.mp3`, `audio/replay/radio-pressure-9.mp3`,
  `audio/replay/radio-probe-silent-1.mp3`, `audio/replay/radio-probe-silent-2.mp3`,
  `audio/replay/radio-probe-silent-3.mp3`, `audio/replay/radio-probe-silent-4.mp3`,
  `audio/replay/radio-probe-silent-5.mp3`, `audio/replay/radio-probe-silent-6.mp3`,
  `audio/replay/radio-probe-silent-7.mp3`, `audio/replay/radio-probe-silent-8.mp3`,
  `audio/replay/radio-problem-1.mp3`, `audio/replay/radio-problem-2.mp3`,
  `audio/replay/radio-problem-3.mp3`, `audio/replay/radio-problem-4.mp3`,
  `audio/replay/radio-problem-5.mp3`, `audio/replay/radio-problem-6.mp3`,
  `audio/replay/radio-problem-7.mp3`, `audio/replay/radio-program-alarm-1.mp3`,
  `audio/replay/radio-program-alarm-2.mp3`, `audio/replay/radio-program-alarm-3.mp3`,
  `audio/replay/radio-program-alarm-4.mp3`, `audio/replay/radio-program-alarm-5.mp3`,
  `audio/replay/radio-program-alarm-6.mp3`, `audio/replay/radio-recovery-good-1.mp3`,
  `audio/replay/radio-recovery-good-2.mp3`, `audio/replay/radio-recovery-good-3.mp3`,
  `audio/replay/radio-recovery-good-4.mp3`, `audio/replay/radio-recovery-list-1.mp3`,
  `audio/replay/radio-recovery-list-2.mp3`, `audio/replay/radio-recovery-list-3.mp3`,
  `audio/replay/radio-recovery-list-4.mp3`, `audio/replay/radio-recovery-list-5.mp3`,
  `audio/replay/radio-recovery-no-contact-1.mp3`, `audio/replay/radio-recovery-no-contact-2.mp3`,
  `audio/replay/radio-recovery-no-contact-3.mp3`, `audio/replay/radio-recovery-no-contact-4.mp3`,
  `audio/replay/radio-recovery-no-contact-5.mp3`, `audio/replay/radio-rendezvous-good-1.mp3`,
  `audio/replay/radio-rendezvous-good-2.mp3`, `audio/replay/radio-rendezvous-good-3.mp3`,
  `audio/replay/radio-rendezvous-good-4.mp3`, `audio/replay/radio-rendezvous-good-5.mp3`,
  `audio/replay/radio-rendezvous-good-6.mp3`, `audio/replay/radio-rendezvous-good-7.mp3`,
  `audio/replay/radio-rendezvous-good-8.mp3`, `audio/replay/radio-rendezvous-lost-1.mp3`,
  `audio/replay/radio-rendezvous-lost-2.mp3`, `audio/replay/radio-rendezvous-lost-3.mp3`,
  `audio/replay/radio-rendezvous-lost-4.mp3`, `audio/replay/radio-rendezvous-lost-5.mp3`,
  `audio/replay/radio-rendezvous-lost-6.mp3`, `audio/replay/radio-retro-glitch-1.mp3`,
  `audio/replay/radio-retro-glitch-2.mp3`, `audio/replay/radio-retro-glitch-3.mp3`,
  `audio/replay/radio-retro-glitch-4.mp3`, `audio/replay/radio-retro-glitch-5.mp3`,
  `audio/replay/radio-retro-good-1.mp3`, `audio/replay/radio-retro-good-2.mp3`,
  `audio/replay/radio-retro-good-3.mp3`, `audio/replay/radio-retro-good-4.mp3`,
  `audio/replay/radio-retro-good-5.mp3`, `audio/replay/radio-retro-good-6.mp3`,
  `audio/replay/radio-retro-shutdown-failed-1.mp3`, `audio/replay/radio-retro-shutdown-failed-2.mp3`,
  `audio/replay/radio-retro-shutdown-failed-3.mp3`, `audio/replay/radio-retro-shutdown-failed-4.mp3`,
  `audio/replay/radio-retro-shutdown-failed-5.mp3`, `audio/replay/radio-retro-shutdown-failed-6.mp3`,
  `audio/replay/radio-retro-shutdown-late-1.mp3`, `audio/replay/radio-retro-shutdown-late-2.mp3`,
  `audio/replay/radio-retro-shutdown-late-3.mp3`, `audio/replay/radio-retro-shutdown-late-4.mp3`,
  `audio/replay/radio-retro-shutdown-late-5.mp3`, `audio/replay/radio-retro-shutdown-late-6.mp3`,
  `audio/replay/radio-roger-1.mp3`, `audio/replay/radio-roger-2.mp3`,
  `audio/replay/radio-roger-3.mp3`, `audio/replay/radio-roger-4.mp3`,
  `audio/replay/radio-roger-5.mp3`, `audio/replay/radio-roger-6.mp3`,
  `audio/replay/radio-roger-7.mp3`, `audio/replay/radio-rover-dead-1.mp3`,
  `audio/replay/radio-rover-dead-2.mp3`, `audio/replay/radio-rover-dead-3.mp3`,
  `audio/replay/radio-rover-dead-4.mp3`, `audio/replay/radio-rover-dead-5.mp3`,
  `audio/replay/radio-rover-dead-6.mp3`, `audio/replay/radio-rover-dead-7.mp3`,
  `audio/replay/radio-rover-glitch-1.mp3`, `audio/replay/radio-rover-glitch-2.mp3`,
  `audio/replay/radio-rover-glitch-3.mp3`, `audio/replay/radio-rover-glitch-4.mp3`,
  `audio/replay/radio-rover-glitch-5.mp3`, `audio/replay/radio-rover-glitch-6.mp3`,
  `audio/replay/radio-rover-good-1.mp3`, `audio/replay/radio-rover-good-2.mp3`,
  `audio/replay/radio-rover-good-3.mp3`, `audio/replay/radio-rover-good-4.mp3`,
  `audio/replay/radio-rover-good-5.mp3`, `audio/replay/radio-rover-good-6.mp3`,
  `audio/replay/radio-rover-good-7.mp3`, `audio/replay/radio-runway-good-1.mp3`,
  `audio/replay/radio-runway-good-2.mp3`, `audio/replay/radio-runway-good-3.mp3`,
  `audio/replay/radio-safe-mode-1.mp3`, `audio/replay/radio-safe-mode-2.mp3`,
  `audio/replay/radio-safe-mode-3.mp3`, `audio/replay/radio-safe-mode-4.mp3`,
  `audio/replay/radio-safe-mode-5.mp3`, `audio/replay/radio-second-stage-late-1.mp3`,
  `audio/replay/radio-second-stage-late-2.mp3`, `audio/replay/radio-separation-failed-1.mp3`,
  `audio/replay/radio-separation-failed-10.mp3`, `audio/replay/radio-separation-failed-11.mp3`,
  `audio/replay/radio-separation-failed-12.mp3`, `audio/replay/radio-separation-failed-2.mp3`,
  `audio/replay/radio-separation-failed-3.mp3`, `audio/replay/radio-separation-failed-4.mp3`,
  `audio/replay/radio-separation-failed-5.mp3`, `audio/replay/radio-separation-failed-6.mp3`,
  `audio/replay/radio-separation-failed-7.mp3`, `audio/replay/radio-separation-failed-8.mp3`,
  `audio/replay/radio-separation-failed-9.mp3`, `audio/replay/radio-separation-good-1.mp3`,
  `audio/replay/radio-separation-good-2.mp3`, `audio/replay/radio-separation-good-3.mp3`,
  `audio/replay/radio-separation-good-4.mp3`, `audio/replay/radio-separation-good-5.mp3`,
  `audio/replay/radio-separation-good-6.mp3`, `audio/replay/radio-separation-good-7.mp3`,
  `audio/replay/radio-separation-good-8.mp3`, `audio/replay/radio-separation-good-9.mp3`,
  `audio/replay/radio-separation-late-1.mp3`, `audio/replay/radio-separation-late-2.mp3`,
  `audio/replay/radio-separation-late-3.mp3`, `audio/replay/radio-separation-late-4.mp3`,
  `audio/replay/radio-separation-late-5.mp3`, `audio/replay/radio-separation-late-6.mp3`,
  `audio/replay/radio-signal-good-1.mp3`, `audio/replay/radio-signal-good-2.mp3`,
  `audio/replay/radio-signal-good-3.mp3`, `audio/replay/radio-signal-good-4.mp3`,
  `audio/replay/radio-skip-out-1.mp3`, `audio/replay/radio-skip-out-2.mp3`,
  `audio/replay/radio-skip-out-3.mp3`, `audio/replay/radio-skip-out-4.mp3`,
  `audio/replay/radio-splashdown-good-1.mp3`, `audio/replay/radio-splashdown-good-2.mp3`,
  `audio/replay/radio-splashdown-good-3.mp3`, `audio/replay/radio-splashdown-good-4.mp3`,
  `audio/replay/radio-staging-1.mp3`, `audio/replay/radio-staging-2.mp3`,
  `audio/replay/radio-staging-3.mp3`, `audio/replay/radio-staging-good-1.mp3`,
  `audio/replay/radio-staging-good-2.mp3`, `audio/replay/radio-staging-good-3.mp3`,
  `audio/replay/radio-staging-good-4.mp3`, `audio/replay/radio-staging-good-5.mp3`,
  `audio/replay/radio-station-keeping-1.mp3`, `audio/replay/radio-station-keeping-2.mp3`,
  `audio/replay/radio-station-keeping-3.mp3`, `audio/replay/radio-station-keeping-4.mp3`,
  `audio/replay/radio-station-keeping-5.mp3`, `audio/replay/radio-surface-good-1.mp3`,
  `audio/replay/radio-surface-good-10.mp3`, `audio/replay/radio-surface-good-11.mp3`,
  `audio/replay/radio-surface-good-2.mp3`, `audio/replay/radio-surface-good-3.mp3`,
  `audio/replay/radio-surface-good-4.mp3`, `audio/replay/radio-surface-good-5.mp3`,
  `audio/replay/radio-surface-good-6.mp3`, `audio/replay/radio-surface-good-7.mp3`,
  `audio/replay/radio-surface-good-8.mp3`, `audio/replay/radio-surface-good-9.mp3`,
  `audio/replay/radio-tei-good-1.mp3`, `audio/replay/radio-tei-good-2.mp3`,
  `audio/replay/radio-tei-good-3.mp3`, `audio/replay/radio-tei-good-4.mp3`,
  `audio/replay/radio-tei-good-5.mp3`, `audio/replay/radio-tei-good-6.mp3`,
  `audio/replay/radio-telemetry-lost-1.mp3`, `audio/replay/radio-telemetry-lost-2.mp3`,
  `audio/replay/radio-telemetry-lost-3.mp3`, `audio/replay/radio-tli-good-1.mp3`,
  `audio/replay/radio-tli-good-10.mp3`, `audio/replay/radio-tli-good-11.mp3`,
  `audio/replay/radio-tli-good-12.mp3`, `audio/replay/radio-tli-good-2.mp3`,
  `audio/replay/radio-tli-good-3.mp3`, `audio/replay/radio-tli-good-4.mp3`,
  `audio/replay/radio-tli-good-5.mp3`, `audio/replay/radio-tli-good-6.mp3`,
  `audio/replay/radio-tli-good-7.mp3`, `audio/replay/radio-tli-good-8.mp3`,
  `audio/replay/radio-tli-good-9.mp3`, `audio/replay/radio-trajectory-low-1.mp3`,
  `audio/replay/radio-trajectory-low-2.mp3`, `audio/replay/radio-trajectory-low-3.mp3`,
  `audio/replay/radio-trajectory-low-4.mp3`, `audio/replay/radio-trajectory-low-5.mp3`,
  `audio/replay/radio-trajectory-low-6.mp3`, `audio/replay/radio-trajectory-low-7.mp3`,
  `audio/replay/radio-tumbling-1.mp3`, `audio/replay/radio-tumbling-2.mp3`,
  `audio/replay/radio-tumbling-3.mp3`, `audio/replay/radio-tumbling-4.mp3`,
  `audio/replay/radio-tumbling-5.mp3`, `audio/replay/radio-tumbling-6.mp3`,
  `audio/replay/radio-tumbling-7.mp3`, `audio/replay/radio-tumbling-8.mp3`,
  `audio/replay/radio-undervolt-1.mp3`, `audio/replay/radio-undervolt-2.mp3`,
  `audio/replay/radio-undervolt-3.mp3`, `audio/replay/radio-undervolt-4.mp3`,
  `audio/replay/radio-undervolt-5.mp3`, `audio/replay/radio-undock-glitch-1.mp3`,
  `audio/replay/radio-undock-glitch-2.mp3`, `audio/replay/radio-undock-glitch-3.mp3`,
  `audio/replay/radio-undock-glitch-4.mp3`, `audio/replay/radio-undock-glitch-5.mp3`,
  `audio/replay/radio-undocked-1.mp3`, `audio/replay/radio-undocked-2.mp3`,
  `audio/replay/radio-undocked-3.mp3`, `audio/replay/radio-undocked-4.mp3`,
  `audio/replay/radio-undocked-5.mp3`, `audio/replay/radio-undocked-6.mp3`,
  `audio/replay/radio-undocked-7.mp3`, `audio/replay/radio-watching-1.mp3`,
  `audio/replay/radio-watching-2.mp3`, `audio/replay/radio-watching-3.mp3`:
  the radio call-outs, made for Launch Window by its author (some with Claude, the author's workflow)
  with VibeVoice (Microsoft, https://huggingface.co/microsoft/VibeVoice-1.5B, MIT License) in
  ComfyUI, with brownian noise mixed under them and the Quindar tones (`assets-raw/audio/quindar/`,
  generated from scratch) at both ends. AI-generated speech, not recordings of real controllers.

## Photos

- `ui/portraits/laika.jpg`: "Laika in 1957", Laika before her orbital flight. Photo: Museum of
  Cosmonautics / Main Archive of Moscow (Музей космонавтики / Главархив Москвы), Mos.ru,
  https://www.mos.ru/news/item/94754073/, via Wikimedia Commons,
  https://commons.wikimedia.org/wiki/File:Laika_in_1957.jpg. Licensed under CC BY 4.0,
  https://creativecommons.org/licenses/by/4.0/. Cropped, converted to greyscale and resized.
- `ui/portraits/belka_strelka.jpg`: "Belka and Strelka", the dogs after their orbital flight,
  22 August 1960. Photo: Sergei Preobrazhensky and Nikolai Sitnikov, Museum of Cosmonautics /
  Main Archive of Moscow (Музей космонавтики / Главархив Москвы), Mos.ru,
  https://www.mos.ru/news/item/94754073/, via Wikimedia Commons,
  https://commons.wikimedia.org/wiki/File:Belka_and_Strelka.jpg. Licensed under CC BY 4.0,
  https://creativecommons.org/licenses/by/4.0/. Cropped, converted to greyscale and resized.
- `ui/portraits/chernushka.jpg`: Chernushka, flown on Korabl-Sputnik 4 in March 1961, as shown in
  the Pauls Stradins Museum of the History of Medicine, Riga. Photo: Michał Derela, via Wikimedia
  Commons, https://commons.wikimedia.org/wiki/File:Vostok_Chernushka_144534898HDR.jpg. Licensed
  under CC BY-SA 4.0, https://creativecommons.org/licenses/by-sa/4.0/. Cropped, converted to
  greyscale and resized; this version is shared under the same licence.
- `ui/portraits/ham.jpg`: Ham being fitted into his Mercury-Redstone 2 capsule, January 1961.
  Photo: NASA (S63-20801), via Wikimedia Commons,
  https://commons.wikimedia.org/wiki/File:Ham_the_chimp_(cropped).jpg. Public domain.
- `ui/portraits/enos.jpg`: Enos with his handler before the Mercury-Atlas 5 flight, 1961. Photo:
  NASA (S61-04540), via Wikimedia Commons,
  https://commons.wikimedia.org/wiki/File:Mercury-Atlas_5_Enos_with_handler.jpg. Public domain.

## The archive

The archive's pictures and sounds (design.md 2.12; each one's source and rights are checked in
`assets-raw/archive/PAGES.md`). NASA's, JPL's, Ames's and Langley's are public domain under NASA's media
guidelines, https://www.nasa.gov/nasa-brand-center/images-and-media/ (JPL's under its image use policy,
https://www.jpl.nasa.gov/jpl-image-use-policy); no endorsement by NASA, JPL or Caltech is implied. All are
resized; a few are cropped.

- `ui/archive/lu3_1.jpg`: Luna 3: the far side of the Moon. The first view of the Moon’s far side, 7 October 1959, with Mare Moscoviense and the crater Tsiolkovskiy. Luna 3 (USSR, 1959). Image via NASA/NSSDC · lu3_1, https://nssdc.gsfc.nasa.gov/imgcat/html/object_page/lu3_1.html. A Soviet image, republished by NASA's NSSDC, which notes it is not necessarily in the public domain. Used as a photograph without creative input whose 50-year term under German law (§ 72(3) UrhG) has run out; recheck before the repository goes public.
- `ui/archive/pia02975.jpg`: Ranger 7: the Moon up close. The first picture of the Moon by a US spacecraft, about 17 minutes before impact, with the crater Alphonsus at center right. NASA/JPL · PIA02975, https://images.nasa.gov/details/PIA02975. Public domain.
- `ui/archive/pia02974.jpg`: Ranger 7: the Moon up close. The crater Guericke on the edge of Mare Nubium, taken 8.5 minutes before impact. NASA/JPL · PIA02974, https://images.nasa.gov/details/PIA02974. Public domain.
- `ui/archive/ra7_p200.jpg`: Ranger 7: the Moon up close. Ranger 7’s last frames, cut off when the spacecraft struck the Moon on 31 July 1964. NASA/JPL · ra7_p200, https://nssdc.gsfc.nasa.gov/imgcat/html/object_page/ra7_p200.html. Public domain.
- `ui/archive/s65-30431.jpg`: The first spacewalks: Leonov and White. Ed White floats outside Gemini 4 on 3 June 1965, holding the oxygen gun he steered with, photographed by McDivitt. NASA · S65-30431, https://images.nasa.gov/details/S65-30431. Public domain.
- `ui/archive/s65-30433.jpg`: The first spacewalks: Leonov and White. White on his tether, his gold-coated visor shielding him from the unfiltered Sun. NASA · S65-30433, https://images.nasa.gov/details/S65-30433. Public domain.
- `ui/archive/pia14033.jpg`: Mariner 4: the first pictures of Mars. The first picture of Mars, colored by hand at JPL from strips of printed numbers before the processed image was ready. NASA/JPL-Caltech/Dan Goods · PIA14033, https://images.nasa.gov/details/PIA14033. Public domain.
- `ui/archive/pia14032.jpg`: Mariner 4: the first pictures of Mars. The first television picture of Mars, released on 15 July 1965, with its contrast enhanced. NASA/JPL · PIA14032, https://images.nasa.gov/details/PIA14032. Public domain.
- `ui/archive/pia02979.jpg`: Mariner 4: the first pictures of Mars. Frame 11E: Mariner crater, 151 km across, with a long ridge running across its floor. NASA/JPL · PIA02979, https://images.nasa.gov/details/PIA02979. Public domain.
- `ui/archive/luna9close_big.jpg`: Luna 9: the first pictures from the Moon. A close-up of the ground beside Luna 9 in the Ocean of Storms, February 1966: small stones and pitted soil. Luna 9 (USSR, 1966). Image via NASA/NSSDC · luna9close_big, https://nssdc.gsfc.nasa.gov/planetary/lunar/luna9close_big.gif. A Soviet image (1966), republished by NASA's NSSDC. Used as a photograph without creative input whose 50-year term under German law (§ 72(3) UrhG) has run out; recheck before the repository goes public.
- `ui/archive/surveyor_1_4_foot_pad.jpg`: Surveyor 1: the first soft landing. One of Surveyor 1's footpads resting on the lunar soil, among its first pictures from the surface. NASA · Surveyor 1, https://www.nasa.gov/wp-content/uploads/2021/05/surveyor_1_4_foot_pad.jpg. Public domain.
- `ui/archive/surveyor_1_7_mosaic_image.jpg`: Surveyor 1: the first soft landing. Surveyor 1's TV frames pieced together into a view of its landing site inside the crater Flamsteed P. NASA · Surveyor 1, https://www.nasa.gov/wp-content/uploads/2021/05/surveyor_1_7_mosaic_image.jpg. Public domain.
- `ui/archive/pia02976.jpg`: Surveyor 1: the first soft landing. Surveyor 1's own shadow stretched across the ground in the late lunar afternoon, with the horizon at upper right. NASA/JPL · PIA02976, https://images.nasa.gov/details/PIA02976. Public domain.
- `ui/archive/s66-32144.jpg`: Surveyor 1: the first soft landing. On landing day, astronauts Tom Stafford (right) and Gene Cernan look over pictures from Surveyor 1. NASA · S66-32144, https://images.nasa.gov/details/S66-32144. Public domain.
- `ui/archive/lrc-1966-b701_p-07825.jpg`: Lunar Orbiter 1: the Earth from the Moon. The first picture of the Earth from the Moon’s vicinity, 23 August 1966, a crescent Earth above the far side. NASA/Langley · lo1_h102_123, https://images.nasa.gov/details/lo1_h102_123. Public domain.
- `ui/archive/lrc-1967-b701_p-01194.jpg`: Lunar Orbiter 1: the Earth from the Moon. Lunar Orbiter 2 looks north across the crater Copernicus on 24 November 1966, the “picture of the century.” NASA/Langley · lo2_h162_123, https://images.nasa.gov/details/lo2_h162_123. Public domain.
- `ui/archive/as08-14-2383.jpg`: Apollo 8: Earthrise. The color Earthrise, taken by Bill Anders on the fourth orbit, 24 December 1968. NASA · AS08-14-2383, https://images.nasa.gov/details/AS08-14-2383. Public domain.
- `ui/archive/as08-13-2329.jpg`: Apollo 8: Earthrise. The first Earthrise ever photographed, on black-and-white film, moments before the color frames. NASA · AS08-13-2329, https://images.nasa.gov/details/AS08-13-2329. Public domain.
- `ui/archive/as08-14-2453.jpg`: Apollo 8: Earthrise. Craters on craters: the far side of the Moon, seen soon after Apollo 8 entered lunar orbit. NASA · AS08-14-2453, https://images.nasa.gov/details/AS08-14-2453. Public domain.
- `ui/archive/as08-17-2670.jpg`: Apollo 8: Earthrise. Looking almost straight down on the far side, near 162 degrees west and 6 degrees south. NASA · AS08-17-2670, https://images.nasa.gov/details/AS08-17-2670. Public domain.
- `audio/archive/apollo-8-genesis.ogg`: Apollo 8: Earthrise. The Genesis reading, 24 December 1968. NASA, via NSSDC, https://nssdc.gsfc.nasa.gov/planetary/. Public domain.
- `ui/archive/as11-40-5903.jpg`: Apollo 11: Tranquility Base. Buzz Aldrin walks beside a leg of the lunar module Eagle, photographed by Neil Armstrong. NASA · AS11-40-5903, https://images.nasa.gov/details/AS11-40-5903. Public domain.
- `ui/archive/as11-40-5878.jpg`: Apollo 11: Tranquility Base. A bootprint pressed into the fine lunar soil at Tranquility Base. NASA · AS11-40-5878, https://images.nasa.gov/details/AS11-40-5878. Public domain.
- `ui/archive/as11-40-5875.jpg`: Apollo 11: Tranquility Base. Aldrin stands beside the flag, with Eagle at the left and the crew's footprints all around. NASA · AS11-40-5875, https://images.nasa.gov/details/AS11-40-5875. Public domain.
- `ui/archive/as11-44-6581.jpg`: Apollo 11: Tranquility Base. Eagle, legs out for landing, seen from Columbia in lunar orbit before the descent. NASA · AS11-44-6581, https://images.nasa.gov/details/AS11-44-6581. Public domain.
- `ui/archive/as11-44-6642.jpg`: Apollo 11: Tranquility Base. Eagle's ascent stage comes back to Columbia on 21 July, with the Earth rising behind it. NASA · AS11-44-6642, https://images.nasa.gov/details/AS11-44-6642. Public domain.
- `audio/archive/apollo-11-eagle-has-landed.ogg`: Apollo 11: Tranquility Base. Neil Armstrong on 20 July 1969: "Tranquility Base here. The Eagle has landed." NASA, via NSSDC, https://nssdc.gsfc.nasa.gov/planetary/. Public domain.
- `ui/archive/pia12984.jpg`: Luna 16: Moon soil by robot. Luna 16’s landing stage, still standing in the Sea of Fertility, seen by NASA’s Lunar Reconnaissance Orbiter (inset enlarged). NASA/GSFC/Arizona State University · PIA12984, https://images.nasa.gov/details/PIA12984. Made by the LROC team at Arizona State University for NASA and republished by NASA; LROC's own terms (https://www.lroc.asu.edu/about/terms) reserve such products, free for educational use with this credit: a low to medium risk.
- `ui/archive/luna16.jpg`: Luna 16: Moon soil by robot. A Luna 16-type spacecraft on Earth: the landing stage with its round tanks below, the return rocket above. Luna 16 (USSR, c. 1970). Image via NASA/NSSDC · luna16, https://nssdc.gsfc.nasa.gov/image/spacecraft/luna16.jpg. A Soviet photo, republished by NASA's NSSDC; its first publication date is unverified, so a low to medium risk. Used as a photograph without creative input whose 50-year term under German law (§ 72(3) UrhG) has run out; recheck before the repository goes public.
- `ui/archive/m175502049rc.jpg`: Lunokhod 1: a rover on the Moon. Luna 17’s landing stage on Mare Imbrium in 2011, with Lunokhod 1’s dark wheel tracks still looping around it. NASA/GSFC/Arizona State University · M175502049RC, https://pds.lroc.im-ldi.com/data/LRO-L-LROC-3-CDR-V1.0/LROLRC_1009/EXTRAS/BROWSE/2011313/M175502049RC_pyr.tif. An LRO camera frame from NASA's Planetary Data System archive, public domain under LROC's terms (https://www.lroc.asu.edu/about/terms). Cropped and mirrored so east is right.
- `ui/archive/lunokhod.jpg`: Lunokhod 1: a rover on the Moon. A Lunokhod rover on Earth: the tub-shaped body, the lid that held its solar cells, and eight wheels. Lunokhod (USSR, c. 1970). Image via NASA/NSSDC · lunokhod, https://nssdc.gsfc.nasa.gov/image/spacecraft/lunokhod.jpg. A Soviet photo, republished by NASA's NSSDC (from the Lavochkin association); its first publication date is unverified, so a low to medium risk. Used as a photograph without creative input whose 50-year term under German law (§ 72(3) UrhG) has run out; recheck before the repository goes public.
- `ui/archive/as15-86-11603.jpg`: Apollo 15: driving on the Moon. James Irwin at the rover on the first moonwalk, with Mount Hadley behind and Falcon's shadow in front. NASA · AS15-86-11603, https://images.nasa.gov/details/AS15-86-11603. Public domain.
- `ui/archive/as15-85-11425.jpg`: Apollo 15: driving on the Moon. Hadley Rille, looking northwest from near St. George crater, with layers in its wall and boulders on its floor. NASA · AS15-85-11425, https://images.nasa.gov/details/AS15-85-11425. Public domain.
- `ui/archive/as15-88-11866.jpg`: Apollo 15: driving on the Moon. Irwin salutes beside the flag, with Falcon, the rover and the 4,000-meter bulk of Hadley Delta behind. NASA · AS15-88-11866, https://images.nasa.gov/details/AS15-88-11866. Public domain.
- `ui/archive/s71-42955.jpg`: Apollo 15: driving on the Moon. The Genesis Rock, sample 15415, in the Lunar Receiving Laboratory in Houston, August 1971. NASA · S71-42955, https://images.nasa.gov/details/S71-42955. Public domain.
- `ui/archive/s71-43788.jpg`: Apollo 15: driving on the Moon. From the rover's TV camera: Scott watches a hammer and a falcon feather strike the ground together. NASA · S71-43788, https://images.nasa.gov/details/S71-43788. Public domain.
- `audio/archive/apollo-15-hammer-and-feather.ogg`: Apollo 15: driving on the Moon. David Scott on 2 August 1971 drops a hammer and a feather for the TV camera: "How about that?" NASA, via NSSDC, https://nssdc.gsfc.nasa.gov/planetary/. Public domain.
- `ui/archive/pia02995.jpg`: Mariner 9: the first orbiter of another planet. On the approach, 11 November 1971: a dark spot above the dust, later known as Olympus Mons. NASA/JPL · PIA02995, https://images.nasa.gov/details/PIA02995. Public domain.
- `ui/archive/pia02999.jpg`: Mariner 9: the first orbiter of another planet. The summit crater of Ascraeus Mons stands above the dust storm, 17 December 1971. NASA/JPL · PIA02999, https://images.nasa.gov/details/PIA02999. Public domain.
- `ui/archive/pia02998.jpg`: Mariner 9: the first orbiter of another planet. The great canyon system appears as the dust storm clears. NASA/JPL · PIA02998, https://images.nasa.gov/details/PIA02998. Public domain.
- `ui/archive/m09_mtvs4109_09.jpg`: Mariner 9: the first orbiter of another planet. Phobos from 5,760 km: a cratered rock about 26 km long. NASA/JPL · MTVS 4109-9, https://nssdc.gsfc.nasa.gov/imgcat/html/object_page/m09_mtvs4109_09.html. Public domain.
- `ui/archive/arc-1973-ac73-9253.jpg`: Pioneer 10: the first to Jupiter. Jupiter on 2 December 1973, from about 2.58 million km, with one of its moons at upper right. NASA Ames Research Center · ARC-1973-AC73-9253, https://images.nasa.gov/details/ARC-1973-AC73-9253. Public domain.
- `ui/archive/arc-1973-ac73-9278.jpg`: Pioneer 10: the first to Jupiter. Jupiter partly in shadow, from about 354,000 km on 3 December 1973, the day before closest approach. NASA Ames Research Center · ARC-1973-AC73-9278, https://images.nasa.gov/details/ARC-1973-AC73-9278. Public domain.
- `ui/archive/arc-1973-ac73-9279.jpg`: Pioneer 10: the first to Jupiter. Ganymede, Jupiter's largest moon, as Pioneer 10's photopolarimeter saw it. NASA Ames Research Center · ARC-1973-AC73-9279, https://images.nasa.gov/details/ARC-1973-AC73-9279. Public domain.
- `ui/archive/arc-1972-ac72-1338.jpg`: Pioneer 10: the first to Jupiter. The plaque Pioneer 10 carries, designed by Carl Sagan and Frank Drake and drawn by Linda Salzman Sagan. NASA Ames Research Center · ARC-1972-AC72-1338, https://images.nasa.gov/details/ARC-1972-AC72-1338. Designed by Carl Sagan and Frank Drake, drawn by Linda Salzman Sagan; NASA distributes it with no copyright mark (a small residual risk).
- `ui/archive/pia03103.jpg`: Mariner 10: Mercury up close. Mercury on the approach, 29 March 1974: 18 frames taken about six hours before the first flyby. NASA/JPL · PIA03103, https://images.nasa.gov/details/PIA03103. Public domain.
- `ui/archive/pia23791.jpg`: Mariner 10: Mercury up close. Venus a few days after the flyby, from images in orange and ultraviolet light, shown as taken and with contrast enhanced. NASA/JPL-Caltech · PIA23791, https://images.nasa.gov/details/PIA23791. Public domain.
- `ui/archive/pia02961.jpg`: Mariner 10: Mercury up close. Old and fresh craters, taken minutes after Mariner 10's closest approach to Mercury on 29 March 1974. NASA/JPL · PIA02961, https://images.nasa.gov/details/PIA02961. Public domain.
- `ui/archive/pia02962.jpg`: Mariner 10: Mercury up close. Half of the Caloris basin, about 1,300 km across; the other half lay in darkness beyond the line of sunset. NASA/JPL · PIA02962, https://images.nasa.gov/details/PIA02962. Public domain.
- `ui/archive/v13_vg261_262.jpg`: Venus's surface: Venera 9 and Venera 13. Venera 13's color panorama: flat rock slabs and soil, with part of the lander and its cast-off lens cover in front. Venera 13 (USSR, 1982). Image via NASA/NSSDC · v13_vg261_262, https://nssdc.gsfc.nasa.gov/imgcat/html/object_page/v13_vg261_262.html. A Soviet image, republished by NASA's NSSDC, which notes it is not necessarily in the public domain. Protected in Germany until 2032 (§ 72(3) UrhG): a medium risk; recheck before the repository goes public.
- `ui/archive/pia00381.jpg`: Viking 1: on the surface of Mars. The first picture taken on Mars, minutes after landing: rocks, sand and dust beside footpad 2. NASA/JPL · PIA00381, https://images.nasa.gov/details/PIA00381. Public domain.
- `ui/archive/pia00382.jpg`: Viking 1: on the surface of Mars. Viking 1's first panorama, with lander hardware in front and a horizon about 3 km away. NASA/JPL · PIA00382, https://images.nasa.gov/details/PIA00382. Public domain.
- `ui/archive/pia00563.jpg`: Viking 1: on the surface of Mars. The first color picture from the surface, taken on 21 July 1976 and balanced against a test chart on the lander. NASA/JPL · PIA00563, https://images.nasa.gov/details/PIA00563. Public domain.
- `ui/archive/p11-saturn-titan.jpg`: Pioneer 11: the first to Saturn. Saturn, its rings and Titan, the dot below the planet, from about 2.8 million km on the approach. NASA Ames Research Center · Pioneer 11, 1979, https://assets.science.nasa.gov/content/dam/science/psd/solar/internal_resources/730/Saturn_and_its_rings.jpeg. Public domain.
- `ui/archive/p11-jupiter-north.jpg`: Pioneer 11: the first to Saturn. Jupiter seen from above its north pole as Pioneer 11 climbed away in December 1974, a view no telescope on Earth can have. NASA · Pioneer 11, 1974, https://assets.science.nasa.gov/content/dam/science/psd/solar/internal_resources/2551/Fuzzy_color_image_of_Jupiter.jpeg. Public domain.
- `ui/archive/arc-1979-ac79-9114-57.jpg`: Pioneer 11: the first to Saturn. Pioneer's mission control at NASA Ames in August 1979, a month before the spacecraft reached Saturn. NASA Ames Research Center · ARC-1979-AC79-9114-57, https://images.nasa.gov/details/ARC-1979-AC79-9114-57. Public domain.
- `ui/archive/venera-maxwell.jpg`: Venera 15 and 16: radar maps of Venus. Maxwell Montes in Venera 15 and 16 radar, 1983: steep slopes drop west to the high plateau of Lakshmi Planum. Venera 15/16 (USSR, 1983). Image via NASA PDS Geosciences Node · venera-maxwell, https://pds-geosciences.wustl.edu/venera/GIF/venera-maxwell.gif. A Soviet radar image (1983-84, IRE Moscow, courtesy of the Vernadsky Institute and the Moscow Power Engineering Institute), republished by NASA's PDS Geosciences Node. Protected in Germany until about 2034 (§ 72(3) UrhG): a medium risk; recheck before the repository goes public.
- `ui/archive/pia00465.jpg`: Venera 15 and 16: radar maps of Venus. One patch of Venus by Magellan in 1991 (left) and by Venera 15/16 in the 1980s (right): the hills are small volcanoes. NASA/JPL · PIA00465, https://assets.science.nasa.gov/content/dam/science/psd/photojournal/pia/pia00/pia00465/PIA00465.jpg. NASA/JPL's comparison of Magellan's radar with Venera 15/16's (1983-84): the Venera half is Soviet data, protected in Germany until about 2034, a medium risk.
- `ui/archive/pia00452.jpg`: Voyager 1: the Pale Blue Dot. The Pale Blue Dot: the Earth, less than a pixel across, inside a band of scattered sunlight. NASA/JPL-Caltech · PIA00452, https://images.nasa.gov/details/PIA00452. Public domain.
- `ui/archive/pia23645.jpg`: Voyager 1: the Pale Blue Dot. Pale Blue Dot Revisited: JPL's 2020 processing of the same data for the picture's 30th anniversary. NASA/JPL-Caltech · PIA23645, https://images.nasa.gov/details/PIA23645. Public domain.
- `ui/archive/pia00451.jpg`: Voyager 1: the Pale Blue Dot. The family portrait: the frames Voyager 1 took on 14 February 1990, with the Sun at the center. NASA/JPL-Caltech · PIA00451, https://images.nasa.gov/details/PIA00451. Public domain.
- `ui/archive/5663627.jpg`: Explorer 1: the Van Allen belts. Pickering, Van Allen and von Braun hold up a full-size model of Explorer 1 at the National Academy of Sciences in Washington, early on 1 February 1958. NASA/JPL · 5663627, https://images.nasa.gov/details/5663627. Public domain.
- `ui/archive/6414024.jpg`: Explorer 1: the Van Allen belts. Explorer 1 being fitted to its Jupiter-C launch vehicle in January 1958. NASA/MSFC · 6414024, https://images.nasa.gov/details/6414024. Public domain.
- `ui/archive/s61-479.jpg`: Explorer 1: the Van Allen belts. An early NASA drawing of the inner and outer Van Allen belts. NASA · S61-479, https://www.nasa.gov/solar-system/studying-the-van-allen-belts-60-years-after-americas-first-spacecraft/. Public domain.
- `ui/archive/strelka.jpg`: Korabl-Sputnik 2: Belka and Strelka come home. A dog in orbit on the ship’s television, August 1960; NASA’s catalog names her Strelka. Korabl-Sputnik 2 (USSR, 1960). Image via NASA/NSSDC · strelka, https://nssdc.gsfc.nasa.gov/image/spacecraft/strelka.jpg. A Soviet image (1960), republished by NASA's NSSDC. Used as a photograph without creative input whose 50-year term under German law (§ 72(3) UrhG) has run out; recheck before the repository goes public.
- `ui/archive/6519204.jpg`: Pegasus: wings to catch meteoroids. Technicians at Fairchild check a Pegasus detector wing at full extension, before the first launch in 1965. NASA/MSFC · 6519204, https://images.nasa.gov/details/6519204. Public domain.
- `ui/archive/6518485.jpg`: Pegasus: wings to catch meteoroids. Pegasus 1, folded inside its Apollo service module shroud, on top of the Saturn I second stage at Cape Kennedy, 13 January 1965. NASA/MSFC · 6518485, https://images.nasa.gov/details/6518485. Public domain.
- `ui/archive/s65-42437.jpg`: Pegasus: wings to catch meteoroids. The first night launch of a Saturn, carrying Pegasus 2 at 2:35 a.m. on 25 May 1965. NASA · S65-42437, https://images.nasa.gov/details/S65-42437. Public domain.
- `ui/archive/pioneer6-9.jpg`: Pioneers 6 to 9: weather reports from the Sun. A painting of the spacecraft design shared by Pioneers 6 to 9, a spinning drum with long booms. NASA · pioneer6-9, https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1965-105A. Public domain.
- `ui/archive/pioneer_solar_weather_console.jpg`: Pioneers 6 to 9: weather reports from the Sun. The Pioneer space weather console at Ames during Apollo 10 in May 1969, showing readings from Pioneers 8 and 9. NASA Ames Research Center · pioneer_solar_weather_console, https://www.nasa.gov/history/50-years-ago-pioneer-8-studies-interplanetary-space/. Public domain.
- `ui/archive/arc-1966-a-37949-1.jpg`: Pioneers 6 to 9: weather reports from the Sun. Ames staff in the data room of the Pioneer tape processing station, 29 November 1966. NASA Ames Research Center · ARC-1966-A-37949-1, https://images.nasa.gov/details/ARC-1966-A-37949-1. Public domain.
- `ui/archive/lunar_orbiter_render.jpg`: Lunar Orbiter: counting meteoroid hits. A NASA rendering of a Lunar Orbiter; the meteoroid cells were mounted around the middle deck, which also carried the propellant tanks. NASA/NSSDCA · lunar_orbiter_render, https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=1966-073A. Public domain.
- `ui/archive/lrc-1967-b701_p-01853.jpg`: Lunar Orbiter: counting meteoroid hits. Lunar Orbiter 3 lifts off on an Atlas-Agena D from Cape Kennedy on 5 February 1967. NASA/Langley · LRC-1967-B701_P-01853, https://images.nasa.gov/details/LRC-1967-B701_P-01853. Public domain.
- `ui/archive/mariner-5-spacecraft.jpg`: Venera 4 and Mariner 5: into the air of Venus. Mariner 5, the rebuilt spare of Mariner 4, with its four solar panels spread. NASA/JPL · Mariner 5, 1967, https://www.nasa.gov/history/55-years-ago-mariner-5-explores-venus/. Public domain.
- `ui/archive/6757466.jpg`: Venera 4 and Mariner 5: into the air of Venus. The Atlas-Agena carrying Mariner 5 on its pad at Cape Kennedy, 14 June 1967. NASA · 6757466, https://images.nasa.gov/details/6757466. Public domain.
- `ui/archive/venera_4_capsule.jpg`: Venera 4 and Mariner 5: into the air of Venus. Venera 4’s descent capsule, the sphere that sent back the first readings from inside the air of Venus. Venera 4 (USSR, 1967). Image via NASA/NSSDC · venera_4_capsule, https://nssdc.gsfc.nasa.gov/planetary/image/venera_4_capsule.jpg. A Soviet photo, republished by NASA’s NSSDC, which notes it is not necessarily in the public domain. Publication date unverified; if published by 1975, out of German protection (§ 72(3) UrhG): a low to medium risk; recheck before the repository goes public.
- `ui/archive/pia16587.jpg`: Mascons: the Moon’s uneven pull. The Moon’s gravity mapped by GRAIL in 2012, far side in the center: red marks extra mass, and the round red spots on the near side, at both edges, mark the mascons under the maria. NASA/ARC/MIT · PIA16587, https://images.nasa.gov/details/PIA16587. Public domain (NASA mission product; MIT co-credited as the GRAIL principal investigator's institution).
- `ui/archive/as12-51-7507.jpg`: Mascons: the Moon’s uneven pull. Apollo 12’s lunar module Intrepid in lunar orbit on 19 November 1969, seen from Yankee Clipper before its descent to the Ocean of Storms. NASA · AS12-51-7507, https://images.nasa.gov/details/as12-51-7507. Public domain.
- `ui/archive/zond5_earth.jpg`: Zond 5: tortoises around the Moon. The whole Earth photographed by Zond 5 from about 90,000 km, September 1968. Zond 5 (USSR, 1968). Image via NASA, credited RKK Energiya · zond5_earth, https://www.nasa.gov/history/50-years-ago-on-the-way-to-the-moon-0/. Cut from the right half of https://www.nasa.gov/wp-content/uploads/2018/09/zond-5.jpg (pixels 609 to 917 across, full height). A Soviet image (1968; first publication date unverified), republished by NASA. Used as a photograph without creative input whose 50-year term under German law (§ 72(3) UrhG) has run out if it was published by 1975; recheck before the repository goes public.
- `ui/archive/zond5_splashdown.jpg`: Zond 5: tortoises around the Moon. Zond 5’s capsule afloat in the Indian Ocean after splashdown, September 1968. Zond 5 (USSR, 1968). Image via NASA, credited RKK Energiya · zond5_splashdown, https://www.nasa.gov/history/50-years-ago-on-the-way-to-the-moon-0/. Cut from the left half of https://www.nasa.gov/wp-content/uploads/2018/09/zond-6.jpg (pixels 0 to 456 across, full height). A Soviet image (1968; first publication date unverified), republished by NASA. Used as a photograph without creative input whose 50-year term under German law (§ 72(3) UrhG) has run out if it was published by 1975; recheck before the repository goes public.
- `ui/archive/pia02981.jpg`: Mariners 6 and 7: the thin air of Mars. Mariner 7 closing in on Mars in early August 1969, the bright south polar cap at the bottom of both views. NASA/JPL · PIA02981, https://images.nasa.gov/details/PIA02981. Public domain.
- `ui/archive/pia02989.jpg`: Mariners 6 and 7: the thin air of Mars. The south polar region from Mariner 7 in August 1969 (bottom), covered in dry ice, and from Mariner 9 through the 1971 dust storm (top). NASA/JPL · PIA02989, https://images.nasa.gov/details/PIA02989. Public domain.
- `ui/archive/69pc-23.jpg`: Mariners 6 and 7: the thin air of Mars. Mariner 6 leaves Cape Kennedy on an Atlas-Centaur on the evening of 24 February 1969, Florida time. NASA · 69PC-23, https://images.nasa.gov/details/69PC-23. Public domain.
- `ui/archive/ntrs-19700024873-fig4.jpg`: Orbiting Frog Otolith: the inner ear in orbit. A cutaway of the Orbiting Frog Otolith spacecraft, with the frogs’ package under its thermal shroud, 1970. NASA · News Release 70-132, figure 4, https://ntrs.nasa.gov/citations/19700024873 (PDF page 14, rotated and cropped to the drawing). Public domain.
- `ui/archive/ntrs-19700024873-fig5.jpg`: Orbiting Frog Otolith: the inner ear in orbit. Inside the Frog Otolith Experiment Package: the water-filled centrifuge and its life support, 1970. NASA · News Release 70-132, figure 5, https://ntrs.nasa.gov/citations/19700024873 (PDF page 16, rotated and cropped to the drawing). Public domain.
- `ui/archive/as12-48-7133.jpg`: Surveyor 3: digging into the Moon. Pete Conrad at Surveyor 3 on 20 November 1969, examining its TV camera before removing it, with Intrepid on the crater rim behind. NASA · AS12-48-7133, https://images.nasa.gov/details/AS12-48-7133. Public domain.
- `ui/archive/surveyor3_sp146_fig4-10.jpg`: Surveyor 3: digging into the Moon. Surveyor 3’s own view of its third trench, widened, with the scoop poised for a bearing test at the bottom, 28 April 1967. NASA/JPL · SP-146, fig. 4-10, https://ntrs.nasa.gov/citations/19670023253. Cut from the scanned page 74 (PDF page 81, embedded scan 2362 × 3163 px, crop x 346 to 2192, y 307 to 2267). Public domain.
- `ui/archive/as12-48-7110.jpg`: Surveyor 3: digging into the Moon. One of Surveyor 3’s footpads and its scoop, photographed by Apollo 12 on 20 November 1969, beside the imprint the pad left when the lander bounced. NASA · as12-48-7110, https://images.nasa.gov/details/as12-48-7110. Public domain.
- `ui/archive/pia00389.jpg`: Vikings 1 and 2: the soil of Mars. Viking 1’s first trench, dug on 28 July 1976: about 15 cm long, its walls standing steep in the dry soil. NASA/JPL · PIA00389, https://images.nasa.gov/details/PIA00389. Public domain.
- `ui/archive/pia00390.jpg`: Vikings 1 and 2: the soil of Mars. Viking 1’s footpad 3, hidden under loose soil after sinking about 13 cm, seen on 1 August 1976. NASA/JPL · PIA00390, https://images.nasa.gov/details/PIA00390. Public domain.
- `ui/archive/pia00145.jpg`: Vikings 1 and 2: the soil of Mars. Viking 2’s arm digs through a thin crust nicknamed “Bonneville Salt Flats” for the organic analyzer, 25 September 1976. NASA/JPL · PIA00145, https://images.nasa.gov/details/PIA00145. Public domain.
- `ui/archive/arc-1977-ac77-0376-8.jpg`: Pioneer Venus 2: four probes at once. Project manager Charlie Hall with the Multiprobe at Hughes Aircraft in December 1976: the large probe in the middle, three small probes around it. NASA Ames Research Center · ARC-1977-AC77-0376-8, https://images.nasa.gov/details/ARC-1977-AC77-0376-8. Photographer listed as TRW; released by NASA Ames. Public domain.
- `ui/archive/arc-1978-ac78-9245.jpg`: Pioneer Venus 2: four probes at once. Paul Hudson’s painting of the Multiprobe’s bus after it had released its four probes toward Venus. NASA Ames Research Center/Paul Hudson · ARC-1978-AC78-9245, https://images.nasa.gov/details/ARC-1978-AC78-9245. Painted by Paul Hudson; NASA distributes it with no copyright mark (a small residual risk).
- `audio/archive/pale-blue-dot-sagan.ogg`: Voyager 1: the Pale Blue Dot. Carl Sagan reading his Pale Blue Dot passage. Library of Congress, Seth MacFarlane Collection of the Carl Sagan and Ann Druyan Archive, item cosmos000110, https://www.loc.gov/item/cosmos000110/. © Druyan-Sagan Associates. Not public domain and used without a licence (the user's decision, 2026-10-13, design.md 1.1): taken out at once if the estate objects.
