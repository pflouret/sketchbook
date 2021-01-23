void setup() {
  size(1000, 800);
  //noCursor();
  colorMode(HSB, 100, 80, 80);
  noStroke();
}

void draw() {
  int stepX = 10;//min(max(mouseX/10, 2), width);
  int stepY = stepX;
  for (int y = 0; y < height; y += stepY) { //<>//
    for (int x = 0; x < width; x += stepX) {
      fill((width-x)/10, mouseX/10, (height-y)/10);
      rect(x, y, stepX, stepY);
    }
  }
}