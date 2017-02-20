package io.github.pflouret.sketchbook.growth.laplacian;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PVector;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/*
 * "Fast simulation of laplacian growth"
 * http://gamma.cs.unc.edu/FRAC/
 */

public class FastLaplacianGrowth extends ProcessingApp {
    private ConcurrentHashMap.KeySetView<PVector, Boolean> charges;
    private ConcurrentHashMap.KeySetView<PVector, Boolean> boundary;
    private Map<PVector, Float> candidates;

    private float R = 1f; // point charge radius
    private float eta = 3.3f;
    private static final int ITERATIONS = 100;

    private void init() {
        PVector initialCharge = new PVector(w2, h2);
        charges.add(initialCharge);
        addCandidates(initialCharge);
        //noLoop();
    }

    public void update() {
        // Minimum and maximum potentials, used to normalize all the potentials.
        float minPotential = Float.MAX_VALUE, maxPotential = Float.MIN_VALUE;
        for (Float p : candidates.values()) {
            minPotential = p < minPotential ? p : minPotential;
            maxPotential = p > maxPotential ? p : maxPotential;
        }

        float minp = minPotential, maxp = maxPotential;
        float totalNormalizedPotential = (float) candidates.values().parallelStream()
            .mapToDouble(p -> normalizedPotential(p, minp, maxp))
            .sum();

        // Try choose a new growth site using the normalized potential of each candidate site as its probability of
        // becoming the growth site.
        PVector newCharge = null;
        Iterator<Map.Entry<PVector, Float>> iter = candidates.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<PVector, Float> e = iter.next();
            float p = probability(e.getValue(), minp, maxp, totalNormalizedPotential);
            if (random(1) < p) {
                newCharge = e.getKey();
                charges.add(newCharge);
                iter.remove();

                // Calculate the new potential at each existing candidate site.
                final PVector c = newCharge;
                candidates.entrySet().parallelStream().forEach(ee -> {
                    candidates.put(ee.getKey(), potential(ee.getKey(), c, ee.getValue()));
                });

                // Add new candidate sites around the new growth site and calculate their potentials.
                addCandidates(c);
            }
        }

    }

    private void addCandidates(PVector charge) {
        IntStream.of(-1, 0, 1).parallel().forEach(i -> {
            IntStream.of(-1, 0, 1)
                .parallel()
                .mapToObj(j -> new PVector(charge.x+i*R, charge.y+j*R))
                .filter(v -> !charges.contains(v) && !candidates.containsKey(v))
                .forEach(v -> candidates.put(v, potential(v)));
        });
    }

    private float potential(PVector candidate) {
        return (float) charges.parallelStream().mapToDouble(c -> 1-R/c.dist(candidate)).sum();
    }

    private float potential(PVector candidate, PVector charge, float prevPotential) {
        return prevPotential+(1-R/charge.dist(candidate));
    }

    private float normalizedPotential(float potential, float minPotential, float maxPotential) {
        return (potential-minPotential)/(maxPotential-minPotential);
    }

    private float probability(float potential, float minPotential, float maxPotential, float totalNormalizedPotential) {
        return pow(normalizedPotential(potential, minPotential, maxPotential), eta)/totalNormalizedPotential;
    }

    @Override
    public void setup() {
        super.setup();

        clear();
        stroke(0, 190);
        strokeWeight(R*2);

        charges = ConcurrentHashMap.newKeySet();
        candidates = new ConcurrentHashMap<>(w2);

        init();
    }

    @Override
    public void draw() {
        clear();
        IntStream.range(0, ITERATIONS).forEach(i -> update());
        //candidates.keySet().forEach(c -> point(c.x, c.y));
        beginShape();
        charges.forEach(c -> point(c.x, c.y));

        /*
        pushStyle();
        stroke(255, 0, 0, 150);

        Voronoi voronoi = new Voronoi();
        voronoi.addPoints(charges.parallelStream().map(v -> new Vec2D(v.x, v.y)).collect(Collectors.toList()));
        voronoi.getTriangles().forEach(gfx::triangle);
        new ConcaveHull().calculateConcaveHull(new ArrayList<>(charges), 10).forEach(v -> vertex(v.x, v.y));
        ArrayList<ConcaveHull2.Point> hull = new ConcaveHull2().calculateConcaveHull(
            candidates.keySet().stream()
                .map(v -> new ConcaveHull2.Point((double) v.x, (double) v.y))
                .collect(Collectors.toList()), 10);
        curveVertex(hull.get(hull.size()-1).getX().floatValue(), hull.get(hull.size()-1).getY().floatValue());
        hull.forEach(p -> curveVertex(p.getX().floatValue(), p.getY().floatValue()));
        curveVertex(hull.get(0).getX().floatValue(), hull.get(0).getY().floatValue());
        popStyle();

        endShape(CLOSE);
        */
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

