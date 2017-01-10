package io.github.pflouret.sketchbook.experiments;

import io.github.pflouret.sketchbook.p5.ProcessingApp;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.lang.invoke.MethodHandles;

public class StripesVolume extends ProcessingApp {
    private int stripeWidth = 4;
    private PImage texture;

    @Override
    public void settings() {
        size(600, 600, P2D);
    }

    @Override
    public void setup() {
        super.setup();

        textureWrap(REPEAT);
        noStroke();
        //noLoop();

        buildTexture();
    }

    @Override
    public void draw() {
        clear();

        pushMatrix();
        translate(width / 2, height / 2);

        beginShape();
        texture(texture);
        float w = 200, h = 25;
        vertex(-w, -h, -w, -h);
        vertex(w, -h, w, -h);
        vertex(w, h, w, h);
        vertex(-w, h, -w, h);
        endShape(CLOSE);

        pushMatrix();
        float angle = -radians(mouseX % 360);//PI / 4.0f;
        translate(h / tan(angle) + 1, 2 * h + 2);
        shearX(angle);


        beginShape();
        texture(texture);
        vertex(-w, -h, -w, -h);
        vertex(w, -h, w, -h);
        vertex(w, h, w, h);
        vertex(-w, h, -w, h);
        endShape(CLOSE);
        popMatrix();

        popMatrix();
        image(texture, 0, 0);
    }

    private void buildTexture() {
        PGraphics pg = createGraphics(2 * stripeWidth, 10);
        pg.beginDraw();
        pg.background(255);
        pg.fill(0);
        pg.noStroke();
        pg.rect(0, 0, stripeWidth, pg.height);
        pg.endDraw();
        texture = pg.get();
    }

    public static void main(String[] args) {
        PApplet.main(MethodHandles.lookup().lookupClass());
    }
}
