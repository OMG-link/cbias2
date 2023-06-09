package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 汇编乘法指令，仅支持寄存器间的乘法
 */
public class AsmMul extends AsmBinaryInstruction {
    public AsmMul(IntRegister goal, IntRegister rsource1, IntRegister rsource2) {
        super("mul", goal, rsource1, rsource2);
    }
    public AsmMul(FloatRegister goal, FloatRegister rsource1, FloatRegister rsource2) {
        super("fmul.s", goal, rsource1, rsource2);
    }
}
