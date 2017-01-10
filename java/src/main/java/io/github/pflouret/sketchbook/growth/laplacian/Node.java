package io.github.pflouret.sketchbook.growth.laplacian;

import processing.core.PVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Node extends PVector {
    private static final LaplacianGrowth app = LaplacianGrowth.app;

    List<Triangle> triangles = new ArrayList<>();
    Node prev, next;
    int skipCount = 0;

    Node(float x, float y) {
        super(x, y);
    }

    void setNext(Node n) {
        next = n;
        if (n != null) {
            n.prev = this;
        }
    }

    void setPrev(Node n) {
        prev = n;
        if (n != null) {
            n.next = this;
        }
    }

    float dist2(Node node) {
        float dx = x-node.x;
        float dy = y-node.y;
        return dx*dx+dy*dy;
    }

    Node splitNode(float d, float randomness) {
        if (triangles.isEmpty() || next.triangles.isEmpty() || dist(next) < d) {
            return null;
        }

        return new Node((x+next.x)/2 + randomness*app.random(-1, 1), (y+next.y)/2 + randomness*app.random(-1, 1));
    }

    float frontDistance() {
        PVector dv = triangles.stream()
            .flatMap(t -> Arrays.stream(t.nodes).map(n -> PVector.sub(this, n)))
            .reduce(new PVector(), (v1, v2) -> v1.add(v2));
        float d = triangles.isEmpty() ? 0 : dv.mag() / (2*triangles.size());
        d = Math.min(Math.min(x, d), app.width-x);
        return Math.min(Math.min(y, d), app.height-y);
    }


    PVector normal() {
        PVector d1 = prev.copy().sub(this).normalize();
        PVector d2 = next.copy().sub(this).normalize();
        float c = (float) Math.pow(d1.dot(d2), 4);
        return new PVector(d1.y-d2.y, d2.x-d1.x).mult(c);
    }

}


