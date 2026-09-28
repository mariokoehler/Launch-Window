# Launch Window logo

`logo.svg` is the approved mission-patch logo (`design.md` §3.12/§4,
decided 2026-09-28), built entirely from this repo's own design system —
no traced or generated bitmap art, no BASPM content. A rocket ascending
past Earth's limb into a starfield, with a partial gold arc marking "the
window" arcing over its nose toward an accent star; a Saturn V-style
tapered body with its own black roll-pattern stripe (not a generic
rocket); a mission-patch frame with the wordmark and tagline on a ribbon.
Colors are the exact design-system tokens (`navy` #16305c, `gold`
#e0a72e, `success`-adjacent Earth blue via `surface-page` #3d6a9a,
`on-fill` #f5efe1, `danger` #c6402b for the literal flame only — not a
status use). Reached over several iteration rounds starting from a
user-generated reference image, refining the arc, rocket silhouette, star
placement, and ribbon spacing against specific feedback each round.

**Known follow-up work, not yet done:**
- The wordmark/tagline are still live `<text>` elements set in Archivo/
  Work Sans, not outlined paths — correct for editing, but a production
  export (rasterized icon, or an outlined vector for print/merch) needs
  the real font files first (`design.md` §3.12 already flags these as
  missing from the repo) or the text converted to outlines in a vector
  editor so rendering doesn't depend on the viewer having those fonts
  installed.
- No rasterized PNG/texture yet for actual use in the libGDX client
  (a title-screen mark, a window icon, etc.) — this SVG is the source
  to export from once a concrete use screen exists.
- This is the primary lockup (icon + wordmark + tagline). An icon-only
  cropped version (for a small app/window icon, where the full patch
  reads as just a colored dot per `design.md` §3.12's own honesty note
  about legibility at small sizes) hasn't been produced separately yet.
