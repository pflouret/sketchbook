package io.github.pflouret.sketchbook.moire;

import static java.util.stream.Collectors.toList;
import static toxi.geom.Line2D.LineIntersection.Type.INTERSECTING;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.Line2D;
import toxi.geom.Polygon2D;
import toxi.geom.Rect;
import toxi.geom.Shape2D;
import toxi.geom.Vec2D;

public class Hatch extends ProcessingApp {
  private Shape2D shape;
  private List<Line2D> hatch;
  private float spacing;
  private float angle = PI / 4;

  @Override
  public void settings() {
    size(800, 800, FX2D);
    // size(800, 800);
  }

  @Override
  public void setup() {
    super.setup();

    strokeWeight(1.1f);
    stroke(0, 255);
    // noLoop();

    spacing = 2.0f;

    shape = new Circle(w2, h2, w2 / 1.5f);
  }

  private List<Line2D> hatchLines(Polygon2D poly) {
    List<Line2D> edges =
      poly.getEdges().stream()
        .sorted((l1, l2) -> Float.compare(l1.a.x, l2.a.x))
        .collect(toList());
    return hatchStream()
        .map(
            line -> {
              List<Vec2D> isecs =
                  edges.stream()
                      .map(line::intersectLine)
                      .filter(i -> i.getType() == INTERSECTING)
                      .map(Line2D.LineIntersection::getPos)
                      .collect(toList());
              if (isecs.size() < 1) {
                return null;
              }
              return new Line2D(isecs.get(0), isecs.size() > 1 ? isecs.get(1) : line.b);
            })
        .filter(Objects::nonNull)
        .collect(toList());
  }

  private Stream<Line2D> hatchStream() {
    Rect b = shape.getBoundingCircle().getBounds();
    Vec2D c = b.getCentroid();
    return DoubleStream.iterate(-b.height/2, y -> y + spacing + random(-1f, 1f))
      .limit(ceil(b.height / spacing) + 1)
      .mapToObj(y -> (float) y)
      .map(y -> new Line2D(-b.width/2, y, b.width/2, y))
      .map(l -> l.set(l.a.getRotated(angle).addSelf(c), l.b.getRotated(angle).addSelf(c)));
  }

  @Override
  public void reset() {
    clear();
    resetSeed(true);
  }

  @Override
  protected void drawInternal() {
    clear();
    // gfx.polygon2D(((Circle) shape).toPolygon2D(6));
    // gfx.ellipse(((Circle) shape).toPolygon2D(6).getBoundingCircle());
    Circle circle = (Circle) this.shape;
    Polygon2D poly = circle.toPolygon2D(9);
    spacing = 5.5f;
    angle = radians(frameCount % 360);
    hatchLines(poly).forEach(gfx::line);
    spacing = 4.3f;
    angle = radians(frameCount % 360);
    Circle circle2 = circle.getBoundingCircle();
    circle2.addSelf(100f, -100f);
    hatchLines(circle2.toPolygon2D(10)).forEach(gfx::line);
  }

  @Override
  public void keyPressed() {
    toggleLoop();
  }

  @Override
  public void mouseClicked() {}

  public static void main(String[] args) {
    PApplet.main(MethodHandles.lookup().lookupClass());
  }
}
