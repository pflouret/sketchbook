package agents

import midi.BS
import midi.MidiDevice
import p5.PaperSize
import p5.ProcessingAppK
import processing.core.PVector
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class Agents : ProcessingAppK() {
    data class Params(
        var seed: Long,
        var octaves: Float = 3f,
        var falloff: Float = 0.5f,
        var speed: Int = 1,
        var maxAgents: Int = 2000,
        var offset: PVector = PVector(0f, 0f)

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

            val noiseVal =
                noise(last().x / width + params.offset.x, last().y / height + params.offset.y)
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
        frameRate(30f)

        background(255)
        noiseSeed(params.seed)

        stroke(0f, 150f)
        strokeWeight(0.3f)

        setupBeatStep()

        agents = buildAgents()
//        noLoop()
//        exportNextFrameSvg = true
    }

    override fun drawInternal() {
//        background(255)
        noFill()
        noiseDetail(params.octaves.roundToInt(), params.falloff)
        for (i in params.speed downTo 0) {
            agents.filter { !it.done }.forEach {
//                it.draw()
                point(it.last())
                it.move()
            }
            if (agents.size < params.maxAgents) {
                repeat(agents.count { it.done }) { agents.add(Agent()) }
            }
        }
    }

    override fun controllerChangeRel(cc: MidiDevice, channel: Int, value: Int): Boolean {
        when (cc) {
            BS.KNOB_1 -> params.octaves += value * 0.2f
            BS.KNOB_2 -> params.falloff += value * 0.01f
            BS.KNOB_3 -> params.offset.add(value*0.1f, 0f)
            BS.KNOB_4 -> params.offset.add(0f, value*0.1f)
            BS.KNOB_11 -> params.maxAgents += value
            BS.KNOB_12 -> params.speed += value
            BS.PAD_1 -> reset(false)
            else -> return super.controllerChangeRel(cc, channel, value)
        }
        println(params)
        return true
    }

    private fun buildAgents() = MutableList(20) { Agent() }

    override fun reset() = reset(newSeed = true)
    private fun reset(newSeed: Boolean = true) {
        if (newSeed) {
            params.seed = random(9999f).toLong()
        }
        agents = buildAgents()
        background(255)
        noiseSeed(params.seed)
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
