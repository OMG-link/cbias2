package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 汇编有符号整数除法指令，仅支持寄存器间的除法
 */
public class AsmSignedIntegerDivide extends AsmBinaryInstruction {
    public AsmSignedIntegerDivide(IntRegister dest, IntRegister source1, IntRegister source2, int bitlength) {
        super("div", dest, source1, source2);
        if (bitlength == 32) {
            setInstructionName("divw");
        }
    }
}
