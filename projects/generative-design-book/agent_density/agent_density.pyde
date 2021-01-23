import lib

class Context(object):
    def __init__(self):
        self.looping = True
        self.seed = int(random(9999999))
        self.maxRadius = 60
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

    size(900, 900)
    colorMode(HSB, 360, 100, 100)
    background(360)
    noFill()
    strokeWeight(2)
    randomSeed(int(c.seed))

    r = c.maxRadius
    circle = Circle(random(r, width-r), random(r, height-r), r)
    c.circles.append(circle)
    ellipse(circle.x, circle.y, 2*circle.r, 2*circle.r)

    noLoop()

def draw():
    global c

    circle = randomCircle()
    closest = sorted(c.circles, key=lambda a: dist(circle.x, circle.y, a.x, a.y) - a.r)[0]

    d = dist(circle.x, circle.y, closest.x, closest.y)
    if d <= closest.r:
        return

    circle.r = min(d-closest.r, c.maxRadius)
    c.circles.append(circle)
    
    strokeWeight(0.75)
    stroke(0, 100)
    line(circle.x, circle.y, closest.x, closest.y)

    strokeWeight(2)
    stroke(0)
    ellipse(circle.x, circle.y, 2*circle.r, 2*circle.r)


def randomCircle():
    r = int(random(3, 7))
    return Circle(int(random(0+r, width)), int(random(0+r, height)), r)

def mousePressed():
    #pass
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
    
