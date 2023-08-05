package cn.edu.bit.newnewcc.backend.asm.controller;

public class LifeTimePoint {
    private enum TYPE {
        use, def
    }

    private final LifeTimeIndex index;
    private final TYPE type;

    private LifeTimePoint(LifeTimeIndex index, TYPE type) {
        this.index = index;
        this.type = type;
    }
    public static LifeTimePoint getUse(LifeTimeIndex index) {
        return new LifeTimePoint(index, TYPE.use);
    }
    public static LifeTimePoint getDef(LifeTimeIndex index) {
        return new LifeTimePoint(index, TYPE.def);
    }
    public LifeTimeIndex getIndex() {
        return index;
    }
    public boolean isDef() {
        return type == TYPE.def;
    }
    public boolean isUse() {
        return type == TYPE.use;
    }

    @Override
    public String toString() {
        return index.toString() + (type == TYPE.use ? ":use" : ":def");
    }
}
