package io.github.pflouret.sketchbook.bluenoise;

import io.github.pflouret.sketchbook.p5.PoissonDiskSampler;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PStyle;
import toxi.geom.Polygon2D;
import toxi.geom.PolygonClipper2D;
import toxi.geom.Rect;
import toxi.geom.SutherlandHodgemanClipper;
import toxi.geom.Vec2D;
import toxi.geom.mesh2d.Voronoi;

import java.lang.invoke.MethodHandles;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tiles extends ProcessingApp {
    private Vector<Tile> tiles = new Vector<>();
    private Voronoi voronoi;
    private PolygonClipper2D windowClipper, areaClipper;

    @Override
    public void settings() {
        size(1000, 1000);
    }

    @Override
    public void setup() {
        //bgColor = 0;
        super.setup();

        strokeWeight(1);
        noiseDetail(7, .95f);
        stroke(0, 100);
        //noStroke();
        noLoop();

        windowClipper = new SutherlandHodgemanClipper(new Rect(0, 0, width, height));
        areaClipper =  new SutherlandHodgemanClipper(new Rect(100, 100, width-200, height-200));

        reset();
    }

    @Override
    public void reset() {
        voronoi = new Voronoi();
        IntStream.range(0, randomInt(5, 12)).mapToObj(i -> randomPoint()).forEach(voronoi::addPoint);
        resetTiles();
    }

    private void resetTiles() {
        tiles.clear();
        tiles.addAll(voronoi.getRegions().stream()
            .map(poly -> areaClipper.clipPolygon(poly))
            .filter(poly -> !poly.vertices.isEmpty())
            .map(poly -> prob(.05) ? poly.getBoundingCircle().toPolygon2D(30) : poly.getBounds().toPolygon2D())
            .map(poly -> areaClipper.clipPolygon(poly))
            .map(Tile::new)
            .peek(t -> {
                PStyle style = g.getStyle();
                float alpha, minDist = random(7, 10);
                Integer c = null;
                if (prob(.7)) {
                    alpha = random(90, 130);
                    c = color(0, 0, random(255), alpha);
                    style.stroke = false;
                    style.fill = true;
                    t.drawPoint = (v) -> wonkyEllipse(v, random(1, 3));
                    minDist = random(4, 6);
                } else if (prob(.4)) {
                    alpha = random(200, 255);
                    c = color(0, 0, random(255), alpha);
                    style.strokeWeight = random(1, 2.4f);
                    minDist = random(style.strokeWeight+.5f, style.strokeWeight+2.5f);
                } else if (prob(.8)) {
                    alpha = random(100, 140);
                    style.fill = true;
                    t.drawPoint = (v) -> wonkyEllipse(v, random(2, 4));
                    minDist = 6;
                } else if (prob(.8)) {
                    alpha = random(210, 255);
                    style.strokeWeight = random(.9f, 1.3f);
                    t.drawPoint = (v) -> curvyStrip(v, random(3, 5));
                } else {
                    alpha = random(40, 70);
                    style.fill = true;
                    t.drawPoint = (v) -> drawSquares(v, random(3, 6));
                }

                c = (c == null) ? color(random(255), random(255), random(255), alpha) : c;
                style.strokeColor = c;
                style.fillColor = color(red(c), green(c), blue(c), alpha / 1.1f);
                t.style = style;

                t.points.addAll(new PoissonDiskSampler(t, minDist, /*this::noise,*/ getRandom()).sample(30000));
            })
            .collect(Collectors.toList()));
    }

    private void relax() {
        Voronoi v = new Voronoi();
        voronoi.getRegions().stream()
            .map(r -> windowClipper.clipPolygon(r).getCentroid())
            .forEachOrdered(v::addPoint);
        voronoi = v;

        resetTiles();
    }

    private void drawSquares(Vec2D p, float size) {
        float off = 1;
        rect(p.x, p.y, size, size);
        rect(p.x+random(-off, off), p.y+random(-off, off), size+random(-2, -1), size);
        rect(p.x+random(-off, off), p.y+random(-off, off), size+random(-1.5f, 2), size+random(-.5f, .5f));
    }

    @Override
    public void draw() {
        clear();
        tiles.forEach(Tile::draw);
    }

    @Override
    public void keyPressed() {
        switch(key) {
            case 'x':
                relax();
                // Fall through.
            default:
                super.keyPressed();
        }
        redraw();
    }

    class Tile extends Polygon2D {
        Vector<Vec2D> points = new Vector<>();
        Consumer<Vec2D> drawPoint = Tiles.this::point;
        PStyle style;

        Tile(Polygon2D poly) {
            super(poly.vertices);
        }

        void draw() {
            pushStyle();
            fill(bgColor);
            noStroke();
            gfx.polygon2D(this);
            popStyle();

            pushStyle();
            if (style != null) {
                style(style);
            }
            points.forEach(drawPoint);
            popStyle();
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

