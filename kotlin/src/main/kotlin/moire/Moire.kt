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
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class Moire : ProcessingAppK() {
    private var animate = false

    private val shapes = mutableListOf<Shape>()
    private var addSpiral: Boolean by LazyGuiControlDelegate("button")
    private var addLinesRect: Boolean by LazyGuiControlDelegate("button")
    private var current: Int by LazyGuiControlDelegate("numberText", "", 0)

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()

        initMidi(MidiController.FIGHTER)
        initGui()

        noiseDetail(5, 0.3f)

        if (!animate) {
            noLoop()
        }

        // FIXME: load from saved state
        if (shapes.isEmpty()) {
//            addSpiral()
            addLinesRect()
        }
    }

    override fun reset() {
        shapes.clear()
        gui.clearFolderChildren("shapes")
        gui.clearNodeTreeCache()
        redraw()
    }

    override fun draw() {
        if (addSpiral) {
            addSpiral()
        }
        if (addLinesRect) {
            addLinesRect()
        }

        if (!exportNextFrameSvg) {
            if (frameCount != 1 && shapes.none { it.changed() }) {
                return
            }
            background(255)
        }
        noFill()
        stroke(0)

        shapes.forEach(Shape::draw)
    }

    private fun addSpiral() {
        shapes.add(Spiral(shapes.size))
        current = shapes.size - 1
    }

    private fun addLinesRect() {
        shapes.add(LinesRect(shapes.size))
        current = shapes.size - 1
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
            0 -> addSpiral()
            1 -> current = (current + 1) % shapes.size
            else ->
                super.controllerChangeAbs(channel, cc, value)
        }
    }

    override fun keyTyped(event: KeyEvent) {
        super.keyTyped(event)
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

    abstract inner class Shape(index: Int) {
        val folder = "shapes/$index"
        val internalFolder = "shapes/$index/internal"

        var prevParamValues: Map<String, Any?> = mapOf()

        abstract fun draw()
        open fun mousePressed(e: MouseEvent) {}
        open fun controllerChangeRel(channel: Int, cc: Int, value: Int) {}

        inline fun <reified T : Shape> T.getParamMap(): Map<String, Any?> {
            return T::class.declaredMemberProperties
                .asSequence()
                .onEach { it.isAccessible = true }
                .filter { (it.getDelegate(this) as? LazyGuiControlDelegate<*>) != null }
                .associate {
                    it.name to it.get(this).let { value ->
                        when (value) {
                            is PickerColor -> value.hex
                            else -> value
                        }
                    }
                }
        }

        abstract fun changed(): Boolean
        inline fun <reified T : Shape> T.paramsChanged() = prevParamValues != getParamMap()
        inline fun <reified T : Shape> T.saveParamValues() {
            prevParamValues = getParamMap()
        }

    }

    inner class Spiral(index: Int) : Shape(index) {
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
        private var spiral = buildPerlinSpiral()

        override fun changed() = paramsChanged()

        override fun draw() {
            if (paramsChanged()) {
                spiral = buildPerlinSpiral()
                saveParamValues()
            }

            withPush {
                stroke(color.hex)
                withShape {
                    translate(center)
                    spiral.map(::curveVertex)
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

        override fun mousePressed(e: MouseEvent) {
            if (e.button == PConstants.LEFT) {
                center = PVector(mouseX.toFloat(), mouseY.toFloat())
            }
        }

        override fun controllerChangeRel(channel: Int, cc: Int, value: Int) {
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

    inner class LinesRect(index: Int) : Shape(index) {
        private var xStepSeparation: Float by LazyGuiControlDelegate("slider", folder, random(2, 5))
        private var xSteps: Int by LazyGuiControlDelegate("sliderInt", folder, 100)
        private var ySteps: Int by LazyGuiControlDelegate("sliderInt", folder, 300)
        private var yHeight: Float by LazyGuiControlDelegate("slider", folder, 400f)
        private var angle: Float by LazyGuiControlDelegate(
            "slider",
            folder,
            random(-0.007f, 0.007f)
        )
        private var topLeft: PVector by LazyGuiControlDelegate(
            "plotXY", folder, randomVector(100f, 100f)
        )

        //        private var coordinateOffset: PVector by LazyGuiControlDelegate(
//            "plotXY", folder, PVector(random(3), random(3))
//        )
        private var noiseOffset: PVector by LazyGuiControlDelegate(
            "plotXY", folder, PVector(random(3), random(3))
        )
        private var noisePositionScale: Float by LazyGuiControlDelegate(
            "slider", folder, random(0.007f, 0.07f)
        )
        private var noiseScale: Float by LazyGuiControlDelegate("slider", folder, 10f)
        private var noiseSeed: Int by LazyGuiControlDelegate(
            "sliderInt", internalFolder, random(1000000).toInt()
        )
        private var color: PickerColor by LazyGuiControlDelegate("colorPicker", folder, 0f)

        private val offsetOffsetStep = random(0.03f)
        private var lines = buildPerlinLines()

        override fun changed() = paramsChanged()

        override fun draw() {
            if (getParamMap() != prevParamValues) {
                lines = buildPerlinLines()
                prevParamValues = getParamMap()
            }

            withPush {
                stroke(color.hex)
                translate(topLeft)
                rotate(angle)
                buildPerlinLines().map {
                    withShape {
                        it.map(::curveVertex)
                    }
                }
            }
//            update()
        }

        private val noiseOffsets = mutableMapOf(
            "radius" to randomNoiseOffset(),
            "revolutions" to randomNoiseOffset(),
            "scale" to randomNoiseOffset(),
            "vScale" to randomNoiseOffset(),
            "color" to randomNoiseOffset(),
            "coordX" to randomNoiseOffset(),
            "coordY" to randomNoiseOffset(),
            "topLeftX" to randomNoiseOffset(),
            "topLeftY" to randomNoiseOffset(),
        )

        private fun randomNoiseOffset() = random(100000f)
        private fun mappedNoise(offsetKey: String, scale: Float = 0.0001f) =
            scale * map(noise(noiseOffsets[offsetKey]!!), 0f, 1f, -0.5f, 1f)

//        private fun update() {
//            if (!looping) {
//                return
//            }
//            noiseOffset = noiseOffset.add(randomVector(0.0008f, 0.0004f))
////            steps += mappedNoise("radius", 0.00001f)
//            noiseScale += mappedNoise("scale", 0.001f)
//            noisePositionScale += mappedNoise("vScale", 0.000001f)
//            coordinateOffset =
//                coordinateOffset.add(mappedNoise("coordX", 0.01f), mappedNoise("coordY", 0.01f))
//            topLeft =
//                topLeft.add(mappedNoise("centerX", 0.01f), mappedNoise("centerY", 0.01f))
//
//            noiseOffsets.keys.forEach {
//                noiseOffsets[it] = noiseOffsets[it]!! + offsetOffsetStep
//            }
//        }

        private fun buildPerlinLines(): List<List<PVector>> {
            noiseSeed(noiseSeed.toLong())
            val yStepLen = yHeight / ySteps
            return (0..xSteps).map { x ->
                (0..ySteps).map { y ->
                    val noise = noise(
                        PVector(x * xStepSeparation, y.toFloat())
                            .mult(noisePositionScale)
                            .add(noiseOffset)
                    )
                    PVector(x * xStepSeparation + noise * noiseScale, yStepLen * y)
                }
            }
        }

        override fun mousePressed(e: MouseEvent) {
            if (e.button == PConstants.LEFT) {
                topLeft = PVector(mouseX.toFloat(), mouseY.toFloat())
            }
        }

        override fun controllerChangeRel(channel: Int, cc: Int, value: Int) {
            when (cc) {
                0 -> xStepSeparation += value * 0.01f
                1 -> ySteps += value
                2 -> noisePositionScale += value * 0.0001f
                3 -> noiseScale += value * 0.1f

                4 -> xSteps += value
                5 -> yHeight += value
                6 -> angle += value * 0.001f

                8 -> noiseOffset = noiseOffset.add(value * 0.005f, 0f)
                9 -> noiseOffset = noiseOffset.add(0f, value * 0.005f)
                10 -> topLeft = topLeft.add(value.toFloat(), 0f)
                11 -> topLeft = topLeft.add(0f, value.toFloat())
            }
        }
    }

    companion object {
        const val ANGLE_STEP = 0.3f

        val SIZE = Size(1000, 1000)
//        val SIZE = PaperSize.INDEX_CARD.size
//        val SIZE = PaperSize.INDEX_CARD.landscape()
//        val SIZE = PaperSize.ORIGAMI_150.size
    }
}

fun main() {
    PApplet.runSketch(arrayOf("moire.Moire"), null)
}
