package io.github.pflouret.sketchbook.p5;

import toxi.geom.ReadonlyVec2D;
import toxi.geom.Vec2D;

public class TVec extends Vec2D {
    public TVec() {
    }

    public TVec(float x, float y) {
        super(x, y);
    }

    public TVec(float[] v) {
        super(v);
    }

    public TVec(ReadonlyVec2D v) {
        super(v);
    }

    public PVec toPVec() {
        return new PVec(x, y);
    }
}
