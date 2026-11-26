# TreeCuts session context (updated 2026-11-24)

File: `kotlin/src/main/kotlin/growth/TreeCuts.kt`, a tree-ring growth sketch (rings = polar radius arrays of
`samples` points). Work up to "smoother centers" is committed on `master`; `./gradlew compileKotlin` passes.
The user commits themselves; don't commit unless asked.

## Noise (`yearWidths`, `grow`)
- Climate (ring width): `map(noise(1000 + i*0.15), 0.25, 0.75, 0.5, 1.5)`.
- Droughts: `drought = constrain((noise(2000 + i*0.6) - 0.66) / 0.08, 0, 1)`, `climate *= 1 - 0.65*drought`.
- Latewood jitter: `0.3 * (noise(3000 + i*0.7) - 0.5)`.
- Lean: `lean0 + 3 * (noise(4000 + i*0.04) - 0.5)`; `lean0 = random(TWO_PI)`.
- Still `random()`: centre offset, pith count/placement, per-year roughness (`random() * w`, user's change),
  bark stroke jitter, ray placement.
- `noiseDetail(octaves, falloff)` comes from gui sliders, applied in `regrow()`.

## Ring growth (`growRing(old, i, w, eccentricity, lean, off)`)
- New ring = smooth(old) + `w * ecc * lobes + roughness * wobble`, smoothed again, then
  `max(..., old + minGap 0.6)` so rings never cross. `off` shifts the noise per stem.
- Smoothing window is ~8px of arc: `k = constrain(8 / (2π·meanR/samples), 3, samples/8)`, so young/small
  rings get smoothed over a wide angle and old rings stay detailed (the fix for jagged young rings).
- Wobble amplitude scaled by `constrain(meanR / 80, 0.1, 1)`.

## Medullary rays (ring discontinuities, not drawn lines)
- `Ray(angle, start, end, id)`; `growRays(rings, raySpacing, idBase)` keeps ~one ray per `raySpacing` px of
  circumference; 50% run to the last ring. idBase 6000 for shared rings, 20000 + 5000·p for stems.
- `rayStrength(ray, k)` = `constrain((noise(id, k*0.4) - 0.45) / 0.2, 0, 1)` within [start, end], so rays
  show up intermittently.
- `cutRings(center, rings, outside, rays, firstYear, latewood, extraCuts)` → `List<RingLines>`:
  - bulge outward near the ray: `amp = 3*st * constrain(meanR/80, 0.1, 1)` px, reach `(3 + 8*st)` px, falloff
    `(1-|da|/reach)^2`, per-side skew `2*noise(id+50, k*0.7) - 1`; clamped to `0.45 * gap to next ring`
    (`outside` for the last ring: inner bark, or last stem ring + half a width).
  - cut: half-width `(0.6 + 1.2*st)` px; earlywood lines (lerp 0.35/0.7 between bent rings) share the cuts.
- `ringPolylines(center, r, cuts)`: open x,y polylines with interpolated endpoints at cut edges; merges
  overlapping/wrapping cuts.

## Multiple piths (1-3, random per seed; always 1 if years < 4)
- Stems start around the centre at a head-start distance (cumulative width of a random 15-35% of the years);
  all widths scaled by R/(R+headStart) to keep the trunk inside maxR. Each stem grows its own rings (leaning
  away from the centre) until `grownTogether` reports all stems connected.
- `envelope()`: stems' outline as a polar ring around the centre (farthest exit per direction; directions
  that miss every stem are interpolated, leaving a straight-edged gap that shows as a white sliver in some
  3-pith seeds). It is ring 0 (not drawn) of the shared rings for the remaining years.
- `overlapCuts`: a stem's ring is cut where it runs into another stem's same-year ring.
- `Tree(center, rings: List<RingLines(latewood, lines, early)>, innerBark, outerBark, barkStrokes)`.
- The pith-count `random()` call changed the random sequence: seeds from before it give different trees.

## Drying cracks (`growCracks`, last in the random sequence)
- 0–3 per tree (35% none, 40% one, 20% two, 5% three), kept ≥0.6 rad apart. Each is a wedge around the
  trunk centre from `r0 = trunkR * random(0.05, 0.35)` out through the bark; half-width at the bark
  `random(3, 9) * maxR/400` px with `pow(t, 1.5)` taper; centre line wanders by noise (`crackSide`,
  `wander = 20 * maxR/400` px plus a fine zigzag).
- `clipToCracks` removes the inside parts of every ring, earlywood, bark line and bark stroke, bisecting
  crossings so pieces end exactly on the edges (closed rings cut once stay one piece). `crackOutline` draws
  the two edges as one V-shaped open polyline. `Tree` now holds bark/strokes/cracks as polyline lists (no
  `center`; `drawRing` was removed).

## Ring ticks
- `ticks(paths, spacing, length, cracks)`: short segments from each (clipped) ring line inwards (normal (-dy, dx), since ring polylines run by increasing angle),
  every `tickSpacing` px of arc (±40% jitter), length `tickLength * map(noise(8000 + x*0.015, 8000 + y*0.015), 0.25, 0.75, 0.1, 1)` (2D Perlin over the canvas); dropped if the inner end
  is in a crack. Stored per ring as `RingLines.ticks` (x0,y0,x1,y1…), drawn with `beginShape(LINES)` in the
  ring's colour at weight 0.5. Computed after the cracks, so they don't change the rest of the random sequence.
- Sliders: tick spacing 1–50 (6), tick length 0–10 (2.5; 0 = off). ~11k ticks on a 60-year tree at defaults.

## GUI (LazyGui)
- `LazyGuiControlDelegate` properties; the delegate gained optional `min`/`max` for `slider`/`sliderInt`.
- Sliders: zoom (trunk diameter as a fraction of the canvas's shorter side; sets `maxR`), years, bark thickness, eccentricity, taper, ray spacing, noise octaves (1–8), noise falloff
  (0–1), ring samples (10–800). The user has edited some ranges/defaults; check the file.
- The rays/earlywood toggles were removed; both are always drawn.
- Taper slider step is 0.01 via the new `gui.sliderPrecision(path, step)` in `gui/LazyGuiExtension.kt`
  (reflection on `SliderNode.precisionRange` / `setPrecisionIndexAndValue`; only steps 0.0001…100). The
  control must be read once first so its node exists.
- The sketch loops (the gui needs it). `draw()` builds a `Params` data class each frame and regrows only when
  it differs from `grownWith`; `reset()` (x key) sets `grownWith = null`. `samples` is a plain var set in
  `regrow()` from `params.samples`; `drawRing` uses `r.size`.

## Keys (besides ProcessingAppK's x/p/space/R/S/V/c)
`1/2` eccentricity, `3/4` years, `5/6` taper (±0.05), `7/8` bark, `9/0` ray spacing. They set the gui values.

## Checking output without opening a window
A throwaway Java harness (scratchpad, not the repo) reflectively called `grow(Params)` on a `TreeCuts` with
width/height = 1000, wrote SVGs, and rendered them with `rsvg-convert`. The classpath came from a gradle init
script printing `sourceSets.main.runtimeClasspath.asPath`.

## Conventions
- Output must stay SVG-path friendly (plotter).
- draw() reads only the immutable `Tree` snapshot.
- Commit messages prefixed `[growth] ...`.

## Possible next steps
- Tune ray threshold/range (0.45/0.2), bulge amp/reach/gap, drought threshold, climate range.
- Better handling of the gap between 3 stems (currently a straight bridge).
- Optional pith-count control (random / 1 / 2 / 3).
