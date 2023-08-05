package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 有符号整数求余数运算
 */
public class AsmSignedIntegerRemainder extends AsmBinaryInstruction {
    public AsmSignedIntegerRemainder(IntRegister dest, IntRegister source1, IntRegister source2, int bitLength) {
        super("rem", dest, source1, source2);
        if (bitLength == 32) {
            setInstructionName("remw");
        }
    }
}
