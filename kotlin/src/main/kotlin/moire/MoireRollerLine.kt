package moire

import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PVector
import util.Size

class MoireRollerLine : ProcessingAppK() {
    private val noisePositionScale = 0.015f
    private val noiseScale = 10f
    private val multiplier = 5

    private var x = 0f
    private var noiseX = 0f
    private lateinit var points: MutableList<PVector>
    private var row = 0

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()

        noiseDetail(5, 0.3f)
        background(255)

//        noLoop()

//        points = (0..900).map { nextPoint() }.toMutableList()
        points = mutableListOf()
        update()
    }

    override fun reset() {
        redraw()
    }

    override fun draw() {
        noFill()
        stroke(0)

        val i = points.size - multiplier
        withPush {
            translate(50f, ROW_COLS + 50f - VERTICAL_SPACING * row)
            withShape {
                points.slice(i until points.size).forEach(::curveVertex)
            }
        }
        update()
    }

    private fun update() {
        repeat(multiplier) {
            points.add(nextPoint())
        }

        if (points.size % ROW_COLS == 0) {
            row++
            x = 0f
//            points.clear()
        }
    }

    private fun nextPoint(): PVector {
        val noise = noise(
            PVector(noiseX++, 0f).mult(noisePositionScale)
        )
        return PVector(x++, noise * noiseScale)
    }


    companion object {
        val SIZE = Size(1000, 1000)
        const val ROW_COLS = 900
        const val VERTICAL_SPACING = 12f
    }
}

fun main() {
    PApplet.runSketch(arrayOf("moire.MoireRollerLine"), null)
}
