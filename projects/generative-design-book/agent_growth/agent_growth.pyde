import lib

class Context(object):
    def __init__(self):
        self.looping = True
        self.seed = int(random(9999999))
        self.circles = []

class Circle(object):
    def __init__(self, x, y, r):
        self.x = x
        self.y = y
        self.r = r

    def __str__(self):
        return "(%f,%f,%f)" % (self.x, self.y, self.r)


c = Context()

def setup():
    global c

    size(720, 720)
    background(255)
    randomSeed(c.seed)
    strokeWeight(1)
    stroke(0, 180)
    fill(0)

    #circle = Circle(random(30, width-30), random(30, height-3), 30)
    circle = Circle(width/2, height/2, 30)
    c.circles.append(circle)
    ellipse(circle.x, circle.y, 2*circle.r, 2*circle.r)
    noLoop()

def draw():
    global c

    circle = randomCircle()
    closest = sorted(c.circles, key=lambda c: pointToCircleDistance(circle, c))[0]

    noStroke()
    fill(0, 50)
    prevX = circle.x
    prevY = circle.y
    ellipse(circle.x, circle.y, 2*circle.r, 2*circle.r)

    a = atan2(circle.y-closest.y, circle.x-closest.x)
    a = a + (a <= 0 and 2*PI or 0)
    circle.x = closest.x + cos(a)*(closest.r + circle.r)
    circle.y = closest.y + sin(a)*(closest.r + circle.r)
    c.circles.append(circle)

    fill(0, 200)
    ellipse(circle.x, circle.y, 2*circle.r, 2*circle.r)
    stroke(0, 10)
    line(circle.x, circle.y, prevX, prevY)

    #line(circle.x, circle.y, closest.x, closest.y)

def pointToCircleDistance(c1, c2):
    a = atan2(c1.y-c2.y, c1.x-c2.x)
    a = a + (a <= 0 and 2*PI or 0)
    return abs(dist(c1.x, c1.y, c2.x+a*c2.r, c2.y+a*c2.r))

def randomCircle():
    r = int(random(3, 7))
    return Circle(int(random(0+r, width)), int(random(0+r, height)), r)


def mouseMoved():
    redraw()

def keyPressed():
    global c
    if key == "p":
        noLoop() if c.looping else loop()
        c.looping = not c.looping
    elif key == "S":
        lib.screenshot()
    elif key in ("z", "x"):
        pass
    elif key in ("a", "s"):
        pass

    #frame.setTitle()
    
