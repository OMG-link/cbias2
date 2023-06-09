package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmFloatDivide extends AsmBinaryInstruction{
    public AsmFloatDivide(FloatRegister result, FloatRegister operand1, FloatRegister operand2) {
        super("fdiv.s", result, operand1, operand2);
    }
}
