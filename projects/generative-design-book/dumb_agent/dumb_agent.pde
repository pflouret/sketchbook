import java.util.Calendar;

enum Dir {
  N, NE, E, SE, S, SW, W, NW
}


void setup() {
  size(720, 720);
  background(255);
  noStroke();
}

int alph = 10;
int speed = 200;
int step = 1;
int diameter = 1;
int x = 0, y = 0;

void draw() {

  fill(0, alph);

  for (int i=0; i < speed; i++) {
    Dir dir = Dir.values()[(int)random(0, 8)];

    switch (dir) {
      case N:
        y -= step; break;
      case NE:
        x += step; y -= step; break;
      case E:
        x += step; break;
      case SE:
        x += step; y += step; break;
      case S:
        y += step; break;
      case SW:
        x -= step; y += step; break;
      case W:
        x -= step; break;
      case NW:
        x -= step; y -= step; break;
    }

    x = max(0, x); y = max(0, y);
    x = min(width, x); y = min(height, y);

    ellipse(x+step/2, y+step/2, diameter, diameter);
  }
}

void mousePressed() {
  x = mouseX; y = mouseY;
}

void keyPressed() {
  switch (key) {
    case 'z':
    case 'x':
      speed += (key == 'z' ? -1 : 1) * 50; break;
    case 'a':
    case 's':
      alph += (key == 'a' ? -1 : 1) * 1; break;
    case 'S':
      save(); break;

  }
  frame.setTitle(speed + " - " + alph);
}

void save() {
  Calendar now = Calendar.getInstance();
  String ts = String.format("%1$tF_%1$tH%1$tM%1$tS", now);
  saveFrame(ts+"_###.png");
}
