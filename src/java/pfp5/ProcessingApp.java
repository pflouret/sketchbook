package pfp5;

import processing.core.PApplet;
import processing.core.PGraphics;
import toxi.processing.ToxiclibsSupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProcessingApp extends PApplet {
	protected int bgColor = color(255);
	protected long seed;

	protected ToxiclibsSupport gfx;

	@Override
	public void settings() {
		size(600, 600);
	}

	@Override
	public void setup() {
		super.setup();

		surface.setResizable(true);
		surface.setTitle("");
		noFill();

		resetSeed(true);

		gfx = new ToxiclibsSupport(this);
	}

	public void clear() {
		background(bgColor);
	}

	public void resetSeed(boolean newSeed) {
		if (newSeed) {
			seed = (long)random(9999999f);
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

	public void screenshot(PGraphics g) throws IOException {
		String sketchPath = sketchPath();
		File folder = new File("~/code/sketchbook/screenshots");
		folder.mkdirs();

		String filename = String.format("%s_####.png",
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")));

		Path sketchName = FileSystems.getDefault().getPath(sketchPath).getFileName();
		String path = folder.toPath()
				.resolve(sketchName)
				.resolve(filename)
				.toAbsolutePath()
				.toString();
		saveFrame(path);
	}

	public void screenshot() throws IOException {
		screenshot(getGraphics());
	}
}
