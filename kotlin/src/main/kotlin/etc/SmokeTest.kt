package etc

import p5.ProcessingAppK


class SmokeTest : ProcessingAppK() {
    var h = 0f
    override fun settings() {
        size(500, 500, FX2D)
    }

    override fun setup() {
        super.setup()
        background(255)
        colorMode(HSB, 1f, 1f, 1f, 1f)
        loop()
    }

    override fun drawInternal() {
        background(h, 0.9f, 0.9f, 1f);
        h = (h+0.001f) % 1f
    }

    companion object {
        fun run() = SmokeTest().runSketch()
    }

}

fun main() {
    SmokeTest.run();
}
