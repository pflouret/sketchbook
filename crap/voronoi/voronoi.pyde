import lib

class Context(object):
    def __init__(self):
        self.looping = True
        self.seed = random(9999999)

c = Context()
add_library('toxiclibscore')
add_library('toxiclibs_p5')

def setup():
    global c

    size(720, 720)
    colorMode(HSB, 360, 100, 100)
    clear()
    randomSeed(int(c.seed))
    noiseSeed(int(c.seed))
    c.fills = []
    c.h = round(random(1))
    noLoop()
    frameRate(3)
    noFill()

    c.v = Voronoi()
    c.v.addPoints([Vec2D(random(width), random(height)) for i in range(1000)])
    c.gfx = ToxiclibsSupport(this)
    c.clipper = SutherlandHodgemanClipper(Rect(0, 0, width, height))

def draw():
    global c

    clear()

    strokeWeight(1)
    stroke(0, 100)
    for (i, r) in enumerate(c.v.getRegions()):
        #fill(c.fills[i])
        c.gfx.polygon2D(r)

    strokeWeight(2)
    stroke(360, 100, 100, 10)
    for t in c.v.getTriangles():
        c.gfx.triangle(t)

    return 
    strokeWeight(4)
    stroke(0)
    for v in c.v.getSites():
        point(v.x(), v.y())

def lloyd():
    voronoi = Voronoi()
    for r in c.v.getRegions():
        voronoi.addPoint(c.clipper.clipPolygon(r).getCentroid())
    c.v = voronoi


def lloyd1():
    newPoints = []
    for r in c.v.getRegions():
        coords = [(constrain(p[0], 0, width), constrain(p[1], 0, height)) for p in r.getCoords()]
        pairs = zip(coords[1:], coords[2:])
        pp = lambda p: (int(p[0]), int(p[1]))
        #println([pp(p) for p in coords])
        println([pp(ppp) for ppp in [(p[0], p[1]) for p in r.getCoords()]])
        pushMatrix()
        scale(0.1)
        translate(200, 200)
        r.draw(this)
        popMatrix()
        #println([(pp(p1), pp(p2)) for (p1, p2) in pairs])
        #println([(pp(p1), pp(p2)) for (p1, p2) in pairs])

        #area = sum([p1[0]*p2[1] - p2[0]*p1[1] for (p1, p2) in pairs]) / 2
        #println(area)
        #cx = coords[0][0] + sum([(p1[0]+p2[0])*(p1[0]*p2[1] - p2[0]*p1[1]) for (p1, p2) in pairs]) / (6*area)
        #cy = coords[0][1] + sum([(p1[1]+p2[1])*(p1[0]*p2[1] - p2[0]*p1[1]) for (p1, p2) in pairs]) / (6*area)
        #newPoints.append((cx, cy))

    #println([pp(p) for p in c.points])
    #print
    #println([pp(p) for p in newPoints])
    #c.points = newPoints


def mouseMoved():
    frame.setTitle("(%d, %d)" % (mouseX, mouseY))
    redraw()

def getColor():
    golden = (1 + 5 ** 0.5)/2
    c.h = (c.h + 1.0/golden) % 1
    return color(c.h*360, 90, 95, 255)

def mousePressed():
    if mouseX <= 0 or mouseY <= 0 or mouseX >= width or mouseY >= height:
        return
    #c.points.append(Vec2D(mouseX, mouseY))
    c.v.addPoint(Vec2D(mouseX, mouseY))
    #c.fills.append(color(random(360), 86, 70, 95))
    redraw()

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
    elif key == "l":
        lloyd()
    elif key in ("z", "x"):
        pass
    elif key in ("a", "s"):
        pass

    redraw()
    #frame.setTitle()
    
