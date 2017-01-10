package io.github.pflouret.sketchbook.growth.eden;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Vec2D;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Eden extends ProcessingApp {

    private int maxNeighbors = 8;
    private int r = 15;
    private int iterations = 20;

    private Particle[][] lattice;
    private Vector<Particle> living;

    public static void main(String[] args) {
        PApplet.main(MethodHandles.lookup().lookupClass());
    }

    class Particle {
        int x, y;
        int paint = -1;
        boolean alive = true;
        Vector<Particle> neighbors = new Vector<>();

        Particle(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private boolean withinBounds() {
            return x > 0 && x < width && y > 0 && y < height;
        }

        Particle getNeighbor() {
            if (neighbors.size() > maxNeighbors) {
                return null;
            }

            Particle p;

            int i = 0;
            do {
                int xx, yy;
                if (random(1) < 0.5f) {
                    xx = random(1) < 0.5f ? x-r : x+r;
                    yy = floor(random(y-r, y+r + 0.1f));
                } else {
                    xx = floor(random(x-r, x+r + 0.1f));
                    yy = random(1) < 0.5f ? y-r : y+r;
                }

                p = new Particle(xx, yy);
            } while(!p.withinBounds() || isOccupied(p) || i++ >= 30);

            return p;
        }

        public String toString() {
            return String.format("(%d,%d)", x, y);
        }
    }

    private boolean isOccupied(Particle p) {
        return lattice[p.x][p.y] != null;
    }

    private Particle occupy(Particle p) {
        assert !isOccupied(p);

        lattice[p.x][p.y] = p;
        int xmin = constrain(p.x - r, 0, width-1), xmax = constrain(p.x + r, 0, width-1);
        int ymin = constrain(p.y - r, 0, height-1), ymax = constrain(p.y + r, 0, height-1);

        for (int y = ymin; y <= ymax; y++) {
            for (int x = xmin; x <= xmax; x++) {
                Particle q = lattice[x][y];
                if (q != null && q != p) {
                    p.neighbors.add(q);
                    q.neighbors.add(p);
                }
            }
        }
        return p;
    }


    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();

        clear();
        stroke(0, 100);
        strokeWeight(4);

        reset();
    }

    @Override
    public void reset() {
        lattice = new Particle[width][height];
        living = new Vector<>();

        Particle p = new Particle(w2, h2);
        occupy(p);
        living.add(p);
    }

    private void update() {
        if (living.isEmpty()) {
            println("no more living cells");
            return;
        }

        Particle p;
        p = randomChoice(living);
        Particle neighbor = p.getNeighbor();
        if (neighbor == null) {
            living.remove(p);
            p.alive = false;
        } else {
            living.add(occupy(neighbor));
        }
    }

    private void drawBranches(Particle p0, int maxSubBranches, int maxSubBranchesDepth) {
        int depth = 0;
        LinkedList<Particle> stack = new LinkedList<>();
        stack.add(p0);

        while (!stack.isEmpty()) {
            Particle p = stack.removeFirst();

            if (p.paint == frameCount) {
                continue;
            }

            p.paint = frameCount;

            int subBranches = 0;
            Particle neighbor = null;
            for (Particle q : p.neighbors) {
                if (q.paint != frameCount) {
                    if (neighbor == null) {
                        neighbor = q;
                    }
                    stack.addFirst(q);
                    /*
                        stack.addFirst(neighbor);
                    } else {// if (depth < maxSubBranchesDepth && subBranches < maxSubBranches) {
                        stack.addFirst(q);
                    }
                    */
                }
            }

            if (neighbor != null) {
                line(p.x, p.y, neighbor.x, neighbor.y);
            }

            depth++;
        }
    }

    private void rays() {
        Vector<Vec2D> boundary = new Vector<>();

        pushStyle();
        stroke(0, 10);
        strokeWeight(1);
        for (int x=0; x < width; x+=3) {
            int anchor = 0;
            boolean out = true;
            for (int y=0; y < height; y++) {
                if (out && lattice[x][y] != null) {
                    boundary.add(new Vec2D(x, y));
                    line(x, anchor, x, y);
                    out = false;
                    anchor = y;
                } else if (!out) {
                    if (lattice[x][y] != null) {
                        anchor = y;
                    } else if (anchor - y > 50) {
                        out = true;
                        boundary.add(new Vec2D(x, anchor));
                    }
                } else {
                }
            }
            line(x, anchor, x, height);
            boundary.add(new Vec2D(x, anchor));
        }
        popStyle();
        pushStyle();
        stroke(0, 20);
        strokeWeight(1);
        /*
        boundary.stream()
            .filter(v -> v.y > 3)
            .forEach(v -> {
                boundary.stream()
                    .filter(vv -> v != vv && vv.y > 3 && v.compareTo(vv) > 0)
                    .forEach(vv -> {
                        line(v.x, v.y, vv.x, vv.y);
                    });
            });
            */
        List<Vec2D> s = boundary.stream()
            .filter(v -> v.y > 3)
            .map(v -> v.addSelf(-w2, -h2))
            .map(Vec2D::toPolar)
            .sorted((v1, v2) -> {
                int c = Float.compare(v1.y < 0 ? 2*PI - v1.y : v1.y, v2.y < 0 ? 2*PI - v2.y : v2.y);
                return c == 0 ? Float.compare(v1.x, v2.x) : c;
            })
            .map(Vec2D::toCartesian)
            .map(v -> v.addSelf(w2, h2))
            .collect(Collectors.toList());
        for (int i=0; i < s.size()-1; i++) {
            pushStyle();
            strokeWeight(6);
            point(s.get(i).x, s.get(i).y);
            popStyle();
            line(s.get(i).x, s.get(i).y, s.get(i+1).x, s.get(i+1).y);
        }
        popStyle();
    }

    @Override
    public void draw() {
        IntStream.range(0, iterations).forEach(i -> update());
        clear();
        /*
        drawBranches(lattice[w2][h2], 1, 0);
        living.forEach(p -> point(p.x, p.y));
        if (frameCount % 5 == 0) {
        }
        drawParticles(lattice[w2][h2]);
        */
        strokeWeight(2);
        for (int y=0; y < lattice.length; y++) {
            for (int x=0; x < lattice[y].length; x++) {
                if (lattice[x][y] != null) {
                    point(x, y);
                }
            }
        }
        //println(living.size() + " " + particles.size());
    }
}
