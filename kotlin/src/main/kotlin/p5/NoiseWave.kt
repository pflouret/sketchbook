package p5

import processing.core.PApplet
import toxi.math.waves.AbstractWave

class NoiseWave(private val app: PApplet, phase: Float, freq: Float, amp: Float, offset: Float) :
    AbstractWave(phase, freq, amp, offset) {
    private var climb = 0f
    private var dir: Int


    init {
        dir = if (app.random(1f) < 0.5f) -1 else 1
    }

    override fun update(): Float {
        val n = PApplet.map(app.noise(phase * frequency), 0f, 1f, -amp / 2, amp / 2)
        var y = n + offset + climb

        if (app.random(1f) < 0.005f || y <= 0 || y >= app.height || climb > app.random(10f, 40f)) {
            dir = -dir
            y = PApplet.constrain(y, 15f, (app.height - 15).toFloat())
        }

        climb += dir * .2f
        phase += .8f

        return y
    }
}