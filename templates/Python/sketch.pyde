import lib

class Context(object):
    def __init__(self):
        self.looping = True
        self.seed = random(9999999)

c = Context()

def setup():
    global c

    size(720, 720)
    colorMode(HSB, 360, 100, 100)
    clear()
    randomSeed(int(c.seed))
    noiseSeed(int(c.seed))

def draw():
    global c
    translate(width/2, height/2)
    strokeWeight(10)
    triangle(-50, 50, 50, 50, 0, -sqrt(3)*100/4)


def clear():
    background(360)

def keyPressed():
    global c
    if key == "p":
        noLoop() if c.looping else loop()
        c.looping = not c.looping
    elif key == "S":
        lib.screenshot()
    elif key == "c":
        clear()
    elif key in ("z", "x"):
        pass
    elif key in ("a", "s"):
        pass

    #frame.setTitle()
    
