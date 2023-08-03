package cn.edu.bit.newnewcc.backend.asm.controller;

public class LifeTimeIndex implements Comparable<LifeTimeIndex> {
    private enum TYPE {
        in, out
    }

    private final int instID;
    private final TYPE type;

    public boolean isIn() {
        return type == TYPE.in;
    }

    public boolean isOut() {
        return type == TYPE.out;
    }

    private LifeTimeIndex(int instID, TYPE type) {
        this.instID = instID;
        this.type = type;
    }

    public int getInstID() {
        return instID;
    }

    static LifeTimeIndex getInstIn(int instID) {
        return new LifeTimeIndex(instID, TYPE.in);
    }

    static LifeTimeIndex getInstOut(int instID) {
        return new LifeTimeIndex(instID, TYPE.out);
    }

    @Override
    public int compareTo(LifeTimeIndex o) {
        if (instID != o.instID) {
            return instID - o.instID;
        }
        if (type != o.type) {
            return type == TYPE.in ? -1 : 1;
        }
        return 0;
    }
}
