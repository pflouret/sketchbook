package moire

import gui.LazyGuiControlDelegate
import gui.clearFolderChildren
import gui.clearNodeTreeCache
import gui.plotAdd
import midi.MidiController
import p5.ProcessingAppK
import processing.core.PVector
import processing.event.MouseEvent
import util.Size
import kotlin.math.roundToInt

class Moire : ProcessingAppK() {
    private var animate = false

    private var addShape: Boolean by LazyGuiControlDelegate("button")
    private var shapeCount: Int by LazyGuiControlDelegate("sliderInt", "internal")

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

        if (shapeCount == 0) {
            addShape()
        }
    }

    override fun reset() {
        shapeCount = 0
        gui.clearFolderChildren("shapes")
        gui.clearNodeTreeCache()
        redraw()
    }

    override fun drawInternal() {
        if (!exportNextFrameSvg) {
            background(255)
        }
        noFill()
        stroke(0)

        if (addShape) {
            addShape()
        }

        for (i in 0 until shapeCount) {
            gui.pushFolder("shapes/$i")
            push()
            stroke(gui.colorPicker("color").hex)
            beginShape()
            translate(gui.plotXY("center"))
            buildPerlinSpiral().map(this::curveVertex)
            endShape()
            pop()
            updateShapeParams()
            gui.popFolder()
        }
    }

    private fun addShape() {
        val i = shapeCount

        gui.slider("shapes/$i/radius step", ANGLE_STEP / 2)
        gui.slider("shapes/$i/revolutions", 100f)
        gui.plotXY("shapes/$i/center", w2, h2)
        gui.plotXY("shapes/$i/coordinate offset", random(3), random(3))
        gui.plotXY("shapes/$i/noise offset", random(3), random(3))
        gui.slider("shapes/$i/noise vector scale", random(0.002f, 0.008f))
        gui.slider("shapes/$i/noise scale", 0.5f)
        gui.sliderInt("shapes/$i/noise seed", random(1000000f).toInt())
        gui.colorPicker("shapes/$i/color", 0f)

        gui.hide("s/$i/noise seed")
        shapeCount += 1
    }

//    private fun buildSpiral(): List<PVector> {
//        val angles = generateSequence(0f) { it + ANGLE_STEP }
//        val radii = generateSequence(0f) { it + p.radiusStep }
//
//        return (angles zip radii)
//            .take((p.revolutions * 2 * Math.PI / ANGLE_STEP).roundToInt())
//            .map { PVector(it.second * cos(it.first), it.second * sin(it.first)) }
//            .toList()
//    }

    private fun buildPerlinSpiral(): List<PVector> {
        val spiral = mutableListOf<PVector>()
        noiseSeed(gui.sliderInt("noise seed").toLong())

        var a = 0f
        var r = 0f
        val totalSteps = (gui.slider("revolutions") * 2 * PI / ANGLE_STEP).roundToInt()
        for (i in 0..totalSteps) {
            val noisePosition = spiralCoord(a, r)
                .mult(gui.slider("noise vector scale"))
                .add(gui.plotXY("noise offset"))
            val noisyRadius = r - r * (noise(noisePosition) * gui.slider("noise scale"))
            val spiralCoordOffset = gui.plotXY("coordinate offset")
                .copy()
                .mult(i.toFloat() / totalSteps)
            spiral.add(spiralCoord(a, noisyRadius).add(spiralCoordOffset))
            a += ANGLE_STEP
            r += gui.slider("radius step")
        }

        return spiral
    }

    private fun updateShapeParams() {
        if (!animate) {
            return
        }

//        gui.sliderAdd("radius step", random(-0.001f, 0.001f))
//        gui.sliderAdd("revolutions", random(-0.001f, 0.001f))
//        gui.plotAdd("center", random(-0.0002f, 0.0002f), random(-0.0002f, 0.0002f))
//        gui.plotAdd(
//            "coordinate offset",
//            random(-0.00001f, 0.00001f),
//            random(-0.00001f, 0.00001f)
//        )
        gui.plotAdd("noise offset", randomVector(0.0004f, 0.0004f))
//        gui.sliderAdd("noise vector scale", random(-0.00002f, 0.00002f))
//        gui.sliderAdd("noise scale", random(-0.0001f, 0.0001f))
    }

    private fun spiralCoord(a: Float, r: Float) = PVector(r * cos(a), r * sin(a))

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
