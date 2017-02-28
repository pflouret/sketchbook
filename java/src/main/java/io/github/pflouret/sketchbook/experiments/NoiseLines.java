package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.PVec;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PVector;

import java.lang.invoke.MethodHandles;

public class NoiseLines extends ProcessingApp {
    private PVec origin;

    @Override
    public void setup() {
        super.setup();
        stroke(0, 10);
        origin = new PVec(random(width), random(height));
    }

    @Override
    public void draw() {
        float resolution = 0.01f;
        PVec mouse = new PVec((float) mouseX, (float) mouseY);
        float d = PVector.dist(origin, mouse);
        int steps = round(d/resolution);
        for (int i=0; i < steps; i++) {
            if (random(1) < 0.01f) {
                point(PVector.lerp(origin, mouse, ((float)i)/steps));
            }
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

