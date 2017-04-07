package io.github.pflouret.sketchbook.shapes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pflouret.sketchbook.bluenoise.PoissonDiskSampler;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import toxi.geom.Polygon2D;
import toxi.geom.Vec2D;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ShapeFiller extends ProcessingApp {
    private List<PShape> shapePoints;

    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();
        reset();
        noiseDetail(6, .93f);
        noStroke();
        noLoop();
    }

    private PShape wonkyEllipseShape(Vec2D p, float offset, int color, int alpha) {
        alpha *= 1+noise(p.x/100f, p.y/100f);

        push();
        stroke(color, alpha+3);
        fill(color, alpha);
        PShape s = createShape(GROUP);
        s.addChild(createShape(ELLIPSE, p.x, p.y, random(offset-1, offset), random(offset, offset+1)));
        s.addChild(createShape(ELLIPSE, p.x+random(-1, 1), p.y+random(-1, 1), random(offset, offset+3), random(offset+1, offset+3)));
        s.addChild(createShape(ELLIPSE, p.x+random(-1, 1), p.y+random(-1, 1), random(offset-1, offset+1), random(offset-4, offset)));
        pop();
        return s;
    }

    @Override
    public void reset() {
        Path path = FileSystems.getDefault().getPath(
            "/Users/pflouret/Dropbox/sketchout/shapeexporter/shapeexporter_2017-04-04_200221_14560.json");
        try {
            float minDist = random(8, 12);
            float offset = random(2, 5);
            int samples = 10000;

            List<List<PVector>> pointLists = new ObjectMapper().readValue(
                new String(Files.readAllBytes(path)), new TypeReference<List<List<PVector>>>() {});

            shapePoints = pointLists.stream()
                .map(pp -> pp.stream().map(v -> new Vec2D(v.x, v.y)).collect(Collectors.toList()))
                .map(Polygon2D::new)
                .map(p -> new PoissonDiskSampler(p, random(5, 8), getRandom()).sample(samples))
                .flatMap(points -> {
                    int alpha = randomInt(8, 30);
                    return points.stream().map(p -> wonkyEllipseShape(p, random(2, 4), color(20, 43, 122), alpha));
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void drawInternal() {
        clear();
        shapePoints.forEach(this::shape);
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

