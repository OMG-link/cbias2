package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;

public class AsmCall extends AsmInstruction {
    public AsmCall(Label label) {
        super("", label, null, null);
    }

    @Override
    public String emit() {
        return String.format("\tcall %s\n", getOperand(1));
    }
}
