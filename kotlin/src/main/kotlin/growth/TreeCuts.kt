package growth

import gui.LazyGuiControlDelegate
import p5.ProcessingAppK
import processing.core.PApplet
import processing.event.KeyEvent
import processing.core.PVector
import util.Size

// Slop galore

class TreeCuts : ProcessingAppK() {
    private val samples = 720
    private var years: Int by LazyGuiControlDelegate("sliderInt", "", 60, 1, 100)
    private var barkThickness: Float by LazyGuiControlDelegate("slider", "", 0.01f, 0.01f, 0.1f)  // fraction of the trunk radius
    private var eccentricity: Float by LazyGuiControlDelegate("slider", "", 0f, 0f, 1f)
    private var taper: Float by LazyGuiControlDelegate("slider", "", 0.7f, 0.2f, 1f)  // 1 = even ring widths, lower = wide young rings, narrow old ones
    private var raySpacing: Float by LazyGuiControlDelegate("slider", "", 14f, 3f, 100f)  // px of circumference per ray
    private var noiseOctaves: Int by LazyGuiControlDelegate("sliderInt", "", 4, 1, 8)
    private var noiseFalloff: Float by LazyGuiControlDelegate("slider", "", 0.5f, 0f, 1f)  // weight of each next octave

    /** The parameters that need a regrow, read from the gui once per frame. */
    private data class Params(
        val years: Int, val barkThickness: Float, val eccentricity: Float, val taper: Float,
        val raySpacing: Float, val noiseOctaves: Int, val noiseFalloff: Float,
    )

    private fun params() = Params(
        years, barkThickness, eccentricity, taper, raySpacing, noiseOctaves, noiseFalloff,
    )
    private var grownWith: Params? = null

    /**
     * Everything draw() needs, swapped in whole. With noLoop() Processing handles key events on
     * the window thread, so draw() can run while a regrow is changing the parameters.
     */
    private class Tree(
        val center: PVector,
        val rings: List<FloatArray>,
        val latewood: FloatArray,  // per-year darkness of the ring boundary, 0..1
        val innerBark: FloatArray,
        val outerBark: FloatArray,
        val barkStrokes: FloatArray,  // x0, y0, x1, y1 per stroke
        val ringPaths: List<List<FloatArray>>,  // per year, the ring line as open x, y polylines
        val earlyPaths: List<List<FloatArray>>,  // per year, the faint earlywood lines
    )

    /** A medullary ray, active from year [start] to [end]. [id] keys its per-year noise. */
    private class Ray(val angle: Float, val start: Int, val end: Int, val id: Float)

    @Volatile private var tree = Tree(
        PVector(), listOf(), floatArrayOf(), floatArrayOf(), floatArrayOf(), floatArrayOf(), listOf(), listOf(),
    )

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()
        initGui()
        // the gui only draws and takes input while the sketch loops
        reset()
    }

    override fun draw() {
        val params = params()
        if (params != grownWith) regrow(params)
        background(255)
        noFill()
        val tree = tree
        val latewood = tree.latewood
        for (i in 1 until tree.rings.size) {
            stroke(0f, 30f)
            strokeWeight(0.5f)
            tree.earlyPaths[i].forEach(::drawPath)
            stroke(0f, 90f + 140f * latewood[i])
            strokeWeight(0.6f + 1.4f * latewood[i])
            tree.ringPaths[i].forEach(::drawPath)
        }
        drawBark(tree)
    }

    private fun drawBark(tree: Tree) {
        if (tree.rings.isEmpty()) return
        stroke(60f, 35f, 15f, 160f)
        strokeWeight(1f)
        drawRing(tree.center, tree.innerBark)

        stroke(45f, 25f, 10f, 190f)
        strokeWeight(1.1f)
        val s = tree.barkStrokes
        for (k in s.indices step 4) line(s[k], s[k + 1], s[k + 2], s[k + 3])

        stroke(30f, 15f, 5f)
        strokeWeight(2.2f)
        drawRing(tree.center, tree.outerBark)
    }

    override fun reset() {
        resetSeed(true)
        grownWith = null  // regrow with the new seed on the next frame
    }

    private fun regrow(params: Params) {
        resetSeed(false)
        noiseDetail(params.noiseOctaves, params.noiseFalloff)
        tree = grow(params)
        grownWith = params
    }

    private fun grow(params: Params): Tree {
        val (years, barkThickness, eccentricity, taper) = params
        val center = PVector(w2 + random(-30, 30), h2 + random(-30, 30))
        val latewood = FloatArray(years + 1)
        val maxR = min(width, height) * 0.4f
        val widths = yearWidths(maxR * (1 - barkThickness), years, taper, latewood)
        val minGap = 0.6f
        val lean0 = random(TWO_PI)
        var r = FloatArray(samples) { 1.5f }
        val out = mutableListOf(r)

        for (i in 0 until years) {
            val w = widths[i]
            val roughness = 0.25f * w
            // slow drift of the lean direction over the tree's life
            val lean = lean0 + 3f * (noise(4000 + i * 0.04f) - 0.5f)

            val old = r
            val prev = smooth(old, 3)
            r = FloatArray(samples) { j ->
                val a = TWO_PI * j / samples
                val ca = cos(a)
                val sa = sin(a)
                // noise is sampled on a circle so there's no seam at angle 0; z = time
                val lobes = 0.4f + 1.2f * noise(5 + ca * 1.2f, 5 + sa * 1.2f, i * 0.06f)
                val ecc = 1 + eccentricity * cos(a - lean)
                val wobble = noise(50 + ca * 8f, 50 + sa * 8f, i * 0.3f) - 0.5f
                max(prev[j] + w * ecc * lobes + roughness * wobble, old[j] + minGap)
            }
            out.add(r)
        }
        return growBark(center, out, latewood, growRays(out, params.raySpacing), maxR * barkThickness)
    }

    /**
     * A thin pale inner bark right outside the last ring, then a thick outer bark whose thickness
     * comes from |2n - 1| noise: wide plateaus with sharp V-shaped fissures where n crosses 0.5.
     */
    private fun growBark(
        center: PVector, rings: List<FloatArray>, latewood: FloatArray, rays: List<Ray>, thickness: Float,
    ): Tree {
        val cambium = rings.last()
        val inner = FloatArray(samples) { j ->
            val a = TWO_PI * j / samples
            cambium[j] + thickness * (0.12f + 0.1f * noise(100 + cos(a) * 3f, 100 + sin(a) * 3f))
        }
        val outer = smooth(FloatArray(samples) { j ->
            val a = TWO_PI * j / samples
            val ca = cos(a)
            val sa = sin(a)
            val swell = 0.5f + noise(200 + ca * 2f, 200 + sa * 2f)
            val fissure = constrain(abs(2 * noise(300 + ca * 10f, 300 + sa * 10f) - 1) * 3f, 0f, 1f)
            inner[j] + max(1.5f, thickness * swell * sqrt(fissure))
        }, 1)

        // fibrous radial strokes across the outer bark, slightly ragged at both ends
        val strokes = FloatArray(samples / 2 * 4)
        for (k in 0 until samples / 2) {
            val j = k * 2
            val a = TWO_PI * j / samples + random(-0.002f, 0.002f)
            val band = outer[j] - inner[j]
            val r0 = inner[j] + band * random(0f, 0.15f)
            val r1 = outer[j] - band * random(0f, 0.1f)
            strokes[k * 4] = center.x + r0 * cos(a)
            strokes[k * 4 + 1] = center.y + r0 * sin(a)
            strokes[k * 4 + 2] = center.x + r1 * cos(a)
            strokes[k * 4 + 3] = center.y + r1 * sin(a)
        }
        val (ringPaths, earlyPaths) = cutRings(center, rings, inner, rays)
        return Tree(center, rings, latewood, inner, outer, strokes, ringPaths, earlyPaths)
    }

    /**
     * Medullary rays: radial bands where the rings are disrupted rather than lines drawn on top.
     * As the circumference grows, new rays start between the old ones to keep the spacing roughly
     * constant. A ray lives for a span of years, and within that span per-year noise decides how
     * strongly (if at all) it breaks that year's ring.
     */
    private fun growRays(rings: List<FloatArray>, raySpacing: Float): List<Ray> {
        val last = rings.size - 1
        val rays = mutableListOf<Ray>()
        var count = 0
        for (start in 1 until last) {
            val target = (TWO_PI * rings[start].average().toFloat() / raySpacing).toInt()
            repeat(target - count) {
                val end = if (prob(0.5f)) last else min(last, start + 3 + random(last - start).toInt())
                rays.add(Ray(random(TWO_PI), start, end, 6000 + rays.size * 7.3f))
            }
            count = max(count, target)
        }
        return rays
    }

    /** How hard [ray] disrupts year [k], 0..1. Zero for most years, so rays show up intermittently. */
    private fun rayStrength(ray: Ray, k: Int): Float =
        if (k < ray.start || k > ray.end) 0f
        else constrain((noise(ray.id, k * 0.4f) - 0.45f) / 0.2f, 0f, 1f)

    /**
     * Applies the rays to every ring: the line is cut where the ray passes, and on each side of the
     * cut the ring bulges outward by a different amount, so the two ends don't line up. Bulges are
     * clamped to under half the gap to the next ring, so rings still never cross. Returns the ring
     * and earlywood lines as open polylines, ready for SVG export.
     */
    private fun cutRings(
        center: PVector, rings: List<FloatArray>, innerBark: FloatArray, rays: List<Ray>,
    ): Pair<List<List<FloatArray>>, List<List<FloatArray>>> {
        val last = rings.size - 1
        val bent = rings.mapIndexed { k, ring ->
            if (k == 0) return@mapIndexed ring
            val next = if (k < last) rings[k + 1] else innerBark
            val meanR = ring.average().toFloat()
            val disp = FloatArray(samples)
            for (ray in rays) {
                val st = rayStrength(ray, k)
                if (st <= 0f) continue
                val reach = (3f + 8f * st) / meanR
                val amp = 3f * st
                val skew = 2 * noise(ray.id + 50, k * 0.7f) - 1  // which side lifts more
                val j0 = ((ray.angle - reach) / TWO_PI * samples).toInt() - 1
                val j1 = ((ray.angle + reach) / TWO_PI * samples).toInt() + 1
                for (jj in j0..j1) {
                    val j = jj.mod(samples)
                    val da = TWO_PI * jj / samples - ray.angle
                    if (abs(da) >= reach) continue
                    val f = sq(1 - abs(da) / reach)
                    disp[j] = max(disp[j], amp * (1 + if (da < 0) -skew else skew) * f)
                }
            }
            FloatArray(samples) { j -> ring[j] + min(disp[j], 0.45f * (next[j] - ring[j])) }
        }
        val cuts = (0..last).map { k ->
            val meanR = rings[k].average().toFloat()
            rays.mapNotNull { ray ->
                val st = rayStrength(ray, k)
                if (st <= 0f) null else (0.6f + 1.2f * st) / meanR to ray
            }.map { (hw, ray) -> ray.angle - hw to ray.angle + hw }
        }
        val ringPaths = (0..last).map { k -> if (k == 0) listOf() else ringPolylines(center, bent[k], cuts[k]) }
        // faint lines inside the year, interpolated so they can't cross the ring lines
        val earlyPaths = (0..last).map { k ->
            if (k == 0) listOf()
            else floatArrayOf(0.35f, 0.7f).flatMap { ringPolylines(center, lerpRing(bent[k - 1], bent[k], it), cuts[k]) }
        }
        return ringPaths to earlyPaths
    }

    /** A ring as open polylines, with the angular intervals in [cuts] left out. */
    private fun ringPolylines(center: PVector, r: FloatArray, cuts: List<Pair<Float, Float>>): List<FloatArray> {
        fun point(out: MutableList<Float>, a: Float, rad: Float) {
            out.add(center.x + rad * cos(a)); out.add(center.y + rad * sin(a))
        }
        if (cuts.isEmpty()) {
            val pts = mutableListOf<Float>()
            for (j in 0..samples) point(pts, TWO_PI * j / samples, r[j % samples])
            return listOf(pts.toFloatArray())
        }
        // normalise to [0, 2pi), sort, and merge overlapping cuts
        val sorted = cuts.map { (a, b) -> val o = a.mod(TWO_PI); o to o + (b - a) }.sortedBy { it.first }
        val merged = mutableListOf(sorted[0])
        for (c in sorted.drop(1)) {
            val m = merged.last()
            if (c.first <= m.second) merged[merged.size - 1] = m.first to max(m.second, c.second)
            else merged.add(c)
        }
        if (merged.size > 1 && merged.last().second >= merged[0].first + TWO_PI) {
            val l = merged.removeAt(merged.size - 1)
            merged[0] = l.first - TWO_PI to max(merged[0].second, l.second - TWO_PI)
        }
        return merged.indices.mapNotNull { i ->
            val from = merged[i].second
            val to = if (i + 1 < merged.size) merged[i + 1].first else merged[0].first + TWO_PI
            if (to <= from) return@mapNotNull null
            val pts = mutableListOf<Float>()
            point(pts, from, ringAt(r, from))
            var j = floor(from / TWO_PI * samples) + 1
            while (TWO_PI * j / samples < to) {
                point(pts, TWO_PI * j / samples, r[j.mod(samples)])
                j++
            }
            point(pts, to, ringAt(r, to))
            pts.toFloatArray()
        }
    }

    private fun drawPath(p: FloatArray) = withShape { for (k in p.indices step 2) vertex(p[k], p[k + 1]) }

    /** A ring's radius at an arbitrary angle, interpolated between samples. */
    private fun ringAt(r: FloatArray, a: Float): Float {
        val t = (a / TWO_PI * samples).mod(samples.toFloat())
        val j = t.toInt()
        return lerp(r[j % samples], r[(j + 1) % samples], t - j)
    }

    /**
     * Mean ring width per year: age taper times a "climate", scaled to maxR. Climate, droughts and
     * latewood darkness all come from 1D Perlin noise over time, so neighbouring years are
     * correlated (good runs, bad runs) and a drought ramps in and recovers over a couple of years.
     */
    private fun yearWidths(maxR: Float, years: Int, taper: Float, latewood: FloatArray): FloatArray {
        val w = FloatArray(years) { i ->
            var climate = map(noise(1000 + i * 0.15f), 0.25f, 0.75f, 0.5f, 1.5f)
            // rare droughts: only the peaks of a faster noise channel get past the threshold
            val drought = constrain((noise(2000 + i * 0.6f) - 0.66f) / 0.08f, 0f, 1f)
            climate *= 1 - 0.65f * drought
            val jitter = 0.3f * (noise(3000 + i * 0.7f) - 0.5f)
            latewood[i + 1] = constrain(1.2f - 0.8f * climate + jitter, 0f, 1f)
            (pow((i + 1f) / years, taper) - pow(i.toFloat() / years, taper)) * max(climate, 0.1f)
        }
        val scale = maxR / w.sum()
        return FloatArray(years) { w[it] * scale }
    }

    private fun smooth(r: FloatArray, k: Int) = FloatArray(samples) { j ->
        (-k..k).sumOf { r[(j + it).mod(samples)].toDouble() }.toFloat() / (2 * k + 1)
    }

    private fun lerpRing(a: FloatArray, b: FloatArray, t: Float) =
        FloatArray(samples) { lerp(a[it], b[it], t) }

    private fun drawRing(center: PVector, r: FloatArray) = withShape(CLOSE) {
        for (j in 0 until samples) {
            val a = TWO_PI * j / samples
            vertex(center.x + r[j] * cos(a), center.y + r[j] * sin(a))
        }
    }

    override fun keyTyped(event: KeyEvent) {
        super.keyTyped(event)
        when (event.key) {
            '1' -> eccentricity = max(0f, eccentricity - 0.05f)
            '2' -> eccentricity = min(1f, eccentricity + 0.05f)
            '3' -> years = max(1, years - 1)
            '4' -> years = min(200, years + 1)
            '5' -> taper = max(0.2f, taper - 0.05f)
            '6' -> taper = min(1f, taper + 0.05f)
            '7' -> barkThickness = max(0.01f, barkThickness - 0.01f)
            '8' -> barkThickness = min(0.3f, barkThickness + 0.01f)
            '9' -> raySpacing = max(3f, raySpacing - 2f)
            '0' -> raySpacing = min(100f, raySpacing + 2f)
            else -> return
        }
        // draw() regrows on the next frame when a parameter changed
    }

    companion object {
        val SIZE = Size(1000, 1000)
//        val SIZE = PaperSize.INDEX_CARD.size
//        val SIZE = PaperSize.INDEX_CARD.landscape()
//        val SIZE = PaperSize.ORIGAMI_150.size
    }
}

fun main() {
    PApplet.runSketch(arrayOf("growth.TreeCuts"), null)
}
