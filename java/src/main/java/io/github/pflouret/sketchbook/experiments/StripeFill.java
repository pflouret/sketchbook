package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.PVec;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PVector;

import java.lang.invoke.MethodHandles;

public class StripeFill extends ProcessingApp {
    @Override
    public void settings() {
        size(600, 600, FX2D);
        smooth(16);
    }

    @Override
    public void setup() {
        super.setup();
        //noLoop();
        stroke(0, 220);
    }

    @Override
    public void draw() {
        clear();

        float a = map(mouseX, 30, width-30, 0, PI/2);
        float spacing = map(mouseY, 0, height, 1, 20);

        PVector o = new PVec(200, 200);
        int w = 200, h = 200;

        // Rotate square top vertices around its center.
        PVector p1 = new PVector(-w/2f, -h/2f).rotate(a).add(o).add(w/2, h/2);
        //p1 = o.copy().rotate(a);//.add(o).add(w/2, h/2);
        PVector p2 = new PVector(w/2f, -h/2).rotate(a).add(o).add(w/2, h/2);
        PVector p0 = o.copy().add(w, 0);
        PVector pdiff = p2.copy().sub(p1);

        float d = abs(pdiff.y*p0.x - pdiff.x*p0.y + p2.x*p1.y - p2.y*p1.x) / pdiff.mag();

        push();
        strokeWeight(4);
        rect(o.x, o.y, w, h);
        pop();

        push();
        translate(o.x+w/2, o.y+h/2);
        rotate(a);
        for (float y=-h/2f-d; y <= h/2f+d; y+=spacing) {
            line(-w/2-d, y, w/2+d, y);
        }
        pop();
    }

    public void draw1() {
        clear();
        int w = 200, h = 200;
        float eps = .0001f;
        float x0 = 200, y0 = 200, xf = x0+w, yf = y0+h;

        float a = map(mouseX, 30, width-30, 0, PI/2);
        float spacing = 10;

        float xx0 = (-w/2*cos(a))-(-h/2*sin(a)) + w/2+x0, yy0 = (-w/2*sin(a))+(-h/2*cos(a)) + h/2+y0;
        float xxf = w/2*cos(a)-(-h/2*sin(a)) + w/2+x0, yyf = w/2*sin(a)+(-h/2*cos(a)) + h/2+y0;

        float d = abs((yyf-yy0)*xf - (xxf-xx0)*y0 + xxf*yy0 - yyf*xx0) / sqrt(pow(yyf-yy0, 2) + pow(xxf-xx0, 2));
        pr(d);

        push();
        strokeWeight(4);
        point(xx0, yy0);
        point(xxf, yyf);
        rect(x0, y0, w, h);
        pop();

        push();
        translate(x0+w/2, y0+h/2);
        rotate(a);
        for (float y=-h/2f-d; y <= h/2f+d; y+=spacing) {
            line(-w/2-d, y, w/2+d, y);
        }
        pop();
        /*
        */

        /*

        float step = spacing / tan(angle);
        if (Float.isNaN(step)) {
            step = 0;
        }

        // PI/4 < a < PI/2
        for (float y=y0, x=x0; y < yf && x < xf; y+=spacing, x+=step) {
            line(x0, y, x, y0);
        }
        // PI/4 < a < PI/2
        for (float x=x0, y=y0; x < x0+w; x+=spacing, y+=step) {
            line(x, y0+h, x0+w, y);
        }
        */
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

