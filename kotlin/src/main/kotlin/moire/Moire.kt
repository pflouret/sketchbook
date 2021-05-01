package moire

import com.sun.glass.ui.Size
import javafx.scene.paint.Color
import midi.Op1
import midi.Op1.*
import p5.PaperSize
import p5.ProcessingAppK
import processing.core.PConstants
import processing.core.PVector
import processing.event.KeyEvent
import processing.event.MouseEvent
import kotlin.math.roundToInt

class Moire : ProcessingAppK() {
    data class Params(
        var radiusStep: Float = ANGLE_STEP / 2,
        var center: PVector = PVector(SIZE.width / 2f, SIZE.height / 2f),
        var revolutions: Int = 50,
        var coordOffset: PVector,
        var noiseOffset: PVector,
        var noiseVectorScale: Float,
        var noiseScale: Float = 0.5f,
        val noiseSeed: Long
    )

    private var shapeParams: MutableList<Params> = mutableListOf()
    private var current = 0
    private var colorCurrent = true

    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
    }

    override fun setup() {
        super.setup()

        noiseDetail(2, 0.3f)

        noLoop()
        addShape()
        setupOp1()
    }

    private fun buildSpiral(p: Params): List<PVector> {
        val angles = generateSequence(0f) { it + ANGLE_STEP }
        val radii = generateSequence(0f) { it + p.radiusStep }

        return (angles zip radii)
            .take((p.revolutions * 2 * Math.PI / ANGLE_STEP).roundToInt())
            .map { PVector(it.second * cos(it.first), it.second * sin(it.first)) }
            .toList()
    }

    private fun buildPerlinSpiral(p: Params): List<PVector> {
        val spiral = mutableListOf<PVector>()
        noiseSeed(p.noiseSeed)

        var a = 0f
        var r = 0f
        val totalSteps = (p.revolutions * 2 * PI / ANGLE_STEP).roundToInt()
        for (i in 0..totalSteps) {
            val noisePosition = spiralCoord(a, r).mult(p.noiseVectorScale).add(p.noiseOffset)
            val noisyRadius = r - r * (noise(noisePosition) * p.noiseScale)
            val spiralCoordOffset = p.coordOffset.copy().mult(i.toFloat() / totalSteps)
            spiral.add(spiralCoord(a, noisyRadius).add(spiralCoordOffset))
            a += ANGLE_STEP
            r += p.radiusStep
        }

        return spiral
    }

    private fun spiralCoord(a: Float, r: Float) = PVector(r * cos(a), r * sin(a))

    override fun drawInternal() {
        if (!record) {
            background(255)
        }
        noFill()
        stroke(0)
        shapeParams.withIndex().forEach { (i, p) ->
            push()
            if (colorCurrent && !record && i == current) {
                stroke(toRgbHex(Color.ORANGERED))
            }
            beginShape()
            translate(p.center.x, p.center.y)
            buildPerlinSpiral(p).map(this::curveVertex)
            endShape()
            pop()
        }
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.button == PConstants.LEFT) {
            shapeParams[current].center = PVector(mouseX.toFloat(), mouseY.toFloat())
        } else if (e.button == PConstants.RIGHT) {
            addShape()
        }
        redraw()
    }

    override fun reset() {
        shapeParams = mutableListOf()
        current = 0
        addShape()
    }

    override fun op1ControllerChangeRel(cc: Op1, channel: Int, value: Int) {
        val p = shapeParams[current]
        when (cc to channel) {
            BLUE_KNOB to 0 ->
                p.radiusStep += value * 0.0005f
            GREEN_KNOB to 0 ->
                p.revolutions += value
            WHITE_KNOB to 0 ->
                p.noiseVectorScale += value * 0.0001f
            ORANGE_KNOB to 0 ->
                p.noiseScale += value * 0.01f
            BLUE_KNOB to 1 ->
                p.coordOffset.x += value * 0.5f
            GREEN_KNOB to 1 ->
                p.coordOffset.y += value * 0.5f
            WHITE_KNOB to 1 ->
                p.noiseOffset.x += value * 0.05f
            ORANGE_KNOB to 1 ->
                p.noiseOffset.y += value * 0.05f
            BLUE_KNOB to 2 ->
                p.center.add(value.toFloat(), 0f)
            GREEN_KNOB to 2 ->
                p.center.add(0f, value.toFloat())
            else ->
                when (cc) {
                    // Work on any channel
                    BLUE -> addShape()
                    GREEN ->
                        current = (current + 1) % shapeParams.size
                    HELP ->
                        colorCurrent = !colorCurrent
                    else ->
                        super.op1ControllerChangeRel(cc, channel, value)
                }
        }
        println(p)
    }


    override fun keyTyped(e: KeyEvent) {
        when (e.key) {
            in KEY_TO_KNOB -> {
                val pair = KEY_TO_KNOB[e.key]!!
                op1ControllerChangeRel(pair.first, 0, pair.second)
            }
            in KEY_TO_BUTTON -> op1ControllerChangeRel(KEY_TO_BUTTON[e.key]!!, 0, 1)
            else -> return
        }
        redraw()
    }

    private fun addShape() {
        shapeParams.add(
            Params(
                coordOffset = PVector(random(0f, 3f), random(0f, 3f)),
                noiseSeed = random(1000000f).toLong(),
                noiseVectorScale = random(0.002f, 0.008f),
                noiseOffset = PVector(random(0f, 3f), random(0f, 3f))
            )
        )
        current = shapeParams.size - 1
    }

    private fun curveVertex(v: PVector) = curveVertex(v.x, v.y)
    private fun noise(v: PVector) = noise(v.x, v.y, v.z)

    companion object {
        val SIZE = Size(461, 699)
        //  val SIZE = PaperSize.ORIGAMI_150.size
        const val ANGLE_STEP = 0.3f

        private val KNOB_TO_KEYS = mapOf(
            BLUE_KNOB to Pair('a', 'z'),
            GREEN_KNOB to Pair('s', 'x'),
//            WHITE_KNOB to Pair('l', 'j'),
//            ORANGE_KNOB to Pair('k', 'i')
        )

        private val KEY_TO_KNOB = KNOB_TO_KEYS.entries
            .map { (knob, keys) -> listOf(keys.first to (knob to 1), keys.second to (knob to -1)) }
            .flatten()
            .toMap()

        private val KEY_TO_BUTTON = mapOf('1' to BLUE, '2' to GREEN, '3' to HELP, '4' to REC)

        fun run() = Moire().runSketch()
    }
}

fun main() {
    Moire.run()
}
