import lib

class Context(object):
    def __init__(self):
        self.looping = True
        self.seed = random(9999999)
        self.x = 1
        self.y = 1
        self.dirX = 1
        self.dirYP = 0.5
        self.lineHeight = 80
        self.sep = 2
        self.alpha = 5

    def inc(self):
        if self.y > height:
            noLoop()
        if self.x < 0 or self.x >= width:
            self.dirX = -1*self.dirX
            self.y = self.y + 15
        self.x = self.x + self.dirX*self.sep
        self.alpha = constrain(self.alpha + random(-1, 1), 5, 30)
        self.sep = constrain(self.sep + random(-0.1, 0.1), 0.2, 1.5)



c = Context()

def setup():
    global c

    size(900, 720)
    background(255)
    strokeWeight(1)
    stroke(0, 20)
    #stroke(255, 150)
    #colorMode(HSB, 360, 100, 100)
    #background(360)
    randomSeed(int(c.seed))

def draw():
    global c

    #g.stroke(c.angle, random(80, 100), random(80, 100), 150)
    for i in range(10):
        a = PVector(c.x, c.y+c.lineHeight/2)
        b = PVector(c.x, c.y-c.lineHeight/2)
        line(a.x, a.y, b.x, b.y)
        c.inc()
        c.lineHeight = constrain(c.lineHeight + (random(1) < c.dirYP and -1 or 1)*0.7*noise(frameCount), 70, 90)
        stroke(0, c.alpha)

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
        c.dirYP = c.dirYP + (key == "z" and -1*delta or delta)
    elif key in ("a", "s"):
        pass

    frame.setTitle(str(c.dirYP))
    