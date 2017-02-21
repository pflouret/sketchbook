package io.github.pflouret.sketchbook.p5;

import toxi.geom.ReadonlyVec2D;

public class TVertex extends TVec {
    public TVertex prev, next;

    public TVertex(ReadonlyVec2D readonlyVec2D) {
        super(readonlyVec2D);
    }

    public void setNext(TVertex v) {
        if (next != null) {
            next.prev = v;
        }
        v.next = next;
        v.prev = this;
        next = v;
    }
}

