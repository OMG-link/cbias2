package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;

public class AsmConstantLong {
    private final String constantName;
    private final ValueTag tag;
    static int index = 0;
    public AsmConstantLong(long value) {
        constantName = "constant_long_" + "_" + index;
        index += 1;
        tag = new ValueTag(value);
    }
    GlobalTag getConstantTag() {
        return new GlobalTag(constantName, false);
    }
    public String emit() {
        String res = ".align 2\n";
        res += getConstantTag().emit() + ":\n";
        res += tag.emit();
        return res;
    }
}
