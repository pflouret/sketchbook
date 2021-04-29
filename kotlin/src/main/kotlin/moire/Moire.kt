package moire

import com.sun.glass.ui.Size
import controlP5.ControlListener
import controlP5.ControlP5
import javafx.scene.paint.Color
import midi.Op1
import midi.Op1.*
import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PVector
import processing.event.KeyEvent
import processing.event.MouseEvent
import kotlin.math.roundToInt

class Moire : ProcessingAppK() {
    data class Params(
        var radiusStep: Float = ANGLE_STEP / 2,
        var modStep: Float = 0.01f,
        var center: PVector = PVector(SIZE.width/2f, SIZE.height/2f),
        var revolutions: Int = 50
    )

    var shapeParams: MutableList<Params> = mutableListOf()
    var current = 0
    var colorCurrent = true

    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
    }

    override fun setup() {
        super.setup()

        noiseDetail(2, 0.3f)

        noLoop()
        addShape()
        setupOp1()
//        ControlFrame(this, 300, 500)
    }

    private fun buildSpiral(p: Params): List<PVector> {
        val angles = generateSequence(0f) { it + ANGLE_STEP }
        val radii = generateSequence(0f) { it + p.radiusStep }
        var mods = generateSequence(random(10000f)) { it + p.modStep }
            .map(this::noise)
            .map { it * 5 }
        mods = generateSequence(0f) { it }

        return (angles zip radii zip mods)
            .take((p.revolutions * 2 * Math.PI / ANGLE_STEP).roundToInt())
            .map { Triple(it.first.first, it.first.second, it.second) }
            .map(
                fun(triple): PVector {
                    val a = triple.first
                    val r = triple.second
                    val mod = triple.third
                    return PVector(mod + r * cos(a), mod + r * sin(a))
                })
            .toList()
    }

    override fun drawInternal() {
        if (!record) {
            background(255)
        }
        noFill()
        stroke(0)
        shapeParams.withIndex().forEach {
            push()
            if (colorCurrent && !record && it.index == current) {
                stroke(toRgbHex(Color.ORANGERED))
            }
            beginShape()
            translate(it.value.center.x, it.value.center.y)
            buildSpiral(it.value).map(this::curveVertex)
            endShape()
            pop()
        }
    }

    fun setRadiusStepForCurrent(step: Float): Unit {
        shapeParams[current].radiusStep = step
    }

    fun getRadiusStepForCurrent(): Float {
        return shapeParams[current].radiusStep
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.button == PConstants.LEFT) {
            shapeParams[current].center = PVector(mouseX.toFloat(), mouseY.toFloat())
        } else if (e.button == PConstants.RIGHT) {
            addShape();
        }
        redraw()
    }

    override fun reset() {
        shapeParams = mutableListOf(Params())
        current = 0
    }

    override fun op1ControllerChangeRel(cc: Op1, channel: Int, value: Int) {
        val p = shapeParams[current]
        when (cc) {
            BLUE_KNOB -> p.radiusStep += value * 0.0005f
            GREEN_KNOB -> p.revolutions += value
            WHITE_KNOB -> p.center.add(value.toFloat(), 0f)
            ORANGE_KNOB -> p.center.add(0f, value.toFloat())
            BLUE -> addShape()
            GREEN -> current = (current + 1) % shapeParams.size
            HELP -> colorCurrent = !colorCurrent
            else -> super.op1ControllerChangeRel(cc, channel, value)
        }
    }


    override fun keyTyped(e: KeyEvent) {
        when(e.key) {
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
        shapeParams.add(Params())
        current = shapeParams.size - 1
    }

    private fun vertex(v: PVector) = vertex(v.x, v.y)
    private fun curveVertex(v: PVector) = curveVertex(v.x, v.y)

    companion object {
        val SIZE = Size(461, 699)
        const val ANGLE_STEP = 0.3f

        private val KNOB_TO_KEYS = mapOf(
            BLUE_KNOB to Pair('a', 'z'),
            GREEN_KNOB to Pair('s', 'x'),
            WHITE_KNOB to Pair('l', 'j'),
            ORANGE_KNOB to Pair('k', 'i')
        )

        private val KEY_TO_KNOB = KNOB_TO_KEYS.entries
            .map { (knob, keys) -> listOf(keys.first to (knob to 1), keys.second to (knob to -1)) }
            .flatten()
            .toMap()

        private val KEY_TO_BUTTON = mapOf('1' to BLUE, '2' to GREEN, '3' to HELP, '4' to REC)

        fun run() = Moire().runSketch()
    }
}

internal class ControlFrame(var parent: Moire, var w: Int, var h: Int) : PApplet() {
    private var cp: ControlP5? = null
    override fun settings() {
        size(w, h)
    }

    override fun setup() {
        surface.setLocation(10, 10)

        cp = ControlP5(this)
        val g1 = cp!!.addGroup("g1")
        cp!!.addSlider("radius step")
            .setRange(0f, 0.02f)
            .setDecimalPrecision(4)
            .setValue(parent.getRadiusStepForCurrent())
            .plugTo(parent, "setRadiusStepForCurrent")
            .setSize(200, 30)
            .setPosition(5f, 56f)
            .setGroup(g1)
        cp!!.addBang("reset")
            .setPosition(5f, 101f)
            .setSize(20, 20)
            .setGroup(g1)
            .plugTo(parent, "reset")
        cp!!.addListener(ControlListener {
            parent.postEvent(MouseEvent(parent, 0, MouseEvent.CLICK, 0, -1, -1, -1, 1))
        })
    }

    override fun draw() {
        background(0)
    }


    init {
        runSketch(arrayOf(this.javaClass.name), this)
    }
}

fun main() {
    Moire.run()
}
