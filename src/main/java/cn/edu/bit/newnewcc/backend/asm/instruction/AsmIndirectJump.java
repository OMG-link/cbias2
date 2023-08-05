package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmIndirectJump extends AsmInstruction {
    public AsmIndirectJump(IntRegister addressRegister) {
        super("", addressRegister, null, null);
    }

    @Override
    public String emit() {
        return String.format("\tjr %s\n", getOperand(1));
    }
}
