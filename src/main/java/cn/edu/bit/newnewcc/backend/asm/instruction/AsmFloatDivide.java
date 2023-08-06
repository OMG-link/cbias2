package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;

public class AsmFloatDivide extends AsmInstruction {
    public AsmFloatDivide(FloatRegister dest, FloatRegister source1, FloatRegister source2) {
        super(dest, source1, source2);
    }

    @Override
    public String toString() {
        return String.format("fdiv.s %s, %s, %s", getOperand(1), getOperand(2), getOperand(3));
    }

    @Override
    public String emit() {
        return "\t" + this + "\n";
    }
}
