# TreeCuts session context (2026-11-22)

File: `kotlin/src/main/kotlin/growth/TreeCuts.kt` — polar tree-ring growth sketch. All changes below are
uncommitted on `master`; `./gradlew compileKotlin` passes. **None of it has been run/viewed yet.**

## 1. Perlin noise for ring randomness (`yearWidths`, `grow`)
- Climate (ring width): `map(noise(1000 + i*0.15), 0.25, 0.75, 0.5, 1.5)` — replaced AR(1) `random()`.
- Droughts: `drought = constrain((noise(2000 + i*0.6) - 0.66) / 0.08, 0, 1)`, `climate *= 1 - 0.65*drought`
  (replaced 4% `prob()`). Threshold 0.66 is a guess — tune if droughts too frequent/rare.
- Latewood jitter: `0.3 * (noise(3000 + i*0.7) - 0.5)`.
- Lean: `lean0 + 3 * (noise(4000 + i*0.04) - 0.5)` (was a random walk); `lean0 = random(TWO_PI)`.
- Still `random()`: pith center offset, bark stroke jitter, ray placement.
- Widths multiplied by `max(climate, 0.1)` to stay positive.

## 2. Medullary rays — first as lines, then reworked into ring discontinuities
Current design (lines version is gone):
- `Ray(angle, start, end, id)`; `growRays(rings)` adds rays per year to keep ~one per `raySpacing` px (14)
  of circumference; 50% run to the last ring, others end at `start + 3 + random(...)`. `id = 6000 + n*7.3`.
- `rayStrength(ray, k)` = `constrain((noise(id, k*0.4) - 0.45) / 0.2, 0, 1)` inside [start, end], else 0 —
  most years zero, so rays appear intermittently.
- `cutRings(center, rings, innerBark, rays)`:
  - bulge: ring bulges outward near the ray, `amp = 3*st` px, reach `(3 + 8*st)` px, falloff `(1-|da|/reach)^2`,
    per-side skew `2*noise(id+50, k*0.7) - 1` so the two broken ends don't line up; clamped to
    `0.45 * gap to next ring` (inner bark for last ring) → rings never cross.
  - cut: gap half-width `(0.6 + 1.2*st)` px around the ray angle; earlywood lines (lerp 0.35/0.7 between bent
    rings) use the same cuts as their year's ring.
- `ringPolylines(center, r, cuts)`: open x,y polylines with exact interpolated endpoints at cut edges
  (normalises, sorts, merges overlapping/wrapping cuts). Uncut ring = one closed-by-duplicate polyline.
- `Tree` now holds `ringPaths` and `earlyPaths` (List<List<FloatArray>>) instead of the old `rays` lines;
  `draw()` just strokes those via `drawPath`. Bark still uses `drawRing`.
- Rays are generated even when disabled (keeps the random sequence stable); `drawRays=false` passes no rays to
  `cutRings`.

## Keys (in addition to ProcessingAppK's x/p/space/R/S/V/c)
`1/2` eccentricity, `3/4` years, `5/6` taper, `7/8` bark thickness, `9/0` ray spacing, `r` toggle rays
(regrows), `e` toggle earlywood.

## GUI (LazyGui)
- Parameters are `LazyGuiControlDelegate` properties (sliders with ranges, toggles); the delegate gained optional
  `min`/`max` args for `slider`/`sliderInt`. The sketch now loops (needed for the gui); `draw()` reads a `Params`
  snapshot each frame and regrows only when it differs from the last one (`reset()` forces a regrow).
- Keys still work and just set the gui values.
- `noise octaves` / `noise falloff` sliders feed `noiseDetail()`, applied in `regrow()` (was fixed at 4, 0.5 in setup).

## Constraints / conventions to keep in mind
- Output must stay SVG-path friendly (plotter) — new CLAUDE.md rule.
- noLoop() + event thread: draw() reads only the immutable `Tree` snapshot swapped in by `regrow()`.
- Commit messages prefixed `[growth] ...`.

## Next steps
- Run the sketch and tune: ray threshold/range (0.45/0.2), bulge amp, reach, gap width, drought threshold,
  climate input range (0.25–0.75).
- Commit when happy.
