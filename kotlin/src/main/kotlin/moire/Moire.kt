package moire

import com.krab.lazy.PickerColor
import gui.LazyGuiControlDelegate
import gui.clearFolderChildren
import gui.clearNodeTreeCache
import midi.MidiController
import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PVector
import processing.event.KeyEvent
import processing.event.MouseEvent
import util.Size
import kotlin.math.roundToInt

class Moire : ProcessingAppK() {
    private var animate = true

    private val shapes = mutableListOf<Shape>()
    private var addShape: Boolean by LazyGuiControlDelegate("button")
    private var current: Int by LazyGuiControlDelegate("numberText", "", 0)

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()

        initMidi(MidiController.FIGHTER)
        initGui()

        noiseDetail(3, 0.3f)

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
        if (channel == 0) {
            shapes[current].controllerChangeRel(channel, cc, value)
        }
    }

    override fun controllerChangeAbs(channel: Int, cc: Int, value: Int) {
        if (channel != 1 || value != 0) {
            return
        }

        when (cc) {
            0 -> addShape()
            1 -> current = (current + 1) % shapes.size
            else ->
                super.controllerChangeAbs(channel, cc, value)
        }
    }

    override fun keyTyped(event: KeyEvent) {
        super.keyTyped()
        if (event.key in '0'..'9') {
            controllerChangeAbs(1, event.key.toString().toInt() - 1, 0)
        }
    }

    override fun mouseDragged(event: MouseEvent) = mousePressed(event)
    override fun mousePressed(event: MouseEvent) {
        if (gui.isMouseOutsideGui) {
            super.mousePressed(event)
            shapes[current].mousePressed(event)
            redraw()
        }
    }

    inner class Shape(index: Int) {
        private val folder = "shapes/$index"
        private val internalFolder = "shapes/$index/internal"
        private var radiusStep: Float by LazyGuiControlDelegate("slider", folder, ANGLE_STEP / 2)
        private var revolutions: Float by LazyGuiControlDelegate("slider", folder, 100f)
        private var center: PVector by LazyGuiControlDelegate(
            "plotXY",
            folder,
            PVector(w2, h2).add(random(-25, 25), random(-25, 25))
        )
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
            random(0.0007f, 0.005f)
        )
        private var noiseScale: Float by LazyGuiControlDelegate("slider", folder, 0.5f)
        private var noiseSeed: Int by LazyGuiControlDelegate(
            "sliderInt",
            internalFolder,
            random(1000000).toInt()
        )
        private var color: PickerColor by LazyGuiControlDelegate("colorPicker", folder, 0f)

        private val offsetOffsetStep = random(0.03f)

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

        private val noiseOffsets = mutableMapOf(
            "radius" to randomNoiseOffset(),
            "revolutions" to randomNoiseOffset(),
            "scale" to randomNoiseOffset(),
            "vScale" to randomNoiseOffset(),
            "color" to randomNoiseOffset(),
            "coordX" to randomNoiseOffset(),
            "coordY" to randomNoiseOffset(),
            "centerX" to randomNoiseOffset(),
            "centerY" to randomNoiseOffset(),
        )

        private fun randomNoiseOffset() = random(100000f)
        private fun mappedNoise(offsetKey: String, scale: Float = 0.0001f) =
            scale * map(noise(noiseOffsets[offsetKey]!!), 0f, 1f, -0.5f, 1f)

        private fun update() {
            if (!looping) {
                return
            }
            noiseOffset = noiseOffset.add(randomVector(0.0008f, 0.0004f))
            radiusStep += mappedNoise("radius", 0.00001f)
            noiseScale += mappedNoise("scale", 0.001f)
            noiseVectorScale += mappedNoise("vScale", 0.000001f)
            coordinateOffset =
                coordinateOffset.add(mappedNoise("coordX", 0.01f), mappedNoise("coordY", 0.01f))
            center =
                center.add(mappedNoise("centerX", 0.01f), mappedNoise("centerY", 0.01f))

            noiseOffsets.keys.forEach {
                noiseOffsets[it] = noiseOffsets[it]!! + offsetOffsetStep
//                noiseOffsets[it]!!.add(0.01f, 0.01f)
            }
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

        fun mousePressed(e: MouseEvent) {
            if (e.button == PConstants.LEFT) {
                center = PVector(mouseX.toFloat(), mouseY.toFloat())
            }
        }

        fun controllerChangeRel(channel: Int, cc: Int, value: Int) {
            when (cc) {
                0 -> radiusStep += value * 0.0005f
                1 -> revolutions += value
                2 -> noiseVectorScale += value * 0.0001f
                3 -> noiseScale += value * 0.01f
                4 -> coordinateOffset = coordinateOffset.add(value * 0.5f, 0f)
                5 -> coordinateOffset = coordinateOffset.add(0f, value * 0.5f)
                6 -> noiseOffset = noiseOffset.add(PVector(value * 0.05f, 0f))
                7 -> noiseOffset = noiseOffset.add(PVector(0f, value * 0.05f))
                8 -> center = center.add(value.toFloat(), 0f)
                9 -> center = center.add(0f, value.toFloat())
            }
        }
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
