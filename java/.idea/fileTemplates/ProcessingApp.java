#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;

import java.lang.invoke.MethodHandles;

public class ${NAME} extends ProcessingApp {
  @Override
  public void settings() {
    super.settings();
    // size(800, 800);
    // size(800, 800, P3D);
  }

  @Override
  public void setup() {
    super.setup();

    reset();
  }

  @Override
  public void reset() {}

  @Override
  public void drawInternal() {}

  @Override
  public void keyPressed() {
    super.keyPressed();
  }

  public static void main(String[] args) {
    PApplet.main(MethodHandles.lookup().lookupClass());
  }
}