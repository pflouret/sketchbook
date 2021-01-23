import lib
import random as pyrandom

from lib import Point

class Context(object):
    def __init__(self):
        self.looping = True
        self.points = []
        self.shapeSize = 50 
        self.vertexCount = 12
        self.prevMouse = Point()

    def initPoints(self):
        p = lambda a: Point(cos(radians(a))*self.shapeSize, sin(radians(a))*self.shapeSize)
        step = 360/self.vertexCount
        self.points = [p(a) for a in range(0, 360+step, step)]

    def randomizePoints(self):
        for p in self.points:
            p.x = p.x + random(-0.3, 0.3)
            p.y = p.y + random(-0.3, 0.3)


c = Context()

def setup():
    size(720, 720)
    colorMode(HSB, 360, 100, 100)
    background(2, 20)
    noFill()
    stroke(0, 0, 100, 80)
    translate(width/2, height/2)
    c.initPoints()
    #c.fader = get()

def draw():
    global c

    #if frameCount < 200:
    #    background(0)
    #background(360)
    #blend(c.fader,0,0,width,height,0,0,width,height,SUBTRACT);
    noFill()
    pushMatrix()

    if mouseX != 0 or mouseY != 0:
        x = c.prevMouse.x + (mouseX-c.prevMouse.x)*0.02
        y = c.prevMouse.y + (mouseY-c.prevMouse.y)*0.02
        translate(x, y)
        c.prevMouse = Point(x, y)

    beginShape()
    curveVertex(c.points[-1].x, c.points[-1].y)
    for p in c.points:
        #strokeWeight(3)
        #point(p.x, p.y)
        strokeWeight(0.5)
        curveVertex(p.x, p.y)
    curveVertex(c.points[0].x, c.points[0].y)
    curveVertex(c.points[1].x, c.points[1].y)
    endShape()
    popMatrix()
    c.randomizePoints()

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

    #frame.setTitle()
    
