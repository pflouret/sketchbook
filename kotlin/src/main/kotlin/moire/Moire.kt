package moire

import com.krab.lazy.PickerColor
import gui.LazyGuiControlDelegate
import gui.clearFolderChildren
import gui.clearNodeTreeCache
import midi.MidiController
import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PVector
import processing.event.MouseEvent
import util.Size
import kotlin.math.roundToInt

class Moire : ProcessingAppK() {
    private var animate = false

    private var addShape: Boolean by LazyGuiControlDelegate("button")
    private val shapes = mutableListOf<Shape>()

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()

        initMidi(MidiController.FIGHTER)
        initGui()

        noiseDetail(4, 0.2f)

        if (!animate) {
            noLoop()
        }

        // FIXME: load from saved state
        if (shapes.isEmpty()) {
            addShape()
        }
    }

    override fun reset() {
        shapes.clear()
        gui.clearFolderChildren("shapes")
        gui.clearNodeTreeCache()
        redraw()
    }

    override fun draw() {
        if (!exportNextFrameSvg) {
            background(255)
        }
        noFill()
        stroke(0)

        if (addShape) {
            addShape()
        }

        shapes.forEach(Shape::draw)
    }

    private fun addShape() {
        shapes.add(Shape(shapes.size))
    }

    override fun controllerChangeRel(channel: Int, cc: Int, value: Int) {
//        if (channel != 0) {
//            return
//        }
//
//        val p = shapeParams[current]
//        when (cc) {
//            0 -> p.radiusStep += value * 0.0005f
//            1 -> p.revolutions += value
//            2 -> p.noiseVectorScale += value * 0.0001f
//            3 -> p.noiseScale += value * 0.01f
//            4 -> p.coordOffset.x += value * 0.5f
//            5 -> p.coordOffset.y += value * 0.5f
//            6 -> p.noiseOffset.x += value * 0.05f
//            7 -> p.noiseOffset.y += value * 0.05f
//            8 -> p.center.add(value.toFloat(), 0f)
//            9 -> p.center.add(0f, value.toFloat())
//            else ->
//                super.controllerChangeRel(channel, cc, value)
//        }
//        println(p)
    }

    override fun controllerChangeAbs(channel: Int, cc: Int, value: Int) {
//        if (channel != 1 || value != 0) {
//            return
//        }
//
//        val p = shapeParams[current]
//        when (cc) {
//            0 -> addShape()
//            1 -> current = (current + 1) % shapeParams.size
//            2 -> colorCurrent = !colorCurrent
//            else ->
//                super.controllerChangeAbs(channel, cc, value)
//        }
//
//        println(p)
    }

    override fun mouseClicked(e: MouseEvent) {
//        super.mouseClicked(e)
//        if (e.button == PConstants.LEFT) {
//            shapeParams[current].center = PVector(mouseX.toFloat(), mouseY.toFloat())
//        }
//        redraw()
    }

    inner class Shape(index: Int) {
        private val folder = "shapes/$index"
        private val internalFolder = "shapes/$index/internal"
        private var radiusStep: Float by LazyGuiControlDelegate("slider", folder, ANGLE_STEP / 2)
        private var revolutions: Float by LazyGuiControlDelegate("slider", folder, 100f)
        private var center: PVector by LazyGuiControlDelegate("plotXY", folder, PVector(w2, h2))
        private var coordinateOffset: PVector by LazyGuiControlDelegate(
            "plotXY",
            folder,
            PVector(random(3), random(3))
        )
        private var noiseOffset: PVector by LazyGuiControlDelegate(
            "plotXY",
            folder,
            PVector(random(3), random(3))
        )
        private var noiseVectorScale: Float by LazyGuiControlDelegate(
            "slider",
            folder,
            random(0.002f, 0.005f)
        )
        private var noiseScale: Float by LazyGuiControlDelegate("slider", folder, 0.5f)
        private var noiseSeed: Int by LazyGuiControlDelegate(
            "sliderInt",
            internalFolder,
            random(1000000).toInt()
        )
        private var color: PickerColor by LazyGuiControlDelegate("colorPicker", folder, 0f)

        fun draw() {
            withPush {
                stroke(color.hex)
                withShape {
                    translate(center)
                    buildPerlinSpiral().map(::curveVertex)
                }
            }

            update()
        }

        private fun update() {
            if (!animate) {
                return
            }
            noiseOffset = noiseOffset.add(randomVector(0.0004f, 0.0004f))
        }

        private fun buildPerlinSpiral(): List<PVector> {
            val spiral = mutableListOf<PVector>()
            noiseSeed(noiseSeed.toLong())

            var a = 0f
            var r = 0f
            val totalSteps = (revolutions * 2 * PI / ANGLE_STEP).roundToInt()
            for (i in 0..totalSteps) {
                val noisePosition = spiralCoord(a, r)
                    .mult(noiseVectorScale)
                    .add(noiseOffset)
                val noisyRadius = r - r * (noise(noisePosition) * noiseScale)
                val spiralCoordOffset = coordinateOffset
                    .copy()
                    .mult(i.toFloat() / totalSteps)
                spiral.add(spiralCoord(a, noisyRadius).add(spiralCoordOffset))
                a += ANGLE_STEP
                r += radiusStep
            }

            return spiral
        }

        private fun spiralCoord(a: Float, r: Float) = PVector(r * cos(a), r * sin(a))
    }

    companion object {
        const val ANGLE_STEP = 0.3f

        val SIZE = Size(700, 700)
        //  val SIZE = PaperSize.ORIGAMI_150.size
    }
}

fun main() {
    PApplet.runSketch(arrayOf("moire.Moire"), null)
}
