package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;

public class AsmConstantLong {
    private final String constantName;
    private final ValueDirective directive;
    static int index = 0;
    public AsmConstantLong(long value) {
        constantName = "constant_long_" + "_" + index;
        index += 1;
        directive = new ValueDirective(value);
    }
    Label getConstantLabel() {
        return new Label(constantName, false);
    }
    public String emit() {
        String res = ".align 2\n";
        res += getConstantLabel().emit() + ":\n";
        res += directive.emit();
        return res;
    }
}
