package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Line2D;
import toxi.geom.Rect;
import toxi.geom.Shape2D;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.Optional;
import java.util.Vector;

public class Division extends ProcessingApp {
    private class State {
        Vector<Shape2D> shapes;
        Vec2D cursor;
        boolean cursorLocked = false;
        Vector<Vec2D> dividers;
        Shape2D shape;
        Line2D edge;

        State() {
            shapes = new Vector<>();
            dividers = new Vector<>();
            cursor = new Vec2D(w2, h2);
            cursorLocked = false;
        }
    }

    private State state;

    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();
        reset();
        noLoop();
    }

    @Override
    public void reset() {
        state = new State();
        state.shapes.add(new Rect(100, 100, width-200, height-200).toPolygon2D());
        update();
    }

    private void updateCursor() {
        Vec2D mouse = new Vec2D(mouseX, mouseY);
        float minD = Float.MAX_VALUE;

        Optional<Shape2D> sh = state.shapes.stream().filter(s -> s.containsPoint(mouse)).findFirst();
        if (sh.isPresent()) {
            for (Line2D edge : sh.get().getEdges()) {
                float d = edge.distanceToPointSquared(mouse);
                if (d < minD) {
                    minD = d;
                    state.cursor = edge.closestPointTo(mouse);
                    state.shape = sh.get();
                    state.edge = edge;
                }
            }
        }
    }

    private void update() {
        if (!state.cursorLocked) {
            updateCursor();
        }
        redraw();
    }

    @Override
    public void draw() {
        clear();

        state.shapes.forEach(s -> gfx.polygon2D(s.toPolygon2D()));

        if (state.cursor != null) {
            drawTarget(state.cursor);
        }

        Vec2D mouse = new Vec2D(mouseX, mouseY);
        if (state.cursorLocked) {
            Vec2D normal = state.edge.getNormal().normalize();
            Vec2D absNormal = normal.getAbs();
            Vec2D target = state.cursor.add(normal.scale(state.edge.distanceToPoint(mouse)))
                .scaleSelf(absNormal)
                .add(new Vec2D(absNormal.y, absNormal.x).scaleSelf(state.cursor));
            target = state.shape.containsPoint(target) ? target : target.constrain(state.shape.toPolygon2D());


            Vec2D normalTarget = state.shape.getEdges().stream()
                .filter(edge -> edge.intersectLine(state.edge).getType() == Line2D.LineIntersection.Type.INTERSECTING)
                .map(edge -> edge.closestPointTo(mouse))
                .min(Comparator.comparingDouble(p -> p.distanceToSquared(mouse)))
                .orElse(null);

            gfx.line(state.cursor, target);
            gfx.line(target, normalTarget);
        }



        /*
        if (!dividers.isEmpty()) {
            push();
            beginShape();
            vertex(cursor.x, cursor.y);
            dividers.forEach(p -> {
                ellipse(p.x, p.y, 3, 3);
                vertex(p.x, p.y);
            });
            endShape();
            pop();
        }
        */
    }

    private void drawTarget(Vec2D p) {
        push();
        fill(0);
        ellipse(p.x, p.y, 5, 5);
        noFill();
        ellipse(p.x, p.y, 12, 12);
        pop();
    }

    @Override
    public void mouseMoved() {
        update();
    }

    @Override
    public void mouseClicked() {
        if (state.cursorLocked) {
            //dividers.add(new Vec2D(mouseX, mouseY));
        } else {
            state.cursorLocked = true;
        }
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

