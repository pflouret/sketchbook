package io.github.pflouret.sketchbook.p5;

import processing.core.PApplet;
import toxi.math.waves.AbstractWave;

public class NoiseWave extends AbstractWave {
    private PApplet app;
    private float climb = 0;
    private int dir;


    public NoiseWave(PApplet app, float phase, float freq, float amp, float offset) {
        super(phase, freq, amp, offset);
        this.app = app;
        dir = app.random(1) < 0.5f ? -1 : 1;
    }

    @Override
    public float update() {
        float n = PApplet.map(app.noise(phase*frequency), 0, 1, -amp/2, amp/2);
        float y = n + offset + climb;

        if (app.random(1) < 0.005f || y <= 0 || y >= app.height || climb > app.random(10, 40)) {
            dir = -dir;
            y = PApplet.constrain(y, 15, app.height-15);
        }

        climb += dir * .2f;
        phase += .8f;

        return y;
    }
}
