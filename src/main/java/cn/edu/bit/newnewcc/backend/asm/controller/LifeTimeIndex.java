package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;

public class LifeTimeIndex implements Comparable<LifeTimeIndex> {
    private enum TYPE {
        in, out
    }

    private final LifeTimeController lifeTimeController;
    private final AsmInstruction sourceInst;
    private final TYPE type;

    public boolean isIn() {
        return type == TYPE.in;
    }

    public boolean isOut() {
        return type == TYPE.out;
    }

    private LifeTimeIndex(LifeTimeController lifeTimeController, AsmInstruction sourceInst, TYPE type) {
        this.lifeTimeController = lifeTimeController;
        this.sourceInst = sourceInst;
        this.type = type;
    }

    public int getInstID() {
        return lifeTimeController.getInstID(sourceInst);
    }

    public AsmInstruction getSourceInst() {
        return sourceInst;
    }

    static LifeTimeIndex getInstIn(LifeTimeController lifeTimeController, AsmInstruction sourceInst) {
        return new LifeTimeIndex(lifeTimeController, sourceInst, TYPE.in);
    }

    static LifeTimeIndex getInstOut(LifeTimeController lifeTimeController, AsmInstruction sourceInst) {
        return new LifeTimeIndex(lifeTimeController, sourceInst, TYPE.out);
    }

    @Override
    public int compareTo(LifeTimeIndex o) {
        if (getInstID() != o.getInstID()) {
            return getInstID() - o.getInstID();
        }
        if (type != o.type) {
            return type == TYPE.in ? -1 : 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return getInstID() + ((type == TYPE.in) ? ".in" : ".out");
    }
}
