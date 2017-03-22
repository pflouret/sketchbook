package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.PoissonDiskSampler;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.Rect;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.Vector;

public class BlueNoiseForms extends ProcessingApp {
    private Vector<Vec2D> points;

    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();

        noiseDetail(7, .73f);
        reset();

        noLoop();
    }

    @Override
    public void reset() {
        clear();
        points = new Vector<>();
        addPoints();
    }


    private void addPoints() {
        Circle circle = new Circle(w2, h2, 350);
        Rect rect = new Rect(100, 100, width-200, height-200);
        points.addAll(new PoissonDiskSampler(circle, random(a+5, a+8), /*this::noise,*/ getRandom()).sample(8000));
    }

    private int a = 5;
    private void drawWonkyEllipse(Vec2D p) {
        ellipse(p.x, p.y, random(a-1,a), random(a,a+1));
        ellipse(p.x+random(-1, 1), p.y+random(-1, 1), random(a,a+3), random(a+1,a+3));
        ellipse(p.x+random(-1, 1), p.y+random(-1, 1), random(a-1,a+1), random(a-4,a));
    }

    private int bb = 3;
    private void drawStrips(Vec2D p) {
        beginShape();
        float b = random(bb-1, bb+1);
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

    private float cc = 5;
    private void drawSquares(Vec2D p) {
        float off = 2;
        rect(p.x, p.y, cc, cc);
        rect(p.x+random(-off, off), p.y+random(-off, off), cc+random(-2, -1), cc);
        rect(p.x+random(-off, off), p.y+random(-off, off), cc+random(-1.5f, 2), cc+random(-.5f, .5f));
    }

    @Override
    public void draw() {
        rectMode(CENTER);
        //colorMode(HSB, 360, 100, 100);
        clear();
        push();

        int c = color(random(255), random(255), random(255));
        float alpha = random(20, 40);
        stroke(c, alpha);
        //noStroke();
        fill(c, alpha / 1.1f);
        //points.forEach(this::drawWonkyEllipse);
        //points.forEach(this::drawStrips);
        points.forEach(this::drawSquares);

        pop();
    }

    @Override
    public void keyPressed() {
        switch(key) {
            case 'a':
                addPoints(); break;
            case '1':
            case '2':
                bb += (key == '1' ? -1 : 1); break;
            case '3':
            case '4':
                a += (key == '3' ? -1 : 1); reset(); break;
            case 'q':
            case 'w':
                cc += (key == 'q' ? -1 : 1); break;
            default:
                super.keyPressed();
        }
        redraw();
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

