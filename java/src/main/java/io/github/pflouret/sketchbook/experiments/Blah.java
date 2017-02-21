package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import io.github.pflouret.sketchbook.p5.TVec;
import processing.core.PApplet;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;

public class Blah extends ProcessingApp {
    @Override
    public void setup() {
        super.setup();

        translate(w2, h2);

        TVec a = new TVec(100, 0);
        TVec b = new TVec(150, -50);
        float d = a.distanceTo(b);
        float minD = 100;

        strokeWeight(7);
        point(a.x, a.y);
        stroke(255, 0, 0);
        point(b.x, b.y);
        stroke(0, 0, 255);
        Vec2D c = a.add(a.sub(b).perpendicular());
        strokeWeight(1);
        stroke(0);
        //c.normalizeTo(minD - c.distanceTo(b));
        line(a.x, a.y, c.x, c.y);
        strokeWeight(3);
        point(c.x, c.y);
        strokeWeight(1);
        stroke(0, 100);
        ellipse(a.x, a.y, minD, minD);
        ellipse(b.x, b.y, minD, minD);
        ellipse(c.x, c.y, minD, minD);

        //line(a.x, a.y, b.x, b.y);
    }

    @Override
    public void draw() {
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

