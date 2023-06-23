package moire

import javafx.scene.paint.Color
import midi.MidiController
import p5.ProcessingAppK
import processing.core.PConstants
import processing.core.PVector
import processing.event.MouseEvent
import util.Size
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
        size(SIZE.width, SIZE.height, P2D)
    }

    override fun setup() {
        super.setup()
        initMidi(MidiController.FIGHTER)

        noiseDetail(5, 0.3f)

        noLoop()
        addShape()
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

    private fun spiralCoord(a: Float, r: Float) = PVector(r * cos(a), r * sin(a))

    override fun reset() {
        shapeParams = mutableListOf()
        current = 0
        addShape()
    }

    override fun drawInternal() {
        if (!exportNextFrameSvg) {
            background(255)
        }
        noFill()
        stroke(0)
        shapeParams.withIndex().forEach { (i, p) ->
            push()
            if (colorCurrent && !exportNextFrameSvg && i == current) {
                stroke(toRgbHex(Color.ORANGERED))
            }
            beginShape()
            translate(p.center.x, p.center.y)
            buildPerlinSpiral(p).map(this::curveVertex)
            endShape()
            pop()
        }

    }

    override fun controllerChangeRel(channel: Int, cc: Int, value: Int) {
        if (channel != 0) {
            return
        }

        val p = shapeParams[current]
        when (cc) {
            0 -> p.radiusStep += value * 0.0005f
            1 -> p.revolutions += value
            2 -> p.noiseVectorScale += value * 0.0001f
            3 -> p.noiseScale += value * 0.01f
            4 -> p.coordOffset.x += value * 0.5f
            5 -> p.coordOffset.y += value * 0.5f
            6 -> p.noiseOffset.x += value * 0.05f
            7 -> p.noiseOffset.y += value * 0.05f
            8 -> p.center.add(value.toFloat(), 0f)
            9 -> p.center.add(0f, value.toFloat())
            else ->
                super.controllerChangeRel(channel, cc, value)
        }
        println(p)
    }

    override fun controllerChangeAbs(channel: Int, cc: Int, value: Int) {
        if (channel != 1 || value != 0) {
            return
        }

        val p = shapeParams[current]
        when (cc) {
            0 -> addShape()
            1 -> current = (current + 1) % shapeParams.size
            2 -> colorCurrent = !colorCurrent
            else ->
                super.controllerChangeAbs(channel, cc, value)
        }

        println(p)
    }

    //    override fun keyTyped(e: KeyEvent) {
//        when (e.key) {
//            in KEY_TO_KNOB -> {
//                val mapping = KEY_TO_KNOB[e.key]!!
//                controllerChangeRel(mapping.knob, mapping.channel, mapping.value)
//            }
//
//            in KEY_TO_BUTTON -> controllerChangeRel(KEY_TO_BUTTON[e.key]!!, 0, 1)
//            'a', 'z' -> controllerChangeRel(BLUE_KNOB, 0, if (e.key == 'a') 1 else -1)
//            'A', 'Z' -> controllerChangeRel(BLUE_KNOB, 1, if (e.key == 'A') 1 else -1)
//            else -> return
//        }
//        redraw()
//    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.button == PConstants.LEFT) {
            shapeParams[current].center = PVector(mouseX.toFloat(), mouseY.toFloat())
        }
        redraw()
    }


    companion object {
        val SIZE = Size(700, 700)

        //  val SIZE = PaperSize.ORIGAMI_150.size
        const val ANGLE_STEP = 0.3f

        fun run() = Moire().runSketch()
    }
}

fun main() {
    Moire.run()
}
