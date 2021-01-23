//int npoints = int(max(mouseX/70, 2));
int npoints = 5;

PGraphics main;
PGraphics drawing;
int radius = 100;
int i = 0;

void setup() {
  size(720, 720);
  background(#FFFFFF);
  noFill();
  
  main = createGraphics(720, 720); //<>//
  drawing = createGraphics(720, 720);

}

void draw() {
  main.beginDraw();
  main.noFill();
  drawing.beginDraw();
  drawing.noFill();
  
  main.background(#FFFFFF);
  //drawing.stroke(#666666);
  drawing.colorMode(HSB, 360, 100, 100);
  //polygon(main, width/2, height/2, mouseX/2, npoints, mouseY % 360);
  polygon(main, mouseX, mouseY, radius, npoints, 0);
  if (mousePressed) {
    drawing.stroke(i++ % 360, 70, 70);
    //polygon(drawing, width/2, height/2, mouseX/2, npoints, mouseY % 360);
    polygon(drawing, mouseX, mouseY, radius, npoints, 0);
  }
  drawing.endDraw();
  main.endDraw();
  
  image(main, 0, 0);
  image(drawing, 0, 0);
}

void polygon(PGraphics g, float x, float y, float r, int npoints, int initialAngle) {
  float angle = TWO_PI / npoints;
  float a0 = radians(initialAngle);
  g.beginShape();
  for (float a = a0; a < TWO_PI+a0; a += angle) {
    float sx = x + cos(a) * r;
    float sy = y + sin(a) * r;
    g.vertex(sx, sy);
  }
  g.endShape(CLOSE);
}

void keyTyped() {
  switch (key) {
    case 'c':
      drawing.beginDraw();
      drawing.background(0, 0, 100, 0);
      drawing.endDraw();
      return;
    case 'z':
      radius -= 5;
      return;
    case 'x':
      radius += 5;
      return;
    case 'r':
      drawing.beginDraw();
      drawing.stroke(random(0, 360), 80, 80);
      drawing.endDraw();
      return;
  }
  npoints = parseInt(""+key);
}