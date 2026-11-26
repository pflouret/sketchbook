# Dunes session context (2026-11-26)

File: `kotlin/src/main/kotlin/lines/Dunes.kt`, sand dunes drawn as dense hidden-line strokes over a heightfield,
modelled on a reference image (a 2017 render, `2017-01-07_153520_s96955_f02561.png`): fine dark lines sweeping
across dunes, bunching into sharp dark edges at the crests. Untracked/uncommitted; `./gradlew compileKotlin`
passes. Never run in a window yet, only rendered offscreen (see below). The user commits themselves.

## How it works
- `height(p, x, z)`: world x across, z depth (0 = nearest). Transverse dunes along the wind direction
  (`along = z·cos(windAngle) + x·sin(windAngle)`), phase `u = wind·(along + warp)/period + 0.5·noise(x·0.004)`,
  `t = frac(u)`. Profile: windward `pow(t/c, 1.6)` for `t < c = windward` (flat trough, steepening to a sharp
  brink), slip face `pow(1 - (t-c)/(1-c), 1.5)`. `warp = crestWander·period·(2·noise - 1)` bends crests;
  amplitude `duneHeight·(0.3 + 1.1·noise)`; plus `swell·(2·noise - 1)` broad undulation. `windTowardsViewer`
  flips the wind sign (slip faces face the viewer).
- `grow(p)`: lines are parallel in the ground plane, `z = z0 + (x - cx)·tan(lineAngle) + bend·(2·noise - 1)`,
  so line k is in front of line k+1 everywhere and drawing in order is front to back. `z0` spans
  `zStart..zEnd` (extended by the slope so angled lines cover the near/far corners); `depth = 2.5·height/tilt`.
  Projection: `f = 1/(1 + perspective·max(z,0)/depth)`, `sx = cx + (x-cx)·f`,
  `sy = height + margin - (z·tilt + h)·f`. World x spans `±(cx+margin)·(1+perspective)` at 1 world px per
  sample, so every line covers the screen even where it's farthest away. Stops once a whole line is above the
  canvas.
- Floating horizon: `horizon[col]` = topmost screen y drawn so far. `visibleRuns` keeps the parts with
  `min(horizon - sy, sy, height - sy) > 0` (above the horizon and on the canvas), interpolating run ends onto
  the horizon/canvas edge; the horizon is raised only after the whole line, so a line never hides itself.
- Output: `paths: List<FloatArray>`, open x,y polylines of only the visible parts (SVG/plotter friendly, no
  overdraw). `draw()` strokes them with `stroke(0, alpha)`, weight 0.5.
- Same pattern as TreeCuts: the sketch loops (for LazyGui), `draw()` builds a `Params` data class each frame
  and regrows only when it changed; `reset()` (x key) reseeds and sets `grownWith = null`; `regrow` applies
  `noiseDetail(octaves, falloff)`.

## Sliders (current defaults, some edited by the user)
lines 400 (100–6000), period 420, dune height 200, windward 0.75 (0.05–0.95), wind angle 0 (±90),
crest wander 1 (0–10), swell 80, line angle 45 (±90), bend 80, tilt 0.35, perspective 0.6,
wind towards viewer (toggle), alpha 90, noise octaves 3, noise falloff 0.5. Canvas `Size(1200, 800)`.
- Note: line angle at ±90 makes `tan` blow up (and zStart/zEnd huge); keep it a bit inside that range or clamp.

## Tuning so far (offscreen renders)
- First version (horizontal lines, period 260, height 90) looked too regular and strip-like.
- Closer to the reference: lines 4000, period 420, height 200, crest wander 1, swell 80, line angle 25,
  tilt 0.35, alpha 90, wind angle ±30–35 (diagonal crests). ~1.4 s per regrow at 4000 lines, ~19k visible
  paths / 620k points.
- Fixed: angled lines left an uncovered wedge at a screen corner (x range was taken from the depth at the
  centre); now uses the widest range.
- Raster renders show some moiré in dense areas (line spacing vs pixels), not geometry.

## Checking output without opening a window
Throwaway Java harness in the session scratchpad (`check/DunesCheck.java`, not in the repo): reflectively
builds `Dunes$Params`, calls `grow`, writes an SVG, rendered with `rsvg-convert`. Classpath from a gradle init
script printing `sourceSets.main.runtimeClasspath.asPath`.

## Conventions
- Output must stay SVG-path friendly (plotter).
- Commit messages prefixed `[lines] ...`.

## Possible next steps
- Run in the window and tune; maybe lower-res preview while dragging sliders (regrow is slow at high counts).
- Layered SVG export like TreeCuts if wanted.
- Clamp line angle away from ±90.
