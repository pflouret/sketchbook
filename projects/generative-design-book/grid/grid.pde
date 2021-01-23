int seed = (int)random(9999999);

void setup() {
  size(600, 600);
}

void draw() {
  strokeCap(SQUARE);
  smooth();
  int size = 25;
  background(255);
  strokeWeight(5);
  strokeWeight(map(mouseY, 0, height, 1, 13));
  randomSeed(mouseX/100);
  noiseSeed(seed);
  noiseSeed(mouseX/100);
  //translate(-width/2, -height/2);
  //rotate(0.1);
  
  for (int y=0; y < height; y += size) {
    for (int x=0; x < width; x += size) {
      //if (noise(x, y) > 0.65) {
      if (random(1) > 0.6) {
        if (random(1) < 0.2) {
          stroke(0, random(180, 220));
          strokeWeight(map(mouseY, 0, height, 1, 100));
        } else {
          stroke(0, 255);
          strokeWeight(random(5, 15));
        }
        //strokeWeight(map(sin(radians(frameCount % 180)), -1, 1, 0, 20));
        line(x, y, x+size, y+size);
      } else {
        stroke(0, 255);
        if (random(1) > 0.8) {
          strokeWeight(map(mouseY, 0, height, 1, 10));
        } else {
          strokeWeight(random(0.2, 2.3));
        }
        //strokeWeight(random(0.2, 1.8));
        line(x, y+size, x+size, y);
      }
    }
  }
}

void mousePressed() {
  seed = (int)random(9999999);
}