package io.github.pflouret.sketchbook.growth.blob;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.Vector;
import java.util.stream.IntStream;

public class Blobs extends ProcessingApp {
    static Blobs app;
    private Vector<Blob> blobs;
    private float r = 2;

    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();
        app = this;

        strokeWeight(1.5f);

        blobs = new Vector<>();
        IntStream.range(0, 10)
            .forEach(i -> blobs.add(new Blob(new Vec2D(random(r, width-r), random(r, height-r)), 6, r)));
    }

    @Override
    public void draw() {
        clear();
        blobs.forEach(b -> b.draw(g));
        blobs.stream().filter(b -> !b.isStatic).forEach(b -> b.update(blobs));
        if (frameCount % 20 == 0) {
            Vec2D origin = new Vec2D(random(r, width-r), random(r, height-r));
            blobs.add(new Blob(origin, 15, r));
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

