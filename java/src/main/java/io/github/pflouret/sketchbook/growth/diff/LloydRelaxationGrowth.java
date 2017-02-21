package io.github.pflouret.sketchbook.growth.diff;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import io.github.pflouret.sketchbook.p5.TVec;
import io.github.pflouret.sketchbook.p5.TVertex;
import processing.core.PApplet;
import toxi.geom.*;
import toxi.geom.mesh2d.DelaunayTriangle;
import toxi.geom.mesh2d.DelaunayTriangulation;
import toxi.geom.mesh2d.DelaunayVertex;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class LloydRelaxationGrowth extends ProcessingApp {
    private Map<TVertex, TVertex> vertices;
    private PolygonClipper2D clipper;

    private float splitDistance = 2;
    private float splitRandomness = 1;
    private boolean drawVoronoi = true;

    private DelaunayTriangle initialTriangle = new DelaunayTriangle(
        new DelaunayVertex(-50000, -50000), new DelaunayVertex(50000, -50000), new DelaunayVertex(0, 50000));
    private DelaunayTriangulation delaunay;

    private void rebuildTriangulation() {
        try {
            delaunay = new DelaunayTriangulation(initialTriangle);
            vertices.keySet().stream().map(v -> new DelaunayVertex(v.x, v.y)).forEach(delaunay::delaunayPlace);
        } catch (Exception e) {
        }
    }

    private void relaxVertices() {
        HashSet<DelaunayVertex> done = new HashSet<>(initialTriangle);
        for (DelaunayTriangle triangle : delaunay) {
            for (DelaunayVertex site : triangle) {
                if (!done.contains(site)) {
                    done.add(site);
                    Polygon2D region = new Polygon2D();
                    for (DelaunayTriangle tri : delaunay.surroundingTriangles(site, triangle)) {
                        DelaunayVertex circumcenter = tri.getCircumcenter();
                        region.add(new Vec2D((float) circumcenter.coord(0), (float) circumcenter.coord(1)));
                    }
                    TVec v = tv((float) site.coord(0), (float) site.coord(1));
                    Vec2D centroid = clipper.clipPolygon(region).getCentroid();
                    TVertex vertex = vertices.remove(v);
                    vertex.interpolateToSelf(centroid, 0.01f);
                    //vertex.set(centroid);
                    if (drawVoronoi) {
                        push();
                        strokeWeight(5);
                        stroke(255, 0, 0);
                        point(centroid);
                        strokeWeight(1);
                        stroke(255, 0, 0, 100);
                        gfx.polygon2D(region);
                        pop();
                    }
                    vertices.put(vertex, vertex);
                }
            }
        }
    }

    @Override
    public void setup() {
        super.setup();
        //fill(0);

        vertices = new LinkedHashMap<>(5000);
        clipper = new SutherlandHodgemanClipper(new Rect(0, 0, width, height));

        int r = 10, n = 5;
        TVertex prev = null, first = null;
        for (Vec2D v : new Circle(w2, h2, r).toPolygon2D(n).vertices) {
            TVertex vx = new TVertex(v);
            vertices.put(vx, vx);
            if (prev == null) {
                first = vx;
            } else {
                prev.setNext(vx);
            }
            prev = vx;
        }
        first.prev = prev;
        prev.next = first;

        rebuildTriangulation();
        noLoop();
    }

    private void update() {
        relaxVertices();
        rebuildTriangulation();
        //resample();
    }

    private void resample() {
        ArrayList<TVertex> newVertices = new ArrayList<>(1000);
        vertices.keySet().forEach(v -> {
            if (v.distanceTo(v.next) > splitDistance) {
                TVertex newVertex = new TVertex(v.add(v.next).scaleSelf(0.5f).jitter(splitRandomness));
                v.setNext(newVertex);
                newVertices.add(newVertex);
            }
        });
        newVertices.forEach(v -> vertices.put(v, v));
    }

    @Override
    public void draw() {
        clear();
        drawShape();
        update();
    }

    private void drawShape() {
        beginShape();
        TVertex first = vertices.keySet().iterator().next();
        curveVertex(first.prev.x, first.prev.y);
        TVertex v = first;
        do {
            pushStyle();
            strokeWeight(3);
            point(v.x, v.y);
            popStyle();
            curveVertex(v.x, v.y);
        } while ((v = v.next) != first);

        curveVertex(first.x, first.y);
        curveVertex(first.next.x, first.next.y);
        endShape();
    }

    @Override
    public void keyPressed() {
        super.keyPressed();
        if (key == 'r') {
            resample();
        } else if (key == 'v') {
            drawVoronoi = !drawVoronoi;
        }
        redraw();
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

