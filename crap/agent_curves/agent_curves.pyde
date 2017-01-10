import lib
import random as pyrandom

from lib import Point

class Context(object):
    def __init__(self):
        self.looping = True
        self.points = []
        self.shapeSize = 50 
        self.vertexCount = 3 
        self.prevMouse = Point()
        self.margin = 20
        self.cursorY = self.margin 

    def initPoints(self):
        m = self.margin
        self.points = [Point(m, 0)]
        self.points.extend(Point(random(m, width-m), 0) for i in range(self.vertexCount))
        self.points.append(Point(width-m, 0))

    def randomizePoints(self):
        for p in self.points:
            p.x = p.x + (random(1) < 0.02 and random(-1, 1) or random(-0.1, 0.1))
            p.y = p.y + (random(1) < 0.5 and random(-3, 3) or random(-1, 1))


c = Context()

def setup():
    size(720, 720)
    colorMode(HSB, 360, 100, 100)
    background(360)
    noFill()
    stroke(0, 0, 0, 90)
    c.initPoints()
    c.angle = 0
    frameRate(20)

    c.points = [
            (100, 10),
            (100, 10),
            [140, 10],
            [180, 10],
            (220, 10),
            (260, 10),
            (260, 10)
    ]

def draw():
    #background(360)
    translate(0, c.cursorY)
    c.points[2][0] = max(140, c.points[2][0] + random(-5, 3))
    c.points[3][1] = c.points[3][1] + (random(1) < 0.95 and random(-1, 0.2) or ((random(1) < 0.5 and -1 or 1)*random(-3, -0.5)))
    beginShape()
    for p in c.points:
        strokeWeight(5)
        #point(p[0], p[1])
        strokeWeight(2)
        curveVertex(p[0], p[1])
    endShape()
    c.angle = (c.angle + 4) % 360
    c.cursorY = c.cursorY + random(0.75,3)

def draw1():
    global c

    #if frameCount < 200:
    #    background(0)
    #background(360)
    #blend(c.fader,0,0,width,height,0,0,width,height,SUBTRACT);
    noFill()
    pushMatrix()

    translate(0, c.cursorY)
    #if mouseX != 0 or mouseY != 0:
    #    x = c.prevMouse.x + (mouseX-c.prevMouse.x)*0.02
    #    y = c.prevMouse.y + (mouseY-c.prevMouse.y)*0.02
    #    translate(x, y)
    #    c.prevMouse = Point(x, y)

    beginShape()
    curveVertex(c.margin, 0)
    for p in c.points:
        strokeWeight(3)
        #point(p.x, p.y)
        strokeWeight(0.5)
        curveVertex(p.x, p.y)
    curveVertex(width-c.margin, 0)
    endShape()
    popMatrix()
    c.randomizePoints()
    c.cursorY = c.cursorY + random(3, 5) 

def mousePressed():
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
    elif key == "c":
        background(360)
        c.cursorY = c.margin
        c.initPoints()

    redraw()
    #frame.setTitle()
    
