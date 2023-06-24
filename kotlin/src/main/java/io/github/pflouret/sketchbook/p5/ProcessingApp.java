package io.github.pflouret.sketchbook.p5;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PStyle;
import processing.core.PVector;

public class ProcessingApp extends PApplet {
  public static ProcessingApp app;

  protected long seed;
  protected boolean redrawOnEvent = true;
  protected boolean saveVideo = false;

  public ProcessingApp() {
    super();
    app = this;
  }


  public void pre() {
//    processControlEvents();
  }

  public void pr(Object... args) {
    println(String.join(" ", Stream.of(args).map(Object::toString).collect(Collectors.toList())));
  }

  public boolean prob(double probability) {
    return random(1) < probability;
  }

  public String makeSketchFilename(String format) {
    String sketchName = this.getClass().getSimpleName().toLowerCase();
    String home = System.getProperty("user.home");
    File folder = new File(home + "/Genart/sketchout/" + sketchName);
    folder.mkdirs();

    String filename =
        String.format(
            insertFrame(sketchName + "_" + format),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss_S")));

    return folder.toPath().resolve(filename).toAbsolutePath().toString();
  }

  public void noiseline(PGraphics pg, PVector v0, PVector v1, float resolution, float probability) {
    int steps = round(v0.dist(v1) / resolution);
    IntStream.range(0, steps)
        .filter(i -> prob(probability))
        .mapToObj(i -> PVector.lerp(v0, v1, ((float) i) / steps))
        .forEach(p -> pg.point(p.x, p.y));
  }

  public void noiseline(PVector v0, PVector v1, float probability) {
    noiseline(g, v0, v1, .01f, probability);
  }

  public void noiseline(float x0, float y0, float x1, float y1, float probability) {
    noiseline(new PVector(x0, y0), new PVector(x1, y1), probability);
  }

  public void humanline(
      PGraphics pg, float x0, float y0, float x1, float y1, float squiggleFactor) {

    float d = dist(x0, y0, x1, y1);
    float step = d <= 200 ? .5f : (d <= 400 ? .3f : .2f);

    LinkedList<PVector> points = humanlinePoints(x0, y0, x1, y1, (float) 2, step, squiggleFactor);

    // Control points.
    points.addFirst(points.getFirst());
    points.addLast(points.getLast());

    pg.beginShape();
    points.forEach(p -> pg.curveVertex(p.x, p.y));
    pg.endShape();
  }

  public void humanline(float x0, float y0, float x1, float y1, float squiggleFactor) {
    humanline(g, x0, y0, x1, y1, squiggleFactor);
  }

  public void humanline(PVector p1, PVector p2, float squiggleFactor) {
    humanline(g, p1.x, p1.y, p2.x, p2.y, squiggleFactor);
  }

  public LinkedList<PVector> humanlinePoints(
      float x0, float y0, float x1, float y1, float tf, float step, float squiggleFactor) {

    float d = dist(x0, y0, x1, y1);
    float squiggleRange = squiggleFactor * (d <= 150 ? .5f : (d <= 400 ? 1 : 2));

    return IntStream.range(0, round(tf / step))
        .mapToDouble(i -> i * step)
        .mapToObj(
            t ->
                new PVector(
                    f(x0, x1, (float) t / tf, squiggleRange),
                    f(y0, y1, (float) t / tf, squiggleRange)))
        .collect(Collectors.toCollection(LinkedList::new));
  }

  private float f(float c0, float c1, float tau, float squiggleRange) {
    float squiggle = random(-squiggleRange, squiggleRange);
    return c0 + (c0 - c1) * (15 * pow(tau, 4) - 6 * pow(tau, 5) - 10 * pow(tau, 3)) + squiggle;
  }

  public PImage stripedTexture(
      int boundingWidth, int boundingHeight, float angle, float spacing, PStyle style) {

    angle = radians(angle);
    // Rotate square top vertices around its center.
    float w2 = boundingWidth / 2f, h2 = boundingHeight / 2f;
    PVector p1 = new PVector(-w2, -h2).rotate(angle).add(w2, h2);
    PVector p2 = new PVector(w2, -h2).rotate(angle).add(w2, h2);
    PVector p0 = new PVector(boundingWidth, 0);
    PVector pdiff = p2.copy().sub(p1);

    p1.set(round(p1.x * 1000) / 1000f, round(p1.y * 1000) / 1000f);
    p2.set(round(p1.x * 1000) / 1000f, round(p1.y * 1000) / 1000f);

    // Distance from NE corner to the rotated line formed by p1 and p2.
    float d = abs(pdiff.y * p0.x - pdiff.x * p0.y + p2.x * p1.y - p2.y * p1.x) / pdiff.mag();

    PGraphics pg = createGraphics(boundingWidth, boundingHeight);
    pg.beginDraw();
    pg.style(style);
    pg.translate(w2, h2);
    pg.rotate(angle);
    for (float y = -h2 - d; y <= h2 + d; y += spacing) {
      pg.line(-w2 - d, y, w2 + d, y);
    }
    pg.endDraw();

    return pg.get();
  }

  public PImage stripedTexture(int boundingWidth, int boundingHeight, float angle, float spacing) {
    return stripedTexture(boundingWidth, boundingHeight, angle, spacing, g.getStyle());
  }

  public void wonkyEllipse(PVector p, float offset) {
    ellipse(p.x, p.y, random(offset - 1, offset), random(offset, offset + 1));
    ellipse(
        p.x + random(-1, 1),
        p.y + random(-1, 1),
        random(offset, offset + 3),
        random(offset + 1, offset + 3));
    ellipse(
        p.x + random(-1, 1),
        p.y + random(-1, 1),
        random(offset - 1, offset + 1),
        random(offset - 4, offset));
  }

  public void curvyStrip(PVector p, float curveLengthSpread) {
    beginShape();
    float b = random(curveLengthSpread - 1, curveLengthSpread + 1);
    float off = random(3, 15);
    if (prob(.98)) {
      curveVertex(p.x + off, p.y + off);
      curveVertex(p.x, p.y);
      curveVertex(p.x + b, p.y - b);
      curveVertex(p.x + b + off, p.y + b + off / 2f);
    } else {
      curveVertex(p.x - off, p.y - off);
      curveVertex(p.x, p.y);
      curveVertex(p.x - b, p.y + b);
      curveVertex(p.x - b - off, p.y - b - off / 2f);
    }
    endShape();
  }
}
