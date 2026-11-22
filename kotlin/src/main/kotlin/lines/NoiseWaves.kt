package lines

import p5.NoiseWave
import p5.PaperSize
import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PVector
import processing.event.KeyEvent
import kotlin.math.absoluteValue

class NoiseWaves: ProcessingAppK() {
    private var speed = 300
    private var octaves = 2
    private var falloff = 0.15f
    private lateinit var waves: MutableList<List<PVector>>
    private lateinit var rotationAngles: MutableList<Float>

    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()
        stroke(0, 25f)
        strokeWeight(0.8f)
        reset()
    }

    override fun reset() {
        background(255)
        noiseSeed(random(9999999).toLong())
    }

    override fun draw() {
        background(255)
        noFill()
        noiseDetail(octaves, falloff)
        withPush {
            stroke(255f, 0f, 0f)
            waves.forEach { withShape { it.forEach(::curveVertex) } }
        }
    }

    private fun initNoiseWaves() {
        noiseDetail(2, 0.5f)
        var offset = random(height / 10f, height / 4f)
        val wave = NoiseWave(this, 0f, 1 / 40f, 80f, offset)

        waves = mutableListOf()
        waves.add( (0..width).map { PVector(it.toFloat(), 0f) } )
        while (offset < height - 30) {
            waves.add( (0..width).map { PVector(it.toFloat(), wave.update()) } )
            offset = (offset + random(100, 150)).also { wave.offset = it }
        }
        noiseDetail(octaves)

        rotationAngles = mutableListOf(random())
        for (i in 1 until waves.size) {
            rotationAngles.add(-random(rotationAngles.last(), -PI/7))
        }
    }

    //    private fun randVector() = PVector(random(width), height.toFloat())
    private fun randVector() = PVector(random(width), random(height))

    override fun keyTyped(event: KeyEvent) {
        when (event.key) {
            '1' -> falloff -= 0.05f
            '2' -> falloff += 0.05f
            '3' -> octaves -= 1
            '4' -> octaves += 1
            '5' -> speed -= 10
            '6' -> speed += 10
            'z' -> reset()
//            'a' -> agents.addAll(List(10, { Agent(PVector(random(width), random(height))) }))
            else -> super.keyTyped(event)
        }
        println("f=%.2f o=${octaves} s=${speed}".format(falloff))
    }

    companion object {
        val SIZE = PaperSize.US_LETTER.size
//        val SIZE = PaperSize.ORIGAMI_150.size
    }
}

fun main() {
    PApplet.runSketch(arrayOf("lines.Agents"), null)
}
