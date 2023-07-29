package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 汇编有符号整数除法指令，仅支持寄存器间的除法
 */
public class AsmSignedIntegerDivide extends AsmBinaryInstruction {
    public AsmSignedIntegerDivide(IntRegister goal, IntRegister rsource1, IntRegister rsource2, int bitlength) {
        super("div", goal, rsource1, rsource2);
        if (bitlength == 32) {
            setInstructionName("divw");
        }
    }
}
