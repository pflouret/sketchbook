package moire

import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PVector
import util.Size

class MoireRollerLine : ProcessingAppK() {
    private val noisePositionScale = 0.01f
    private val noiseScale = 1f

    private var x = 0f
    private var noiseX = 0f
    private lateinit var points: MutableList<PVector>

    companion object {
        val SIZE = Size(1000, 50)
        const val ROW_COLS = (3000 * 3.78).toInt()
    }

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()

        noiseDetail(5, 0.3f)
        background(255)


        noLoop()
        initPoints()
    }

    override fun reset() {
        initPoints()
        redraw()
    }

    private fun initPoints() {
        x = 0f
        points = (0 until ROW_COLS).map { nextPoint() }.toMutableList()
    }

    override fun draw() {
        noFill()
        stroke(0)

        background(255)
        withPush {
            withShape(closeMode = CLOSE) {
                points.forEach(::curveVertex)
                points.reversed().forEach { curveVertex(it.x, it.y + 2) }
            }
        }
    }


    private fun nextPoint(): PVector {
        val noise = noise(
            PVector(noiseX++, 0f).mult(noisePositionScale)
        )
        val v = PVector(x, noise * noiseScale)
        x += 10
        return v
    }
}

fun main() {
    PApplet.runSketch(arrayOf("moire.MoireRollerLine"), null)
}
