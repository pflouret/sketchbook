package lines

import p5.NoiseWave
import p5.PaperSize
import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PVector
import processing.event.KeyEvent
import kotlin.math.absoluteValue

class Agents : ProcessingAppK() {
    private var speed = 30
    private var octaves = 2
    private var falloff = 0.15f
    private lateinit var agents: MutableList<Agent>
    private lateinit var noiseWaves: MutableList<List<PVector>>

    private val a = random(-PI / 4, -PI / 4 + PI / 8)
    private val b = -random(a, a + PI / 7)
    private val c = -random(b, b + PI / 7)

    inner class Agent() {
        private val points = mutableListOf<PVector>()
        var done = false

        init {
            points.add(randVector())
        }

        fun move() {
            if (!withinBounds()) {
                done = true
                return
            }
            val p = points.last()

            val rotation = when {
                p.y < height / 4 -> 0f
                p.y < 2*height / 4 -> a
                p.y < 3*height / 4 -> b
                else -> c
            }
            val n = noise(p.x / width * 1.2f, p.y / height)
            val noiseAngle = map(n, 0, 1, -2 * PI, 0).absoluteValue + rotation
            points.add(
                PVector.add(p, PVector.fromAngle(noiseAngle).mult(5f))
            )
        }

        fun draw() {
            withShape {
                points.forEach(::curveVertex)
            }
        }

        fun withinBounds(): Boolean {
            return points.last().let { it.x in 0f..widthf && it.y in 0f..heightf }
//            val waveFrom = noiseWaves[noiseWaveIndex]
//            val waveTo = noiseWaves.getOrNull(noiseWaveIndex+1)
//            return points.last().let { p ->
//                if (p.x !in 0f..widthf || p.y !in 0f..heightf) {
//                    return false
//                }
//                val fromY = waveFrom[floor(p.x)].lerp(waveFrom[ceil(p.x)], ceil(p.x) - p.x).y
//                val toY = waveTo?.let { wave ->
//                    wave[floor(p.x)].lerp(wave[ceil(p.x)], ceil(p.x) - p.x).y
//                } ?: heightf
//
//                p.y in fromY..toY
//            }
        }
    }

    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()
        stroke(0, 15f)
        strokeWeight(0.8f)
        reset()
    }

    override fun reset() {
        background(255)
        noiseSeed(random(9999999).toLong())
//        initNoiseWaves()
        initAgents()
    }

    override fun draw() {
        background(255)
        noFill()
        noiseDetail(octaves, falloff)

//        withPush {
//            stroke(255f, 0f, 0f)
//            noiseWaves.forEach {
//                withShape {
//                    it.forEach(::curveVertex)
//                }
////                stroke(0);
////                it.forEach({p -> circle(p.x, p.y, 10f)})
//            }
//        }

        for (i in 0 until speed) {
            agents.addAll(
                agents.mapNotNull {
                    it.move()
                    if (!it.done && !it.withinBounds()) Agent() else null
                }
            )
        }
        agents.forEach(Agent::draw)
    }

    private fun initAgents() {
        agents = MutableList(25, { Agent() })
    }

    private fun initNoiseWaves() {
        noiseDetail(1)
        var offset = random(height / 10f, height / 4f)
        val wave = NoiseWave(this, 0f, 1 / 40f, 80f, offset)

        noiseWaves = mutableListOf()
        noiseWaves.add( (0..width).map { PVector(it.toFloat(), 0f) } )
        while (offset < height - 30) {
            noiseWaves.add( (0..width).map { PVector(it.toFloat(), wave.update()) } )
            offset = (offset + random(100, 150)).also { wave.offset = it }
        }
        noiseDetail(octaves)
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
//            'a' -> agents.addAll(List(10, { Agent(PVector(random(width), random(height))) }))
            else -> super.keyTyped(event)
        }
        println("f=%.2f o=${octaves} s=${speed}".format(falloff))
    }

    companion object {
        //        val SIZE = PaperSize.US_LETTER.size
        val SIZE = PaperSize.ORIGAMI_150.size
    }
}

fun main() {
    PApplet.runSketch(arrayOf("lines.Agents"), null)
}
