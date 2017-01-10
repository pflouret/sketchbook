void setup() {
  size(720, 720);
  //noCursor();
  colorMode(HSB, 360, 100, 100);
  rectMode(CENTER);
  noStroke();
}

void draw() {
  background(mouseY/2.0, 70, 70);
  fill(360 - mouseY/2.0, 70, 70);
  rect(360, 360, mouseX, mouseX);
}