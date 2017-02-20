package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import org.jbox2d.collision.shapes.ChainShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import processing.core.PApplet;
import toxi.geom.Circle;

import java.lang.invoke.MethodHandles;
import java.util.Vector;
import java.util.stream.Collectors;

public class B2d extends ProcessingApp {
    private World w;
    private Vector<Body> bodies = new Vector<>();

    @Override
    public void setup() {
        super.setup();

        w = new World(new Vec2());
        BodyDef bd = new BodyDef();
        bd.fixedRotation = true;

        Vec2[] vertices = new Circle(0, 0, 50).toPolygon2D(10).vertices.stream()
            .map(v -> new Vec2(v.x, v.y))
            .collect(Collectors.toList())
            .toArray(new Vec2[]{});
        Body body = w.createBody(bd);
        ChainShape shape = new ChainShape();
        shape.createLoop(vertices, 10);
        body.createFixture(shape, 0);
        bodies.add(body);

        vertices = new Circle(0, 0, 50).toPolygon2D(10).vertices.stream()
            .map(v -> new Vec2(v.x, v.y))
            .collect(Collectors.toList())
            .toArray(new Vec2[]{});
        body = w.createBody(bd);
        shape = new ChainShape();
        shape.createLoop(vertices, 10);
        body.createFixture(shape, 0);
        bodies.add(body);
    }

    @Override
    public void draw() {
        clear();
        pushMatrix();
        translate(w2, h2);
        bodies.forEach(b -> {
            Vec2[] vertices = ((ChainShape) b.getFixtureList().getShape()).m_vertices;
            beginShape();
            for (int i=0; i < vertices.length; i++) {
                vertex(vertices[i].x, vertices[i].y);
            }
            endShape(CLOSE);
        });
        popMatrix();
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

