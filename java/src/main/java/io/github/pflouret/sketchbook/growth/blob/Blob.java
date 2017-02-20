package io.github.pflouret.sketchbook.growth.blob;

import processing.core.PGraphics;
import toxi.geom.Circle;
import toxi.geom.ReadonlyVec2D;
import toxi.geom.Vec2D;

import java.util.List;
import java.util.Vector;

public class Blob {
    static Blobs app = Blobs.app;

    private Vector<Vertex> vertices = new Vector<>();
    private Vec2D origin;
    private float minDistance = 10;

    private float splitDistance = 10;
    private float splitRandomness = 0;
    private boolean drawPoints = false;

    private float maxRadius = 40;

    boolean isStatic = false;

    Blob(Vec2D origin, int n, float r) {
        this.origin = origin;
        Vertex prev = null;
        List<Vec2D> c = new Circle(origin, r).toPolygon2D(n).vertices;
        for (Vec2D v : c) {
            Vertex vx = new Vertex(v);
            vertices.add(vx);
            if (prev != null) {
                prev.setNext(vx);
            }
            prev = vx;
        }
        vertices.firstElement().prev = prev;
        vertices.lastElement().next = vertices.firstElement();

        maxRadius += app.random(-8, 8);
    }

    void update(Vector<Blob> blobs) {
        resample();

        boolean[] updated = new boolean[] { false };
        vertices.stream()
            .filter(v ->
                v.isWithinBounds() && v.distanceTo(origin) < maxRadius &&
                blobs.stream()
                    .filter(b -> b != this)
                    .noneMatch(b -> b.vertices.stream()
                        .anyMatch(vv -> v.distanceTo(vv) < minDistance)))
            .peek(v -> updated[0] = true)
            .forEach(v -> v.addSelf(v.sub(origin).normalizeTo(0.5f)));
        isStatic = !updated[0];
    }

    private void resample() {
        Vector<Vertex> newVertices = new Vector<>();
        vertices.forEach(v -> {
            if (v.distanceTo(v.next) > splitDistance) {
                Vertex newVertex = new Vertex(v.add(v.next).scaleSelf(0.5f).jitter(splitRandomness));
                v.setNext(newVertex);
                newVertices.add(newVertex);
            }
        });
        vertices.addAll(newVertices);
    }

    void draw(PGraphics pg) {
        Vertex first = vertices.firstElement();
        Vertex v = first;

        pg.beginDraw();
        pg.beginShape();
        pg.curveVertex(first.prev.x, first.prev.y);
        do {
            if (drawPoints) {
                drawPoint(v, pg);
            }
            pg.curveVertex(v.x, v.y);
        } while ((v = v.next) != first);

        pg.curveVertex(first.x, first.y);
        pg.curveVertex(first.next.x, first.next.y);
        pg.endShape();
        pg.endDraw();
    }

    private void drawPoint(Vec2D v, PGraphics pg) {
        pg.pushStyle();
        pg.strokeWeight(3);
        pg.point(v.x, v.y);
        pg.popStyle();
    }

    class Vertex extends Vec2D {
        Vertex prev, next;

        Vertex(ReadonlyVec2D readonlyVec2D) {
            super(readonlyVec2D);
        }

        void setNext(Vertex v) {
            if (next != null) {
                next.prev = v;
            }
            v.next = next;
            v.prev = this;
            next = v;
        }

        boolean isWithinBounds() {
            return x > minDistance && x < app.width-minDistance && y > minDistance && y < app.height-minDistance;
        }
    }


}


