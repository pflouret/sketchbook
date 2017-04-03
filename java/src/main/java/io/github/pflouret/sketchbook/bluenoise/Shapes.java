package io.github.pflouret.sketchbook.bluenoise;

import controlP5.CallbackListener;
import controlP5.Group;
import io.github.pflouret.sketchbook.p5.ShapeMaker;
import io.github.pflouret.sketchbook.p5.BaseControlFrame;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PStyle;
import toxi.geom.Polygon2D;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Shapes extends ShapeMaker {
    private Vector<BlueNoiseShape> shapes;
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
        super.reset();
        shapes = new Vector<>();
        clear();
    }


    @Override
    protected void onNewPoly(Polygon2D poly) {
        BlueNoiseShape shape = new BlueNoiseShape(poly);
        shape.offset = offset;
        shape.drawPoint = p -> wonkyEllipse(p, shape.offset);
        shapes.add(shape);
        update();
    }

    public void update() {
        if (!shapes.isEmpty()) {
            BiFunction<Float, Float, Float> distribution = useNoise ? this::noise : PoissonDiskSampler.NOOP_DISTRIBUTION;
            BlueNoiseShape s = shapes.lastElement();
            s.sampler = new PoissonDiskSampler(s.poly, minDist, distribution, getRandom());
            s.style = g.getStyle();
            s.offset = offset;
            s.update();
        }
        redraw();
    }

    @Override
    protected void drawInternal() {
        clear();

        if (noStroke) {
            noStroke();
        } else {
            stroke(color, alpha);
        }
        fill(color, alpha / 1.1f);

        super.drawInternal();

        shapes.forEach(BlueNoiseShape::draw);
    }

    @Override
    public void keyPressed() {
        switch(key) {
            case 'a':
                break;
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

            CallbackListener update = e -> parent.postControlEvent(new ControlFrameEvent("update", null));

            cp.addSlider("alpha")
                .setValue(alpha)
                .setRange(0, 255)
                //.setNumberOfTickMarks(15)
                .setSize(200, 30)
                .setPosition(5, 5)
                .setGroup(g1)
                .onChange(update)
            ;

            cp.addSlider("minDist")
                .setValue(minDist)
                .setRange(1, 30)
                .setSize(200, 30)
                .setPosition(5, 36)
                .setGroup(g1)
                .onChange(update)
            ;

            cp.addSlider("offset")
                .setValue(offset)
                .setRange(1, 30)
                .setSize(200, 30)
                .setPosition(5, 67)
                .setGroup(g1)
                .onChange(update)
            ;

            cp.addToggle("noStroke")
                .setValue(noStroke)
                .setPosition(5, 98)
                .setSize(30, 30)
                .setGroup(g1)
                .onChange(update)
            ;

            cp.addToggle("useNoise")
                .setValue(useNoise)
                .setPosition(55, 98)
                .setSize(30, 30)
                .setGroup(g1)
                .onChange(update)
            ;

            cp.addBang("reset")
                .setPosition(105, 98)
                .setSize(30, 30)
                .setGroup(g1)
            ;

            cp.addColorWheel("color", 10, 149, 200)
                .setRGB(color)
                .setGroup(g1)
                .onChange(update)
            ;

            surface.setLocation(10, 10);
            surface.setSize(w, h);
            surface.setResizable(true);
        }
    }

    class BlueNoiseShape {
        Polygon2D poly;
        PoissonDiskSampler sampler;
        List<Vec2D> points;
        Consumer<Vec2D> drawPoint = Shapes.this::point;
        PStyle style;
        float offset;

        BlueNoiseShape(Polygon2D poly) {
            this.poly = poly;
        }

        void update() {
            points = sampler.sample(20000);
        }


        public void draw() {
            push();
            style(style);
            points.forEach(drawPoint);
            pop();
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

