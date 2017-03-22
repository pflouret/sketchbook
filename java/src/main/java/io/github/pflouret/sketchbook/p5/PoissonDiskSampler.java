package io.github.pflouret.sketchbook.p5;

import toxi.geom.Polygon2D;
import toxi.geom.Rect;
import toxi.geom.Shape2D;
import toxi.geom.Vec2D;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

/**
 * Adapted from Herman Tulleken's http://code-spot.co.za/2010/04/07/poisson-disk-sampling-example-code/
 */
public class PoissonDiskSampler {
    private final static int DEFAULT_K = 15;

    private int k;
    private Shape2D shape;
    private Rect bounds;
    private float cellSize;
    private float minDist;
    private int gridWidth, gridHeight;
    private Random random;

    private BiFunction<Float, Float, Float> distribution;

    private List<Vec2D> grid[][];
    private List<Vec2D> actives = new LinkedList<>();


    @SuppressWarnings("unchecked")
    public PoissonDiskSampler(Shape2D shape, float minDist, BiFunction<Float, Float, Float> distribution,
                              int k, Random random) {
        this.shape = shape;
        this.minDist = minDist;
        this.distribution = distribution;
        this.k = k;
        this.random = random;

        bounds = shape.getBounds();
        cellSize = minDist/(float)Math.sqrt(2);
        gridWidth = (int)(bounds.width/cellSize) + 1;
        gridHeight = (int)(bounds.height/cellSize) + 1;
        grid =  new List[gridWidth][gridHeight];

        Vec2D p;
        do {
            p = new Vec2D(bounds.x+bounds.width*random.nextFloat(), bounds.y+bounds.height*random.nextFloat());
        } while (!shape.containsPoint(p));

        int[] index = pointToGrid(p, cellSize);
        grid[index[0]][index[1]] = new ArrayList<>(100);
        grid[index[0]][index[1]].add(p);
        actives.add(p);
    }

    public PoissonDiskSampler(
        Shape2D shape, float minDist, BiFunction<Float, Float, Float> distribution, Random random) {
        this(shape, minDist, distribution, DEFAULT_K, random);
    }

    public PoissonDiskSampler(Shape2D shape, float minDist, Random random) {
        this(shape, minDist, (x, y) -> 1f, DEFAULT_K, random);
    }

    public List<Vec2D> sample(int n) {
        List<Vec2D> points = new ArrayList<>();

        while (!actives.isEmpty() && points.size() < n) {
            int listIndex = random.nextInt(actives.size());
            Vec2D center = actives.get(listIndex);

            Vec2D p = newPoint(center);
            for (int i=0; p == null && i < k; i++) {
                p = newPoint(center);
            }

            if (p != null) {
                points.add(p);
            } else {
                actives.remove(listIndex);
            }
        }

        return points;
    }

    private Vec2D newPoint(Vec2D center) {
        float fraction = distribution.apply(center.x, center.y);
        Vec2D p = randomInAnnulus(center, fraction*minDist);

        if (!shape.containsPoint(p)) {
            return null;
        }

        int[] g = pointToGrid(p, cellSize);
        int gx = g[0], gy = g[1];

        for (int i = Math.max(0, gx-2); i < Math.min(gridWidth, gx+3); i++) {
            for (int j = Math.max(0, gy-2); j < Math.min(gridHeight, gy+3); j++) {
                if (grid[i][j] != null) {
                    for (Vec2D gridPoint : grid[i][j]) {
                        if (gridPoint.distanceTo(p) < minDist*fraction) {
                            return null;
                        }
                    }
                }
            }
        }

        actives.add(p);

        if (grid[gx][gy] == null) {
            grid[gx][gy] = new ArrayList<>();
        }
        grid[gx][gy].add(p);

        return p;
    }

    private int[] pointToGrid(Vec2D p, float cellSize) {
        return new int[] { (int)((p.x-bounds.x)/cellSize),  (int)((p.y-bounds.y)/cellSize) };
    }

    private Vec2D randomInAnnulus(Vec2D centre, float minDist) {
        float radius = minDist + minDist*random.nextFloat();
        double angle = 2*Math.PI * random.nextDouble();
        double newX = radius*Math.sin(angle);
        double newY = radius*Math.cos(angle);
        return new Vec2D(centre.x + (float)newX, centre.y + (float)newY);
    }
}
