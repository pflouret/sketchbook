import random as pyrand

class Context(object):
    DIR_ANGLE_ADJUST = {
        "up": lambda a: a - 90,
        "left": lambda a: a + 180,
        "down": lambda a: a + 90,
        "right": lambda a: a 
    }

    def __init__(self):
        self.x = self.y = self.x0 = self.y0 = 0
        self.speed = 2
        self.looping = True
        self.dir = pyrand.choice(["up", "left", "down", "right"])

        self.randomizeSeed()
        self.randomizeAngle()

    def randomizeSeed(self):
        self.seed = int(round(random(99999)))

    def randomizeAngle(self):
        #a = round(random(180)/6)
        a = (floor(random(-6, 6)) + 0.5) * 90.0/6
        self.angle = self.DIR_ANGLE_ADJUST[self.dir](a)

    def flipDir(self):
        if c.x <= 4:
            self.dir = "right"
        elif c.x >= width-4:
            self.dir = "left"
        elif c.y <= 4:
            self.dir = "down"
        elif c.y >= height-4:
            self.dir = "up"

c = Context()

def setup():
    global c
    size(720, 720)
    c.y = c.y0 = round(random(height-1))
    background(255)

def draw():
    global c

    for i in xrange(c.speed):
        #strokeWeight(1)
        #stroke(200)
        #point(c.x, c.y)
        
        c.x = c.x + cos(radians(c.angle))*2
        c.y = c.y + sin(radians(c.angle))*2

        switchedDir = c.x <= 4 or c.x >= width-4 or c.y <= 4 or c.y >= height-4
        px = int(c.x)
        py = int(c.y)

        loadPixels()
        if switchedDir or pixels[py*width + px] != color(255):
            if switchedDir:
                c.flipDir()
            d = dist(c.x, c.y, c.x0, c.y0)
            if d > 10:
                c.randomizeAngle()
                strokeWeight(d / (width/15))
                stroke(0)
                line(c.x0, c.y0, c.x, c.y)
                c.x0 = c.x
                c.y0 = c.y
                c.x = min(max(5, c.x), width-5)
                c.y = min(max(5, c.y), height-5)
    
    #println("(%d, %d) %d %d %s %f" % (px, py, len(pixels), py*width+px, c.dir, c.angle))

def keyPressed():
    global c
    if key == "s":
        noLoop() if c.looping else loop()
        c.looping = not c.looping
    elif key in ("z", "x"):
        c.speed = c.speed + (key == "z" and -1 or 1)*2

    frame.setTitle(str(c.speed))
