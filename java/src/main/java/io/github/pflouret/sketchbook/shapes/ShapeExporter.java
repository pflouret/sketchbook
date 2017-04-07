package io.github.pflouret.sketchbook.shapes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pflouret.sketchbook.p5.ShapeMaker;
import processing.core.PApplet;
import processing.core.PVector;
import processing.event.KeyEvent;
import toxi.geom.Polygon2D;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Vector;
import java.util.stream.Collectors;

public class ShapeExporter extends ShapeMaker {

    private Vector<Polygon2D> polys;

    @Override
    public void settings() {
        size(800, 800);
    }

    @Override
    public void setup() {
        super.setup();
        reset();
        noStroke();
    }

    @Override
    public void reset() {
        super.reset();
        polys = new Vector<>();
    }

    @Override
    protected void drawInternal() {
        clear();
        push();
        stroke(0, 20);
        fill(0, 50);
        polys.forEach(gfx::polygon2D);
        pop();

        super.drawInternal();
    }

    protected void onNewPoly(Polygon2D p) {
        polys.add(p);
    }

    private void exportShapes() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(getSketchFilename("%s_####.json"));
            writer.println(new ObjectMapper().writeValueAsString(polys.stream()
                .map(p -> p.vertices.stream().map(v -> new PVector(v.x, v.y)).collect(Collectors.toList()))
                .collect(Collectors.toList())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch(key) {
            case 'E':
                exportShapes(); break;
            default:
                super.keyPressed(e);
        }
        redraw();
    }

    public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
}

