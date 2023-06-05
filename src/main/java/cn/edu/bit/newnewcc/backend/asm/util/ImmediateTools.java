package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;

public class ImmediateTools {
    public static final int StackMostRegisterSize = 80;
    public static boolean bitlengthNotInLimit(long value) {
        return value < -2048 || value >= 2048;
    }

    public static boolean stackOffsetNotInLimit(long value) {
        return bitlengthNotInLimit(value - StackMostRegisterSize);
    }

    public static boolean bitlengthLimit(Immediate immediate) {
        var value = immediate.getValue();
        return value >= -2048 && value < 2048;
    }
}
