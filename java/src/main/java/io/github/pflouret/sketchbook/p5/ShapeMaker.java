package io.github.pflouret.sketchbook.p5;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.event.KeyEvent;
import toxi.geom.Polygon2D;

import java.lang.invoke.MethodHandles;
import java.util.Vector;

public abstract class ShapeMaker extends ProcessingApp {
    Vector<Polygon2D> polys;
    Polygon2D poly;

    @Override
    public void settings() {
        super.settings();
        //size(800, 800);
        //size(800, 800, P3D);
    }

    @Override
    public void setup() {
        super.setup();
        reset();
    }

    @Override
    public void reset() {
        polys = new Vector<>();
        poly = new Polygon2D();
    }

    void drawCurrentPoly() {
        push();

        fill(0, 80);
        gfx.polygon2D(poly);

        fill(0, 100);
        synchronized(poly) {
            poly.vertices.forEach(v -> ellipse(v.x, v.y, 8, 8));
        }

        pop();
    }

    @Override
    protected void drawInternal() {
        /*
        clear();
        push();
        stroke(0, 20);
        fill(0, 50);
        polys.forEach(gfx::polygon2D);
        pop();
        */

        drawCurrentPoly();
    }

    protected abstract void onNewPoly(Polygon2D p);


    @Override
    public void mouseClicked() {
        synchronized(poly) {
            poly.add(mouseX, mouseY);
            redraw();
        }
    }

    @Override
    public void mouseDragged() {
        synchronized(poly) {
            poly.add(mouseX, mouseY);
            redraw();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch(key) {
            case ' ':
                onNewPoly(poly);
                poly = new Polygon2D();
                break;
            case 'z':
                if (e.isMetaDown()) {
                    poly.vertices.remove(poly.vertices.size()-1);
                }
            default:
                super.keyPressed();
        }
        redraw();
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

