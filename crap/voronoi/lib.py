import datetime
import errno
import os

class Point(object):
    def __init__(self):
        self.x = self.y = self.z = 0

    def dist(self, p):
        return dist(self.x, self.y, self.z, p.x, p.y, p.z)


def screenshot():
    # TODO: hi-res

    p = os.path.realpath(sketchPath(""))
    name = os.path.basename(p)
    ts = datetime.datetime.now().strftime("%Y-%m-%d_%H%M%S")
    dir = os.path.join(os.path.expanduser("~/code/sketchbook/screenshots"), name)
    mkdirp(dir)
    saveFrame(os.path.join(dir, "%s_####.png" % ts))

def mkdirp(path):
    try:
        os.makedirs(path)
    except OSError as exception:
        if exception.errno != errno.EEXIST:
            raise