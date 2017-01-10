package io.github.pflouret.sketchbook.growth.laplacian;

import processing.core.PVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class Triangle {
    Node[] nodes = new Node[3];

    Triangle(Node n0, Node n1, Node n2) {
        setNodes(n0, n1, n2);
    }

    private void setNode(Node n, int i) {
        if (nodes[i] != null) {
            n.triangles.remove(this);
        }
        nodes[i] = n;
        if (!n.triangles.contains(this)) {
            n.triangles.add(this);
        }
    }

    private void setNodes(Node n0, Node n1, Node n2) {
        setNode(n0, 0);
        setNode(n1, 1);
        setNode(n2, 2);
    }

    List<PVector> vertices() {
        return Arrays.asList(nodes[0], nodes[1], nodes[2]);
    }

    private float orientation() {
        return cross(nodes[0], nodes[1], nodes[2]);
    }

    private static float cross(Node n0, Node n1, Node n2) {
        float x1 = n0.x-n1.x;
        float y1 = n0.y-n1.y;
        float x2 = n2.x-n1.x;
        float y2 = n2.y-n1.y;
        return y1*x2-x1*y2;
    }

    private boolean hasNode(Node node) {
        return Arrays.stream(nodes).anyMatch(n -> n == node);
    }

    static List<Triangle> splitEdge(Node n1, Node middle, Node n2) {
        if (n1 == n2) {
            return Collections.emptyList();
        }

        if (middle == null) {
            middle = new Node(0.5f*(n1.x+n2.x), 0.5f*(n1.y+n2.y));
        }

        List<Triangle> triangles = new ArrayList<>();
        for (Triangle t : findTriangles(n1, n2)) {
            Triangle newTr = new Triangle(n1, middle, n2);
            t.flipEdge(newTr);
            triangles.add(newTr);

        }
        return triangles;
    }

    private static List<Triangle> findTriangles(Node n1, Node n2) {
        return n1.triangles.stream().filter(t -> t.hasNode(n2)).collect(Collectors.toList());
    }

    void testTriangulation() {
        for (int i=0; i < nodes.length; i++) {
            List<Triangle> triangles = findTriangles(nodes[i], nodes[(i+1) % nodes.length]);
            if (triangles.size() == 2) {
                triangles.get(0).flipEdge(triangles.get(1));
            }
        }
    }

    private boolean flipEdge(Triangle tr2) {
        Node n0 = null, n1 = null, n2 = null, n3 = null;

        if (!tr2.hasNode(nodes[0])) {
            n0 = nodes[2];
            n1 = nodes[0];
            n2 = nodes[1];
        } else if (!tr2.hasNode(nodes[1])) {
            n0 = nodes[0];
            n1 = nodes[1];
            n2 = nodes[2];
        } else if (!tr2.hasNode(nodes[2])) {
            n0 = nodes[1];
            n1 = nodes[2];
            n2 = nodes[0];
        }

        if (!tr2.hasNode(n0) || !tr2.hasNode(n2)) {
            return false;
        }

        if (!this.hasNode(tr2.nodes[0])) {
            n3 = tr2.nodes[0];
        } else if (!this.hasNode(tr2.nodes[1])) {
            n3 = tr2.nodes[1];
        } else if (!this.hasNode(tr2.nodes[2])) {
            n3 = tr2.nodes[2];
        }

        if (n0 == null || n1 == null || n2 == null || n3 == null) {
            return false;
        }

        if (!(this.orientation() > -1 || tr2.orientation() > -1)) {
            if (cross(n0, n1, n3) > -1 || cross(n1, n2, n3) > -1) {
                return false;
            }
            float m1 = Math.max(circumscribedMeasure(n0, n1, n2), circumscribedMeasure(n2, n3, n0));
            float m2 = Math.max(circumscribedMeasure(n0, n1, n3), circumscribedMeasure(n1, n2, n3));
            if (m2 >= m1) {
                return false;
            }
        }

        detach();
        setNodes(n0, n1, n3);
        tr2.detach();
        tr2.setNodes(n1, n2, n3);

        return true;
    }

    private float sqrt(float n) {
        return (float)Math.sqrt(n);
    }

    private float circumscribedMeasure(Node vertex1, Node vertex2, Node vertex3) {
        float a = sqrt(vertex1.dist2(vertex2));
        float b = sqrt(vertex2.dist2(vertex3));
        float c = sqrt(vertex3.dist2(vertex1));
        float s = (a+b+c)/2;
        return a*b*c/sqrt(s*(s-a)*(s-b)*(s-c));
    }

    void detach() {
        Arrays.stream(nodes).forEach(n -> n.triangles.remove(this));
    }

    boolean isSkipped() {
        return Arrays.stream(nodes).allMatch(n -> n.skipCount > LaplacianGrowth.app.maxSkipCount);
    }
}



