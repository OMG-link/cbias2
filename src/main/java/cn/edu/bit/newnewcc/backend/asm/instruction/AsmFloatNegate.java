package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;

public class AsmFloatNegate extends AsmInstruction {
    public AsmFloatNegate(FloatRegister dest, FloatRegister source) {
        super("", dest, source, null);
    }

    @Override
    public String emit() {
        return String.format("\tfneg.s %s, %s\n", getOperand(1), getOperand(2));
    }
}
