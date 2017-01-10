package io.github.pflouret.sketchbook.experiments;

import controlP5.Group;
import hype.*;
import hype.extended.behavior.HRotate;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.stream.IntStream;

public class Wires3d extends ProcessingApp {

    private int numWires = 5;
    private int numVertices = 4;
    private float spacing = 7;
    private float shapeStrokeWidth = 5.5f;
    private PGraphics noiseLayer;

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }

    @Override
    public void settings() {
        size(640, 640, P3D);
    }

    @Override
    public void setup() {
        super.setup();

        H.init(this);
        H.use3D(true);
        H.background(255);

        newFigure();
        newNoiseLayer();

        buildGui();
    }

    private HShape newShape() {
        PShape ps = createShape();
        ps.beginShape();
        ps.stroke(0, 200);
        ps.strokeWeight(shapeStrokeWidth);
        ps.strokeCap(PROJECT);
        ps.noFill();
        IntStream.range(0, numVertices).forEach(i -> {
            ps.vertex(random(-100, 100), random(-150, 150), random(-110, 110));
        });
        ps.endShape(CLOSE);

        ps.getWidth();
        ps.getHeight();

        return (HShape) new HShape(ps)
            .loc(w2 - spacing * numWires / 2.0f, h2);
    }

    public void newFigure() {
        HDrawablePool pool = new HDrawablePool(numWires);
        pool.autoAddToStage();
        pool.add(newShape());
        pool.onCreate(new HCallback() {
            @Override
            public void run(Object obj) {
                HShape hs = (HShape) obj;
                int i = pool.currentIndex();

                hs.loc(hs.loc().add(i * spacing, 0));
                /*
                new HOscillator()
                    .target(hs)
                    .property(H.ROTATIONY)
                    //.waveform(H.TRIANGLE)
                    .speed(0.5f)
                    .addAmplifier(new HOscillator().range(0, 360))
                    .range(0, 720);
                    */
                new HRotate(hs, 1, 1, 0)
                    .speedY(1)
                    .register();
            }
        });
        pool.requestAll();
    }

    private void newNoiseLayer() {
        noiseLayer = createGraphics(width, height);
        noiseLayer.beginDraw();
        noiseLayer.loadPixels();
        for (int i = 0; i < noiseLayer.pixels.length; i++) {
            noiseLayer.pixels[i] = color(random(0, 55), 90);
        }
        noiseLayer.updatePixels();
        noiseLayer.endDraw();
    }

    @Override
    public void reset() {
        for (Iterator<HDrawable> it = H.stage().iterator(); it.hasNext(); it.remove(), it.next()) {
        }
        newFigure();
        clear();
    }

    @Override
    public void buildGui() {
        super.buildGui();

        Group g1 = cp.addGroup("g1");
        cp.addSlider("spacing")
            .setRange(1, 80)
            .setSize(200, 20)
            .setPosition(0, 0)
            .setGroup(g1)
            ;
        cp.addSlider("numWires")
            .setRange(1, 8)
            .setSize(200, 20)
            .setPosition(0, 0)
            .setPosition(0, 21)
            .setGroup(g1)
        ;
        cp.addSlider("numVertices")
            .setRange(1, 8)
            .setSize(200, 20)
            .setPosition(0, 42)
            .setGroup(g1)
        ;
        cp.addSlider("shapeStrokeWidth")
            .setRange(1, 10)
            .setSize(200, 20)
            .setPosition(0, 63)
            .setGroup(g1)
        ;
        cp.addBang("newFigure")
            .setSize(20, 20)
            .setPosition(0, 84)
            .plugTo(this, "reset")
            .setGroup(g1)
        ;
    }

    @Override
    public void draw() {
        PVector loc = new PVector(w2 - spacing * numWires / 2.0f, h2);
        for (Iterator<HDrawable> it = H.stage().iterator(); it.hasNext(); loc.add(spacing, 0)) {
            HShape s = (HShape)it.next();
            s.shape().beginShape();
            s.shape().strokeWeight(shapeStrokeWidth);
            s.shape().endShape(CLOSE);
            s.loc(loc);
        }
        draw1();
    }

    private void draw1() {
        if (frameCount % 2 == 0) {
            newNoiseLayer();
        }
        H.drawStage();
        image(noiseLayer, 0, 0);
    }

    private void draw2() {
        clear();
        if (frameCount % 2 == 0) {
            newNoiseLayer();
        }
        H.autoClears(false);
        image(noiseLayer, 0, 0);
        H.drawStage();
    }

    private void draw3() {
        H.autoClears(false);
        H.drawStage();
    }
}

