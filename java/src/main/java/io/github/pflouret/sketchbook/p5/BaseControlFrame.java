package io.github.pflouret.sketchbook.p5;

import controlP5.Button;
import controlP5.ColorWheel;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.Toggle;
import processing.core.PApplet;

import java.lang.reflect.Field;

public class BaseControlFrame extends PApplet {
    protected int w, h;
    protected ProcessingApp parent;
    protected ControlP5 cp;

    public BaseControlFrame(ProcessingApp parent, int w, int h) {
        super();
        this.parent = parent;
        this.w = w;
        this.h = h;

        PApplet.runSketch(new String[]{this.getClass().getName()}, this);
    }

    @Override
    public void settings() {
        size(w, h);
    }

    @Override
    public void setup() {
        cp = new ControlP5(this);
        cp.addListener(e -> this.parent.postControlEvent(new ControlFrameEvent(e)));
    }

    @Override
    public void draw() {
        background(0);
    }

    public void initValues() {
        cp.getAll().forEach(c -> {
            try {
                Field field = parent.getClass().getDeclaredField(c.getName());
                field.setAccessible(true);
                if (field.getType().equals(boolean.class)) {
                    c.setValue(field.getBoolean(parent) ? 1 : 0);
                } else if (c instanceof ColorWheel){
                    ((ColorWheel) c).setRGB(field.getInt(parent));
                } else {
                    c.setValue(field.getFloat(parent));
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
            }
        });
    }



    public class ControlFrameEvent {
        public String name;
        public Object value;

        public ControlFrameEvent(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public ControlFrameEvent(ControlEvent e) {
            Controller<?> c = e.getController();
            this.name = c.getName();
            if (c instanceof Toggle) {
                this.value = ((Toggle)c).getBooleanValue();
            } else if (c instanceof Button) {
                this.value = ((Button) c).getBooleanValue();
            } else if (c instanceof ColorWheel) {
                this.value = ((ColorWheel) c).getRGB();
            } else {
                this.value = c.getValue();
            }
        }
    }
}
