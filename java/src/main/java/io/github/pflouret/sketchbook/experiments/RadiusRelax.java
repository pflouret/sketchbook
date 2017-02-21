package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import io.github.pflouret.sketchbook.p5.TVec;
import io.github.pflouret.sketchbook.p5.TVertex;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.PointQuadtree;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.Vector;

public class RadiusRelax extends ProcessingApp {
    private Vector<TVertex> vertices;
    private PointQuadtree quadtree;

    private float splitDistance = 6;
    private float splitProbability = 0.004f;
    private float splitRandomness = 0.5f;
    private float minGap = 40;
    private int relaxationIterations = 30;
    private float attractionForce = 0.005f;

    private boolean drawPoints = false;
    private boolean fillShape = false;

    @Override
    public void settings() {
        size(1000, 1000);
    }

    @Override
    public void setup() {
        super.setup();

        vertices = new Vector<>(5000);
        quadtree = new PointQuadtree(-w2, -h2, w2, h2);

        int r = 10, n = 4;
        TVertex prev = null, first = null;
        for (Vec2D v : new Circle(0, 0, r).toPolygon2D(n).vertices) {
            TVertex vx = new TVertex(v);
            vertices.add(vx);
            quadtree.index(vx);
            if (prev == null) {
                first = vx;
            } else {
                prev.setNext(vx);
            }
            prev = vx;
        }
        first.prev = prev;
        prev.next = first;

        strokeWeight(3);
        //noLoop();
    }

    private void update() {
        relaxVertices(relaxationIterations);
        resample();
    }

    private void relaxVertices(int maxIterations) {
        boolean globalOverlap = true;
        vertices.sort((v1, v2) -> -Float.compare(v1.magSquared(), v2.magSquared()));
        for (int i=0; globalOverlap && i < maxIterations; i++) {
            for (TVertex a : vertices) {
                for (TVertex b : vertices) {
                    float d = a.distanceTo(b);
                    if (a != b && d < minGap) {
                        globalOverlap = true;
                        //a.addSelf(a.next.sub(a).addSelf(a.prev.sub(a)).getNormalizedTo(0.0003f));
                        a.addSelf(a.getNormalizedTo(0.0009f));
                        a.addSelf(a.sub(b).getNormalizedTo(1/d/d));
                        // TODO: normals based on curvature
                    }
                }
            }
            for (TVertex a : vertices) {
                float d0 = a.distanceTo(new TVec());
                if (d0 > 50) {
                    a.addSelf(a.getNormalizedTo(-10/(d0*d0)));
                }
                if (a.distanceTo(a.next) > 2*splitDistance) {
                    a.addSelf(a.next.sub(a).getNormalizedTo(attractionForce));
                }
                if (a.distanceTo(a.prev) > 2*splitDistance) {
                    a.addSelf(a.prev.sub(a).getNormalizedTo(attractionForce));
                }
            }
        }
    }

    private void resample() {
        resample(splitProbability);
    }

    private void resample(float p) {
        Vector<TVertex> newVertices = new Vector<>(1000);
        for (TVertex v : vertices) {
            if (random(1) < p && v.distanceTo(v.next) > splitDistance) {
                TVertex newVertex = new TVertex(v.add(v.next).scaleSelf(0.5f).jitter(splitRandomness));
                v.setNext(newVertex);
                newVertices.add(newVertex);
            }
        }
        vertices.addAll(newVertices);
    }


    @Override
    public void draw() {
        clear();
        if (fillShape) {
            fill(0, 230);
        } else {
            noFill();
        }

        push();
        translate(w2, h2);
        drawShape();
        if (drawPoints) {
            drawPoints();
        }
        update();
        pop();
    }

    private void drawShape() {
        beginShape();
        TVertex v;
        TVertex first = v = vertices.firstElement();
        curveVertex(first.prev.x, first.prev.y);
        do {
            curveVertex(v.x, v.y);
        } while ((v = v.next) != first);
        curveVertex(first.x, first.y);
        curveVertex(first.next.x, first.next.y);
        endShape();
    }

    private void drawPoints() {
        push();
        strokeWeight(3);
        vertices.forEach(v -> {
            point(v.x, v.y);
            push();
            strokeWeight(1);
            stroke(0, 30);
            ellipse(v.x, v.y, minGap, minGap);
            pop();
        });
        pop();
    }

    @Override
    public void keyPressed() {
        super.keyPressed();
        if (key == 'r') {
            resample(0.2f);
        } else if (key == 'd') {
            drawPoints = !drawPoints;
        } else if (key == 'f') {
            fillShape = !fillShape;
        }
        redraw();
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

