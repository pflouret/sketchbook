package io.github.pflouret.sketchbook.lines;

import controlP5.ControlP5;
import controlP5.Group;
import io.github.pflouret.sketchbook.p5.NoiseWave;
import io.github.pflouret.sketchbook.p5.PVec;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class OffsetStripes extends ProcessingApp {
    private int stripeWidth = 5;
    private float textureOffset;
    private PImage texture;
    private List<LinkedList<PVec>> curves;
    private List<PShape> shapes;

    @Override
    public void settings() {
        size(800, 800, P2D);

    }

    @Override
    public void setup() {
        super.setup();

        textureWrap(REPEAT);
        noStroke();
        noLoop();
        noiseDetail(3, .45f);

        reset();

        new ControlFrame(this, 300, 150);

    }

    @Override
    public void reset() {
        textureOffset = stripeWidth;
        buildTexture();
        buildCurves();
        buildShapes();
    }

    private void buildTexture() {
        PGraphics pg = createGraphics(2*stripeWidth, 1);
        pg.beginDraw();
        pg.background(255);
        pg.fill(0);
        pg.noStroke();
        pg.rect(0, 0, stripeWidth, pg.height);
        pg.endDraw();
        texture = pg.get();
    }

    private void buildCurves() {
        curves = new ArrayList<>();

        float offset = random(height/10.0f, height/4.0f);
        NoiseWave wave = new NoiseWave(this, 0, 1/30f, 100, offset);

        curves.add(Stream.of(pv(0, 0), pv(width, 0)).collect(Collectors.toCollection(LinkedList::new)));

        while (offset < height - 30) {
            curves.add(IntStream.rangeClosed(0, width)
                .mapToObj(x -> pv(x, wave.update()))
                .collect(Collectors.toCollection(LinkedList::new)));
            offset += random(60, 100);
            wave.offset = offset;
        }

        curves.add(Stream.of(pv(0, height), pv(width, height)).collect(Collectors.toCollection(LinkedList::new)));
    }

    private void buildShapes() {
        shapes = new ArrayList<>(curves.size());
        for (int i=0; i < curves.size()-1; i++) {
            shapes.add(buildShape(curves.get(i), curves.get(i+1), i % 2 == 0 ? 0 : textureOffset));
        }
    }

    private PShape buildShape(List<PVec> topVertices, LinkedList<PVec> bottomVertices, float textureOffset) {
        PShape s = createShape();
        s.beginShape();
        s.texture(texture);
        topVertices.forEach(v -> s.vertex(v.x, v.y, v.x+textureOffset, v.y));
        ((Iterable<PVec>)bottomVertices::descendingIterator).forEach(v -> s.vertex(v.x, v.y, v.x+textureOffset, v.y));
        s.endShape(CLOSE);
        return s;
    }

    @Override
    public void draw() {
        clear();

        if (looping) {
            textureOffset = (frameCount/20.0f) % 2*stripeWidth;
        }

        buildShapes();

        shapes.forEach(this::shape);
    }

    @Override
    public void keyPressed() {
        switch (key) {
            case 'R':
                reset();
                redraw();
                break;
            default:
                super.keyPressed(); break;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == -1) {
            // ControlFrame signalling that it handled an event.
            textureOffset = stripeWidth;
            buildTexture();
            redraw();
        }
    }

    class ControlFrame extends PApplet {
        int w, h;
        PApplet parent;
        ControlP5 cp;

        ControlFrame(PApplet parent, int w, int h) {
            super();
            this.parent = parent;
            this.w = w;
            this.h = h;
            PApplet.runSketch(new String[]{this.getClass().getName()}, this);
        }

        public void settings() {
            size(w, h);
        }

        public void setup() {
            cp = new ControlP5(this);
            Group g1 = cp.addGroup("g1");
            cp.addSlider("stripeWidth")
                .setRange(1, 15)
                .setNumberOfTickMarks(15)
                .setSize(200, 30)
                .setPosition(0, 0)
                .setGroup(g1)
                .setValue(((OffsetStripes)parent).stripeWidth)
                .plugTo(parent, "stripeWidth")
            ;
                pr(((OffsetStripes)parent).stripeWidth);

            cp.addListener(e ->
                parent.postEvent(new MouseEvent(parent, 0, MouseEvent.CLICK, 0, -1, -1, -1, 1)));//, '!', Character.getNumericValue('!')));

            surface.setLocation(10, 10);
            surface.setSize(w, h);
            surface.setResizable(true);
        }

        public void draw() {
            background(0);
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

