void setup() {
  size(720, 720);
  //noCursor();
  colorMode(HSB, 360, 720, 720);
  noStroke();
  //noLoop();
}

void draw() {
  background(0, 0, width);
  beginShape(TRIANGLE_FAN);
  vertex(width/2, height/2);
  randomSeed(mouseY);
  int step = max(2, mouseX/2);
  for (int i = 0; i <= 360; i+=360/step) {
    fill(random(360), height, width);
    vertex(cos(radians(i))*width/2+width/2, sin(radians(i))*height/2+height/2);
  }
  endShape();
}