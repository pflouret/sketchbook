int borderSize = 2;
int crookedness = 5;
long seed;
int step = 50;
int fadeStep = 20;

void setup() {
  size(720, 720);
  noStroke();
  seed = (long)random(9999999);
  //colorMode(HSB, 360, 100, 100);
  //background(0, 0, 100);
  background(255);
}

void draw() {
  //noLoop();
  randomSeed(seed);

  //background(0, 0, 100, 50);
  //background(255, 10);
  //fill(mouseX/2, 100, 100, 80);

  //int step = (int)max(2, mouseY / 10);

  //fadeGraphics(450000);
  fadeGraphics(fadeStep);
  crookedness = (int)max(0.2, mouseX/8);
  borderSize = (int)max(1, mouseY/8);

  for (int y=0; y < height; y += step) {
    for (int x=0; x < width; x += step) {
      //fill(355, 99, 68+(int)random(-15, 5), 80+(int)random(0, 10));
      fill(random(1) < 1 ? #0000FF : #FF0000, 10+(int)random(0, 10));
      pushMatrix();
      translate(x+1, y+1);
      beginShape();
      vertex(0+r(), 0+r());
      vertex(step-borderSize+r(), 0+r());
      vertex(step-borderSize+r(), step-borderSize+r());
      vertex(0+r(), step-borderSize+r());
      endShape(CLOSE);
      //rotate(radians(random(-10, 10)));
      //rect(borderSize, borderSize, step-borderSize, step-borderSize);
      popMatrix();
    }
  }
}

void keyPressed() {
  switch (key) {
    case 'z':
      //borderSize--;
      fadeStep -= 5;
      break;
    case 'x':
      //borderSize++;
      fadeStep += 5;
      break;
    case 'a':
      //crookedness--;
      step-=2;
      break;
    case 's':
      //crookedness++;
      step+=2;
      break;
    case 'r':
      seed = (long)random(999999);
      randomSeed(seed);
      break;
  }
  println(fadeStep);
  loop();
}

float r() {
  return random(-crookedness, crookedness);
}

void mouseMoved() {
  loop();
}

void fadeGraphics(int fadeAmount) {
  loadPixels();
 
  // iterate over pixels
  for (int i=0; i < pixels.length; i++) {
 
    // get alpha value
    int a = (pixels[i] >> 24) & 0xFF ;
 
    // reduce alpha value
    a = max(0, a-fadeAmount);
 
    // assign color with new alpha-value
    pixels[i] = a<<24 | (pixels[i]) & 0xFFFFFF ;
  }

  for (int i=0; i < pixels.length-20; i++) {
    pixels[i] = pixels[i+(int)random(9.5, 20.1)];
  }
 
  updatePixels();
}

/*
void fadeGraphics(PGraphics c, int fadeAmount) {
  c.beginDraw();
  c.loadPixels();
 
  // iterate over pixels
  for (int i =0; i<c.pixels.length; i++) {
 
    // get alpha value
    int alpha = (c.pixels[i] >> 24) & 0xFF ;
 
    // reduce alpha value
    alpha = max(0, alpha-fadeAmount);
 
    // assign color with new alpha-value
    c.pixels[i] = alpha<<24 | (c.pixels[i]) & 0xFFFFFF ;
  }
 
  canvas.updatePixels();
  canvas.endDraw();
}
*/
