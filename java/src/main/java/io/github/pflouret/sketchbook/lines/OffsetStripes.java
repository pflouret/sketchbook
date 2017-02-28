package io.github.pflouret.sketchbook.lines;

import com.hamoid.VideoExport;
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

    private VideoExport video;

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

        curves.add(Stream.of(new PVec((float) 0, (float) 0), new PVec((float) width, (float) 0)).collect(Collectors.toCollection(LinkedList::new)));

        while (offset < height - 30) {
            curves.add(IntStream.rangeClosed(0, width)
                .mapToObj(x -> new PVec((float) x, wave.update()))
                .collect(Collectors.toCollection(LinkedList::new)));
            offset += random(60, 100);
            wave.offset = offset;
        }

        curves.add(Stream.of(new PVec((float) 0, (float) height), new PVec((float) width, (float) height)).collect(Collectors.toCollection(LinkedList::new)));
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
            textureOffset = frameCount/2.0f;
        }

        buildShapes();

        shapes.forEach(this::shape);

        if (video != null) {
            video.saveFrame();
        }
    }

    @Override
    public void keyPressed() {
        switch (key) {
            case 'R':
                reset();
                redraw();
                break;
            case 'v':
                if (video == null) {
                    video = getVideoExporter();
                    video.startMovie();
                    pr("movie started");
                } else {
                    video.endMovie();
                    video = null;
                    pr("movie ended");
                }
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

            int stripeWidth = ((OffsetStripes) parent).stripeWidth;
            cp.addSlider("stripe width")
                .setRange(1, 15)
                .setNumberOfTickMarks(15)
                .setSize(200, 30)
                .setPosition(5, 5)
                .setGroup(g1)
                .setValue(stripeWidth)
                .plugTo(parent, "stripeWidth")
            ;

            cp.addSlider("texture offset")
                .setRange(1, 30)
                .setSize(200, 30)
                .setPosition(5, 46)
                .setGroup(g1)
                .setValue(((OffsetStripes)parent).textureOffset)
                .plugTo(parent, "textureOffset")
            ;

            cp.addBang("reset")
                .setPosition(5, 81)
                .setSize(20, 20)
                .setGroup(g1)
                .plugTo(parent, "reset")
            ;

            cp.addBang("reset offset")
                .setPosition(35, 81)
                .setSize(20, 20)
                .setGroup(g1)
                .onClick(e -> ((OffsetStripes)parent).textureOffset = stripeWidth)
            ;

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

