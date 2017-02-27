package io.github.pflouret.sketchbook.p5;

import com.hamoid.VideoExport;
import controlP5.ControlP5;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import toxi.geom.Vec2D;
import toxi.processing.ToxiclibsSupport;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessingApp extends PApplet {
    protected static final int INITIAL_W = 600;
    protected static final int INITIAL_H = 600;

    protected int bgColor = color(255);
    protected long seed;
    protected boolean redraw_on_event = true;
    protected int w2;
    protected int h2;

    protected ToxiclibsSupport gfx;
    protected ControlP5 cp;

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

        resetSeed(true);

        gfx = new ToxiclibsSupport(this);
        w2 = width / 2;
        h2 = height / 2;
    }

    public PVec pv(float x, float y) {
        return new PVec(x, y);
    }
    public TVec tv(float x, float y) {
        return new TVec(x, y);
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

    public void point(PVector p) { point(p.x, p.y); }
    public void point(Vec2D p) { point(p.x, p.y); }

    public void clear() {
        background(bgColor);
    }

    public void reset() {
    }

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

    public void screenshot(PGraphics g) {
        saveFrame(getSketchFilename("%s_####.png"));
    }

    public VideoExport getVideoExporter() {
        VideoExport video = new VideoExport(this, getSketchFilename("%s.mp4"));
        video.setFrameRate(60);
        video.dontSaveDebugInfo();
        return video;
    }

    public String getSketchFilename(String format) {
        String sketchName = this.getClass().getSimpleName().toLowerCase();
        File folder = new File("/Users/pflouret/code/sketchbook/screenshots/" + sketchName);
        folder.mkdirs();

        String filename = String.format(format,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")));

        return folder.toPath()
            .resolve(filename)
            .toAbsolutePath()
            .toString();
    }

    public void screenshot() {
        screenshot(getGraphics());
    }

    @Override
    public void keyPressed() {
        switch (key) {
            case 'c':
                clear(); break;
            case 'R':
                reset(); break;
            case 'p':
                toggleLoop(); break;
            case 'S':
                screenshot(); break;
            case 'h':
                toggleGuiVisibility(); break;
            default:
        }

        if (redraw_on_event) {
            redraw();
        }
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
    /*
	public static void main(String[] args) { PApplet.main(MethodHandles.lookup().lookupClass()); }
	*/
}
