package io.github.pflouret.sketchbook.moire;

import controlP5.Controller;
import controlP5.Group;
import controlP5.Slider;
import io.github.pflouret.sketchbook.p5.ProcessingApp;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import processing.core.PApplet;
import toxi.geom.Line2D;
import toxi.geom.Vec2D;

public class Moire extends ProcessingApp {
  private static final int PADDING = 150;

  private List<List<Line2D>> shapes = new ArrayList<>();

  private int ww;
  private int spacing = 5;

  @Override
  public void settings() {
    // size(600, 800, FX2D);
    size(800, 800);
  }

  @Override
  public void setup() {
    super.setup();

    strokeWeight(1.1f);
    stroke(0, 255);

    ww = width - 2 * PADDING;
    // shapes.add(makeShape(0, 0));
    // buildGui();
  }

  private List<Line2D> makeTriangle1(int x0, int y0, float spacing) {
    return DoubleStream.iterate(0, i -> i + spacing)
        .limit(ceil(ww / spacing))
        .mapToObj(i -> (float)i)
        .map(i -> new Line2D(x0, y0, x0 + ww, y0 + i))
        .collect(Collectors.toList());
  }

  private List<Line2D> makeSquare(int x0, int y0) {
    return IntStream.iterate(0, i -> i + spacing)
        .limit(ww / spacing)
        .mapToObj(i -> new Line2D(x0, y0 + i, x0 + ww, y0 + i))
        .collect(Collectors.toList());
  }

  private List<Line2D> makeShape(int x0, int y0) {
    return makeTriangle1(x0, y0, random(5, 8));
  }

  @Override
  public void reset() {
    clear();
    shapes.clear();
    resetSeed(true);
  }

  @Override
  protected void drawInternal() {
    clear();
    shapes.forEach(s -> s.forEach(gfx::line));
    makeShape(mouseX - ww / 2, mouseY - ww / 2).forEach(gfx::line);
  }

  @Override
  public void keyPressed() {}

  @Override
  public void keyTyped() {
    super.keyPressed();
  }

  @Override
  public void mouseClicked() {
    shapes.add(makeShape(mouseX - ww / 2, mouseY - ww / 2));
  }

  @Override
  public void buildGui() {
    super.buildGui();

    Vector<Controller> controllers = new Vector<>();

    controllers.add(
        cp.addSlider("octaves").setRange(1, 8).setNumberOfTickMarks(8).snapToTickMarks(true));

    controllers.add(cp.addSlider("falloff").setRange(0, 1));

    controllers.add(cp.addSlider("noiseScale").setRange(1, 4000));

    controllers.add(
        cp.addSlider("speed").setRange(0, 200).setNumberOfTickMarks(2000).snapToTickMarks(true));

    controllers.add(cp.addBang("reset").setSize(20, 20).plugTo(this, "reset"));

    Group g1 = cp.addGroup("g1");
    for (int y = 0, i = 0; i < controllers.size(); i++, y += 20 + 1) {
      Controller c = controllers.get(i);
      c.setPosition(0, y);
      c.setGroup(g1);
      if (c instanceof Slider) {
        c.setSize(400, 20);
      }
    }
  }

  public static void main(String[] args) {
    PApplet.main(MethodHandles.lookup().lookupClass());
  }
}
