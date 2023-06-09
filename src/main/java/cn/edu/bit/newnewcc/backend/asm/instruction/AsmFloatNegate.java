package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;

public class AsmFloatNegate extends AsmInstruction {
    public AsmFloatNegate(FloatRegister result, FloatRegister operand1) {
        super("fneg.s", result, operand1, null);
    }
}
