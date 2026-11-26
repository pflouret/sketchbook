package lines

import gui.LazyGuiControlDelegate
import p5.ProcessingAppK
import processing.core.PApplet
import util.Size

// slop galore

class Dunes : ProcessingAppK() {
    private var lines: Int by LazyGuiControlDelegate("sliderInt", "", 400, 100, 6000)
    private var period: Float by LazyGuiControlDelegate("slider", "", 420f, 40f, 1000f)  // px between crests
    private var duneHeight: Float by LazyGuiControlDelegate("slider", "", 200f, 0f, 400f)  // px
    private var windward: Float by LazyGuiControlDelegate("slider", "", 0.75f, 0.05f, 0.95f)  // gentle side's share of a dune
    private var windAngle: Float by LazyGuiControlDelegate("slider", "", 0f, -90f, 90f)  // degrees the wind is turned from straight into the picture; turns the crests
    private var crestWander: Float by LazyGuiControlDelegate("slider", "", 1f, 0f, 10f)  // how far crests bend, in periods
    private var swell: Float by LazyGuiControlDelegate("slider", "", 80f, 0f, 300f)  // px, broad undulation under the dunes
    private var lineAngle: Float by LazyGuiControlDelegate("slider", "", 45f, -90f, 90f)  // degrees the lines cross the dunes at
    private var bend: Float by LazyGuiControlDelegate("slider", "", 80f, 0f, 400f)  // px the lines sway in depth
    private var tilt: Float by LazyGuiControlDelegate("slider", "", 0.35f, 0.1f, 1f)  // screen px per px of depth
    private var perspective: Float by LazyGuiControlDelegate("slider", "", 0.6f, 0f, 3f)
    private var windTowardsViewer: Boolean by LazyGuiControlDelegate("toggle", "", false)
    private var alpha: Float by LazyGuiControlDelegate("slider", "", 90f, 10f, 255f)
    private var noiseOctaves: Int by LazyGuiControlDelegate("sliderInt", "", 3, 1, 8)
    private var noiseFalloff: Float by LazyGuiControlDelegate("slider", "", 0.5f, 0f, 1f)

    /** The parameters that need a regrow, read from the gui once per frame. */
    private data class Params(
        val lines: Int, val period: Float, val duneHeight: Float, val windward: Float, val windAngle: Float, val crestWander: Float,
        val swell: Float, val lineAngle: Float, val bend: Float, val tilt: Float, val perspective: Float, val windTowardsViewer: Boolean,
        val noiseOctaves: Int, val noiseFalloff: Float,
    )

    private fun params() = Params(
        lines, period, duneHeight, windward, windAngle, crestWander, swell, lineAngle, bend, tilt, perspective, windTowardsViewer,
        noiseOctaves, noiseFalloff,
    )

    private var grownWith: Params? = null

    /** The visible parts of every line, as open x, y polylines. Swapped in whole, read by draw(). */
    @Volatile private var paths: List<FloatArray> = listOf()

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()
        initGui()
        reset()
    }

    override fun draw() {
        val params = params()
        if (params != grownWith) regrow(params)
        background(255)
        noFill()
        stroke(0f, alpha)
        strokeWeight(0.5f)
        for (p in paths) {
            beginShape()
            for (k in p.indices step 2) vertex(p[k], p[k + 1])
            endShape()
        }
    }

    override fun reset() {
        resetSeed(true)
        grownWith = null  // regrow with the new seed on the next frame
    }

    private fun regrow(params: Params) {
        resetSeed(false)
        noiseDetail(params.noiseOctaves, params.noiseFalloff)
        paths = grow(params)
        grownWith = params
    }

    /**
     * Surface height at world x (across) and z (depth, 0 = nearest). Transverse dunes: along the wind,
     * each period rises gently over the windward share, then drops down a steep slip face. Noise
     * bends the crests, varies their height and adds a broad swell underneath.
     */
    private fun height(p: Params, x: Float, z: Float): Float {
        val warp = p.crestWander * p.period * (noise(x * 0.0015f, z * 0.0015f + 100f) - 0.5f) * 2f
        val wind = if (p.windTowardsViewer) 1f else -1f
        val a = radians(p.windAngle)
        val along = z * cos(a) + x * sin(a)  // distance along the wind
        val u = wind * (along + warp) / p.period + 0.5f * noise(x * 0.004f, 50f)
        val t = u - floor(u)
        val c = p.windward
        val profile =
            if (t < c) pow(t / c, 1.6f)  // flat trough, steepening up to a sharp brink
            else pow(1 - (t - c) / (1 - c), 1.5f)  // slip face, easing out at its foot
        val amp = p.duneHeight * (0.3f + 1.1f * noise(x * 0.0025f, z * 0.0025f + 300f))
        val base = p.swell * (noise(x * 0.001f + 700f, z * 0.001f) - 0.5f) * 2f
        return amp * profile + base
    }

    private fun grow(p: Params): List<FloatArray> {
        val depth = height * 2.5f / p.tilt  // enough depth for the farthest lines to leave the top
        val cx = w2
        val margin = 40f
        // Lines are parallel in the ground plane, so line k is in front of line k + 1 everywhere and
        // drawing them in order is still front to back. Angled lines start further forward so the
        // near corner of the canvas is covered too.
        val slope = tan(radians(p.lineAngle))
        val zStart = -abs(slope) * (cx + margin) * (1 + p.perspective)
        val zEnd = depth + abs(slope) * (cx + margin) * (1 + p.perspective)
        val horizon = FloatArray(width) { Float.MAX_VALUE }  // topmost screen y drawn so far per column
        val out = mutableListOf<FloatArray>()
        // world x range that covers the screen even where a line is farthest away (smallest on screen)
        val halfWidth = (cx + margin) * (1 + p.perspective)
        val n = (2 * halfWidth).toInt() + 1  // ~1 world px per sample, which is at most 1 screen px
        val sx = FloatArray(n)
        val sy = FloatArray(n)

        for (k in 0 until p.lines) {
            val z0 = lerp(zStart, zEnd, k / (p.lines - 1f))
            var lowest = -Float.MAX_VALUE
            for (i in 0 until n) {
                val x = cx - halfWidth + i
                val z = z0 + (x - cx) * slope + p.bend * (noise(x * 0.002f + 900f, z0 * 0.003f) - 0.5f) * 2f
                val f = 1 / (1 + p.perspective * max(z, 0f) / depth)
                sx[i] = cx + (x - cx) * f
                sy[i] = height + margin - (z * p.tilt + height(p, x, z)) * f
                lowest = max(lowest, sy[i])
            }
            if (lowest < -margin) break  // this line and every one behind it is above the canvas

            visibleRuns(sx, sy, n, horizon, out)
            // raise the horizon only after the whole line, so a line never hides itself
            for (i in 0 until n) {
                val col = sx[i].toInt()
                if (col in horizon.indices) horizon[col] = min(horizon[col], sy[i])
            }
        }
        return out
    }

    /**
     * Appends the parts of the line that are above [horizon] and on the canvas to [out], with the
     * ends interpolated onto the horizon or the canvas edge.
     */
    private fun visibleRuns(sx: FloatArray, sy: FloatArray, n: Int, horizon: FloatArray, out: MutableList<FloatArray>) {
        fun gap(i: Int): Float {
            val col = sx[i].toInt()
            if (col !in horizon.indices) return -1f
            return minOf(horizon[col] - sy[i], sy[i], height - sy[i])  // > 0 means visible
        }
        var run = mutableListOf<Float>()
        var prev = gap(0)
        if (prev > 0) { run.add(sx[0]); run.add(sy[0]) }
        for (i in 1 until n) {
            val g = gap(i)
            if ((g > 0) != (prev > 0)) {
                val t = prev / (prev - g)
                val x = lerp(sx[i - 1], sx[i], t)
                val y = lerp(sy[i - 1], sy[i], t)
                run.add(x); run.add(y)
                if (prev > 0) {
                    if (run.size >= 4) out.add(run.toFloatArray())
                    run = mutableListOf()
                }
            }
            if (g > 0) { run.add(sx[i]); run.add(sy[i]) }
            prev = g
        }
        if (run.size >= 4) out.add(run.toFloatArray())
    }

    companion object {
        val SIZE = Size(1200, 800)
//        val SIZE = PaperSize.A4.landscape()
    }
}

fun main() {
    PApplet.runSketch(arrayOf("lines.Dunes"), null)
}
