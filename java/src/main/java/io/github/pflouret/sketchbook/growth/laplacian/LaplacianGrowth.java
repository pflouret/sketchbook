package io.github.pflouret.sketchbook.growth.laplacian;

import com.hamoid.VideoExport;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PVector;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A processing port of the algorithm from
 * https://pixelero.wordpress.com/2011/11/18/laplacian-growth-vector-graphics/
 */
public class LaplacianGrowth extends ProcessingApp {
    static LaplacianGrowth app;
    private Node first, last;
    private List<Triangle> triangles;
    private List<Node> activeNodes;

    private boolean showPoints = false;
    private boolean showTriangulation = false;

    private int direction = 0;
    private float splitDistance = 4;
    private float minDistance = 3.5f;
    private float randomness = 0.92f;
    private float maxStepDistance = 1f;
    private float stepScale = 0.4f;
    int maxSkipCount = 4;

    private boolean saveVideo = false;
    private VideoExport video;

    public LaplacianGrowth() {
        if (app == null) {
            app = this;
        }
    }

    @Override
    public void settings() {
        size(1000, 1000);
    }

    @Override
    public void reset() {
        /*
         * 	1      0 boundary 0,1,2,3
         *    5	 4	 initial path 4,5,6,7
         *    6  7
         * 	2      3
         */

        int cx = w2, cy = h2;
        int sx = 5, sy = 5;

        switch (direction) {
            case 1:    //	horizontal line
                sx = 2;
                sy = height/4;
                break;
            case 2:    //	vertical growth
                cy = height-20;
                sx = width/6;
                sy = 2;
                break;
        }

        Node[] borders = new Node[]{
            new Node(width, 0),      // #0
            new Node(0, 0),          // #1
            new Node(0, height),     // #2
            new Node(width, height), // #3
        };
        activeNodes = Stream.of(
            //	initial path, 4 vertices inside area
            new Node(cx+sx, cy-sy),  // #4
            new Node(cx-sx, cy-sy),  // #5
            new Node(cx-sx, cy+sy),  // #6
            new Node(cx+sx, cy+sy)   // #7
        ).collect(Collectors.toList());

        first = activeNodes.get(0);
        first.setNext(activeNodes.get(1));
        activeNodes.get(1).setNext(activeNodes.get(2));
        activeNodes.get(2).setNext(activeNodes.get(3));
        activeNodes.get(3).setNext(first);
        last = activeNodes.get(3);
        first.setPrev(last);

        triangles = Stream.of(
            new Triangle(borders[0], borders[1], activeNodes.get(1)),
            new Triangle(borders[0], activeNodes.get(1), activeNodes.get(0)),
            new Triangle(borders[1], borders[2], activeNodes.get(1)),
            new Triangle(borders[2], activeNodes.get(2), activeNodes.get(1)),
            new Triangle(borders[2], borders[3], activeNodes.get(2)),
            new Triangle(borders[3], activeNodes.get(3), activeNodes.get(2)),
            new Triangle(borders[3], borders[0], activeNodes.get(3)),
            new Triangle(borders[0], activeNodes.get(0), activeNodes.get(3))
        ).collect(Collectors.toList());
    }

    private void addNode(Node n, Node newNode) {
        Node oldNext = n.next;
        n.setNext(newNode);
        newNode.setNext(oldNext);
        if (n == last) {
            last = newNode;
        }
        activeNodes.add(newNode);
    }

    private void update() {
        Iterator<Node> it = activeNodes.iterator();
        while (it.hasNext()) {
            Node node = it.next();
            float d0 = node.prev.frontDistance();
            float d1 = node.frontDistance();
            float d2 = node.next.frontDistance();

            if (++node.skipCount > maxSkipCount) {
                it.remove();
            }

            PVector n = node.normal();
            float d = (d0+d1+d2)/3;

            if (d > minDistance) {
                if (d > maxStepDistance) {
                    d = maxStepDistance;
                }
                d *= stepScale;

                node.add(n.copy().mult(d));
                node.skipCount = 0;
            }
        }

        Node node = first;
        do {
            if (!node.triangles.isEmpty()) {
                Node newNode = node.splitNode(splitDistance, randomness);
                if (newNode != null) {
                    addNode(node, newNode);
                    triangles.addAll(Triangle.splitEdge(node.next.next, node.next, node));
                    node = node.next;
                }
            }
        } while ((node = node.next) != first);

        for (Iterator<Triangle> i = triangles.iterator(); i.hasNext(); ) {
            Triangle t = i.next();
            if (t.isSkipped()) {
                i.remove();
                t.detach();
            } else {
                t.testTriangulation();
            }
        }
    }

    public void setup() {
        super.setup();
        reset();
        clear();

        //stroke(0, 1);
        //noLoop();
        fill(0);
        //stroke(255);
        //strokeWeight(1.5f);

        if (saveVideo) {
            video = getVideoExporter();
            video.startMovie();
        }
    }

    @Override
    public void draw() {
        clear();

        beginShape();
        curveVertex(last.x, last.y);

        Node n = first;
        do {
            if (showPoints) {
                strokeWeight(3);
                point(n.x, n.y);
                strokeWeight(2);
            }

            curveVertex(n.x, n.y);
        } while ((n = n.next) != first);

        curveVertex(first.x, first.y);
        curveVertex(first.next.x, first.next.y);
        endShape();

        if (showTriangulation) {
            pushStyle();
            stroke(0, 10);
            strokeWeight(1);
            triangles.forEach(t -> {
                line(t.nodes[0].x, t.nodes[0].y, t.nodes[1].x, t.nodes[1].y);
                line(t.nodes[1].x, t.nodes[1].y, t.nodes[2].x, t.nodes[2].y);
                line(t.nodes[2].x, t.nodes[2].y, t.nodes[0].x, t.nodes[0].y);
            });
            popStyle();
        }

        update();

        if (video != null) {
            video.saveFrame();
        }
    }

    @Override
    public void keyPressed() {
        super.keyPressed();
        if (key == ' ') {
            redraw();
        }
    }

    public static void main(String[] args) {
        PApplet.main(MethodHandles.lookup().lookupClass());
    }
}

