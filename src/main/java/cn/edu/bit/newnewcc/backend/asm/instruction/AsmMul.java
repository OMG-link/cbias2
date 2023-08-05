package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 汇编乘法指令，仅支持寄存器间的乘法
 */
public class AsmMul extends AsmBinaryInstruction {
    public AsmMul(IntRegister dest, IntRegister source1, IntRegister source2, int bitLength) {
        super("mul", dest, source1, source2);
        if (bitLength == 32) {
            setInstructionName("mulw");
        }
    }
    public AsmMul(FloatRegister dest, FloatRegister source1, FloatRegister source2) {
        super("fmul.s", dest, source1, source2);
    }
}
