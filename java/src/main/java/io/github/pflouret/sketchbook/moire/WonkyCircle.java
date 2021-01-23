package io.github.pflouret.sketchbook.moire;

import static java.util.stream.Collectors.toList;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.Polygon2D;

public class WonkyCircle extends ProcessingApp {

  @Override
  public void settings() {
    size(800, 800, FX2D);
    // size(800, 800);
  }

  @Override
  public void setup() {
    super.setup();
    strokeWeight(2);
    noLoop();
  }

  @Override
  protected void drawInternal() {
    clear();
    pushMatrix();
    translate(w2, h2);
    ArrayList<Polygon2D> circles = new ArrayList<>();
    Polygon2D p0 = new Circle(300).toPolygon2D(100), p = p0;

    for (int i=0; i < 90; i++) {
      p.vertices.forEach(v -> v.jitter(random(-2, 2)));
      // p.smooth(0.2f, 0.09f);
      circles.add(p);
      p = p.copy().scaleSize(1-5f/(300-i*5));
    }

    circles.forEach(gfx::polygon2D);
    popMatrix();
  }

  public static void main(String[] args) {
    PApplet.main(MethodHandles.lookup().lookupClass());
  }
}
