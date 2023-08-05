package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;

public class AsmFloatNegate extends AsmInstruction {
    public AsmFloatNegate(FloatRegister dest, FloatRegister source) {
        super("fneg.s", dest, source, null);
    }
}
