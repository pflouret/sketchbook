package agents

import p5.PaperSize
import p5.ProcessingAppK
import processing.core.PVector
import kotlin.math.absoluteValue

class Agents : ProcessingAppK() {
    data class Params(
        var seed: Long,
        var octaves: Int = 3,
        var falloff: Float = 0.5f,
        var speed: Int = 1,

//        var coordOffset: PVector,
//        var noiseOffset: PVector,
//        var noiseVectorScale: Float,
//        var noiseScale: Float = 0.5f,
//        var max_step: Float = 0.9f,
//        var scale: Int = 500,
    )

    inner class Agent : ArrayList<PVector>() {
        var done = false

        init {
            add(randomVector())
        }

        fun draw() = withShape {
            forEach { curveVertex(it) }
        }

        fun move() {
            if (done) {
                return
            }

            val noiseVal = noise(last().x / width, last().y / height)
            val angle = map(noiseVal, 0f, 1f, -2 * PI, 0f)
            add(last().copy().add(PVector.fromAngle(angle.absoluteValue)))
            if (!inViewport(last())) {
                done = true
            }
        }

    }

    private var agents = mutableListOf<Agent>()
    private var params = Params(seed = random(9999999f).toLong())

    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
    }

    override fun setup() {
        super.setup()
        background(255)
        noiseSeed(params.seed)

        stroke(0f, 150f)
        strokeWeight(0.3f)

        agents = buildAgents()
//        noLoop()
//        exportNextFrameSvg = true
    }

    override fun drawInternal() {
        background(255)
        noFill()
        noiseDetail(params.octaves, params.falloff)
        for (i in params.speed downTo 0) {
            agents.forEach {
                it.draw()
                it.move()
            }
            repeat(agents.count { it.done }) { agents.add(Agent()) }
        }
    }

    private fun buildAgents() = MutableList(20) { Agent() }

    override fun reset() {
        agents = buildAgents()
        background(255)
        noiseSeed(random(9999f).toLong())
    }

    companion object {
//        val SIZE = PaperSize.INDEX_CARD.size
        val SIZE = PaperSize.ORIGAMI_150.size
//        val SIZE = PaperSize.TRAVELER_PASSPORT.size
//        val SIZE = PaperSize.POSTCARD.size
        fun run() = Agents().runSketch()
    }
}

fun main() {
    Agents.run()
}
