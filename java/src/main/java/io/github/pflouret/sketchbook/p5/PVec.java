package io.github.pflouret.sketchbook.p5;

import processing.core.PVector;

public class PVec extends PVector {
    public PVec() {
    }

    public PVec(float x, float y, float z) {
        super(x, y, z);
    }

    public PVec(float x, float y) {
        super(x, y);
    }

    public float dist2(PVector v) {
        float dx = x - v.x, dy = y - v.y, dz = z - v.z;
        return dx*dx + dy*dy + dz*dz;
    }

    public TVec toTVec() {
        return new TVec(x, y);
    }
}
