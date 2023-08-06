package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.controller.LifeTimeIndex;
import cn.edu.bit.newnewcc.backend.asm.util.ComparablePair;

public class LifeTimeInterval {
    public ComparablePair<LifeTimeIndex, LifeTimeIndex> range;
    public int vRegID;

    public LifeTimeInterval(int vRegID, ComparablePair<LifeTimeIndex, LifeTimeIndex> range) {
        this.vRegID = vRegID;
        this.range = range;
    }

    @Override
    public String toString() {
        return vRegID + ":" + range.toString();
    }
}
