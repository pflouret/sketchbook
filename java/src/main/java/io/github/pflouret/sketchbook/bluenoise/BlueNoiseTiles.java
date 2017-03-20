package io.github.pflouret.sketchbook.bluenoise;

import io.github.pflouret.sketchbook.p5.PoissonDiskSampler;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PStyle;
import toxi.geom.ReadonlyVec2D;
import toxi.geom.Rect;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlueNoiseTiles extends ProcessingApp {
    private Vector<Tile> tiles;

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
        reset();
        noLoop();
    }

    @Override
    public void reset() {
        tiles = new Vector<>();
        tiles.add(new Tile(100, 100, width-200, height-200));
        addPoints();

        /*
        p = new Vector<>();
        p.add(new PoissonDiskSampler(new Circle(w2, h2, 200), random(4.5f, 5), getRandom()).sample(5000));
        p.add(new PoissonDiskSampler(new Circle(w2+70, h2+70, 200), random(3.9f, 4.4f), getRandom()).sample(5000));
        p.add(new PoissonDiskSampler(new Rect(0, 0, width, height), 10, getRandom()).sample(5000));
        p.add(new PoissonDiskSampler(100, 100, width-100, height-100, 8, getRandom()).sample(5000));
        p.add(new PoissonDiskSampler(150, 150, width-150, height-150, 4, getRandom()).sample(5000));
        p.add(new PoissonDiskSampler(250, 250, width-250, height-250, 5, getRandom()).sample(5000));
        */
    }

    private void subdivide1() {
        // weird.

        Vector<Tile> newTiles = new Vector<>();

        tiles.forEach(rect -> {
            boolean vertical = random(1) < .5f;
            Vec2D topLeft = rect.getTopLeft();
            newTiles.addAll(IntStream.range(0, randomInt(1,5))
                .mapToObj(i -> vertical ? random(topLeft.x, rect.width) : random(topLeft.y, rect.height))
                .sorted()
                .map(d -> new Vec2D(vertical ? d : rect.width, vertical ? rect.height: d))
                .map(widthHeight -> new Tile(topLeft, widthHeight))
                .peek(r -> topLeft.addSelf(vertical ? r.width : 0, vertical ? 0 : r.height))
                .collect(Collectors.toList()));

            Rect last = newTiles.lastElement();
            newTiles.add(new Tile(last.x, last.y,
                vertical ? rect.width - last.width : rect.width, vertical ? rect.height : rect.height - last.height));
        });
        tiles = newTiles;
    }

    private void subdivide2() {
        // weird.

        Vector<Tile> newTiles = new Vector<>();

        tiles.forEach(rect -> {
            boolean vertical = random(1) < .5f;
            Vec2D topLeft = rect.getTopLeft();
            int n = randomInt(1,5);
            while (n-- > 0) {
                float d = vertical ? random(topLeft.x, rect.width) : random(topLeft.y, rect.height);
                Vec2D widthHeight = new Vec2D(vertical ? d : rect.width, vertical ? rect.height: d);
                Tile r = new Tile(topLeft, widthHeight);
                //topLeft.addSelf(vertical ? r.width : 0, vertical ? 0 : r.height);
                topLeft = vertical ? r.getTopRight() : r.getBottomLeft();
                newTiles.add(r);
            }

            topLeft = vertical ? newTiles.lastElement().getTopRight() : newTiles.lastElement().getBottomLeft();
            newTiles.add(new Tile(topLeft, rect.getBottomRight()));
        });
        tiles = newTiles;
    }

    private PStyle getRandomStyle() {
        float alpha = random(20, 40);

        PStyle style = g.getStyle();
        style.strokeWeight = random(1, 3);
        style.strokeColor = color(random(255), random(255), random(255), alpha);
        //fill(c, alpha / 1.1f);
        return style;
    }

    private Consumer<Vec2D> getRandomDrawFun() {
        float p = random(1);

        if (p < .33333f) {
            return (v) -> drawWonkyEllipse(v, 5);
        } else if (p < .66666f) {
            return (v) -> drawStrips(v, 3);
        } else {
            return (v) -> drawSquares(v, 3);
        }
    }

    private void subdivide() {
        Vector<Tile> newTiles = new Vector<>();

        tiles.forEach(rect -> {
            boolean vertical = random(1) < .5f;
            Vec2D topLeft = rect.getTopLeft();
            int n = randomInt(1,5);
            while (n-- > 0) {
                Tile r;
                if (vertical) {
                    float x = random(topLeft.x, rect.getBottomRight().x);
                    if (x-topLeft.x < 10) {
                        continue;
                    }
                    r = new Tile(topLeft.x, topLeft.y, x-topLeft.x, rect.height, getRandomStyle(), getRandomDrawFun());
                    topLeft = r.getTopRight();
                } else {
                    float y = random(topLeft.y, rect.getBottomRight().y);
                    if (y-topLeft.y < 10) {
                        continue;
                    }
                    r = new Tile(topLeft.x, topLeft.y, rect.width, y-topLeft.y, getRandomStyle(), getRandomDrawFun());
                    topLeft = r.getBottomLeft();
                }
                newTiles.add(r);
            }

            if (newTiles.isEmpty()) {
                newTiles.add(rect);
            } else {
                topLeft = vertical ? newTiles.lastElement().getTopRight() : newTiles.lastElement().getBottomLeft();
                newTiles.add(new Tile(topLeft, rect.getBottomRight(), getRandomStyle(), getRandomDrawFun()));
            }
        });
        tiles = newTiles;
    }
    private void addPoints() {
        tiles.forEach((t) -> {
            t.points.addAll(new PoissonDiskSampler(t, random(10, 17), getRandom()).sample(15000));
        });
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
            case 's':
                subdivide();
                // Fall through.
            case 'a':
                addPoints(); break;
            default:
                super.keyPressed();
        }
        redraw();
    }

    class Tile extends Rect {
        Vector<Vec2D> points = new Vector<>();
        Consumer<Vec2D> drawPoint;
        PStyle style;

        Tile(float x, float y, float width, float height, PStyle style, Consumer<Vec2D> drawPoint) {
            super(x, y, width, height);
            this.style = style;
            this.drawPoint = drawPoint;
        }

        Tile(float x, float y, float width, float height) {
            this(x, y, width, height, null, BlueNoiseTiles.this::point);
        }

        Tile(ReadonlyVec2D p1, ReadonlyVec2D p2, PStyle style, Consumer<Vec2D> drawPoint) {
            super(p1, p2);
            this.style = style;
            this.drawPoint = drawPoint;
        }

        Tile(ReadonlyVec2D p1, ReadonlyVec2D p2) {
            this(p1, p2, null, BlueNoiseTiles.this::point);
        }

        void randomizeStyles() {


        }


        void draw() {
            push();
            if (style != null) {
                style(style);
            }
            points.forEach(drawPoint);
            pop();
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

