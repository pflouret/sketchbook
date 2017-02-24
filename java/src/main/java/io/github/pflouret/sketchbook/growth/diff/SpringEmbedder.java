package io.github.pflouret.sketchbook.growth.diff;

import com.hamoid.VideoExport;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import io.github.pflouret.sketchbook.p5.TVertex;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * https://arxiv.org/abs/1201.3011
 */
public class SpringEmbedder extends ProcessingApp {
    private Vector<TVertex> vertices;

    private float splitDistance = 9;
    private float splitProbability = 0.208f;
    private float splitRandomness = 0.2f;

    private boolean drawPoints = false;
    private boolean fillShape = true;

    private float padding = 15;
    private float temperature;

    private VideoExport video;
    private boolean saveVideo = false;

    @Override
    public void settings() {
        size(400, 400);
    }

    @Override
    public void setup() {
        super.setup();

        if (video == null && saveVideo) {
            video = getVideoExporter();
            video.startMovie();
        }

        noiseDetail(6, 0.45f);

        vertices = new Vector<>(5000);
        temperature = 1;

        int r = 10, n = 4;
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

        strokeWeight(2);
        //noLoop();
    }

    private void fruchtermanReingold() {
        float area = width*height;
        float repulsionRadius = 18;

        splitDistance = 7;
        splitProbability = 0.003f;
        splitRandomness = 0.3f;

        if (temperature <= 0) {
            return;
        }

        Map<TVertex, Vec2D> attractionDeltas = new HashMap<>(vertices.size());
        for (TVertex a : vertices) {
            Vec2D v = a.sub(a.next);
            v.normalizeTo(v.magSquared()/sqrt(area/vertices.size()));
            attractionDeltas.put(a, attractionDeltas.computeIfAbsent(a, key -> new Vec2D()).add(v.scale(-1)));
            attractionDeltas.put(a.next, attractionDeltas.computeIfAbsent(a, key -> new Vec2D()).add(v));
        }

        ConcurrentMap<TVertex, Vec2D> repulsionDeltas = vertices.parallelStream().collect(Collectors.toConcurrentMap(
            Function.identity(),
            a -> vertices.parallelStream()
                .filter(b -> b != a && b != a.prev && b != a.next && a.distanceTo(b) < repulsionRadius)
                .map(b -> a.sub(b).normalizeTo(area/vertices.size()/a.distanceTo(b)))
                .reduce(Vec2D::add)
                .orElse(new Vec2D())));


        vertices.parallelStream().forEach(a -> {
            Vec2D da = attractionDeltas.get(a).add(repulsionDeltas.get(a));
            Vec2D delta = da.normalizeTo(.5f*min(da.magnitude(), temperature));
            a.addSelf(delta);
            a.set(min(w2-padding, max(-w2+padding, a.x)), min(h2-padding, max(-h2+padding, a.y)));
        });

        noiseResample(false);
        temperature -= .0001f;
    }

    private void eames() {
        float attractionFactor = 0.15f;
        float repulsionStrength = 0.7f;
        float repulsionRadius = 27;
        splitDistance = 6.5f;
        splitRandomness = 1.0f;

        ConcurrentMap<TVertex, Vec2D> attractionDeltas = vertices.parallelStream()
            .collect(Collectors.toConcurrentMap(
                Function.identity(),
                a -> a.prev.sub(a).normalizeTo(log(a.distanceTo(a.prev)/15)).addSelf(
                    a.next.sub(a).normalizeTo(log(a.distanceTo(a.next)/15)))));

        ConcurrentMap<TVertex, Vec2D> repulsionDeltas = vertices.parallelStream().collect(Collectors.toConcurrentMap(
            Function.identity(),
            a -> vertices.parallelStream()
                .filter(b -> b != a && b != a.prev && b != a.next && a.distanceTo(b) < repulsionRadius)
                .map(b -> a.sub(b).normalizeTo(1/sqrt(a.distanceTo(b))))
                .reduce(Vec2D::add)
                .orElse(new Vec2D())));


        vertices.parallelStream().forEach(a -> {
            Vec2D delta = attractionDeltas.get(a).scale(attractionFactor)
                .addSelf(repulsionDeltas.get(a).scale(repulsionStrength));
            Vec2D b = a.add(delta);
            if (b.x > -w2+padding && b.x < w2-padding && b.y > -h2+padding && b.y < h2-padding) {
                a.set(b);
            }
        });

        noiseResample(false);
    }

    private void resample() {
        resample(splitProbability);
    }

    private void resample(float p) {
        Vector<TVertex> newVertices = new Vector<>(1000);
        //vertices.sort((o1, o2) -> -Float.compare(o1.magSquared(), o2.magSquared()));
        for (TVertex v : vertices) {
            if (random(1) < p && v.distanceTo(v.next) > splitDistance) {
                TVertex newVertex = new TVertex(v.add(v.next).scaleSelf(0.5f).jitter(splitRandomness));
                v.setNext(newVertex);
                newVertices.add(newVertex);
            }
        }
        vertices.addAll(newVertices);
    }

    private void noiseResample(boolean force) {
        float noiseScale = 1/100.0f;
        Vector<TVertex> newVertices = new Vector<>(1000);
        for (TVertex v : vertices) {
            if ((force || random(1) < .17f*noise(v.x*noiseScale)) && v.distanceTo(v.next) > splitDistance) {
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
        eames();
        //fruchtermanReingold();
        pop();

        if (video != null) {
            video.saveFrame();
        }
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
        switch (key) {
            case 'r':
                noiseResample(true); break;
            case 'd':
                drawPoints = !drawPoints; break;
            case 'f':
                fillShape = !fillShape; break;
            case 'v':
                if (video != null) {
                    video.endMovie();
                    video = null;
                }
                break;
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

