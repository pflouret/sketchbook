package growth

import p5.ProcessingAppK
import processing.core.PApplet
import processing.event.KeyEvent
import processing.core.PVector
import util.Size

/**
 * Growth rings grown outward, one year at a time, in polar coordinates.
 *
 * Every ring is a radius sampled at fixed angles around the pith. Each year's ring is the
 * previous ring (slightly smoothed) plus a strictly positive increment, so rings can never
 * cross, and bumps get carried outward and fade the way they do in real wood.
 */
class TreeCuts : ProcessingAppK() {
    private val samples = 720
    private var years = 60
    private var barkThickness = 0.07f  // fraction of the trunk radius
    private var eccentricity = 0.3f
    private var taper = 0.7f  // 1 = even ring widths, lower = wide young rings, narrow old ones
    private var drawEarlywood = true

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
    )

    @Volatile private var tree = Tree(PVector(), listOf(), floatArrayOf(), floatArrayOf(), floatArrayOf(), floatArrayOf())

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()
        noiseDetail(4, 0.5f)
        noLoop()
        reset()
    }

    override fun draw() {
        background(255)
        noFill()
        val tree = tree
        val rings = tree.rings
        val latewood = tree.latewood
        for (i in 1 until rings.size) {
            if (drawEarlywood) {
                // faint lines inside the year, interpolated so they can't cross the ring lines
                stroke(0f, 30f)
                strokeWeight(0.5f)
                floatArrayOf(0.35f, 0.7f).forEach { drawRing(tree.center, lerpRing(rings[i - 1], rings[i], it)) }
            }
            stroke(0f, 90f + 140f * latewood[i])
            strokeWeight(0.6f + 1.4f * latewood[i])
            drawRing(tree.center, rings[i])
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
        regrow()
    }

    private fun regrow() {
        resetSeed(false)
        tree = grow(years, barkThickness, eccentricity, taper)
        redraw()
    }

    private fun grow(years: Int, barkThickness: Float, eccentricity: Float, taper: Float): Tree {
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
        return growBark(center, out, latewood, maxR * barkThickness)
    }

    /**
     * A thin pale inner bark right outside the last ring, then a thick outer bark whose thickness
     * comes from |2n - 1| noise: wide plateaus with sharp V-shaped fissures where n crosses 0.5.
     */
    private fun growBark(center: PVector, rings: List<FloatArray>, latewood: FloatArray, thickness: Float): Tree {
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
        return Tree(center, rings, latewood, inner, outer, strokes)
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
            '2' -> eccentricity += 0.05f
            '3' -> years = max(5, years - 5)
            '4' -> years += 5
            '5' -> taper = max(0.2f, taper - 0.05f)
            '6' -> taper = min(1f, taper + 0.05f)
            '7' -> barkThickness = max(0.01f, barkThickness - 0.01f)
            '8' -> barkThickness = min(0.3f, barkThickness + 0.01f)
            'e' -> drawEarlywood = !drawEarlywood
            else -> return
        }
        println("ecc=%.2f years=$years taper=%.2f bark=%.2f".format(eccentricity, taper, barkThickness))
        regrow()
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
