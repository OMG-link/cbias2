package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.util.ComparablePair;

public class LifeTimeInterval implements Comparable<LifeTimeInterval> {
    public ComparablePair<LifeTimeIndex, LifeTimeIndex> range;
    public Register reg;

    public int getVRegID() {
        if (!reg.isVirtual()) {
            throw new RuntimeException("get virtual register id on physical register");
        }
        return reg.getAbsoluteIndex();
    }

    public LifeTimeInterval(Register reg, ComparablePair<LifeTimeIndex, LifeTimeIndex> range) {
        this.reg = reg;
        this.range = range;
    }

    @Override
    public String toString() {
        return reg.emit() + ":" + range.toString();
    }

    @Override
    public int compareTo(LifeTimeInterval o) {
        if (range.compareTo(o.range) != 0) {
            return range.compareTo(o.range);
        }
        return reg.getIndex() - o.reg.getIndex();
    }
}
