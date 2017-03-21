package io.github.pflouret.sketchbook.bluenoise;

import io.github.pflouret.sketchbook.p5.PoissonDiskSampler;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PStyle;
import toxi.geom.Polygon2D;
import toxi.geom.PolygonClipper2D;
import toxi.geom.Rect;
import toxi.geom.Shape2D;
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
        noiseDetail(7, .4f);
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
        IntStream.range(0, randomInt(7, 30)).mapToObj(i -> randomPoint()).forEach(voronoi::addPoint);
        resetTiles();
    }

    private void resetTiles() {
        tiles.clear();
        tiles.addAll(voronoi.getRegions().stream()
            .map(poly -> prob(.05) ? poly.getBoundingCircle().toPolygon2D(30) : poly.getBounds().toPolygon2D())
            .map(poly -> areaClipper.clipPolygon(poly))
            .filter(poly -> !poly.vertices.isEmpty())
            .map(Tile::new)
            .peek(t -> {
                PStyle style = g.getStyle();
                float alpha = 255;//random(50, 100);
                style.strokeWeight = random(1, 3);
                style.strokeColor = color(random(255), random(255), random(255), alpha);
                style.strokeColor = color(0, random(155, 255));
                t.style = style;

                t.points.addAll(new PoissonDiskSampler(t, random(2, 5), getRandom()).sample(10000));
                //fill(c, alpha / 1.1f);
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
        /*
        List<Polygon2D> regions = voronoi.getRegions();
        for (int i = 0; i < regions.size();) {
            Polygon2D p = clipper.clipPolygon(regions.get(i));
            if (!p.vertices.isEmpty()) {
                tiles.get(i++).set(p.getBounds());
            }
        }
        */
    }

    private Vec2D randomPoint() {
        return new Vec2D(random(0, width), random(0, height));
    }

    private Consumer<Vec2D> getRandomDrawFun() {
        float p = random(1);

        if (p < .5) {
            return (v) -> drawWonkyEllipse(v, random(1, 4));
        } else if (p < .9) {
            return (v) -> drawStrips(v, 3);
        } else {
            return (v) -> drawSquares(v, 3);
        }
    }

    private void drawWonkyEllipse(Vec2D p, float offset) {
        ellipse(p.x, p.y, random(offset-1, offset), random(offset, offset+1));
        ellipse(p.x+random(-1, 1), p.y+random(-1, 1), random(offset, offset+3), random(offset+1, offset+3));
        ellipse(p.x+random(-1, 1), p.y+random(-1, 1), random(offset-1, offset+1), random(offset-4, offset));
    }

    private void drawStrips(Vec2D p, float curveLengthSpread) {
        beginShape();
        float b = random(curveLengthSpread-1, curveLengthSpread+1);
        float off = random(3, 20);
        //stroke(360*noise((frameCount+p.x)/100f, (frameCount+p.y)/100f), 100, 100);
        if (random(1) < .93f) {
            curveVertex(p.x+off, p.y+off);
            curveVertex(p.x, p.y);
            curveVertex(p.x+b, p.y-b);
            curveVertex(p.x+b+off, p.y+b+off/2f);
        } else {
            curveVertex(p.x-off, p.y-off);
            curveVertex(p.x, p.y);
            curveVertex(p.x-b, p.y+b);
            curveVertex(p.x-b-off, p.y-b-off/2f);
        }
        endShape();
    }

    private void drawSquares(Vec2D p, float size) {
        float off = 2;
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

