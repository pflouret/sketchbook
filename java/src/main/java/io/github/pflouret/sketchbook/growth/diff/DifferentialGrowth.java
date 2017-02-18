package io.github.pflouret.sketchbook.growth.diff;

import io.github.pflouret.sketchbook.p5.Pair;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.PointQuadtree;
import toxi.geom.ReadonlyVec2D;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.Vector;

public class DifferentialGrowth extends ProcessingApp {
    private Vector<Node> vertices;
    private PointQuadtree quadtree;

    private static boolean drawPoints = false;
    private static boolean drawShape = true;
    private static boolean fillShape = true;

    class Node extends Vec2D {
        Node prev, next;

        Node(float v, float v1) {
            super(v, v1);
        }

        Node(ReadonlyVec2D readonlyVec2D) {
            super(readonlyVec2D);
        }
    }

    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();
        reset();
        clear();
        strokeWeight(1.5f);
        noLoop();
    }

    @Override
    public void reset() {
        vertices = new Vector<>(10000);
        quadtree = new PointQuadtree(0, 0, width, height);
        int n = 20, r = 15;
        for (int i = 0; i < n; i++) {
            float angle = i*2*PI/n;
            Node v = new Node(w2+r*cos(angle)+random(-.5f, .5f), h2+r*sin(angle)+random(-.5f, .5f));
            vertices.add(v);
            quadtree.index(v);
            if (i > 0) {
                v.prev = vertices.get(i-1);
                v.prev.next = v;
            }
        }
        vertices.firstElement().prev = vertices.lastElement();
        vertices.lastElement().next = vertices.firstElement();
    }

    private void update() {
        float repulsionRadius = 25;
        float nearDistance = 12;
        float splitDistance = 8;
        float splitRandomness = 0.2f;
        float attractionForce = 0.05f;

        vertices.forEach(a -> {
            vertices.stream()
                .filter(b -> b != a && b != a.prev && b != a.next && a.distanceTo(b) < repulsionRadius)
                .forEach(b -> {
                    float d2 = a.distanceToSquared(b);
                    a.set(lerp(a.x, b.x, -1/d2), lerp(a.y, b.y, -1/d2));
                });
            Node v = vertices.firstElement();
            do {
                if (a.distanceTo(a.prev) < nearDistance) {
                    a.set(lerp(a.x, a.prev.x, attractionForce), lerp(a.y, a.prev.y, attractionForce));
                }
                if (a.distanceTo(a.next) < nearDistance) {
                    a.set(lerp(a.x, a.next.x, attractionForce), lerp(a.y, a.next.y, attractionForce));
                }
            } while ((v = v.next) != vertices.firstElement());
        });

        for (int i=0; i < vertices.size(); i++) {
            Node a = vertices.get(i);
            if (a.distanceTo(a.next) > splitDistance) {
                Node newVertex = new Node(a.add(a.next).scaleSelf(0.5f).jitter(splitRandomness));
                vertices.add(newVertex);
                newVertex.prev = a;
                newVertex.next = a.next;
                a.next = newVertex;
                quadtree.index(newVertex);
            }
        }
    }

    @Override
    public void clear() {
        super.clear();
        if (fillShape) {
            fill(0, 250);
        } else {
            noFill();
        }
    }

    @Override
    public void draw() {
        clear();
        drawClosedShape();
        update();
    }

    private void drawClosedShape() {

        if (drawShape) {
            beginShape();
            Vec2D last = vertices.firstElement().prev;
            curveVertex(last.x, last.y);
        }

        Node v = vertices.firstElement();
        do {
            drawPoint(v);
            if (drawShape) {
                curveVertex(v.x, v.y);
            }
        } while ((v = v.next) != vertices.firstElement());

        if (drawShape) {
            curveVertex(vertices.get(0).x, vertices.get(0).y);
            curveVertex(vertices.get(1).x, vertices.get(1).y);
            endShape();
        }
    }

    private void drawPoint(Vec2D v) {
        if (!drawPoints) {
            return;
        }
        pushStyle();
        strokeWeight(3);
        point(v.x, v.y);
        popStyle();
    }

    public static void main(String[] args) {
        PApplet.main(MethodHandles.lookup().lookupClass());
    }
}
