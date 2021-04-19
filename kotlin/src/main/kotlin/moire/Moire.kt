package moire

import com.sun.glass.ui.Size
import controlP5.ControlListener
import controlP5.ControlP5
import javafx.scene.paint.Color
import lombok.Data
import midi.Op1
import midi.Op1.*
import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PVector
import processing.event.MouseEvent
import kotlin.math.roundToInt

class Moire : ProcessingAppK() {
    @Data
    class Params(
        var radiusStep: Float = 0.005f,
        var modStep: Float = 0.01f,
        var center: PVector = PVector(SIZE.width/2f, SIZE.height/2f),
        var revolutions: Int = 50 //(SIZE.width.coerceAtMost(SIZE.height) / 2 / (1 + radiusStep)).toInt()
    )

    var shapeParams: MutableList<Params> = mutableListOf()
    var current = 0
    var colorCurrent = true

    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
    }

    override fun setup() {
        super.setup()

        background(255)
        stroke(0)
        noLoop()
        noiseDetail(2, 0.3f)
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
            .take((2 * p.revolutions * Math.PI / 0.01f).roundToInt())
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
        background(255)
        shapeParams.withIndex().forEach {
            push()
            if (colorCurrent && !record && it.index == current) {
                stroke(toRgbHex(Color.ORANGERED))
            }
            beginShape()
            translate(it.value.center.x, it.value.center.y)
            buildSpiral(it.value).map(this::vertex)
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
            BLUE_KNOB -> {
                p.radiusStep += value * 0.00005f
                p.revolutions += -1*value
            }
            GREEN_KNOB -> p.revolutions += value
            WHITE_KNOB -> p.center.add(value.toFloat(), 0f)
            ORANGE_KNOB -> p.center.add(0f, value.toFloat())
            BLUE -> addShape()
            GREEN -> current = (current + 1) % shapeParams.size
            HELP -> colorCurrent = !colorCurrent
            else -> super.op1ControllerChangeRel(cc, channel, value)
        }
    }

    private fun addShape() {
        shapeParams.add(Params())
        current = shapeParams.size - 1
    }

    private fun vertex(v: PVector) = vertex(v.x, v.y)

    companion object {
        val SIZE = Size(461, 699)
        const val ANGLE_STEP = 0.009f
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
