package io.github.pflouret.sketchbook.lines;

import hype.H;
import hype.HDrawablePool;
import hype.HShape;
import hype.extended.layout.HGridLayout;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PStyle;
import toxi.geom.Polygon2D;
import toxi.geom.Rect;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PolyStripes extends ProcessingApp {
    private List<PShape> shapes = new ArrayList<>();

    @Override
    public void settings() {
        size(1000, 1000, P3D);
        smooth(8);
    }

    private PShape randomShape() {
        Polygon2D p = new Polygon2D(IntStream.range(0, round(random(3, 6)))
            .mapToObj(j -> new Vec2D(random(w2), random(h2)))
            .collect(Collectors.toList()));

        PStyle style = g.getStyle();
        style.strokeColor = color(random(360), random(100), random(100), 200);
        style.strokeWeight = random(.9f, 1.5f);

        Rect bounds = p.getBounds();
        PImage texture = stripedTexture(round(bounds.width), round(bounds.height), random(0, PI/2), random(3, 10), style);

        return buildShape(p, texture);
    }

    private PShape buildShape(Polygon2D p, PImage texture) {
        PShape shape = createShape();
        shape.setTexture(texture);
        shape.beginShape();
        shape.noStroke();
        shape.fill(0, 0, 0);
        p.vertices.forEach(v -> shape.vertex(v.x, v.y, v.x, v.y));
        shape.endShape(CLOSE);
        shape.width = shape.getWidth();
        shape.height = shape.getHeight();
        return shape;
    }

    @Override
    public void setup() {
        super.setup();

        H.init(this);

        colorMode(HSB, 360, 100, 100);
        stroke(0, 220);

        reset();
    }

    @Override
    public void reset() {
        H.stage().forEach(d -> H.stage().remove(d));
        setupGrid();
        //Triangle2D s = Triangle2D.createEquilateralFrom(new Vec2D(0, 0), new Vec2D(200, 0));
    }

    private void setupGrid() {
        int n = 3, m = 3;
        float spacing = 90, textureSpacing = 7.5f;
        float w = (float)width/n-spacing-spacing/n;
        float h = (float)height/m-spacing-spacing/m;
        float size = w <= h ? w : h;

        strokeWeight(1);
        Polygon2D p = new Rect(0, 0, size, size).toPolygon2D();

        H.background(color(0, 0, 100));
        HDrawablePool pool = new HDrawablePool(n*m);
        pool.add(new HShape())
            .autoAddToStage()
            .layout(new HGridLayout(n, m).spacing(size+spacing, size+spacing).startLoc(spacing, spacing))
            .onCreate(obj -> {
                HShape s = (HShape) obj;
                s.shape(buildShape(p, stripedTexture(round(size), round(size), random(360), textureSpacing)));
                s.add(
                    new HShape(buildShape(p, stripedTexture(round(size), round(size), random(360), textureSpacing)))
                    .loc(-30, 30));
            })
            .requestAll();
    }

    @Override
    public void draw() {
        H.drawStage();
    }

    @Override
    public void keyPressed() {
        switch (key) {
            default:
                super.keyPressed();
        }
    }

    class ShapeWrapper {
        private Polygon2D poly;
        private PImage texture;

        PShape shape;

        ShapeWrapper(Polygon2D poly, PImage texture) {
            super();
            this.poly = poly;
            this.texture = texture;
            update();
        }

        private void update() {
            shape = createShape();
            shape.setTexture(texture);
            shape.beginShape();
            shape.noStroke();
            poly.vertices.forEach(v -> shape.vertex(v.x, v.y, v.x, v.y));
            shape.endShape(CLOSE);
        }

        void draw(float x, float y) {
            Vec2D c = poly.getCentroid();
            shape(shape, x-c.x, y-c.y);
        }


        public void setPoly(Polygon2D poly) {
            this.poly = poly;
            update();
        }

        public void setTexture(PImage texture) {
            this.texture = texture;
            update();
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

