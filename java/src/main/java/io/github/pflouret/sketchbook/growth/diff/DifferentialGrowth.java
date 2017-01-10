package io.github.pflouret.sketchbook.growth.diff;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.PointQuadtree;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.function.Consumer;

public class DifferentialGrowth extends ProcessingApp {
    private Node first;
    private PointQuadtree quadtree;
    private int nodeCount = 0;

    private boolean drawPoints = false;
    private boolean drawShape = true;

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
        strokeWeight(1.5f);
    }

    @Override
    public void reset() {
        int a = 10;
        first = new Node(w2+a, h2-a);
        first.setNext(new Node(w2-a, h2-a));
        first.next.setNext(new Node(w2-a, h2+a));
        first.next.next.setNext(new Node(w2+a, h2+a));
        first.next.next.next.next = first;
        first.prev = first.next.next.next;

        quadtree = new PointQuadtree(0, 0, width, height);
        forEachNode(quadtree::index);
        nodeCount = 4;
    }

    private void update() {
        float repulsionRadius = 50;
        float attractionRadius = 10;
        float splitDistance = 8;
        float attractionForce = 0.05f;

        forEachNode(a -> {
            if (false) {
                forEachNode(b -> {
                    if (b != a && b != a.prev && b != a.next && a.distanceTo(b) < repulsionRadius) {
                        float f = -1/a.distanceToSquared(b);
                        a.set(lerp(a.x, b.x, f), lerp(a.y, b.y, f));
                    }
                });
            } else {
                Vec2D v = new Vec2D();
                quadtree.itemsWithinRadius(a, repulsionRadius, new ArrayList<>()).stream()
                    .filter(b -> b != a && b != a.prev && b != a.next)
                    .forEach(b -> {
                        float f = -1/a.distanceToSquared(b);
                        quadtree.reindex(a.copy(), a.set(lerp(a.x, b.x, f), lerp(a.y, b.y, f)));
                    });
            }
        });

        /*
        forEachNode(a -> {
            forEachNode(b -> {
                if (b != a && b != a.prev && b != a.next && a.distanceTo(b) < attractionRadius) {
                    float f = 0.005f;
                    a.set(lerp(a.x, b.x, f), lerp(a.y, b.y, f));
                }
            });
        });
        */

        forEachNode(a -> {
            if (a.distanceTo(a.prev) > attractionRadius) {
                a.set(lerp(a.x, a.prev.x, attractionForce), lerp(a.y, a.prev.y, attractionForce));
            }
            if (a.distanceTo(a.next) > attractionRadius) {
                a.set(lerp(a.x, a.next.x, attractionForce), lerp(a.y, a.next.y, attractionForce));
            }
        });

        forEachNode(n -> {
            if (n.distanceTo(n.next) > splitDistance) {
                Node newNode = new Node((n.x+n.next.x)/2+random(-.5f, .5f), (n.y+n.next.y)/2+random(-.5f, .5f));
                n.setNext(newNode);
                quadtree.index(newNode);
                nodeCount++;
            }
        });
        /*
        */
    }

    private void update1() {
        float repulsionRadius = 20;
        float splitDistance = 5;
        float speed = 0.01f;

        forEachNode(a -> {
            Vec2D f = new Vec2D();
            float d = (float)quadtree.itemsWithinRadius(a, repulsionRadius, new ArrayList<>()).stream()
                .filter(b -> b != a && b != a.prev && b != a.next)
                .peek(b -> {
                    f.addSelf(a.sub(b).normalizeTo(1f/a.distanceTo(b)));
                })
                .mapToDouble(a::distanceTo)
                .min()
                .orElse(repulsionRadius);
            Vec2D delta = f.normalizeTo(d*speed);
            /*
            if (delta.magnitude() < 0.05) {
                return;
            }
            */
            quadtree.reindex(a.copy(), a.addSelf(delta));
        });

        forEachNode(n -> {
            if (n.distanceTo(n.next) > splitDistance) {
                Node newNode = new Node((n.x+n.next.x)/2+random(-1, 1), (n.y+n.next.y)/2+random(-1, 1));
                n.setNext(newNode);
                quadtree.index(newNode);
            }
        });
    }

    @Override
    public void draw() {
        clear();

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

    private void forEachNode(Consumer<Node> consumer) {
        Node n = first;
        do {
            consumer.accept(n);
        } while ((n = n.next) != first);
    }

    class Node extends Vec2D {
        Node prev, next;

        Node(float x, float y) {
            super(x, y);
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
