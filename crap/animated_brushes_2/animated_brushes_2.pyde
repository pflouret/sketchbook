import lib

class Context(object):
    def __init__(self):
        self.looping = True
        self.seed = random(9999999)
        self.lineLength = 0
        self.canvas = None
        self.angle = 0
        self.angleUnlimited = 0
        self.dir = 1
        self.dirP = 0.5
        self.fullTurns = 0

    def incAngle(self):
        #if pmouseX-mouseX != 0:
        #    self.dir = (pmouseX-mouseX) / max(1, abs(pmouseX-mouseX))
        self.angleUnlimited = self.angleUnlimited + 0.07 + random(-0.05, 0.05)
        self.angle = self.angleUnlimited % 360
        self.fullTurns = int(self.angleUnlimited / 360)


c = Context()

def setup():
    global c

    size(900, 720)
    background(255)
    strokeWeight(0.75)
    stroke(0, 10)
    #stroke(255, 150)
    #colorMode(HSB, 360, 100, 100)
    #background(360)
    randomSeed(int(c.seed))

def draw():
    global c

    #g.stroke(c.angle, random(80, 100), random(80, 100), 150)
    translate(width/2, height/2)
    for i in range(10):
        a, b = mouseVectors()
        line(a.x, a.y, b.x, b.y)

def mouseVectors():
    a = radians(c.angle)
    r = c.lineLength
    m = PVector(mouseX, mouseY)
    v1 = PVector(cos(a)*r, sin(a)*r)
    v2 = PVector(cos(a)*(r+100), sin(a)*(r+100))
    #v2 = PVector(cos(a+PI)*r, sin(a+PI)*r)
    c.incAngle()
    #c.lineLength = c.lineLength + sin(a)
    c.lineLength = c.lineLength + (random(1) < c.dirP and -1 or 1)*0.7*noise(frameCount)
    return (v1, v2)

def keyPressed():
    global c
    if key == "p":
        noLoop() if c.looping else loop()
        c.looping = not c.looping
    elif key == "S":
        lib.screenshot()
    elif key in ("z", "x"):
        delta = 0.01
        c.dirP = c.dirP + (key == "z" and -1*delta or delta)
    elif key in ("a", "s"):
        pass

    c.dir = -1*c.dir

    frame.setTitle(str(c.dirP))
    
