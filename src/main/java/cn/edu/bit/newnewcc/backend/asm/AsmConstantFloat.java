package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;

public class AsmConstantFloat {
    private final String constantName;
    private final ValueTag tag;
    public AsmConstantFloat(float value) {
        constantName = "constant_float_" + "_" + Integer.toUnsignedString(Float.floatToIntBits(value));
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
