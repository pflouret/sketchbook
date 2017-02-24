package io.github.pflouret.sketchbook.growth.diff;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import io.github.pflouret.sketchbook.p5.TVertex;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * http://www.andylomas.com/extra/andylomas_paper_cellular_forms_aisb50.pdf
 */
public class CellularForms extends ProcessingApp {
    private Vector<TVertex> vertices;

    private float splitDistance = 5;
    private float splitProbability = 0.008f;
    private float splitRandomness = 0.3f;

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

        int r = 10, n = 10;
        TVertex prev = null, first = null;
        for (Vec2D v : new Circle(0, 0, r).toPolygon2D(n).vertices) {
            TVertex vx = new TVertex(v);
            vertices.add(vx);
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
        float linkRestLength = 20;
        float springFactor = 0.001f;
        float planarFactor = 0.0015f;
        float bulgeFactor = 0.0005f;
        float roi = 18, roi2 = roi*roi;
        float repulsionStrength = 0.01f;

        ConcurrentMap<TVertex, Vec2D> springDeltas = vertices.parallelStream()
            .collect(Collectors.toConcurrentMap(
                Function.identity(),
                a -> a.prev.sub(a).normalize().addSelf(a.next.sub(a).normalize()).scaleSelf(linkRestLength/2)));

        ConcurrentMap<TVertex, Vec2D> planarDeltas = vertices.parallelStream()
            .collect(Collectors.toConcurrentMap(
                Function.identity(),
                a -> a.next.add(a.prev).scaleSelf(0.5f)));

        ConcurrentMap<TVertex, Vec2D> bulgeDeltas = vertices.parallelStream()
            .collect(Collectors.toConcurrentMap(
                Function.identity(),
                a -> {
                    Vec2D normal = a.prev.add(a.next).scaleSelf(0.5f).normalize();
                    float dotNPrev = a.prev.sub(a).dot(normal);
                    float dotNNext = a.next.sub(a).dot(normal);
                    float l2 = linkRestLength*linkRestLength;
                    float bulgeDist =
                        (sqrt(l2-a.prev.magSquared()+dotNPrev*dotNPrev)+dotNPrev+
                            sqrt(l2-a.next.magSquared()+dotNNext*dotNNext)+dotNNext)/2;
                    return !Float.isNaN(bulgeDist) ? normal.scaleSelf(bulgeDist) : new Vec2D();
                }));

        ConcurrentMap<TVertex, Vec2D> repulsionDeltas = vertices.parallelStream().collect(Collectors.toConcurrentMap(
            Function.identity(),
            a -> vertices.parallelStream()
                .filter(b -> b != a && b != a.prev && b != a.next && b.distanceToSquared(a) < roi2)
                .map(b -> a.sub(b).scaleSelf((roi2-a.sub(b).magSquared())/roi2))
                .reduce(Vec2D::add)
                .orElse(new Vec2D())));


        vertices.parallelStream().forEach(a -> {
            Vec2D delta = springDeltas.get(a).scale(springFactor)
                .addSelf(planarDeltas.get(a).scale(planarFactor))
                .addSelf(bulgeDeltas.get(a).scale(bulgeFactor))
                .addSelf(repulsionDeltas.get(a).scale(repulsionStrength));
            a.addSelf(delta);
        });

        resample();
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
        strokeWeight(6);
        vertices.forEach(v -> {
            point(v.x, v.y);
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

