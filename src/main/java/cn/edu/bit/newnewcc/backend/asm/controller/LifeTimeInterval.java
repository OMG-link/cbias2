package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.util.ComparablePair;

public class LifeTimeInterval implements Comparable<LifeTimeInterval> {
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

    @Override
    public int compareTo(LifeTimeInterval o) {
        if (range.compareTo(o.range) != 0) {
            return range.compareTo(o.range);
        }
        return vRegID - o.vRegID;
    }
}
