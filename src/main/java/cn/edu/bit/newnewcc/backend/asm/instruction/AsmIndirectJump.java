package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmIndirectJump extends AsmInstruction {
    public AsmIndirectJump(IntRegister addressRegister) {
        super(addressRegister, null, null);
    }

    @Override
    public String toString() {
        return String.format("jr %s", getOperand(1));
    }

    @Override
    public String emit() {
        return "\t" + this + "\n";
    }
}
