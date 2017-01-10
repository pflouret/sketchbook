package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AttractionRepulsion extends ProcessingApp {

    private List<Vec2D> vv;

    @Override
    public void setup() {
        super.setup();

        stroke(0, 230);
        strokeWeight(5);

        reset();
        noLoop();
    }

    @Override
    public void reset() {
        /*
		vv = Arrays.asList(new Vec2D(-80, 0), new Vec2D(80, 0), new Vec2D(0, 80));
		*/
        vv = Stream.generate(() -> Vec2D.randomVector().scaleSelf(random(-width / 2, width / 2)))
            .limit(10)
            .collect(Collectors.toList());
        //vv = new Circle(150).toPolygon2D(9).vertices;//.stream().map(v -> v.jitter(1)).collect(Collectors.toList());
    }

    @Override
    public void draw() {
        clear();

        pushMatrix();
        translate(width / 2, height / 2);
        //line(-15, -30, 15, -30);
        gfx.points2D(vv);
        drawEdges();
        popMatrix();

        for (int i = 0; i < 1; i++) {
            update();
        }
    }

    private void drawEdges() {
        pushStyle();
        stroke(0, 20);
        strokeWeight(2);

        beginShape();
        //curveVertex(vv.get(0).x, vv.get(0).y);
        curveVertex(vv.get(vv.size() - 1).x, vv.get(vv.size() - 1).y);
        vv.forEach(v -> curveVertex(v.x, v.y));
        curveVertex(vv.get(0).x, vv.get(0).y);
        curveVertex(vv.get(1).x, vv.get(1).y);
        endShape();
        popStyle();
    }

    private void update() {
        Map<Vec2D, List<Vec2D>> forces = vv.stream()
            .collect(Collectors.toMap(Function.identity(), this::getForceVectors));

        vv.forEach(a -> a.addSelf(forces.get(a).stream().reduce(new Vec2D(), Vec2D::add)));
    }

    private List<Vec2D> getForceVectors(Vec2D a) {
        int i = vv.indexOf(a);
        if (i < 0) {
            return Collections.emptyList();
        }

        int n = vv.size();
        Vec2D u = vv.get(Math.floorMod(i - 1, n));
        Vec2D v = vv.get((i + 1) % n);

        List<Vec2D> f = new ArrayList<>(vv.size() + 1);

        f.add(getAttractiveForce(a, u));
        f.add(getAttractiveForce(a, v));

        vv.stream().filter(b -> a != b).forEach(b -> {
            f.add(getRepulsiveForce(a, b));
        });

        return f;
    }

    private Vec2D getAttractiveForce(Vec2D a, Vec2D b) {
        float f = -0.015f * a.distanceTo(b);
        Vec2D v = a.sub(b).normalizeTo(f);
        //println(v);
        //rintln(a.distanceTo(b));
        return v;
    }

    private Vec2D getRepulsiveForce(Vec2D a, Vec2D b) {
        float r = a.distanceTo(b);
        float f = 0.3f / r * r;
        Vec2D v = a.sub(b).normalizeTo(f);
        println(v);
        return v;
    }

    private Vec2D getAttractiveForce2(Vec2D a, Vec2D b) {
        // lennard-jones potential
        float s = 30, r = a.distanceTo(b);
        float ep = r > 1.5 * s ? (r > 2 * s ? 120 : 10) : 0.8f;
        float V = constrain(4 * ep * ((pow(s / r, 12)) - pow(s / r, 6)), -s / 2, s / 2);
        return a.sub(b).normalizeTo(V);
    }

    private Vec2D getRepulsiveForce2(Vec2D a, Vec2D b) {
        // lennard-jones potential
        float s = 30, r = a.distanceTo(b);
        float ep = r > 1.5 * s ? (r > 2 * s ? 120 : 10) : 0.8f;
        float V = constrain(4 * ep * ((pow(s / r, 12)) - pow(s / r, 6)), -s / 2, s / 2);
        return a.sub(b).normalizeTo(V);
    }

    @Override
    public void keyPressed() {
        super.keyPressed();

    }

    public static void main(String[] args) {
        PApplet.main(MethodHandles.lookup().lookupClass());
    }
}
