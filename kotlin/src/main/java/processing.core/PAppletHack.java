package processing.core;

import java.util.Random;

public class PAppletHack extends PApplet {
    public Random getRandom() {
        return internalRandom;
    }
}
