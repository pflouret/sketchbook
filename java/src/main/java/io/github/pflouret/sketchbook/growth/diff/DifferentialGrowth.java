package io.github.pflouret.sketchbook.growth.diff;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.ReadonlyVec2D;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Inconvergent-style differential growth.
 * http://inconvergent.net/shepherding-random-growth/
 */
public class DifferentialGrowth extends ProcessingApp {
    private Node first;

    private boolean drawPoints = false;
    private boolean drawShape = true;

    private int padding = 15;

    private float repulsionRadius = 25;
    private float nearDistance = 6;
    private float splitDistance = 7;
    private float splitRandomness = 0.7f;
    private float attractionForce = 0.005f;

    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();
        reset();
        clear();
        //noLoop();
        fill(0);
        //stroke(0, 1);
        strokeWeight(2f);
    }

    @Override
    public void reset() {
        initialCircle();
    }

    private void initialLine() {
        first = new Node(w2 - 130, h2 - 130);
        first.next = new Node(w2 + 130, h2 + 130);
    }

    private void initialTube() {
        first = new Node(w2 - 150, h2 - 130);
        first.next = new Node(w2 + 130, h2 + 130);
        first.next.next = new Node(w2 + 150, h2 + 130);
        first.next.next.next = new Node(w2 - 130, h2 - 130);
    }

    private void initialCircle() {
        int n = 10, r = 20;
        Node v = first = new Node(w2+r+random(-.5f, .5f), h2+random(-.5f, .5f));
        for (int i=1; i < n; i++) {
            float angle = i*2*PI/n;
            v.next = new Node(w2+r*cos(angle)+random(-.5f, .5f), h2+r*sin(angle)+random(-.5f, .5f));
            v.next.prev = v;
            v = v.next;
        }
        first.prev = v;
        v.next = first;
    }

    private void update() {
        reject();
        splitUniformly();
        attract();
    }

    private void reject() {
        forEachNode(a -> {
            forEachNode(b -> {
                float d2 = a.distanceToSquared(b);
                if (b != a &&
                    b != a.prev &&
                    b != a.next &&
                    a.x > padding && a.x < width-padding && a.y > padding && a.y < height-padding &&
                    d2 < repulsionRadius*repulsionRadius) {
                    a.set(lerp(a.x, b.x, -1/d2), lerp(a.y, b.y, -1/d2));
                }
            });
        });
    }

    private void attract() {
        forEachNode(a -> {
            if (a.x < padding || a.x > width-padding || a.y < padding || a.y > height-padding) {
                return;
            }
            if (a.distanceTo(a.prev) > nearDistance) {
                a.set(lerp(a.x, a.prev.x, attractionForce), lerp(a.y, a.prev.y, attractionForce));
            }
            if (a.distanceTo(a.next) > nearDistance) {
                a.set(lerp(a.x, a.next.x, attractionForce), lerp(a.y, a.next.y, attractionForce));
            }
        });
    }


    private void splitUniformly() {
        forEachNode(n -> {
            if (n.distanceTo(n.next) > splitDistance) {
                Node newNode = new Node(n.add(n.next).scaleSelf(0.5f).jitter(splitRandomness));
                n.setNext(newNode);
            }
        });
    }

    private void splitByCurvature() {
        for (int i=0; i < 100; i++) {
            // Split
            List<Node> nodes = new ArrayList<>();
            forEachNode(nodes::add);
            nodes.sort((v1, v2) -> -Float.compare(
                abs(v1.sub(v1.prev).angleBetween(v1.sub(v1.next))),
                abs(v2.sub(v2.prev).angleBetween(v2.sub(v2.next)))));
            List<Node> toSplit = nodes.subList(0, min(10, nodes.size()));//nodes.size()/100);
            forEachNode(a -> {
                if (a.x < padding || a.x > width-padding || a.y < padding || a.y > height-padding) {
                    return;
                }
                if (a.distanceTo(a.next) > splitDistance && (toSplit.contains(a) || random(1) < 0.00005f)) {
                    Node newNode = new Node(a.add(a.next).scaleSelf(0.5f).jitter(splitRandomness));
                    a.setNext(newNode);
                }
            });
        }

    }

    @Override
    public void draw() {
        clear();
        drawShape();
        update();
    }

    private void drawPoint(Vec2D v) {
        if (!drawPoints) {
            return;
        }
        pushStyle();
        strokeWeight(3);
        point(v.x, v.y);
        popStyle();
    }

    private void drawLine() {
        beginShape();
        forEachNode(n -> {
            drawPoint(n);
            vertex(n.x, n.y);
        });
        endShape();
    }

    private void drawShape() {
        if (drawShape) {
            beginShape();
            curveVertex(first.prev.x, first.prev.y);
        }

        forEachNode(n -> {
            drawPoint(n);
            if (drawShape) {
                curveVertex(n.x, n.y);
            }
        });

        if (drawShape) {
            curveVertex(first.x, first.y);
            curveVertex(first.next.x, first.next.y);
            endShape();
        }
    }

    private void forEachNode(Consumer<Node> consumer) {
        Node n = first;
        do {
            consumer.accept(n);
        } while (n.next != null && (n = n.next) != first);
    }

    class Node extends Vec2D {
        Node prev, next;
        int age = 0;

        Node(float x, float y) {
            super(x, y);
        }

        public Node(ReadonlyVec2D readonlyVec2D) {
            super(readonlyVec2D);
        }

        void setNext(Node n) {
            if (next != null) {
                next.prev = n;
            }
            n.next = next;
            n.prev = this;
            next = n;
        }
    }

    public static void main(String[] args) {
        PApplet.main(MethodHandles.lookup().lookupClass());
    }
}
