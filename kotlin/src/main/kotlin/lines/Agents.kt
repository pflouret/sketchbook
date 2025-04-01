package lines

import p5.NoiseWave
import p5.PaperSize
import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PVector
import processing.event.KeyEvent
import kotlin.math.absoluteValue

class Agents : ProcessingAppK() {
    private var speed = 300
    private var octaves = 2
    private var falloff = 0.15f
    private var showNoiseWaves = true
    private lateinit var agents: MutableList<Agent>
    private lateinit var noiseWaves: MutableList<List<PVector>>
    private lateinit var rotationAngles: MutableList<Float>

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

            val rotation = rotationAngles[noiseWaveAreaIndex(p)]
            val n = noise(p.x / width * 1.2f, p.y / height)
            val noiseAngle = map(n, 0, 1, -2 * PI, 0).absoluteValue + rotation
            points.add(
                PVector.add(p, PVector.fromAngle(noiseAngle))
            )
        }

        fun draw() {
            withShape {
                points.forEach(::curveVertex)
            }
        }

        private fun noiseWaveAreaIndex(p: PVector): Int {
            for (i in 0 until noiseWaves.size - 1) {
                val waveFrom = noiseWaves[i]
                val waveTo = noiseWaves[i+1]
                val fromY = PVector.lerp(
                    waveFrom[floor(p.x)],
                    waveFrom[ceil(p.x)],
                    ceil(p.x) - p.x
                ).y
                val toY = PVector.lerp(
                    waveTo[floor(p.x)],
                    waveTo[ceil(p.x)],
                    ceil(p.x) - p.x
                ).y
                if (p.y in fromY..toY) {
                    return i
                }
            }
            return noiseWaves.size - 1
        }

        fun withinBounds() =
            points.last().let { it.x in 0f..widthf && it.y in 0f..heightf }
    }

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

    private fun reset(resetNoiseWaves: Boolean) {
        background(255)
        noiseSeed(random(9999999).toLong())
        if (resetNoiseWaves) {
            initNoiseWaves()
        }
        initAgents()
    }
    override fun reset() {
        reset(true)
    }

    override fun draw() {
        background(255)
        noFill()
        noiseDetail(octaves, falloff)

        for (i in 0 until speed) {
            agents.addAll(
                agents.mapNotNull {
                    it.move()
                    if (!it.done && !it.withinBounds()) Agent() else null
                }
            )
        }
        agents.forEach(Agent::draw)

        if (showNoiseWaves) {
            withPush {
                stroke(255f, 0f, 0f)
                noiseWaves.forEach { withShape { it.forEach(::curveVertex) } }
            }
        }
    }

    private fun initAgents() {
        agents = MutableList(250, { Agent() })
    }

    private fun initNoiseWaves() {
        noiseDetail(2, 0.5f)
        var offset = random(height / 10f, height / 4f)
        val wave = NoiseWave(this, 0f, 1 / 40f, 80f, offset)

        noiseWaves = mutableListOf()
        noiseWaves.add( (0..width).map { PVector(it.toFloat(), 0f) } )
        while (offset < height - 30) {
            noiseWaves.add( (0..width).map { PVector(it.toFloat(), wave.update()) } )
            offset = (offset + random(100, 150)).also { wave.offset = it }
        }
        noiseDetail(octaves)

        rotationAngles = mutableListOf(random())
        for (i in 1 until noiseWaves.size) {
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
            'o' -> showNoiseWaves = !showNoiseWaves
            'z' -> reset(false)
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
