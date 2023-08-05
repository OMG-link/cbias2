package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmFloatDivide extends AsmBinaryInstruction{
    public AsmFloatDivide(FloatRegister dest, FloatRegister source1, FloatRegister source2) {
        super("fdiv.s", dest, source1, source2);
    }
}
