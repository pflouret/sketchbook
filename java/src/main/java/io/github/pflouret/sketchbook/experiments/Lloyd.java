package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import io.github.pflouret.sketchbook.p5.TVec;
import processing.core.PApplet;
import toxi.geom.*;
import toxi.geom.mesh2d.Voronoi;

import java.lang.invoke.MethodHandles;
import java.util.stream.IntStream;

public class Lloyd extends ProcessingApp {
    private Voronoi voronoi = new Voronoi();
    private PolygonClipper2D clipper;

    @Override
    public void setup() {
        super.setup();
        noLoop();

        clipper =  new SutherlandHodgemanClipper(new Rect(0, 0, width, height));
        //IntStream.range(0, 100).forEach(i -> voronoi.addPoint(tv(random(width), random(height))));
        voronoi.addPoints(new Circle(w2, h2, 100).toPolygon2D(100).vertices);
    }

    @Override
    public void draw() {
        clear();

        strokeWeight(2);
        stroke(255, 0, 0, 50);
        //voronoi.getTriangles().forEach(gfx::triangle);
        stroke(0, 30);
        voronoi.getRegions().forEach(gfx::polygon2D);
        stroke(0, 230);
        strokeWeight(5);
        voronoi.getSites().forEach(gfx::point);

        Voronoi v = new Voronoi();
        voronoi.getRegions().forEach(r -> {
            v.addPoint(clipper.clipPolygon(r).getCentroid());
        });
        //voronoi.getRegions().forEach(r -> v.addPoint(clipper.clipPolygon(r).getCentroid()));
        voronoi = v;
    }

    @Override
    public void keyPressed() {
        super.keyPressed();
        redraw();
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

