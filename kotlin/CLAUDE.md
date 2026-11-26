# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Generative-art sketches built on Processing, written in Kotlin. This directory is one subproject of a larger
sketchbook repo (siblings `../java`, `../ruby`, `../projects` are independent builds). Many outputs end up on a pen
plotter, so line-based drawing and SVG export matter more than raster effects.

## Build / run

- Compile: `./gradlew compileKotlin`
- There is no application plugin, test suite, or linter. Each sketch is launched from its own top-level `main()`
  (usually run from the IDE), e.g. `PApplet.runSketch(arrayOf("growth.TreeCuts"), null)` — the string must be the
  fully qualified class name.
- Processing core, LazyGui, VideoExport, themidibus, toxiclibs etc. are vendored jars in `lib/` (pulled in via
  `fileTree`), not Maven dependencies. JavaFX 17 and JOGL come from Gradle.

## Architecture

Class hierarchy for every sketch: `PApplet` → `ProcessingApp` (Java, `src/main/java/.../p5/ProcessingApp.java`) →
`ProcessingAppK` (`src/main/kotlin/p5/ProcessingAppK.kt`) → the sketch.

- `ProcessingApp` holds `seed`, `redrawOnEvent`, `saveVideo`, `makeSketchFilename()` (outputs go to
  `~/Genart/sketchout/<sketchname>/`), and drawing helpers (`humanline`, `noiseline`, `stripedTexture`, ...).
- `ProcessingAppK` adds the conventions sketches rely on:
  - `reset()` hook; `resetSeed(newSeed)` reseeds both `random` and `noise` from `seed`, so regenerating with
    `resetSeed(false)` reproduces the same piece after a parameter tweak.
  - Built-in keys: `x` reset, `p`/space toggle loop, `R` export next frame as SVG (via `pre()`/`post()`
    `beginRecord`), `S` PNG screenshot, `V` stop video. Sketches override `keyTyped` and call `super.keyTyped`.
  - Kotlin-friendly overloads (`random(Number)`, `prob`, `PVector` variants of `vertex`/`curveVertex`/`noise`),
    `withShape {}` / `withPush {}`, `w2`/`h2`/`widthf`/`heightf`.
  - `PaperSize` enum (pixel sizes at ~96dpi) for plotter paper; sketches pick a `SIZE` in their companion object.
  - Optional MIDI (`initMidi(MidiController.FIGHTER)`, mappings in `midi/` and `midi-maps/`) and LazyGui
    (`initGui()`).
- `gui/LazyGuiControlDelegate` binds a Kotlin property to a LazyGui control by property name
  (`var x: Boolean by LazyGuiControlDelegate("button")`). LazyGui autosaves state under `data/gui/<SketchName>/`.

## Gotchas

- With `noLoop()`, Processing's `postEvent` dispatches key/mouse events immediately on the window thread, concurrently
  with `draw()` on the animation thread. Don't let `draw()` read fields that event handlers mutate mid-update — build
  new state and swap it in as one immutable object (see `growth/TreeCuts.kt`).
- Commit messages are prefixed with the sketch/package in brackets, e.g. `[agents] Rotation zones.`, `[p5] ...`.
- Always keep in mind that the drawn output should be optimized to be able to be exported as svg paths, unless specified otherwise
