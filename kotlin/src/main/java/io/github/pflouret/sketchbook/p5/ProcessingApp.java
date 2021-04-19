package io.github.pflouret.sketchbook.p5;

import com.hamoid.VideoExport;
import controlP5.ControlP5;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PStyle;
import processing.core.PVector;
import processing.event.KeyEvent;

public class ProcessingApp extends PApplet {
  public static ProcessingApp app;

  protected static final int INITIAL_W = 600;
  protected static final int INITIAL_H = 600;

  protected int bgColor = color(255);
  protected long seed;
  protected boolean redrawOnEvent = true;
  protected boolean record = false;
  protected boolean saveVideo = false;
  private VideoExport video;

  protected int w2;
  protected int h2;

  protected ControlP5 cp;

  public ProcessingApp() {
    super();
    app = this;
  }

  @Override
  public void settings() {
    size(INITIAL_W, INITIAL_H);
  }

  @Override
  public void setup() {
    super.setup();

    clear();

    surface.setResizable(true);
    surface.setTitle("");
    noFill();
    hint(ENABLE_STROKE_PURE);

    resetSeed(true);

    w2 = width / 2;
    h2 = height / 2;

    if (saveVideo) {
      video = getVideoExporter();
      video.startMovie();
    }

    registerMethod("pre", this);
  }

  @Override
  public void draw() {
    String svgFilename = "";

    if (record) {
      svgFilename = recordSVG();
    }

    drawInternal();

    if (record) {
      endRecord();
      record = false;
      pr(svgFilename);
    }

    if (video != null) {
      video.saveFrame();
    }
  }

  protected void drawInternal() {}

  public void pre() {
    processControlEvents();
  }

  public void pr(Object... args) {
    println(String.join(" ", Stream.of(args).map(Object::toString).collect(Collectors.toList())));
  }

  public void push() {
    pushMatrix();
    pushStyle();
  }

  public void pop() {
    popStyle();
    popMatrix();
  }

  public void point(PVector p) {
    point(p.x, p.y);
  }

  public void clear() {
    background(bgColor);
  }

  public void reset() {}

  public void resetSeed(boolean newSeed) {
    if (newSeed) {
      seed = (long) random(9999999f);
    }
    randomSeed(seed);
    noiseSeed(seed);
  }

  public void resetSeed() {
    resetSeed(false);
  }

  public void toggleLoop() {
    if (isLooping()) {
      noLoop();
    } else {
      loop();
    }
  }

  public <T> T randomChoice(List<T> list) {
    return list.isEmpty() ? null : list.get(floor(random(0, list.size())));
  }

//  public int randomInt(int low, int high) {
//    return getRandom().nextInt(high - low) + low;
//  }

  public PVector randomPoint() {
    return new PVector(random(0, width), random(0, height));
  }

  public boolean prob(double probability) {
    return random(1) < probability;
  }

  public void screenshot(PGraphics g) {
    saveFrame(getSketchFilename("%s_####.png"));
  }

  public void screenshot() {
    screenshot(getGraphics());
  }

  public String recordSVG() {
    String sketchFilename = getSketchFilename("%s_####.svg");
    beginRecord(SVG, sketchFilename);
    return sketchFilename;
  }

  public VideoExport getVideoExporter() {
    VideoExport video = new VideoExport(this, getSketchFilename("%s.mp4"));
    video.setFrameRate(60);
    video.setDebugging(false);
    return video;
  }

  public String getSketchFilename(String format) {
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

  @Override
  public void keyTyped(KeyEvent e) {
    switch (e.getKey()) {
      case 'c':
        clear();
        break;
      case 'r':
        reset();
        break;
      case 'R':
        record = true;
        redraw();
        break;
      case 'V':
        if (video != null) {
          video.endMovie();
          video = null;
        }
        break;
      case 'p':
        toggleLoop();
        break;
      case 'S':
        screenshot();
        break;
      case 'h':
        toggleGuiVisibility();
        break;
      default:
    }

    if (redrawOnEvent) {
      redraw();
    }
  }

  public void repeat(int n, Runnable block) {
    IntStream.range(0, n).forEach(i -> block.run());
  }

  public void buildGui() {
    cp = new ControlP5(this);
  }

  public void toggleGuiVisibility() {
    if (cp != null) {
      if (cp.isVisible()) {
        cp.hide();
      } else {
        cp.show();
      }
    }
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

  protected ConcurrentLinkedQueue<BaseControlFrame.ControlFrameEvent> controlEventQueue =
      new ConcurrentLinkedQueue<>();

  public void postControlEvent(BaseControlFrame.ControlFrameEvent e) {
    controlEventQueue.add(e);
    redraw();
  }

  protected void processControlEvents() {
    BaseControlFrame.ControlFrameEvent e;
    boolean processed = false;
    while ((e = controlEventQueue.poll()) != null) {
      try {
        Field field = getClass().getDeclaredField(e.name);
        field.setAccessible(true);
        field.set(this, e.value);
        processed = true;
        continue;
      } catch (NoSuchFieldException ex) {
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }

      try {
        Method method = getClass().getDeclaredMethod(e.name);
        method.invoke(this);
        processed = true;
      } catch (NoSuchMethodException ex) {
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
