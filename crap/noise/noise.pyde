import lib
import math
import perlin

class Context(object):
    def __init__(self):
        self.looping = True
        self.seed = random(9999999)
        self.freq = random(0, 32)
        self.octaves = 7

c = Context()

def setup():
    global c

    size(400, 400)
    #size(100, 100)
    #colorMode(HSB, 360, 100, 100)
    clear()
    randomSeed(int(c.seed))
    noiseSeed(int(c.seed))
    c.noise = perlin.SimplexNoise()
    c.noise.randomize()
    #strokeWeight(2)
    noiseDetail(c.octaves)
    noStroke()
    noLoop()

def draw():
    global c

    background(255)
    c.freq = map(mouseX, 0, width, 0, 32)
    s = 1
    noiseDetail(c.octaves, 0.6)
    e = map(mouseX, 0, width, 0, 5)
    for y in range(0, height, s):
        for x in range(0, width, s):
            nx = float(x)/width
            ny = float(y)/height
            nx = float(x)/200
            ny = float(y)/200
            v = 0
            for i in range(1, c.octaves):
                freq = pow(2, i)
                v = v + map(c.noise.noise2(freq*nx, freq*ny), -1, 1, 0, 1)/freq
            v = pow(v, e)
            #v = noise(nx, ny) + 0.05

            stroke(round(v*255))
            point(x, y)

            #fill(round(v*255))
            #rect(x, y, s, s)

    frame.setTitle("%f %d" % (e, c.octaves))


def mousePressed():
    redraw()

def clear():
    background(255)

def keyPressed():
    global c
    if key == "p":
        noLoop() if c.looping else loop()
        c.looping = not c.looping
    elif key == "S":
        lib.screenshot()
    elif key == "c":
        clear()
    elif key == "z":
        c.octaves = c.octaves - 1
    elif key == "x":
        c.octaves = c.octaves + 1
    elif key in ("a", "s"):
        pass
    elif key == "r":
        c.noise.randomize()

    redraw()
    #frame.setTitle()
    
