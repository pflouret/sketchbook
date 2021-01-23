import lib

class Context(object):
    def __init__(self):
        self.looping = True
        self.seed = random(9999999)
        self.lineLength = 50
        self.canvas = None
        self.angle = 0
        self.dir = 1

    def incAngle(self):
        #if pmouseX-mouseX != 0:
        #    self.dir = (pmouseX-mouseX) / max(1, abs(pmouseX-mouseX))
        self.angle = (self.angle + self.dir*2) % 360


c = Context()

def setup():
    global c

    size(900, 720)
    c.canvas =  createGraphics(900, 720)
    c.overlay = createGraphics(900, 720)

    c.canvas.beginDraw()
    c.canvas.background(255)
    c.canvas.strokeWeight(0.75)
    c.canvas.stroke(0, 150)
    c.canvas.stroke(0, 150)
    #c.canvas.colorMode(HSB, 360, 100, 100)
    c.canvas.endDraw()

    #colorMode(HSB, 360, 100, 100)
    #background(360)
    randomSeed(int(c.seed))

def draw():
    global c

    if mousePressed:
        drawImpl(c.canvas)

    drawImpl(c.overlay, clear=True)

    image(c.canvas, 0, 0)
    image(c.overlay, 0, 0)

def drawImpl(g, clear=False):
    g.beginDraw()
    if clear:
        g.background(255, 0)
    #g.stroke(c.angle, random(80, 100), random(80, 100), 150)
    a, b = mouseVectors()
    g.line(a.x, a.y, b.x, b.y)
    g.endDraw()

def mouseVectors():
    a = radians(c.angle)
    r = c.lineLength
    m = PVector(mouseX, mouseY)
    v1 = PVector(cos(a)*r, sin(a)*r)
    v2 = PVector(cos(a+PI)*r, sin(a+PI)*r)
    v1.add(m)
    v2.add(m)
    c.incAngle()
    return (v1, v2)

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
    
