package lines

import p5.PaperSize
import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PVector
import kotlin.math.absoluteValue

class Agents : ProcessingAppK() {
    private var speed = 200
    private var octaves = 2
    private var falloff = 0.4f
    private lateinit var agents: List<PVector>

    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()
        background(255)
        stroke(0, 20f)
        strokeWeight(0.8f)

        agents = List(25, { PVector(random(width), random(height)) })
    }

    override fun reset() {
        background(255)
        noiseSeed(random(9999999).toLong())
    }

    override fun draw() {
        noFill()
        for (i in 0 until speed) {
            drawAgents2d()
        }
    }

    private fun drawAgents2d() {
        noiseDetail(octaves, falloff)
        agents.forEach {
            if (it.x.toInt() !in 0..width && it.y.toInt() !in 0..height) {
                it.set(random(width), random(height))
            }

            point(it)
            moveAgent2d(it, noise(it.x / width, it.y / height))
        }
    }

    private fun moveAgent2d(a: PVector, n: Float) {
        a.add(PVector.fromAngle(map(n, 0, 1, -2 * PI, 0).absoluteValue))

    }

    companion object {
//        val SIZE = PaperSize.US_LETTER.size
        val SIZE = PaperSize.ORIGAMI_150.size
    }
}

fun main() {
    PApplet.runSketch(arrayOf("lines.Agents"), null)
}
