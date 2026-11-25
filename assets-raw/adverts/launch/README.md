# The 1.0 launch's poster and banner

New versions of `../launch-window-poster.png` (2400x3600, the Teams announcement) and
`../launch-window-banner.png` (1920x1080, the wiki's landing page) for when 1.0 is out: "1.0 out now... we
have ignition!" (the user, 2026-11-25). The old ones stay beside them.

- `poster.html`: the Saturn V liftoff (trailer shot c28, its picked keyframe) full height, the sky deepening into
  space under the headline; the logo, name, tagline and a one-line pitch at the foot.
- `banner.html`: the boot about to step down (shot c36) large on the right, the name and "1.0 out now" on the
  left, and a film strip of the journey before that step: liftoff (c28), touchdown (c34), the ladder (c35).
- `make_art.py` (ComfyUI running): the keyframes are only 1344x768, so it makes bigger versions with the
  trailer's Qwen Image 2.1 workflow. The poster's is the keyframe itself set on a 1600x2400 canvas with its sky
  carried up and feathered in, refined with a light denoise (`poster_i2i_*`); `poster_t2i_*` are fresh renders
  of the same prompt, kept as an alternative. The banner's frames are the keyframes refined at 2x. Output goes
  to `out/` (ignored: only picked files are committed, with `git add -f`).
- `picks.json`: the picture per role, and `_poster_drop` (px) to lift or lower the poster's picture.
- `render.py`: copies the picks to `img/` and renders both pages with Playwright to
  `../launch-window-poster-1.0.png` and `../launch-window-banner-1.0.png`.

Fonts and the logo are the game's own (`assets/fonts`, `assets/branding/logo-outlined.svg`).
