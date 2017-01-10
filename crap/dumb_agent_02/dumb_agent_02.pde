import java.util.Calendar;

enum Dir {
  N, NE, E, SE, S, SW, W, NW
}

private static class Coord {
  public int x;
  public int y;

  Coord(int x, int y) {
    this.x = x;
    this.y = y;
  }
}

int alph = 15;
int speed = 50;
int step = 1;
int diameter = 1;
int x = 0, y = 0;

Coord[] coords = new Coord[720];

void setup() {
  size(720, 720);
  background(255);
  noStroke();
}


void draw() {

  fill(0, alph);

  for (int j=0; j < speed; j++) {
    for (int i=0; i < coords.length; i++) {
      Coord c = coords[i];
      if (c == null) {
        continue;
      }

      Dir dir = Dir.values()[(int)random(2, 7)];
      switch (dir) {
        case N:
          c.y -= step; break;
        case NE:
          c.x += step; c.y -= step; break;
        case E:
          c.x += step; break;
        case SE:
          c.x += step; c.y += step; break;
        case S:
          c.y += step; break;
        case SW:
          c.x -= step; c.y += step; break;
        case W:
          c.x -= step; break;
        case NW:
          c.x -= step; c.y -= step; break;
      }

      if (c.y >= height) {
        coords[i] = null;
        continue;
      }
      c.x = max(0, c.x); c.y = max(0, c.y);
      c.x = min(width, c.x); c.y = min(height, c.y);

      ellipse(c.x+step/2, c.y+step/2, diameter, diameter);
    }
  }
}

void mouseDragged() {
  int x = (int)mouseX;
  if (x < coords.length && coords[x] == null) {
    coords[x] = new Coord(x, mouseY < 30 ? 0 : (int)mouseY);
  }
}

void keyPressed() {
  switch (key) {
    case 'z':
    case 'x':
      speed += (key == 'z' ? -1 : 1) * 10; break;
    case 'a':
    case 's':
      alph += (key == 'a' ? -1 : 1) * 1; break;
    case 'S':
      save(); break;

  }
  surface.setTitle(speed + " - " + alph);
}

void save() {
  Calendar now = Calendar.getInstance();
  String ts = String.format("%1$tF_%1$tH%1$tM%1$tS", now);
  saveFrame(ts+"_###.png");
}
