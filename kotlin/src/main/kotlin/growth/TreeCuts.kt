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
    private var zoom: Float by LazyGuiControlDelegate("slider", "", 0.99f, 0.1f, 2f)  // trunk size, fraction of the canvas's shorter side
    private var years: Int by LazyGuiControlDelegate("sliderInt", "", 60, 1, 100)
    private var barkThickness: Float by LazyGuiControlDelegate("slider", "", 0.01f, 0.01f, 0.1f)  // fraction of the trunk radius
    private var eccentricity: Float by LazyGuiControlDelegate("slider", "", 0f, 0f, 1f)
    private var taper: Float by LazyGuiControlDelegate("slider", "", 0.7f, 0.2f, 1f)  // 1 = even ring widths, lower = wide young rings, narrow old ones
    private var raySpacing: Float by LazyGuiControlDelegate("slider", "", 14f, 3f, 100f)  // px of circumference per ray
    private var tickSpacing: Float by LazyGuiControlDelegate("slider", "", 0.9f, 0.05f, 5f)  // px along the ring between ticks
    private var tickLength: Float by LazyGuiControlDelegate("slider", "", 2.5f, 0f, 10f)  // px, 0 = no ticks
    private var noiseOctaves: Int by LazyGuiControlDelegate("sliderInt", "", 4, 1, 8)
    private var noiseFalloff: Float by LazyGuiControlDelegate("slider", "", 0.5f, 0f, 1f)  // weight of each next octave

    /** The parameters that need a regrow, read from the gui once per frame. */
    private data class Params(
        val years: Int, val zoom: Float, val samples: Int, val eccentricity: Float,
        val taper: Float, val tickSpacing: Float, val tickLength: Float, val raySpacing: Float,
        val barkThickness: Float, val noiseOctaves: Int, val noiseFalloff: Float,
    )

    private fun params() = Params(
        years,
        zoom,
        ringSamples,
        eccentricity,
        taper,
        tickSpacing,
        tickLength,
        raySpacing,
        barkThickness,
        noiseOctaves,
        noiseFalloff,
    )
    private var grownWith: Params? = null

    /**
     * Everything draw() needs, swapped in whole. With noLoop() Processing handles key events on
     * the window thread, so draw() can run while a regrow is changing the parameters.
     */
    private class Tree(
        val rings: List<RingLines>,  // the separate stems' rings, then the shared ones
        val innerBark: List<FloatArray>,  // open x, y polylines, like everything below
        val outerBark: List<FloatArray>,
        val cracks: List<FloatArray>,
    )

    /**
     * A drying crack: a wedge from radius [r0] around [center] out through the bark at [r1], opening
     * to [width] px either side of a wandering centre line at [angle]. [id] keys its noise.
     */
    private class Crack(val center: PVector, val angle: Float, val r0: Float, val r1: Float, val width: Float, val wander: Float, val id: Float)

    /** One year's ring, ready to stroke: open x, y polylines for the ring and its faint earlywood lines. */
    private class RingLines(
        val latewood: Float,
        val lines: List<FloatArray>,
        val early: List<FloatArray>,
        val ticks: FloatArray = floatArrayOf(),  // x0, y0, x1, y1 per tick across the ring line
    )

    private var exportLayers: Boolean by LazyGuiControlDelegate("button")

    /** A medullary ray, active from year [start] to [end]. [id] keys its per-year noise. */
    private class Ray(val angle: Float, val start: Int, val end: Int, val id: Float)

    @Volatile private var tree = Tree(listOf(), listOf(), listOf(), listOf())

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
        if (exportLayers) exportLayeredSvg(tree)
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
            strokeWeight(0.5f)
            drawSegments(ring.ticks)
        }
        drawBark(tree)
        stroke(20f, 200f)
        strokeWeight(1f)
        tree.cracks.forEach(::drawPath)
    }

    private fun drawBark(tree: Tree) {
        if (tree.innerBark.isEmpty()) return
        stroke(60f, 35f, 15f, 160f)
        strokeWeight(1f)
        tree.innerBark.forEach(::drawPath)

        stroke(30f, 15f, 5f)
        strokeWeight(2.2f)
        tree.outerBark.forEach(::drawPath)
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
        val (years, zoom1, samples1, eccentricity, taper, tickSpacing1, tickLength1, raySpacing1, barkThickness) = params
        val center = PVector(w2 + random(-30, 30), h2 + random(-30, 30))
        val piths = (1 + random(3).toInt()).let { if (years < 4) 1 else it }
        val latewood = FloatArray(years + 1)
        val maxR = min(width, height) * params.zoom / 2
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
        val (inner, outer) = growBark(rings.last(), maxR * barkThickness)
        lines += cutRings(center, rings, inner, rays, stemYears, latewood)
        val cracks = growCracks(center, rings.last().average().toFloat(), outer, maxR)
        fun clip(paths: List<FloatArray>) = paths.flatMap { clipToCracks(it, cracks) }
        return Tree(
            lines.map {
                val ringLines = clip(it.lines)
                RingLines(it.latewood, ringLines, clip(it.early), ticks(ringLines, params.tickSpacing, params.tickLength, cracks))
            },
            clip(ringPolylines(center, inner, listOf())),
            clip(ringPolylines(center, outer, listOf())),
            cracks.map { crackOutline(it, outer) },
        )
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
    private fun growBark(cambium: FloatArray, thickness: Float): Pair<FloatArray, FloatArray> {
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

        return inner to outer
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

    /**
     * Writes the tree as an SVG with one Inkscape layer (a top-level group, which vpype also reads as
     * a layer) per part, to plot each with its own pen: earlywood lines, latewood (the ring lines),
     * ticks, bark and cracks. Coordinates are canvas px, like the `R` export.
     */
    private fun exportLayeredSvg(tree: Tree) {
        val sb = StringBuilder()
        fun n(v: Float) = (Math.round(v * 100) / 100f).toString()
        fun polylines(paths: List<FloatArray>) {
            for (p in paths) {
                if (p.size < 4) continue
                sb.append("<path d=\"M")
                for (k in p.indices step 2) {
                    if (k > 0) sb.append(" L")
                    sb.append(n(p[k])).append(' ').append(n(p[k + 1]))
                }
                sb.append("\"/>\n")
            }
        }
        // many short segments, x0, y0, x1, y1 each: one path with a subpath per segment
        fun segments(s: FloatArray) {
            if (s.isEmpty()) return
            sb.append("<path d=\"")
            for (k in s.indices step 4) {
                sb.append("M").append(n(s[k])).append(' ').append(n(s[k + 1]))
                    .append(" L").append(n(s[k + 2])).append(' ').append(n(s[k + 3])).append(' ')
            }
            sb.append("\"/>\n")
        }
        var id = 0
        fun layer(name: String, weight: Float, body: () -> Unit) {
            id++
            sb.append("<g inkscape:groupmode=\"layer\" inkscape:label=\"$id $name\" id=\"layer$id\" ")
                .append("style=\"fill:none;stroke:#000;stroke-width:$weight;stroke-linecap:round;stroke-linejoin:round\">\n")
            // the layer's paths go in one plain group inside it, so they move/select together
            sb.append("<g id=\"$name\">\n")
            body()
            sb.append("</g>\n</g>\n")
        }
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:inkscape=\"http://www.inkscape.org/namespaces/inkscape\" ")
            .append("width=\"${width}px\" height=\"${height}px\" viewBox=\"0 0 $width $height\">\n")
        layer("earlywood", 0.5f) { tree.rings.forEach { polylines(it.early) } }
        layer("latewood", 1f) { tree.rings.forEach { polylines(it.lines) } }
        layer("ticks", 0.5f) { tree.rings.forEach { segments(it.ticks) } }
        layer("bark", 1f) {
            polylines(tree.innerBark)
            polylines(tree.outerBark)
        }
        layer("cracks", 1f) { polylines(tree.cracks) }
        sb.append("</svg>\n")
        val file = makeSketchFilename("%s_####_layers.svg")
        java.io.File(file).writeText(sb.toString())
        println(file)
    }

    /** Separate line segments, x0, y0, x1, y1 each, in one shape. */
    private fun drawSegments(s: FloatArray) {
        if (s.isEmpty()) return
        beginShape(LINES)
        for (k in s.indices step 2) vertex(s[k], s[k + 1])
        endShape()
    }

    /**
     * Tiny ticks from the line inwards, perpendicular to it, every [spacing] px (jittered) of arc
     * length. Ring polylines run by increasing angle, so (-dy, dx) points towards the ring's centre.
     * Ticks that would reach into a crack are dropped.
     */
    private fun ticks(paths: List<FloatArray>, spacing: Float, length: Float, cracks: List<Crack>): FloatArray {
        if (length <= 0f) return floatArrayOf()
        val out = mutableListOf<Float>()
        for (p in paths) {
            var next = spacing * random(0.2f, 1f)  // distance along the path to the next tick
            var travelled = 0f
            for (k in 2 until p.size step 2) {
                val dx = p[k] - p[k - 2]
                val dy = p[k + 1] - p[k - 1]
                val seg = mag(dx, dy)
                if (seg == 0f) continue
                while (next <= travelled + seg) {
                    val t = (next - travelled) / seg
                    val x = p[k - 2] + dx * t
                    val y = p[k - 1] + dy * t
                    // length from 2D noise over the canvas, so neighbouring ticks (across rings too)
                    // are similar: patches of long ticks fading into short ones
                    val l = length * constrain(map(noise(8000 + x * 0.015f, 8000 + y * 0.015f), 0.25f, 0.75f, 0.1f, 1f), 0.1f, 1f)
                    val ix = x - dy / seg * l
                    val iy = y + dx / seg * l
                    if (cracks.none { inCrack(it, ix, iy) }) {
                        out.add(x); out.add(y); out.add(ix); out.add(iy)
                    }
                    next += spacing * random(0.6f, 1.4f)
                }
                travelled += seg
            }
        }
        return out.toFloatArray()
    }

    private fun drawPath(p: FloatArray) = withShape { for (k in p.indices step 2) vertex(p[k], p[k + 1]) }

    /** A ring's radius at an arbitrary angle, interpolated between samples. */
    private fun ringAt(r: FloatArray, a: Float): Float {
        val t = (a / TWO_PI * samples).mod(samples.toFloat())
        val j = t.toInt()
        return lerp(r[j % samples], r[(j + 1) % samples], t - j)
    }

    /**
     * 0 to 3 drying cracks, mostly 0 or 1, spread around the trunk. Drawn last from the random
     * sequence, so they don't change the rest of the tree.
     */
    private fun growCracks(center: PVector, trunkR: Float, outerBark: FloatArray, maxR: Float): List<Crack> {
        val u = random(1f)
        val count = when {
            u < 0.35f -> 0
            u < 0.75f -> 1
            u < 0.95f -> 2
            else -> 3
        }
        val scale = maxR / 400f  // crack sizes were picked on a 400px-radius trunk
        val cracks = mutableListOf<Crack>()
        repeat(count) { n ->
            var angle = random(TWO_PI)
            // a few tries to keep cracks apart
            for (t in 0 until 10) {
                if (cracks.none { abs((it.angle - angle + PI).mod(TWO_PI) - PI) < 0.6f }) break
                angle = random(TWO_PI)
            }
            val r1 = outerBark.max() + 2f
            cracks.add(Crack(center, angle, trunkR * random(0.05f, 0.35f), r1, random(3f, 9f) * scale, 20f * scale, 7000f + 31f * n))
        }
        return cracks
    }

    /** Sideways offset (px) of the crack's centre line at radius [r]: a slow wander plus a fine zigzag. */
    private fun crackSide(c: Crack, r: Float) =
        c.wander * (noise(c.id, r * 0.02f) - 0.5f) + 0.25f * c.wander * (noise(c.id + 9, r * 0.3f) - 0.5f)

    /** Half the crack's opening at radius [r]: zero at the tip, widest at the bark. */
    private fun crackHalfWidth(c: Crack, r: Float) =
        if (r <= c.r0) 0f else c.width * pow(min(1f, (r - c.r0) / (c.r1 - c.r0)), 1.5f)

    private fun inCrack(c: Crack, x: Float, y: Float): Boolean {
        val dx = x - c.center.x
        val dy = y - c.center.y
        val r = mag(dx, dy)
        if (r <= c.r0 || r > c.r1) return false
        val side = r * ((atan2(dy, dx) - c.angle + PI).mod(TWO_PI) - PI)  // px along the arc
        if (abs(side) > c.width + c.wander) return false  // cheap reject before the noise
        return abs(side - crackSide(c, r)) < crackHalfWidth(c, r)
    }

    /**
     * Polyline [p] with the parts inside any crack removed. Crossings are found by bisection, so the
     * pieces end right on the crack's edge lines. A closed polyline cut once stays one piece.
     */
    private fun clipToCracks(p: FloatArray, cracks: List<Crack>): List<FloatArray> {
        if (cracks.isEmpty() || p.size < 4) return listOf(p)
        fun inside(x: Float, y: Float) = cracks.any { inCrack(it, x, y) }
        val out = mutableListOf<MutableList<Float>>()
        var cur = mutableListOf<Float>()
        var px = p[0]
        var py = p[1]
        var pin = inside(px, py)
        if (!pin) { cur.add(px); cur.add(py) }
        for (k in 2 until p.size step 2) {
            val x = p[k]
            val y = p[k + 1]
            val inn = inside(x, y)
            if (inn != pin) {
                var ax = px; var ay = py; var bx = x; var by = y
                repeat(12) {
                    val mx = (ax + bx) / 2
                    val my = (ay + by) / 2
                    if (inside(mx, my) == pin) { ax = mx; ay = my } else { bx = mx; by = my }
                }
                val ex = (ax + bx) / 2
                val ey = (ay + by) / 2
                if (pin) {
                    cur = mutableListOf(ex, ey)
                } else {
                    cur.add(ex); cur.add(ey)
                    out.add(cur)
                    cur = mutableListOf()
                }
            }
            if (!inn) { cur.add(x); cur.add(y) }
            px = x; py = y; pin = inn
        }
        if (cur.isNotEmpty()) out.add(cur)
        val closed = p[0] == p[p.size - 2] && p[1] == p[p.size - 1]
        if (closed && out.size > 1 && !inside(p[0], p[1])) {
            // the first and last pieces meet at the seam: join them
            val last = out.removeAt(out.size - 1)
            out[0] = (last.dropLast(2) + out[0]).toMutableList()
        }
        return out.filter { it.size >= 4 }.map { it.toFloatArray() }
    }

    /** The crack's two edges as one V-shaped open polyline, from the bark in to the tip and back out. */
    private fun crackOutline(c: Crack, outerBark: FloatArray): FloatArray {
        fun edge(sign: Float): List<Float> {
            val pts = mutableListOf<Float>()
            var r = c.r0
            while (true) {
                val a = c.angle + (crackSide(c, r) + sign * crackHalfWidth(c, r)) / r
                pts.add(c.center.x + r * cos(a)); pts.add(c.center.y + r * sin(a))
                if (r >= ringAt(outerBark, a) || r >= c.r1) break
                r = min(r + 1.5f, c.r1)
            }
            return pts
        }
        val left = edge(-1f)
        val right = edge(1f)
        // left edge from the bark in to the tip, then the right edge back out (tip shared once)
        val l = left.chunked(2).reversed().flatten()
        return (l + right.drop(2)).toFloatArray()
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
            'L' -> {
                exportLayeredSvg(tree)
                return
            }
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
