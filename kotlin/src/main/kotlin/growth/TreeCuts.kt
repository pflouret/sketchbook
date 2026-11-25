package growth

import gui.LazyGuiControlDelegate
import p5.ProcessingAppK
import processing.core.PApplet
import processing.event.KeyEvent
import processing.core.PVector
import util.Size

// Slop galore

class TreeCuts : ProcessingAppK() {
    private var ringSamples: Int by LazyGuiControlDelegate("sliderInt", "", 400, 10, 800)  // points per ring
    /** Points per ring of the tree being grown; set from [ringSamples] in regrow(), on the draw thread. */
    private var samples = 720
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
        val raySpacing: Float, val noiseOctaves: Int, val noiseFalloff: Float, val samples: Int,
    )

    private fun params() = Params(
        years, barkThickness, eccentricity, taper, raySpacing, noiseOctaves, noiseFalloff, ringSamples,
    )
    private var grownWith: Params? = null

    /**
     * Everything draw() needs, swapped in whole. With noLoop() Processing handles key events on
     * the window thread, so draw() can run while a regrow is changing the parameters.
     */
    private class Tree(
        val center: PVector,
        val rings: List<RingLines>,  // the separate stems' rings, then the shared ones
        val innerBark: FloatArray,
        val outerBark: FloatArray,
        val barkStrokes: FloatArray,  // x0, y0, x1, y1 per stroke
    )

    /** One year's ring, ready to stroke: open x, y polylines for the ring and its faint earlywood lines. */
    private class RingLines(val latewood: Float, val lines: List<FloatArray>, val early: List<FloatArray>)

    /** A medullary ray, active from year [start] to [end]. [id] keys its per-year noise. */
    private class Ray(val angle: Float, val start: Int, val end: Int, val id: Float)

    @Volatile private var tree = Tree(PVector(), listOf(), floatArrayOf(), floatArrayOf(), floatArrayOf())

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
        for (ring in tree.rings) {
            stroke(0f, 30f)
            strokeWeight(0.5f)
            ring.early.forEach(::drawPath)
            stroke(0f, 90f + 140f * ring.latewood)
            strokeWeight(0.6f + 1.4f * ring.latewood)
            ring.lines.forEach(::drawPath)
        }
        drawBark(tree)
    }

    private fun drawBark(tree: Tree) {
        if (tree.innerBark.isEmpty()) return
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
        samples = params.samples
        tree = grow(params)
        grownWith = params
    }

    private fun grow(params: Params): Tree {
        val (years, barkThickness, eccentricity, taper) = params
        val center = PVector(w2 + random(-30, 30), h2 + random(-30, 30))
        val piths = (1 + random(3).toInt()).let { if (years < 4) 1 else it }
        val latewood = FloatArray(years + 1)
        val maxR = min(width, height) * 0.4f
        var widths = yearWidths(maxR * (1 - barkThickness), years, taper, latewood)
        val lean0 = random(TWO_PI)
        val lines = mutableListOf<RingLines>()

        var first = FloatArray(samples) { 1.5f }
        var stemYears = 0  // years the stems grew apart before sharing rings
        if (piths > 1) {
            // The stems start apart, so they get a head start on the trunk radius; every year is
            // narrowed a bit to make up for it and keep the trunk inside maxR.
            val target = max(1, (years * random(0.15f, 0.35f)).toInt())
            val headStart = widths.take(target).sum()
            val k = widths.sum() / (widths.sum() + headStart)
            widths = FloatArray(years) { widths[it] * k }
            val spread = random(TWO_PI)
            val centers = List(piths) { p ->
                val a = spread + TWO_PI * p / piths + random(-0.3f, 0.3f)
                val d = headStart * k * random(0.85f, 1.15f)
                PVector(center.x + d * cos(a), center.y + d * sin(a))
            }
            val stems = centers.map { mutableListOf(FloatArray(samples) { 1.5f }) }
            while (stemYears < years - 1 && !grownTogether(centers, stems.map { it.last() })) {
                val i = stemYears
                for (p in 0 until piths) {
                    // each stem leans away from the others
                    val out = atan2(centers[p].y - center.y, centers[p].x - center.x)
                    val lean = out + 1.5f * (noise(4000 + 50f * (p + 1) + i * 0.04f) - 0.5f)
                    stems[p].add(growRing(stems[p].last(), i, widths[i], eccentricity, lean, 100f * (p + 1)))
                }
                stemYears++
            }
            for (p in 0 until piths) {
                val stem = stems[p]
                val last = stem.last()
                val outside = FloatArray(samples) { last[it] + 0.5f * widths[max(0, stemYears - 1)] }
                val rays = growRays(stem, params.raySpacing, 20000f + 5000f * p)
                lines += cutRings(centers[p], stem, outside, rays, 0, latewood) { k ->
                    // where two stems press together, the ring of one stops at the other's
                    overlapCuts(centers[p], stem[k], (0 until piths).filter { it != p }.map { centers[it] to stems[it][k] })
                }
            }
            first = envelope(center, centers, stems.map { it.last() })
        }

        val rings = mutableListOf(first)
        for (i in stemYears until years) {
            // slow drift of the lean direction over the tree's life
            val lean = lean0 + 3f * (noise(4000 + i * 0.04f) - 0.5f)
            rings.add(growRing(rings.last(), i, widths[i], eccentricity, lean, 0f))
        }
        val rays = growRays(rings, params.raySpacing, 6000f)
        val (inner, outer, strokes) = growBark(center, rings.last(), maxR * barkThickness)
        lines += cutRings(center, rings, inner, rays, stemYears, latewood)
        return Tree(center, lines, inner, outer, strokes)
    }

    /** Year [i]'s ring grown out of [old]. [off] shifts the noise so separate stems grow differently. */
    private fun growRing(old: FloatArray, i: Int, w: Float, eccentricity: Float, lean: Float, off: Float): FloatArray {
        val minGap = 0.6f
        val meanR = old.average().toFloat()
        // the wobble is sized in px, so on a small young ring it's large next to the circumference;
        // scale it down until the ring's radius is about 80px
        val roughness = random() * w * constrain(meanR / 80f, 0.1f, 1f)
        // smoothing window of ~8px of arc: a few samples on old rings, a wide angle on young ones
        val k = constrain((8f / (TWO_PI * max(meanR, 1f) / samples)).toInt(), 3, samples / 8)
        val prev = smooth(old, k)
        val grown = smooth(FloatArray(samples) { j ->
            val a = TWO_PI * j / samples
            val ca = cos(a)
            val sa = sin(a)
            // noise is sampled on a circle so there's no seam at angle 0; z = time
            val lobes = 0.4f + 1.2f * noise(5 + off + ca * 1.2f, 5 + sa * 1.2f, i * 0.06f)
            val ecc = 1 + eccentricity * cos(a - lean)
            val wobble = noise(50 + off + ca * 8f, 50 + sa * 8f, i * 0.3f) - 0.5f
            prev[j] + w * ecc * lobes + roughness * wobble
        }, k)
        return FloatArray(samples) { j -> max(grown[j], old[j] + minGap) }
    }

    /** Whether the stems' rings have met into one connected group. */
    private fun grownTogether(centers: List<PVector>, rings: List<FloatArray>): Boolean {
        val group = mutableSetOf(0)
        var grew = true
        while (grew) {
            grew = false
            for (a in group.toList()) for (b in centers.indices) {
                if (b in group) continue
                val ang = atan2(centers[b].y - centers[a].y, centers[b].x - centers[a].x)
                val d = PVector.dist(centers[a], centers[b])
                if (ringAt(rings[a], ang) + ringAt(rings[b], ang + PI) >= d) {
                    group.add(b)
                    grew = true
                }
            }
        }
        return group.size == centers.size
    }

    /**
     * The stems' outline as a ring around [c]: in each direction, the farthest point where the
     * direction leaves a stem. Directions that miss every stem (a gap between them) are bridged by
     * interpolating the neighbouring directions.
     */
    private fun envelope(c: PVector, centers: List<PVector>, rings: List<FloatArray>): FloatArray {
        val env = FloatArray(samples) { -1f }
        val step = TWO_PI / samples
        fun span(x0: Float, y0: Float, x1: Float, y1: Float) {
            var a0 = atan2(y0, x0)
            var d0 = mag(x0, y0)
            var d1 = mag(x1, y1)
            var da = (atan2(y1, x1) - a0 + PI).mod(TWO_PI) - PI
            if (da < 0) {
                a0 += da; da = -da
                d0 = d1.also { d1 = d0 }
            }
            if (da == 0f) return
            var m = ceil(a0 / step)
            while (m * step <= a0 + da) {
                val j = m.mod(samples)
                env[j] = max(env[j], lerp(d0, d1, (m * step - a0) / da))
                m++
            }
        }
        for ((q, r) in centers.zip(rings)) {
            var px = 0f
            var py = 0f
            for (j in 0..samples) {
                val a = j * step
                val x = q.x + r[j % samples] * cos(a) - c.x
                val y = q.y + r[j % samples] * sin(a) - c.y
                if (j > 0) span(px, py, x, y)
                px = x; py = y
            }
        }
        val covered = env.indices.filter { env[it] >= 0 }
        if (covered.isEmpty()) return FloatArray(samples) { 1.5f }
        for (n in covered.indices) {
            val a = covered[n]
            val b = if (n + 1 < covered.size) covered[n + 1] else covered[0] + samples
            for (j in a + 1 until b) env[j.mod(samples)] = lerp(env[a], env[b.mod(samples)], (j - a).toFloat() / (b - a))
        }
        return env
    }

    /** Angular intervals of the ring [r] around [q] that fall inside any of the [others]' rings. */
    private fun overlapCuts(q: PVector, r: FloatArray, others: List<Pair<PVector, FloatArray>>): List<Pair<Float, Float>> {
        val step = TWO_PI / samples
        val cuts = mutableListOf<Pair<Float, Float>>()
        for (j in 0 until samples) {
            val a = j * step
            val x = q.x + r[j] * cos(a)
            val y = q.y + r[j] * sin(a)
            val inside = others.any { (o, ro) ->
                dist(x, y, o.x, o.y) < ringAt(ro, atan2(y - o.y, x - o.x))
            }
            // a bit over half a step either side, so neighbouring cuts overlap and merge
            if (inside) cuts.add(a - 0.6f * step to a + 0.6f * step)
        }
        return cuts
    }

    /**
     * A thin pale inner bark right outside the last ring, then a thick outer bark whose thickness
     * comes from |2n - 1| noise: wide plateaus with sharp V-shaped fissures where n crosses 0.5.
     */
    private fun growBark(center: PVector, cambium: FloatArray, thickness: Float): Triple<FloatArray, FloatArray, FloatArray> {
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
        return Triple(inner, outer, strokes)
    }

    /**
     * Medullary rays: radial bands where the rings are disrupted rather than lines drawn on top.
     * As the circumference grows, new rays start between the old ones to keep the spacing roughly
     * constant. A ray lives for a span of years, and within that span per-year noise decides how
     * strongly (if at all) it breaks that year's ring.
     */
    private fun growRays(rings: List<FloatArray>, raySpacing: Float, idBase: Float): List<Ray> {
        val last = rings.size - 1
        val rays = mutableListOf<Ray>()
        var count = 0
        for (start in 1 until last) {
            val target = (TWO_PI * rings[start].average().toFloat() / raySpacing).toInt()
            repeat(target - count) {
                val end = if (prob(0.5f)) last else min(last, start + 3 + random(last - start).toInt())
                rays.add(Ray(random(TWO_PI), start, end, idBase + rays.size * 7.3f))
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
     * clamped to under half the gap to the next ring ([outside] for the last one), so rings still
     * never cross. Ring k is year [firstYear] + k; ring 0 is where growth started and isn't drawn.
     * [extraCuts] leaves out more of ring k. Returns open polylines, ready for SVG export.
     */
    private fun cutRings(
        center: PVector, rings: List<FloatArray>, outside: FloatArray, rays: List<Ray>,
        firstYear: Int, latewood: FloatArray, extraCuts: (Int) -> List<Pair<Float, Float>> = { listOf() },
    ): List<RingLines> {
        val last = rings.size - 1
        val bent = rings.mapIndexed { k, ring ->
            if (k == 0) return@mapIndexed ring
            val next = if (k < last) rings[k + 1] else outside
            val meanR = ring.average().toFloat()
            val disp = FloatArray(samples)
            for (ray in rays) {
                val st = rayStrength(ray, k)
                if (st <= 0f) continue
                val reach = (3f + 8f * st) / meanR
                val amp = 3f * st * constrain(meanR / 80f, 0.1f, 1f)  // gentler on small young rings
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
            }.map { (hw, ray) -> ray.angle - hw to ray.angle + hw } + if (k == 0) listOf() else extraCuts(k)
        }
        return (1..last).map { k ->
            RingLines(
                latewood[firstYear + k],
                ringPolylines(center, bent[k], cuts[k]),
                // faint lines inside the year, interpolated so they can't cross the ring lines
                floatArrayOf(0.35f, 0.7f).flatMap { ringPolylines(center, lerpRing(bent[k - 1], bent[k], it), cuts[k]) },
            )
        }
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
        for (j in r.indices) {
            val a = TWO_PI * j / r.size
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
