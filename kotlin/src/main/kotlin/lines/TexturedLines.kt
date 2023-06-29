package lines

import p5.PaperSize
import p5.ProcessingAppK
import kotlin.math.roundToInt

class TexturedLines : ProcessingAppK() {
    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
    }

    override fun setup() {
        super.setup()
        background(255)
        stroke(0f, 50f)
//        noiseDetail(5, 0.3f)
        noLoop()
        exportNextFrameSvg = true
    }

    override fun reset() {
    }

    private fun hardcodedPoly() {
        generateSequence(0f) { it + 1f }
            .take(100)
            .forEach {
                line(15f, 15f, w2 + it, (height + it) * 0.7f)
            }
    }

    private fun circles() {
        generateSequence(0f) { it + 1f }
            .take(w2.roundToInt() + 1)
            .forEach {
                arc(0f, 0f, (w2 - it).coerceAtLeast(0.1f), w2, -HALF_PI, TWO_PI - HALF_PI - 0.0001f)
            }
    }

    private fun jitter() = 0//if (prob(0.1)) random(10f) else random(3f)
    private fun randAngle() = random(0f, TWO_PI)

    private fun circleBounce() {
        val r = width / 4
        val initial = randAngle() to randAngle()
        generateSequence(initial) { (_, b) -> b to randAngle() }
            .take(500)
            .forEach { (a, b) ->
                line(
                    r * cos(a) + jitter(),
                    r * sin(a) + jitter(),
                    r * cos(b) + jitter(),
                    r * sin(b) + jitter()
                )
            }

    }

    override fun draw() {
        noFill()
        translate(w2, h2)
        circles()
//        circleBounce()
    }

    companion object {
//        val SIZE = PaperSize.INDEX_CARD.size
//        val SIZE = PaperSize.ORIGAMI_150.size
//        val SIZE = PaperSize.TRAVELER_PASSPORT.size
        val SIZE = PaperSize.POSTCARD.size
        fun run() = TexturedLines().runSketch()
    }
}

fun main() {
    TexturedLines.run()
}
