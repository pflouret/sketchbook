package io.github.pflouret.sketchbook.bluenoise;

import controlP5.CallbackListener;
import controlP5.Group;
import io.github.pflouret.sketchbook.p5.BaseControlFrame;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.Rect;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.Vector;
import java.util.function.BiFunction;

public class Shapes extends ProcessingApp {
    private Vector<Vec2D> points;
    private int color;
    private float alpha, minDist, offset;
    private boolean noStroke = false, useNoise = false;

    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();

        alpha = random(20, 60);
        minDist = random(8, 12);
        color = color(random(255), random(255), random(255));
        offset = random(2, 5);

        new ControlFrame(this, 300, 500);

        noiseDetail(6, .93f);

        reset();
        noLoop();
    }

    @Override
    public void reset() {
        clear();

        points = new Vector<>();
        addPoints();
    }


    private void addPoints() {
        Circle circle = new Circle(w2, h2, 350);
        Rect rect = new Rect(100, 100, width-200, height-200);

        BiFunction<Float, Float, Float> distribution = useNoise ? this::noise : PoissonDiskSampler.NOOP_DISTRIBUTION;
        points.addAll(new PoissonDiskSampler(circle, minDist, distribution, getRandom()).sample(8000));
    }

    @Override
    public void draw() {
        clear();

        if (noStroke) {
            noStroke();
        } else {
            stroke(color, alpha);
        }
        fill(color, alpha / 1.1f);

        points.forEach(p -> wonkyEllipse(p, offset));
    }

    @Override
    public void keyPressed() {
        switch(key) {
            case 'a':
                addPoints(); break;
            default:
                super.keyPressed();
        }
        redraw();
    }

    class ControlFrame extends BaseControlFrame {
        ControlFrame(ProcessingApp parent, int w, int h) {
            super(parent, w, h);
        }

        public void setup() {
            super.setup();

            Group g1 = cp.addGroup("g1");

            CallbackListener reset = e -> parent.postControlEvent(new ControlFrameEvent("reset", null));

            cp.addSlider("alpha")
                .setRange(0, 255)
                //.setNumberOfTickMarks(15)
                .setSize(200, 30)
                .setPosition(5, 5)
                .setGroup(g1)
            ;

            cp.addSlider("minDist")
                .setRange(1, 30)
                .setSize(200, 30)
                .setPosition(5, 36)
                .setGroup(g1)
                .onChange(reset)
            ;

            cp.addSlider("offset")
                .setRange(1, 30)
                .setSize(200, 30)
                .setPosition(5, 67)
                .setGroup(g1)
            ;

            cp.addToggle("noStroke")
                .setPosition(5, 98)
                .setSize(30, 30)
                .setGroup(g1)
            ;

            cp.addToggle("useNoise")
                .setPosition(55, 98)
                .setSize(30, 30)
                .setGroup(g1)
                .onChange(reset)
            ;

            cp.addBang("reset")
                .setPosition(105, 98)
                .setSize(30, 30)
                .setGroup(g1)
            ;

            cp.addColorWheel("color", 10, 149, 200)
                .setGroup(g1)
            ;

            initValues();

            surface.setLocation(10, 10);
            surface.setSize(w, h);
            surface.setResizable(true);
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

